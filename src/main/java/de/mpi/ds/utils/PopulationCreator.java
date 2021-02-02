package de.mpi.ds.utils;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.*;
import java.util.stream.Collectors;

import static de.mpi.ds.utils.GeneralUtils.calculateDistancePeriodicBC;
import static de.mpi.ds.utils.GeneralUtils.getNetworkDimensionsMinMax;

public class PopulationCreator implements UtilComponent {
    private static Random rand = new Random();

    private PopulationCreator() {
    }

    public static void main(String... args) {
//        Boolean[] bools = {true, false, true, true, true, false, true, true, true, false};
//        System.out.println(Arrays.stream(bools).map(b -> b ? 1 : 0).mapToInt(Integer::intValue).sum());
//        for (Boolean bool : bools) {
//            System.out.println(bool);
//        }
        String networkPath = "./output/network_diag.xml";
        createPopulation("./output/population.xml.gz", networkPath, nRequests, 31357);

        // Not neccessary with above method (string .gz already indicates saving in gzip format)
//        compressGzipFile("./output/population.xml", "./output/population.xml.gz");
//        deleteFile("./output/population.xml");
    }

    public static void createPopulation(String outputPopulationPath, String networkPath, int nRequests,
                                        long seed) {
        Network net = NetworkUtils.readNetwork(networkPath);
        createPopulation(outputPopulationPath, net, nRequests, seed);
    }
    public static void createPopulation(String outputPopulationPath, Network net, int nRequests,
                                        long seed) {

        double[] netDimsMinMax = getNetworkDimensionsMinMax(net);
        double xy_0 = netDimsMinMax[0];
        double xy_1 = netDimsMinMax[1];
        System.out.println("Network dimensions (min, max): " + Arrays.toString(netDimsMinMax));
        InverseTransformSampler sampler = new InverseTransformSampler(
                a -> 1 / (xy_1 - xy_0),
                true,
                xy_0,
                xy_1,
                10000);

        rand.setSeed(seed);
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Population population = scenario.getPopulation();
        generatePopulation(population, net, nRequests, sampler, xy_1);

        PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
        populationWriter.write(outputPopulationPath);
    }


    private static void generatePopulation(Population population, Network net, int nRequests,
                                           InverseTransformSampler sampler, double L) {
//        Id<Node> orig_id;
//        Id<Node> dest_id;
//        List<Id<Node>> nodeIdList = net.getNodes().values().stream()
//                .filter(n -> n.getCoord().getX() % delta_xy == 0)
//                .filter(n -> n.getCoord().getY() % delta_xy == 0)
//                .map(Identifiable::getId)
//                .collect(Collectors.toList());
        Coord orig_coord;
        Coord dest_coord;
        List<Node> nonStationNodeList = net.getNodes().values().stream()
                .filter(n -> n.getAttributes().getAttribute("isStation").equals(false)).collect(
                        Collectors.toList());
        for (int j = 0; j < nRequests; j++) {
            do {
//                orig_coord = getRandomNodeOfCollection(net.getNodes().values()).getCoord();
                orig_coord = nonStationNodeList.get(rand.nextInt(nonStationNodeList.size())).getCoord();
                if (sampler != null) {
                    double dist = 0;
                    try {
                        dist = sampler.getSample();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    double angle = rand.nextDouble() * 2 * Math.PI;

                    double newX = ((orig_coord.getX() + dist * Math.cos(angle)) % L + L) % L;
                    double newY = ((orig_coord.getY() + dist * Math.sin(angle)) % L + L) % L;
                    Coord pre_target = new Coord(newX, newY);
                    dest_coord = getClosestNode(pre_target, nonStationNodeList, L).getCoord();
                } else {
//                    dest_coord = getRandomNodeOfCollection(net.getNodes().values()).getCoord();
                    dest_coord = nonStationNodeList.get(rand.nextInt(nonStationNodeList.size())).getCoord();
                }
            } while (orig_coord.equals(dest_coord));
            generateTrip(orig_coord, dest_coord, j, population);
        }
    }

    private static Node getRandomNodeOfCollection(Collection<? extends Node> collection) {
        return collection.stream().skip(rand.nextInt(collection.size())).findFirst().orElseThrow();
    }

    private static void generateTrip(Coord source, Coord sink, int passenger_id, Population population) {
        Person person = population.getFactory()
                .createPerson(Id.createPersonId(String.valueOf(passenger_id)));
        // person.getCustomAttributes().put("hasLicense", "false");
        person.getAttributes().putAttribute("hasLicense", "false");
        Plan plan = population.getFactory().createPlan();
        Coord sourceLocation = shoot(source);
        Coord sinkLocation = shoot(sink);

        plan.addActivity(createFirst(sourceLocation, population));
        plan.addLeg(createDriveLeg(population, TransportMode.pt));
//        if (DistanceUtils.calculateDistance(sourceLocation, sinkLocation) > gamma * pt_interval * delta_xy) {
//            plan.addLeg(createDriveLeg(population, TransportMode.pt));
//        } else {
//            plan.addLeg(createDriveLeg(population, TransportMode.drt));
//        }
        plan.addActivity(createSecond(sinkLocation, population));
        person.addPlan(plan);
        population.addPerson(person);
    }

    private static Leg createDriveLeg(Population population, String mode) {
        Leg leg = population.getFactory().createLeg(mode);
        return leg;
    }

    private static Coord shoot(Coord source) {
        // Insert code here to blur the input coordinate.
        // For example, add a random number to the x and y coordinates.
        return source;
    }

    private static Activity createSecond(Coord workLocation, Population population) {
        Activity activity = population.getFactory().createActivityFromCoord("dummy", workLocation);
//        activity.setEndTime(24 * 60 * 60); // [s]
        return activity;
    }

    private static Activity createFirst(Coord homeLocation, Population population) {
        Activity activity = population.getFactory().createActivityFromCoord("dummy", homeLocation);
        activity.setEndTime(rand.nextInt(requestEndTime)); // [s]
        return activity;
    }

    private static Coord searchTransferLoc(Coord startLoc, Coord targetLoc) {
        double source_x = startLoc.getX();
        double source_y = startLoc.getY();
        double sink_x = targetLoc.getX();
        double sink_y = targetLoc.getY();
        double new_x = source_x;
        double new_y = source_y;
        if (source_x / 1000 % 2 == 0 && source_y / 1000 % 2 == 0) {
            if (sink_x - source_x < sink_y - source_y && sink_x - source_x != 0) {
                new_x = source_x + Math.signum(sink_x - source_x) * 1000;
            } else {
                new_y = source_y + Math.signum(sink_y - source_y) * 1000;
            }
        }
        return new Coord(new_x, new_y);
    }

    private static Activity createDrtActivity(Coord location, Population population) {
        Activity activity = population.getFactory().createActivityFromCoord("dummy", location);
        activity.setMaximumDuration(0);
        return activity;
    }

    private static Id<Person> createId(String source, String sink, int i, String transportMode) {
        return Id.create(source + "_" + sink + "_" + i, Person.class);
    }


    private static Node getClosestNode(Coord coord, List<Node> nodes, double L) {
        return nodes.stream()
//                        .allMatch(l -> l.getAllowedModes().stream().map(s -> s.contains("train"))))
                .min(Comparator
                        .comparingDouble(node -> calculateDistancePeriodicBC(node.getCoord(), coord, L)))
                .orElseThrow();
    }


    public static double taxiDistDistributionNotNormalized(double x, double mean, double k) {
        double z = x / mean;
        return Math.exp(-1. / z) * Math.pow(z, -k);
    }
}