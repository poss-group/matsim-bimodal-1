package de.mpi.ds.osm_utils;

import de.mpi.ds.polygon_utils.RayCasting;
import de.mpi.ds.utils.InverseTransformSampler;
import de.mpi.ds.utils.UtilComponent;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.mpi.ds.utils.GeneralUtils.*;
import static de.mpi.ds.utils.InverseTransformSampler.taxiDistDistributionNotNormalized;
import static de.mpi.ds.utils.ScenarioCreator.IS_START_LINK;

public class PopulationCreatorOsm implements UtilComponent {

    private int nRequests;
    private int requestEndTime;
    private Random random;
    private String transportMode;
    private Function<Double, Double> travelDistanceDistribution;
    private double travelMeanDist;

    public PopulationCreatorOsm(int nRequests, int requestEndTime, Random random, String transportMode,
                                Function<Double, Double> travelDistanceDistribution, double travelMeanDist) {
        this.nRequests = nRequests;
        this.requestEndTime = requestEndTime;
        this.random = random;
        this.transportMode = transportMode;
        this.travelDistanceDistribution = travelDistanceDistribution;
        this.travelMeanDist = travelMeanDist;
    }

    public static void main(String... args) {
//        String networkPath = "scenarios/Manhatten/network_trams.xml";
//        String outputPath = "scenarios/Manhatten/population.xml";
//        PopulationCreatorOsm populationCreatorOsm = new PopulationCreatorOsm(1000, 9 * 3600, new Random(),
//                TransportMode.drt, "Uniform", 1 / 4);
//        populationCreatorOsm.createPopulation(outputPath, networkPath);
    }

    public void createPopulation(String outputPopulationPath, String networkPath, ArrayList<Coord> hull) {
        Network net = NetworkUtils.readNetwork(networkPath);
        createPopulation(outputPopulationPath, net, hull);
    }

    public void createPopulation(String outputPopulationPath, Network net, ArrayList<Coord> hull) {

        double[] netDimsMinMax = getNetworkDimensionsMinMax(net, false);
        double xy_0 = netDimsMinMax[0];
        double xy_1 = netDimsMinMax[1];
        double x0 = net.getNodes().values().stream().mapToDouble(n -> n.getCoord().getX()).min().getAsDouble();
        double x1 = net.getNodes().values().stream().mapToDouble(n -> n.getCoord().getX()).max().getAsDouble();
        double y0 = net.getNodes().values().stream().mapToDouble(n -> n.getCoord().getY()).min().getAsDouble();
        double y1 = net.getNodes().values().stream().mapToDouble(n -> n.getCoord().getY()).max().getAsDouble();
        System.out.println("Network dimensions (min, max): " + Arrays.toString(netDimsMinMax));

        InverseTransformSampler sampler = new InverseTransformSampler(
                travelDistanceDistribution,
                false,
                xy_0 + 1e-3,
                Math.abs(xy_1 - xy_0),
                (int) 1e7,
                random);

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Population population = scenario.getPopulation();
        try {
            generatePopulation(population, net, nRequests, sampler, x0, x1, y0, y1, hull);
        } catch (Exception e) {
            e.printStackTrace();
        }

        PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
        populationWriter.write(outputPopulationPath);
    }


    private void generatePopulation(Population population, Network net, int nRequests, InverseTransformSampler sampler,
                                    double x0, double x1, double y0, double y1, ArrayList<Coord> hull) throws
            Exception {
        double Lx = x1 - x0;
        double Ly = y1 - y0;
        Link orig_link = null;
        Link dest_link = null;
        boolean unsuccsessfull = true;
        List<Link> startLinks = null;
        startLinks = net.getLinks().values().stream()
                .filter(l -> l.getAttributes().getAttribute(IS_START_LINK).equals(true))
                .collect(Collectors.toList());

        for (int j = 0; j < nRequests; j++) {
            Coord pre_target_origin = null;
            Coord pre_target_destination = null;
            do {
                unsuccsessfull = true;
                double newX = random.nextDouble() * Lx + x0;
                double newY = random.nextDouble() * Ly + y0;
                pre_target_origin = new Coord(newX, newY);
                if (!RayCasting.contains(hull, pre_target_origin)) {
                    continue;
                }
                orig_link = getClosestDestLink(pre_target_origin, startLinks);
                if (sampler != null) {
                    int counter = 0;
                    do {
                        double dist = sampler.getSample();
                        double angle = random.nextDouble() * 2 * Math.PI;
                        newX = orig_link.getCoord().getX() + dist * Math.cos(angle);
                        newY = orig_link.getCoord().getY() + dist * Math.sin(angle);
                        pre_target_destination = new Coord(newX, newY);
                        counter++;
                    } while (counter < 5 && !RayCasting.contains(hull, pre_target_destination));
                    if (counter == 5) {
                        continue;
                    }
                    dest_link = getClosestDestLink(pre_target_destination, startLinks);
                    unsuccsessfull = false;

                } else {
                    do {
                        newX = random.nextDouble() * Lx + x0;
                        newY = random.nextDouble() * Ly + y0;
                        pre_target_destination = new Coord(newX, newY);
                    } while (!RayCasting.contains(hull, pre_target_destination));
                    dest_link = getClosestDestLink(pre_target_destination, startLinks);
                }
//            } while (calculateDistancePeriodicBC(orig_link, dest_link, L) < carGridSpacing);
            } while (unsuccsessfull || dest_link.equals(orig_link));
            generateTrip(orig_link, dest_link, j, population);
        }
//        System.out.println("preDistAverage" + preDistAverage/nRequests);
//        System.out.println("resultingDistAverage" + resultingDistAverage/nRequests);
    }

    private Node getRandomNodeOfCollection(Collection<? extends Node> collection) {
        return collection.stream().skip(random.nextInt(collection.size())).findFirst().orElseThrow();
    }

    private void generateTrip(Link source, Link sink, int passenger_id, Population population) {
        Person person = population.getFactory().createPerson(Id.createPersonId(String.valueOf(passenger_id)));
        // person.getCustomAttributes().put("hasLicense", "false");
        person.getAttributes().putAttribute("hasLicense", "false"); // Necessary ?
        Plan plan = population.getFactory().createPlan();

        plan.addActivity(createFirst(source, population));
        plan.addLeg(population.getFactory().createLeg(transportMode));
        plan.addActivity(createSecond(sink, population));
        person.addPlan(plan);
        population.addPerson(person);
    }

    private Activity createSecond(Link link, Population population) {
        Activity activity = population.getFactory().createActivityFromLinkId("dummy", link.getId());
        // Apparently Transit router needs Coordinates to work (of toNoe because otherwise, passengers have to walk to final dest.)
        activity.setCoord(link.getToNode().getCoord());
        return activity;
    }

    private Activity createFirst(Link link, Population population) {
        Activity activity = population.getFactory().createActivityFromLinkId("dummy", link.getId());
        activity.setCoord(link.getToNode().getCoord());
        // For uniform temporal distribution
        activity.setEndTime(random.nextInt(requestEndTime)); // [s]
        return activity;
    }

    private Link getClosestDestLink(Coord coord, List<Link> outLinks) {
        return outLinks.stream()
                .min(Comparator.comparingDouble(node -> calculateDistanceNonPeriodic(node.getCoord(), coord)))
                .orElseThrow();
    }
}