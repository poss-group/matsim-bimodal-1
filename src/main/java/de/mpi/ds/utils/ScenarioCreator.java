package de.mpi.ds.utils;

import org.apache.log4j.Logger;

import java.util.Random;
import java.util.function.Function;

import static de.mpi.ds.utils.GeneralUtils.doubleCloseToZero;
import static de.mpi.ds.utils.InverseTransformSampler.taxiDistDistributionNotNormalized;

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
    private double numberOfLanes;
    private int requestEndTime;
    private int nRequests;
    private double transitEndTime;
    private double departureIntervalTime;
    private double transitStopLength;
    private int drtFleetSize;
    private int drtCapacity;
    private double drtOperationStartTime;
    private double drtOperationEndTime;
    private long seed;
    private String transportMode;
    private boolean isGridNetwork;
    private boolean diagonalConnections;
    private boolean smallLinksCloseToStations;
    private boolean createTrainLines;
    private Function<Double, Double> travelDistanceDistribution;
    private double travelDistanceMean;
    private double effectiveFreeTrainSpeed;

    public ScenarioCreator(double systemSize, int railInterval, double carGridSpacing,
                           long linkCapacity, double freeSpeedCar, double freeSpeedTrain,
                           double numberOfLanes, int requestEndTime, int nRequests,
                           double transitEndTime, double departureIntervalTime, double transitStopLength,
                           int drtFleetSize, int drtCapacity, double drtOperationStartTime, double drtOperationEndTime,
                           long seed, String transportMode, boolean isGridNetwork, boolean diagonalConnections,
                           boolean smallLinksCloseToStations, boolean createTrainLines,
                           String travelDistanceDistribution, double travelDistanceMean, double meanAndSpeedScaleFactor) {

        this.systemSize = systemSize;
        this.railInterval = railInterval;
        this.carGridSpacing = carGridSpacing;
        this.linkCapacity = linkCapacity;
        this.freeSpeedCar = freeSpeedCar*meanAndSpeedScaleFactor;
        this.freeSpeedTrain = freeSpeedTrain*meanAndSpeedScaleFactor;
        this.numberOfLanes = numberOfLanes;
        this.requestEndTime = requestEndTime;
        this.nRequests = nRequests;
        this.transitEndTime = transitEndTime;
        this.departureIntervalTime = departureIntervalTime;
        this.transitStopLength = transitStopLength;
        this.drtFleetSize = drtFleetSize;
        this.drtCapacity = drtCapacity;
        this.drtOperationStartTime = drtOperationStartTime;
        this.drtOperationEndTime = drtOperationEndTime;
        this.seed = seed;
        this.transportMode = transportMode;
        this.isGridNetwork = isGridNetwork;
        this.diagonalConnections = diagonalConnections;
        this.smallLinksCloseToStations = smallLinksCloseToStations;
        this.createTrainLines = createTrainLines;
        this.travelDistanceMean = travelDistanceMean*meanAndSpeedScaleFactor;

        if (travelDistanceDistribution.equals("InverseGamma")) {
            this.travelDistanceDistribution = x -> taxiDistDistributionNotNormalized(x, this.travelDistanceMean, 3.1);
        } else if (travelDistanceDistribution.equals("Uniform")) {
            this.travelDistanceDistribution = x -> x < this.travelDistanceMean * 2 ? 1 / this.travelDistanceMean * 2 : 0;
        }

        // Apparently every stops must take 2 seconds -> calc effective velocity to cover distance in planned time
//        int numberOfStopsPerLine = (int) (systemSize/carGridSpacing)/railInterval;
//        this.effectiveFreeTrainSpeed = systemSize/(600-numberOfStopsPerLine*2);
        this.effectiveFreeTrainSpeed = freeSpeedTrain;

        assert railInterval > 0 : "Pt grid spacing must be bigger than drt grid spacing";
        assert carGridSpacing * railInterval < systemSize : "Rail interval bigger than system size";
//        assert railGridSpacing % carGridSpacing == 0 :
//                "Pt grid spacing mus be integer multiple of drt grid spacing";

        this.random = new Random(seed);
//        for (int i=0; i<10;i++) {
//            System.out.println(random.nextInt());
//        }
        if (!doubleCloseToZero(systemSize - 10000)) {
            LOG.error("Periodicity is fixed to 10000m, the simulated system has another size however: " + systemSize);
        }
        this.networkCreator = new NetworkCreator(systemSize, railInterval, carGridSpacing, linkCapacity,
                effectiveFreeTrainSpeed, numberOfLanes, freeSpeedCar, diagonalConnections,
                smallLinksCloseToStations, createTrainLines);
        this.populationCreator = new PopulationCreator(nRequests, requestEndTime, random, transportMode, isGridNetwork,
                smallLinksCloseToStations, createTrainLines, this.travelDistanceDistribution, systemSize);
        this.transitScheduleCreator = new TransitScheduleCreator(systemSize, railInterval, freeSpeedTrain,
                effectiveFreeTrainSpeed, transitEndTime, transitStopLength, departureIntervalTime, carGridSpacing);
        this.drtFleetVehiclesCreator = new DrtFleetVehiclesCreator(drtCapacity, drtOperationStartTime,
                drtOperationEndTime, random, this.travelDistanceDistribution, travelDistanceMean, railInterval*carGridSpacing);
    }


    public static void main(String... args) {
        ScenarioCreator scenarioCreator = new ScenarioCreatorBuilder().build();

        String netPath = "./output/network_diag.xml.gz";
        String popPath = "./output/population.xml.gz";
        String drtFleetPath = "./output/drtvehicles.xml";
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

    public void createDrtFleet(String networkPath, String ouputPath) {
        drtFleetVehiclesCreator.run(networkPath, ouputPath, drtFleetSize);
    }

    public void createDrtFleet(String networkPath, String outputUnimPath, String outputBimPath, double zetacut) {
        drtFleetVehiclesCreator.run(networkPath, outputUnimPath, outputBimPath, zetacut, drtFleetSize);
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
        return freeSpeedTrain;
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

    public int getdrtFleetSize() {
        return drtFleetSize;
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

    public Function<Double, Double> getTravelDistanceDistribution() {
        return travelDistanceDistribution;
    }

    public double getTravelDistanceMean() {
        return travelDistanceMean;
    }
}
