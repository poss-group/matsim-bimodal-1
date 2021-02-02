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

import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static de.mpi.ds.utils.CreateScenarioElements.compressGzipFile;
import static de.mpi.ds.utils.CreateScenarioElements.deleteFile;

public class TransitScheduleCreator implements UtilComponent {
    private static final VehicleType vehicleType = VehicleUtils.getFactory().createVehicleType(Id.create("1",
            VehicleType.class));
    private static final Logger LOG = Logger.getLogger(TransitScheduleCreator.class.getName());

    public static void main(String[] args) {
        String suffix = "_15min";
        runTransitScheduleUtil("./output/network_diag.xml", "./output/transitSchedule" + suffix + ".xml",
                "./output/transitVehicles" + suffix + ".xml");
    }

    static void runTransitScheduleUtil(String networkPath, String outputSchedulePath,
                                       String outputVehiclesPath) {
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
        compressGzipFile(outputSchedulePath, outputSchedulePath.concat(".gz"));
        compressGzipFile(outputVehiclesPath, outputVehiclesPath.concat(".gz"));
        deleteFile(outputSchedulePath);
        deleteFile(outputVehiclesPath);
    }

    public static void createTransitSchedule(TransitScheduleFactory transitScheduleFactory, TransitSchedule schedule,
                                             PopulationFactory populationFactory, Network net, Vehicles vehicles) {

        List<Node> stationNodes = net.getNodes().values().stream()
                .filter(n -> n.getAttributes().getAttribute("isStation").equals(true)).collect(Collectors.toList());

        List<Node> startNodesXDir = stationNodes.stream()
                .filter(n -> n.getCoord().getX() == 0)
                .sorted(Comparator.comparingDouble(n -> n.getCoord().getY())).collect(Collectors.toList());
        List<Node> startNodesXDecDir = (List<Node>) stationNodes.stream()
                .collect(Collectors.groupingBy(n -> n.getCoord().getX(), TreeMap::new, Collectors.toList()))
                .lastEntry().getValue().stream()
                .sorted(Comparator.comparingDouble(n -> n.getCoord().getY())).collect(Collectors.toList());

        List<Node> startNodesYDir = stationNodes.stream()
                .filter(n -> n.getCoord().getY() == 0)
                .sorted(Comparator.comparingDouble(n -> n.getCoord().getX())).collect(Collectors.toList());
        List<Node> startNodesYDecDir = stationNodes.stream()
                .collect(Collectors.groupingBy(n -> n.getCoord().getY(), TreeMap::new, Collectors.toList()))
                .lastEntry().getValue().stream()
                .sorted(Comparator.comparingDouble(n -> n.getCoord().getX())).collect(Collectors.toList());

        LOG.info(transitIntervalTime);
        TransitScheduleConstructor transitScheduleConstructor = new TransitScheduleConstructor(transitScheduleFactory,
                populationFactory, net, schedule, vehicles, ptInterval, cellLength/ptInterval,
                cellLength / freeSpeedTrain,
                transitStopLength, 0,
                transitEndTime, cellLength*gridLengthInCells/freeSpeedTrain);

//        LOG.info(
//                "Transit time station-station: " + delta_xy * pt_interval / FREE_SPEED_TRAIN + "\nStop time @ " +
//                        "station: " + transitStopLength + "\nTransit Interval Time: " + transitIntervalTime +
//                        "\nTransit time grid start - grid end: " + (startNodesXDec.get(0).getCoord()
//                        .getX() - startNodesX
//                        .get(0).getCoord().getX()) / FREE_SPEED_TRAIN);

        for (int i = 0; i < startNodesXDir.size(); i++) {
            transitScheduleConstructor.createLine(startNodesXDir.get(i), startNodesXDecDir.get(i), "x");
            transitScheduleConstructor.createLine(startNodesXDecDir.get(i), startNodesXDir.get(i), "x");
        }
        for (int i = 0; i < startNodesYDir.size(); i++) {
            transitScheduleConstructor.createLine(startNodesYDir.get(i), startNodesYDecDir.get(i), "y");
            transitScheduleConstructor.createLine(startNodesYDecDir.get(i), startNodesYDir.get(i), "y");
        }
        schedule = transitScheduleConstructor.getSchedule();
        vehicles = transitScheduleConstructor.getVehicles();
    }
}

