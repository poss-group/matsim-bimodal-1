package de.mpi.ds.utils;

import org.opengis.parameter.InvalidParameterValueException;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static de.mpi.ds.utils.PopulationUtil.taxiDistDistributionNotNormalized;

public class InverseTransformSampler {
    Function<Double, Double> function;
    double[] domain;
    double[] cummulative_values;
    double[] probs_values;
    double x0 = 0;
    double x1 = 0;
    int N;
    Random rand = new Random();
    double EPSILON = 0.00001;

    InverseTransformSampler(Function<Double, Double> function, boolean isNormalized, double x0, double x1,
                            int integrationSteps) {
        this.function = function;
        this.x0 = x0;
        this.x0 = x1;

        double arg = 0;
        double last_arg = 0;
        double cumsum = 0;
        N = integrationSteps;
        domain = new double[N];
        probs_values = new double[N];
        cummulative_values = new double[N];

        for (int i = 0; i < N; i++) {
            arg = x0 + (double) i / (N-1) * (x1 - x0); // N-1 so that integration really ends at x1 for given number
            // of integration steps N
            double value = function.apply(arg);
            cumsum += (arg - last_arg) * value;
            domain[i] = arg;
            probs_values[i] = value;
            cummulative_values[i] = cumsum;
            last_arg = arg;
        }
        if (isNormalized) {
            assert cumsum > 1 - EPSILON &&
                    cumsum < 1 + EPSILON : "distribution is not normalized (on given domain), integral: "
                    .concat(String.valueOf(cumsum));
        } else {
            double finalCumsum = cumsum;
            cummulative_values = Arrays.stream(cummulative_values).map(d -> d / finalCumsum).toArray();
            probs_values = Arrays.stream(probs_values).map(d -> d / finalCumsum).toArray();
        }
    }

    public double getSample() throws Exception {
        double random = rand.nextDouble();
        int idx = search(random, cummulative_values);
        return domain[idx];
    }


    public static void main(String[] args) {
        try {
            InverseTransformSampler sampler = new InverseTransformSampler(a -> 1/10., true, 0, 10,
                    100000);
//            InverseTransformSampler sampler = new InverseTransformSampler(x -> normalDist(x, 1000, 1000), false, 0,
//                    10000,
//                    10000);
//            InverseTransformSampler sampler = new InverseTransformSampler(
//            x -> taxiDistDistributionNotNormalized(x, 2000, 3), false, 0.001, 10000, (int) 1E5);
            StringBuilder testout = new StringBuilder();
//            List<Double> probabilities = new ArrayList<>(sampler.probs.values());
            for (int i = 0; i < (int) 1E5; i++) {
                testout.append(String.valueOf(sampler.getSample())).append("\n");
//                testout.append(String.valueOf(sampler.probs_values[i])).append("\n");
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter("testout.csv"));
            writer.write(testout.toString());
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int search(double value, double[] a) throws Exception {
        if (value < a[0]) {
            throw new Exception("value is smaller than smallest element in array to be searched in for");
//            return 0;
        }
        if (value > a[a.length - 1]) {
            throw new Exception("value is bigger than biggest element in array to be searched in for");
//            return a.length-1;
        }

        int lo = 0;
        int hi = a.length - 1;

        while (lo <= hi) {
            int mid = (hi + lo) / 2;

            if (value < a[mid]) {
                hi = mid - 1;
            } else if (value > a[mid]) {
                lo = mid + 1;
            } else {
                return mid;
            }
        }
        // lo == hi + 1
        return (a[lo] - value) < (value - a[hi]) ? lo : hi;
    }

    public static double normalDist(double x, double mu, double sig) {
        return 1 / (Math.sqrt(2 * Math.PI) * sig) * Math.exp(-0.5 * ((x - mu) / sig) * ((x - mu) / sig));
    }
}
