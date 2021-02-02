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
import org.matsim.contrib.dvrp.fleet.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.mpi.ds.utils.CreateScenarioElements.compressGzipFile;
import static de.mpi.ds.utils.CreateScenarioElements.deleteFile;

/**
 * @author jbischoff
 * This is an example script to create a vehicle file for taxis, SAV or DRTs.
 * The vehicles are distributed randomly in the network.
 */
public class DrtFleetVehiclesCreator implements UtilComponent {

    /**
     * Adjust these variables and paths to your need.
     */
    private static final Random random = MatsimRandom.getRandom();

    public static void main(String[] args) {

		new DrtFleetVehiclesCreator().run("./output/network_diag.xml", "output/drtvehicles.xml", numberOfDrtVehicles);
//        new CreateDrtFleetVehicles().runModifyForDoubleFleet(
//                "scenarios/fine_grid/drtvehicles/drtvehicles_optDrtCount_diag/");
    }

    public void run(String networkPath, String outputDrtVehiclesPath, int nVehicles) {

//	    random.setSeed(42);
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkPath.toString());
        final int[] i = {0};
        List<? extends Map.Entry<Id<Link>, ? extends Link>> linkList = scenario.getNetwork().getLinks().entrySet()
                .stream()
                .filter(entry -> entry.getValue().getAllowedModes()
                        .contains(TransportMode.car)) // drt can only start on links with Transport mode 'car'
                .collect(Collectors.toList());

        Collections.shuffle(linkList);


        Stream<DvrpVehicleSpecification> vehicleSpecificationStream = linkList.stream()
                .limit(nVehicles) // select the first *numberOfVehicles* links
                .map(entry -> ImmutableDvrpVehicleSpecification.newBuilder()
                        .id(Id.create("drt_" + i[0]++, DvrpVehicle.class))
                        .startLinkId(entry.getKey())
                        .capacity(seatsPerDrtVehicle)
                        .serviceBeginTime(operationStartTime)
                        .serviceEndTime(operationEndTime)
                        .build());

        new FleetWriter(vehicleSpecificationStream).write(outputDrtVehiclesPath.toString());
        System.out.println("Wrote drt vehicles");
    }

    public void runModifyForDoubleFleet(String dir) {
        String[] files = getDrtFiles(dir);
        Path outPath = Paths.get(dir, "splitted");
        outPath.toFile().mkdirs();

        for (String file : files) {
            FleetSpecification fleetSpecification = new FleetSpecificationImpl();
            try {
                Path path = Paths.get(dir, file);
                new FleetReader(fleetSpecification).parse(new File(path.toString()).toURI().toURL());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            Stream<DvrpVehicleSpecification> vehicleSpecificationStream = fleetSpecification.getVehicleSpecifications()
                    .values().stream();
            List<DvrpVehicleSpecification> vehicleSpecificationList = vehicleSpecificationStream
                    .collect(Collectors.toList());

            Stream<DvrpVehicleSpecification> specificationStream1 = vehicleSpecificationList
                    .subList(0, vehicleSpecificationList.size() / 2).stream();
            Stream<DvrpVehicleSpecification> specificationStream2 = vehicleSpecificationList
                    .subList(vehicleSpecificationList.size() / 2, vehicleSpecificationList.size()).stream();

            String path1 = Paths.get(outPath.toString(), stripFileEnd(file).concat("_1.xml")).toString();
            String path2 = Paths.get(outPath.toString(), stripFileEnd(file).concat("_2.xml")).toString();
            new FleetWriter(specificationStream1).write(path1);
            new FleetWriter(specificationStream2).write(path2);
            compressGzipFile(path1, path1.concat(".gz"));
            compressGzipFile(path2, path2.concat(".gz"));
            deleteFile(path1);
            deleteFile(path2);
        }
    }

    private String stripFileEnd(String file) {
        if (file.endsWith(".xml.gz")) {
            return file.substring(0, file.indexOf(".xml.gz"));
        }
        if (file.endsWith(".xml")) {
            return file.substring(0, file.indexOf(".xml"));
        } else
            return file;
    }

    private static String[] getDrtFiles(String Dir) {
        Pattern pattern = Pattern.compile("drtvehicles.*\\.xml(\\.gz)?");
        File dir = new File(Dir);
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return pattern.matcher(name).matches();
            }
        };
        String[] files = dir.list(filter);
//        String[] files = (String[]) Arrays.stream(dir.listFiles(filter)).map(file -> file.getAbsolutePath())
//        .toArray(String[]::new);
        assert files != null;
        Arrays.sort(files);

        return files;
    }
}
