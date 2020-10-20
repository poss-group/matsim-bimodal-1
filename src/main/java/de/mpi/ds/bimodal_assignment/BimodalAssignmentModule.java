package de.mpi.ds.bimodal_assignment;

import org.apache.log4j.Logger;
import org.matsim.core.controler.AbstractModule;

public class BimodalAssignmentModule extends AbstractModule {
    private final static Logger LOG = Logger.getLogger(BimodalAssignmentModule.class.getName());

    @Override
    public void install() {
        LOG.info("Initiating");
        this.addControlerListenerBinding().to(BimodalAssignmentStartupListener.class);
        LOG.info("Finalizing");
    }
}
