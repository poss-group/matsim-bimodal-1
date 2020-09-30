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
package de.mpi.ds.matsim_bimodal.utils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.*;

import java.util.ArrayList;
import java.util.List;

public class TransitScheduleUtil { // TODO: Make trams start from all sides
    private static final int train_interval = 10;
    private static final double delta_x = 100;
    private static final double delta_y = 100;
    private static final VehicleType vehicleType = VehicleUtils.getFactory().createVehicleType(Id.create("1",
            VehicleType.class));
    private static final Logger LOG = Logger.getLogger(TransitScheduleUtil.class.getName());

    public static void main(String[] args) {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        TransitSchedule schedule = scenario.getTransitSchedule();
        TransitScheduleFactory transitScheduleFactory = schedule.getFactory();
        PopulationFactory populationFactory = scenario.getPopulation().getFactory();
        Network net = NetworkUtils.readNetwork("./output/network.xml");
        Vehicles vehicles = createVehicles();

        createTransitSchedule(transitScheduleFactory, schedule, populationFactory, net, vehicles);

        new TransitScheduleWriter(schedule).writeFile("./output/transitSchedule.xml");
        new MatsimVehicleWriter(vehicles).writeFile("./output/transitVehicles.xml");

    }

    private static Vehicles createVehicles() {
        Vehicles vehicles = VehicleUtils.createVehiclesContainer();
        vehicleType.setDescription("Train");
        vehicleType.setLength(50);
        vehicleType.getCapacity().setSeats(10);
        vehicleType.getCapacity().setStandingRoom(10);
//        vehicleType.setMaximumVelocity(10);
        vehicles.addVehicleType(vehicleType);
        return vehicles;
    }

    public static void createTransitSchedule(TransitScheduleFactory transitScheduleFactory, TransitSchedule schedule,
                                             PopulationFactory populationFactory, Network net, Vehicles vehicles) {
        int[] counter = {0, 0}; //{#routes, #stops}
        createLines(transitScheduleFactory, net, schedule, populationFactory, true, counter, vehicles, false);
        createLines(transitScheduleFactory, net, schedule, populationFactory, true, counter, vehicles, true);
        createLines(transitScheduleFactory, net, schedule, populationFactory, false, counter, vehicles, false);
        createLines(transitScheduleFactory, net, schedule, populationFactory, false, counter, vehicles, true);
    }

    private static void createLines(TransitScheduleFactory transitScheduleFactory, Network net,
                                    TransitSchedule schedule,
                                    PopulationFactory populationFactory, boolean vertical, int[] counter,
                                    Vehicles vehicles, boolean reverse) {
        int route_counter = counter[0];
        int stop_counter = counter[1];
        int net_size = net.getNodes().values().size();
        int n_x = (int) Math.sqrt(net_size);
        int n_y = n_x;
        for (int i = train_interval / 2; i < n_x; i += train_interval) {
            TransitLine transitLine =
                    transitScheduleFactory.createTransitLine(Id.create("Line".concat(String.valueOf(route_counter)),
                            TransitLine.class));
            List<TransitRouteStop> transitRouteStopList = new ArrayList<>();
            List<Id<Link>> id_link_list = new ArrayList<>();
            double departureDelay = 30;
            for (int k = 0; k < (n_y - 1) * 2; k++) { // 2*n_y - 2 instead of 2*n_y - 1 because always placing the
                // next stop ahead
                int j = !reverse ? -Math.abs(k - (n_y - 1)) + (n_y - 1) :
                        (n_y - 1) + Math.abs(k - (n_y - 1)) - (n_y - 1);
                boolean forward = !reverse ? k < (n_y - 1) : k >= (n_y - 1); // forward true for counting 0..9;
                // forward false for 10..1
                TransitStopFacility transitStopFacility = null;
                transitStopFacility = createTransitStop(stop_counter, id_link_list, transitScheduleFactory, i, j,
                        forward, vertical, n_x, n_y);

                TransitRouteStop transitrouteStop = null;
                if (k == 0) {
                    transitrouteStop = transitScheduleFactory.createTransitRouteStop(transitStopFacility,
                            0, 0);
                } else if (k == (n_y - 2)) {
                    transitrouteStop = transitScheduleFactory.createTransitRouteStop(transitStopFacility,
                            k * (departureDelay - 10), k * (departureDelay - 10));
                } else {
                    transitrouteStop = transitScheduleFactory.createTransitRouteStop(transitStopFacility,
                            k * (departureDelay - 10), k * (departureDelay));
                }
                transitrouteStop.setAwaitDepartureTime(true);
                transitRouteStopList.add(transitrouteStop);
                schedule.addStopFacility(transitStopFacility);

                stop_counter++;
            }
            TransitRoute transitRoute = giveTransitRoute(route_counter, id_link_list, populationFactory,
                    transitScheduleFactory, transitRouteStopList, n_x, n_y, vehicles, departureDelay);
            transitLine.addRoute(transitRoute);

            schedule.addTransitLine(transitLine);
            route_counter++;//= 2;
        }
        counter[0] = route_counter;
        counter[1] = stop_counter;
    }

