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
    private double avDrtDist;
    private double fracWithCommonOrigDest;

    private static final Logger LOG = Logger.getLogger(PopulationCreator.class.getName());

    public PopulationCreator(int nRequests, int requestEndTime, Random random, String transportMode,
                             boolean isGridNetwork, boolean smallLinksCloseToNodes,
                             boolean createTrainLines, Function<Double, Double> travelDistanceDistribution,
                             double systemSize, double avDrtDist, double fracWithCommonOrigDest) {
        this.nRequests = nRequests;
        this.requestEndTime = requestEndTime;
        this.random = random;
        this.transportMode = transportMode;
        this.isGridNetwork = isGridNetwork;
        this.smallLinksCloseToNodes = smallLinksCloseToNodes;
        this.createTrainLines = createTrainLines;
        this.travelDistanceDistribution = travelDistanceDistribution;
        this.systemSize = systemSize;
        this.avDrtDist = avDrtDist;
        this.fracWithCommonOrigDest = fracWithCommonOrigDest;
    }

    public static void main(String... args) {
        String networkPath = "scenarios/Manhatten/network_trams.xml";
        String outputPath = "scenarios/Manhatten/population.xml";
        PopulationCreator populationCreator = new PopulationCreator(1000, 9 * 3600, new Random(), TransportMode.drt,
                false, false, true, x -> (double) ((x < 5000) ? 1 / 5000 : 0), 2500, 1234, -1);
        populationCreator.createPopulation(outputPath, networkPath, false);
    }

    public void createPopulation(String outputPopulationPath, String networkPath, boolean constDrtDemand) {
        Network net = NetworkUtils.readNetwork(networkPath);
        createPopulation(outputPopulationPath, net, constDrtDemand);
    }

    public void createPopulation(String outputPopulationPath, Network net, boolean constDrtDemand) {
        InverseTransformSampler sampler = null;

        if (travelDistanceDistribution != null) {
            sampler = new InverseTransformSampler(
                    travelDistanceDistribution,
                    false,
                    1e-3,
                    // xy_1/2 -> periodic BC
                    systemSize / 2,
                    (int) 1e7,
                    random);
        }


        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Population population = scenario.getPopulation();
        int reqsToGenerate = nRequests;
        if (constDrtDemand) {
            reqsToGenerate /= avDrtDist;
        }
        LOG.info("Generating " + reqsToGenerate + " Requests...");
        if (fracWithCommonOrigDest <= 0) {
            generatePopulation(population, net, reqsToGenerate, sampler, systemSize);
        } else if (fracWithCommonOrigDest > 0 && fracWithCommonOrigDest <= 1) {
            generatePopulation(population, net, reqsToGenerate, sampler, systemSize, fracWithCommonOrigDest);
        }

        PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
        populationWriter.write(outputPopulationPath);
    }


    private void generatePopulation(Population population, Network net, int nRequests,
                                    InverseTransformSampler sampler, double L) {
        Link firstLink = null;
        Link secondLink = null;
        List<Link> startLinks = net.getLinks().values().stream()
                .filter(l -> l.getAttributes().getAttribute(IS_START_LINK).equals(true))
                .collect(Collectors.toList());

//        StringBuilder sb = new StringBuilder();
//        sb.append("x;y\n");
        ClosestLinkFinder closestLinkFinder = new ClosestLinkFinder(net, 10, startLinks, L);
//        double preDistAverage = 0;
//        double resultingDistAverage = 0;
        Coord pre_target = null;
        double newX = 0;
        double newY = 0;
        for (int j = 0; j < nRequests; j++) {
            do {
                if (sampler != null) {
                    newX = random.nextDouble() * L;
                    newY = random.nextDouble() * L;
                    pre_target = new Coord(newX, newY);
                    firstLink = closestLinkFinder.findClosestLink(pre_target);

                    double dist = 0;
                    try {
                        dist = sampler.getSample();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    double angle = random.nextDouble() * 2 * Math.PI;

                    newX = ((firstLink.getCoord().getX() + dist * Math.cos(angle)) % L + L) % L;
                    newY = ((firstLink.getCoord().getY() + dist * Math.sin(angle)) % L + L) % L;
                    pre_target = new Coord(newX, newY);
                    secondLink = closestLinkFinder.findClosestLink(pre_target);
//                    if (!secondLink.equals(firstLink)) {
//                        preDistAverage += dist;
//                        resultingDistAverage += calculateDistancePeriodicBC(secondLink, firstLink, L);
//                    }
                } else {
                    firstLink = startLinks.get(random.nextInt(startLinks.size()));
                    secondLink = startLinks.get(random.nextInt(startLinks.size()));
                }
//            } while (calculateDistancePeriodicBC(orig_link, dest_link, L) < carGridSpacing);
            } while (secondLink.equals(firstLink));
            generateTrip(firstLink, secondLink, j, population);
//            sb.append(dest_link.getCoord().getX()).append(";").append(dest_link.getCoord().getY()).append("\n");
        }
//        for (Link l: net.getLinks().values()) {
//            sb.append(l.getCoord().getX()).append(";").append(l.getCoord().getY()).append("\n");
//        }
//        LOG.info("preDistAverage: " + preDistAverage / nRequests);
//        LOG.info("resultingDistAverage: " + resultingDistAverage / nRequests);
//        try (OutputStream outputStream = new FileOutputStream("test.xml.gz")) {
//            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);
//            try (Writer writer = new OutputStreamWriter(gzipOutputStream)) {
//                writer.write(sb.toString());
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private void generatePopulation(Population population, Network net, int nRequests,
                                    InverseTransformSampler sampler, double L, double fracWithCommonOrigDest) {
        Link origLink = null;
        Link destLink = null;
        Coord firstCoord;
        Coord secondCoord;
        List<Link> startLinks = net.getLinks().values().stream()
                .filter(l -> l.getAttributes().getAttribute(IS_START_LINK).equals(true))
                .collect(Collectors.toList());

//        StringBuilder sb = new StringBuilder();
//        sb.append("x;y\n");
        ClosestLinkFinder closestLinkFinder = new ClosestLinkFinder(net, 10, startLinks, L);
        Coord centerCoord = new Coord(L / 2, L / 2);
        Node centerNode = net.getNodes().values().stream()
                .min(Comparator.comparingDouble(n -> calculateDistancePeriodicBC(n.getCoord(), centerCoord, L))).get();
        List<Link> centerInLinks = centerNode.getInLinks().values().stream()
                .filter(l -> l.getAttributes().getAttribute(IS_START_LINK).equals(true)).collect(
                        Collectors.toList());
//        Link centerLink = closestLinkFinder.findClosestLink();
        double newX = 0;
        double newY = 0;
        if (sampler != null) {
            for (int j = 0; j < nRequests; j++) {
                do {
                    boolean firstCenter = false;
                    if (random.nextDouble() < fracWithCommonOrigDest) {
                        firstCoord = centerCoord;
                        firstCenter = true;
                    } else {
                        newX = random.nextDouble() * L;
                        newY = random.nextDouble() * L;
                        firstCoord = new Coord(newX, newY);
                    }
                    double dist = 0;
                    try {
                        dist = sampler.getSample();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    double angle = random.nextDouble() * 2 * Math.PI;

                    newX = ((firstCoord.getX() + dist * Math.cos(angle)) % L + L) % L;
                    newY = ((firstCoord.getY() + dist * Math.sin(angle)) % L + L) % L;
                    secondCoord = new Coord(newX, newY);
                    if (firstCenter && random.nextDouble() < 0.5) {
                        destLink = closestLinkFinder.findClosestLink(secondCoord);
                        Coord finalSecondCoord = secondCoord;
                        origLink = centerInLinks.stream()
                                .min(Comparator.comparing(l -> calculateDistancePeriodicBC(l.getCoord(),
                                        finalSecondCoord, L))).get();
                    } else if (firstCenter) {
                        origLink = closestLinkFinder.findClosestLink(secondCoord);
                        Coord finalSecondCoord = secondCoord;
                        destLink = centerInLinks.stream()
                                .min(Comparator.comparing(l -> calculateDistancePeriodicBC(l.getCoord(),
                                        finalSecondCoord, L))).get();
                    } else {
                        origLink = closestLinkFinder.findClosestLink(firstCoord);
                        destLink = closestLinkFinder.findClosestLink(secondCoord);
                    }
//            } while (calculateDistancePeriodicBC(orig_link, dest_link, L) < carGridSpacing);
                } while (origLink.equals(destLink));
                generateTrip(origLink, destLink, j, population);
//            sb.append(dest_link.getCoord().getX()).append(";").append(dest_link.getCoord().getY()).append("\n");
            }
        } else {
            for (int j = 0; j < nRequests; j++) {
                do {
                    if (random.nextDouble() < fracWithCommonOrigDest) {
                        if (random.nextDouble() < 0.5) {
                            destLink = startLinks.get(random.nextInt(startLinks.size()));
                            Coord finalSecondCoord = destLink.getCoord();
                            origLink = centerInLinks.stream()
                                    .min(Comparator.comparing(l -> calculateDistancePeriodicBC(l.getCoord(),
                                            finalSecondCoord, L))).get();
                        } else {
                            origLink = startLinks.get(random.nextInt(startLinks.size()));
                            Coord finalSecondCoord = origLink.getCoord();
                            destLink = centerInLinks.stream()
                                    .min(Comparator.comparing(l -> calculateDistancePeriodicBC(l.getCoord(),
                                            finalSecondCoord, L))).get();
                        }
                    } else {
                        origLink = startLinks.get(random.nextInt(startLinks.size()));
                        destLink = startLinks.get(random.nextInt(startLinks.size()));
                    }
                } while (origLink.equals(destLink));
                generateTrip(origLink, destLink, j, population);
            }
        }
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
                .min(Comparator.comparingDouble(node -> calculateDistancePeriodicBC(node.getCoord(), coord, L)))
                .orElseThrow();
    }

    private double getAverageDrtDist(double dCut, double ell) {
//        travelDistanceDistribution
        return 32.;
    }
}