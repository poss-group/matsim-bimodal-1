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

    public NetworkCreator(double systemSize, int railInterval, int small_railInterval, double carGridSpacing,
                          double linkCapacity, double effectiveFreeSpeedTrain, double numberOfLanes,
                          double freeSpeedCar, boolean diagonalConnections,
                          boolean smallLinksCloseToNodes, boolean createTrainLines, TransitScheduleCreator transitScheduleCreator) {
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
        for (int i = 0; i < n_y; i++) {
            for (int j = 0; j < n_x; j++) {
                String newNodeId = i + "_" + j;
                boolean newNodeStationAttribute = false;
                boolean newNodeStationAttribute_corssing = false;
                Node n = fac.createNode(Id.createNodeId(newNodeId),
                        new Coord(i * carGridSpacing, j * carGridSpacing));
                if (createTrainLines) {
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
                }

                n.getAttributes().putAttribute(IS_STATION_NODE, newNodeStationAttribute);
                n.getAttributes().putAttribute(IS_STATION_CROSSING_NODE, newNodeStationAttribute_corssing);
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
                    insertCarLinks(net, fac, nodes[i][j], nodes[i_minus1_periodic][j], carGridSpacing, false, true);
                } else {
                    insertCarLinks(net, fac, nodes[i][j], nodes[i_minus1_periodic][j], periodicLength, true, false);
                }
                if ((j - 1) >= 0) {
                    insertCarLinks(net, fac, nodes[i][j], nodes[i][j_minus1_periodic], carGridSpacing, false, true);
                } else {
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

    private void insertTrainLinks(Network net, NetworkFactory fac, Node a, Node b, double length,
                                  boolean periodicConnection) {
        Link l3 = fac.createLink(Id.createLinkId(String.valueOf(a.getId()).concat("-")
                        .concat(String.valueOf(b.getId()).concat("_pt"))),
                a, b);
        Link l4 = fac.createLink(Id.createLinkId(String.valueOf(b.getId()).concat("-")
                        .concat(String.valueOf(a.getId()).concat("_pt"))),
                b, a);
        setLinkAttributes(l3, linkCapacity, length, effectiveFreeTrainSpeed, numberOfLanes, periodicConnection, false);
        setLinkAttributes(l4, linkCapacity, length, effectiveFreeTrainSpeed, numberOfLanes, periodicConnection, false);
        setLinkModes(l3, NETWORK_MODE_TRAIN);
        setLinkModes(l4, NETWORK_MODE_TRAIN);
        net.addLink(l3);
        net.addLink(l4);
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
    private void putNodesCloseToStations(Network net, NetworkFactory fac) {
        // For each node so that there are no possible "U turns" in simulations (facilities are actually links)
        // Not just like nodes = net.getNodes().values() because then iteration is over the reference (nodes grow in
        // every iteration
        List<Node> nodes = new ArrayList<>(net.getNodes().values());
        double east = 0;
        double north = Math.PI / 2;
        double west = Math.PI;
        double south = -Math.PI / 2;
        double[] neighbourToAddDirections = new double[]{east, north, west, south};
        for (Node n : nodes) {
            //sort lists so same direction gets picked for in and outlink
            List<Link> outLinksNonPeriodic = n.getOutLinks().values().stream()
                    .filter(l -> l.getAttributes().getAttribute(PERIODIC_LINK).equals(false))
                    .collect(Collectors.toList());
            List<Link> inLinksNonPeriodic = n.getInLinks().values().stream()
                    .filter(l -> l.getAttributes().getAttribute(PERIODIC_LINK).equals(false))
                    .collect(Collectors.toList());

            if (smallLinksCloseToNodes) {
                for (double dir : neighbourToAddDirections) {
                    List<Link> inLinksNonPt = getLinksWithOppositeDirection(inLinksNonPeriodic, dir, NETWORK_MODE_CAR);
                    List<Link> outLinksNonPt = getLinksWithDirection(outLinksNonPeriodic, dir, NETWORK_MODE_CAR);
                    assert (inLinksNonPt.size() <= 1 &&
                            outLinksNonPt.size() <= 1) : "Expected to find max one in/out link in given direction";
                    if (!inLinksNonPt.isEmpty() && !outLinksNonPt.isEmpty()) {
                        divide(net, fac, outLinksNonPt.get(0), "out", true);
                        divide(net, fac, inLinksNonPt.get(0), "in", true);
                        break;
                    }
                }
            } else {
                List<Link> inLinksNonPt = null;
                List<Link> outLinksNonPt = null;
                int i = 0;
                do {
                    double dir = neighbourToAddDirections[i];
                    inLinksNonPt = getLinksWithDirection(inLinksNonPeriodic, dir, NETWORK_MODE_CAR);
                    outLinksNonPt = getLinksWithOppositeDirection(outLinksNonPeriodic, dir, NETWORK_MODE_CAR);
                    i++;
                } while (inLinksNonPt.isEmpty() || outLinksNonPt.isEmpty());
                inLinksNonPt.get(0).getAttributes().putAttribute(IS_START_LINK, true);
                outLinksNonPt.get(0).getAttributes().putAttribute(IS_START_LINK, true);
            }
            if (n.getAttributes().getAttribute(IS_STATION_NODE).equals(true)) {
                for (double dir : neighbourToAddDirections) {
                    List<Link> inLinksPt = getLinksWithOppositeDirection(inLinksNonPeriodic, dir,
                            NETWORK_MODE_TRAIN);
                    List<Link> outLinksPt = getLinksWithDirection(outLinksNonPeriodic, dir,
                            NETWORK_MODE_TRAIN);
                    assert (inLinksPt.size() <= 1 &&
                            outLinksPt.size() <= 1) : "Expected to find max one in/out link in given direction";
                    if (!inLinksPt.isEmpty() && !outLinksPt.isEmpty()) {
                        divide(net, fac, outLinksPt.get(0), "out", false);
                        divide(net, fac, inLinksPt.get(0), "in", false);
                    }
                }
            }
        }
    }

    private List<Link> getLinksWithDirection(List<Link> links, double direction, String networkModeFilter) {
        return links.stream()
                .filter(l -> doubleCloseToZero(Math.abs(getDirectionOfLink(l) - direction) % (2 * Math.PI)))
                .filter(l -> l.getAllowedModes().contains(networkModeFilter))
                .collect(Collectors.toList());
    }

    private List<Link> getLinksWithOppositeDirection(List<Link> links, double direction, String networkModeFilter) {
        return links.stream()
                .filter(l -> doubleCloseToZero(Math.abs(getDirectionOfLink(l) - direction - Math.PI) % (2 * Math.PI)))
                .filter(l -> l.getAllowedModes().contains(networkModeFilter))
                .collect(Collectors.toList());
    }


    private void divide(Network net, NetworkFactory fac, Link link, String inOut, boolean isStartLink) {

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
            node.getAttributes().putAttribute(IS_STATION_NODE, false);
            net.addNode(node);
        } else {
            node = net.getNodes().get(nodeId);
        }
        String ptStr = link.getAllowedModes().contains(NETWORK_MODE_TRAIN) ? "_pt" : "";
        String firstLinkId = link.getFromNode().getId() + "-" + node.getId() + ptStr;
        String secondLinkId = node.getId() + "-" + link.getToNode().getId() + ptStr;
        Link fstLink = fac.createLink(Id.createLinkId(firstLinkId), link.getFromNode(), node);
        Link scndLink = fac.createLink(Id.createLinkId(secondLinkId), node, link.getToNode());
//        if (firstLinkId.equals("PT_1_1-PT_2_1west_pt") || secondLinkId.equals("PT_1_1-PT_2_1west_pt")) {
//            System.out.println("test");
//        }

        copyLinkProperties(link, fstLink);
        copyLinkProperties(link, scndLink);
        fstLink.setLength(calculateDistancePeriodicBC(fstLink.getFromNode(), fstLink.getToNode(), systemSize));
        scndLink.setLength(calculateDistancePeriodicBC(scndLink.getFromNode(), scndLink.getToNode(), systemSize));

        if (inOut.equals("in")) {
            if (isStartLink) {
                scndLink.getAttributes().putAttribute(IS_START_LINK, true);
            }
//            } else {
//                fstLink.getAttributes().putAttribute(IS_START_LINK, false);
//                scndLink.getAttributes().putAttribute(IS_START_LINK, true);
//            }
//            if (isTrainFacility) {
            Set<String> allowedModes = new HashSet<>();
            allowedModes.add(NETWORK_MODE_CAR);
            allowedModes.add(NETWORK_MODE_TRAIN);
            scndLink.setAllowedModes(allowedModes);
//            }
        }

        if (inOut.equals("out")) {
            if (isStartLink) {
                fstLink.getAttributes().putAttribute(IS_START_LINK, true);
            }
//            } else {
//                fstLink.getAttributes().putAttribute(IS_START_LINK, true);
//                scndLink.getAttributes().putAttribute(IS_START_LINK, false);
//            }
//            if (isTrainFacility) {
//            if (isTrainFacility) {
            Set<String> allowedModes = new HashSet<>();
            allowedModes.add(NETWORK_MODE_CAR);
            allowedModes.add(NETWORK_MODE_TRAIN);
            fstLink.setAllowedModes(allowedModes);
//            }
        }

        net.addLink(fstLink);
        net.addLink(scndLink);
        net.removeLink(link.getId());
    }

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

    private void makeTriGrid(Network net, NetworkFactory fac) {
        double diag_length = 0.5 * Math.sqrt(carGridSpacing * carGridSpacing + carGridSpacing * carGridSpacing);

        List<Node> nodes = new ArrayList<>(net.getNodes().values());
//        for (Node temp : net.getNodes().values()) {
        for (Node temp : nodes) {
            Coord eastCoord = new Coord(temp.getCoord().getX() + carGridSpacing, temp.getCoord().getY());
            Coord northCoord = new Coord(temp.getCoord().getX(), temp.getCoord().getY() + carGridSpacing);
            Coord northEastCoord = new Coord(temp.getCoord().getX() + carGridSpacing,
                    temp.getCoord().getY() + carGridSpacing);
            Node eastNeighbour = getClosestNeighbourNodeToCoord(temp, eastCoord);
            Node northNeighbour = getClosestNeighbourNodeToCoord(temp, northCoord);
            Node northEastNeighbour = getClosestNeighbourNodeToCoord(temp, northEastCoord);
            if (eastNeighbour == null || northNeighbour == null || northEastNeighbour == null) {
                continue;
            }
            List<Node> nodesToConnect = Arrays.asList(temp, eastNeighbour, northNeighbour, northEastNeighbour);
            Node middleNode = fac.createNode(Id.createNodeId(temp.getId().toString() + "_tri"),
                    new Coord(temp.getCoord().getX() + carGridSpacing / 2,
                            temp.getCoord().getY() + carGridSpacing / 2));
            middleNode.getAttributes().putAttribute(IS_STATION_NODE, false);
            net.addNode(middleNode);

            for (Node neighbour : nodesToConnect) {
                Link nij_ndiag = fac
                        .createLink(Id.createLinkId(middleNode.getId() + "-" + neighbour.getId()), middleNode,
                                neighbour);
                Link ndiag_nij = fac
                        .createLink(Id.createLinkId(neighbour.getId() + "-" + middleNode.getId()), neighbour,
                                middleNode);
                setLinkAttributes(nij_ndiag, linkCapacity, diag_length, freeSpeedCar, numberOfLanes, false, true);
                setLinkAttributes(ndiag_nij, linkCapacity, diag_length, freeSpeedCar, numberOfLanes, false, true);
                setLinkModes(nij_ndiag, NETWORK_MODE_CAR);
                setLinkModes(ndiag_nij, NETWORK_MODE_CAR);

                net.addLink(nij_ndiag);
                net.addLink(ndiag_nij);
            }
        }
    }

    private Node getClosestNeighbourNodeToCoord(Node node, Coord coord) {
        List<Node> nodeList = null;
        if (doubleCloseToZero(node.getCoord().getX() - coord.getX()) ||
                doubleCloseToZero(node.getCoord().getY() - coord.getY())) {
            // For direct neighbour
            nodeList = node.getOutLinks().values().stream()
                    .map(Link::getToNode)
                    .filter(n -> doubleCloseToZero(calculateDistancePeriodicBC(coord, n.getCoord(), systemSize)))
                    .collect(Collectors.toList());
        } else {
            // For diagonal neighbour
            nodeList = node.getOutLinks().values().stream()
                    .map(Link::getToNode)
                    .flatMap(n -> n.getOutLinks().values().stream().map(Link::getToNode))
                    .filter(n -> doubleCloseToZero(calculateDistancePeriodicBC(coord, n.getCoord(), systemSize)))
                    .distinct()
                    .collect(Collectors.toList());
        }
        assert nodeList.size() <= 1 : "Number of neighbours satisfying constraint should be max one";
        if (nodeList.size() == 1) {
            return nodeList.get(0);
        } else {
            return null;
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
