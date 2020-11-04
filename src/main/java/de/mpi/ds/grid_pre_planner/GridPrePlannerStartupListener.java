package de.mpi.ds.grid_pre_planner;

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

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GridPrePlannerStartupListener implements StartupListener {
    private static final Logger LOG = Logger.getLogger(GridPrePlannerStartupListener.class.getName());
    private static final Random rand = new Random();
    private static final double cutoff_drt_pt = 2;

    private static Node searchTransferNode(Node actNode,
                                           List<Coord> transitStopCoords) {
        Queue<Node> queue = new LinkedList<>();
        List<Node> visited = new ArrayList<>();
        queue.add(actNode);
        while (!queue.isEmpty()) {
            Node current = queue.remove();
            visited.add(current);
            Collection<? extends Link> outLinks = null;
            outLinks = current.getOutLinks().values();
            if (transitStopCoords.contains(current.getCoord())) {
                return current;
            }
            // Add closest connected nodes to queue (sorted by their distance to the toNode)
            queue.addAll(outLinks.stream().map(Link::getToNode)
                    .filter(e -> !visited.contains(e))
                    .sorted(Comparator
                            .comparingDouble(n -> DistanceUtils.calculateDistance(n.getCoord(), actNode.getCoord())))
                    .collect(Collectors.toList()));
        }
        return null;
    }

    @Override
    public void notifyStartup(StartupEvent event) {
        rand.setSeed(event.getServices().getConfig().global().getRandomSeed());
        Scenario scenario = event.getServices().getScenario();
        Network network = scenario.getNetwork();
        List<Coord> transitStopCoords = scenario.getTransitSchedule().getTransitLines().values().stream()
                .map(tl -> tl.getRoutes().values().stream()
                        .map(tr -> tr.getStops().stream().map(stop -> stop.getStopFacility().getCoord())))
                .flatMap(Function.identity())
                .flatMap(Function.identity())
                .collect(Collectors.toList());
        List<Double> trainDeltas = network.getLinks().values().stream()
                .filter(e -> e.getAllowedModes().contains(TransportMode.train))
                .filter(e -> e.getFromNode().getCoord().getY() == 0)
                .map(e -> e.getCoord().getX())
                .sorted()
                .limit(2)
                .collect(Collectors.toList());
        double trainDelta = trainDeltas.get(1) - trainDeltas.get(0);
        Map<Coord, Id<Node>> coordToNode = network.getNodes().entrySet().stream().collect(
                Collectors.toMap(e -> e.getValue().getCoord(),
                        Map.Entry::getKey));
        Population population = scenario.getPopulation();
        for (Person person : population.getPersons().values()) {
            for (Plan plan : person.getPlans()) {
                Activity firstAct = null;
                Activity lastAct = null;
                boolean foundFirst = false;
                for (PlanElement planElement : plan.getPlanElements()) {
                    if (planElement instanceof Activity) {
                        if (foundFirst) {
                            lastAct = (Activity) planElement;
                        } else {
                            firstAct = (Activity) planElement;
                            foundFirst = true;
                        }
                    }
                }
                modifyPlan(scenario, plan, firstAct, lastAct, trainDelta, coordToNode, transitStopCoords);
            }
        }
        // To get resulting plans in output directory
        PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
        populationWriter.write("./output/grid_pre_planner_modified_plans.xml");
    }

    private void modifyPlan(Scenario scenario, Plan plan, Activity firstAct,
                            Activity lastAct, double trainDelta,
                            Map<Coord, Id<Node>> coordToNode,
                            List<Coord> transitStopCoords) {
        Population population = scenario.getPopulation();
        Network network = scenario.getNetwork();
        double x1 = firstAct.getCoord().getX();
        double x2 = lastAct.getCoord().getX();
        double y1 = firstAct.getCoord().getY();
        double y2 = lastAct.getCoord().getY();
        if (DistanceUtils.calculateDistance(firstAct.getCoord(), lastAct.getCoord()) > cutoff_drt_pt * trainDelta) {
            if (Math.abs(x2 - x1) > cutoff_drt_pt * trainDelta && Math.abs(y2 - y1) > cutoff_drt_pt * trainDelta) {
                // Trips longer than distance btw. transit routes:
                Coord newCoord = null;
                if (rand.nextInt(2) == 0) {
                    Node firstNode = network.getNodes().get(coordToNode.get(new Coord(x2, y1)));
                    newCoord = searchTransferNode(firstNode, transitStopCoords).getCoord();
                } else {
                    Node lastNode = network.getNodes().get(coordToNode.get(new Coord(x1, y2)));
                    newCoord = searchTransferNode(lastNode, transitStopCoords).getCoord();
                }
                Activity newActivity = population.getFactory().createActivityFromCoord("dummy", newCoord);
//            population.getFactory().createActivityFromLinkId("dummy", Id); //TODO transfer loc on link
                newActivity.setMaximumDuration(0);
                Leg newLeg = population.getFactory().createLeg(TransportMode.pt);
                plan.getPlanElements().add(2, newActivity);
                plan.getPlanElements().add(3, newLeg);


//                Coord firstCoord = network.getNodes().get(coordToNode.get(firstAct.getCoord())).getCoord();
//                Coord drtDummyCord = newCoord
//                newActivity = population.getFactory().createActivityFromCoord("dummy", drtDummyCord);
//                newActivity.setMaximumDuration(0);
//                plan.getPlanElements().add(1,)
            }
        } else {
            // Trips shorter than distance btw. transit routes:
            plan.getPlanElements().remove(1);
            plan.getPlanElements().add(1, population.getFactory().createLeg(TransportMode.drt));
        }
    }
}
