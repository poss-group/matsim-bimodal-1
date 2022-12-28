package de.mpi.ds.utils;

import org.apache.commons.math3.analysis.integration.RombergIntegrator;
import org.apache.commons.math3.analysis.integration.SimpsonIntegrator;
import org.apache.commons.math3.analysis.integration.UnivariateIntegrator;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.function.Function;

import static de.mpi.ds.utils.GeneralUtils.doubleCloseToZero;
import static de.mpi.ds.utils.GeneralUtils.getNetworkDimensionsMinMax;
import static de.mpi.ds.utils.InverseTransformSampler.taxiDistDistribution;
import static de.mpi.ds.utils.InverseTransformSampler.taxiDistDistributionNotNormalized;

public class ScenarioCreator {
    private final static Logger LOG = Logger.getLogger(ScenarioCreator.class.getName());

    public final static String IS_START_LINK = "isStartLink";
    public final static String IS_STATION_NODE = "isStation";
    public final static String IS_STATION_CROSSING_NODE = "isStationCrossing";
    public final static String PERIODIC_LINK = "periodicConnection";
    public final static String NETWORK_MODE_TRAIN = "train";
    public final static String NETWORK_MODE_CAR = "car";
    private final static double BETA = 0.382597858232;

    private NetworkCreator networkCreator;
    private PopulationCreator populationCreator;
    private TransitScheduleCreator transitScheduleCreator;
    private DrtFleetVehiclesCreator drtFleetVehiclesCreator;
    private Random random;

    private double systemSize;
    private int railInterval;
    private int small_railInterval;
    private double carGridSpacing;
    private long linkCapacity;
    private double freeSpeedCar;
    private double freeSpeedTrain;
    private double numberOfLanes;
    private int requestEndTime;
    private int nRequests;
    private double transitStartTime;
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
    private final double cutoffDistance;
    private boolean constDrtDemand;
    private double fracWithcommonOrigDest;
    private boolean periodic_network;

    public ScenarioCreator(double systemSize, int railInterval, int small_railInterval, double carGridSpacing,
                           long linkCapacity, double freeSpeedCar, double freeSpeedTrain,
                           double numberOfLanes, int requestEndTime, int nRequests,
                           double transitStartTime, double transitEndTime, double departureIntervalTime,
                           double transitStopLength,
                           int drtFleetSize, int drtCapacity, double drtOperationStartTime, double drtOperationEndTime,
                           long seed, String transportMode, boolean isGridNetwork, boolean diagonalConnections,
                           boolean smallLinksCloseToStations, boolean createTrainLines,
                           String travelDistanceDistribution, double travelDistanceMean, double meanAndSpeedScaleFactor,
                           double cutoffDistance, boolean constDrtDemand, double fracWithcommonOrigDest, boolean periodic_network) {

        this.systemSize = systemSize;
        this.railInterval = railInterval;
        this.small_railInterval = small_railInterval;
        this.carGridSpacing = carGridSpacing;
        this.linkCapacity = linkCapacity;
        this.freeSpeedCar = freeSpeedCar * meanAndSpeedScaleFactor;
        this.freeSpeedTrain = freeSpeedTrain * meanAndSpeedScaleFactor;
        this.numberOfLanes = numberOfLanes;
        this.requestEndTime = requestEndTime;
        this.nRequests = nRequests;
        this.transitStartTime = transitStartTime;
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
        this.travelDistanceMean = travelDistanceMean * meanAndSpeedScaleFactor;
        this.cutoffDistance = cutoffDistance;
        this.constDrtDemand = constDrtDemand;
        this.fracWithcommonOrigDest = fracWithcommonOrigDest;
        this.periodic_network = periodic_network;

        if (travelDistanceDistribution.equals("InverseGamma")) {
            this.travelDistanceDistribution = x -> taxiDistDistributionNotNormalized(x, this.travelDistanceMean, 3.1);
        } else if (travelDistanceDistribution.equals("Uniform")) {
            this.travelDistanceDistribution = x ->
                    x < this.travelDistanceMean * 2 ? 1 / this.travelDistanceMean * 2 : 0;
        } else if (travelDistanceDistribution.equals("UniformDiscrete")) {
            this.travelDistanceDistribution = null;
        }

        double avDistFracFromDCut = 0;
        double avDistFracToDCut = 0;
        double avDrtDist = 0;
        if (constDrtDemand) {
            UnivariateIntegrator integrator = new RombergIntegrator();
            double boundedNorm = integrator
                    .integrate(1000000, x -> this.travelDistanceDistribution.apply(x), 0.0001,
                            this.systemSize / 2);
            avDistFracToDCut = integrator
                    .integrate(1000000, x -> x * this.travelDistanceDistribution.apply(x) / boundedNorm,
                            0.0001, cutoffDistance);
            avDistFracFromDCut = integrator
                    .integrate(1000000, x -> this.travelDistanceDistribution.apply(x) / boundedNorm,
                            cutoffDistance, this.systemSize / 2)
                    * 2 * BETA * small_railInterval * carGridSpacing;
            avDrtDist = avDistFracFromDCut + avDistFracToDCut;
//            double[] drtDistsHardCodedV2 = new double[]{397.79121022, 407.809472, 457.86695313, 534.34625905, 621.89311347
//                    , 683.48428029, 757.25604274, 812.20517302, 874.20169806, 942.85764959
//                    , 984.68117987, 1037.74078996, 1077.13210279, 1098.91155073, 1141.42712331
//                    , 1181.2785359, 1216.3565463, 1240.39852175, 1248.23867956, 1287.32667592
//                    , 1316.102699, 1335.14582401, 1352.51491479, 1353.98810628};
//            double[] drtDistsHardCodedV3 = new double[]{407.60869186, 417.5323817, 481.19544669, 568.25788556, 659.73380141
//                    , 755.39394724, 859.54887946, 920.97974561, 1015.06725571, 1067.17688668
//                    , 1121.35707207, 1192.66936841, 1251.68870807, 1265.42062124, 1307.79261321
//                    , 1350.52365534, 1385.74616525, 1409.16607744, 1422.09820814, 1458.53834046
//                    , 1456.32215468, 1484.30860808, 1497.79522769, 1519.45947849};
////
//            int idx = (int) Math.round(cutoffDistance / 200) - 1;
//            avDrtDist = drtDistsHardCodedV3[idx];

            LOG.info("Mean drt dist: " + avDrtDist);
            LOG.info("creating " + (int) (nRequests / avDrtDist) + " requests");

//            try (FileOutputStream fos = new FileOutputStream("request_dists.csv", true)) {
////                String toWrite = cutoffDistance + "," + avDistFracToDCut + "," + avDistFracFromDCut + "," +
////                        avDrtDist + "," + (int) (nRequests / avDrtDist) + "\n";
//                String toWrite = cutoffDistance + "," +(int) (nRequests / avDrtDist) + "\n";
//                fos.write(toWrite.getBytes());
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }

        this.effectiveFreeTrainSpeed = this.freeSpeedTrain;

        assert railInterval > 0 : "Pt grid spacing must be bigger than drt grid spacing";
        assert carGridSpacing * railInterval < systemSize : "Rail interval bigger than system size";

        this.random = new Random(seed);

        if (!doubleCloseToZero(systemSize - 10000)) {
            LOG.error("Periodicity is fixed to 10000m, the simulated system has another size however: " + systemSize);
        }

        this.transitScheduleCreator = new TransitScheduleCreator(systemSize, railInterval, small_railInterval, this.freeSpeedTrain,
                transitStartTime, transitEndTime, transitStopLength, departureIntervalTime, carGridSpacing,
                linkCapacity, numberOfLanes, periodic_network);
        this.networkCreator = new NetworkCreator(systemSize, railInterval, small_railInterval, carGridSpacing, linkCapacity,
                effectiveFreeTrainSpeed, numberOfLanes, this.freeSpeedCar, diagonalConnections,
                smallLinksCloseToStations, createTrainLines, transitScheduleCreator, periodic_network);
        this.populationCreator = new PopulationCreator(nRequests, requestEndTime, random, transportMode, isGridNetwork,
                smallLinksCloseToStations, createTrainLines, this.travelDistanceDistribution, systemSize, avDrtDist,
                this.fracWithcommonOrigDest);
        this.drtFleetVehiclesCreator = new DrtFleetVehiclesCreator(drtCapacity, drtOperationStartTime,
                drtOperationEndTime, random, avDistFracToDCut, avDistFracFromDCut);
    }


