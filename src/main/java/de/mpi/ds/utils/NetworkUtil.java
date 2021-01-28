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
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static de.mpi.ds.utils.CreateScenarioElements.compressGzipFile;
import static de.mpi.ds.utils.CreateScenarioElements.deleteFile;
import static de.mpi.ds.utils.GeneralUtils.calculateDistancePeriodicBC;

/**
 * @author tthunig
 */
public class NetworkUtil implements UtilComponent {


    private static final Map<String, int[]> directions = Map.of(
            "north", new int[]{0, 1},
            "east", new int[]{1, 0},
            "south", new int[]{0, -1},
            "west", new int[]{-1, 0}
    );


    private NetworkUtil() {
    }

    public static void main(String... args) {
        String path = "./output/network_diag.xml.gz";
        createGridNetwork(path, true);
    }

    public static void createGridNetwork(String path, boolean createTrainLanes) {
        // create an empty network
        //TODO name pt stations indep. of other nodes/links so transit schedule can work for all scenarios
        Network net = NetworkUtils.createNetwork();
        NetworkFactory fac = net.getFactory();

        // create nodes
        // Add nodes to network
        int n_x = pt_interval * gridLengthInCells;
        int n_y = pt_interval * gridLengthInCells;
        int n_station_x = 0;
        int n_station_y = 0;
        Node[][] nodes = new Node[n_x][n_y];
        for (int i = 0; i < n_y; i++) {
            for (int j = 0; j < n_x; j++) {
                String newNodeId = i + "_" + j;
                boolean newStationAtrribute = false;
                if (createTrainLanes) {
                    if (((i + pt_interval / 2) % pt_interval == 0) && ((j + pt_interval / 2) % pt_interval == 0)) {
                        newNodeId = "PT_" + i/pt_interval + "_" + j/pt_interval;
                        newStationAtrribute = true;
                    }
                }

                Node n = fac.createNode(Id.createNodeId(newNodeId),
                        new Coord(i * cellLength / pt_interval, j * cellLength / pt_interval));
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
                int i_minusPtInterval_periodic = (((i - pt_interval) % n_y) + n_y) % n_y;
                int j_minusPtInterval_periodic = (((j - pt_interval) % n_x) + n_x) % n_x;
                insertCarLinks(net, fac, nodes[i][j], nodes[i_minus1_periodic][j]);
                insertCarLinks(net, fac, nodes[i][j], nodes[i][j_minus1_periodic]);
                if (((i + pt_interval / 2) % pt_interval == 0) &&
                        ((j + pt_interval / 2) % pt_interval == 0) && createTrainLanes) {
                    insertTrainLinks(net, fac, nodes[i][j], nodes[i_minusPtInterval_periodic][j]);
                    insertTrainLinks(net, fac, nodes[i][j], nodes[i][j_minusPtInterval_periodic]);
                }

                // PT connections btw. all nodes
//                if ((j + pt_interval / 2) % pt_interval == 0 && createTrainLanes) {
//                    insertTrainLinks(net, fac, nodes[i][j], nodes[i_minusPtInterval_periodic][j]);
//                }
//                if ((i + pt_interval / 2) % pt_interval == 0 && createTrainLanes) {
//                    insertTrainLinks(net, fac, nodes[i][j], nodes[i][j_minusPtInterval_periodic]);
//                }
            }
        }
        makeDiagConnections(net, fac, nodes);
        // this has to be done second because diagonal connections where also introduced before
        putNodesCloseToStations(net, fac, nodes, createTrainLanes);
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

    private static void insertTrainLinks(Network net, NetworkFactory fac, Node a, Node b) {
        Link l3 = fac.createLink(Id.createLinkId(String.valueOf(a.getId()).concat("-")
                        .concat(String.valueOf(b.getId()).concat("_pt"))),
                a, b);
        Link l4 = fac.createLink(Id.createLinkId(String.valueOf(b.getId()).concat("-")
                        .concat(String.valueOf(a.getId()).concat("_pt"))),
                b, a);
        setLinkAttributes(l3, CAP_MAIN, cellLength, FREE_SPEED_TRAIN_FOR_SCHEDULE, NUMBER_OF_LANES);
        setLinkAttributes(l4, CAP_MAIN, cellLength, FREE_SPEED_TRAIN_FOR_SCHEDULE, NUMBER_OF_LANES);
        setLinkModes(l3, "train");
        setLinkModes(l4, "train");
        net.addLink(l3);
        net.addLink(l4);
    }

    private static void insertCarLinks(Network net, NetworkFactory fac, Node a, Node b) {
        Link l1 = fac.createLink(Id.createLinkId(String.valueOf(a.getId()).concat("-")
                        .concat(String.valueOf(b.getId()))),
                a, b);
        Link l2 = fac.createLink(Id.createLinkId(String.valueOf(b.getId()).concat("-")
                        .concat(String.valueOf(a.getId()))),
                b, a);
        setLinkAttributes(l1, CAP_MAIN, LINK_LENGTH, FREE_SPEED, NUMBER_OF_LANES);
        setLinkAttributes(l2, CAP_MAIN, LINK_LENGTH, FREE_SPEED, NUMBER_OF_LANES);
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

    private static void makeDiagConnections(Network net, NetworkFactory fac, Node[][] nodes) {
        double diag_length = Math.sqrt(cellLength / pt_interval * cellLength / pt_interval +
                cellLength / pt_interval * cellLength / pt_interval);
        double side_length = cellLength / pt_interval;
        List<Link> newLinks = new ArrayList<>();
        for (Node[] node : nodes) {
            for (int j = 0; j < nodes[0].length; j++) {
                Node temp = node[j];
                List<Node> diagNeighbours = temp.getOutLinks().values().stream()
                        .map(Link::getToNode)
                        .flatMap(n -> n.getOutLinks().values().stream().map(Link::getToNode))
                        .filter(n -> {
                            double dist = DistanceUtils.calculateDistance(n.getCoord(), temp.getCoord());
                            return dist <= diag_length && dist > side_length;
                        })
                        .distinct()
                        .collect(Collectors.toList());
                for (Node ndiag : diagNeighbours) {
                    // Only consider one direction because the other one is done when iterating over neighbour node
                    Link nij_ndiag = fac.createLink(Id.createLinkId(temp.getId() + "-" + ndiag.getId()), temp, ndiag);
                    setLinkAttributes(nij_ndiag, CAP_MAIN, diag_length, FREE_SPEED, NUMBER_OF_LANES);
                    setLinkModes(nij_ndiag, "car");

                    newLinks.add(nij_ndiag);
                }
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
     * @param nodes            2D array of Node
     * @param createTrainLanes
     */
    private static void putNodesCloseToStations(Network net, NetworkFactory fac, Node[][] nodes,
                                                boolean createTrainLanes) {
        for (int i = 0; i < nodes.length; i++) {
            for (int j = 0; j < nodes[0].length; j++) {
                if ((i + pt_interval / 2) % pt_interval == 0 && (j + pt_interval / 2) % pt_interval == 0) {
                    Node temp = nodes[i][j];
                    for (Link link : temp.getInLinks().values()) {
                        divide(net, fac, link, "in", createTrainLanes);
                    }
                    for (Link link : temp.getOutLinks().values()) {
                        divide(net, fac, link, "out", createTrainLanes);
                    }
//                    List<Link> outLinks = temp.getOutLinks().values().stream()
//                            .filter(l -> l.getLength() == LINK_LENGTH)
//                            .collect(Collectors.toList());
//                    List<Link> inLinks = temp.getInLinks().values().stream()
//                            .filter(l -> l.getLength() == LINK_LENGTH)
//                            .collect(Collectors.toList());
//                    List<Node> neighbourNodes = outLinks.stream().map(Link::getToNode).collect(
//                            Collectors.toList());
//                    removeOrigLinks(net, inLinks, outLinks);
//                    addNewConnection(net, fac, nodes[i][j], neighbourNodes, createTrainLanes);
                }
            }
        }
    }

    private static void divide(Network net, NetworkFactory fac, Link link, String inOut,
                               boolean createTrainLanes) {
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

        // Following to cases because of periodic BC
        if (Math.abs(neighY - stationY) > cellLength) {
            dirY *= -1;
        }
        if (Math.abs(neighX - stationX) > cellLength) {
            dirX *= -1;
        }

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
        String bla = link.getFromNode().getId() + "-" + node.getId() + ptStr;
        Link fstLink = fac.createLink(Id.createLinkId(bla),
                link.getFromNode(), node);
        String blub = node.getId() + "-" + link.getToNode().getId() + ptStr;
        Link scndLink = fac.createLink(Id.createLinkId(blub), node,
                link.getToNode());

        copyLinkProperties(link, fstLink);
        copyLinkProperties(link, scndLink);
        fstLink.setLength(calculateDistancePeriodicBC(fstLink.getFromNode(), fstLink.getToNode(), cellLength*gridLengthInCells));
        scndLink.setLength(calculateDistancePeriodicBC(scndLink.getFromNode(), scndLink.getToNode(), cellLength*gridLengthInCells));
        net.addLink(fstLink);
        net.addLink(scndLink);
        net.removeLink(link.getId());
    }

    private static void copyLinkProperties(Link link, Link toCopyLink) {
        toCopyLink.setCapacity(link.getCapacity());
        toCopyLink.setAllowedModes(link.getAllowedModes());
        toCopyLink.setFreespeed(link.getFreespeed());
        toCopyLink.setNumberOfLanes(link.getNumberOfLanes());
    }

    private static void addNewConnection(Network net, NetworkFactory fac, Node node, List<Node> neighbourNodes,
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

            setLinkAttributes(neigh_newNode, CAP_MAIN, LINK_LENGTH, FREE_SPEED, NUMBER_OF_LANES);
            setLinkAttributes(newNode_neigh, CAP_MAIN, LINK_LENGTH, FREE_SPEED, NUMBER_OF_LANES);
            setLinkAttributes(newNode_node, CAP_MAIN, LINK_LENGTH, FREE_SPEED, NUMBER_OF_LANES);
            setLinkAttributes(node_newNode, CAP_MAIN, LINK_LENGTH, FREE_SPEED, NUMBER_OF_LANES);

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
                setLinkAttributes(neigh_newNode_pt, CAP_MAIN, LINK_LENGTH, FREE_SPEED_TRAIN_FOR_SCHEDULE,
                        NUMBER_OF_LANES);
                setLinkAttributes(newNode_neigh_pt, CAP_MAIN, LINK_LENGTH, FREE_SPEED_TRAIN_FOR_SCHEDULE,
                        NUMBER_OF_LANES);
                setLinkAttributes(newNode_node_pt, CAP_MAIN, LINK_LENGTH, FREE_SPEED_TRAIN_FOR_SCHEDULE,
                        NUMBER_OF_LANES);
                setLinkAttributes(node_newNode_pt, CAP_MAIN, LINK_LENGTH, FREE_SPEED_TRAIN_FOR_SCHEDULE,
                        NUMBER_OF_LANES);
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

    private static void removeOrigLinks(Network net, List<Link> inLinks,
                                        List<Link> outLinks) {
        for (Id<Link> linkId : outLinks.stream().map(Identifiable::getId).collect(Collectors.toList())) {
            net.removeLink(linkId);
        }
        for (Id<Link> linkId : inLinks.stream().map(Identifiable::getId).collect(Collectors.toList())) {
            net.removeLink(linkId);
        }
    }

    private static void setLinkAttributes(Link link, double capacity, double length, double freeSpeed,
                                          double numberLanes) {
        link.setCapacity(capacity);
        link.setLength(length);
        link.setFreespeed(freeSpeed);
        link.setNumberOfLanes(numberLanes);
    }

    private static void setLinkModes(Link link, String modes) {
        HashSet<String> hash_Set = new HashSet<String>();
        hash_Set.add(modes);
        link.setAllowedModes(hash_Set);
    }

}
