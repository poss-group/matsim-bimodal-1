package de.mpi.ds.custom_transit_stop_handler;

import de.mpi.ds.grid_pre_planner.GridPrePlannerStartupListener;
import org.apache.log4j.Logger;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.pt.TransitStopHandler;
import org.matsim.core.mobsim.qsim.pt.TransitStopHandlerFactory;

public class CustomTransitStopHandlerModule extends AbstractQSimModule {
    private final static Logger LOG = Logger.getLogger(CustomTransitStopHandlerModule.class.getName());

//    @Override
//    public void install() {
//        LOG.info("Initiating");
////        this.bindEventsManager().to(CustomTransitStopHandler.class);
//
//        this.bind(TransitStopHandlerFactory.class).to(CustomTransitStopHandlerFactory.class);
//        LOG.info("Finalizing");
//    }

    @Override
    protected void configureQSim() {
        LOG.info("Initiating");
//        this.bindEventsManager().to(CustomTransitStopHandler.class);

        this.bind(TransitStopHandlerFactory.class).to(CustomTransitStopHandlerFactory.class);
        LOG.info("Finalizing");
    }
}
