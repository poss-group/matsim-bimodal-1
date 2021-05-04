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
public class NetworkCreatorRadCirc implements UtilComponent {
    private final static Logger LOG = Logger.getLogger(NetworkCreatorRadCirc.class.getName());

    private static final Map<String, int[]> directions = Map.of(
            "north", new int[]{0, 1},
            "east", new int[]{1, 0},
            "south", new int[]{0, -1},
            "west", new int[]{-1, 0}
    );

    //    private double railGridSpacing;
    private double systemSize;
    private int railInterval;
    private double carGridSpacing;
    private double linkCapacity;
    private double freeSpeedTrain;
    private double freeSpeedCar;
    private double numberOfLanes;
    private boolean diagonalConnections;
    private Random random;
    private boolean smallLinksCloseToNodes;
    private boolean createTrainLines;

    public NetworkCreatorRadCirc(double systemSize, int railInterval, double carGridSpacing,
                                 double linkCapacity, double freeSpeedTrain, double numberOfLanes,
                                 double freeSpeedCar, boolean diagonalConnections, Random random,
                                 boolean smallLinksCloseToNodes, boolean createTrainLines) {
        this.systemSize = systemSize;
//        this.railGridSpacing = railGridSpacing;
        this.carGridSpacing = carGridSpacing;
        this.linkCapacity = linkCapacity;
        this.freeSpeedTrain = freeSpeedTrain;
        this.numberOfLanes = numberOfLanes;
        this.freeSpeedCar = freeSpeedCar;
        this.diagonalConnections = diagonalConnections;
        this.railInterval = railInterval;
        this.random = random;
        this.smallLinksCloseToNodes = smallLinksCloseToNodes;
        this.createTrainLines = createTrainLines;
    }

    public static void main(String... args) {
        String path = "./output/network_circ_rad.xml.gz";
        double cellLength = 1000;
        double systemSize = 1000;
        int ptInterval = 4; // L/l
        int linkCapacity = 1000;
        double freeSpeedTrain = 60 / 3.6 * 1.4;
        double numberOfLanes = 4;
        double freeSpeedCar = 30 / 3.6;
        new NetworkCreatorRadCirc(systemSize, ptInterval, 200, linkCapacity, freeSpeedTrain,
                numberOfLanes, freeSpeedCar, true, new Random(), false, true).createGridNetwork(path);
    }

    public void createGridNetwork(String path) {
        // create an empty network
        Network net = NetworkUtils.createNetwork();
        NetworkFactory fac = net.getFactory();

        double deltaPhi = 60;
        double deltaR = 100;
        int nR = (int) (systemSize / deltaR) - 1;
        int nPhi = (int) (360 / deltaPhi);
        Node[][] nodes = new Node[nR][nPhi];

        // Create one center node
        String centerNodeString = "0_0";
        Node centerNode = fac.createNode(Id.createNodeId(centerNodeString),
                new Coord(1e-5, 1e-5)); // 0,0 not possible for landmarks refining
        centerNode.getAttributes().putAttribute(IS_STATION_NODE, true);
        net.addNode(centerNode);
        // Create other nodes
        for (int i = 0; i < nR; i++) {
            for (int j = 0; j < nPhi; j++) {
                int ip = i + 1;
                double r = ip * deltaR;
                double phi = j * deltaPhi / 180 * Math.PI;
                String newNodeId = ip + "_" + j;
                double xCoord = Math.cos(phi) * r;
                double yCoord = Math.sin(phi) * r;
                Node n = fac.createNode(Id.createNodeId(newNodeId), new Coord(xCoord, yCoord));
                n.getAttributes().putAttribute(IS_STATION_NODE, true);
                nodes[i][j] = n;
                net.addNode(n);
            }
        }

        // Links in phi direction
        for (int i = 0; i < nodes.length; i++) {
            for (int j = 0; j < nodes[0].length; j++) {
                insertCarLinks(net, fac, nodes[i][j], nodes[i][(j + 1) % nPhi]);
            }
        }

        // Links from/to center
        for (int j = 0; j < nPhi; j++) {
            insertCarLinks(net, fac, centerNode, nodes[0][j]);
            insertTrainLinks(net, fac, centerNode, nodes[0][j]);
        }

        // Links in radial direction
        for (int i = 0; i < nodes.length - 1; i++) {
            for (int j = 0; j < nodes[0].length; j++) {
                insertCarLinks(net, fac, nodes[i][j], nodes[i + 1][j]);
                insertTrainLinks(net, fac, nodes[i][j], nodes[i + 1][j]);
            }
        }
        // TODO train links in radial direction

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


    private void insertCarLinks(Network net, NetworkFactory fac, Node a, Node b) {
        Link l1 = fac.createLink(Id.createLinkId(String.valueOf(a.getId()).concat("-")
                .concat(String.valueOf(b.getId()))), a, b);
        Link l2 = fac.createLink(Id.createLinkId(String.valueOf(b.getId()).concat("-")
                .concat(String.valueOf(a.getId()))), b, a);
        double length = GeneralUtils.calculateDistanceNonPeriodic(a, b);
        setLinkAttributes(l1, linkCapacity, length, freeSpeedCar, numberOfLanes);
        setLinkAttributes(l2, linkCapacity, length, freeSpeedCar, numberOfLanes);
        l1.getAttributes().putAttribute(PERIODIC_LINK, false);
        l2.getAttributes().putAttribute(PERIODIC_LINK, false);
        setLinkModes(l1, NETWORK_MODE_CAR);
        setLinkModes(l2, NETWORK_MODE_CAR);
        net.addLink(l1);
        net.addLink(l2);
    }

    private void insertTrainLinks(Network net, NetworkFactory fac, Node a, Node b) {
        Link l3 = fac.createLink(Id.createLinkId(String.valueOf(a.getId()).concat("-")
                        .concat(String.valueOf(b.getId()).concat("_pt"))),
                a, b);
        Link l4 = fac.createLink(Id.createLinkId(String.valueOf(b.getId()).concat("-")
                        .concat(String.valueOf(a.getId()).concat("_pt"))),
                b, a);
        double length = GeneralUtils.calculateDistanceNonPeriodic(a, b);
        setLinkAttributes(l3, linkCapacity, length, freeSpeedTrain, numberOfLanes);
        setLinkAttributes(l4, linkCapacity, length, freeSpeedTrain, numberOfLanes);
        l3.getAttributes().putAttribute(PERIODIC_LINK, false);
        l4.getAttributes().putAttribute(PERIODIC_LINK, false);
        setLinkModes(l3, NETWORK_MODE_TRAIN);
        setLinkModes(l4, NETWORK_MODE_TRAIN);
        net.addLink(l3);
        net.addLink(l4);
    }

    private void setLinkAttributes(Link link, double capacity, double length, double freeSpeed, double numberLanes) {
        link.setCapacity(capacity);
        link.setLength(length);
        link.setFreespeed(freeSpeed);
        link.setNumberOfLanes(numberLanes);
        link.getAttributes().putAttribute(PERIODIC_LINK, false);
        link.getAttributes().putAttribute(IS_START_LINK, false);
    }

    private void setLinkModes(Link link, String modes) {
        HashSet<String> hashSet = new HashSet<String>();
        hashSet.add(modes);
        link.setAllowedModes(hashSet);
    }

}
