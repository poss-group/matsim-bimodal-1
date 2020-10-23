package de.mpi.ds.utils;

import org.matsim.api.core.v01.TransportMode;

import java.util.Random;

import static de.mpi.ds.utils.NetworkUtil.createGridNetwork;
import static de.mpi.ds.utils.PopulationUtil.createPopulation;

public class CreateScenarioElements {
    private static String SUBFOLDER = "scenario/";
    private static final Random random = new Random();

    public static void main(String[] args) {
        createGridNetwork("./output/" + SUBFOLDER + "network.xml", false);
        long seed = random.nextLong();
        System.out.println(seed);
//        runTransitScheduleUtil("./output/network.xml", "./output/transitSchedule.xml", "./output/transitVehicles
//        .xml");
        int[] iterations = new int[]{1000, 10000, 50000, 100000, 500000, 1000000};

        for (int i : iterations) {
            String namePopulationFileDrt = "population_" + String.valueOf(i) + "reqs_drt.xml";
            String namePopulationFilePt = "population_" + String.valueOf(i) + "reqs_pt.xml";
            String nameDrtVehiclesFile = "drtvehicles_" + String.valueOf(i) + "reqs.xml";
            createPopulation("./output/" + SUBFOLDER + namePopulationFileDrt, "./output/" + SUBFOLDER + "network.xml",
                    i,
                    TransportMode.drt, seed);
            createPopulation("./output/" + SUBFOLDER + namePopulationFilePt, "./output/" + SUBFOLDER + "network.xml", i,
                    TransportMode.pt, seed);
            new CreateDrtFleetVehicles()
                    .run("./output/" + SUBFOLDER + "network.xml", "output/" + SUBFOLDER + nameDrtVehiclesFile,
                            (int) (0.01 * i));
        }
    }
}
