package de.mpi.ds.my_analysis;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.util.distance.DistanceUtils;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.mobsim.qsim.qnetsimengine.vehicle_handler.VehicleHandler;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class PtVehicleDistanceTracker implements VehicleArrivesAtFacilityEventHandler, IterationEndsListener {
    private final static Logger LOG = Logger.getLogger(PtVehicleDistanceTracker.class.getName());
    private Map<Id<Vehicle>, Id<TransitStopFacility>> vehicleToLastStop = new HashMap<>();
    private Map<Id<TransitStopFacility>, Coord> transitStopFacilityToCoord = new HashMap<>();
    private double distance = 0;

    @Inject
    Scenario sc;

    public PtVehicleDistanceTracker() {}

    @Override
    public void handleEvent(VehicleArrivesAtFacilityEvent event) {
        Coord currCoord = transitStopFacilityToCoord.get(event.getFacilityId());
        if (currCoord == null) {
            Coord coord = ((BasicLocation) sc.getTransitSchedule().getFacilities().get(event.getFacilityId()))
                    .getCoord();
            transitStopFacilityToCoord.put(event.getFacilityId(), coord);
            currCoord = coord;
        }
        Coord lastCoord = transitStopFacilityToCoord.get(vehicleToLastStop.get(event.getVehicleId()));
        if (lastCoord != null) {
            distance += CoordUtils.calcEuclideanDistance(currCoord, lastCoord);
        }
        vehicleToLastStop.put(event.getVehicleId(), event.getFacilityId());
    }

    @Override
    public void reset(int iteration) {
        this.vehicleToLastStop.clear();
        this.transitStopFacilityToCoord.clear();
        this.distance = 0;
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        LOG.info("Cummulative Pt distance driven: " + distance);
//        String outputPath = event.getServices().getControlerIO().getIterationPath(event.getIteration());
        String outputPath = event.getServices().getControlerIO()
                .getIterationFilename(event.getIteration(), "CummulativePtDistance.txt");
        try {
            FileWriter fileWriter = new FileWriter(outputPath);
            fileWriter.write("#Distance\n"+distance);
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
