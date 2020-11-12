package de.mpi.ds.my_analysis;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class MyActivityStartHandler implements ActivityStartEventHandler {
    private static final Logger LOG = Logger.getLogger(MyActivityStartHandler.class.getName());

    private Map<Id<Person>, Boolean> succsessfullTrips;
    private Map<Id<Person>, Activity> lastActs;

    public MyActivityStartHandler(Scenario sc) {
        lastActs = sc.getPopulation().getPersons().entrySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), getLastAct(e.getValue())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        succsessfullTrips = sc.getPopulation().getPersons().keySet().stream()
                .map(e -> new AbstractMap.SimpleEntry<>(e, Boolean.FALSE))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Activity getLastAct(Person person) {
        Activity lastAct = (Activity) person.getSelectedPlan().getPlanElements().stream()
                .filter(pe -> pe instanceof Activity)
                .reduce((fst, scnd) -> scnd)
                .orElse(null);

        assert lastAct != null;
        return lastAct;
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        if (event.getActType().equals("dummy")) {
            Id<Link> eventLink = event.getLinkId();
            Id<Link> finalLink = lastActs.get(event.getPersonId()).getLinkId();
            if (eventLink.equals(finalLink)) {
                succsessfullTrips.replace(event.getPersonId(), Boolean.TRUE);
            }
        }
    }

    public Map<Id<Person>, Boolean> getSuccsessfullTrips() {
        return succsessfullTrips;
    }
}
