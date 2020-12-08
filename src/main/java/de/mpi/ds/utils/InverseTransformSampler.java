package de.mpi.ds.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;

public class InverseTransformSampler {
    Function<Double, Double> function;
    Map<Double, Double> probs;
    Map<Double, Double> cummulative;
    double[] cummulative_values;
    double[] cummulative_keys;
    double x0 = 0;
    double x1 = 0;
    int N = 10000;
    Random rand = new Random();

    InverseTransformSampler(Function<Double, Double> function, double x0, double x1) {
        this.function = function;
        this.x0 = x0;
        this.x0 = x1;

        probs = new LinkedHashMap<>();
        cummulative = new LinkedHashMap<>();
        double arg = 0;
        double last_arg = 0;
        double cumsum = 0;
//        String out = "";
        for (int i = 0; i < N; i++) {
            arg = x0 + (double) i / N * (x1-x0);
            double value = function.apply(arg);
            cumsum += (arg - last_arg) * value;
            probs.put(arg, value);
            cummulative.put(arg, cumsum);
            last_arg = arg;
//            out += (String.valueOf(arg) + ";" + String.valueOf(value) + ";" + String.valueOf(cumsum) + "\n");
        }
        assert cumsum > 0.99 && cumsum < 1.01 : "distribution is not normalized";
        cummulative_values = cummulative.values().stream().mapToDouble(Double::doubleValue).toArray();
        cummulative_keys = cummulative.keySet().stream().mapToDouble(Double::doubleValue).toArray();
//        BufferedWriter writer = new BufferedWriter(new FileWriter("testout.csv"));
//        writer.write(out);
//        writer.close();
    }

    public double getSample() {
        double random = rand.nextDouble();
        double delta = 0;
        double last_delta = Double.POSITIVE_INFINITY;
        int idx = search(random, cummulative_values);
        return cummulative_keys[idx];
    }

    public void applyFunc(Double argument) {
        System.out.println(this.function.apply(argument));
    }

    public static void main(String[] args) {
        try {
//            InverseTransformSampler test = new InverseTransformSampler(a -> 1/10., 0, 10);
            InverseTransformSampler test = new InverseTransformSampler(a -> normalDist(a, 0, 1), -10, 10);
//            System.out.println(test.getSample());
            StringBuilder testout = new StringBuilder();
            for (int i = 0; i < 10000; i++) {
                testout.append(String.valueOf(test.getSample())).append("\n");
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter("testout.csv"));
            writer.write(testout.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static double normalDist(double x, double mu, double sig) {
        return 1/Math.sqrt(2*Math.PI)*Math.exp(-0.5*(x-mu/sig)*(x-mu/sig));
    }

    public static int search(double value, double[] a) {
        if(value < a[0]) {
            return 0;
        }
        if(value > a[a.length-1]) {
            return a.length-1;
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
}
