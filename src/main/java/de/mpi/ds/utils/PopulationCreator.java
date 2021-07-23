package de.mpi.ds.utils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import static de.mpi.ds.utils.GeneralUtils.*;
import static de.mpi.ds.utils.GeneralUtils.calculateDistancePeriodicBC;
import static de.mpi.ds.utils.InverseTransformSampler.taxiDistDistributionNotNormalized;
import static de.mpi.ds.utils.ScenarioCreator.IS_START_LINK;

public class PopulationCreator implements UtilComponent {

    private int nRequests;
    private int requestEndTime;
    private Random random;
    private String transportMode;
    private boolean isGridNetwork;
    private boolean smallLinksCloseToNodes;
    private boolean createTrainLines;
    private Function<Double, Double> travelDistanceDistribution;
    private double systemSize;

    private static final Logger LOG = Logger.getLogger(PopulationCreator.class.getName());

    public PopulationCreator(int nRequests, int requestEndTime, Random random, String transportMode,
                             boolean isGridNetwork, boolean smallLinksCloseToNodes,
                             boolean createTrainLines, Function<Double, Double> travelDistanceDistribution,
                             double systemSize) {
        this.nRequests = nRequests;
        this.requestEndTime = requestEndTime;
        this.random = random;
        this.transportMode = transportMode;
        this.isGridNetwork = isGridNetwork;
        this.smallLinksCloseToNodes = smallLinksCloseToNodes;
        this.createTrainLines = createTrainLines;
        this.travelDistanceDistribution = travelDistanceDistribution;
        this.systemSize = systemSize;
    }

    public static void main(String... args) {
        String networkPath = "scenarios/Manhatten/network_trams.xml";
        String outputPath = "scenarios/Manhatten/population.xml";
        PopulationCreator populationCreator = new PopulationCreator(1000, 9 * 3600, new Random(), TransportMode.drt,
                false, false, true, x -> (double) ((x < 5000) ? 1 / 5000 : 0), 2500);
        populationCreator.createPopulation(outputPath, networkPath);
    }

    public void createPopulation(String outputPopulationPath, String networkPath) {
        Network net = NetworkUtils.readNetwork(networkPath);
        createPopulation(outputPopulationPath, net);
    }

    public void createPopulation(String outputPopulationPath, Network net) {

        InverseTransformSampler sampler = new InverseTransformSampler(
                travelDistanceDistribution,
                false,
                1e-3,
                // xy_1/2 -> periodic BC
                systemSize / 2,
                (int) 1e7,
                random);

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Population population = scenario.getPopulation();
        generatePopulation(population, net, nRequests, sampler, systemSize);

        PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
        populationWriter.write(outputPopulationPath);
    }


    private void generatePopulation(Population population, Network net, int nRequests,
                                    InverseTransformSampler sampler, double L) {
        Link orig_link;
        Link dest_link;
        List<Link> startLinks = null;
        startLinks = net.getLinks().values().stream()
                .filter(l -> l.getAttributes().getAttribute(IS_START_LINK).equals(true))
                .collect(Collectors.toList());

//        StringBuilder sb = new StringBuilder();
//        sb.append("x;y\n");
//        double preDistAverage = 0;
//        double resultingDistAverage = 0;
        for (int j = 0; j < nRequests; j++) {
            do {
                double newX = random.nextDouble() * L;
                double newY = random.nextDouble() * L;
                Coord pre_target = new Coord(newX, newY);
                orig_link = getClosestDestLink(pre_target, startLinks, L);
                if (sampler != null) {
                    double dist = 0;
                    try {
                        dist = sampler.getSample();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    double angle = random.nextDouble() * 2 * Math.PI;

                    newX = ((orig_link.getCoord().getX() + dist * Math.cos(angle)) % L + L) % L;
                    newY = ((orig_link.getCoord().getY() + dist * Math.sin(angle)) % L + L) % L;
                    pre_target = new Coord(newX, newY);
                    dest_link = getClosestDestLink(pre_target, startLinks, L);

//                    if (!dest_link.equals(orig_link)) {
//                        preDistAverage += dist;
//                        resultingDistAverage += calculateDistancePeriodicBC(dest_link, orig_link, L);
//                    }
                } else {
                    newX = random.nextDouble() * L;
                    newY = random.nextDouble() * L;
                    pre_target = new Coord(newX, newY);
                    dest_link = getClosestDestLink(pre_target, startLinks, L);
                }
//            } while (calculateDistancePeriodicBC(orig_link, dest_link, L) < carGridSpacing);
            } while (dest_link.equals(orig_link));
            generateTrip(orig_link, dest_link, j, population);
//            sb.append(dest_link.getCoord().getX()).append(";").append(dest_link.getCoord().getY()).append("\n");
        }
//        for (Link l: net.getLinks().values()) {
//            sb.append(l.getCoord().getX()).append(";").append(l.getCoord().getY()).append("\n");
//        }
//        System.out.println("preDistAverage: " + preDistAverage/nRequests);
//        System.out.println("resultingDistAverage: " + resultingDistAverage/nRequests);
//        try (OutputStream outputStream = new FileOutputStream("test.xml.gz")) {
//            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);
//            try (Writer writer = new OutputStreamWriter(gzipOutputStream)) {
//                writer.write(sb.toString());
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
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

    private Link getClosestDestLink(Coord coord, List<Link> outLinks, double L) {
        return outLinks.stream()
                .min(Comparator
                        .comparingDouble(node -> calculateDistancePeriodicBC(node.getCoord(), coord, L)))
                .orElseThrow();
    }

    private double getAverageDrtDist(double dCut, double ell) {
//        travelDistanceDistribution
        return 32.;
    }
}