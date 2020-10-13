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
import java.util.stream.Collectors;

public class TransitScheduleUtil {
    private static final int pt_interval = 10;
    private static final double delta_x = 100;
    private static final double delta_y = 100;
    private static final double FREE_SPEED_TRAIN = 70 / 3.6;
    private static final int n_xy = 101;
    private static final VehicleType vehicleType = VehicleUtils.getFactory().createVehicleType(Id.create("1",
            VehicleType.class));
    private static final Logger LOG = Logger.getLogger(TransitScheduleUtil.class.getName());

    public static void main(String[] args) {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        TransitSchedule schedule = scenario.getTransitSchedule();
        TransitScheduleFactory transitScheduleFactory = schedule.getFactory();
        PopulationFactory populationFactory = scenario.getPopulation().getFactory();
        Network net = NetworkUtils.readNetwork("./output/network.xml");
        Vehicles vehicles = VehicleUtils.createVehiclesContainer();
//        Vehicles vehicles = createVehicles();

        createTransitSchedule(transitScheduleFactory, schedule, populationFactory, net, vehicles);

        new TransitScheduleWriter(schedule).writeFile("./output/transitSchedule.xml");
        new MatsimVehicleWriter(vehicles).writeFile("./output/transitVehicles.xml");

    }

    public static void createTransitSchedule(TransitScheduleFactory transitScheduleFactory, TransitSchedule schedule,
                                             PopulationFactory populationFactory, Network net, Vehicles vehicles) {
        int firstStation = pt_interval / 2;
//        Map<Id<Link>, ? extends Link> test = net.getNodes().values().iterator().next().getOutLinks();
        List<Node> startNodesX = net.getNodes().values().stream()
                .filter(n -> (n.getCoord().getY() / delta_y + firstStation) % pt_interval == 0 && n.getCoord()
                        .getX() / delta_x == firstStation).sorted(
                        Comparator.comparingDouble(n -> n.getCoord().getX())).collect(Collectors.toList());
        List<Node> startNodesXDec = net.getNodes().values().stream()
                .filter(n -> (n.getCoord().getY() / delta_y + firstStation) % pt_interval == 0 && n.getCoord()
                        .getX() / delta_x == (n_xy - 1) - firstStation).sorted(
                        Comparator.comparingDouble(n -> n.getCoord().getX())).collect(Collectors.toList());
        List<Node> startNodesY = net.getNodes().values().stream()
                .filter(n -> (n.getCoord().getX() / delta_x + firstStation) % pt_interval == 0 && n.getCoord()
                        .getY() / delta_y == firstStation).sorted(
                        Comparator.comparingDouble(n -> n.getCoord().getY())).collect(Collectors.toList());
        List<Node> startNodesYDec = net.getNodes().values().stream()
                .filter(n -> (n.getCoord().getX() / delta_x + firstStation) % pt_interval == 0 && n.getCoord()
                        .getY() / delta_y == (n_xy - 1) - firstStation).sorted(
                        Comparator.comparingDouble(n -> n.getCoord().getY())).collect(Collectors.toList());

        TransitScheduleConstructor transitScheduleConstructor = new TransitScheduleConstructor(transitScheduleFactory,
                populationFactory, net, schedule, vehicles, pt_interval, delta_x, delta_x * pt_interval / FREE_SPEED_TRAIN + 30,
                30, 0,
                24 * 3600, 15 * 60);

        for (int i = 0; i < startNodesX.size(); i++) {
            transitScheduleConstructor.createLine(startNodesX.get(i), startNodesXDec.get(i), "x");
            transitScheduleConstructor.createLine(startNodesXDec.get(i), startNodesX.get(i), "x");
        }
        for (int i = 0; i < startNodesY.size(); i++) {
            transitScheduleConstructor.createLine(startNodesY.get(i), startNodesYDec.get(i), "y");
            transitScheduleConstructor.createLine(startNodesYDec.get(i), startNodesY.get(i), "y");
        }
        schedule = transitScheduleConstructor.getSchedule();
        vehicles = transitScheduleConstructor.getVehicles();
    }
}

