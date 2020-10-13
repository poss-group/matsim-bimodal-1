package de.mpi.ds.custom_transit_stop_handler;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.mobsim.qsim.pt.PTPassengerAgent;
import org.matsim.core.mobsim.qsim.pt.PassengerAccessEgress;
import org.matsim.core.mobsim.qsim.pt.TransitStopHandler;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.List;

public class CustomTransitStopHandler implements TransitStopHandler {
//    private static final Logger LOG = Logger.getLogger(CustomTransitStopHandler.class.getName());

    @Override
    public double handleTransitStop(TransitStopFacility stop, double now, List<PTPassengerAgent> leavingPassengers,
                                    List<PTPassengerAgent> enteringPassengers, PassengerAccessEgress accessEgress,
                                    MobsimVehicle vehicle) {
        int cntEgress = leavingPassengers.size();
        int cntAccess = enteringPassengers.size();
//        double stopTime = 0;
        if ((cntAccess > 0) || (cntEgress > 0)) {
//            stopTime = cntAccess*40+cntEgress*20;
            for (PTPassengerAgent passenger : leavingPassengers) {
                accessEgress.handlePassengerLeaving(passenger, vehicle, stop.getLinkId(), now);
            }
            for (PTPassengerAgent passenger : enteringPassengers) {
                accessEgress.handlePassengerEntering(passenger, vehicle, stop.getId(), now);
            }
        }
//        LOG.warn("Returning zero");
        return 0;
    }
}