    private static TransitRoute giveTransitRoute(int route_counter, List<Id<Link>> id_link_list,
                                                 PopulationFactory populationFactory,
                                                 TransitScheduleFactory transitScheduleFactory,
                                                 List<TransitRouteStop> transitRouteStopList, int n_x, int n_y,
                                                 Vehicles vehicles, double departureDelay) {
        Id<TransitRoute> r_id = Id.create(String.valueOf(route_counter), TransitRoute.class);
        NetworkRoute networkRoute = createNetworkRoute(id_link_list, populationFactory);
        TransitRoute transitRoute = transitScheduleFactory.createTransitRoute(r_id, networkRoute,
                transitRouteStopList, "train");
        createDepartures(transitRoute, transitScheduleFactory, route_counter, 0, 24 * 60 * 60,
                30 * 60, n_x, n_y, vehicles, departureDelay);
        return transitRoute;
    }


    private static TransitStopFacility createTransitStop(int stop_counter, List<Id<Link>> id_link_list,
                                                         TransitScheduleFactory transitScheduleFactory, int i, int j,
                                                         boolean forward, boolean vertical, int n_x, int n_y) {
        int target;
        Id<TransitStopFacility> tsf_id = Id.create(String.valueOf(stop_counter), TransitStopFacility.class);
        TransitStopFacility transitStopFacility = null;
        String link_id;

        if (vertical) {
            if (forward) {
                target = i * n_x + j + 1;
            } else {
                target = i * n_x + j - 1;
            }
//            if (!reverse) {
//                link_id = String.valueOf(i * n_x + j).concat("_").concat(String.valueOf(target));
//            } else {
//                link_id = String.valueOf(target).concat("_").concat(String.valueOf(i * n_x + j));
//            }
            link_id = String.valueOf(i * n_x + j).concat("_").concat(String.valueOf(target));
            transitStopFacility = transitScheduleFactory.createTransitStopFacility(tsf_id,
                    new Coord(i * delta_x, j * delta_y), false);
        } else {
            if (forward) {
                target = (j + 1) * n_y + i;
            } else {
                target = (j - 1) * n_y + i;
            }
//            if (!reverse) {
//                link_id = String.valueOf(j * n_y + i).concat("_").concat(String.valueOf(target));
//            } else {
//                link_id = String.valueOf(target).concat("_").concat(String.valueOf(j * n_y + i));
//            }
            link_id = String.valueOf(j * n_y + i).concat("_").concat(String.valueOf(target));
            transitStopFacility = transitScheduleFactory.createTransitStopFacility(tsf_id,
                    new Coord(j * delta_x, i * delta_y), false);
        }

        transitStopFacility.setLinkId(Id.createLinkId(link_id));
        id_link_list.add(Id.createLinkId(link_id));
        return transitStopFacility;
    }

    private static void createDepartures(TransitRoute route, TransitScheduleFactory transitScheduleFactory,
                                         int route_counter, double start, double end, double interval, int n_x, int n_y,
                                         Vehicles vehicles, double departureDelay) {
        double time = start;
        int i = 0;
        int transportersPerLine = (int) Math.ceil((departureDelay * (n_x * 2 - 1)) / interval); // calculate how many
        // transporters are presented on a line maximally
        while (time < end) {
            Departure dep = transitScheduleFactory.createDeparture(Id.create(String.valueOf(i), Departure.class), time);
            Id<Vehicle> vehicleId =
                    Id.create("tr_".concat(
                            String.valueOf(route_counter).concat("_").concat(String.valueOf(i % transportersPerLine))),
                            Vehicle.class);
            dep.setVehicleId(vehicleId);
            if (!vehicles.getVehicles().containsKey(vehicleId)) {
                vehicles.addVehicle(VehicleUtils.createVehicle(Id.create(vehicleId, Vehicle.class), vehicleType));
            }
            route.addDeparture(dep);
            i++;

            time += interval;
        }
    }

    private static NetworkRoute createNetworkRoute(List<Id<Link>> linkIds, PopulationFactory pf) {
        NetworkRoute route = pf.getRouteFactories().createRoute(NetworkRoute.class, linkIds.get(0),
                linkIds.get(linkIds.size() - 1));
        route.setLinkIds(linkIds.get(0), linkIds.subList(1, linkIds.size() - 1), linkIds.get(linkIds.size() - 1));
        return route;
    }

}
