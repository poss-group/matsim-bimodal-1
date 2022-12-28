/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package de.mpi.ds.utils;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import static de.mpi.ds.utils.GeneralUtils.*;
import static de.mpi.ds.utils.ScenarioCreator.*;

/**
 * @author tthunig
 */
public class NetworkCreator implements UtilComponent {
    private final static Logger LOG = Logger.getLogger(NetworkCreator.class.getName());

    private static final Map<String, int[]> directions = Map.of(
            "north", new int[]{0, 1},
            "east", new int[]{1, 0},
            "south", new int[]{0, -1},
            "west", new int[]{-1, 0}
    );

    //    private double railGridSpacing;
    private double systemSize;
    private int railInterval;
    private int small_railInterval;
    private double carGridSpacing;
    private double linkCapacity;
    private double effectiveFreeTrainSpeed;
    private double freeSpeedCar;
    private double numberOfLanes;
    private boolean diagonalConnections;
    private boolean smallLinksCloseToNodes;
    private boolean createTrainLines;
    private TransitScheduleCreator transitScheduleCreator;
    private boolean periodic_network;

    public NetworkCreator(double systemSize, int railInterval, int small_railInterval, double carGridSpacing,
                          double linkCapacity, double effectiveFreeSpeedTrain, double numberOfLanes,
                          double freeSpeedCar, boolean diagonalConnections,
                          boolean smallLinksCloseToNodes, boolean createTrainLines, TransitScheduleCreator transitScheduleCreator, boolean periodic_network) {
        this.systemSize = systemSize;
//        this.railGridSpacing = railGridSpacing;
        this.carGridSpacing = carGridSpacing;
        this.linkCapacity = linkCapacity;
        this.effectiveFreeTrainSpeed = effectiveFreeSpeedTrain;
        this.numberOfLanes = numberOfLanes;
        this.freeSpeedCar = freeSpeedCar;
        this.diagonalConnections = diagonalConnections;
        this.railInterval = railInterval;
        this.small_railInterval = small_railInterval;
        this.smallLinksCloseToNodes = smallLinksCloseToNodes;
        this.createTrainLines = createTrainLines;
        this.transitScheduleCreator = transitScheduleCreator;
        this.periodic_network = periodic_network;
    }

    public static void main(String... args) {
        String path = "./output/network_diag.xml.gz";
        double cellLength = 1000;
        double systemSize = 10000;
        int ptInterval = 4; // L/l
        int linkCapacity = 1000;
        double freeSpeedTrain = 60 / 3.6 * 1.4;
        double numberOfLanes = 4;
        double freeSpeedCar = 30 / 3.6;
//        new NetworkCreator(cellLength, systemSize, ptInterval, linkCapacity, freeSpeedTrain,
//                numberOfLanes, freeSpeedCar, true).createGridNetwork(path, true);
    }

