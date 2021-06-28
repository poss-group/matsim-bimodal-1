package de.mpi.ds.osm_utils;

import de.mpi.ds.polygon_utils.AlphaShape;
import de.mpi.ds.utils.*;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;

import java.util.ArrayList;
import java.util.Random;

public class ScenarioCreatorOsm {
    private final static Logger LOG = Logger.getLogger(ScenarioCreatorOsm.class.getName());

    public final static String IS_START_LINK = "isStartLink";
    public final static String IS_STATION_NODE = "isStation";
//    public final static String NETWORK_MODE_TRAIN = "train";
//    public final static String NETWORK_MODE_CAR = "car";

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
    private double meanTravelDist;
    private double effectiveFreeTrainSpeed;
    private double ptSpacingOverMean;
    private Network net = null;
    private ArrayList<Coord> hull = null;
    private Random rand;

    public ScenarioCreatorOsm(long linkCapacity, double freeSpeedCar, double freeSpeedTrain,
                              double numberOfLanes, int requestEndTime, int nRequests,
                              double transitEndTime, double departureIntervalTime, double transitStopLength,
                              int nDrtVehicles, int drtCapacity, double drtOperationStartTime,
                              double drtOperationEndTime,
                              long seed, String transportMode, String travelDistanceDistribution,
                              double meanTravelDist, double ptSpacingOverMean) {

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
        this.meanTravelDist = meanTravelDist;
        this.ptSpacingOverMean = ptSpacingOverMean;
        this.effectiveFreeTrainSpeed = freeSpeedTrain;
        this.rand = new Random(seed);

        // Apparently every stops must take 2 seconds -> calc effective velocity to cover distance in planned time
//        int numberOfStopsPerLine = (int) (systemSize/carGridSpacing)/railInterval;
//        this.effectiveFreeTrainSpeed = systemSize/(600-numberOfStopsPerLine*2);

//        assert railInterval > 0 : "Pt grid spacing must be bigger than drt grid spacing";
//        assert carGridSpacing * railInterval < systemSize : "Rail interval bigger than sysem size";
//        assert railGridSpacing % carGridSpacing == 0 :
//                "Pt grid spacing mus be integer multiple of drt grid spacing";

//        this.random = new Random(seed);

//        this.networkCreatorFromOsm = new NetworkCreatorFromOsm(linkCapacity, effectiveFreeTrainSpeed, numberOfLanes,
//                freeSpeedCar);
    }


    public static void main(String... args) {
        ScenarioCreatorOsm scenarioCreator = new ScenarioCreatorBuilderOsm().setNRequests(1000).setNDrtVehicles(300).build();

//        String netPath = "./output/network_diag.xml.gz";
//        String popPath = "./output/population.xml.gz";
//        String drtFleetPath = "output/drtvehicles.xml";
//        String transitSchedulePath = "output/transitSchedule_15min.xml.gz";
//        String transitVehiclesPath = "output/transitVehicles_15min.xml.gz";
//        scenarioCreator.createNetwork(netPath);
//        scenarioCreator.createPopulation(popPath, netPath);
//        scenarioCreator.createDrtFleet(netPath, drtFleetPath);
//        scenarioCreator.createTransitSchedule(netPath, transitSchedulePath, transitVehiclesPath);
        String inPathNet = "/home/helge/Applications/matsim/matsim-bimodal.git/master/scenarios/Manhatten/network_clean.xml";
        String outPathNet = "/home/helge/Applications/matsim/matsim-bimodal.git/master/scenarios/Manhatten/network_trams.xml";
        String outPathTransitSchedule = "/home/helge/Applications/matsim/matsim-bimodal.git/master/scenarios/Manhatten/transit_schedule.xml";
        String outPathTransitVehicles = "/home/helge/Applications/matsim/matsim-bimodal.git/master/scenarios/Manhatten/transit_vehicles.xml";
        String populationPath = "/home/helge/Applications/matsim/matsim-bimodal.git/master/scenarios/Manhatten/population.xml";
        String drtPath = "/home/helge/Applications/matsim/matsim-bimodal.git/master/scenarios/Manhatten/drtvehicles.xml";
        scenarioCreator.addTramsToNetwork(inPathNet, outPathNet, outPathTransitSchedule, outPathTransitVehicles);
        scenarioCreator.generatePopulation(populationPath);
        scenarioCreator.generateDrtVehicles(drtPath);


//        String networkPath = "scenarios/Manhatten/network_trams.xml";
//        String outputPath = "scenarios/Manhatten/population.xml";
//
//        new DrtFleetVehiclesCreator(4, 0, 26 * 3600, 10, new Random())
//                .run("scenarios/Manhatten/network_trams.xml", "scenarios/Manhatten/drtvehicles.xml");
    }

    public void generateDrtVehicles(String drtPath) {
        new DrtFleetVehiclesCreator(drtCapacity, drtOperationStartTime, drtOperationEndTime, nDrtVehicles, rand)
                .run(net, drtPath);
    }

    public void generatePopulation(String outPath) {
        PopulationCreatorOsm populationCreatorOsm = new PopulationCreatorOsm(nRequests, requestEndTime, rand,
                transportMode, travelDistanceDistribution, meanTravelDist);
        populationCreatorOsm.createPopulation(outPath, net, hull);
    }

    public void addTramsToNetwork(String networkInPath, String networkOutPath, String transitScheduleOutPath, String transitVehiclesOutPath) {
        NetworkCleaner networkCleaner = new NetworkCleaner(linkCapacity, freeSpeedCar, numberOfLanes);
        net = networkCleaner.cleanNetwork(networkInPath);
        hull = new AlphaShape(net.getNodes().values(), 0.1).compute();

        NetworkCreatorFromOsm networkCreatorFromOsm = new NetworkCreatorFromOsm(hull, linkCapacity, freeSpeedTrain, numberOfLanes, freeSpeedCar, 0,
                transitEndTime, departureIntervalTime, ptSpacingOverMean*meanTravelDist);
        net = networkCreatorFromOsm.addTramNet(net, networkOutPath, transitScheduleOutPath, transitVehiclesOutPath);
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

    public String getTravelDistanceDistribution() {
        return travelDistanceDistribution;
    }

    public double getMeanTravelDist() {
        return meanTravelDist;
    }

    public double getEffectiveFreeTrainSpeed() {
        return effectiveFreeTrainSpeed;
    }

    public double getPtSpacingOverMean() {
        return ptSpacingOverMean;
    }

    public Network getNet() {
        return net;
    }

    public ArrayList<Coord> getHull() {
        return hull;
    }

    public Random getRand() {
        return rand;
    }
}
