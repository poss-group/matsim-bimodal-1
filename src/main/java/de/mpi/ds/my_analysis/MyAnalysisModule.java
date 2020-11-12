package de.mpi.ds.my_analysis;

import com.google.inject.Inject;
import de.mpi.ds.custom_transit_stop_handler.CustomTransitStopHandlerFactory;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.pt.TransitStopHandlerFactory;
import org.matsim.core.scenario.ScenarioUtils;

public class MyAnalysisModule extends AbstractModule {
    private final static Logger LOG = Logger.getLogger(MyAnalysisModule.class.getName());

    @Override
    public void install() {
        LOG.info("Initiating");
        this.addControlerListenerBinding().to(ServabilityListener.class);
        LOG.info("Finalizing");
    }
}
