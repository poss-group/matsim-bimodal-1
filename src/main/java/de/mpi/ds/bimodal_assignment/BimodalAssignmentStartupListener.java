package de.mpi.ds.bimodal_assignment;

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

public class BimodalAssignmentStartupListener implements StartupListener {
    private static final Logger LOG = Logger.getLogger(BimodalAssignmentStartupListener.class.getName());
    private static final Random rand = new Random();
    private static final double cutoff_drt_pt = 0;

    @Override
    public void notifyStartup(StartupEvent event) {
        Scenario scenario = event.getServices().getScenario();
        Network network = scenario.getNetwork();
        Population population = scenario.getPopulation();
        List<Double> trainDeltas = network.getLinks().values().stream()
                .filter(e -> e.getAllowedModes().contains(TransportMode.train))
                .filter(e -> e.getFromNode().getCoord().getY() == 0)
                .map(e -> e.getCoord().getX())
                .sorted()
                .limit(2)
                .collect(Collectors.toList());
        double trainDelta = trainDeltas.get(1) - trainDeltas.get(0);
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
                assert firstAct != null;
                assert lastAct != null;
                modifyPlan(scenario, plan, firstAct, lastAct, trainDelta);
            }
        }
        // To get resulting plans in output directory
        PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation(), scenario.getNetwork());
        populationWriter.write("./output/grid_pre_planner_modified_plans.xml");
    }

    private void modifyPlan(Scenario scenario, Plan plan, Activity firstAct,
                            Activity lastAct, double trainDelta) {
        Population population = scenario.getPopulation();
        if (DistanceUtils.calculateDistance(firstAct.getCoord(), lastAct.getCoord()) < cutoff_drt_pt * trainDelta) {
            plan.getPlanElements().remove(1);
            plan.getPlanElements().add(1, population.getFactory().createLeg(TransportMode.drt));
        }
    }
}
