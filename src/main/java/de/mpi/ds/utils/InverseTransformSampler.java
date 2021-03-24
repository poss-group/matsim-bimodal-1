package de.mpi.ds.utils;

import org.apache.commons.math3.special.Gamma;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Function;

import static de.mpi.ds.utils.GeneralUtils.doubleCloseToZero;

public class InverseTransformSampler {
    private Function<Double, Double> function;
    private double[] domain;
    private double[] cummulative_values;
    private double[] probs_values;
    private double x0 = 0;
    private double x1 = 0;
    private int N;
    private Random random;
    private double EPSILON = 0.00001;

    InverseTransformSampler(Function<Double, Double> function, boolean isNormalized, double x0, double x1,
                            int integrationSteps, Random random) {
        this.function = function;
        this.x0 = x0;
        this.x0 = x1;

        double arg = 0;
        double last_arg = x0;
        double cumsum = 0;
        this.random = random;
        N = integrationSteps;
        domain = new double[N];
        probs_values = new double[N];
        cummulative_values = new double[N];

        for (int i = 0; i < N; i++) {
            arg = x0 + (double) i / (N - 1) * (x1 - x0);
            double value = function.apply(arg);
            cumsum += (arg - last_arg) * value;
            domain[i] = arg;
            probs_values[i] = value;
            cummulative_values[i] = cumsum;
            last_arg = arg;
        }
        if (isNormalized) {
            assert doubleCloseToZero(cumsum - 1) : "distribution is not normalized (on given domain), integral: "
                    .concat(String.valueOf(cumsum));
        } else {
            double finalCumsum = cumsum;
            cummulative_values = Arrays.stream(cummulative_values).map(d -> d / finalCumsum).toArray();
            probs_values = Arrays.stream(probs_values).map(d -> d / finalCumsum).toArray();
        }
    }

    public double getSample() throws Exception {
        double randDouble = random.nextDouble();
        int idx = search(randDouble, cummulative_values);
        return domain[idx];
    }

    public static int search(double value, double[] a) throws Exception {
        //binary search
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

    public static void main(String[] args) {
        try {
//            InverseTransformSampler sampler = new InverseTransformSampler(a -> 1/20., true, -10, 10,
//                    100000, new Random());
//            InverseTransformSampler sampler = new InverseTransformSampler(x -> normalDist(x, 1000, 1000), false, 0,
//                    10000,
//                    10000);
            InverseTransformSampler sampler = new InverseTransformSampler(
                    x -> taxiDistDistributionNotNormalized(x, 1250, 3.1), false, 0.0001, 10000, (int) 1e7, new Random());
//            List<Double> probabilities = new ArrayList<>(sampler.probs.values());
            StringBuilder testout = new StringBuilder();
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

    public static double taxiDistDistributionNotNormalized(double x, double mean, double k) {
        double z = x / mean;
        return Math.exp(-(k-2)/z) * Math.pow(z, -k);
    }

    public static double normalDist(double x, double mu, double sig) {
        return 1 / (Math.sqrt(2 * Math.PI) * sig) * Math.exp(-0.5 * ((x - mu) / sig) * ((x - mu) / sig));
    }
}
