package de.mpi.ds.utils;

import org.matsim.api.core.v01.TransportMode;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

import static de.mpi.ds.utils.NetworkUtil.createGridNetwork;
import static de.mpi.ds.utils.PopulationUtil.createPopulation;

public class CreateScenarioElements {
    private static String SUBFOLDER = "scenario/";
    private static final Random random = new Random();

    public static void main(String[] args) {
        String outPath = "./output/" + SUBFOLDER;
        createGridNetwork(outPath + "network.xml", false);
        long seed = random.nextLong();
        System.out.println(seed);
//        runTransitScheduleUtil("./output/network.xml", "./output/transitSchedule.xml", "./output/transitVehicles
//        .xml");
        int[] iterations = new int[]{1000, 10000, 50000, 100000, 500000, 1000000};

        for (int i : iterations) {
            String namePopulationFileDrt = "population_" + String.valueOf(i) + "reqs_drt.xml";
            String namePopulationFilePt = "population_" + String.valueOf(i) + "reqs_pt.xml";
            String nameDrtVehiclesFile = "drtvehicles_" + String.valueOf(i) + "reqs.xml";
            createPopulation(outPath + namePopulationFileDrt, "./output/" + SUBFOLDER + "network.xml",
                    i,
                    TransportMode.drt, 0,seed);
            createPopulation(outPath + namePopulationFilePt, "./output/" + SUBFOLDER + "network.xml", i,
                    TransportMode.pt, 0, seed);
            new CreateDrtFleetVehicles()
                    .run(outPath + "network.xml", "output/" + SUBFOLDER + nameDrtVehiclesFile,
                            (int) (0.01 * i));

            compressGzipFile(outPath + namePopulationFilePt, outPath + namePopulationFilePt + ".gz");
            deleteFile(outPath + namePopulationFilePt);
            compressGzipFile(outPath + namePopulationFileDrt, outPath + namePopulationFileDrt + ".gz");
            deleteFile(outPath + namePopulationFileDrt);
            compressGzipFile(outPath + nameDrtVehiclesFile, outPath + nameDrtVehiclesFile + ".gz");
            deleteFile(outPath + nameDrtVehiclesFile);
        }
    }

    static void deleteFile(String filePath) {
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (Exception e) {
            System.out.println("Unable to delete file: " + filePath);
        }
    }

    static void compressGzipFile(String file, String gzipFile) {
        try {
            FileInputStream fis = new FileInputStream(file);
            FileOutputStream fos = new FileOutputStream(gzipFile);
            GZIPOutputStream gzipOS = new GZIPOutputStream(fos);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                gzipOS.write(buffer, 0, len);
            }
            //close resources
            gzipOS.close();
            fos.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
