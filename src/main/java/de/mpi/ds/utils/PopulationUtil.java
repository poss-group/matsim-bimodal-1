package de.mpi.ds.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.*;
import java.util.stream.Collectors;

public class PopulationUtil implements UtilComponent {
    private static Random rand = new Random();

    private PopulationUtil() {
    }

    public static void main(String... args) {
        createPopulation("./output/population.xml", "./output/network.xml", N_REQUESTS, TransportMode.pt, 234);
    }

    public static void createPopulation(String outputPopulationPath, String networkPath, int nRequests, String transportMode, long seed) {
        rand.setSeed(seed);
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Population population = scenario.getPopulation();
        Map<String, Coord> zoneGeometries = new HashMap<>();
        Network net = NetworkUtils.readNetwork(networkPath);
        fillZoneData(zoneGeometries, net);
        generatePopulation(zoneGeometries, population, net, nRequests, transportMode, seed);
        PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
        populationWriter.write(outputPopulationPath);
    }

    private static void fillZoneData(Map<String, Coord> zoneGeometries, Network net) {
        // Add the locations you want to use here.
        // (with proper coordinates)
        for (Node node : net.getNodes().values()) {
            zoneGeometries.put(node.getId().toString(), node.getCoord());
        }
    }

    private static void generatePopulation(Map<String, Coord> zoneGeometries, Population population,
                                           Network net, int nRequests, String transportMode, long seed) {
        rand.setSeed(seed);
        Id<Node> orig_id;
        Id<Node> dest_id;
        List<Id<Node>> nodeIdList = net.getNodes().values().stream()
                .filter(n -> n.getCoord().getX() % delta_xy == 0)
                .filter(n -> n.getCoord().getY() % delta_xy == 0)
                .map(n -> n.getId())
                .collect(Collectors.toList());
        // Coord orig_coord;
        // Coord dest_coord;
        for (int j = 0; j < nRequests; j++) {
            do {
                orig_id = nodeIdList.get(rand.nextInt(nodeIdList.size()));
                dest_id = nodeIdList.get(rand.nextInt(nodeIdList.size()));
            } while (orig_id.equals(dest_id));
            // // modulo cases only if agents should be placed on non pt nodes & next 2
            // lines
            // orig_coord = zoneGeometries.get(String.valueOf(orig_id));
            // dest_coord = zoneGeometries.get(String.valueOf(dest_id));
            // } while (orig_id == dest_id || orig_coord.getX() / 1000 % 2 == 1 ||
            // orig_coord.getY() / 1000 % 2 == 1
            // || dest_coord.getX() / 1000 % 2 == 1 || dest_coord.getY() / 1000 % 2 == 1);
            generateTrip(orig_id.toString(), dest_id.toString(), j, zoneGeometries, population, transportMode);
        }
    }

    private static void generateTrip(String from, String to, int passenger_id,
                                     Map<String, Coord> zoneGeometries, Population population,
                                     String transportMode) {
        Coord source = zoneGeometries.get(from);
        Coord sink = zoneGeometries.get(to);
        Person person = population.getFactory()
                .createPerson(Id.createPersonId(String.valueOf(passenger_id)));
        // person.getCustomAttributes().put("hasLicense", "false");
        person.getAttributes().putAttribute("hasLicense", "false");
        Plan plan = population.getFactory().createPlan();
        Coord sourceLocation = shoot(source);
        Coord sinkLocation = shoot(sink);
//			Coord sourceTransferLocation = searchTransferLoc(sourceLocation, sinkLocation);
//			Coord sinkTransferLocation = searchTransferLoc(sinkLocation, sourceLocation);
        plan.addActivity(createFirst(sourceLocation, population));
//			if (!sourceLocation.equals(sourceTransferLocation)) {
//				plan.addLeg(createDriveLeg(population, TransportMode.drt));
//				plan.addActivity(createDrtActivity(sourceTransferLocation, population));
//			}
        plan.addLeg(createDriveLeg(population, transportMode));
//			if (!sinkLocation.equals(sinkTransferLocation)) {
//				plan.addActivity(createDrtActivity(sinkTransferLocation, population));
//				plan.addLeg(createDriveLeg(population, TransportMode.drt));
//			}
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
        activity.setEndTime(rand.nextInt(MAX_END_TIME)); // [s]
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
}