package de.mpi.ds.polygon_utils;

import org.matsim.api.core.v01.Coord;

import java.util.ArrayList;
import java.util.stream.Collectors;

import static java.lang.Math.*;

public class RayCasting {

    static boolean intersects(double[] A, double[] B, double[] P) {
        if (A[1] > B[1])
            return intersects(B, A, P);

        if (P[1] == A[1] || P[1] == B[1])
            P[1] += 0.0001;

        if (P[1] > B[1] || P[1] < A[1] || P[0] >= max(A[0], B[0]))
            return false;

        if (P[0] < min(A[0], B[0]))
            return true;

        double red = (P[1] - A[1]) / (double) (P[0] - A[0]);
        double blue = (B[1] - A[1]) / (double) (B[0] - A[0]);
        return red >= blue;
    }

    public static boolean contains(double[][] shape, double[] pnt) {
        boolean inside = false;
        int len = shape.length;
        for (int i = 0; i < len; i++) {
            if (intersects(shape[i], shape[(i + 1) % len], pnt))
                inside = !inside;
        }
        return inside;
    }

    public static boolean contains(ArrayList<Coord> shape, Coord point) {
        double[][] shapeToArray = new double[shape.size()][2];
        shapeToArray = shape.stream().map(n -> new double[]{n.getX(), n.getY()}).collect(Collectors.toList())
                .toArray(shapeToArray);

        double[] pointToAray = new double[]{point.getX(), point.getY()};
        return contains(shapeToArray, pointToAray);
    }
}