package de.mpi.ds.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.mpi.ds.utils.GeneralUtils.*;

public class ClosestLinkFinder {
    private List<ReferencePoint> referencePoints;
    private double periodicity;

    class ReferencePoint {
        private List<Link> assignedLinks = new ArrayList<>();
        private Coord coord;

        ReferencePoint(double xx0, double yy0) {
            this.coord = new Coord(xx0, yy0);
        }

        void assignLink(Link l) {
            this.assignedLinks.add(l);
        }

        Link findClosesLink(Coord coord) {
            return assignedLinks.stream()
                    .min(Comparator.comparing(l -> calculateDistancePeriodicBC(l.getCoord(), coord, periodicity)))
                    .get();
        }
    }

    public ClosestLinkFinder(Network net, int n, List<Link> linkList, double periodicity) {
        this.periodicity = periodicity;
        double x0 = net.getLinks().values().stream().mapToDouble(l -> l.getCoord().getX()).min().getAsDouble();
        double x1 = net.getLinks().values().stream().mapToDouble(l -> l.getCoord().getX()).max().getAsDouble();
        double y0 = net.getLinks().values().stream().mapToDouble(l -> l.getCoord().getY()).min().getAsDouble();
        double y1 = net.getLinks().values().stream().mapToDouble(l -> l.getCoord().getY()).max().getAsDouble();
        double dX = (x1 - x0) / n;
        double dY = (y1 - y0) / n;
        referencePoints = new ArrayList<>();
        for (double xx0 = x0+dX/2; xx0 < x1; xx0 += dX) {
            for (double yy0 = y0+dY/2; yy0 < y1; yy0 += dY) {
                referencePoints.add(new ReferencePoint(xx0, yy0));
            }
        }
        for (Link link : linkList) {
//            ReferencePoint r = referencePoints.stream()
//                    .min(Comparator
//                            .comparing(ref -> calculateDistancePeriodicBC(ref.coord, link.getCoord(), periodicity)))
//                    .get();
            List<ReferencePoint> minDistRefPoints = referencePoints.stream()
                    .collect(Collectors.groupingBy(ref -> calculateDistancePeriodicBC(ref.coord, link.getCoord(), periodicity), TreeMap::new, Collectors.toList()))
                    .firstEntry().getValue();

            for (ReferencePoint r: minDistRefPoints) {
                r.assignLink(link);
            }
        }
    }

    public Link findClosestLink(Coord coord) {
        ReferencePoint r = referencePoints.stream()
                .min(Comparator.comparing(ref -> calculateDistancePeriodicBC(ref.coord, coord, periodicity))).get();
        return r.findClosesLink(coord);
    }
}
