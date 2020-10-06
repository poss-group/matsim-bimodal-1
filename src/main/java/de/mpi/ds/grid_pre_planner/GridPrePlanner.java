package de.mpi.ds.grid_pre_planner;

import org.apache.log4j.Logger;
import org.matsim.core.controler.AbstractModule;

public class GridPrePlanner extends AbstractModule {
    private final static Logger LOG = Logger.getLogger(GridPrePlanner.class.getName());

    @Override
    public void install() {
        LOG.info("Initiating");
        this.addControlerListenerBinding().to(GridPrePlannerStartupListener.class);
        LOG.info("Finalizing");
    }
}
