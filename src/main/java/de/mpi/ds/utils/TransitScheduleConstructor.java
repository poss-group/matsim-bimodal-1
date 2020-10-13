package de.mpi.ds.utils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TransitScheduleConstructor {
    private final static Logger LOG = Logger.getLogger(TransitScheduleConstructor.class.getName());
    private final int pt_interval;
    private final int n_xy;
    private final double delta_xy;
    private final double stop_length;
    private final double departure_delay;
    private final double transitStartTime;
    private final double transitEndTime;
    private final double transitIntervalTime;
    private TransitScheduleFactory transitScheduleFactory;
    private PopulationFactory populationFactory;
    private Network network;
    private Vehicles vehicles;
    private TransitSchedule schedule;
    private int route_counter = 0;
    private int stop_counter = 0;
    private int currRouteStopCount;

    public TransitScheduleConstructor(TransitScheduleFactory tsf,
                                      PopulationFactory pf, Network net, TransitSchedule ts, Vehicles vehicles, int pt_interval,
                                      double delta_xy,
                                      double departure_delay, double stop_length, double transitStartTime,
                                      double transitEndTime, double transitIntervalTime) {
        this.transitScheduleFactory = tsf;
        this.transitEndTime = transitEndTime;
        this.transitIntervalTime = transitIntervalTime;
        this.schedule = ts;
        this.populationFactory = pf;
        this.network = net;
        this.pt_interval = pt_interval;
        this.delta_xy = delta_xy;
        this.stop_length = stop_length;
        this.departure_delay = departure_delay;
        this.transitStartTime = transitStartTime;
        this.n_xy = (int) Math.sqrt(network.getNodes().size());
        this.vehicles = vehicles;
        createVehicles();
    }

    public void createVehicles() {
        VehicleType vehicleType = VehicleUtils.getFactory().createVehicleType(Id.create("1", VehicleType.class));
        vehicleType.setDescription("train");
        vehicleType.setNetworkMode("train");
        vehicleType.setLength(50);
        vehicleType.getCapacity().setSeats(10);
        vehicleType.getCapacity().setStandingRoom(10);
        vehicles.addVehicleType(vehicleType);
    }

    public void createLine(Node startNode, Node endNode, String direction) {
        ArrayList<Link> linkList = new ArrayList<>();
        TransitLine transitLine = transitScheduleFactory
                .createTransitLine(Id.create("Line".concat(String.valueOf(route_counter)), TransitLine.class));
        List<TransitRouteStop> transitRouteStopList = new ArrayList<>();
        currRouteStopCount = 0;
        moveFromTo(linkList, startNode, endNode, transitRouteStopList, direction);
        moveFromTo(linkList, endNode, startNode, transitRouteStopList, direction);

        List<Id<Link>> idLinkList = linkList.stream().map(Identifiable::getId).collect(Collectors.toList());
        Id<TransitRoute> tr_id = Id.create(String.valueOf(route_counter), TransitRoute.class);
        NetworkRoute networkRoute = populationFactory.getRouteFactories()
                .createRoute(NetworkRoute.class, idLinkList.get(0),
                        idLinkList.get(idLinkList.size() - 1));
        networkRoute.setLinkIds(idLinkList.get(0), idLinkList.subList(1, linkList.size() - 1),
                idLinkList.get(idLinkList.size() - 1));
        TransitRoute transitRoute = transitScheduleFactory.createTransitRoute(tr_id, networkRoute,
                transitRouteStopList, "train");
        createDepartures(transitRoute);
        transitLine.addRoute(transitRoute);

        schedule.addTransitLine(transitLine);
        route_counter++;
    }

    private void moveFromTo(ArrayList<Link> linkList, Node startNode, Node endNode,
                            List<TransitRouteStop> transitRouteStopList, String direction) {
        int dir;
        if (direction.equals("x")) {
            dir = endNode.getCoord().getX() > startNode.getCoord().getX() ? 1 : -1;
        } else {
            dir = endNode.getCoord().getY() > startNode.getCoord().getY() ? 1 : -1;
        }
        Optional<? extends Link> newLink = null;
        Node currNode = startNode;
        do {
            Node tempNode = currNode;
            if (direction.equals("x")) {
                newLink = currNode.getOutLinks().values().stream()
                        .filter(l -> l.getAllowedModes().contains("train"))
                        .filter(l -> l.getToNode().getCoord().getY() == startNode.getCoord().getY() && Math
                                .signum(l.getToNode()
                                        .getCoord().getX() - tempNode.getCoord().getX()) == dir).findFirst();
                if (newLink.isPresent()) {
                    linkList.add(newLink.get());
                    currNode = newLink.get().getToNode();
                    if ((currNode.getCoord().getX() / delta_xy + pt_interval / 2) % pt_interval == 0) {
                        createStop(transitRouteStopList, currNode, newLink.get());
                    }
                }
            } else {
                newLink = currNode.getOutLinks().values().stream()
                        .filter(l -> l.getAllowedModes().contains("train"))
                        .filter(l -> l.getToNode().getCoord().getX() == startNode.getCoord().getX() && Math
                                .signum(l.getToNode()
                                        .getCoord().getY() - tempNode.getCoord().getY()) == dir).findFirst();
                if (newLink.isPresent()) {
                    linkList.add(newLink.get());
                    currNode = newLink.get().getToNode();
                    if ((currNode.getCoord().getY() / delta_xy + pt_interval / 2) % pt_interval == 0) {
                        createStop(transitRouteStopList, currNode, newLink.get());
                    }
                }
            }
        }
        while (currNode != endNode);
    }

    private void createStop(List<TransitRouteStop> transitRouteStopList, Node currNode, Link link) {
        TransitStopFacility transitStopFacility = transitScheduleFactory.createTransitStopFacility(
                Id.create(String.valueOf(stop_counter), TransitStopFacility.class), currNode.getCoord(), false);
        transitStopFacility.setLinkId(link.getId());
        TransitRouteStop transitrouteStop = transitScheduleFactory
                .createTransitRouteStop(transitStopFacility, currRouteStopCount * departure_delay,// - stop_length,
                        currRouteStopCount * departure_delay);
        transitrouteStop.setAwaitDepartureTime(true);
        transitRouteStopList.add(transitrouteStop);
        schedule.addStopFacility(transitStopFacility);
        currRouteStopCount++;
        stop_counter++;
    }

    private void createDepartures(TransitRoute transitRoute) {
        double time = transitStartTime;
        int i = 0;
        int transportersPerLine = (int) Math
                .ceil((departure_delay * (n_xy * 2 - 1)) / transitIntervalTime); // calculate how many
        // transporters are presented on a line maximally
        while (time < transitEndTime) {
            Departure dep = transitScheduleFactory.createDeparture(Id.create(String.valueOf(i), Departure.class), time);
            Id<Vehicle> vehicleId =
                    Id.create("tr_".concat(
                            String.valueOf(route_counter).concat("_").concat(String.valueOf(i % transportersPerLine))),
                            Vehicle.class);
            dep.setVehicleId(vehicleId);
            if (!vehicles.getVehicles().containsKey(vehicleId)) {
                vehicles.addVehicle(VehicleUtils.createVehicle(Id.create(vehicleId, Vehicle.class),
                        vehicles.getVehicleTypes().entrySet().iterator().next().getValue()));
            }
            transitRoute.addDeparture(dep);
            i++;

            time += transitIntervalTime;
        }
    }

    public TransitSchedule getSchedule() {
        return schedule;
    }

    public Vehicles getVehicles() {
        return vehicles;
    }
}
