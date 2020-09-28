package de.mpi.ds.matsim_bimodal.drt_plan_modification;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.checkerframework.common.value.qual.IntRangeFromNonNegative;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;

import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

class DrtPlanModifierStartupListener implements StartupListener {
    private final static Logger LOG = Logger.getLogger(DrtPlanModifierStartupListener.class.getName());

    @Override
    public void notifyStartup(StartupEvent event) {
        LOG.info("Modifying plans...");
        LOG.warn("This module only supports grid networks with x/y coordinates spacing 1000 and pt available on x or y=n*1000 with n odd");
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
                Coord dummy_first_coord = null;
                Coord dummy_last_coord = null;
                assert firstAct != null;
                assert lastAct != null;
//                LOG.warn(network.getLinks().get(coordToLink.get(firstAct.getCoord())).getAllowedModes());
//                if (!network.getNodes().get(coordToNode.get(firstAct.getCoord())).getOutLinks().values().stream().anyMatch(e -> e.getAllowedModes().contains("pt"))) {
//                    dummy_first_coord = searchTransferLoc(firstAct.getCoord(), lastAct.getCoord());
//                }
//                if (!network.getNodes().get(coordToNode.get(lastAct.getCoord())).getOutLinks().values().stream().anyMatch(e -> e.getAllowedModes().contains("pt"))) {
//                    dummy_last_coord = searchTransferLoc(lastAct.getCoord(), firstAct.getCoord());
//                }
                if (firstAct.getCoord().getX() / 1000 % 2 == 0 && firstAct.getCoord().getY() / 1000 % 2 == 0) {
                    dummy_first_coord = searchTransferLoc(firstAct.getCoord(), lastAct.getCoord());
                }
                if (lastAct.getCoord().getX() / 1000 % 2 == 0 && lastAct.getCoord().getY() / 1000 % 2 == 0) {
                    dummy_last_coord = searchTransferLoc(lastAct.getCoord(), firstAct.getCoord());
                }
                insertTransferStops(plan, sc.getPopulation(), dummy_first_coord, dummy_last_coord);
            }
        }
        // To get resulting plans in output directory
        PopulationWriter populationWriter = new PopulationWriter(sc.getPopulation(), sc.getNetwork());
        populationWriter.write("./output/drt_plan_modified_plans.xml");
    }

    private void insertTransferStops(Plan plan, Population population, Coord dummy_first_coord, Coord dummy_last_coord) {
        int size = plan.getPlanElements().size();
        for (int i = 0, counter = 0; i < size; i++) {
            PlanElement el = plan.getPlanElements().get(i);
            if (el instanceof Activity) {
                if (counter == 0 && dummy_first_coord != null) {
                    plan.getPlanElements().add(i + 1, population.getFactory().createLeg(TransportMode.drt));
                    plan.getPlanElements().add(i + 2, createDummyActivity(dummy_first_coord, population));
                    i += 2;
                } else if (counter == 1 && dummy_last_coord != null) {
                    plan.getPlanElements().add(i, population.getFactory().createLeg(TransportMode.drt));
                    plan.getPlanElements().add(i, createDummyActivity(dummy_last_coord, population));
                    i += 2;
                }
                counter++;
            }
        }
    }

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

    private static Activity createDummyActivity(Coord location, Population population) {
        Activity activity = population.getFactory().createActivityFromCoord("dummy", location);
        activity.setMaximumDuration(0);
        return activity;
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
