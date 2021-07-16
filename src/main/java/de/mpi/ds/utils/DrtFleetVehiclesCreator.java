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
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.fleet.*;
import org.matsim.core.network.NetworkUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.mpi.ds.utils.CreateScenarioElements.compressGzipFile;
import static de.mpi.ds.utils.CreateScenarioElements.deleteFile;
import static de.mpi.ds.utils.GeneralUtils.getNetworkDimensionsMinMax;
import static de.mpi.ds.utils.InverseTransformSampler.taxiDistDistribution;

import org.apache.commons.math3.analysis.integration.*;

/**
 * @author jbischoff
 * This is an example script to create a vehicle file for taxis, SAV or DRTs.
 * The vehicles are distributed randomly in the network.
 */
public class DrtFleetVehiclesCreator implements UtilComponent {
    private static final double BETA = 0.382597858232;

    /**
     * Adjust these variables and paths to your need.
     */
    private int seatsPerDrtVehicle;
    private double operationStartTime;
    private double operationEndTime;
    private double meanDistance;
    private double ptSpacing;
    private Random random;
    private Function<Double, Double> travelDistanceDistribution;

    public DrtFleetVehiclesCreator(int seatsPerDrtVehicle, double operationStartTime, double operationEndTime,
                                   Random random, Function<Double, Double> travelDistanceDistribution,
                                   double meanDistance, double ptSpacing) {
        this.seatsPerDrtVehicle = seatsPerDrtVehicle;
        this.operationStartTime = operationStartTime;
        this.operationEndTime = operationEndTime;
        this.random = random;
        this.travelDistanceDistribution = travelDistanceDistribution;
        this.meanDistance = meanDistance;
        this.ptSpacing = ptSpacing;
    }

    public static void main(String[] args) {

//        new DrtFleetVehiclesCreator(4, 0, 26 * 3600, 10, new Random(), travelDistanceDistribution)
//                .run("scenarios/Manhatten/network_trams.xml", "scenarios/Manhatten/drtvehicles.xml");
//        new CreateDrtFleetVehicles().runModifyForDoubleFleet(
//                "scenarios/fine_grid/drtvehicles/drtvehicles_optDrtCount_diag/");
    }

    public void run(String inputNetworkPath, String outputUnimPath, String outputBimPath, double dCut,
                    int drtFleetSize) {
        Network net = NetworkUtils.readNetwork(inputNetworkPath);
        run(net, outputUnimPath, outputBimPath, dCut, drtFleetSize);
    }

    // If dCut method is called with dCut split fleets
    public void run(Network net, String outputUnimPath, String outputBimPath, double dCut, int drtFleetSize) {
        UnivariateIntegrator integrator = new RombergIntegrator();
        double[] netDimsMinMax = getNetworkDimensionsMinMax(net);
        double boundedNorm = integrator
                .integrate(1000000, x -> taxiDistDistribution(x, meanDistance, 3.1), 0.0001, netDimsMinMax[1]);
        double avDistFracToDCut = integrator
                .integrate(1000000, x -> x * taxiDistDistribution(x, meanDistance, 3.1) / boundedNorm, 0.0001, dCut);
        double avDistFracFromDCut = integrator
                .integrate(1000000, x -> taxiDistDistribution(x, meanDistance, 3.1) / boundedNorm, dCut, netDimsMinMax[1]) * 2 *
                BETA * ptSpacing;
        int fleetSizeBimodal = (int) Math
                .round(drtFleetSize * avDistFracToDCut / (avDistFracToDCut + avDistFracFromDCut));
        int fleetSizeUnimodal = drtFleetSize - fleetSizeBimodal;

//        fleetSizeUnimodal = drtFleetSize/2;
//        fleetSizeBimodal = drtFleetSize - fleetSizeUnimodal;
        run(net, outputUnimPath, fleetSizeUnimodal, "unim_");
        run(net, outputBimPath, fleetSizeBimodal, "bim_");
    }

    public void run(String networkPath, String outputPath, int drtFleetSize) {
        Network net = NetworkUtils.readNetwork(networkPath);
        run(net, outputPath, drtFleetSize, "");
    }

    public void run(Network net, String outputPath, int drtFleetSize) {
        run(net, outputPath, drtFleetSize, "");
    }

    public void run(Network net, String outputPath, int drtFleetSize, String prefix) {

//        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
//        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkPath.toString());
        final int[] i = {0};
        List<? extends Map.Entry<Id<Link>, ? extends Link>> linkList = net.getLinks().entrySet()
                .stream()
                .filter(entry -> entry.getValue().getAllowedModes()
                        .contains(TransportMode.car)) // drt can only start on links with Transport mode 'car'
                .collect(Collectors.toList());

        Collections.shuffle(linkList, random);

        Stream<DvrpVehicleSpecification> vehicleSpecificationStream = linkList.stream()
                .limit(drtFleetSize) // select the first *numberOfVehicles* links
                .map(entry -> ImmutableDvrpVehicleSpecification.newBuilder()
                        .id(Id.create(prefix + "drt_" + i[0]++, DvrpVehicle.class))
                        .startLinkId(entry.getKey())
                        .capacity(seatsPerDrtVehicle)
                        .serviceBeginTime(operationStartTime)
                        .serviceEndTime(operationEndTime)
                        .build());

        new FleetWriter(vehicleSpecificationStream).write(outputPath.toString());
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
