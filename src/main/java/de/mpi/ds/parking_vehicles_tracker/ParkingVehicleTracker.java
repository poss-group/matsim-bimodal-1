package de.mpi.ds.parking_vehicles_tracker;

import de.mpi.ds.DrtTrajectoryAnalyzer.TrajectoryLinkLogger;
import org.apache.log4j.Logger;
import org.matsim.core.controler.AbstractModule;

public class ParkingVehicleTracker extends AbstractModule {
    private final static Logger LOG = Logger.getLogger(ParkingVehicleTracker.class.getName());

    @Override
    public void install() {
        LOG.info("Initiating");
        this.addControlerListenerBinding().to(ParkingVehicleLogger.class);
        this.addEventHandlerBinding().to(ParkingVehicleLogger.class);
//        this.addMobsimListenerBinding().to(ParkingVehicleLogger.class);;
        LOG.info("Finalizing");
    }
}
