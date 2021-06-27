package de.mpi.ds.polygon_utils;

import org.matsim.api.core.v01.Coord;

import java.util.ArrayList;
import java.util.Scanner;
 
public class QuickHull
{
    public ArrayList<Coord> quickHull(ArrayList<Coord> points) {
        ArrayList<Coord> convexHull = new ArrayList<Coord>();
        if (points.size() < 3)
            return (ArrayList) points.clone();
 
        int minPoint = -1, maxPoint = -1;
        double minX = Integer.MAX_VALUE;
        double maxX = Integer.MIN_VALUE;
        for (int i = 0; i < points.size(); i++)
        {
            if (points.get(i).getX() < minX)
            {
                minX = points.get(i).getX();
                minPoint = i;
            }
            if (points.get(i).getX() > maxX)
            {
                maxX = points.get(i).getX();
                maxPoint = i;
            }
        }
        Coord A = points.get(minPoint);
        Coord B = points.get(maxPoint);
        convexHull.add(A);
        convexHull.add(B);
        points.remove(A);
        points.remove(B);
 
        ArrayList<Coord> leftSet = new ArrayList<Coord>();
        ArrayList<Coord> rightSet = new ArrayList<Coord>();
 
        for (int i = 0; i < points.size(); i++)
        {
            Coord p = points.get(i);
            if (pointLocation(A, B, p) == -1)
                leftSet.add(p);
            else if (pointLocation(A, B, p) == 1)
                rightSet.add(p);
        }
        hullSet(A, B, rightSet, convexHull);
        hullSet(B, A, leftSet, convexHull);
 
        return convexHull;
    }
 
    public double distance(Coord A, Coord B, Coord C) {
        double ABx = B.getX() - A.getX();
        double ABy = B.getY() - A.getY();
        double num = ABx * (A.getY() - C.getY()) - ABy * (A.getX() - C.getX());
        if (num < 0)
            num = -num;
        return num;
    }
 
    public void hullSet(Coord A, Coord B, ArrayList<Coord> set,
            ArrayList<Coord> hull) {
        int insertPosition = hull.indexOf(B);
        if (set.size() == 0)
            return;
        if (set.size() == 1)
        {
            Coord p = set.get(0);
            set.remove(p);
            hull.add(insertPosition, p);
            return;
        }
        double dist = Double.MIN_VALUE;
        int furthestPoint = -1;
        for (int i = 0; i < set.size(); i++)
        {
            Coord p = set.get(i);
            double distance = distance(A, B, p);
            if (distance > dist)
            {
                dist = distance;
                furthestPoint = i;
            }
        }
        Coord P = set.get(furthestPoint);
        set.remove(furthestPoint);
        hull.add(insertPosition, P);
 
        // Determine who's to the left of AP
        ArrayList<Coord> leftSetAP = new ArrayList<Coord>();
        for (int i = 0; i < set.size(); i++)
        {
            Coord M = set.get(i);
            if (pointLocation(A, P, M) == 1)
            {
                leftSetAP.add(M);
            }
        }
 
        // Determine who's to the left of PB
        ArrayList<Coord> leftSetPB = new ArrayList<Coord>();
        for (int i = 0; i < set.size(); i++)
        {
            Coord M = set.get(i);
            if (pointLocation(P, B, M) == 1)
            {
                leftSetPB.add(M);
            }
        }
        hullSet(A, P, leftSetAP, hull);
        hullSet(P, B, leftSetPB, hull);
 
    }
 
    public int pointLocation(Coord A, Coord B, Coord P) {
        double cp1 = (B.getX() - A.getX()) * (P.getY() - A.getY()) - (B.getY() - A.getY()) * (P.getX() - A.getX());
        if (cp1 > 0)
            return 1;
        else if (cp1 == 0)
            return 0;
        else
            return -1;
    }
 
    public static void main(String args[]) {
        System.out.println("Quick Hull Test");
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter the number of points");
        int N = sc.nextInt();
 
        ArrayList<Coord> points = new ArrayList<Coord>();
        System.out.println("Enter the coordinates of each points: <x> <y>");
        for (int i = 0; i < N; i++)
        {
            double x = sc.nextDouble();
            double y = sc.nextDouble();
            Coord e = new Coord(x, y);
            points.add(i, e);
        }
 
        QuickHull qh = new QuickHull();
        ArrayList<Coord> p = qh.quickHull(points);
        System.out
                .println("The points in the Convex hull using Quick Hull are: ");
        for (int i = 0; i < p.size(); i++)
            System.out.println("(" + p.get(i).getX() + ", " + p.get(i).getY() + ")");
        sc.close();
    }
}