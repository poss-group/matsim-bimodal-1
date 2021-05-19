package de.mpi.ds.utils;

import org.matsim.api.core.v01.*;
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
import static de.mpi.ds.utils.GeneralUtils.calculateDistancePeriodicBC;
import static de.mpi.ds.utils.InverseTransformSampler.taxiDistDistributionNotNormalized;
import static de.mpi.ds.utils.ScenarioCreator.IS_START_LINK;

public class PopulationCreator implements UtilComponent {

    private int nRequests;
    private int requestEndTime;
    private Random random;
    private String transportMode;
    private boolean isGridNetwork;
    private double carGridSpacing;
    private boolean smallLinksCloseToNodes;
    private boolean createTrainLines;
    private String travelDistanceDistribution;
    private double travelDistanceMeanOverL;
    private double systemSize;

    public PopulationCreator(int nRequests, int requestEndTime, Random random, String transportMode,
                             boolean isGridNetwork, double carGridSpacing, boolean smallLinksCloseToNodes,
                             boolean createTrainLines, String travelDistanceDistribution,
                             double travelDistanceMeanOverL, double systemSize) {
        this.nRequests = nRequests;
        this.requestEndTime = requestEndTime;
        this.random = random;
        this.transportMode = transportMode;
        this.isGridNetwork = isGridNetwork;
        this.carGridSpacing = carGridSpacing;
        this.smallLinksCloseToNodes = smallLinksCloseToNodes;
        this.createTrainLines = createTrainLines;
        this.travelDistanceDistribution = travelDistanceDistribution;
        this.travelDistanceMeanOverL = travelDistanceMeanOverL;
        this.systemSize = systemSize;
    }

    public static void main(String... args) {
        String networkPath = "./output/network_circ_rad.xml.gz";
        PopulationCreator populationCreator = new PopulationCreator(1000, 4 * 3600, new Random(), TransportMode.pt,
                false, 100, false, true, "Uniform", 1 / 4, 10000);
        populationCreator.createPopulation("./output/population.xml.gz", networkPath);
    }

    public void createPopulation(String outputPopulationPath, String networkPath) {
        Network net = NetworkUtils.readNetwork(networkPath);
        createPopulation(outputPopulationPath, net);
    }

    public void createPopulation(String outputPopulationPath, Network net) {

        double[] netDimsMinMax = getNetworkDimensionsMinMax(net, (isGridNetwork && createTrainLines));
        double xy_0 = netDimsMinMax[0];
        double xy_1 = netDimsMinMax[1];
        System.out.println("Network dimensions (min, max): " + Arrays.toString(netDimsMinMax));

        Function<Double, Double> probabilityDensityDist = null;
        if (travelDistanceDistribution.equals("InverseGamma")) {
            probabilityDensityDist = x -> taxiDistDistributionNotNormalized(x, travelDistanceMeanOverL * systemSize,
                    3.1);
        } else if (travelDistanceDistribution.equals("Uniform")) {
            probabilityDensityDist = x ->
                    x < travelDistanceMeanOverL * systemSize * 2 ?
                            1 / (travelDistanceMeanOverL * systemSize * 2 - xy_0) : 0;
        }

        InverseTransformSampler sampler = new InverseTransformSampler(
                probabilityDensityDist,
                false,
                xy_0 + 1e-3,
                // xy_1/2 -> periodic BC
                Math.abs(xy_1/2 - xy_0),
                (int) 1e7,
                random);

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Population population = scenario.getPopulation();
        generatePopulation(population, net, nRequests, sampler, xy_1);

        PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
        populationWriter.write(outputPopulationPath);
    }


