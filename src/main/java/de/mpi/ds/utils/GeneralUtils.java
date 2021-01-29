package de.mpi.ds.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GeneralUtils {
    private static final double EPSILON = 1e-4;

    public static double calculateDistancePeriodicBC(Node from, Node to, double L) {
        return calculateDistancePeriodicBC(from.getCoord(), to.getCoord(), L);
    }

    public static double calculateDistancePeriodicBC(Coord from, Coord to, double L) {
        double deltaX = Math.abs(to.getX() - from.getX());
        double deltaXPeriodic = deltaX < L / 2 ? deltaX : -deltaX + L;
        double deltaY = Math.abs(to.getY() - from.getY());
        double deltaYPeriodic = deltaY < L / 2 ? deltaY : -deltaY + L;
        return Math.sqrt(deltaXPeriodic * deltaXPeriodic + deltaYPeriodic * deltaYPeriodic);
    }

    public static double calculateManhattenDistancePeriodicBC(Coord from, Coord to, double L) {
        double deltaX = Math.abs(to.getX() - from.getX());
        double deltaXPeriodic = deltaX < L / 2 ? deltaX : -deltaX + L;
        double deltaY = Math.abs(to.getY() - from.getY());
        double deltaYPeriodic = deltaY < L / 2 ? deltaY : -deltaY + L;
        return Math.abs(deltaXPeriodic) + Math.abs(deltaYPeriodic);
    }

    public static double[] getNetworkDimensionsMinMax(Network net) {
        // This method calculates the network dimensions in terms of link lengths; this is neccessary because of
        // Periodic BC, otherwise it would suffice to take the distance btw. nodes
        // TODO check what has to be used distance based on coordinates or max link distance?
        double maxLinkDist = net.getNodes().values().stream()
                .filter(n -> n.getCoord().getY() == 0)
                .flatMap(n -> n.getInLinks().values().stream())
                .filter(l -> l.getFromNode().getCoord().getY() == 0)
                .filter(l -> l.getAllowedModes().contains(TransportMode.train))
                .reduce( 0., (subtotal, l) -> (subtotal + l.getLength()), Double::sum) / 2;

        return new double[]{0, maxLinkDist};

        // Standard method
//        List<Double> xCoords = net.getNodes().values().stream().map(n -> n.getCoord().getX())
//                .collect(Collectors.toList());
//        List<Double> yCoords = net.getNodes().values().stream().map(n -> n.getCoord().getY())
//                .collect(Collectors.toList());
//        double xmin = Collections.min(xCoords);
//        double xmax = Collections.max(xCoords);
//        double ymin = Collections.min(yCoords);
//        double ymax = Collections.max(yCoords);
//
//        return new double[]{Math.min(xmin,ymin), Math.max(xmax, ymax)};
    }

    public static boolean doubleCloseToZero(double x) {
        return Math.abs(x) < EPSILON;
    }
}
