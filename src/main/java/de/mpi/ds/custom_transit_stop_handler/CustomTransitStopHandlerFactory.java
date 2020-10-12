package de.mpi.ds.custom_transit_stop_handler;


import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.core.mobsim.qsim.pt.TransitStopHandler;
import org.matsim.core.mobsim.qsim.pt.TransitStopHandlerFactory;
import org.matsim.vehicles.Vehicle;

public class CustomTransitStopHandlerFactory implements TransitStopHandlerFactory {
//    private static final Logger LOG = Logger.getLogger(CustomTransitStopHandlerFactory.class.getName());

    @Override
    public TransitStopHandler createTransitStopHandler(Vehicle vehicle) {
//        LOG.warn("Creating new CustomTransitStopHandler");
        return new CustomTransitStopHandler();
    }
}
