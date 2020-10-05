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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkWriter;

import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;

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

    private NetworkUtil() {
    }

    public static void main(String... args) {
        createGridNetwork("./output/network.xml");
    }

    public static void createGridNetwork(String path) {
        // create an empty network
        Network net = NetworkUtils.createNetwork();
        NetworkFactory fac = net.getFactory();

        int n_x = 101;
        double delta_x = 100;
        int n_y = 101;
        double delta_y = 100;
        int pt_interval = 10;

        // create nodes
        Node[][] nodes = new Node[n_y][n_x];
        Link l = null;
        for (int i = 0; i < n_y; i++) {
            for (int j = 0; j < n_x; j++) {
                Node n = fac.createNode(Id.createNodeId(i * n_y + j), new Coord(i * delta_x, j * delta_y));
                nodes[i][j] = n;
                net.addNode(n);
                if (i > 0) {
                    l = fac
                            .createLink(
                                    Id.createLinkId(String.valueOf(nodes[i - 1][j].getId()).concat("_")
                                            .concat(String.valueOf(nodes[i][j].getId()))),
                                    nodes[i - 1][j], nodes[i][j]);
                    Link l2 = fac.createLink(Id.createLinkId(String.valueOf(nodes[i][j].getId()).concat("_")
                                    .concat(String.valueOf(nodes[i - 1][j].getId()))),
                            nodes[i][j], nodes[i - 1][j]);
                    setLinkAttributes(l, CAP_MAIN, LINK_LENGTH, FREE_SPEED);
                    setLinkAttributes(l2, CAP_MAIN, LINK_LENGTH, FREE_SPEED);
                    net.addLink(l);
                    net.addLink(l2);
                    if ((j+pt_interval/2) % pt_interval == 0) {
                        setLinkModes(l, "car, train");
                        setLinkModes(l2, "car, train");
                    } else {
                        setLinkModes(l, "car");
                        setLinkModes(l2, "car");
                    }
                }
                if (j > 0) {
                    l = fac
                            .createLink(
                                    Id.createLinkId(String.valueOf(nodes[i][j - 1].
                                            getId()).concat("_")
                                            .concat(String.valueOf(nodes[i][j].getId()))),
                                    nodes[i][j - 1], nodes[i][j]);
                    Link l2 = fac
                            .createLink(
                                    Id.createLinkId(String.valueOf(nodes[i][j].getId()).concat("_")
                                            .concat(String.valueOf(nodes[i][j - 1].getId()))),
                                    nodes[i][j], nodes[i][j - 1]);
                    setLinkAttributes(l, CAP_MAIN, LINK_LENGTH, FREE_SPEED);
                    setLinkAttributes(l2, CAP_MAIN, LINK_LENGTH, FREE_SPEED);
                    net.addLink(l);
                    net.addLink(l2);
                    if ((i+pt_interval/2) % pt_interval == 0) {
                        setLinkModes(l, "car, train");
                        setLinkModes(l2, "car, train");
                    } else {
                        setLinkModes(l, "car");
                        setLinkModes(l2, "car");
                    }
                }
            }
        }
//		new NetworkCleaner().run(net);
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

    private static void setLinkAttributes(Link link, double capacity, double length, double freeSpeed) {
        link.setCapacity(capacity);
        link.setLength(length);
        link.setFreespeed(freeSpeed);
    }

    private static void setLinkModes(Link link, String modes) {
        HashSet<String> hash_Set = new HashSet<String>();
        hash_Set.add(modes);
        link.setAllowedModes(hash_Set);
    }

}
