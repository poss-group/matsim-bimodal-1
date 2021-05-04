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
import org.matsim.api.core.v01.TransportMode;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static de.mpi.ds.utils.ScenarioCreator.*;

public class TransitScheduleCreatorRadCirc implements UtilComponent {
    private static final VehicleType vehicleType = VehicleUtils.getFactory().createVehicleType(Id.create("1",
            VehicleType.class));
    private static final Logger LOG = Logger.getLogger(TransitScheduleCreatorRadCirc.class.getName());


    private double freeSpeedTrain;
    private double systemSize;
    private double transitEndTime;
    private int railInterval;
    private double transitStopLength;
    private double departureIntervalTime;
    private double carGridSpacing;

    public TransitScheduleCreatorRadCirc(double systemSize, int railInterval, double freeSpeedTrain,
                                         double transitEndTime, double transitStopLength,
                                         double departureIntervalTime, double carGridSpacing) {
        this.railInterval = railInterval;
        this.systemSize = systemSize;
        this.freeSpeedTrain = freeSpeedTrain;
        this.transitEndTime = transitEndTime;
        this.transitStopLength = transitStopLength;
        this.departureIntervalTime = departureIntervalTime;
        this.carGridSpacing = carGridSpacing;
    }

    public static void main(String[] args) {
        String suffix = "_15min";

        new TransitScheduleCreatorRadCirc(10000, 5, 60 / 3.6,
                24 * 3600, 0, 15 * 60, 100).
                runTransitScheduleUtil("./output/network_circ_rad.xml.gz", "./output/transitSchedule.xml.gz",
                        "./output/transitVehicles.xml.gz");
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

        Node centerNode = net.getNodes().values().stream()
                .min(Comparator.comparingDouble(n -> (Math.abs(n.getCoord().getX()) + Math.abs(n.getCoord().getY()))))
                .get();
        List<Link> startLinks = centerNode.getOutLinks().values().stream()
                .filter(l -> l.getAllowedModes().contains(TransportMode.train)).collect(Collectors.toList());

        TransitScheduleConstructor transitScheduleConstructor = new TransitScheduleConstructor(transitScheduleFactory,
                populationFactory, net, schedule, vehicles, railInterval * carGridSpacing / freeSpeedTrain,
                transitStopLength, 0, transitEndTime, systemSize / freeSpeedTrain, freeSpeedTrain,
                2*60, "RadCirc");

        for (int i = 0; i < startLinks.size(); i++) {
            transitScheduleConstructor.createLine(startLinks.get(i));
        }
    }
}

