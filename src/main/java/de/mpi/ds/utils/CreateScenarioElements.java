package de.mpi.ds.utils;

import org.matsim.api.core.v01.TransportMode;

import static de.mpi.ds.utils.NetworkUtil.createGridNetwork;
import static de.mpi.ds.utils.PopulationUtil.createPopulation;

public class CreateScenarioElements {
    private static String SUBFOLDER = "drt_scenario/";

    public static void main(String[] args) {
        createGridNetwork("./output/" + SUBFOLDER + "network.xml", false);
//        runTransitScheduleUtil("./output/network.xml", "./output/transitSchedule.xml", "./output/transitVehicles
//        .xml");

        for (int i = 1000; i <= 10000; i += 1000) {
            String namePopulationFile = "population_" + String.valueOf(i) + "reqs_drt.xml";
            String nameDrtVehiclesFile = "drtvehicles_" + String.valueOf(i) + "reqs.xml";
            createPopulation("./output/" + SUBFOLDER + namePopulationFile, "./output/" + SUBFOLDER + "network.xml", i,
                    TransportMode.drt);
            new CreateDrtFleetVehicles()
                    .run("./output/" + SUBFOLDER + "network.xml", "output/" + SUBFOLDER + nameDrtVehiclesFile,
                            (int) (0.01 * i));
        }
    }
}
