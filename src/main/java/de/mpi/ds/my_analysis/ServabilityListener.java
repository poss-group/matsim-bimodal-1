package de.mpi.ds.my_analysis;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.interfaces.Netsim;
import org.matsim.core.mobsim.qsim.interfaces.NetsimLink;
import org.matsim.core.mobsim.qsim.interfaces.NetsimNode;

import java.io.*;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;

public class ServabilityListener implements IterationStartsListener, IterationEndsListener {
    private final static Logger LOG = Logger.getLogger(ServabilityListener.class.getName());
    private MyActivityStartHandler myActivityStartHandler;

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        myActivityStartHandler = new MyActivityStartHandler(event.getServices().getScenario());
        event.getServices().getEvents().addHandler(myActivityStartHandler);
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        String eol = System.getProperty("line.separator");
        String outPath = event.getServices().getControlerIO().getOutputPath();

        try (Writer writer = new FileWriter(Paths.get(outPath, "trip_success.csv").toString())) {
            for (Map.Entry<Id<Person>, Boolean> entry : myActivityStartHandler.getSuccsessfullTrips().entrySet()) {
                writer.append(entry.getKey().toString())
                        .append(';')
                        .append(entry.getValue().toString())
                        .append(eol);
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }
}