    private void generatePopulation(Population population, Network net, int nRequests,
                                    InverseTransformSampler sampler, double L) {
//        Id<Node> orig_id;
//        Id<Node> dest_id;
//        List<Id<Node>> nodeIdList = net.getNodes().values().stream()
//                .filter(n -> n.getCoord().getX() % delta_xy == 0)
//                .filter(n -> n.getCoord().getY() % delta_xy == 0)
//                .map(Identifiable::getId)
//                .collect(Collectors.toList());
        Link orig_link;
        Link dest_link;
        List<Link> startLinks = null;
        startLinks = net.getLinks().values().stream()
                .filter(n -> n.getAttributes().getAttribute(IS_START_LINK).equals(true))
                .collect(Collectors.toList());
//        List<Node> borderNonStationNodeList = facilityNodes.stream()
//                .filter(n -> n.getCoord().getX() != 0 && n.getCoord().getX() != 10000 && n.getCoord().getY() != 0 &&
//                        n.getCoord().getY() != 10000).collect(Collectors.toList());
//        double preDistAverage = 0;
//        double resultingDistAverage = 0;
        for (int j = 0; j < nRequests; j++) {
            do {
//                orig_coord = getRandomNodeOfCollection(net.getNodes().values()).getCoord();
                double newX = random.nextDouble()*L;
                double newY = random.nextDouble()*L;
                Coord pre_target = new Coord(newX, newY);
                orig_link = getClosestDestLink(pre_target, startLinks, L);
//                orig_coord = borderNonStationNodeList.get(rand.nextInt(borderNonStationNodeList.size())).getCoord();
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

//                    preDistAverage += dist;
//                    resultingDistAverage += calculateDistancePeriodicBC(dest_link, orig_link, L);
                } else {
//                    dest_link = getRandomNodeOfCollection(net.getNodes().values()).getCoord();
                    dest_link = startLinks.get(random.nextInt(startLinks.size()));
                }
//            } while (calculateDistancePeriodicBC(orig_link, dest_link, L) < carGridSpacing);
            } while (dest_link.equals(orig_link));
            generateTrip(orig_link, dest_link, j, population);
        }
//        System.out.println("preDistAverage" + preDistAverage/nRequests);
//        System.out.println("resultingDistAverage" + resultingDistAverage/nRequests);
    }

    private Node getRandomNodeOfCollection(Collection<? extends Node> collection) {
        return collection.stream().skip(random.nextInt(collection.size())).findFirst().orElseThrow();
    }

    private void generateTrip(Link source, Link sink, int passenger_id, Population population) {
        Person person = population.getFactory()
                .createPerson(Id.createPersonId(String.valueOf(passenger_id)));
        // person.getCustomAttributes().put("hasLicense", "false");
        person.getAttributes().putAttribute("hasLicense", "false");
        Plan plan = population.getFactory().createPlan();

        plan.addActivity(createFirst(source, population));
        plan.addLeg(population.getFactory().createLeg(transportMode));
//        if (DistanceUtils.calculateDistance(sourceLocation, sinkLocation) > gamma * pt_interval * delta_xy) {
//            plan.addLeg(createDriveLeg(population, TransportMode.pt));
//        } else {
//            plan.addLeg(createDriveLeg(population, TransportMode.drt));
//        }
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
        // Apparently Transit router needs Coordinates to work (of toNode because otherwise, passengers have to walk to final dest.)
        activity.setCoord(link.getToNode().getCoord());
//        Activity activity = population.getFactory().createActivityFromCoord("dummy", link.getCoord());
        activity.setEndTime(random.nextInt(requestEndTime)); // [s]
        return activity;
    }

    private Coord searchTransferLoc(Coord startLoc, Coord targetLoc) {
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

    private Activity createDrtActivity(Coord location, Population population) {
        Activity activity = population.getFactory().createActivityFromCoord("dummy", location);
        activity.setMaximumDuration(0);
        return activity;
    }

    private Id<Person> createId(String source, String sink, int i, String transportMode) {
        return Id.create(source + "_" + sink + "_" + i, Person.class);
    }


    private Link getClosestDestLink(Coord coord, List<Link> outLinks, double L) {
        return outLinks.stream()
//                        .allMatch(l -> l.getAllowedModes().stream().map(s -> s.contains("train"))))
                .min(Comparator
                        .comparingDouble(node -> calculateDistancePeriodicBC(node.getCoord(), coord, L)))
                .orElseThrow();
    }


    boolean isInsertedNode(Node node) {
        String nodeId = node.getId().toString();
        return (nodeId.contains("north") || nodeId.contains("west") || nodeId.contains("south") ||
                nodeId.contains("east"));
    }
}