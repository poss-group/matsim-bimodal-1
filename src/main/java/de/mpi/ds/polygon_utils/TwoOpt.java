package de.mpi.ds.polygon_utils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;

import java.util.ArrayList;

import static de.mpi.ds.utils.GeneralUtils.calculateDistanceNonPeriodic;

public class TwoOpt {
    private static final Logger LOG = Logger.getLogger(TwoOpt.class.getName());

    public static ArrayList<Coord> alternate(ArrayList<Coord> cities) {
        // Add first city to end to get circular path
        cities.add(cities.get(0));
        ArrayList<Coord> newTour;
        double bestDist = routeLength(cities);
        double newDist;
        int swaps = 1;
        int improve = 0;
        int iterations = 0;
        long comparisons = 0;

        while (swaps != 0) { //loop until no improvements are made.
            swaps = 0;

            //initialise inner/outer loops avoiding adjacent calculations and making use of problem symmetry to half total comparisons.
            for (int i = 1; i < cities.size() - 2; i++) {
                for (int j = i + 1; j < cities.size() - 1; j++) {
                    comparisons++;
                    //check distance of line A,B + line C,D against A,C + B,D if there is improvement, call swap method.
                    if ((calculateDistanceNonPeriodic(cities.get(i), cities.get(i - 1)) + calculateDistanceNonPeriodic(cities.get(j + 1),cities.get(j))) >=
                            (calculateDistanceNonPeriodic(cities.get(i),cities.get(j + 1)) + calculateDistanceNonPeriodic(cities.get(i - 1),cities.get(j)))) {

                        newTour = swap(cities, i, j); //pass arraylist and 2 points to be swapped.

                        newDist = routeLength(newTour);

                        if (newDist < bestDist) { //if the swap results in an improved distance, increment counters and update distance/tour
                            cities = newTour;
                            bestDist = newDist;
                            swaps++;
                            improve++;
                        }
                    }
                }
            }
            iterations++;
        }
        LOG.info("Total comparisons made: " + comparisons);
        LOG.info("Total improvements made: " + improve);
        LOG.info("Total iterations made: " + iterations);
        // Remove last added last/first city again to avoid duplicate point
        cities.remove(cities.size()-1);
        return cities;
    }

    private static double routeLength(ArrayList<Coord> cities) {
        double length = 0;
        for (int i=1; i<cities.size(); i++) {
            length += calculateDistanceNonPeriodic(cities.get(i), cities.get(i-1));
        }
        return length;
    }

    private static ArrayList<Coord> swap(ArrayList<Coord> cities, int i, int j) {
        //conducts a 2 opt swap by inverting the order of the points between i and j
        ArrayList<Coord> newTour = new ArrayList<>();

        //take array up to first point i and add to newTour
        int size = cities.size();
        for (int c = 0; c <= i - 1; c++) {
            newTour.add(cities.get(c));
        }

        //invert order between 2 passed points i and j and add to newTour
        int dec = 0;
        for (int c = i; c <= j; c++) {
            newTour.add(cities.get(j - dec));
            dec++;
        }

        //append array from point j to end to newTour
        for (int c = j + 1; c < size; c++) {
            newTour.add(cities.get(c));
        }

        return newTour;
    }
}