    public void createGridNetwork(String outputPathNet, String outputPathSchedule, String outputPathVehicles) {
        // create an empty network
        Network net = NetworkUtils.createNetwork();
        NetworkFactory fac = net.getFactory();

        // create nodes and add to network
        int n_x = (int) (systemSize / carGridSpacing + 1); // So that there are L_l_fraction*gridLengthInCells links per
        // direction
        int n_y = (int) (systemSize / carGridSpacing + 1);
        LOG.info("small_railInterval" + small_railInterval);

        Node[][] nodes = new Node[n_x][n_y];
        for (int i = 0; i < n_x; i++) {
            for (int j = 0; j < n_y; j++) {
                String newNodeId = i + "_" + j;
                boolean newNodeStationAttribute = false;
                boolean newNodeStationAttribute_corssing = false;
                Node n = fac.createNode(Id.createNodeId(newNodeId),
                        new Coord(i * carGridSpacing, j * carGridSpacing));
                if (createTrainLines && periodic_network) {
                    // create train lines with no crossing
                    if ((i % railInterval == 0) && i != n_x - 1 && j != n_y - 1) {
                        if ((j % small_railInterval == 0)){
                            newNodeStationAttribute = true;
                            newNodeStationAttribute_corssing = false;
                        }
                    }

                    if ((j % railInterval == 0) && i != n_x - 1 && j != n_y - 1){
                        if ((i % small_railInterval == 0)){
                            newNodeStationAttribute = true;
                            newNodeStationAttribute_corssing = false;
                        }
                    }
                    if ((i % railInterval == 0) && (j % railInterval == 0) && i != n_x - 1 && j != n_y - 1) {
//                            && (i + ptInterval < n_x  && j + ptInterval < n_y)) { // For periodic BC
//                        newNodeId = "PT_" + i / railInterval + "_" + j / railInterval;
                        newNodeStationAttribute = true;
                        newNodeStationAttribute_corssing = true;
                    }
                } else if (createTrainLines && !periodic_network){
                    // create train lines with no crossing
                    if ((i- (int) railInterval/2)%railInterval==0){
                        if (j%small_railInterval == 0 ){
                            newNodeStationAttribute = true;
                            newNodeStationAttribute_corssing = false;
                        }
                    }

                    if ((j - (int) railInterval/2)%railInterval==0 ){
                        if (i%small_railInterval == 0){
                            newNodeStationAttribute = true;
                            newNodeStationAttribute_corssing = false;
                        }
                    }

                    if ((i - (int) railInterval/2)%railInterval==0 && (j -(int) railInterval/2)%railInterval==0 && i!=n_x-1 && j!=n_y-1){
                        newNodeStationAttribute = true;
                        newNodeStationAttribute_corssing = true;
                    }
                }

                n.getAttributes().putAttribute(IS_STATION_NODE, newNodeStationAttribute);
                n.getAttributes().putAttribute(IS_STATION_CROSSING_NODE, newNodeStationAttribute_corssing);
                nodes[i][j] = n;
                net.addNode(n);
            }
        }

        double periodicLength = 0.00001;
//         double periodicLength = 1;
            // Add links to network
        for (int i = 0; i < n_y; i++) {
            for (int j = 0; j < n_x; j++) {
                int i_minus1_periodic = (((i - 1) % n_y) + n_y) % n_y;
                int j_minus1_periodic = (((j - 1) % n_x) + n_x) % n_x;
//                 int i_minusPtInterval_periodic = (((i - ptInterval) % n_y) + n_y) % n_y;
//                 int j_minusPtInterval_periodic = (((j - ptInterval) % n_x) + n_x) % n_x;
                if (i - 1 >= 0) {
                    insertCarLinks(net, fac, nodes[i][j], nodes[i_minus1_periodic][j], carGridSpacing, false, true);
                } else if (periodic_network){
                    insertCarLinks(net, fac, nodes[i][j], nodes[i_minus1_periodic][j], periodicLength, true, false);
                }
                if ((j - 1) >= 0) {
                        insertCarLinks(net, fac, nodes[i][j], nodes[i][j_minus1_periodic], carGridSpacing, false, true);
                } else if (periodic_network){
                        insertCarLinks(net, fac, nodes[i][j], nodes[i][j_minus1_periodic], periodicLength, true, false);
                }
            }
        }

        if (createTrainLines) {
            transitScheduleCreator.createPtLinksVehiclesSchedule(net, nodes, outputPathSchedule, outputPathVehicles);
        }

        if (diagonalConnections) {
            makeDiagConnections(net, fac);
//            makeTriGrid(net, fac);
        }
        // this has to be done second because diagonal connections where also introduced before
//        putNodesCloseToStations(net, fac);
        try {
            File outFile = new File(outputPathNet);
            // create output folder if necessary
            Files.createDirectories(outFile.getParentFile().toPath());
            // write network
            new NetworkWriter(net).write(outFile.getAbsolutePath());
        } catch (Exception e) {
            System.out.println("Failed to write output...");
            e.printStackTrace();
        }
    }