    public static void main(String... args) {
        ScenarioCreator scenarioCreator = new ScenarioCreatorBuilder().build();

        String netPath = "./output/network_diag.xml.gz";
        String popPath = "./output/population.xml.gz";
        String drtFleetPath = "./output/drtvehicles.xml";
        String transitSchedulePath = "output/transitSchedule_15min.xml.gz";
        String transitVehiclesPath = "output/transitVehicles_15min.xml.gz";
        scenarioCreator.createNetwork(netPath, transitSchedulePath, transitVehiclesPath);
        scenarioCreator.createPopulation(popPath, netPath);
        scenarioCreator.createDrtFleet(netPath, drtFleetPath);
//        scenarioCreator.createTransitSchedule(netPath, transitSchedulePath, transitVehiclesPath);
    }

    public void createNetwork(String outputPathNet, String outputPathSchedule, String outputPathVehicles) {
        networkCreator.createGridNetwork(outputPathNet, outputPathSchedule, outputPathVehicles);
    }

    public void createPopulation(String outputPopulationPath, String networkPath) {
        populationCreator.createPopulation(outputPopulationPath, networkPath, constDrtDemand);
    }

//    public void createTransitSchedule(String networkPath, String outputSchedulePath, String outputVehiclePath) {
//        transitScheduleCreator.runTransitScheduleUtil(networkPath, outputSchedulePath, outputVehiclePath);
//    }

    public void createDrtFleet(String networkPath, String ouputPath) {
        drtFleetVehiclesCreator.run(networkPath, ouputPath, drtFleetSize);
    }

    public void createDrtFleet(String networkPath, String outputUnimPath, String outputBimPath) {
        drtFleetVehiclesCreator.runSplitted(networkPath, outputUnimPath, outputBimPath, drtFleetSize);
    }

    public double getSystemSize() {
        return systemSize;
    }

    public int getRailInterval() {
        return railInterval;
    }
    public int getSmall_railInterval() {return small_railInterval; }

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
