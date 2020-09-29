package de.mpi.ds.matsim_bimodal.utils;

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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class PopulationUtil {

	private PopulationUtil() {
	}

	public static void main(String... args) {
		createPopulation("./output/population.xml", "./output/network.xml");
	}

	public static void createPopulation(String populationPath, String networkPath) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population population = scenario.getPopulation();
		Map<String, Coord> zoneGeometries = new HashMap<>();
		fillZoneData(zoneGeometries, networkPath);
		generatePopulation(zoneGeometries, population);
		PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
		populationWriter.write(populationPath);
	}

	private static void fillZoneData(Map<String, Coord> zoneGeometries, String networkPath) {
		// Add the locations you want to use here.
		// (with proper coordinates)
		Network net = NetworkUtils.readNetwork(networkPath);
		int i = 0;
		for (Node node : net.getNodes().values()) {
			zoneGeometries.put(String.valueOf(i), node.getCoord());
			i++;
		}
	}

	private static void generatePopulation(Map<String, Coord> zoneGeometries, Population population) {
		Network net = NetworkUtils.readNetwork("./output/network.xml");
		Random rand = new Random();
		rand.setSeed(231494);
		int trips = 1000;
		int orig_id;
		int dest_id;
		// Coord orig_coord;
		// Coord dest_coord;
		for (int j = 0; j < trips; j++) {
			do {
				orig_id = rand.nextInt(net.getNodes().size());
				dest_id = rand.nextInt(net.getNodes().size());
			} while (orig_id == dest_id);
			// // modulo cases only if agents should be placed on non pt nodes & next 2
			// lines
			// orig_coord = zoneGeometries.get(String.valueOf(orig_id));
			// dest_coord = zoneGeometries.get(String.valueOf(dest_id));
			// } while (orig_id == dest_id || orig_coord.getX() / 1000 % 2 == 1 ||
			// orig_coord.getY() / 1000 % 2 == 1
			// || dest_coord.getX() / 1000 % 2 == 1 || dest_coord.getY() / 1000 % 2 == 1);
			generateTrips(String.valueOf(orig_id), String.valueOf(dest_id), 1, j, zoneGeometries, population);
		}
	}

	private static void generateTrips(String from, String to, int quantity, int passenger_id,
			Map<String, Coord> zoneGeometries, Population population) {
		for (int i = 0; i < quantity; ++i) {
			Coord source = zoneGeometries.get(from);
			Coord sink = zoneGeometries.get(to);
			Person person = population.getFactory()
					.createPerson(createId(from, to, passenger_id + i, TransportMode.pt));
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
			plan.addLeg(createDriveLeg(population, TransportMode.pt));
//			if (!sinkLocation.equals(sinkTransferLocation)) {
//				plan.addActivity(createDrtActivity(sinkTransferLocation, population));
//				plan.addLeg(createDriveLeg(population, TransportMode.drt));
//			}
			plan.addActivity(createSecond(sinkLocation, population));
			person.addPlan(plan);
			population.addPerson(person);
		}
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
		activity.setEndTime(24 * 60 * 60); // [s]
		return activity;
	}

	private static Activity createFirst(Coord homeLocation, Population population) {
		Random rand = new Random();
		Activity activity = population.getFactory().createActivityFromCoord("dummy", homeLocation);
		activity.setEndTime(rand.nextInt(24 * 60 * 60)); // [s]
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