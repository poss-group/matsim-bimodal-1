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
package de.mpi.ds.osm_utils;

import de.mpi.ds.utils.UtilComponent;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import de.mpi.ds.polygon_utils.*;
import triangulation.DelaunayTriangulator;
import triangulation.NotEnoughPointsException;
import triangulation.Triangle2D;
import triangulation.Vector2D;

import static de.mpi.ds.utils.GeneralUtils.calculateDistanceNonPeriodic;
import static java.lang.Double.NaN;

/**
 * @author tthunig
 */
public class NetworkCreatorFromOsm implements UtilComponent {
    private final static Logger LOG = Logger.getLogger(NetworkCreatorFromOsm.class.getName());

    private double linkCapacity;
    private double effectiveFreeTrainSpeed;
    private double numberOfLanes;
    private double freeSpeedCar;

    public NetworkCreatorFromOsm(double linkCapacity, double effectiveFreeTrainSpeed, double numberOfLanes,
                                 double freeSpeedCar) {
        this.linkCapacity = linkCapacity;
        this.effectiveFreeTrainSpeed = effectiveFreeTrainSpeed;
        this.numberOfLanes = numberOfLanes;
        this.freeSpeedCar = freeSpeedCar;
    }

    public static void main(String... args) {
        String path = "/home/helge/Applications/matsim/matsim-bimodal.git/master/scenarios/Manhatten/network.xml";
        NetworkCreatorFromOsm networkCreatorFromOsm = new NetworkCreatorFromOsm(9999999, 60 / 3.6, 100, 30.6);
        networkCreatorFromOsm.addTramNet(path);
    }

    public void testAlphaShapeCreation(String path) {
        Network net = NetworkUtils.readNetwork(path);
        NetworkFactory fac = net.getFactory();
        Network newNet = NetworkUtils.createNetwork();

        double minX = net.getNodes().values().stream().mapToDouble(n -> n.getCoord().getX()).min().getAsDouble();
        double maxX = net.getNodes().values().stream().mapToDouble(n -> n.getCoord().getX()).max().getAsDouble();
        double minY = net.getNodes().values().stream().mapToDouble(n -> n.getCoord().getY()).min().getAsDouble();
        double maxY = net.getNodes().values().stream().mapToDouble(n -> n.getCoord().getY()).max().getAsDouble();
        double lX = maxX - minX;
        double lY = maxY - minY;

        for (Node node : net.getNodes().values()) {
            node.setCoord(new Coord(node.getCoord().getX() - minX, node.getCoord().getY() - minY));
        }

        List<Coord> hull = new AlphaShape(
                net.getNodes().values().stream().map(n -> new Vector2D(n.getCoord().getX(), n.getCoord().getY()))
                        .collect(Collectors.toList()), 1000).compute();

        int i = 0;
        Node lastNode = null;
        for (Coord coord : hull) {
            Node node = fac.createNode(Id.createNodeId(i), coord);
            newNet.addNode(node);
            if (lastNode != null) {
                Link link = fac.createLink(Id.createLinkId(lastNode.getId().toString() + "-" + node.getId().toString()), lastNode, node);
                newNet.addLink(link);
            }
            lastNode = node;
            i++;
        }

        try {
            String[] filenameArray = path.split("/");
            File outFile = new File(path.replace(filenameArray[filenameArray.length - 1], "network_trams.xml"));
            Files.createDirectories(outFile.getParentFile().toPath());
            NetworkUtils.writeNetwork(newNet, outFile.getAbsolutePath());
        } catch (Exception e) {
            System.out.println("Failed to write output...");
            e.printStackTrace();
        }
    }

