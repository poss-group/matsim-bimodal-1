package de.mpi.ds.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GeneralUtils {
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
        List<Double> xCoords = net.getNodes().values().stream().map(n -> n.getCoord().getX())
                .collect(Collectors.toList());
        List<Double> yCoords = net.getNodes().values().stream().map(n -> n.getCoord().getY())
                .collect(Collectors.toList());
        double xmin = Collections.min(xCoords);
        double xmax = Collections.max(xCoords);
        double ymin = Collections.min(yCoords);
        double ymax = Collections.max(yCoords);

        return new double[]{Math.min(xmin, ymin), Math.max(xmax, ymax)};
    }
}
