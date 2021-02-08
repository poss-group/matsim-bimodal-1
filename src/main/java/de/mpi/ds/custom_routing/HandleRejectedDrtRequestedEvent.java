package de.mpi.ds.custom_routing;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.DefaultActivityTypes;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.population.routes.RouteFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.vis.snapshotwriters.VisMobsim;

import java.util.List;
import java.util.Map;

public class HandleRejectedDrtRequestedEvent implements PassengerRequestRejectedEventHandler,
        MobsimInitializedListener {

    private final static Logger LOG = Logger.getLogger(HandleRejectedDrtRequestedEvent.class.getName());

    private Map<Id<Person>, MobsimAgent> agents;
    private PopulationFactory populationFactory;

    @Inject private LeastCostPathCalculatorFactory pathCalculatorFactory ;

    public HandleRejectedDrtRequestedEvent() {}

    @Override
    public void handleEvent(PassengerRequestRejectedEvent event) {
        Id<Person> personId = Id.createPersonId(event.getAttributes().get("person"));
        MobsimAgent mobsimAgent = agents.get(personId);
        Plan modifiablePlan = WithinDayAgentUtils.getModifiablePlan(mobsimAgent);
        List<PlanElement> planElements = modifiablePlan.getPlanElements();
//        Activity firstActivity = (Activity) planElements.get(0);
        Activity lastActivity = (Activity) planElements.get(planElements.size() - 1);
        Integer planElementsIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(mobsimAgent);
        for (int i=planElements.size()-1; i > planElementsIndex; i--) {
            planElements.remove(i);
        }

//        firstActivity.setEndTime(event.getTime()+1);
//        modifiablePlan.addActivity(firstActivity);
//        planElements.add(planElementsIndex+1, firstActivity);
//        Leg newLeg = populationFactory.createLeg(TransportMode.walk);
//        newLeg.setRoute(route);
//        planElements.add(planElementsIndex, lastActivity);
//        planElements.add(planElementsIndex, newLeg);
//
//        firstActivity.setLinkId(mobsimAgent.getCurrentLinkId());
//        firstActivity.setEndTime(event.getTime()+1);
//        planElements.add(planElementsIndex, firstActivity);

//        modifiablePlan.addActivity(lastActivity);

//        while (!planElements.isEmpty()) {
//            planElements.remove(0);
//        }
        Activity newActivity = populationFactory.createActivityFromLinkId("dummy", mobsimAgent.getCurrentLinkId());
        newActivity.setEndTime(event.getTime()+10);
        newActivity.setStartTime(event.getTime());
        Leg newLeg = populationFactory.createLeg(TransportMode.drt);
//        Route route = new GenericRouteImpl(mobsimAgent.getCurrentLinkId(), lastActivity.getLinkId());
//        route.setDistance(42);
//        newLeg.setRoute(route);
        planElements.add(planElementsIndex, lastActivity);
        planElements.add(planElementsIndex, newLeg);
        planElements.add(planElementsIndex, newActivity);

        mobsimAgent.endLegAndComputeNextState(event.getTime());
//        mobsimAgent.endActivityAndComputeNextState(event.getTime());
        planElementsIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(mobsimAgent);
        WithinDayAgentUtils.resetCaches(mobsimAgent);
    }

    @Override
    public void notifyMobsimInitialized(MobsimInitializedEvent e) {
        VisMobsim visMobsim = (VisMobsim) e.getQueueSimulation();
        agents = visMobsim.getAgents();
        populationFactory = ((Netsim) e.getQueueSimulation()).getScenario().getPopulation().getFactory();
    }
}