    public void addTramNet(String path) {
        Network net = NetworkUtils.readNetwork(path);
        Network newNet = NetworkUtils.createNetwork();
        NetworkFactory fac = net.getFactory();

        double minX = net.getNodes().values().stream().mapToDouble(n -> n.getCoord().getX()).min().getAsDouble();
        double maxX = net.getNodes().values().stream().mapToDouble(n -> n.getCoord().getX()).max().getAsDouble();
        double minY = net.getNodes().values().stream().mapToDouble(n -> n.getCoord().getY()).min().getAsDouble();
        double maxY = net.getNodes().values().stream().mapToDouble(n -> n.getCoord().getY()).max().getAsDouble();
        double lX = maxX - minX;
        double lY = maxY - minY;

        for (Node node : net.getNodes().values()) {
            node.setCoord(new Coord(node.getCoord().getX() - minX, node.getCoord().getY() - minY));
        }

        for (Link link : net.getLinks().values()) {
            setLinkAttributes(link, linkCapacity, freeSpeedCar, numberOfLanes, false);
        }

//        ArrayList<Coord> hull = new QuickHull().quickHull(
//                new ArrayList<>(net.getNodes().values().stream().map(Node::getCoord).collect(Collectors.toList())));
        ArrayList<Coord> hull = new AlphaShape(
                net.getNodes().values().stream().map(n -> new Vector2D(n.getCoord().getX(), n.getCoord().getY()))
                        .collect(Collectors.toList()), 999).compute();

        addCoordsToNet(newNet, hull, "hull", true);

        try {
            List<Vector2D> pointSet = net.getNodes().values().stream().map(n -> new Vector2D(n.getCoord().getX(), n.getCoord().getY())).collect(
                    Collectors.toList());
            DelaunayTriangulator delaunyTriangulator = new DelaunayTriangulator(pointSet);
            delaunyTriangulator.triangulate();

            List<Triangle2D> triangles = delaunyTriangulator.getTriangles();
        } catch (NotEnoughPointsException e) {
            e.printStackTrace();
        }

        double deltaPtX = 1000;
        double deltaPtY = 1000;
        int nX = (int) (lX / deltaPtX) + 1;
        int nY = (int) (lY / deltaPtY) + 1;

        Node[][] transitNodes = new Node[nX][nY];
        for (int i = 0; i * deltaPtX < lX; i++) {
            for (int j = 0; j * deltaPtY < lY; j++) {
                double x = i * deltaPtX;
                double y = j * deltaPtY;
                Coord transitCoord = new Coord(x, y);
                if (RayCasting.contains(hull, transitCoord)) {
                    Node transitNode = fac.createNode(Id.createNodeId("pt_" + i + "_" + j), transitCoord);
//                    Node transitNode = net.getNodes().values().stream().min(Comparator
//                            .comparingDouble(n -> calculateDistanceNonPeriodic(n.getCoord(), transitCoord))).get();
                    if (!containedInMatrix(transitNode, transitNodes)) {
                        newNet.addNode(transitNode);
                        transitNodes[i][j] = transitNode;
                    }
                }
            }
        }

        // Links in x directions
        for (int i = 0; i < transitNodes.length - 1; i++) {
            for (int j = 0; j < transitNodes[0].length; j++) {
                Node from = transitNodes[i][j];
                if (from == null) {
                    continue;
                }
                Node toX = findNextNonNull(transitNodes, i, j, "i");
                if (toX == null) {
                    continue;
                }

                Link linkX = fac
                        .createLink(Id.createLinkId("pt_" + from.getId().toString() + "-" + toX.getId().toString()),
                                from, toX);
                Link linkX_r = fac
                        .createLink(Id.createLinkId("pt_" + toX.getId().toString() + "-" + from.getId().toString()),
                                toX, from);

                double length = calculateDistanceNonPeriodic(from, toX);
                setLinkAttributes(linkX, linkCapacity, length, effectiveFreeTrainSpeed, numberOfLanes, true);
                setLinkAttributes(linkX_r, linkCapacity, length, effectiveFreeTrainSpeed, numberOfLanes, true);
                newNet.addLink(linkX);
                newNet.addLink(linkX_r);
            }
        }

        // Links in y direction
        for (int i = 0; i < transitNodes.length; i++) {
            for (int j = 0; j < transitNodes[0].length - 1; j++) {
                Node from = transitNodes[i][j];
                if (from == null) {
                    continue;
                }
                Node toY = findNextNonNull(transitNodes, i, j, "j");
                if (toY == null) {
                    continue;
                }

                if (from != null && toY != null) {
                    Link linkY = fac
                            .createLink(Id.createLinkId("pt_" + from.getId().toString() + "-" + toY.getId().toString()),
                                    from, toY);
                    Link linkY_r = fac
                            .createLink(Id.createLinkId("pt_" + toY.getId().toString() + "-" + from.getId().toString()),
                                    toY, from);

                    double length = calculateDistanceNonPeriodic(from, toY);
                    setLinkAttributes(linkY, linkCapacity, length, effectiveFreeTrainSpeed, numberOfLanes, true);
                    setLinkAttributes(linkY_r, linkCapacity, length, effectiveFreeTrainSpeed, numberOfLanes, true);
                    newNet.addLink(linkY);
                    newNet.addLink(linkY_r);
                }
            }
        }

        try {
            String[] filenameArray = path.split("/");
            File outFile = new File(path.replace(filenameArray[filenameArray.length - 1], "network_trams.xml"));
            // create output folder if necessary
            Files.createDirectories(outFile.getParentFile().toPath());
            // write network
            NetworkUtils.writeNetwork(newNet, outFile.getAbsolutePath());
        } catch (Exception e) {
            System.out.println("Failed to write output...");
            e.printStackTrace();
        }
    }

