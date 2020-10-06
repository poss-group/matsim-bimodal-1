package de.mpi.ds.drt_plan_modification;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.ActivityFacility;
//import org.matsim.core.population.LegImpl;

import java.util.*;
import java.util.stream.Collectors;

class DrtPlanModifierStartupListener implements StartupListener {
    private final static Logger LOG = Logger.getLogger(DrtPlanModifierStartupListener.class.getName());
    @Inject
    Provider<TripRouter> tripRouterProvider;
//    @Inject
//    Controler controler;

    private static Coord searchTransferLoc(Coord startLoc, Coord targetLoc) {
        double source_x = startLoc.getX();
        double source_y = startLoc.getY();
        double sink_x = targetLoc.getX();
        double sink_y = targetLoc.getY();
        double new_x = source_x;
        double new_y = source_y;
        Random rnd = new Random();
        rnd.setSeed(12342);
        //Could also ask if sink_x/y - source_x/y == 1
        if (sink_y - source_y == 0) {
            new_x = source_x + Math.signum(sink_x - source_x) * 1000;
        } else if (sink_x - source_x == 0) {
            new_y = source_y + Math.signum(sink_y - source_y) * 1000;
        } else if (rnd.nextBoolean()) {
            new_x = source_x + Math.signum(sink_x - source_x) * 1000;
        } else {
            new_y = source_y + Math.signum(sink_y - source_y) * 1000;
        }
        return new Coord(new_x, new_y);
    }

    private static Node searchTransferNode(Node fromNode, Node toNode) {
        Queue<Node> queue = new LinkedList<>();
        queue.add(fromNode);
        while (!queue.isEmpty()) {
            Node current = queue.remove();
            Collection<? extends Link> outLinks = current.getOutLinks().values();
            if (outLinks.stream().anyMatch(l -> l.getAllowedModes().contains(TransportMode.train))) {
                return current;
            }
            // Add closest connected nodes to queue (sorted by their distance to the toNode)
            queue.addAll(outLinks.stream().map(Link::getToNode)
                    .sorted(Comparator.comparingDouble(n -> DistanceUtils.calculateDistance(n.getCoord(), toNode.getCoord())))
                    .collect(Collectors.toList()));
        }
        return null;
    }

    private static Activity createDummyActivity(Coord location, Population population) {
        Activity activity = population.getFactory().createActivityFromCoord("dummy", location);
        activity.setMaximumDuration(0);
        return activity;
    }

    @Override
    public void notifyStartup(StartupEvent event) {
        LOG.info("Modifying plans...");
//        LOG.warn(controler.getTripRouterProvider().get());
//        TripRouter tripRouter = tripRouterProvider.get();
//        Person testPerson = event.getServices().getScenario().getPopulation().getFactory().createPerson(Id.createPersonId("-1"));
//        ActivityFacilitiesFactoryImpl activityFacilitiesFactory =  new ActivityFacilitiesFactoryImpl();
//        ActivityFacility fstAct = activityFacilitiesFactory.createActivityFacility(Id.create("-1", ActivityFacility.class),Id.createLinkId("6029_6130"));
//        ActivityFacility scndAct = activityFacilitiesFactory.createActivityFacility(Id.create("-1", ActivityFacility.class),Id.createLinkId("5524_5625"));
//        List<? extends PlanElement> routeList = tripRouter.calcRoute("car", fstAct,scndAct, 0, testPerson);
//        LOG.warn(routeList.get(0));
//        ((LegImpl) routeList.get(0)).travTime;
        //TODO replace drtrouter in InsertionCostCalculator with this

        Scenario sc = event.getServices().getScenario();
        Network network = sc.getNetwork();
        Map<Coord, Id<Node>> coordToNode = network.getNodes().entrySet().stream().collect(
                Collectors.toMap(e -> e.getValue().getCoord(),
                        Map.Entry::getKey));
        for (Person person : sc.getPopulation().getPersons().values()) {
            for (Plan plan : person.getPlans()) {
                Activity firstAct = null;
                Activity lastAct = null;
                boolean foundFirst = false;
                for (PlanElement el : plan.getPlanElements()) {
                    if (el instanceof Activity && !foundFirst) {
                        firstAct = (Activity) el;
                        foundFirst = true;
                    } else if (el instanceof Activity) {
                        lastAct = (Activity) el;
                    }
                }
                Coord dummyFirstCoord = null;
                Coord dummyLastCoord = null;
                assert firstAct != null;
                assert lastAct != null;
                Node firstNode = network.getNodes().get(coordToNode.get(firstAct.getCoord()));
                Node lastNode = network.getNodes().get(coordToNode.get(lastAct.getCoord()));
                if (firstNode.getOutLinks().values().stream().noneMatch(e -> e.getAllowedModes().contains("train"))) {
//                    dummyFirstCoord = searchTransferLoc(firstAct.getCoord(), lastAct.getCoord());
                    Node dummyFirstNode = searchTransferNode(firstNode, lastNode);
                    assert dummyFirstNode != null;
                    dummyFirstCoord = dummyFirstNode.getCoord();
                }
                if (lastNode.getOutLinks().values().stream().noneMatch(e -> e.getAllowedModes().contains("train"))) {
//                    dummyLastCoord = searchTransferLoc(lastAct.getCoord(), firstAct.getCoord());
                    Node dummyLastNode = searchTransferNode(lastNode, firstNode);
                    assert dummyLastNode != null;
                    dummyLastCoord = dummyLastNode.getCoord();
                }
                insertTransferStops(plan, sc.getPopulation(), dummyFirstCoord, dummyLastCoord);
            }
        }
        // To get resulting plans in output directory
        PopulationWriter populationWriter = new PopulationWriter(sc.getPopulation(), sc.getNetwork());
        populationWriter.write("./output/drt_plan_modified_plans.xml");
    }

    private void insertTransferStops(Plan plan, Population population, Coord dummy_first_coord, Coord dummy_last_coord) {
        if (dummy_last_coord != null) {
            plan.getPlanElements().add(2, createDummyActivity(dummy_last_coord, population));
            plan.getPlanElements().add(3, population.getFactory().createLeg(TransportMode.drt));
        }
        if (dummy_first_coord != null) {
            plan.getPlanElements().add(1, population.getFactory().createLeg(TransportMode.drt));
            plan.getPlanElements().add(2, createDummyActivity(dummy_first_coord, population));
        }
    }

//    @Override
//    public void notifyIterationStarts(IterationStartsEvent event) {
//        Scenario scenario = event.getServices().getScenario();
//        LOG.warn("Now at Iteration Starts - scenario: " + scenario);
//        for (Person person : scenario.getPopulation().getPersons().values()) {
//            for (Plan plan : person.getPlans()) {
//                Activity firstAct = null;
//                Activity lastAct = null;
//                boolean foundFirst = false;
//                for (PlanElement el : plan.getPlanElements()) {
//                    LOG.warn(el);
//                }
//            }
//        }
//    }

//    private static Link wideSearchLink()
}
