package de.mpi.ds.polygon_utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import triangulation.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static de.mpi.ds.utils.GeneralUtils.calculateDistanceNonPeriodic;

public class AlphaShape {
    private double alpha;
    private List<Vector2D> points;
    private List<Triangle2D> triangulation = null;
    private TriangleSoup triSoup = null;

//    public AlphaShape(String netPath, double normalizedAlpha) {
//        Network net = NetworkUtils.readNetwork(netPath);
//        points = net.getNodes().values().stream().map(n -> new Vector2D(n.getCoord().getX(), n.getCoord().getY())).collect(Collectors.toList());
//        double minR = Double.MAX_VALUE;
//        double maxR = -Double.MAX_VALUE;
//
//        try {
//            DelaunayTriangulator delaunyTriangulator = new DelaunayTriangulator(points);
//            delaunyTriangulator.triangulate();
//
//            triangulation = delaunyTriangulator.getTriangles();
//            triSoup = delaunyTriangulator.getTriangleSoup();
//            for (Triangle2D tri : triangulation) {
//                minR = min4(tri.ab.l, tri.bc.l, tri.ca.l, minR);
//                maxR = max4(tri.ab.l, tri.bc.l, tri.ca.l, maxR);
//            }
//        } catch (NotEnoughPointsException e) {
//            e.printStackTrace();
//        }
//
//        this.alpha = normalizedAlpha * (maxR - minR) + minR;
//
//        // This would be easier
////        this(net.getNodes().values(), normalizedAlpha);
//    }

    public AlphaShape(Collection<? extends Node> nodes, double normalizedAlpha) {
        points = nodes.stream().map(n -> new Vector2D(n.getCoord().getX(), n.getCoord().getY()))
                .collect(Collectors.toList());
        double minR = Double.MAX_VALUE;
        double maxR = -Double.MAX_VALUE;

        try {
            DelaunayTriangulator delaunyTriangulator = new DelaunayTriangulator(points);
            delaunyTriangulator.triangulate();

            triangulation = delaunyTriangulator.getTriangles();
            triSoup = delaunyTriangulator.getTriangleSoup();
            for (Triangle2D tri : triangulation) {
                minR = min4(tri.ab.l, tri.bc.l, tri.ca.l, minR);
                maxR = max4(tri.ab.l, tri.bc.l, tri.ca.l, maxR);
            }
        } catch (NotEnoughPointsException e) {
            e.printStackTrace();
        }

        this.alpha = normalizedAlpha * (maxR - minR) + minR;
//        System.out.println((1000 - minR) / (maxR - minR));
    }

    public ArrayList<Coord> compute() {
        List<Vector2D> resultPolygon = new ArrayList<>();

        computeAllNeighbours(triangulation);
        List<Vector2D> cur = new ArrayList<>();
        Map<Triangle2D, Boolean> flagged = triangulation.stream().collect(Collectors.toMap(tri -> tri, tri -> false));

        for (Triangle2D tri : triangulation) {
            if (!flagged.get(tri)) {
                flagged.replace(tri, true);
                if (tri.r2 <= alpha * alpha) {
                    processNeighbor(cur, flagged, tri, tri.nab, tri.a, tri.b);
                    processNeighbor(cur, flagged, tri, tri.nbc, tri.b, tri.c);
                    processNeighbor(cur, flagged, tri, tri.nca, tri.c, tri.a);
                }
                if (cur.size() > 0) {
                    resultPolygon.addAll(cur);
                    cur = new ArrayList<>();
                }
            }
        }


//        for (Triangle2D tri: triangulation) {
//            if (tri.nab == null) {
//                resultPolygon.add(tri.a);
//                resultPolygon.add(tri.b);
//            }
//            if (tri.nbc == null) {
//                resultPolygon.add(tri.b);
//                resultPolygon.add(tri.c);
//            }
//            if (tri.nca == null) {
//                resultPolygon.add(tri.c);
//                resultPolygon.add(tri.a);
//            }
//        }
        return TwoOpt.alternate(
                (ArrayList<Coord>) resultPolygon.stream().map(v -> new Coord(v.x, v.y)).collect(Collectors.toList()));
//        return (ArrayList<Edge2D>) triangulation.stream().flatMap(t -> Stream.of(t.ab, t.bc, t.ca)).collect(
//                Collectors.toList());
        //        return sortCoords(resultPolygon.stream().map(v -> new Coord(v.x, v.y)).collect(Collectors.toList()),
//                "closest+");
    }

