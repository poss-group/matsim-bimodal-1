package de.mpi.ds.DrtTrajectoryAnalyzer;

import org.apache.log4j.Logger;
import org.matsim.core.controler.AbstractModule;

public class DrtTrajectoryAnalyzer extends AbstractModule {
    private final static Logger LOG = Logger.getLogger(DrtTrajectoryAnalyzer.class.getName());

    @Override
    public void install() {
        LOG.info("Initiating");
        this.addControlerListenerBinding().to(TrajectoryLinkLogger.class);
        this.addEventHandlerBinding().to(TrajectoryLinkLogger.class);
        LOG.info("Finalizing");
    }
}