    private void addCoordsToNet(Network net, ArrayList<Coord> hull, String nodePrefix, boolean addLinks) {
        int i = 0;
        Node lastNode = null;
        for (Coord c : hull) {
            Node n = net.getFactory().createNode(Id.createNodeId(nodePrefix + i), c);
            net.addNode(n);
            if (lastNode != null && addLinks) {
                Link link = net.getFactory()
                        .createLink(Id.createLinkId(lastNode.getId().toString() + "-" + lastNode.getId().toString()),
                                lastNode, n);
                net.addLink(link);
            }
            i++;
            lastNode = n;
        }
    }

    private Node findNextNonNull(Node[][] transitNodes, int i, int j, String direction) {
        if (direction.equals("i")) {
            Node next = transitNodes[i + 1][j];
            while (next == null && i + 1 < transitNodes.length - 1) {
                i++;
                next = transitNodes[i + 1][j];
            }
            return next;
        } else if (direction.equals("j")) {
            Node next = transitNodes[i][j + 1];
            while (next == null && j + 1 < transitNodes[0].length - 1) {
                j++;
                next = transitNodes[i][j + 1];
            }
            return next;
        } else {
            return null;
        }
    }

    private boolean containedInMatrix(Node transitNode, Node[][] transitNodes) {
        boolean result = false;
        for (int ii = 0; ii < transitNodes.length; ii++) {
            for (int jj = 0; jj < transitNodes[0].length; jj++) {
                if (transitNode.equals(transitNodes[ii][jj])) {
                    result = true;
                }
            }
        }
        return result;
    }

    public void test(Network net) {
        Network newNet = NetworkUtils.createNetwork();
        NetworkFactory factory = newNet.getFactory();
        ArrayList<Coord> hull = new QuickHull().quickHull(
                new ArrayList<>(net.getNodes().values().stream().map(Node::getCoord).collect(Collectors.toList())));
        Node[] nodes = new Node[hull.size()];
        int i = 0;
        Node node = null;
        for (Coord coord : hull) {
            Id<Node> id = Id.createNodeId(i);
            node = factory.createNode(id, coord);
            nodes[i] = node;
            i++;
            newNet.addNode(node);
        }
        for (i = 0; i < nodes.length; i++) {
            Id<Link> linkId = Id.createLinkId(i + "" + i + 1);
            Link link = factory.createLink(linkId, nodes[i], nodes[(i + 1) % nodes.length]);
            newNet.addLink(link);
        }
        RayCasting.contains(hull, new Coord(0, 0));

        try {
            File outFile = new File("output/test.xml");
            // create output folder if necessary
            Files.createDirectories(outFile.getParentFile().toPath());
            // write network
            NetworkUtils.writeNetwork(newNet, outFile.getAbsolutePath());
        } catch (Exception e) {
            System.out.println("Failed to write output...");
            e.printStackTrace();
        }
    }

    private void setLinkAttributes(Link link, double capacity, double length, double freeSpeed, double numberLanes,
                                   boolean trainLink) {
        setLinkAttributes(link, capacity, freeSpeed, numberLanes, trainLink);
        link.setLength(length);
    }

    private void setLinkAttributes(Link link, double capacity, double freeSpeed, double numberLanes,
                                   boolean trainLink) {
        link.setCapacity(capacity);
        link.setFreespeed(freeSpeed);
        link.setNumberOfLanes(numberLanes);
        Set<String> modes = new HashSet<>();
        if (trainLink) {
            modes.add(TransportMode.pt);
        } else {
            modes.add(TransportMode.car);
        }
        link.setAllowedModes(modes);
    }
}
