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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import static de.mpi.ds.utils.GeneralUtils.calculateDistancePeriodicBC;
import static de.mpi.ds.utils.GeneralUtils.doubleCloseToZero;

/**
 * @author tthunig
 */
public class NetworkCreator implements UtilComponent {
    private final static Logger LOG = Logger.getLogger(NetworkCreator.class.getName());

    private final static String PERIODIC_LINK = "periodicConnection";

    private static final Map<String, int[]> directions = Map.of(
            "north", new int[]{0, 1},
            "east", new int[]{1, 0},
            "south", new int[]{0, -1},
            "west", new int[]{-1, 0}
    );

    private double cellLength;
    private double systemSize;
    private int systemSizeOverGridSize;
    private int systemSizeOverPtGridSize;
    private int ptInterval;
    private double linkLength;
    private double linkCapacity;
    private double freeSpeedTrainForSchedule;
    private double freeSpeedCar;
    private double numberOfLanes;
    private boolean diagonalConnections;

    public NetworkCreator(double systemSize, int systemSizeOverGridSize, int systemSizeOverPtGridSize,
                          double linkCapacity, double freeSpeedTrainForSchedule, double numberOfLanes,
                          double freeSpeedCar, boolean diagonalConnections) {
        this.systemSizeOverPtGridSize = systemSizeOverPtGridSize;
        this.cellLength = systemSize / systemSizeOverPtGridSize;
        this.systemSize = systemSize;
        this.systemSizeOverGridSize = systemSizeOverGridSize;
        this.linkLength = systemSize / systemSizeOverGridSize;
        this.linkCapacity = linkCapacity;
        this.freeSpeedTrainForSchedule = freeSpeedTrainForSchedule;
        this.numberOfLanes = numberOfLanes;
        this.freeSpeedCar = freeSpeedCar;
        this.diagonalConnections = diagonalConnections;
        this.ptInterval = systemSizeOverGridSize / systemSizeOverPtGridSize;
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
        int n_x = (int) (systemSizeOverGridSize + 1); // So that there are L_l_fraction*gridLengthInCells links per
        // direction
        int n_y = (int) (systemSizeOverGridSize + 1);
        Node[][] nodes = new Node[n_x][n_y];
        for (int i = 0; i < n_y; i++) {
            for (int j = 0; j < n_x; j++) {
                String newNodeId = i + "_" + j;
                boolean newStationAtrribute = false;
                if (createTrainLanes) {
                    if ((i % ptInterval == 0) && (j % ptInterval == 0)) {
                        newNodeId = "PT_" + i / ptInterval + "_" + j / ptInterval;
                        newStationAtrribute = true;
                    }
                }

                Node n = fac.createNode(Id.createNodeId(newNodeId),
                        new Coord(i * systemSize/systemSizeOverGridSize, j * systemSize / systemSizeOverGridSize));
                n.getAttributes().putAttribute("isStation", newStationAtrribute);
                nodes[i][j] = n;
                net.addNode(n);
            }
        }
        // Add links to network
        for (int i = 0; i < n_y; i++) {
            for (int j = 0; j < n_x; j++) {
                int i_minus1_periodic = (((i - 1) % n_y) + n_y) % n_y;
                int j_minus1_periodic = (((j - 1) % n_x) + n_x) % n_x;
                int i_minusPtInterval_periodic = (((i - ptInterval) % n_y) + n_y) % n_y;
                int j_minusPtInterval_periodic = (((j - ptInterval) % n_x) + n_x) % n_x;
                double periodicLength = 0.00001;
                if (i - 1 >= 0) {
                    insertCarLinks(net, fac, nodes[i][j], nodes[i_minus1_periodic][j], linkLength, false);
                } else {
                    insertCarLinks(net, fac, nodes[i][j], nodes[i_minus1_periodic][j], periodicLength, true);
                }
                if ((j - 1) >= 0) {
                    insertCarLinks(net, fac, nodes[i][j], nodes[i][j_minus1_periodic], linkLength, false);
                } else {
                    insertCarLinks(net, fac, nodes[i][j], nodes[i][j_minus1_periodic], periodicLength, true);
                }
                if ((i % ptInterval == 0) &&
                        (j % ptInterval == 0) && createTrainLanes) {
                    if (i - ptInterval >= 0) {
                        insertTrainLinks(net, fac, nodes[i][j], nodes[i_minusPtInterval_periodic][j], cellLength,
                                false);
                    } else {
                        //i_minus1_periodic is right because point is identified with last point
                        insertTrainLinks(net, fac, nodes[i][j], nodes[i_minus1_periodic][j], periodicLength, true);
                    }
                    if (j - ptInterval >= 0) {
                        insertTrainLinks(net, fac, nodes[i][j], nodes[i][j_minusPtInterval_periodic], cellLength,
                                false);
                    } else {
                        insertTrainLinks(net, fac, nodes[i][j], nodes[i][j_minus1_periodic], periodicLength, true);
                    }
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
        if (periodicConnection) {
            l3.getAttributes().putAttribute(PERIODIC_LINK, true);
            l4.getAttributes().putAttribute(PERIODIC_LINK, true);
        } else {
            l3.getAttributes().putAttribute(PERIODIC_LINK, false);
            l4.getAttributes().putAttribute(PERIODIC_LINK, false);
        }
        setLinkModes(l3, "train");
        setLinkModes(l4, "train");
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

    private void makeDiagConnections(Network net, NetworkFactory fac) {
        double diag_length = Math.sqrt(linkLength * linkLength + linkLength * linkLength);
        List<Link> newLinks = new ArrayList<>();
        for (Node temp : net.getNodes().values()) {
            List<Node> diagNeighbours = temp.getOutLinks().values().stream()
                    .map(Link::getToNode)
                    .flatMap(n -> n.getOutLinks().values().stream().map(Link::getToNode))
                    .filter(n -> {
                        double dist = DistanceUtils.calculateDistance(n.getCoord(), temp.getCoord());
                        return doubleCloseToZero(dist - diag_length);
                    })
                    .distinct()
                    .collect(Collectors.toList());
            for (Node ndiag : diagNeighbours) {
                // Only consider one direction because the other one is done when iterating over neighbour node
                Link nij_ndiag = fac.createLink(Id.createLinkId(temp.getId() + "-" + ndiag.getId()), temp, ndiag);
                setLinkAttributes(nij_ndiag, linkCapacity, diag_length, freeSpeedCar, numberOfLanes);
                nij_ndiag.getAttributes().putAttribute(PERIODIC_LINK, false);
                setLinkModes(nij_ndiag, "car");

                newLinks.add(nij_ndiag);
            }
        }
        for (Link newLink : newLinks) {
            net.addLink(newLink);
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
        List<Node> stations = net.getNodes().values().stream()
                .filter(n -> n.getAttributes().getAttribute("isStation").equals(true)).collect(Collectors.toList());
        for (Node n : stations) {
            for (Link link : n.getInLinks().values()) {
                divide(net, fac, link, "in", createTrainLanes);
            }
            for (Link link : n.getOutLinks().values()) {
                divide(net, fac, link, "out", createTrainLanes);
            }
        }
    }

    private void divide(Network net, NetworkFactory fac, Link link, String inOut,
                        boolean createTrainLanes) {

        // Do nothing if link length is zero
        if (link.getAttributes().getAttribute(PERIODIC_LINK).equals(true)) {
            return;
        }
        // else split link in two and create a node in between
        Node stationNode = null;
        Node neighbourNode = null;
        if (inOut.equals("in")) {
            stationNode = link.getToNode();
            neighbourNode = link.getFromNode();
        } else if (inOut.equals("out")) {
            stationNode = link.getFromNode();
            neighbourNode = link.getToNode();
        }
        double stationX = stationNode.getCoord().getX();
        double stationY = stationNode.getCoord().getY();
        double neighX = neighbourNode.getCoord().getX();
        double neighY = neighbourNode.getCoord().getY();
        int dirX = (int) Math.signum(neighX - stationX);
        int dirY = (int) Math.signum(neighY - stationY);

        Id<Node> nodeId = Id.createNodeId(
                stationNode.getId().toString().concat(direction2String(dirX, dirY)));
        Node node = net.getNodes().get(nodeId);
        if (node == null) {
            node = fac.createNode(nodeId, new Coord(stationX + dirX, stationY + dirY));
            node.getAttributes().putAttribute("isStation", false);
            net.addNode(node);
        } else {
            node = net.getNodes().get(nodeId);
        }
        String ptStr = link.getAllowedModes().contains("train") ? "_pt" : "";
        String firstLinkId = link.getFromNode().getId() + "-" + node.getId() + ptStr;
        Link fstLink = fac.createLink(Id.createLinkId(firstLinkId), link.getFromNode(), node);
        String secondLinkId = node.getId() + "-" + link.getToNode().getId() + ptStr;
        Link scndLink = fac.createLink(Id.createLinkId(secondLinkId), node, link.getToNode());

        copyLinkProperties(link, fstLink);
        copyLinkProperties(link, scndLink);
        fstLink.setLength(calculateDistancePeriodicBC(fstLink.getFromNode(), fstLink.getToNode(), systemSize));
        scndLink.setLength(calculateDistancePeriodicBC(scndLink.getFromNode(), scndLink.getToNode(), systemSize));
        net.addLink(fstLink);
        net.addLink(scndLink);
        net.removeLink(link.getId());
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

    private void addNewConnection(Network net, NetworkFactory fac, Node node, List<Node> neighbourNodes,
                                  boolean createTrainLanes) {
        for (String dir : directions.keySet()) {
            int[] xy_deltas = directions.get(dir);
            Node newNode = fac.createNode(Id.createNodeId(node.getId().toString() + dir),
                    new Coord(node.getCoord().getX() + xy_deltas[0], node.getCoord().getY() + xy_deltas[1]));

            Node neighbourNode = neighbourNodes.stream()
                    .filter(neigh -> Math.signum(neigh.getCoord().getX() - node.getCoord().getX())
                            == Math.signum(xy_deltas[0]) &&
                            Math.signum(neigh.getCoord().getY() - node.getCoord().getY()) == Math.signum(xy_deltas[1]))
                    .findFirst().get();
            Link neigh_newNode = fac
                    .createLink(Id.createLinkId(neighbourNode.getId() + "-" + newNode.getId()), neighbourNode,
                            newNode);
            Link newNode_neigh = fac
                    .createLink(Id.createLinkId(newNode.getId() + "-" + neighbourNode.getId()), newNode,
                            neighbourNode);
            Link newNode_node = fac
                    .createLink(Id.createLinkId(newNode.getId() + "-" + node.getId()), newNode, node);
            Link node_newNode = fac
                    .createLink(Id.createLinkId(node.getId() + "-" + newNode.getId()), node, newNode);

            setLinkAttributes(neigh_newNode, linkCapacity, linkLength, freeSpeedCar, numberOfLanes);
            setLinkAttributes(newNode_neigh, linkCapacity, linkLength, freeSpeedCar, numberOfLanes);
            setLinkAttributes(newNode_node, linkCapacity, linkLength, freeSpeedCar, numberOfLanes);
            setLinkAttributes(node_newNode, linkCapacity, linkLength, freeSpeedCar, numberOfLanes);

            net.addNode(newNode);
            net.addLink(neigh_newNode);
            net.addLink(newNode_neigh);
            net.addLink(newNode_node);
            net.addLink(node_newNode);

            if (createTrainLanes) {
                Link neigh_newNode_pt = fac
                        .createLink(Id.createLinkId(neighbourNode.getId() + "-" + newNode.getId() + "_pt"),
                                neighbourNode,
                                newNode);
                Link newNode_neigh_pt = fac
                        .createLink(Id.createLinkId(newNode.getId() + "-" + neighbourNode.getId() + "_pt"), newNode,
                                neighbourNode);
                Link newNode_node_pt = fac
                        .createLink(Id.createLinkId(newNode.getId() + "-" + node.getId() + "_pt"), newNode, node);
                Link node_newNode_pt = fac
                        .createLink(Id.createLinkId(node.getId() + "-" + newNode.getId() + "_pt"), node, newNode);
                setLinkAttributes(neigh_newNode_pt, linkCapacity, linkLength, freeSpeedTrainForSchedule,
                        numberOfLanes);
                setLinkAttributes(newNode_neigh_pt, linkCapacity, linkLength, freeSpeedTrainForSchedule,
                        numberOfLanes);
                setLinkAttributes(newNode_node_pt, linkCapacity, linkLength, freeSpeedTrainForSchedule,
                        numberOfLanes);
                setLinkAttributes(node_newNode_pt, linkCapacity, linkLength, freeSpeedTrainForSchedule,
                        numberOfLanes);
                setLinkModes(neigh_newNode_pt, "train");
                setLinkModes(newNode_neigh_pt, "train");
                setLinkModes(newNode_node_pt, "train");
                setLinkModes(node_newNode_pt, "train");
                net.addLink(neigh_newNode_pt);
                net.addLink(newNode_neigh_pt);
                net.addLink(newNode_node_pt);
                net.addLink(node_newNode_pt);
            }
        }
    }

    private void removeOrigLinks(Network net, List<Link> inLinks,
                                 List<Link> outLinks) {
        for (Id<Link> linkId : outLinks.stream().map(Identifiable::getId).collect(Collectors.toList())) {
            net.removeLink(linkId);
        }
        for (Id<Link> linkId : inLinks.stream().map(Identifiable::getId).collect(Collectors.toList())) {
            net.removeLink(linkId);
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
