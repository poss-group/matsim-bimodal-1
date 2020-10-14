/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetWriter;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author jbischoff
 * This is an example script to create a vehicle file for taxis, SAV or DRTs.
 * The vehicles are distributed randomly in the network.
 */
public class CreateDrtFleetVehicles implements UtilComponent {

	/**
	 * Adjust these variables and paths to your need.
	 */
	private static final Random random = MatsimRandom.getRandom();

	private static final Path networkFile = Paths.get("output/network.xml");
	private static final Path outputFile = Paths.get("output/drtvehicles.xml");

	public static void main(String[] args) {

		new CreateDrtFleetVehicles().run();
	}

	private void run() {

	    random.setSeed(42);
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile.toString());
		final int[] i = {0};
		List<? extends Map.Entry<Id<Link>, ? extends Link>> linkList = scenario.getNetwork().getLinks().entrySet().stream()
				.filter(entry -> entry.getValue().getAllowedModes().contains(TransportMode.car)) // drt can only start on links with Transport mode 'car'
                .collect(Collectors.toList());

		Collections.shuffle(linkList);


		Stream<DvrpVehicleSpecification> vehicleSpecificationStream = linkList.stream()
				.limit(numberOfDrtVehicles) // select the first *numberOfVehicles* links
				.map(entry -> ImmutableDvrpVehicleSpecification.newBuilder()
						.id(Id.create("drt_" + i[0]++, DvrpVehicle.class))
						.startLinkId(entry.getKey())
						.capacity(seatsPerDrtVehicle)
						.serviceBeginTime(operationStartTime)
						.serviceEndTime(operationEndTime)
						.build());

		new FleetWriter(vehicleSpecificationStream).write(outputFile.toString());
		System.out.println("Wrote drt vehicles");

	}

}
