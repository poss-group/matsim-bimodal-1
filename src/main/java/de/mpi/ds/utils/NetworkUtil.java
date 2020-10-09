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

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author tthunig
 */
public class NetworkUtil {

    // capacity at all links
    private static final long CAP_MAIN = 10; // [veh/h]
    // link length for all links
    private static final long LINK_LENGTH = 100; // [m]
    // link freespeed for all links
    private static final double FREE_SPEED = 7.5;
    private static final int pt_interval = 10;
    private static final double NUMBER_OF_LANES = 4.;
    private static final Map<String, int[]> directions = Map.of(
            "north", new int[]{0, 1},
            "east", new int[]{1, 0},
            "south", new int[]{0, -1},
            "west", new int[]{-1, 0}
    );
    private static int n_x = 101;
    private static double delta_x = 100;
    private static int n_y = 101;
    private static double delta_y = 100;

    private NetworkUtil() {
    }

    public static void main(String... args) {
        createGridNetwork("./output/network.xml");
    }

    public static void createGridNetwork(String path) {
        // create an empty network
        Network net = NetworkUtils.createNetwork();
        NetworkFactory fac = net.getFactory();

        // create nodes
        int n_x_new = n_y + 2 * n_x / pt_interval;
        int n_y_new = n_x + 2 * n_y / pt_interval;
        Node[][] nodes = new Node[n_x][n_y];
        Link l1 = null;
        Link l2 = null;
        for (int i = 0; i < n_y; i++) {
            for (int j = 0; j < n_x; j++) {
                Node n = fac.createNode(Id.createNodeId(String.valueOf(i) + "_" + String.valueOf(j)),
                        new Coord(i * delta_x, j * delta_y));
                nodes[i][j] = n;
                net.addNode(n);
                if (i > 0) {
                    l1 = fac.createLink(Id.createLinkId(String.valueOf(nodes[i - 1][j].getId()).concat("-")
                                    .concat(String.valueOf(nodes[i][j].getId()))),
                            nodes[i - 1][j], nodes[i][j]);
                    l2 = fac.createLink(Id.createLinkId(String.valueOf(nodes[i][j].getId()).concat("-")
                                    .concat(String.valueOf(nodes[i - 1][j].getId()))),
                            nodes[i][j], nodes[i - 1][j]);
                    setLinkAttributes(l1, CAP_MAIN, LINK_LENGTH, FREE_SPEED, NUMBER_OF_LANES);
                    setLinkAttributes(l2, CAP_MAIN, LINK_LENGTH, FREE_SPEED, NUMBER_OF_LANES);
                    net.addLink(l1);
                    net.addLink(l2);
                    if ((j + pt_interval / 2) % pt_interval == 0) {
                        setLinkModes(l1, "car, train");
                        setLinkModes(l2, "car, train");
                    } else {
                        setLinkModes(l1, "car");
                        setLinkModes(l2, "car");
                    }
                }
                if (j > 0) {
                    l1 = fac.createLink(Id.createLinkId(String.valueOf(nodes[i][j - 1].getId()).concat("-")
                            .concat(String.valueOf(nodes[i][j].getId()))), nodes[i][j - 1], nodes[i][j]);
                    l2 = fac.createLink(Id.createLinkId(String.valueOf(nodes[i][j].getId()).concat("-")
                            .concat(String.valueOf(nodes[i][j - 1].getId()))), nodes[i][j], nodes[i][j - 1]);
                    setLinkAttributes(l1, CAP_MAIN, LINK_LENGTH, FREE_SPEED, NUMBER_OF_LANES);
                    setLinkAttributes(l2, CAP_MAIN, LINK_LENGTH, FREE_SPEED, NUMBER_OF_LANES);
                    net.addLink(l1);
                    net.addLink(l2);
                    if ((i + pt_interval / 2) % pt_interval == 0) {
                        setLinkModes(l1, "car, train");
                        setLinkModes(l2, "car, train");
                    } else {
                        setLinkModes(l1, "car");
                        setLinkModes(l2, "car");
                    }
                }
            }
        }
        makeDiagConnections(net, fac, nodes);
        // this has to be done second because mod for pt modifies next neighbours of stations
        putNodesCloseToStations(net, fac, nodes);
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

    private static void makeDiagConnections(Network net, NetworkFactory fac, Node[][] nodes) {
        double diag_length = Math.sqrt(delta_x * delta_x + delta_y * delta_y);
        List<Link> newLinks = new ArrayList<>();
        for (Node[] node : nodes) {
            for (int j = 0; j < nodes[0].length; j++) {
                Node temp = node[j];
                List<Node> diagNeighbours = temp.getOutLinks().values().stream()
                        .map(Link::getToNode)
                        .flatMap(n -> n.getOutLinks().values().stream().map(Link::getToNode))
                        .filter(n -> {
                            double dist = DistanceUtils.calculateDistance(n.getCoord(), temp.getCoord());
                            return dist <= diag_length && dist > 0;
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
     * @param net   the network
     * @param fac   the network factory
     * @param nodes 2D array of Node
     */
    private static void putNodesCloseToStations(Network net, NetworkFactory fac, Node[][] nodes) {
        for (int i = 0; i < nodes.length; i++) {
            for (int j = 0; j < nodes[0].length; j++) {
                if ((i + pt_interval / 2) % pt_interval == 0 && (j + pt_interval / 2) % pt_interval == 0) {
                    Node temp = nodes[i][j];
                    List<Link> outLinks = temp.getOutLinks().values().stream()
                            .filter(l -> l.getLength() == LINK_LENGTH)
                            .collect(Collectors.toList());
                    List<Link> inLinks = temp.getInLinks().values().stream()
                            .filter(l -> l.getLength() == LINK_LENGTH)
                            .collect(Collectors.toList());
                    List<Node> neighbourNodes = outLinks.stream().map(Link::getToNode).collect(
                            Collectors.toList());
                    removeOrigLinks(net, inLinks, outLinks);
                    addNewConnection(net, fac, nodes[i][j], neighbourNodes);
                }
            }
        }
    }

    private static void addNewConnection(Network net, NetworkFactory fac, Node node, List<Node> neighbourNodes) {
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
