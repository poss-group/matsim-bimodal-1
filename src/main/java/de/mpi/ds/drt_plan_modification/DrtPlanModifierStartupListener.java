package de.mpi.ds.drt_plan_modification;

import de.mpi.ds.utils.GeneralUtils;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static de.mpi.ds.utils.GeneralUtils.*;
import static de.mpi.ds.utils.ScenarioCreator.*;

class DrtPlanModifierStartupListener implements StartupListener {
    private final static Logger LOG = Logger.getLogger(DrtPlanModifierStartupListener.class.getName());
    private double dCut;
    private String mode;
    private double periodicity;

    @FunctionalInterface
    interface Function2<One, Two> {
        public double apply(One one, Two two);
    }

    DrtPlanModifierStartupListener(DrtPlanModifierConfigGroup configGroup) {
        this.dCut = configGroup.getdCut();
        this.mode = configGroup.getMode();
        this.periodicity = configGroup.getPeriodicity();
    }

    private static Link searchTransferLink(Link fromLink, Link toLink, List<Link> transitStopLinks,
                                           String mode, Function2<Link, Link> calcDist) {
        if (mode.equals("wide_search")) {
            Queue<Link> queue = new LinkedList<>();
            List<Link> visited = new ArrayList<>();
            queue.add(fromLink);
            while (!queue.isEmpty()) {
                Link current = queue.remove();
                visited.add(current);
                if (current.getToNode().getAttributes().getAttribute(IS_STATION_NODE).equals(true)) {
                    return current;
                }
                // TODO check if this makes the method slow and makes sense for port on link routing
                // Add closest connected nodes to queue (sorted by their distance to the toNode)
                queue.addAll(current.getToNode().getOutLinks().values().stream()
                        .filter(e -> !visited.contains(e))
                        .sorted(Comparator
                                .comparingDouble(l -> calcDist.apply(l, toLink)))
                        .collect(Collectors.toList()));
            }
        } else if (mode.equals("shortest_dist")) {
            Link result = transitStopLinks.stream()
                    .min(Comparator
                            .comparingDouble(link -> calcDist.apply(link, fromLink)))
                    .orElseThrow();
            return result;
        }
        return null;
    }


