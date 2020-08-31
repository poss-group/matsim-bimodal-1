package de.mpi.ds.matsim_bimodal.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * "P" has to do with "Potsdam" and "Z" with "Zurich", but P and Z are mostly
 * used to show which classes belong together.
 */
public class PopulationUtil {

	private PopulationUtil() {
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
			zoneGeometries.put("home".concat(String.valueOf(i)), node.getCoord());
			zoneGeometries.put("work".concat(String.valueOf(i)), node.getCoord());
			i++;
		}
	}

	private static void generatePopulation(Map<String, Coord> zoneGeometries, Population population) {
		Network net = NetworkUtils.readNetwork("./output/network.xml");
		Random rand = new Random();
		int trips = 100;
		int h_id;
		int w_id;
		for (int j = 0; j < trips; j++) {
			do {
				h_id = rand.nextInt(net.getNodes().size());
				w_id = rand.nextInt(net.getNodes().size());
			} while (h_id == w_id);
			generateHomeWorkHomeTrips("home".concat(String.valueOf(h_id)), "work".concat(String.valueOf(w_id)), 1, j,
				zoneGeometries, population);
		}
	}

	private static void generateHomeWorkHomeTrips(String from, String to, int quantity, int passenger_id,
					Map<String, Coord> zoneGeometries, Population population) {
		for (int i=0; i<quantity; ++i) {
			Coord source = zoneGeometries.get(from);
			Coord sink = zoneGeometries.get(to);
			Person person = population.getFactory().createPerson(createId(from, to, passenger_id+i, TransportMode.car));
			Plan plan = population.getFactory().createPlan();
			Coord homeLocation = shoot(source);
			Coord workLocation = shoot(sink);
			plan.addActivity(createHome(homeLocation, population));
			plan.addLeg(createDriveLeg(population));
			plan.addActivity(createWork(workLocation, population));
//			plan.addLeg(createDriveLeg());
//			plan.addActivity(createHome(homeLocation));
			person.addPlan(plan);
			population.addPerson(person);
		}
	}

	private static Leg createDriveLeg(Population population) {
		Leg leg = population.getFactory().createLeg(TransportMode.car);
		return leg;
	}

	private static Coord shoot(Coord source) {
		// Insert code here to blur the input coordinate.
		// For example, add a random number to the x and y coordinates.
		return source;
	}

	private static Activity createWork(Coord workLocation, Population population) {
		Activity activity = population.getFactory().createActivityFromCoord("work", workLocation);
		activity.setEndTime(24 * 60 * 60); // [s]
		return activity;
	}

	private static Activity createHome(Coord homeLocation, Population population) {
		Random rand = new Random();
		Activity activity = population.getFactory().createActivityFromCoord("home", homeLocation);
		activity.setEndTime(rand.nextInt(24 * 60 * 60)); // [s]
		return activity;
	}

	private static Id<Person> createId(String source, String sink, int i, String transportMode) {
		return Id.create(transportMode + "_" + source + "_" + sink + "_" + i, Person.class);
	}
}