    private void processNeighbor(List<Vector2D> cur, Map<Triangle2D, Boolean> flagged, Triangle2D tri, Triangle2D ab,
                                 Vector2D b, Vector2D c) {
        if (ab != null) {
            if (flagged.get(ab)) {
                return;
            }
            flagged.replace(ab, true);
            if (ab.r2 < alpha * alpha) {
                if (ab.nab != null && ab.nab.equals(tri)) {
                    processNeighbor(cur, flagged, ab, ab.nbc, ab.b, ab.c);
                    processNeighbor(cur, flagged, ab, ab.nca, ab.c, ab.a);
                } else if (ab.nbc != null && ab.nbc.equals(tri)) {
                    processNeighbor(cur, flagged, ab, ab.nca, ab.c, ab.a);
                    processNeighbor(cur, flagged, ab, ab.nab, ab.a, ab.b);
                } else if (ab.nca != null && ab.nca.equals(tri)) {
                    processNeighbor(cur, flagged, ab, ab.nab, ab.a, ab.b);
                    processNeighbor(cur, flagged, ab, ab.nbc, ab.b, ab.c);
                }
                return;
            }
        }
        cur.add(b);
        cur.add(c);
    }

    private void computeAllNeighbours(List<Triangle2D> triangulation) {
        for (Triangle2D tri : triangulation) {
            triSoup.findNeighbour(tri, tri.ab);
            triSoup.findNeighbour(tri, tri.bc);
            triSoup.findNeighbour(tri, tri.ca);
        }
    }

    private ArrayList<Coord> sortCoords(List<Coord> points, String method) {
        ArrayList<Coord> result = new ArrayList<>();
        double avX = points.stream().map(Coord::getX).reduce(0., (subtotal, element) -> subtotal + element) /
                points.size();
        double avY = points.stream().map(Coord::getY).reduce(0., (subtotal, element) -> subtotal + element) /
                points.size();
        if (method.equals("angleToCenter")) {
            double[] angles = points.stream().map(c -> Math.atan2(c.getY() - avY, c.getX() - avX)).mapToDouble(d -> d)
                    .toArray();
            int[] sortedIndices = IntStream.range(0, angles.length).boxed().sorted(Comparator.comparing(i -> angles[i]))
                    .mapToInt(i -> i).toArray();
            for (int i : sortedIndices) {
                result.add(points.get(i));
            }
        } else if (method.equals("closest")) {
            List<Coord> stillToAdd = new ArrayList<>(points);
            Coord current = new Coord(0, 0);
            for (int i = 0; i < points.size(); i++) {
                Coord finalCurrent = current;
                current = stillToAdd.stream()
                        .min(Comparator.comparingDouble(p -> calculateDistanceNonPeriodic(finalCurrent, p)))
                        .get();
                result.add(current);
                stillToAdd.remove(current);
            }
        } else if (method.equals("closest+")) {
            List<Coord> stillToAdd = new ArrayList<>(points);
            Coord current = new Coord(0, 0);
            double direction = -4;
            for (int i = 0; i < points.size(); i++) {
                Coord finalCurrent = current;
                List<Coord> closestThree = stillToAdd.stream()
                        .sorted(Comparator.comparingDouble(p -> calculateDistanceNonPeriodic(finalCurrent, p))).limit(3)
                        .collect(Collectors.toList());
                if (i < 2) {
                    current = closestThree.get(0);
                } else {
                    direction = Math.atan2(result.get(i - 1).getY() - result.get(i - 2).getY(),
                            result.get(i - 1).getX() - result.get(i - 2).getX());
                    int finalI = i;
                    double finalDirection = direction;
                    current = closestThree.stream()
                            .min(Comparator
                                    .comparingDouble(p -> angleDifference(result.get(finalI - 1), p, finalDirection)))
//                                    .comparingDouble(p -> customComparator(result.get(finalI - 1), p, finalDirection, avLinkLength)))
                            .get();

                }
                result.add(current);
                stillToAdd.remove(current);
            }
        }
        return result;
    }

    private double customComparator(Coord coord, Coord p, double direction, double avLinkLength) {
        return angleDifference(coord, p, direction) + calculateDistanceNonPeriodic(coord, p) / avLinkLength;
    }

    private double angleDifference(Coord c1, Coord c2, double prevDir) {
        return Math.abs(Math.atan2(c2.getY() - c1.getY(),
                c2.getX() - c1.getX()) - prevDir);
    }

    private double min4(double a, double b, double c, double d) {
        double min = a;
        if (b < min) {
            min = b;
        }
        if (c < min) {
            min = c;
        }
        if (d < min) {
            min = d;
        }
        return min;
    }

    private double max4(double a, double b, double c, double d) {
        double max = a;
        if (b > max) {
            max = b;
        }
        if (c > max) {
            max = c;
        }
        if (d > max) {
            max = d;
        }
        return max;
    }
}