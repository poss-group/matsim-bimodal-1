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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static de.mpi.ds.utils.ScenarioCreator.*;

public class TransitScheduleCreator implements UtilComponent {
    private static final VehicleType vehicleType = VehicleUtils.getFactory().createVehicleType(Id.create("1",
            VehicleType.class));
    private static final Logger LOG = Logger.getLogger(TransitScheduleCreator.class.getName());


    private double freeSpeedTrain;
    private double systemSize;
    private double transitEndTime;
    private int railInterval;
    private double transitStopLength;
    private double departureIntervalTime;
    private double carGridSpacing;
    private double effectiveFreeSpeedTrain;

    public TransitScheduleCreator(double systemSize, int railInterval, double freeSpeedTrain, double effectiveFreeSpeedTrain,
                                  double transitEndTime, double transitStopLength,
                                  double departureIntervalTime, double carGridSpacing) {
        this.railInterval = railInterval;
        this.systemSize = systemSize;
        this.freeSpeedTrain = freeSpeedTrain;
        this.transitEndTime = transitEndTime;
        this.transitStopLength = transitStopLength;
        this.departureIntervalTime = departureIntervalTime;
        this.carGridSpacing = carGridSpacing;
        this.effectiveFreeSpeedTrain = effectiveFreeSpeedTrain;
    }

    public static void main(String[] args) {
//        String suffix = "_15min";
//        runTransitScheduleUtil("./output/network_diag.xml", "./output/transitSchedule" + suffix + ".xml",
//                "./output/transitVehicles" + suffix + ".xml");
    }

    public void runTransitScheduleUtil(String networkPath, String outputSchedulePath, String outputVehiclesPath) {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        TransitSchedule schedule = scenario.getTransitSchedule();
        TransitScheduleFactory transitScheduleFactory = schedule.getFactory();
        PopulationFactory populationFactory = scenario.getPopulation().getFactory();
        Network net = NetworkUtils.readNetwork(networkPath);
        Vehicles vehicles = VehicleUtils.createVehiclesContainer();
//        Vehicles vehicles = createVehicles();

        createTransitSchedule(transitScheduleFactory, schedule, populationFactory, net, vehicles);

        new TransitScheduleWriter(schedule).writeFile(outputSchedulePath);
        new MatsimVehicleWriter(vehicles).writeFile(outputVehiclesPath);
//        compressGzipFile(outputSchedulePath, outputSchedulePath.concat(".gz"));
//        compressGzipFile(outputVehiclesPath, outputVehiclesPath.concat(".gz"));
//        deleteFile(outputSchedulePath);
//        deleteFile(outputVehiclesPath);
    }

    public void createTransitSchedule(TransitScheduleFactory transitScheduleFactory, TransitSchedule schedule,
                                      PopulationFactory populationFactory, Network net, Vehicles vehicles) {

        List<Node> stationNodes = net.getNodes().values().stream()
                .filter(n -> n.getAttributes().getAttribute(IS_STATION_NODE).equals(true)).collect(Collectors.toList());

        // !doubleCloseToZeroCondition for Periodic BC, otherwise two trains would share one line effectively at the
        // borders or just delete last element of each list
//        List<Link> startLinksXDir =
        List<Link> startLinksXDir = stationNodes.stream()
                .filter(n -> n.getCoord().getX() == 0)
                // Should be more general:
//                .collect(Collectors.groupingBy(n -> n.getCoord().getX(), TreeMap::new, Collectors.toList()))
//                .firstEntry().getValue().stream()
                .flatMap(n -> n.getOutLinks().values().stream())
                .filter(l -> l.getAllowedModes().contains(NETWORK_MODE_TRAIN))
                .filter(l -> l.getAttributes().getAttribute(PERIODIC_LINK).equals(false))
                .filter(l -> l.getToNode().getCoord().getX() > l.getFromNode().getCoord().getX())
                .filter(l -> l.getCoord().getY() != systemSize)
                .collect(Collectors.toList());
        List<Link> startLinksXDecDir = stationNodes.stream()
                .collect(Collectors.groupingBy(n -> n.getCoord().getX(), TreeMap::new, Collectors.toList()))
                .lastEntry().getValue().stream()
                .flatMap(n -> n.getOutLinks().values().stream())
                .filter(l -> l.getAllowedModes().contains(NETWORK_MODE_TRAIN))
                .filter(l -> l.getAttributes().getAttribute(PERIODIC_LINK).equals(false))
                .filter(l -> l.getToNode().getCoord().getX() < l.getFromNode().getCoord().getX())
                .filter(l -> l.getCoord().getY() != systemSize)
                .collect(Collectors.toList());

        List<Link> startLinksYDir = stationNodes.stream()
                .filter(n -> n.getCoord().getY() == 0)
                // Should be more general:
//                .collect(Collectors.groupingBy(n -> n.getCoord().getX(), TreeMap::new, Collectors.toList()))
//                .firstEntry().getValue().stream()
                .flatMap(n -> n.getOutLinks().values().stream())
                .filter(l -> l.getAllowedModes().contains(NETWORK_MODE_TRAIN))
                .filter(l -> l.getAttributes().getAttribute(PERIODIC_LINK).equals(false))
                .filter(l -> l.getToNode().getCoord().getY() > l.getFromNode().getCoord().getY())
                .filter(l -> l.getCoord().getX() != systemSize)
                .collect(Collectors.toList());
        List<Link> startLinksYDecDir = stationNodes.stream()
                .collect(Collectors.groupingBy(n -> n.getCoord().getY(), TreeMap::new, Collectors.toList()))
                .lastEntry().getValue().stream()
                .flatMap(n -> n.getOutLinks().values().stream())
                .filter(l -> l.getAllowedModes().contains(NETWORK_MODE_TRAIN))
                .filter(l -> l.getAttributes().getAttribute(PERIODIC_LINK).equals(false))
                .filter(l -> l.getToNode().getCoord().getY() < l.getFromNode().getCoord().getY())
                .filter(l -> l.getCoord().getX() != systemSize)
                .collect(Collectors.toList());

        TransitScheduleConstructor transitScheduleConstructor = new TransitScheduleConstructor(transitScheduleFactory,
                populationFactory, net, schedule, vehicles, railInterval*carGridSpacing / freeSpeedTrain, transitStopLength, 0,
                transitEndTime, systemSize / freeSpeedTrain, effectiveFreeSpeedTrain, departureIntervalTime, "Manhatten");

//        LOG.info(
//                "Transit time station-station: " + delta_xy * pt_interval / FREE_SPEED_TRAIN + "\nStop time @ " +
//                        "station: " + transitStopLength + "\nTransit Interval Time: " + transitIntervalTime +
//                        "\nTransit time grid start - grid end: " + (startNodesXDec.get(0).getCoord()
//                        .getX() - startNodesX
//                        .get(0).getCoord().getX()) / FREE_SPEED_TRAIN);

        LOG.info("Rail grid spacing: " + railInterval*carGridSpacing);
        LOG.info(startLinksXDir.size());
        LOG.info(startLinksXDecDir.size());
        LOG.info(startLinksYDir.size());
        LOG.info(startLinksYDecDir.size());
        for (int i = 0; i < startLinksXDir.size(); i++) {
            transitScheduleConstructor.createLine(startLinksXDir.get(i));
            transitScheduleConstructor.createLine(startLinksXDecDir.get(i));
            transitScheduleConstructor.createLine(startLinksYDir.get(i));
            transitScheduleConstructor.createLine(startLinksYDecDir.get(i));
        }
    }
}