    private void insertCarLinks(Network net, NetworkFactory fac, Node a, Node b, double length,
                                boolean periodicConnection, boolean isStartLink) {
        Link l1 = fac.createLink(Id.createLinkId(String.valueOf(a.getId()).concat("-")
                        .concat(String.valueOf(b.getId()))),
                a, b);
        Link l2 = fac.createLink(Id.createLinkId(String.valueOf(b.getId()).concat("-")
                        .concat(String.valueOf(a.getId()))),
                b, a);
        setLinkAttributes(l1, linkCapacity, length, freeSpeedCar, numberOfLanes, periodicConnection, isStartLink);
        setLinkAttributes(l2, linkCapacity, length, freeSpeedCar, numberOfLanes, periodicConnection, isStartLink);
        setLinkModes(l1, NETWORK_MODE_CAR);
        setLinkModes(l2, NETWORK_MODE_CAR);
        net.addLink(l1);
        net.addLink(l2);
    }

    private static String direction2String(int x, int y) {
        switch (x) {
            case -1:
                switch (y) {
                    case -1:
                        return "southwest";
                    case 0:
                        return "west";
                    case 1:
                        return "northwest";
                }
            case 0:
                switch (y) {
                    case -1:
                        return "south";
                    case 1:
                        return "north";
                }
            case 1:
                switch (y) {
                    case -1:
                        return "southeast";
                    case 0:
                        return "east";
                    case 1:
                        return "northeast";
                }
            default:
                return "null";
        }
    }

    /**
     * This method adds nodes close to the nodes where the stations are going to be to reduce transitwalks of passengers
     *
     * @param net the network
     * @param fac the network factory
     */

    private void makeDiagConnections(Network net, NetworkFactory fac) {
        double diag_length = Math.sqrt(carGridSpacing * carGridSpacing + carGridSpacing * carGridSpacing);
        for (Node temp : net.getNodes().values()) {
            List<Node> diagNeighbours = temp.getOutLinks().values().stream()
                    .map(Link::getToNode)
                    .flatMap(n -> n.getOutLinks().values().stream().map(Link::getToNode))
                    .filter(n -> {
                        double dist = calculateDistanceNonPeriodic(n.getCoord(), temp.getCoord());
                        return doubleCloseToZero(dist - diag_length);
                    })
                    .distinct()
                    .collect(Collectors.toList());
            for (Node ndiag : diagNeighbours) {
                // Only consider one direction because the other one is done when iterating over neighbour node
                Link nij_ndiag = fac.createLink(Id.createLinkId(temp.getId() + "-" + ndiag.getId()), temp, ndiag);
                setLinkAttributes(nij_ndiag, linkCapacity, diag_length, freeSpeedCar, numberOfLanes, false, true);
                setLinkModes(nij_ndiag, NETWORK_MODE_CAR);

                net.addLink(nij_ndiag);
            }
        }
    }

    public static void copyLinkProperties(Link link, Link toCopyLink) {
        toCopyLink.setCapacity(link.getCapacity());
        toCopyLink.setAllowedModes(link.getAllowedModes());
        toCopyLink.setFreespeed(link.getFreespeed());
        toCopyLink.setNumberOfLanes(link.getNumberOfLanes());
        Set<Map.Entry<String, Object>> entries = link.getAttributes().getAsMap().entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            toCopyLink.getAttributes().putAttribute(entry.getKey(), entry.getValue());
        }
    }

     static void setLinkAttributes(Link link, double capacity, double length, double freeSpeed, double numberLanes,
                                   boolean isPeriodic, boolean isStartLink) {
        link.setCapacity(capacity);
        link.setLength(length);
        link.setFreespeed(freeSpeed);
        link.setNumberOfLanes(numberLanes);
        link.getAttributes().putAttribute(PERIODIC_LINK, isPeriodic);
        link.getAttributes().putAttribute(IS_START_LINK, isStartLink);
    }

    static void setLinkModes(Link link, String modes) {
        HashSet<String> hashSet = new HashSet<String>();
        hashSet.add(modes);
        link.setAllowedModes(hashSet);
    }

}
