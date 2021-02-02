package de.mpi.ds.utils;

import com.google.common.base.Function;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
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

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

public class TransitScheduleConstructor implements UtilComponent {
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
                                      PopulationFactory pf, Network net, TransitSchedule ts, Vehicles vehicles,
                                      int pt_interval,
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
        vehicleType.setMaximumVelocity(freeSpeedTrainForSchedule);
        vehicleType.setLength(10);
        vehicleType.getCapacity().setSeats(10000);
        vehicleType.getCapacity().setStandingRoom(0);
        vehicles.addVehicleType(vehicleType);
    }

    public void createLine(Node startNode, Node endNode, String direction) {
        ArrayList<Link> linkList = new ArrayList<>();
        TransitLine transitLine = transitScheduleFactory
                .createTransitLine(Id.create("Line".concat(String.valueOf(route_counter)), TransitLine.class));
        List<TransitRouteStop> transitRouteStopList = new ArrayList<>();
        currRouteStopCount = 0;

        try {
//        moveFromTo(linkList, startNode, endNode, transitRouteStopList, direction, true);
//        moveFromTo(linkList, endNode, startNode, transitRouteStopList, direction, false);
            movePeriodic(linkList, startNode, endNode, transitRouteStopList, direction, true);
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

//    private void moveFromTo(ArrayList<Link> linkList, Node startNode, Node endNode,
//                            List<TransitRouteStop> transitRouteStopList, String direction,
//                            boolean addFirstAsTransitStop) throws Exception {
//        boolean addAsTransitStop = addFirstAsTransitStop;
//        int forwardBackwardDetermination = getDirection(startNode, endNode, direction);
//        Node currNode = startNode;
//        do {
//            currNode = moveToNextLink(linkList, startNode, currNode, transitRouteStopList, direction,
//                    forwardBackwardDetermination,
//                    addAsTransitStop);
//            addAsTransitStop = true;
//        }
//        while (currNode != endNode);
//        createStop(transitRouteStopList, currNode, linkList.get(linkList.size() - 1));
//    }

    private int getDirection(Node startNode, Node endNode, String direction) throws Exception {
        if (direction.equals("x")) {
            return endNode.getCoord().getX() > startNode.getCoord().getX() ? 1 : -1;
        } else if (direction.equals("y")) {
            return endNode.getCoord().getY() > startNode.getCoord().getY() ? 1 : -1;
        } else {
            throw new Exception("direction must be either \"x\" or \"y\"");
        }
    }

    private void movePeriodic(ArrayList<Link> linkList, Node startNode, Node forwardBackwardIndicatorNode,
                              List<TransitRouteStop> transitRouteStopList,
                              String direction, boolean addFirstAsTransitStop) throws Exception {
        boolean addAsTransitStop = addFirstAsTransitStop;
        int forwardBackwardDetermination = getDirection(startNode, forwardBackwardIndicatorNode, direction);
        Node lastNode = getPredecessor(startNode, direction, forwardBackwardDetermination);
        Node currNode = startNode;
        do {
            List<Node> lastCurrNodes = moveToNextLinkPeriodic(linkList, startNode, currNode, lastNode,
                    transitRouteStopList, direction, addAsTransitStop);
            lastNode = lastCurrNodes.get(0);
            currNode = lastCurrNodes.get(1);
            addAsTransitStop = true;
        }
        while (currNode != startNode);
        createStop(transitRouteStopList, currNode, linkList.get(linkList.size() - 1));
    }

    private Node getPredecessor(Node startNode, String direction, int forwardBackwardDetermination) {
        Function<Comparator<Node>, BinaryOperator<Node>> minMax = null;
        if (forwardBackwardDetermination == 1)
            minMax = BinaryOperator::minBy;
        else
            minMax = BinaryOperator::maxBy;

        if (direction.equals("x")) {
            return startNode.getOutLinks().values().stream()
                    .map(Link::getToNode)
                    .filter(n -> n.getCoord().getY() == startNode.getCoord().getY())
                    .reduce(Objects.requireNonNull(minMax.apply(Comparator.comparing(n -> n.getCoord().getX()))))
                    .orElseThrow();
        }
        if (direction.equals("y")) {
            return startNode.getOutLinks().values().stream()
                    .map(Link::getToNode)
                    .filter(n -> n.getCoord().getX() == startNode.getCoord().getX())
                    .reduce(Objects.requireNonNull(minMax.apply(Comparator.comparing(n -> n.getCoord().getY()))))
                    .orElseThrow();
        }
        return null;
    }

    private List<Node> moveToNextLinkPeriodic(ArrayList<Link> linkList, Node startNode,
                                              Node currNode, Node lastNode,
                                              List<TransitRouteStop> transitRouteStopList,
                                              String direction, boolean addAsTransitStop) {
        Link newLink = null;
        Function<Coord, Double> getSameCoordComp = coord -> coord != null ? coord.getX() : 0;
        Function<Coord, Double> getOtherCoordComp = coord -> coord != null ? coord.getY() : 0;
        if (direction.equals("x")) {
            getSameCoordComp = Coord::getX;
            getOtherCoordComp = Coord::getY;
        } else if (direction.equals("y")) {
            getSameCoordComp = Coord::getY;
            getOtherCoordComp = Coord::getX;
        }

        Function<Coord, Double> finalGetSameCoordComp = getSameCoordComp;
        Function<Coord, Double> finalGetOtherCoordComp = getOtherCoordComp;
        Node finalLastNode = lastNode;
        newLink = currNode.getOutLinks().values().stream()
                .filter(l -> l.getAllowedModes().contains("train"))
                .filter(l -> l.getToNode() != finalLastNode
                        && finalGetOtherCoordComp.apply(l.getToNode().getCoord())
                        .equals(finalGetOtherCoordComp.apply(startNode.getCoord())))
                .findFirst().orElseThrow();
        linkList.add(newLink);
//        if ((finalGetSameCoordComp.apply(currNode.getCoord()) / delta_xy + pt_interval / 2) % pt_interval == 0 && addAsTransitStop) {
        if (currNode.getAttributes().getAttribute("isStation").equals(true) && addAsTransitStop) {
            createStop(transitRouteStopList, currNode, newLink);
        }
        lastNode = currNode;
        currNode = newLink.getToNode();
        return Arrays.asList(lastNode, currNode);
    }

//    private Node moveToNextLink(ArrayList<Link> linkList, Node startNode,
//                                Node currNode,
//                                List<TransitRouteStop> transitRouteStopList,
//                                String direction, int forwardBackwardDetermination, boolean addAsTransitStop) {
//        Link newLink = null;
//        Node tempNode = currNode;
//
//        Function<Coord, Double> getSameCoordComp = coord -> coord != null ? coord.getX() : 0;
//        Function<Coord, Double> getOtherCoordComp = coord -> coord != null ? coord.getY() : 0;
//        if (direction.equals("x")) {
//            getSameCoordComp = Coord::getX;
//            getOtherCoordComp = Coord::getY;
//        } else if (direction.equals("y")) {
//            getSameCoordComp = Coord::getY;
//            getOtherCoordComp = Coord::getX;
//        }
//        Function<Coord, Double> finalGetSameCoordComp = getSameCoordComp;
//        Function<Coord, Double> finalGetOtherCoordComp = getOtherCoordComp;
//        newLink = currNode.getOutLinks().values().stream()
//                .filter(l -> l.getAllowedModes().contains("train"))
//                .filter(l -> finalGetOtherCoordComp.apply(l.getToNode().getCoord())
//                        .equals(finalGetOtherCoordComp.apply(l.getCoord())) && Math
//                        .signum(finalGetSameCoordComp.apply(l.getToNode().getCoord()) -
//                                finalGetSameCoordComp.apply(tempNode.getCoord()))
//                        == forwardBackwardDetermination).findFirst().orElseThrow();
//        linkList.add(newLink);
//        if (currNode.getAttributes().getAttribute("isStation").equals(true) && addAsTransitStop) {
//            createStop(transitRouteStopList, currNode, newLink);
//        }
//        currNode = newLink.getToNode();
//
//        return currNode;
//    }

    private void createStop(List<TransitRouteStop> transitRouteStopList, Node currNode, Link link) {
        Id<TransitStopFacility> stopId = Id.create(String.valueOf(link.getId()) + "_trStop", TransitStopFacility.class);
        TransitStopFacility transitStopFacility = null;
        if (!schedule.getFacilities().containsKey(stopId)) {
            transitStopFacility = transitScheduleFactory.createTransitStopFacility(
                    stopId, currNode.getCoord(), false);
            transitStopFacility.setLinkId(link.getId());
            schedule.addStopFacility(transitStopFacility);
        } else {
            transitStopFacility = schedule.getFacilities().get(stopId);
        }
        TransitRouteStop transitrouteStop = transitScheduleFactory
                .createTransitRouteStop(transitStopFacility, currRouteStopCount * departure_delay - stop_length,
                        currRouteStopCount * departure_delay);
        transitrouteStop.setAwaitDepartureTime(true);
        transitRouteStopList.add(transitrouteStop);
        currRouteStopCount++;
        stop_counter++;
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
