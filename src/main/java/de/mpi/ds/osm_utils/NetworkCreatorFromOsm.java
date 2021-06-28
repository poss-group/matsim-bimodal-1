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
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import de.mpi.ds.polygon_utils.*;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.*;
import triangulation.*;

import static de.mpi.ds.osm_utils.ScenarioCreatorOsm.*;
import static de.mpi.ds.utils.GeneralUtils.calculateDistanceNonPeriodic;

/**
 * @author tthunig
 */
public class NetworkCreatorFromOsm implements UtilComponent {
    private final static Logger LOG = Logger.getLogger(NetworkCreatorFromOsm.class.getName());

    private double linkCapacity;
    private double freeSpeedTrain;
    private double numberOfLanes;
    private double freeSpeedCar;

    private ArrayList<Coord> hull;

    private double transitStartTime;
    private double transitEndTime;
    private double departureIntervalTime;
    private int route_counter;
    private double ptSpacing;

    public NetworkCreatorFromOsm(ArrayList<Coord> hull, double linkCapacity,
                                 double freeSpeedTrain, double numberOfLanes,
                                 double freeSpeedCar, double transitStartTime, double transitEndTime,
                                 double departureIntervalTime, double ptSpacing) {
        this.hull = hull;
        this.linkCapacity = linkCapacity;
        this.freeSpeedTrain = freeSpeedTrain;
        this.numberOfLanes = numberOfLanes;
        this.freeSpeedCar = freeSpeedCar;
        this.transitStartTime = transitStartTime;
        this.transitEndTime = transitEndTime;
        this.departureIntervalTime = departureIntervalTime;
        this.route_counter = 0;
        this.ptSpacing = ptSpacing;
    }

    public static void main(String... args) {
//        String path = "/home/helge/Applications/matsim/matsim-bimodal.git/master/scenarios/Manhatten/network_clean.xml";
//        ArrayList<Coord> hull = new AlphaShape(path, 0.1).compute();
//        NetworkCreatorFromOsm networkCreatorFromOsm = new NetworkCreatorFromOsm(hull, 9999999, 60 / 3.6, 100, 30.6, 0,
//                10 * 3600, 15 * 3600);
//        networkCreatorFromOsm.addTramNet(path, path);
    }

