package de.mpi.ds.utils;

import com.google.common.base.Function;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import static de.mpi.ds.utils.GeneralUtils.doubleCloseToZero;
import static de.mpi.ds.utils.GeneralUtils.getDirectionOfLink;
import static de.mpi.ds.utils.ScenarioCreator.*;

public class TransitScheduleConstructor implements UtilComponent {
    private final static Logger LOG = Logger.getLogger(TransitScheduleConstructor.class.getName());
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
    private int currRouteStopCount;
    private double freeSpeedTrainForSchedule;
    private double departureIntervalTime;

    public TransitScheduleConstructor(TransitScheduleFactory tsf, PopulationFactory pf, Network net, TransitSchedule ts,
                                      Vehicles vehicles, double departure_delay, double stop_length,
                                      double transitStartTime, double transitEndTime, double transitIntervalTime,
                                      double freeSpeedTrainForSchedule, double departureIntervalTime) {
        this.transitScheduleFactory = tsf;
        this.transitEndTime = transitEndTime;
        this.transitIntervalTime = transitIntervalTime;
        this.schedule = ts;
        this.populationFactory = pf;
        this.network = net;
        this.stop_length = stop_length;
        this.departure_delay = departure_delay;
        this.transitStartTime = transitStartTime;
        this.vehicles = vehicles;
        this.freeSpeedTrainForSchedule = freeSpeedTrainForSchedule;
        this.departureIntervalTime = departureIntervalTime;
        createVehicles();
    }

    public void createVehicles() {
        VehicleType vehicleType = VehicleUtils.getFactory().createVehicleType(Id.create("1", VehicleType.class));
        vehicleType.setDescription("train");
        vehicleType.setNetworkMode("train");
        vehicleType.setMaximumVelocity(freeSpeedTrainForSchedule);
        vehicleType.setLength(10);
        vehicleType.getCapacity().setSeats(10000);
        vehicleType.getCapacity().setStandingRoom(0);
        vehicles.addVehicleType(vehicleType);
    }

