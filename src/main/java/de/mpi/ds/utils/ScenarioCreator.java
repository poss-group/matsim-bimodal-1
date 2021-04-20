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

    private double systemSize;
    private int railInterval;
    private double carGridSpacing;
    private long linkCapacity;
    private double freeSpeedCar;
    private double freeSpeedTrain;
    private double freeSpeedTrainForSchedule;
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
    private boolean isGridNetwork;
    private boolean diagonalConnections;
    private boolean smallLinksCloseToStations;
    private boolean createTrainLines;
    private String travelDistanceDistribution;
    private double travelDistanceMeanOverL;

    public ScenarioCreator(double systemSize, int railInterval, double carGridSpacing,
                           long linkCapacity, double freeSpeedCar, double freeSpeedTrain,
                           double freeSpeedTrainForSchedule, double numberOfLanes, int requestEndTime, int nRequests,
                           double transitEndTime, double departureIntervalTime, double transitStopLength,
                           int nDrtVehicles, int drtCapacity, double drtOperationStartTime, double drtOperationEndTime,
                           long seed, String transportMode, boolean isGridNetwork, boolean diagonalConnections,
                           boolean smallLinksCloseToStations, boolean createTrainLines,
                           String travelDistanceDistribution, double travelDistanceMeanOverL) {

        this.systemSize = systemSize;
        this.railInterval = railInterval;
        this.carGridSpacing = carGridSpacing;
        this.linkCapacity = linkCapacity;
        this.freeSpeedCar = freeSpeedCar;
        this.freeSpeedTrain = freeSpeedTrain;
        this.freeSpeedTrainForSchedule = freeSpeedTrainForSchedule;
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
        this.isGridNetwork = isGridNetwork;
        this.diagonalConnections = diagonalConnections;
        this.smallLinksCloseToStations = smallLinksCloseToStations;
        this.createTrainLines = createTrainLines;
        this.travelDistanceDistribution = travelDistanceDistribution;
        this.travelDistanceMeanOverL = travelDistanceMeanOverL;

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

    public double getSystemSize() {
        return systemSize;
    }

    public int getRailInterval() {
        return railInterval;
    }

    public double getCarGridSpacing() {
        return carGridSpacing;
    }

    public long getLinkCapacity() {
        return linkCapacity;
    }

    public double getFreeSpeedCar() {
        return freeSpeedCar;
    }

    public double getFreeSpeedTrain() {
        return freeSpeedTrain;
    }

    public double getFreeSpeedTrainForSchedule() {
        return freeSpeedTrainForSchedule;
    }

    public double getNumberOfLanes() {
        return numberOfLanes;
    }

    public int getRequestEndTime() {
        return requestEndTime;
    }

    public int getnRequests() {
        return nRequests;
    }

    public double getTransitEndTime() {
        return transitEndTime;
    }

    public double getDepartureIntervalTime() {
        return departureIntervalTime;
    }

    public double getTransitStopLength() {
        return transitStopLength;
    }

    public int getnDrtVehicles() {
        return nDrtVehicles;
    }

    public int getDrtCapacity() {
        return drtCapacity;
    }

    public double getDrtOperationStartTime() {
        return drtOperationStartTime;
    }

    public double getDrtOperationEndTime() {
        return drtOperationEndTime;
    }

    public long getSeed() {
        return seed;
    }

    public String getTransportMode() {
        return transportMode;
    }

    public boolean isGridNetwork() {
        return isGridNetwork;
    }

    public boolean isDiagonalConnections() {
        return diagonalConnections;
    }

    public boolean isSmallLinksCloseToStations() {
        return smallLinksCloseToStations;
    }

    public boolean isCreateTrainLines() {
        return createTrainLines;
    }

    public String getTravelDistanceDistribution() {
        return travelDistanceDistribution;
    }

    public double getTravelDistanceMeanOverL() {
        return travelDistanceMeanOverL;
    }
}