    public void testAlphaShapeCreation(String path) {
        Network net = NetworkUtils.readNetwork(path);
        NetworkFactory fac = net.getFactory();
        Network newNet = NetworkUtils.createNetwork();

        double minX = net.getNodes().values().stream().mapToDouble(n -> n.getCoord().getX()).min().getAsDouble();
        double maxX = net.getNodes().values().stream().mapToDouble(n -> n.getCoord().getX()).max().getAsDouble();
        double minY = net.getNodes().values().stream().mapToDouble(n -> n.getCoord().getY()).min().getAsDouble();
        double maxY = net.getNodes().values().stream().mapToDouble(n -> n.getCoord().getY()).max().getAsDouble();

        for (Node node : net.getNodes().values()) {
            node.setCoord(new Coord(node.getCoord().getX() - minX, node.getCoord().getY() - minY));
        }

        addCoordsToNet(newNet, hull, "hull", true);
//        ArrayList<Edge2D> hull = new AlphaShape(
//                net.getNodes().values().stream().map(n -> new Vector2D(n.getCoord().getX(), n.getCoord().getY()))
//                        .collect(Collectors.toList()), 1.0).compute();
//
//        addEdgesToNet(newNet, hull, "hull", false);

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

    private void addEdgesToNet(Network newNet, ArrayList<Edge2D> edges, String hull1, boolean b) {
        int i = 0;
        for (Edge2D edge : edges) {
            Node n1 = newNet.getFactory().createNode(Id.createNodeId(i + "a"), new Coord(edge.a.x, edge.a.y));
            Node n2 = newNet.getFactory().createNode(Id.createNodeId(i + "b"), new Coord(edge.b.x, edge.b.y));
            Link l = newNet.getFactory()
                    .createLink(Id.createLinkId(n1.getId().toString() + "-" + n2.getId().toString()), n1, n2);
            newNet.addNode(n1);
            newNet.addNode(n2);
            newNet.addLink(l);
            i++;
        }
    }

    public Network addTramNet(Network net, String networkOutPath, String transitScheduleOutPath,
                              String transitVehiclesOutPath) {
        NetworkFactory fac = net.getFactory();

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        TransitSchedule schedule = scenario.getTransitSchedule();
        TransitScheduleFactory transitScheduleFactory = schedule.getFactory();
        PopulationFactory populationFactory = scenario.getPopulation().getFactory();
        Vehicles vehicles = createVehicles();

        double minX = net.getNodes().values().stream().mapToDouble(n -> n.getCoord().getX()).min().getAsDouble();
        double maxX = net.getNodes().values().stream().mapToDouble(n -> n.getCoord().getX()).max().getAsDouble();
        double minY = net.getNodes().values().stream().mapToDouble(n -> n.getCoord().getY()).min().getAsDouble();
        double maxY = net.getNodes().values().stream().mapToDouble(n -> n.getCoord().getY()).max().getAsDouble();
        double lX = maxX - minX;
        double lY = maxY - minY;

//        addCoordsToNet(net, hull, "hull", true);

        try {
            List<Vector2D> pointSet = net.getNodes().values().stream()
                    .map(n -> new Vector2D(n.getCoord().getX(), n.getCoord().getY())).collect(
                            Collectors.toList());
            DelaunayTriangulator delaunyTriangulator = new DelaunayTriangulator(pointSet);
            delaunyTriangulator.triangulate();

            List<Triangle2D> triangles = delaunyTriangulator.getTriangles();
        } catch (NotEnoughPointsException e) {
            e.printStackTrace();
        }

        double deltaPtX = ptSpacing;
        double deltaPtY = ptSpacing;
        int nX = (int) (lX / deltaPtX) + 1;
        int nY = (int) (lY / deltaPtY) + 1;

        Node[][] transitNodes = new Node[nX][nY];
        for (int i = 0; i * deltaPtX < lX; i++) {
            for (int j = 0; j * deltaPtY < lY; j++) {
                double x = i * deltaPtX;
                double y = j * deltaPtY;
                Coord transitCoord = new Coord(x, y);
                if (RayCasting.contains(hull, transitCoord)) {
//                    Node transitNode = fac.createNode(Id.createNodeId("pt_" + i + "_" + j), transitCoord);
                    Node transitNode = net.getNodes().values().stream().min(Comparator
                            .comparingDouble(n -> calculateDistanceNonPeriodic(n.getCoord(), transitCoord))).get();
                    transitNode.getAttributes().putAttribute(IS_STATION_NODE, true);
                    if (!containedInMatrix(transitNode, transitNodes)) {
//                        net.addNode(transitNode);
                        transitNodes[i][j] = transitNode;
                    }
                }
            }
        }

        // Links in y directions
        for (int i = 0; i < transitNodes.length; i++) {
            ArrayList<Id<Link>> transitLinks = new ArrayList<>();
            ArrayList<Id<Link>> transitLinksReverse = new ArrayList<>();
            List<TransitRouteStop> transitRouteStops = new ArrayList<>();
            List<TransitRouteStop> transitRouteStopsReverse = new ArrayList<>();
            Link lastAddedLink = null;
            for (int j = 0; j < transitNodes[0].length - 1; j++) {
                Node from = transitNodes[i][j];
                if (from == null) {
                    continue;
                }
                Node toX = findNextNonNull(transitNodes, i, j, "j");
                if (toX == null) {
                    break;
                }


                Link linkY = fac
                        .createLink(Id.createLinkId("pt_" + from.getId().toString() + "-" + toX.getId().toString()),
                                from, toX);

                Link linkY_r = fac
                        .createLink(Id.createLinkId("pt_" + toX.getId().toString() + "-" + from.getId().toString()),
                                toX, from);


                if (transitLinks.size() == 0) {
                    addTransitStop(linkY_r, schedule, transitScheduleFactory, transitRouteStops);
                    transitLinks.add(linkY_r.getId());
                }
                addTransitStop(linkY, schedule, transitScheduleFactory, transitRouteStops);
                addTransitStop(linkY_r, schedule, transitScheduleFactory, transitRouteStopsReverse);
                transitLinks.add(linkY.getId());
                transitLinksReverse.add(linkY_r.getId());
                lastAddedLink = linkY;

                double length = calculateDistanceNonPeriodic(from, toX);
                setLinkAttributes(linkY, linkCapacity, length, freeSpeedTrain, numberOfLanes, true);
                setLinkAttributes(linkY_r, linkCapacity, length, freeSpeedTrain, numberOfLanes, true);
                net.addLink(linkY);
                net.addLink(linkY_r);
            }
            if (transitLinks.size() > 0) {
                addTransitStop(lastAddedLink, schedule,
                        transitScheduleFactory, transitRouteStopsReverse);
                transitLinksReverse.add(lastAddedLink.getId());

                TransitLine transitLine = transitScheduleFactory
                        .createTransitLine(Id.create("Line".concat(String.valueOf(route_counter)), TransitLine.class));
                addTransitLineToSchedule(transitLinks, transitRouteStops, vehicles, transitLine, populationFactory,
                        transitScheduleFactory, schedule);

                TransitLine transitLineReverse = transitScheduleFactory
                        .createTransitLine(Id.create("Line".concat(String.valueOf(route_counter)), TransitLine.class));
                Collections.reverse(transitLinksReverse);
                Collections.reverse(transitRouteStopsReverse);
                addTransitLineToSchedule(transitLinksReverse, transitRouteStopsReverse, vehicles, transitLineReverse,
                        populationFactory, transitScheduleFactory, schedule);
            }
        }

        // Links in x direction
        for (int j = 0; j < transitNodes[0].length; j++) {
            ArrayList<Id<Link>> transitLinks = new ArrayList<>();
            ArrayList<Id<Link>> transitLinksReverse = new ArrayList<>();
            List<TransitRouteStop> transitRouteStops = new ArrayList<>();
            List<TransitRouteStop> transitRouteStopsReverse = new ArrayList<>();
            Link lastAddedLink = null;
            for (int i = 0; i < transitNodes.length - 1; i++) {
                Node from = transitNodes[i][j];
                if (from == null) {
                    continue;
                }
                Node toY = findNextNonNull(transitNodes, i, j, "i");
                if (toY == null) {
                    break;
                }

                Link linkX = fac
                        .createLink(Id.createLinkId("pt_" + from.getId().toString() + "-" + toY.getId().toString()),
                                from, toY);
                Link linkX_r = fac
                        .createLink(Id.createLinkId("pt_" + toY.getId().toString() + "-" + from.getId().toString()),
                                toY, from);

                if (transitLinks.size() == 0) {
                    addTransitStop(linkX_r, schedule, transitScheduleFactory, transitRouteStops);
                    transitLinks.add(linkX_r.getId());
                }
                addTransitStop(linkX, schedule, transitScheduleFactory, transitRouteStops);
                addTransitStop(linkX_r, schedule, transitScheduleFactory, transitRouteStopsReverse);
                transitLinks.add(linkX.getId());
                transitLinksReverse.add(linkX_r.getId());
                lastAddedLink = linkX;

                double length = calculateDistanceNonPeriodic(from, toY);
                setLinkAttributes(linkX, linkCapacity, length, freeSpeedTrain, numberOfLanes, true);
                setLinkAttributes(linkX_r, linkCapacity, length, freeSpeedTrain, numberOfLanes, true);
                net.addLink(linkX);
                net.addLink(linkX_r);
            }
            if (transitLinks.size() > 0) {
                addTransitStop(lastAddedLink, schedule,
                        transitScheduleFactory, transitRouteStopsReverse);
                transitLinksReverse.add(lastAddedLink.getId());

                TransitLine transitLine = transitScheduleFactory
                        .createTransitLine(Id.create("Line".concat(String.valueOf(route_counter)), TransitLine.class));
                addTransitLineToSchedule(transitLinks, transitRouteStops, vehicles, transitLine, populationFactory,
                        transitScheduleFactory, schedule);

                TransitLine transitLineReverse = transitScheduleFactory
                        .createTransitLine(Id.create("Line".concat(String.valueOf(route_counter)), TransitLine.class));
                Collections.reverse(transitLinksReverse);
                Collections.reverse(transitRouteStopsReverse);
                addTransitLineToSchedule(transitLinksReverse, transitRouteStopsReverse, vehicles, transitLineReverse,
                        populationFactory,
                        transitScheduleFactory, schedule);
            }
        }


        try {
//            String[] filenameArray = path.split("/");
//            File outFile = new File(path.replace(filenameArray[filenameArray.length - 1], "network_trams.xml"));
//            File outTransitSchedule = new File(
//                    path.replace(filenameArray[filenameArray.length - 1], "transitSchedule.xml"));
//            File outVehicles = new File(path.replace(filenameArray[filenameArray.length - 1], "transitVehicles.xml"));
            // create output folder if necessary
            File outFile = new File(networkOutPath);
            Files.createDirectories(outFile.getParentFile().toPath());
            // write network
            NetworkUtils.writeNetwork(net, outFile.getAbsolutePath());
            new TransitScheduleWriter(schedule).writeFile(transitScheduleOutPath);
            new MatsimVehicleWriter(vehicles).writeFile(transitVehiclesOutPath);
        } catch (Exception e) {
            System.out.println("Failed to write output...");
            e.printStackTrace();
        }

        return net;
    }


    private void addTransitLineToSchedule(ArrayList<Id<Link>> idLinkList, List<TransitRouteStop> transitRouteStopList,
                                          Vehicles vehicles, TransitLine transitLine,
                                          PopulationFactory populationFactory,
                                          TransitScheduleFactory transitScheduleFactory, TransitSchedule schedule) {
        Id<TransitRoute> tr_id = Id.create(String.valueOf(route_counter), TransitRoute.class);
        NetworkRoute networkRoute = populationFactory.getRouteFactories()
                .createRoute(NetworkRoute.class, idLinkList.get(0),
                        idLinkList.get(idLinkList.size() - 1));
        networkRoute.setLinkIds(idLinkList.get(0), idLinkList.subList(1, idLinkList.size() - 1),
                idLinkList.get(idLinkList.size() - 1));
        TransitRoute transitRoute = transitScheduleFactory.createTransitRoute(tr_id, networkRoute,
                transitRouteStopList, "train");
        createDepartures(transitRoute, transitScheduleFactory, vehicles);
        transitLine.addRoute(transitRoute);

        schedule.addTransitLine(transitLine);
        route_counter++;
    }

    private void addTransitStop(Link link, TransitSchedule schedule,
                                TransitScheduleFactory transitScheduleFactory,
                                List<TransitRouteStop> transitRouteStopList) {
        Id<TransitStopFacility> transitStopFacilityId = null;
        TransitStopFacility transitStopFacility = null;
        transitStopFacilityId = Id
                .create(link.getId() + "_trStop", TransitStopFacility.class);
        transitStopFacility = schedule.getFacilities().get(transitStopFacilityId);
        if (transitStopFacility == null) {
            transitStopFacility = transitScheduleFactory
                    .createTransitStopFacility(transitStopFacilityId, link.getToNode().getCoord(), false);
            transitStopFacility.setLinkId(link.getId());
            schedule.addStopFacility(transitStopFacility);
        }
        TransitRouteStop transitRouteStop = transitScheduleFactory
                .createTransitRouteStop(transitStopFacility, 0, 0);
        transitRouteStopList.add(transitRouteStop);
    }

    private void createDepartures(TransitRoute transitRoute, TransitScheduleFactory transitScheduleFactory,
                                  Vehicles vehicles) {
        double time = transitStartTime;
        int i = 0;
        while (time < transitEndTime) {
            Departure dep = transitScheduleFactory.createDeparture(Id.create(String.valueOf(i), Departure.class), time);
            Id<Vehicle> vehicleId =
                    Id.create("tr_".concat(String.valueOf(route_counter).concat("_").concat(String.valueOf(i))),
                            Vehicle.class);
            dep.setVehicleId(vehicleId);
            if (!vehicles.getVehicles().containsKey(vehicleId)) {
                vehicles.addVehicle(VehicleUtils.createVehicle(Id.create(vehicleId, Vehicle.class),
                        vehicles.getVehicleTypes().entrySet().iterator().next().getValue()));
            }
            transitRoute.addDeparture(dep);
            i++;

            time += departureIntervalTime;
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

    public Vehicles createVehicles() {
        VehicleType vehicleType = VehicleUtils.getFactory().createVehicleType(Id.create("1", VehicleType.class));
        vehicleType.setDescription("train");
        vehicleType.setNetworkMode("train");
        vehicleType.setMaximumVelocity(freeSpeedTrain);
        vehicleType.setLength(10);
        vehicleType.getCapacity().setSeats(10000);
        vehicleType.getCapacity().setStandingRoom(0);
        Vehicles result = VehicleUtils.createVehiclesContainer();
        result.addVehicleType(vehicleType);
        return result;
    }

    public static void setLinkAttributes(Link link, double capacity, double length, double freeSpeed,
                                         double numberLanes,
                                         boolean trainLink) {
        setLinkAttributes(link, capacity, freeSpeed, numberLanes, trainLink);
        link.setLength(length);
    }

    public static void setLinkAttributes(Link link, double capacity, double freeSpeed, double numberLanes,
                                         boolean trainLink) {
        link.setCapacity(capacity);
//        link.setFreespeed(freeSpeed);
        link.setNumberOfLanes(numberLanes);
        Set<String> modes = new HashSet<>();
        if (trainLink) {
            modes.add(TransportMode.train);
            link.setFreespeed(freeSpeed);
            link.getAttributes().putAttribute(IS_START_LINK, false);
        } else {
            modes.add(TransportMode.car);
            if (link.getFreespeed() < 9) {
                link.getAttributes().putAttribute(IS_START_LINK, true);
            } else {
                link.getAttributes().putAttribute(IS_START_LINK, false);
            }
        }
        link.setAllowedModes(modes);
    }
}
