package de.mpi.ds.utils;

import org.apache.log4j.Logger;
import org.jfree.util.Log;

import java.util.Random;

public class ScenarioCreator {
    private final static Logger LOG = Logger.getLogger(ScenarioCreator.class.getName());

    public final static String IS_START_LINK = "isStartLink";
    public final static String IS_STATION_NODE = "isStation";
    public final static String PERIODIC_LINK = "periodicConnection";
    public final static String NETWORK_MODE_TRAIN = "train";
    public final static String NETWORK_MODE_CAR = "car";

    private NetworkCreator networkCreator;
    private PopulationCreator populationCreator;
    private TransitScheduleCreator transitScheduleCreator;
    private DrtFleetVehiclesCreator drtFleetVehiclesCreator;
    private Random random;

    public ScenarioCreator(double systemSize, int railInterval, double carGridSpacing,
                           long linkCapacity, double freeSpeedCar, double freeSpeedTrain,
                           double freeSpeedTrainForSchedule, double numberOfLanes, int requestEndTime, int nRequests,
                           double transitEndTime, double departureIntervalTime, double transitStopLength,
                           int nDrtVehicles, int drtCapacity, double drtOperationStartTime, double drtOperationEndTime,
                           long seed, String transportMode, boolean isGridNetwork, boolean diagonalConnections,
                           boolean smallLinksCloseToStations, boolean createTrainLines,
                           String travelDistanceDistribution, double travelDistanceMeanOverL) {

        assert railInterval > 0 : "Pt grid spacing must be bigger than drt grid spacing";
        assert carGridSpacing * railInterval < systemSize : "Rail interval bigger than sysem size";
//        assert railGridSpacing % carGridSpacing == 0 :
//                "Pt grid spacing mus be integer multiple of drt grid spacing";

        this.random = new Random(seed);
        this.networkCreator = new NetworkCreator(systemSize, railInterval, carGridSpacing, linkCapacity,
                freeSpeedTrainForSchedule, numberOfLanes, freeSpeedCar, diagonalConnections, random,
                smallLinksCloseToStations, createTrainLines);
        this.populationCreator = new PopulationCreator(nRequests, requestEndTime, random, transportMode, isGridNetwork,
                carGridSpacing, smallLinksCloseToStations, createTrainLines, travelDistanceDistribution,
                travelDistanceMeanOverL, systemSize);
        this.transitScheduleCreator = new TransitScheduleCreator(systemSize, railInterval, freeSpeedTrain,
                transitEndTime, transitStopLength, freeSpeedTrainForSchedule, departureIntervalTime, carGridSpacing);
        this.drtFleetVehiclesCreator = new DrtFleetVehiclesCreator(drtCapacity, drtOperationStartTime,
                drtOperationEndTime, nDrtVehicles, random);
    }


    public static void main(String... args) {
        ScenarioCreator scenarioCreator = new ScenarioCreatorBuilder().build();

        String netPath = "./output/network_diag.xml.gz";
        String popPath = "./output/population.xml.gz";
        String drtFleetPath = "output/drtvehicles.xml";
        String transitSchedulePath = "output/transitSchedule_15min.xml.gz";
        String transitVehiclesPath = "output/transitVehicles_15min.xml.gz";
        scenarioCreator.createNetwork(netPath);
        scenarioCreator.createPopulation(popPath, netPath);
        scenarioCreator.createDrtFleet(netPath, drtFleetPath);
        scenarioCreator.createTransitSchedule(netPath, transitSchedulePath, transitVehiclesPath);
    }

    public void createNetwork(String outputPath) {
        networkCreator.createGridNetwork(outputPath);
    }

    public void createPopulation(String outputPopulationPath, String networkPath) {
        populationCreator.createPopulation(outputPopulationPath, networkPath);
    }

    public void createTransitSchedule(String networkPath, String outputSchedulePath, String outputVehiclePath) {
        transitScheduleCreator.runTransitScheduleUtil(networkPath, outputSchedulePath, outputVehiclePath);
    }

    public void createDrtFleet(String networkPath, String outputPath) {
        drtFleetVehiclesCreator.run(networkPath, outputPath);
    }
}
