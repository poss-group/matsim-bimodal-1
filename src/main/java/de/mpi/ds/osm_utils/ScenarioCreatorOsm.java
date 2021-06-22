package de.mpi.ds.osm_utils;

import de.mpi.ds.utils.ScenarioCreatorBuilder;
import de.mpi.ds.utils.*;
import org.apache.log4j.Logger;

import java.util.Random;

public class ScenarioCreatorOsm {
    private final static Logger LOG = Logger.getLogger(ScenarioCreatorOsm.class.getName());

    public final static String IS_START_LINK = "isStartLink";
    public final static String IS_STATION_NODE = "isStation";
    public final static String PERIODIC_LINK = "periodicConnection";
    public final static String NETWORK_MODE_TRAIN = "train";
    public final static String NETWORK_MODE_CAR = "car";

    private NetworkCreatorFromOsm networkCreatorFromOsm;
//    private PopulationCreator populationCreator;
//    private TransitScheduleCreator transitScheduleCreator;
//    private DrtFleetVehiclesCreator drtFleetVehiclesCreator;
//    private Random random;

    private long linkCapacity;
    private double freeSpeedCar;
    private double freeSpeedTrain;
    private double numberOfLanes;
    private int requestEndTime;
    private int nRequests;
    private double transitEndTime;
    private double departureIntervalTime;
    private double transitStopLength;
    private int nDrtVehicles;
    private int drtCapacity;
    private double drtOperationStartTime;
    private double drtOperationEndTime;
    private long seed;
    private String transportMode;
    private String travelDistanceDistribution;
    private double travelDistanceMeanOverL;
    private double effectiveFreeTrainSpeed;

    public ScenarioCreatorOsm(long linkCapacity, double freeSpeedCar, double freeSpeedTrain,
                              double numberOfLanes, int requestEndTime, int nRequests,
                              double transitEndTime, double departureIntervalTime, double transitStopLength,
                              int nDrtVehicles, int drtCapacity, double drtOperationStartTime,
                              double drtOperationEndTime,
                              long seed, String transportMode, String travelDistanceDistribution,
                              double travelDistanceMeanOverL) {

        this.linkCapacity = linkCapacity;
        this.freeSpeedCar = freeSpeedCar;
        this.freeSpeedTrain = freeSpeedTrain;
        this.numberOfLanes = numberOfLanes;
        this.requestEndTime = requestEndTime;
        this.nRequests = nRequests;
        this.transitEndTime = transitEndTime;
        this.departureIntervalTime = departureIntervalTime;
        this.transitStopLength = transitStopLength;
        this.nDrtVehicles = nDrtVehicles;
        this.drtCapacity = drtCapacity;
        this.drtOperationStartTime = drtOperationStartTime;
        this.drtOperationEndTime = drtOperationEndTime;
        this.seed = seed;
        this.transportMode = transportMode;
        this.travelDistanceDistribution = travelDistanceDistribution;
        this.travelDistanceMeanOverL = travelDistanceMeanOverL;

        // Apparently every stops must take 2 seconds -> calc effective velocity to cover distance in planned time
//        int numberOfStopsPerLine = (int) (systemSize/carGridSpacing)/railInterval;
//        this.effectiveFreeTrainSpeed = systemSize/(600-numberOfStopsPerLine*2);
        this.effectiveFreeTrainSpeed = freeSpeedTrain;

//        assert railInterval > 0 : "Pt grid spacing must be bigger than drt grid spacing";
//        assert carGridSpacing * railInterval < systemSize : "Rail interval bigger than sysem size";
//        assert railGridSpacing % carGridSpacing == 0 :
//                "Pt grid spacing mus be integer multiple of drt grid spacing";

//        this.random = new Random(seed);

        this.networkCreatorFromOsm = new NetworkCreatorFromOsm(linkCapacity, effectiveFreeTrainSpeed, numberOfLanes,
                freeSpeedCar);
    }


    public static void main(String... args) {
        ScenarioCreatorOsm scenarioCreator = new ScenarioCreatorBuilderOsm().build();

//        String netPath = "./output/network_diag.xml.gz";
//        String popPath = "./output/population.xml.gz";
//        String drtFleetPath = "output/drtvehicles.xml";
//        String transitSchedulePath = "output/transitSchedule_15min.xml.gz";
//        String transitVehiclesPath = "output/transitVehicles_15min.xml.gz";
//        scenarioCreator.createNetwork(netPath);
//        scenarioCreator.createPopulation(popPath, netPath);
//        scenarioCreator.createDrtFleet(netPath, drtFleetPath);
//        scenarioCreator.createTransitSchedule(netPath, transitSchedulePath, transitVehiclesPath);
    }
//
//    public void createNetwork(String outputPath) {
//        networkCreator.createGridNetwork(outputPath);
//    }
//
//    public void createPopulation(String outputPopulationPath, String networkPath) {
//        populationCreator.createPopulation(outputPopulationPath, networkPath);
//    }
//
//    public void createTransitSchedule(String networkPath, String outputSchedulePath, String outputVehiclePath) {
//        transitScheduleCreator.runTransitScheduleUtil(networkPath, outputSchedulePath, outputVehiclePath);
//    }
//
//    public void createDrtFleet(String networkPath, String outputPath) {
//        drtFleetVehiclesCreator.run(networkPath, outputPath);
//    }
//
//    public double getSystemSize() {
//        return systemSize;
//    }
//
//    public int getRailInterval() {
//        return railInterval;
//    }
//
//    public double getCarGridSpacing() {
//        return carGridSpacing;
//    }
//
//    public long getLinkCapacity() {
//        return linkCapacity;
//    }
//
//    public double getFreeSpeedCar() {
//        return freeSpeedCar;
//    }
//
//    public double getFreeSpeedTrain() {
//        return freeSpeedTrain;
//    }
//
//    public double getFreeSpeedTrainForSchedule() {
//        return freeSpeedTrain;
//    }
//
//    public double getNumberOfLanes() {
//        return numberOfLanes;
//    }
//
//    public int getRequestEndTime() {
//        return requestEndTime;
//    }
//
//    public int getnRequests() {
//        return nRequests;
//    }
//
//    public double getTransitEndTime() {
//        return transitEndTime;
//    }
//
//    public double getDepartureIntervalTime() {
//        return departureIntervalTime;
//    }
//
//    public double getTransitStopLength() {
//        return transitStopLength;
//    }
//
//    public int getnDrtVehicles() {
//        return nDrtVehicles;
//    }
//
//    public int getDrtCapacity() {
//        return drtCapacity;
//    }
//
//    public double getDrtOperationStartTime() {
//        return drtOperationStartTime;
//    }
//
//    public double getDrtOperationEndTime() {
//        return drtOperationEndTime;
//    }
//
//    public long getSeed() {
//        return seed;
//    }
//
//    public String getTransportMode() {
//        return transportMode;
//    }
//
//    public boolean isGridNetwork() {
//        return isGridNetwork;
//    }
//
//    public boolean isDiagonalConnections() {
//        return diagonalConnections;
//    }
//
//    public boolean isSmallLinksCloseToStations() {
//        return smallLinksCloseToStations;
//    }
//
//    public boolean isCreateTrainLines() {
//        return createTrainLines;
//    }
//
//    public String getTravelDistanceDistribution() {
//        return travelDistanceDistribution;
//    }
//
//    public double getTravelDistanceMeanOverL() {
//        return travelDistanceMeanOverL;
//    }
}
