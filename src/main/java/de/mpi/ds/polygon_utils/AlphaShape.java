package de.mpi.ds.polygon_utils;

import org.matsim.api.core.v01.Coord;
import triangulation.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static de.mpi.ds.utils.GeneralUtils.calculateDistanceNonPeriodic;

public class AlphaShape {
    private double alpha;
    private List<Vector2D> points;
    private List<Triangle2D> triangulation = null;
    private TriangleSoup triSoup = null;

    public AlphaShape(List<Vector2D> points, double alpha) {
        this.alpha = alpha;
        this.points = points;
    }

    public ArrayList<Coord> compute() {
        List<Vector2D> resultPolygon = new ArrayList<>();

        try {
            DelaunayTriangulator delaunyTriangulator = new DelaunayTriangulator(points);
            delaunyTriangulator.triangulate();

            triangulation = delaunyTriangulator.getTriangles();
            triSoup = new TriangleSoup();
            for (Triangle2D tri : triangulation) {
                triSoup.add(tri);
            }
        } catch (NotEnoughPointsException e) {
            e.printStackTrace();
        }

        // Working data
        List<Vector2D> cur = new ArrayList<>();
        Map<Triangle2D, Boolean> flagged = triangulation.stream().collect(Collectors.toMap(tri -> tri, tri -> false));
        computeAllNeighbours(triangulation);

        for (Triangle2D tri : triangulation) {
            if (!flagged.get(tri)) {
                flagged.replace(tri, true);
                if (tri.r2 <= alpha * alpha) {
                    // Check neighbors
                    processNeighbor(cur, flagged, tri, tri.nab, tri.b);
                    processNeighbor(cur, flagged, tri, tri.nbc, tri.c);
                    processNeighbor(cur, flagged, tri, tri.nca, tri.a);
                }
                if (cur.size() > 0) {
                    resultPolygon.addAll(cur);
                    cur = new ArrayList<>();
                }
            }
        }


        return TwoOpt.alternate((ArrayList<Coord>) resultPolygon.stream().map(v -> new Coord(v.x, v.y)).collect(Collectors.toList()));
//        return sortCoords(resultPolygon.stream().map(v -> new Coord(v.x, v.y)).collect(Collectors.toList()),
//                "closest+");
    }

    private void processNeighbor(List<Vector2D> cur, Map<Triangle2D, Boolean> flagged, Triangle2D tri, Triangle2D ab,
                                 Vector2D b) {
        if (ab != null) {
            if (flagged.get(ab)) {
                return;
            }
            flagged.replace(ab, true);
            if (ab.r2 < alpha * alpha) {
                if (ab.nab != null && ab.nab.equals(tri)) {
                    processNeighbor(cur, flagged, ab, ab.nbc, ab.c);
                    processNeighbor(cur, flagged, ab, ab.nca, ab.a);
                } else if (ab.nbc != null && ab.nbc.equals(tri)) {
                    processNeighbor(cur, flagged, ab, ab.nca, ab.a);
                    processNeighbor(cur, flagged, ab, ab.nab, ab.b);
                } else if (ab.nca != null && ab.nca.equals(tri)) {
                    processNeighbor(cur, flagged, ab, ab.nab, ab.b);
                    processNeighbor(cur, flagged, ab, ab.nbc, ab.c);
                }
                return;
            }
        }
        cur.add(b);
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
        return angleDifference(coord, p, direction) + calculateDistanceNonPeriodic(coord, p)/avLinkLength;
    }

    private double angleDifference(Coord c1, Coord c2, double prevDir) {
        return Math.abs(Math.atan2(c2.getY() - c1.getY(),
                c2.getX() - c1.getX()) - prevDir);
    }

}