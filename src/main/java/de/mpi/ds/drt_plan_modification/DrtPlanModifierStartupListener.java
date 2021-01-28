package de.mpi.ds.drt_plan_modification;

import de.mpi.ds.utils.GeneralUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.core.api.internal.MatsimWriter;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.utils.io.IOUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class DrtPlanModifierStartupListener implements StartupListener {
    private final static Logger LOG = Logger.getLogger(DrtPlanModifierStartupListener.class.getName());
    private double zetaCut;
    private boolean privateCarMode;

    DrtPlanModifierStartupListener(DrtPlanModifierConfigGroup configGroup) {
        this.zetaCut = configGroup.getZetaCut();
        this.privateCarMode = configGroup.getPrivateCarMode();
    }

    private static Node searchTransferNode(Node fromNode, Node toNode,
                                           List<Coord> transitStopCoords,
                                           Map<Coord, Node> coordToNode,
                                           String mode) {
        if (mode.equals("wide_search")) {
            Queue<Node> queue = new LinkedList<>();
            List<Node> visited = new ArrayList<>();
            queue.add(fromNode);
            while (!queue.isEmpty()) {
                Node current = queue.remove();
                visited.add(current);
                Collection<? extends Link> outLinks = current.getOutLinks().values();
                if (isPtStation(outLinks.stream())) {
                    return current;
                }
                // TODO check if this makes the method slow
                // Add closest connected nodes to queue (sorted by their distance to the toNode)
                queue.addAll(outLinks.stream().map(Link::getToNode)
                        .filter(e -> !visited.contains((e)))
                        .sorted(Comparator
                                .comparingDouble(n -> DistanceUtils.calculateDistance(n.getCoord(), toNode.getCoord())))
                        .collect(Collectors.toList()));
            }
        } else if (mode.equals("shortest_dist")) {
            Coord min = transitStopCoords.stream()
                    .min(Comparator
                            .comparingDouble(coord -> DistanceUtils.calculateDistance(coord, fromNode.getCoord())))
                    .orElseThrow();
            return coordToNode.get(min);
        }
        return null;
    }


    @Override
    public void notifyStartup(StartupEvent event) {
        LOG.info("Modifying plans...");

        Scenario sc = event.getServices().getScenario();
        Network network = sc.getNetwork();
        Map<Coord, Node> coordToNode = network.getNodes().entrySet().stream().collect(
                Collectors.toMap(e -> e.getValue().getCoord(),
                        e -> network.getNodes().get(e.getKey())));

        List<Coord> transitStopCoords = sc.getTransitSchedule().getTransitLines().values().stream()
                .flatMap(tl -> tl.getRoutes().values().stream()
                        .map(tr -> tr.getStops().stream().map(stop -> stop.getStopFacility().getCoord())))
                .flatMap(Function.identity())
                .collect(Collectors.toList());

//        List<Double> trainDeltas = network.getLinks().values().stream()
//                .filter(e -> e.getAllowedModes().contains(TransportMode.train))
//                .filter(e -> e.getFromNode().getCoord().getY() == 0)
//                .map(e -> e.getCoord().getX())
//                .distinct()
//                .sorted()
//                .limit(2)
//                .collect(Collectors.toList());
        Node bla = network.getNodes().values().stream()
                .filter(n -> n.getAttributes().getAttribute("isStation").equals(true))
                .findAny().orElseThrow();
        List<Double> trainDeltas = network.getNodes().values().stream()
                .filter(n -> n.getAttributes().getAttribute("isStation").equals(true))
                .filter(n -> n.getCoord().getY() == bla.getCoord().getY())
                .map(n -> n.getCoord().getX())
                .distinct()
                .sorted()
                .limit(2)
                .collect(Collectors.toList());
        double trainDelta = trainDeltas.get(1) - trainDeltas.get(0);
        assert trainDelta == 1000 : "Did not find predefined length L=1000, instead found: " + trainDelta;

        double[] netDimsMinMax = GeneralUtils.getNetworkDimensionsMinMax(network);

        MultiModeDrtConfigGroup multiModeConfGroup = MultiModeDrtConfigGroup.get(sc.getConfig());
        int multiConfSize = multiModeConfGroup.getModalElements().size();
        boolean splittedFleet = false;
        if (multiConfSize == 1)
            LOG.warn("Working with only drt legs");
        else if (multiConfSize == 2) {
            splittedFleet = true;
            LOG.warn("Working with \"drt\" and \"acc_egr_drt\" legs");
        } else
            LOG.error("MultiModeDrtConfigGroup size expected to be 1 or 2");

        int count = 0;
        for (Person person : sc.getPopulation().getPersons().values()) {
            if (count % 10000 == 0) {
                LOG.info("Person" + count);
            }
            count++;
            for (Plan plan : person.getPlans()) {
                Activity firstAct = null;
                Leg middleLeg = null;
                Activity lastAct = null;
                boolean foundFirst = false;
                for (PlanElement el : plan.getPlanElements()) {
                    if (el instanceof Activity && !foundFirst) {
                        firstAct = (Activity) el;
                        foundFirst = true;
                    } else if (el instanceof Activity) {
                        lastAct = (Activity) el;
                    } else {
                        middleLeg = (Leg) el;
                    }
                }
                assert middleLeg != null;
                // Only insert transit activities if leg mode is pt
                if (middleLeg.getMode().equals(TransportMode.pt) && !privateCarMode) {
                    if (GeneralUtils
                            .calculateDistancePeriodicBC(firstAct.getCoord(), lastAct.getCoord(), netDimsMinMax[1]) >
                            zetaCut * trainDelta) {
                        Coord dummyFirstCoord = null;
                        Coord dummyLastCoord = null;
                        Node firstNode = coordToNode.get(firstAct.getCoord());
                        Node lastNode = coordToNode.get(lastAct.getCoord());
                        Link dummyFirstLink = null;
                        Link dummyLastLink = null;
                        if (!isPtStation(firstNode)) {
                            Node dummyFirstNode = searchTransferNode(firstNode, lastNode, transitStopCoords,
                                    coordToNode,
                                    "shortest_dist");
                            assert dummyFirstNode != null;
                            Activity finalFirstAct = firstAct;
                            dummyFirstLink = dummyFirstNode.getInLinks().values().stream()
                                    .min(Comparator.comparingDouble(l ->
                                            GeneralUtils
                                                    .calculateDistancePeriodicBC(l.getCoord(),
                                                            finalFirstAct.getCoord(),
                                                            netDimsMinMax[1]))).orElseThrow();
                        }
                        if (!isPtStation(lastNode)) {
                            Node dummyLastNode = searchTransferNode(lastNode, firstNode, transitStopCoords, coordToNode,
                                    "shortest_dist");
                            assert dummyLastNode != null;
                            Activity finalLastAct = firstAct;
                            dummyLastLink = dummyLastNode.getInLinks().values().stream()
                                    .min(Comparator.comparingDouble(l ->
                                            GeneralUtils
                                                    .calculateDistancePeriodicBC(l.getCoord(), finalLastAct.getCoord(),
                                                            netDimsMinMax[1]))).orElseThrow();
                        }
//                        insertTransferStops(plan, sc.getPopulation(), dummyFirstCoord, dummyLastCoord, splittedFleet);
                        insertTransferStops(plan, sc.getPopulation(), dummyFirstLink, dummyLastLink, splittedFleet);
                    } else {
                        middleLeg.setMode(TransportMode.drt);
                    }
                } else if (privateCarMode) {
                    middleLeg.setMode(TransportMode.car);
                }
            }
        }
        // To get resulting plans in output directory
        PopulationWriter populationWriter = new PopulationWriter(sc.getPopulation(), sc.getNetwork());
        String outputPath = event.getServices().getControlerIO().getOutputPath().concat("/drt_plan_modified_plans.xml.gz");
        populationWriter.write(outputPath);
    }

    private static boolean isPtStation(Node node) {
        return node.getOutLinks().values().stream().map(e -> e.getAllowedModes().contains("train"))
                .map(b -> b ? 1 : 0)
                .mapToInt(Integer::intValue).sum() > 3;
    }

    private static boolean isPtStation(Stream<? extends Link> linkStream) {
        return linkStream.map(e -> e.getAllowedModes().contains("train"))
                .map(b -> b ? 1 : 0)
                .mapToInt(Integer::intValue).sum() > 3;
    }

    private void insertTransferStops(Plan plan, Population population, Coord dummy_first_coord,
                                     Coord dummy_last_coord, boolean splittedFleet) {
        if (dummy_last_coord != null) {
            Activity activity = population.getFactory().createActivityFromCoord("dummy", dummy_last_coord);
            activity.setMaximumDuration(0);
            plan.getPlanElements().add(2, activity);
            if (splittedFleet)
                plan.getPlanElements().add(3, population.getFactory().createLeg("acc_egr_drt"));
            else
                plan.getPlanElements().add(3, population.getFactory().createLeg(TransportMode.drt));
        }
        if (dummy_first_coord != null) {
            if (splittedFleet)
                plan.getPlanElements().add(1, population.getFactory().createLeg("acc_egr_drt"));
            else
                plan.getPlanElements().add(1, population.getFactory().createLeg(TransportMode.drt));
            Activity activity = population.getFactory().createActivityFromCoord("dummy", dummy_first_coord);
            activity.setMaximumDuration(0);
            plan.getPlanElements().add(2, activity);
        }
    }

    private void insertTransferStops(Plan plan, Population population, Link dummy_first_link,
                                     Link dummy_last_link, boolean splittedFleet) {
        if (dummy_last_link != null) {
            Activity activity = population.getFactory().createActivityFromLinkId("dummy", dummy_last_link.getId());
            activity.setCoord(dummy_last_link.getToNode().getCoord());
            activity.setMaximumDuration(0);
            plan.getPlanElements().add(2, activity);
            if (splittedFleet)
                plan.getPlanElements().add(3, population.getFactory().createLeg("acc_egr_drt"));
            else
                plan.getPlanElements().add(3, population.getFactory().createLeg(TransportMode.drt));
        }
        if (dummy_first_link != null) {
            if (splittedFleet)
                plan.getPlanElements().add(1, population.getFactory().createLeg("acc_egr_drt"));
            else
                plan.getPlanElements().add(1, population.getFactory().createLeg(TransportMode.drt));
            Activity activity = population.getFactory().createActivityFromLinkId("dummy", dummy_first_link.getId());
            activity.setCoord(dummy_first_link.getToNode().getCoord());
            activity.setMaximumDuration(0);
            plan.getPlanElements().add(2, activity);
        }
    }
}
