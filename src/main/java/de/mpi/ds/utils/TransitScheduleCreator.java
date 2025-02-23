/* *********************************************************************** *
 * project: org.matsim.*
 * RunEmissionToolOffline.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package de.mpi.ds.utils;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.*;

import java.util.ArrayList;
import java.util.List;

import static de.mpi.ds.utils.GeneralUtils.calculateDistancePeriodicBC;
import static de.mpi.ds.utils.GeneralUtils.doubleCloseToZero;
import static de.mpi.ds.utils.NetworkCreator.setLinkAttributes;
import static de.mpi.ds.utils.NetworkCreator.setLinkModes;
import static de.mpi.ds.utils.ScenarioCreator.*;

public class TransitScheduleCreator implements UtilComponent {
    private static final VehicleType vehicleType = VehicleUtils.getFactory().createVehicleType(Id.create("1",
            VehicleType.class));
    private static final Logger LOG = Logger.getLogger(TransitScheduleCreator.class.getName());


    private double freeSpeedTrain;
    private double systemSize;
    private double transitStartTime;
    private double transitEndTime;
    private int railInterval;
    private int small_railInterval;
    private double transitStopLength;
    private double departureIntervalTime;
    private double carGridSpacing;
    private double linkCapacity;
    private double numberOfLanes;
    private int route_counter;
    private boolean periodic_network;

    public TransitScheduleCreator(double systemSize, int railInterval, int small_railInterval, double freeSpeedTrain,
                                  double transitStartTime, double transitEndTime, double transitStopLength,
                                  double departureIntervalTime, double carGridSpacing, double linkCapacity,
                                  double numberOfLanes, boolean periodic_network) {
        this.railInterval = railInterval;
        this.small_railInterval = small_railInterval;
        this.systemSize = systemSize;
        this.freeSpeedTrain = freeSpeedTrain;
        this.transitStartTime = transitStartTime;
        this.transitEndTime = transitEndTime;
        this.transitStopLength = transitStopLength;
        this.departureIntervalTime = departureIntervalTime;
        this.carGridSpacing = carGridSpacing;
        this.linkCapacity = linkCapacity;
        this.numberOfLanes = numberOfLanes;
        this.route_counter = 0;
        this.periodic_network = periodic_network;
    }

    public static void main(String[] args) {
//        String suffix = "_15min";
//        runTransitScheduleUtil("./output/network_diag.xml", "./output/transitSchedule" + suffix + ".xml",
//                "./output/transitVehicles" + suffix + ".xml");
    }

    public void createPtLinksVehiclesSchedule(Network net, Node[][] networkNodes, String transitScheduleOutPath,
                                              String transitVehiclesOutPath) {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        TransitSchedule schedule = scenario.getTransitSchedule();
        Vehicles vehicles = createVehicles();

        int n_dummy = (int) (small_railInterval / carGridSpacing);

        generateLinesInYDir(networkNodes, scenario, schedule, net, vehicles);
        Node[][] inverted_networkNodes = switchYDirMatrix(networkNodes);
        generateLinesInYDir(inverted_networkNodes, scenario, schedule, net, vehicles);
        Node[][] transposed_networkNodes = transposeMatrix(networkNodes);
        generateLinesInYDir(transposed_networkNodes, scenario, schedule, net, vehicles);
        Node[][] inverted_transposed_networkNodes = switchYDirMatrix(transposed_networkNodes);
        generateLinesInYDir(inverted_transposed_networkNodes, scenario, schedule, net, vehicles);

        try {
            new TransitScheduleWriter(schedule).writeFile(transitScheduleOutPath);
            new MatsimVehicleWriter(vehicles).writeFile(transitVehiclesOutPath);
        } catch (Exception e) {
            System.out.println("Failed to write output...");
            e.printStackTrace();
        }
    }


    private void generateLinesInYDir(Node[][] networkNodes, Scenario scenario,
                                     TransitSchedule schedule, Network net, Vehicles vehicles) {
        NetworkFactory fac = net.getFactory();
        TransitScheduleFactory transitScheduleFactory = schedule.getFactory();
        PopulationFactory populationFactory = scenario.getPopulation().getFactory();
        int nX = networkNodes.length; // for 11 nodes, nx = 11
        int nY = networkNodes[0].length;

        if (periodic_network) {
            for (int i = 0; i < nX - 1; i = i + railInterval) {
                ArrayList<Id<Link>> transitLinks = new ArrayList<>();
                List<TransitRouteStop> transitRouteStops = new ArrayList<>();
                for (int j = 0; j < nY - small_railInterval; j = j + small_railInterval) {
                    Node from = networkNodes[i][j];
                    Node toY = networkNodes[i][(j + small_railInterval) % (nY)];
                    LOG.info("from:" + from + " to: " + toY);
                    Link linkY = getOrCreateLink(from, toY, net);
                    if (transitLinks.size() == 0) {
                        Link linkY_r = getOrCreateLink(toY, from, net);
                        addTransitStop(linkY_r, schedule, transitScheduleFactory, transitRouteStops, false);
                        transitLinks.add(linkY_r.getId());
                    }
                    addTransitStop(linkY, schedule, transitScheduleFactory, transitRouteStops, false);
                    transitLinks.add(linkY.getId());
                }
                if (transitLinks.size() > 0) {
                    TransitLine transitLine = transitScheduleFactory.createTransitLine(Id.create("Line".concat(String.valueOf(route_counter)), TransitLine.class));
                    addTransitLineToSchedule(transitLinks, transitRouteStops, vehicles, transitLine, populationFactory,
                            transitScheduleFactory, schedule);
                }
            }
        } else if (!periodic_network) {
            if (small_railInterval<railInterval) {
                for (int i = railInterval / 2; i < nX - 1; i = i + railInterval) {
                    ArrayList<Id<Link>> transitLinks = new ArrayList<>();
                    List<TransitRouteStop> transitRouteStops = new ArrayList<>();
                    for (int j = 0; j < nY - small_railInterval; j = j + small_railInterval) {
                        Node from = networkNodes[i][j];
                        Node toY = networkNodes[i][(j + small_railInterval)];
                        LOG.info("from:" + from + " to: " + toY);
                        Link linkY = getOrCreateLink(from, toY, net);
                        if (transitLinks.size() == 0) {
                            Link linkY_r = getOrCreateLink(toY, from, net);
                            addTransitStop(linkY_r, schedule, transitScheduleFactory, transitRouteStops, false);
                            transitLinks.add(linkY_r.getId());
                        }
                        addTransitStop(linkY, schedule, transitScheduleFactory, transitRouteStops, false);
                        transitLinks.add(linkY.getId());
                    }
                    if (transitLinks.size() > 0) {
                        TransitLine transitLine = transitScheduleFactory.createTransitLine(Id.create("Line".concat(String.valueOf(route_counter)), TransitLine.class));
                        addTransitLineToSchedule(transitLinks, transitRouteStops, vehicles, transitLine, populationFactory, transitScheduleFactory, schedule);
                    }
                }
            } else if(small_railInterval == railInterval){
                for (int i = railInterval / 2; i < nX - 1; i = i + railInterval) {
                    ArrayList<Id<Link>> transitLinks = new ArrayList<>();
                    List<TransitRouteStop> transitRouteStops = new ArrayList<>();
                    for (int j = railInterval / 2; j < nY - railInterval; j = j + railInterval) {
                        Node from = networkNodes[i][j];
                        Node toY = networkNodes[i][j + railInterval];
                        LOG.info("from:" + from + " to: " + toY);
                        Link linkY = getOrCreateLink(from, toY, net);
                        if (transitLinks.size() == 0) {
                            Link linkY_r = getOrCreateLink(toY, from, net);
                            addTransitStop(linkY_r, schedule, transitScheduleFactory, transitRouteStops, false);
                            transitLinks.add(linkY_r.getId());
                        }
                        addTransitStop(linkY, schedule, transitScheduleFactory, transitRouteStops, false);
                        transitLinks.add(linkY.getId());
                    }
                    if (transitLinks.size() > 0) {
                        TransitLine transitLine = transitScheduleFactory.createTransitLine(Id.create("Line".concat(String.valueOf(route_counter)), TransitLine.class));
                        addTransitLineToSchedule(transitLinks, transitRouteStops, vehicles, transitLine, populationFactory, transitScheduleFactory, schedule);
                    }
                }
            }
        }
    }


    private Link getOrCreateLink(Node from, Node toY, Network net) {
        Id<Link> linkYId =  Id.createLinkId("pt_" + from.getId().toString() + "-" + toY.getId().toString());
        Link linkY = net.getLinks().get(linkYId);
        if (linkY == null) {
            linkY = net.getFactory().createLink(linkYId, from, toY);
            double length = calculateDistancePeriodicBC(from, toY, systemSize);
            setLinkAttributes(linkY, linkCapacity, length, freeSpeedTrain, numberOfLanes, doubleCloseToZero(length),
                    false);
            setLinkModes(linkY, NETWORK_MODE_TRAIN);
            net.addLink(linkY);
        }
        return linkY;
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
                                List<TransitRouteStop> transitRouteStopList, boolean atStart) {
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
        if (atStart) {
            transitRouteStopList.add(0, transitRouteStop);
        } else {
            transitRouteStopList.add(transitRouteStop);
        }
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

    private Node findNextNonNull(Node[][] transitNodes, int i, int j, String direction) {
        if (direction.equals("i")) {
            Node next = transitNodes[i + 1][j];
            while (next == null) {
                i = (i + 1) % transitNodes.length;
                next = transitNodes[i + 1][j];
            }
            return next;
        } else if (direction.equals("j")) {
            Node next = transitNodes[i][j + 1];
            while (next == null && j + 1 < transitNodes[0].length - 1) {
                j = (j + 1) % transitNodes[0].length;
                next = transitNodes[i][j + 1];
            }
            return next;
        } else {
            return null;
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

    public static Node[][] transposeMatrix(Node[][] m) {
        Node[][] temp = new Node[m[0].length][m.length];
        for (int i = 0; i < m.length; i++)
            for (int j = 0; j < m[0].length; j++)
                temp[j][i] = m[i][j];
        return temp;
    }

    public static Node[][] switchYDirMatrix(Node[][] m) {
        int nx = m.length;
        int ny = m[0].length;
        Node[][] temp = new Node[nx][ny];
        for (int i = 0; i < nx; i++)
            for (int j = 0; j < ny; j++)
                temp[i][ny-j-1] = m[i][j];
        return temp;
    }

//    public static Node[][] switchYDirMatrix(Node[][] m, int n_dummy) {
//        int nx = m.length;
//        int ny = m[0].length;
//        Node[][] temp = new Node[nx][ny];
//        for (int i = 0; i < nx; i++) {
//            for (int j = 0; j <ny-n_dummy; j++) {
//                temp[i][j] = m[i][ny - n_dummy - 1 - j];
//            }
//            for (int j = ny - n_dummy; j < ny; j++) {
//                temp[i][j] = m[i][j];
//            }
//        }
//        return temp;
//    }
}