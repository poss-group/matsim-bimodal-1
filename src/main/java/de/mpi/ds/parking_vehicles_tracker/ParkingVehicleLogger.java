package de.mpi.ds.parking_vehicles_tracker;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.vehicles.Vehicle;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipOutputStream;

public class ParkingVehicleLogger implements VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler,
        IterationEndsListener {

    private final static Logger LOG = Logger.getLogger(ParkingVehicleLogger.class.getName());
    private static List<ParkEvent> parkEvents = new ArrayList<>();
    private static Map<Id<Vehicle>, ParkEvent> currentlyParkingMap = new HashMap<>();
    @Inject
    Scenario sc;

    private class ParkEvent {
        private Id<Vehicle> vehicleId;
        private Coord coord;
        private double startTime;
        private double endTime;

        ParkEvent(Id<Vehicle> vehicleId, Coord coord, double startTime, double endTime) {
            this.vehicleId = vehicleId;
            this.coord = coord;
            this.startTime = startTime;
            this.endTime = endTime;
        }

    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        if (event.getVehicleId().toString().contains("drt")) {
            ParkEvent parkEvent = currentlyParkingMap.remove(event.getVehicleId());
            Coord coord = sc.getNetwork().getLinks().get(event.getLinkId()).getCoord();
            if (parkEvent != null) {
                parkEvent.endTime = event.getTime();
                if (parkEvent.coord == null) {
                    parkEvent.coord = coord;
                }
            } else {
                parkEvent = new ParkEvent(event.getVehicleId(), coord, 0, event.getTime());
            }
            parkEvents.add(parkEvent);
        }
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        if (event.getVehicleId().toString().contains("drt")) {
            ParkEvent parkEvent = new ParkEvent(event.getVehicleId(),
                    sc.getNetwork().getLinks().get(event.getLinkId()).getCoord(), event.getTime(), -1);
            currentlyParkingMap.put(event.getVehicleId(), parkEvent);
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        for (ParkEvent parkEvent : currentlyParkingMap.values()) {
            parkEvent.endTime = event.getServices().getConfig().qsim().getEndTime().seconds();
            parkEvents.add(parkEvent);
        }

        String path = event.getServices().getControlerIO()
                .getIterationFilename(event.getIteration(),
                        "drt_park_tracking.csv.gz");

        writeOutCsv(parkEvents, path);
    }

    private void writeOutCsv(List<ParkEvent> parkEvents, String path) {
        try (OutputStream outputStream = new FileOutputStream(path)) {
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);
            Writer writer = new OutputStreamWriter(gzipOutputStream);

            StringBuilder sb = new StringBuilder();
            sb.append("VehId,");
            sb.append("X,");
            sb.append("Y,");
            sb.append("startTimePark,");
            sb.append("endTimePark");
            sb.append('\n');

            for (ParkEvent parkEvent : parkEvents) {
                sb.append(parkEvent.vehicleId).append(",")
                        .append(parkEvent.coord.getX()).append(",")
                        .append(parkEvent.coord.getY()).append(",")
                        .append(parkEvent.startTime).append(",")
                        .append(parkEvent.endTime).append("\n");
            }

            try {
                writer.write(sb.toString());
            } finally {
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