    @Override
    public void notifyStartup(StartupEvent event) {
        LOG.info("Modifying plans...");

        Scenario sc = event.getServices().getScenario();
        Network network = sc.getNetwork();

//        List<Link> transitStopLinks = sc.getTransitSchedule().getTransitLines().values().stream()
//                .flatMap(tl -> tl.getRoutes().values().stream()
//                        .map(tr -> tr.getStops().stream()
//                                .map(stop -> network.getLinks().get(stop.getStopFacility().getLinkId()))))
//                .flatMap(Function.identity())
//                .collect(Collectors.toList());
        List<Node> transitStopNodes = null;
        List<Link> transitStopInLinks = null;
        double trainDelta = Double.MAX_VALUE;

        Function2<Link, Link> calcDist;
        Function<Link, Boolean> linkPeriodicityFilter;
        if (periodicity > 0) {
            double[] netDimsMinMax = GeneralUtils.getNetworkDimensionsMinMax(network, true);
            calcDist = (l1, l2) -> calculateDistancePeriodicBC(l1, l2, netDimsMinMax[1]);
            linkPeriodicityFilter = l -> l.getAttributes().getAttribute(PERIODIC_LINK).equals(false);
        } else {
            calcDist = GeneralUtils::calculateDistanceNonPeriodic;
            // links are never periodic in this case
            linkPeriodicityFilter = l -> true;
        }
        if (mode.equals("bimodal")) {
            transitStopNodes = network.getNodes().values().stream()
                    .filter(node -> node.getAttributes().getAttribute(IS_STATION_NODE).equals(true))
                    .collect(Collectors.toList());
            transitStopInLinks = transitStopNodes.stream()
                    .flatMap(n -> n.getInLinks().values().stream())
//                    .filter(l -> l.getAttributes().getAttribute(IS_START_LINK).equals(true))
//                    .filter(l -> doubleCloseToZero(l.getLength() - 100))
                    .filter(l -> !l.getAllowedModes().contains(TransportMode.train))
                    .filter(linkPeriodicityFilter::apply)
//                    .filter(l -> l.getAttributes().getAttribute(PERIODIC_LINK).equals(false))
                    .collect(Collectors.toList());

//            Node randomNode = network.getNodes().values().stream()
//                    .filter(n -> n.getAttributes().getAttribute(IS_STATION_NODE).equals(true))
//                    .findAny().orElseThrow();
//            List<Double> trainDeltas = network.getNodes().values().stream()
//                    .filter(n -> n.getAttributes().getAttribute(IS_STATION_NODE).equals(true))
//                    .filter(n -> n.getCoord().getY() == randomNode.getCoord().getY())
//                    .map(n -> n.getCoord().getX())
//                    .distinct()
//                    .sorted()
//                    .limit(2)
//                    .collect(Collectors.toList());
//            trainDelta = trainDeltas.get(1) - trainDeltas.get(0);

            double[] trainLinkLengths = network.getLinks().values().stream()
                    .filter(l -> l.getAllowedModes().contains(TransportMode.train)).mapToDouble(Link::getLength)
                    .toArray();
            trainDelta = Arrays.stream(trainLinkLengths).average().orElseThrow();
        }
//        assert trainDelta == 1000 : "Did not find predefined length L=1000, instead found: " + trainDelta;


        MultiModeDrtConfigGroup multiModeConfGroup = MultiModeDrtConfigGroup.get(sc.getConfig());
        int multiConfSize = multiModeConfGroup.getModalElements().size();
        boolean splittedFleet = false;
        if (multiConfSize == 1)
            LOG.warn("Working with only drt legs (single fleet)");
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
                switch (mode) {
                    case "bimodal":
                        if (middleLeg.getMode().equals(TransportMode.pt)) {
                            Link firstLink = network.getLinks().get(firstAct.getLinkId());
                            Link lastLink = network.getLinks().get(lastAct.getLinkId());
                            if (calcDist.apply(firstLink, lastLink) > dCut) {
                                Link dummyFirstLink = null;
                                Link dummyLastLink = null;
                                if (firstLink.getToNode().getAttributes().getAttribute(IS_STATION_NODE).equals(false)) {
                                    dummyFirstLink = searchTransferLink(firstLink, lastLink, transitStopInLinks,
                                            "shortest_dist", calcDist);
                                }
                                // Todo ToNode or FromNode?
                                if (lastLink.getToNode().getAttributes().getAttribute(IS_STATION_NODE).equals(false)) {
                                    dummyLastLink = searchTransferLink(lastLink, firstLink, transitStopInLinks,
                                            "shortest_dist", calcDist);
                                }
//                        insertTransferStops(plan, sc.getPopulation(), dummyFirstCoord, dummyLastCoord, splittedFleet);
                                insertTransferStops(plan, sc.getPopulation(), dummyFirstLink, dummyLastLink,
                                        splittedFleet);
//                        middleLeg.setMode(TransportMode.car);
                            } else {
                                middleLeg.setMode(TransportMode.drt);
                            }
                        }
                        break;
                    case "car":
                        middleLeg.setMode(TransportMode.car);
                        break;
                    case "unimodal":
                        middleLeg.setMode(TransportMode.drt);
                        break;
                }
            }
        }
        // To get resulting plans in output directory
        PopulationWriter populationWriter = new PopulationWriter(sc.getPopulation(), sc.getNetwork());
        String outputPath = event.getServices().getControlerIO().getOutputPath()
                .concat("/drt_plan_modified_plans.xml.gz");
        populationWriter.write(outputPath);
    }

    private List<Link> getOppositeDirectionLinks(List<Link> transitStopOutLinks, Network network) {
        return transitStopOutLinks.stream().map(l -> getLinkConnectingNodes(l.getToNode(), l.getFromNode(), network))
                .collect(
                        Collectors.toList());
    }

    private Link getLinkConnectingNodes(Node n1, Node n2, Network network) {
        String linkIdString = n1.getId().toString() + "-" + n2.getId().toString();
        Id<Link> linkId = Id.createLinkId(linkIdString);
        return network.getLinks().get(linkId);
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

    private void insertTransferStops(Plan plan, Population population, Link dummy_first_link,
                                     Link dummy_last_link, boolean splittedFleet) {
        if (dummy_last_link != null) {
            Activity activity = population.getFactory().createActivityFromLinkId("dummy", dummy_last_link.getId());
            activity.setCoord(dummy_last_link.getToNode().getCoord());
//            Activity activity = population.getFactory().createActivityFromCoord("dummy", dummy_last_link.getCoord());
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
//            Activity activity = population.getFactory().createActivityFromCoord("dummy", dummy_first_link.getCoord());
            activity.setMaximumDuration(0);
            plan.getPlanElements().add(2, activity);
        }
    }

    private static Node getPtNode(Link link) {
        if (link.getToNode().getAttributes().getAttribute(IS_STATION_NODE).equals(true)) {
            return link.getToNode();
        } else if (link.getFromNode().getAttributes().getAttribute(IS_STATION_NODE).equals(true)) {
            return link.getFromNode();
        } else {
            LOG.error("Neither To no From Node is PT station");
            return null;
        }
    }
}
