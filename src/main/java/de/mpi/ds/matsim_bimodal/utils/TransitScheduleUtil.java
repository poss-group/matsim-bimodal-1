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
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.List;

public class TransitScheduleUtil { // TODO: Make more generic

    public static void main(String[] args) {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        TransitSchedule schedule = scenario.getTransitSchedule();
        TransitScheduleFactory transitScheduleFactory = schedule.getFactory();
        PopulationFactory populationFactory = scenario.getPopulation().getFactory();
        Network net = NetworkUtils.readNetwork("./output/network.xml");

        createTransitSchedule(transitScheduleFactory, schedule, populationFactory);

        new TransitScheduleWriter(schedule).writeFile("./output/transitSchedule.xml");

    }

    public static void createTransitSchedule(TransitScheduleFactory transitScheduleFactory, TransitSchedule schedule, PopulationFactory populationFactory) {
        int[] counter = {0, 0};
        createLines(transitScheduleFactory, schedule, populationFactory, true, counter);
        createLines(transitScheduleFactory, schedule, populationFactory, false, counter);
    }

    private static void createLines(TransitScheduleFactory transitScheduleFactory, TransitSchedule schedule, PopulationFactory populationFactory, boolean vertical, int[] counter) {
        int route_counter = counter[0];
        int stop_counter = counter[1];
        for (int i = 1; i < 10; i += 2) {
            TransitLine transitLine = transitScheduleFactory.createTransitLine(Id.create("Line".concat(String.valueOf(route_counter)), TransitLine.class));
            List<TransitRouteStop> transitRouteStopList = new ArrayList<>();
            List<Id<Link>> id_link_list = new ArrayList<>();
            for (int k = 0; k < 20; k++) {
                int j = -Math.abs(k - 10) + 10; // counting 0..10..1
                boolean forward = k < 10; // forward true for counting 0..9; forward false for 10..1
                TransitStopFacility transitStopFacility = null;
                transitStopFacility = createTransitStop(stop_counter, id_link_list, transitScheduleFactory, i, j, forward, vertical);

                TransitRouteStop transitrouteStop = transitScheduleFactory.createTransitRouteStop(transitStopFacility, 150, 180);
                transitrouteStop.setAwaitDepartureTime(true);
                transitRouteStopList.add(transitrouteStop);
                schedule.addStopFacility(transitStopFacility);

                stop_counter++;
            }

            TransitRoute transitRoute = giveTransitRoute(route_counter, id_link_list, populationFactory, transitScheduleFactory, transitRouteStopList);
            transitLine.addRoute(transitRoute);

            schedule.addTransitLine(transitLine);
            route_counter++;
        }
        counter[0] = route_counter;
        counter[1] = stop_counter;
    }

    private static TransitRoute giveTransitRoute(int route_counter, List<Id<Link>> id_link_list, PopulationFactory populationFactory, TransitScheduleFactory transitScheduleFactory, List<TransitRouteStop> transitRouteStopList) {
        Id<TransitRoute> r_id = Id.create(String.valueOf(route_counter), TransitRoute.class);
        NetworkRoute networkRoute = createNetworkRoute(id_link_list, populationFactory);
        TransitRoute transitRoute = transitScheduleFactory.createTransitRoute(r_id, networkRoute, transitRouteStopList, "train");
        createDepartures(transitRoute, transitScheduleFactory, route_counter, 6 * 60 * 60, 20 * 60 * 60, 15 * 60);
        return transitRoute;
    }


    private static TransitStopFacility createTransitStop(int stop_counter, List<Id<Link>> id_link_list, TransitScheduleFactory transitScheduleFactory, int i, int j, boolean forward, boolean vertical) {
        int target;
        Id<TransitStopFacility> tsf_id = Id.create(String.valueOf(stop_counter), TransitStopFacility.class);
        TransitStopFacility transitStopFacility = null;
        String link_id;

        if (vertical) {
            if (forward) {
                target = i * 11 + j + 1;
            } else {
                target = i * 11 + j - 1;
            }
            link_id = String.valueOf(i * 11 + j).concat("_").concat(String.valueOf(target));
            transitStopFacility = transitScheduleFactory.createTransitStopFacility(tsf_id, new Coord(i * 1000, j * 1000), false);
        } else {
            if (forward) {
                target = (j + 1) * 11 + i;
            } else {
                target = (j - 1) * 11 + i;
            }
            link_id = String.valueOf(j * 11 + i).concat("_").concat(String.valueOf(target));
            transitStopFacility = transitScheduleFactory.createTransitStopFacility(tsf_id, new Coord(j * 1000, i * 1000), false);
        }

        transitStopFacility.setLinkId(Id.createLinkId(link_id));
        id_link_list.add(Id.createLinkId(link_id));
        return transitStopFacility;
    }

    private static void createDepartures(TransitRoute route, TransitScheduleFactory transitScheduleFactory, int route_counter, double start, double end, double interval) {
        double time = start;
        int i = 0;
        while (time < end) {
            Departure dep = transitScheduleFactory.createDeparture(Id.create(String.valueOf(i), Departure.class), time);
            dep.setVehicleId(Id.create("tr_".concat(String.valueOf(route_counter + 10 * (i % 4))), Vehicle.class));
            route.addDeparture(dep);
            i++;
            time += interval;
        }
    }

    private static NetworkRoute createNetworkRoute(List<Id<Link>> linkIds, PopulationFactory pf) {
        NetworkRoute route = pf.getRouteFactories().createRoute(NetworkRoute.class, linkIds.get(0), linkIds.get(linkIds.size() - 1));
        route.setLinkIds(linkIds.get(0), linkIds.subList(1, linkIds.size() - 1), linkIds.get(linkIds.size() - 1));
        return route;
    }

}
