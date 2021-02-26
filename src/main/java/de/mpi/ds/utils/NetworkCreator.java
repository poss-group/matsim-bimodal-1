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
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
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

    private double railGridSpacing;
    private double systemSize;
    private int ptInterval;
    private double carGridSpacing;
    private double linkCapacity;
    private double freeSpeedTrainForSchedule;
    private double freeSpeedCar;
    private double numberOfLanes;
    private boolean diagonalConnections;
    private Random random;

    public NetworkCreator(double systemSize, double railGridSpacing, double carGridSpacing,
                          double linkCapacity, double freeSpeedTrainForSchedule, double numberOfLanes,
                          double freeSpeedCar, boolean diagonalConnections, Random random) {
        this.systemSize = systemSize;
        this.railGridSpacing = railGridSpacing;
        this.carGridSpacing = carGridSpacing;
        this.linkCapacity = linkCapacity;
        this.freeSpeedTrainForSchedule = freeSpeedTrainForSchedule;
        this.numberOfLanes = numberOfLanes;
        this.freeSpeedCar = freeSpeedCar;
        this.diagonalConnections = diagonalConnections;
        this.ptInterval = (int) (railGridSpacing / carGridSpacing);
        this.random = random;
    }

    public static void main(String... args) {
        String path = "./output/network_diag.xml.gz";
        double cellLength = 1000;
        double systemSize = 10000;
        int ptInterval = 4; // L/l
        int linkCapacity = 1000;
        double freeSpeedTrainForSchedule = 60 / 3.6 * 1.4;
        double numberOfLanes = 4;
        double freeSpeedCar = 30 / 3.6;
//        new NetworkCreator(cellLength, systemSize, ptInterval, linkCapacity, freeSpeedTrainForSchedule,
//                numberOfLanes, freeSpeedCar, true).createGridNetwork(path, true);
    }

    public void createGridNetwork(String path, boolean createTrainLanes) {
        // create an empty network
        Network net = NetworkUtils.createNetwork();
        NetworkFactory fac = net.getFactory();

        // create nodes and add to network
        int n_x = (int) (systemSize / carGridSpacing + 1); // So that there are L_l_fraction*gridLengthInCells links per
        // direction
        int n_y = (int) (systemSize / carGridSpacing + 1);
        int n_xPt = (int) (systemSize / railGridSpacing + 1);
        int n_yPt = (int) (systemSize / railGridSpacing + 1);
        assert (n_xPt > 1 && n_yPt > 1) : "There must be at least 2 stations";
        //TODO make possible to have just 2 stations
        assert (systemSize / railGridSpacing >= 2) : "does not make sense with periodic BC";
        Node[][] nodes = new Node[n_x][n_y];
        int[] stationNodesX = new int[n_xPt];
        int[] stationNodesY = new int[n_yPt];
        for (int i = 0; i < n_y; i++) {
            for (int j = 0; j < n_x; j++) {
                String newNodeId = i + "_" + j;
                boolean newStationAtrribute = false;
                if (createTrainLanes) {
                    if ((i % ptInterval == 0) && (j % ptInterval == 0)) {
//                            && (i + ptInterval < n_x  && j + ptInterval < n_y)) { // For periodic BC
                        newNodeId = "PT_" + i / ptInterval + "_" + j / ptInterval;
                        newStationAtrribute = true;
                        stationNodesX[i / ptInterval] = i;
                        stationNodesY[j / ptInterval] = j;
                    }
                }

                Node n = fac.createNode(Id.createNodeId(newNodeId),
                        new Coord(i * carGridSpacing, j * carGridSpacing));
                n.getAttributes().putAttribute(IS_STATION, newStationAtrribute);
                nodes[i][j] = n;
                net.addNode(n);
            }
        }

        double periodicLength = 0.00001;
//        double periodicLength = 1;
        // Add links to network
        for (int i = 0; i < n_y; i++) {
            for (int j = 0; j < n_x; j++) {
                int i_minus1_periodic = (((i - 1) % n_y) + n_y) % n_y;
                int j_minus1_periodic = (((j - 1) % n_x) + n_x) % n_x;
//                int i_minusPtInterval_periodic = (((i - ptInterval) % n_y) + n_y) % n_y;
//                int j_minusPtInterval_periodic = (((j - ptInterval) % n_x) + n_x) % n_x;
                if (i - 1 >= 0) {
                    insertCarLinks(net, fac, nodes[i][j], nodes[i_minus1_periodic][j], carGridSpacing, false);
                } else {
                    insertCarLinks(net, fac, nodes[i][j], nodes[i_minus1_periodic][j], periodicLength, true);
                }
                if ((j - 1) >= 0) {
                    insertCarLinks(net, fac, nodes[i][j], nodes[i][j_minus1_periodic], carGridSpacing, false);
                } else {
                    insertCarLinks(net, fac, nodes[i][j], nodes[i][j_minus1_periodic], periodicLength, true);
                }
            }
        }
        if (createTrainLanes) {
            int iterToX = n_xPt > 2 ? n_xPt : 1; // if there are only two stations per direction it does not make sense
            int iterToY = n_yPt > 2 ? n_yPt : 1;
            for (int i = 0; i < iterToX; i++) {
                for (int j = 0; j < iterToY; j++) {
                    int i_minus1_periodic = (((i - 1) % n_xPt) + n_xPt) % n_xPt;
                    int j_minus1_periodic = (((j - 1) % n_yPt) + n_yPt) % n_yPt;

                    Node to = nodes[stationNodesX[i]][stationNodesY[j]];

                    Node from = nodes[stationNodesX[i_minus1_periodic]][stationNodesY[j]];
                    double length = calculateDistancePeriodicBC(from, to, systemSize);
                    length = doubleCloseToZero(length) ? periodicLength : length;
                    insertTrainLinks(net, fac, from, to, length, i == 0);

                    from = nodes[stationNodesX[i]][stationNodesY[j_minus1_periodic]];
                    length = calculateDistancePeriodicBC(from, to, systemSize);
                    length = doubleCloseToZero(length) ? periodicLength : length;
                    insertTrainLinks(net, fac, from, to, length, j == 0);
//                    if (i - ptInterval >= 0) {
//                        insertTrainLinks(net, fac, nodes[i][j], nodes[i_minusPtInterval_periodic][j], cellLength,
//                                false);
//                    } else {
//                        //i_minus1_periodic is right because point is identified with last point
//                        insertTrainLinks(net, fac, nodes[i][j], nodes[i_minus1_periodic][j], periodicLength, true);
//                    }
//                    if (j - ptInterval >= 0) {
//                        insertTrainLinks(net, fac, nodes[i][j], nodes[i][j_minusPtInterval_periodic], cellLength,
//                                false);
//                    } else {
//                        insertTrainLinks(net, fac, nodes[i][j], nodes[i][j_minus1_periodic], periodicLength, true);
//                    }
                }

            }
        }
        if (diagonalConnections) {
            makeDiagConnections(net, fac);
        }
        // this has to be done second because diagonal connections where also introduced before
        putNodesCloseToStations(net, fac, createTrainLanes);
        try {
            File outFile = new File(path);
            // create output folder if necessary
            Files.createDirectories(outFile.getParentFile().toPath());
            // write network
            new NetworkWriter(net).write(outFile.getAbsolutePath());
        } catch (Exception e) {
            System.out.println("Failed to write output...");
            e.printStackTrace();
        }
    }

    private void insertTrainLinks(Network net, NetworkFactory fac, Node a, Node b, double length,
                                  boolean periodicConnection) {
        Link l3 = fac.createLink(Id.createLinkId(String.valueOf(a.getId()).concat("-")
                        .concat(String.valueOf(b.getId()).concat("_pt"))),
                a, b);
        Link l4 = fac.createLink(Id.createLinkId(String.valueOf(b.getId()).concat("-")
                        .concat(String.valueOf(a.getId()).concat("_pt"))),
                b, a);
        setLinkAttributes(l3, linkCapacity, length, freeSpeedTrainForSchedule, numberOfLanes);
        setLinkAttributes(l4, linkCapacity, length, freeSpeedTrainForSchedule, numberOfLanes);
        l3.getAttributes().putAttribute(IS_FACILITY, false);
        l4.getAttributes().putAttribute(IS_FACILITY, false);
        if (periodicConnection) {
            l3.getAttributes().putAttribute(PERIODIC_LINK, true);
            l4.getAttributes().putAttribute(PERIODIC_LINK, true);
        } else {
            l3.getAttributes().putAttribute(PERIODIC_LINK, false);
            l4.getAttributes().putAttribute(PERIODIC_LINK, false);
        }
        setLinkModes(l3, NETWORK_MODE_TRAIN);
        setLinkModes(l4, NETWORK_MODE_TRAIN);
        net.addLink(l3);
        net.addLink(l4);
    }

    private void insertCarLinks(Network net, NetworkFactory fac, Node a, Node b, double length,
                                boolean periodicConnection) {
        Link l1 = fac.createLink(Id.createLinkId(String.valueOf(a.getId()).concat("-")
                        .concat(String.valueOf(b.getId()))),
                a, b);
        Link l2 = fac.createLink(Id.createLinkId(String.valueOf(b.getId()).concat("-")
                        .concat(String.valueOf(a.getId()))),
                b, a);
        setLinkAttributes(l1, linkCapacity, length, freeSpeedCar, numberOfLanes);
        setLinkAttributes(l2, linkCapacity, length, freeSpeedCar, numberOfLanes);
        l1.getAttributes().putAttribute(IS_FACILITY, false);
        l2.getAttributes().putAttribute(IS_FACILITY, false);
        if (periodicConnection) {
            l1.getAttributes().putAttribute(PERIODIC_LINK, true);
            l2.getAttributes().putAttribute(PERIODIC_LINK, true);
        } else {
            l1.getAttributes().putAttribute(PERIODIC_LINK, false);
            l2.getAttributes().putAttribute(PERIODIC_LINK, false);
        }
        setLinkModes(l1, "car");
        setLinkModes(l2, "car");
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
     * @param net              the network
     * @param fac              the network factory
     * @param createTrainLanes
     */
    private void putNodesCloseToStations(Network net, NetworkFactory fac,
                                         boolean createTrainLanes) {
//        List<Node> stations = net.getNodes().values().stream()
//                .filter(n -> n.getAttributes().getAttribute(IS_STATION).equals(true)).collect(Collectors.toList());
//        List<Node> nonStations = net.getNodes().values().stream()
//                .filter(n -> n.getAttributes().getAttribute(IS_STATION).equals(false)).collect(Collectors.toList());

//        for (Node n : stations) {
//            for (Link link : n.getInLinks().values()) {
//                divide(net, fac, link, "in", createTrainLanes);
//            }
//            for (Link link : n.getOutLinks().values()) {
//                divide(net, fac, link, "out", createTrainLanes);
//            }
//        }

        // For each node so that there are no possible "U turns" in simulations (facilities are actually links)
        // Not just like nodes = net.getNodes().values() because then iteration is over the reference (nodes grow in
        // every iteration
        List<Node> nodes = new ArrayList<>(net.getNodes().values());
        for (Node n : nodes) {
            //sort lists so same direction gets picked for in and outlink
            List<Link> outLinks = n.getOutLinks().values().stream()
                    .filter(l -> l.getAttributes().getAttribute(PERIODIC_LINK).equals(false))
                    .filter(l -> !l.getAllowedModes().contains(NETWORK_MODE_TRAIN))
                    .sorted(Comparator.comparing(l -> l.getToNode().toString()))
                    .collect(Collectors.toList());
            List<Link> inLinks = n.getInLinks().values().stream()
                    .filter(l -> l.getAttributes().getAttribute(PERIODIC_LINK).equals(false))
                    .filter(l -> !l.getAllowedModes().contains(NETWORK_MODE_TRAIN))
                    .sorted(Comparator.comparing(l -> l.getFromNode().toString()))
                    .collect(Collectors.toList());
            int randInt = random.nextInt(outLinks.size());
            Link outLink = outLinks.get(randInt);
            Link inLink = inLinks.get(randInt);
//            if (inLink.getAllowedModes().contains(TransportMode.pt) || outLink.getAllowedModes().contains(TransportMode.pt)) {
//                System.out.println("wtf");
//            }
            divide(net, fac, outLink, "out", createTrainLanes);
            divide(net, fac, inLink, "in", createTrainLanes);
        }
    }


    private void divide(Network net, NetworkFactory fac, Link link, String inOut,
                        boolean createTrainLanes) {

        // Do nothing if link length is zero
        if (link.getAttributes().getAttribute(PERIODIC_LINK).equals(true)) {
            return;
        }
//        if (link.getAllowedModes().contains(TransportMode.pt)) {
//            System.out.println("wtf");
//        }
        // else split link in two and create a node in between
        Node putNearNeighbourNode = null;
        Node neighbourNode = null;
        if (inOut.equals("in")) {
            putNearNeighbourNode = link.getToNode();
            neighbourNode = link.getFromNode();
        } else if (inOut.equals("out")) {
            putNearNeighbourNode = link.getFromNode();
            neighbourNode = link.getToNode();
        }
        double putNearNeighbourNodeX = putNearNeighbourNode.getCoord().getX();
        double putNearNeighbourNodeY = putNearNeighbourNode.getCoord().getY();
        double neighX = neighbourNode.getCoord().getX();
        double neighY = neighbourNode.getCoord().getY();
        int dirX = (int) Math.signum(neighX - putNearNeighbourNodeX);
        int dirY = (int) Math.signum(neighY - putNearNeighbourNodeY);

        Id<Node> nodeId = Id.createNodeId(
                putNearNeighbourNode.getId().toString().concat(direction2String(dirX, dirY)));
        Node node = net.getNodes().get(nodeId);
        if (node == null) {
            node = fac.createNode(nodeId, new Coord(putNearNeighbourNodeX + dirX, putNearNeighbourNodeY + dirY));
            node.getAttributes().putAttribute(IS_STATION, false);
            net.addNode(node);
        } else {
            node = net.getNodes().get(nodeId);
        }
        String ptStr = link.getAllowedModes().contains(NETWORK_MODE_TRAIN) ? "_pt" : "";
        String firstLinkId = link.getFromNode().getId() + "-" + node.getId() + ptStr;
        Link fstLink = fac.createLink(Id.createLinkId(firstLinkId), link.getFromNode(), node);
        String secondLinkId = node.getId() + "-" + link.getToNode().getId() + ptStr;
        Link scndLink = fac.createLink(Id.createLinkId(secondLinkId), node, link.getToNode());

        copyLinkProperties(link, fstLink);
        copyLinkProperties(link, scndLink);
        fstLink.setLength(calculateDistancePeriodicBC(fstLink.getFromNode(), fstLink.getToNode(), systemSize));
        scndLink.setLength(calculateDistancePeriodicBC(scndLink.getFromNode(), scndLink.getToNode(), systemSize));
        if (inOut.equals("in")) {
            scndLink.getAttributes().putAttribute(IS_FACILITY, true);
//            if (scndLink.getId().toString().contains("_pt")) {
//                System.out.println("strange");
//            }
        } else if (inOut.equals("out")) {
            fstLink.getAttributes().putAttribute(IS_FACILITY, true);
//            if (fstLink.getId().toString().contains("_pt")) {
//                System.out.println("strange");
//            }
        }
        net.addLink(fstLink);
        net.addLink(scndLink);
        net.removeLink(link.getId());
    }

    private void makeDiagConnections(Network net, NetworkFactory fac) {
        double diag_length = Math.sqrt(carGridSpacing * carGridSpacing + carGridSpacing * carGridSpacing);
        List<Link> newLinks = new ArrayList<>();
        for (Node temp : net.getNodes().values()) {
            List<Node> diagNeighbours = temp.getOutLinks().values().stream()
                    .map(Link::getToNode)
                    .flatMap(n -> n.getOutLinks().values().stream().map(Link::getToNode))
                    .filter(n -> {
                        //TODO introduce diag connection also over boundary maybe
                        double dist = calculateDistanceNonPeriodic(n.getCoord(), temp.getCoord());
                        return doubleCloseToZero(dist - diag_length);
                    })
                    .distinct()
                    .collect(Collectors.toList());
            for (Node ndiag : diagNeighbours) {
                // Only consider one direction because the other one is done when iterating over neighbour node
                Link nij_ndiag = fac.createLink(Id.createLinkId(temp.getId() + "-" + ndiag.getId()), temp, ndiag);
                setLinkAttributes(nij_ndiag, linkCapacity, diag_length, freeSpeedCar, numberOfLanes);
                nij_ndiag.getAttributes().putAttribute(PERIODIC_LINK, false);
                nij_ndiag.getAttributes().putAttribute(IS_FACILITY, false);
                setLinkModes(nij_ndiag, "car");

                newLinks.add(nij_ndiag);
            }
        }
        for (Link newLink : newLinks) {
            net.addLink(newLink);
        }
    }


    private void copyLinkProperties(Link link, Link toCopyLink) {
        toCopyLink.setCapacity(link.getCapacity());
        toCopyLink.setAllowedModes(link.getAllowedModes());
        toCopyLink.setFreespeed(link.getFreespeed());
        toCopyLink.setNumberOfLanes(link.getNumberOfLanes());
        Set<Map.Entry<String, Object>> entries = link.getAttributes().getAsMap().entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            toCopyLink.getAttributes().putAttribute(entry.getKey(), entry.getValue());
        }
    }

    private void setLinkAttributes(Link link, double capacity, double length, double freeSpeed, double numberLanes) {
        link.setCapacity(capacity);
        link.setLength(length);
        link.setFreespeed(freeSpeed);
        link.setNumberOfLanes(numberLanes);
    }

    private void setLinkModes(Link link, String modes) {
        HashSet<String> hash_Set = new HashSet<String>();
        hash_Set.add(modes);
        link.setAllowedModes(hash_Set);
    }

}
