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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class DrtPlanModifierStartupListener implements StartupListener {
    private final static Logger LOG = Logger.getLogger(DrtPlanModifierStartupListener.class.getName());
    @Inject
    Provider<TripRouter> tripRouterProvider;
//    @Inject
//    Controler controler;

    private static Node searchTransferNode(Node fromNode, Node toNode) {
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
            // Add closest connected nodes to queue (sorted by their distance to the toNode)
            queue.addAll(outLinks.stream().map(Link::getToNode)
                    .filter(e -> !visited.contains((e)))
                    .sorted(Comparator
                            .comparingDouble(n -> DistanceUtils.calculateDistance(n.getCoord(), toNode.getCoord())))
                    .collect(Collectors.toList()));
        }
        return null;
    }

//    private static boolean isPtStation(Stream<? extends Link> stream) {
//    }

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
//        Person testPerson = event.getServices().getScenario().getPopulation().getFactory().createPerson(Id
//        .createPersonId("-1"));
//        ActivityFacilitiesFactoryImpl activityFacilitiesFactory =  new ActivityFacilitiesFactoryImpl();
//        ActivityFacility fstAct = activityFacilitiesFactory.createActivityFacility(Id.create("-1", ActivityFacility
//        .class),Id.createLinkId("6029_6130"));
//        ActivityFacility scndAct = activityFacilitiesFactory.createActivityFacility(Id.create("-1",
//        ActivityFacility.class),Id.createLinkId("5524_5625"));
//        List<? extends PlanElement> routeList = tripRouter.calcRoute("car", fstAct,scndAct, 0, testPerson);
//        LOG.warn(routeList.get(0));
//        ((LegImpl) routeList.get(0)).travTime;
        //TODO replace drtrouter in InsertionCostCalculator with this

        Scenario sc = event.getServices().getScenario();
        Network network = sc.getNetwork();
        Map<Coord, Id<Node>> coordToNode = network.getNodes().entrySet().stream().collect(
                Collectors.toMap(e -> e.getValue().getCoord(),
                        Map.Entry::getKey));
        int count = 0;
        for (Person person : sc.getPopulation().getPersons().values()) {
            if (count % 1000 == 0) {
                LOG.info("Person" + count);
            }
            count++;
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
                if (!isPtStation(firstNode)) {
                    Node dummyFirstNode = searchTransferNode(firstNode, lastNode);
                    assert dummyFirstNode != null;
                    dummyFirstCoord = dummyFirstNode.getCoord();
                }
                if (!isPtStation(lastNode)) {
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
                                     Coord dummy_last_coord) {
        if (dummy_last_coord != null) {
            plan.getPlanElements().add(2, createDummyActivity(dummy_last_coord, population));
            plan.getPlanElements().add(3, population.getFactory().createLeg(TransportMode.drt));
        }
        if (dummy_first_coord != null) {
            plan.getPlanElements().add(1, population.getFactory().createLeg(TransportMode.drt));
            plan.getPlanElements().add(2, createDummyActivity(dummy_first_coord, population));
        }
    }
}