    public void createLine(Link startLink) {
        ArrayList<Link> linkList = new ArrayList<>();
        TransitLine transitLine = transitScheduleFactory
                .createTransitLine(Id.create("Line".concat(String.valueOf(route_counter)), TransitLine.class));
        List<TransitRouteStop> transitRouteStopList = new ArrayList<>();
        currRouteStopCount = 0;

        try {
//        moveFromTo(linkList, startNode, endNode, transitRouteStopList, direction, true);
//        moveFromTo(linkList, endNode, startNode, transitRouteStopList, direction, false);
            movePeriodic(linkList, startLink, transitRouteStopList, true);
        } catch (Exception e) {
            e.printStackTrace();
        }

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


    private int getDirection(Node startNode, Node endNode, String direction) throws Exception {
        if (direction.equals("x")) {
            return endNode.getCoord().getX() > startNode.getCoord().getX() ? 1 : -1;
        } else if (direction.equals("y")) {
            return endNode.getCoord().getY() > startNode.getCoord().getY() ? 1 : -1;
        } else {
            throw new Exception("direction must be either \"x\" or \"y\"");
        }
    }

    private void movePeriodic(ArrayList<Link> linkList, Link startLink, List<TransitRouteStop> transitRouteStopList,
                              boolean addFirstAsTransitStop) throws Exception {
        boolean addAsTransitStop = addFirstAsTransitStop;
//        int forwardBackwardDetermination = getDirection(startNode, forwardBackwardIndicatorNode, direction);
//        Node lastNode = getPredecessor(startNode, direction, forwardBackwardDetermination);
//        Node lastNode = forwardBackwardIndicatorNode; // Because Periodic BC
        Link currLink = startLink;
        do {
            currLink = moveToNextLinkPeriodic(linkList, startLink, currLink, transitRouteStopList,
                    addAsTransitStop);
            addAsTransitStop = true;
        }
        while (!currLink.equals(startLink));
    }

    private Node getPredecessor(Node startNode, String direction, int forwardBackwardDetermination) {
        Function<Comparator<Node>, BinaryOperator<Node>> minMax = null;
        if (forwardBackwardDetermination == 1)
            minMax = BinaryOperator::minBy;
        else
            minMax = BinaryOperator::maxBy;

        if (direction.equals("x")) {
            Coord startNodePlusDirection = new Coord(startNode.getCoord().getX() + forwardBackwardDetermination,
                    startNode.getCoord().getY());
            return startNode.getOutLinks().values().stream()
                    .filter(l -> l.getAllowedModes().contains(TransportMode.train))
                    .map(Link::getToNode)
                    .filter(n -> n.getCoord().getY() == startNode.getCoord().getY())
                    .reduce(Objects.requireNonNull(minMax.apply(Comparator.comparing(n -> CoordUtils
                            .calcEuclideanDistance(n.getCoord(), startNodePlusDirection)))))
                    .orElseThrow();
        }
        if (direction.equals("y")) {
            Coord startNodePlusDirection = new Coord(startNode.getCoord().getX(),
                    startNode.getCoord().getY() + forwardBackwardDetermination);
            return startNode.getOutLinks().values().stream()
                    .filter(l -> l.getAllowedModes().contains(TransportMode.train))
                    .map(Link::getToNode)
                    .filter(n -> n.getCoord().getX() == startNode.getCoord().getX())
                    .reduce(Objects.requireNonNull(minMax.apply(Comparator
                            .comparing(n -> CoordUtils.calcEuclideanDistance(n.getCoord(), startNodePlusDirection)))))
                    .orElseThrow();
        }
        return null;
    }

    private Link moveToNextLinkPeriodic(ArrayList<Link> linkList, Link startLink, Link currLink,
                                        List<TransitRouteStop> transitRouteStopList,
                                        boolean addAsTransitStop) throws Exception {
        Link newLink = null;
        Node toNode = currLink.getToNode();
        Node fromNode = currLink.getFromNode();
        if (fromNode.getAttributes().getAttribute(IS_STATION_NODE).equals(true) && addAsTransitStop) {
            if (currLink.getAttributes().getAttribute(PERIODIC_LINK).equals(false)) {
                createStop(transitRouteStopList, currLink,
                        currRouteStopCount * departure_delay - stop_length, currRouteStopCount * departure_delay);
            } else {
                createStop(transitRouteStopList, linkList.get(linkList.size() - 1),
                        currRouteStopCount * departure_delay - stop_length, currRouteStopCount * departure_delay);
            }
        }

        List<Link> newLinkCandidates = toNode.getOutLinks().values().stream()
                .filter(l -> l.getAllowedModes().contains(NETWORK_MODE_TRAIN))
                .filter(l -> l.getAttributes().getAttribute(PERIODIC_LINK).equals(false))
                .filter(l -> doubleCloseToZero(getDirectionOfLink(startLink) - getDirectionOfLink(l)))
                .collect(Collectors.toList());
//        assert (newLinkCandidates.size() == 1) : "Expected to encounter only one link in same direction!";
        if (newLinkCandidates.isEmpty()) {
            try {
                newLinkCandidates.add(toNode.getOutLinks().values().stream()
                        .filter(l -> l.getAllowedModes().contains(NETWORK_MODE_TRAIN))
                        // Filter so that they have to be periodic
                        .filter(l -> l.getAttributes().getAttribute(PERIODIC_LINK).equals(true))
                        // Filter so that only links are relevant which do have the same direction modulo 180°
                        .filter(l -> doubleCloseToZero(
                                (getDirectionOfLink(l) - getDirectionOfLink(startLink) + Math.PI) %
                                        (2 * Math.PI)))
                        .findAny().orElseThrow());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        newLink = newLinkCandidates.get(0);
        // Add last node so connection becomes periodic if start link is reached
        linkList.add(currLink);
        if (newLink.equals(startLink) && toNode.getAttributes().getAttribute(IS_STATION_NODE).equals(true)) {
            linkList.add(newLink);
            createStop(transitRouteStopList, newLink,
                    (currRouteStopCount - 1) * departure_delay - stop_length + 5,
                    (currRouteStopCount - 1) * departure_delay + 5);
        }
        return newLink;
    }

    private void createStop(List<TransitRouteStop> transitRouteStopList, Link currLink,
                            double arrivalDelay, double departureDelay) {
        Id<TransitStopFacility> stopId = Id
                .create(String.valueOf(currLink.getId()) + "_trStop", TransitStopFacility.class);
        TransitStopFacility transitStopFacility = null;
        if (!schedule.getFacilities().containsKey(stopId)) {
            transitStopFacility = transitScheduleFactory.createTransitStopFacility(
                    stopId, currLink.getCoord(), false);
            transitStopFacility.setLinkId(currLink.getId());
            schedule.addStopFacility(transitStopFacility);
        } else {
            transitStopFacility = schedule.getFacilities().get(stopId);
        }
        TransitRouteStop transitrouteStop = transitScheduleFactory
                .createTransitRouteStop(transitStopFacility, arrivalDelay, departureDelay);
        transitrouteStop.setAwaitDepartureTime(true);
        transitRouteStopList.add(transitrouteStop);
        currRouteStopCount++;
    }

    private void createDepartures(TransitRoute transitRoute) {
        double time = transitStartTime;
        int i = 0;
        boolean first_dep = true;
        int transportersPerLine = (int) Math.ceil(transitIntervalTime / departureIntervalTime);
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

            if (first_dep) {
//                time += transitIntervalTime / 4;
                time += departureIntervalTime;
                first_dep = false;
            } else {
//                time += transitIntervalTime * 3/4;
                time += departureIntervalTime;
                first_dep = true;
            }
        }
    }

    public TransitSchedule getSchedule() {
        return schedule;
    }

    public Vehicles getVehicles() {
        return vehicles;
    }
}
