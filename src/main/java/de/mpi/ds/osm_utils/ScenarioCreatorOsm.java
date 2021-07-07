package de.mpi.ds.osm_utils;

import de.mpi.ds.polygon_utils.AlphaShape;
import de.mpi.ds.utils.*;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;

import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

import static de.mpi.ds.utils.InverseTransformSampler.taxiDistDistribution;
import static de.mpi.ds.utils.InverseTransformSampler.taxiDistDistributionNotNormalized;

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
    private int drtFleetSize;
    private int drtCapacity;
    private double drtOperationStartTime;
    private double drtOperationEndTime;
    private long seed;
    private String transportMode;
    private Function<Double, Double> travelDistanceDistribution;
    private double meanTravelDist;
    private double effectiveFreeTrainSpeed;
    private double ptSpacingOverMean;
    private Network net = null;
    private ArrayList<Coord> hull = null;
    private Random rand;

    private static final Map<String, Double> normalizedAlphas = Map.of(
            "Manhatten", 0.1,
            //Berlin needs bigger normalized alpha, because of "Tempelhofer Feld"
            "Berlin", 0.15);

    public ScenarioCreatorOsm(long linkCapacity, double freeSpeedCar, double freeSpeedTrain,
                              double numberOfLanes, int requestEndTime, int nRequests,
                              double transitEndTime, double departureIntervalTime, double transitStopLength,
                              int drtFleetSize, int drtCapacity, double drtOperationStartTime,
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
        this.drtFleetSize = drtFleetSize;
        this.drtCapacity = drtCapacity;
        this.drtOperationStartTime = drtOperationStartTime;
        this.drtOperationEndTime = drtOperationEndTime;
        this.seed = seed;
        this.transportMode = transportMode;
        this.meanTravelDist = meanTravelDist;
        this.ptSpacingOverMean = ptSpacingOverMean;
        this.effectiveFreeTrainSpeed = freeSpeedTrain;
        this.rand = new Random(seed);
        if (travelDistanceDistribution.equals("InverseGamma")) {
            this.travelDistanceDistribution = x -> taxiDistDistributionNotNormalized(x, meanTravelDist, 3.1);
        } else if (travelDistanceDistribution.equals("Uniform")) {
            this.travelDistanceDistribution = x -> x < meanTravelDist * 2 ? 1 / meanTravelDist * 2 : 0;
        }

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
        ScenarioCreatorOsm scenarioCreator = new ScenarioCreatorBuilderOsm().setNRequests(1000).setdrtFleetSize(300)
                .build();

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
        String drtOuputPath = "/home/helge/Applications/matsim/matsim-bimodal.git/master/scenarios/Manhatten/drtvehicles.xml";
        scenarioCreator.addTramsToNetwork(inPathNet, outPathNet, outPathTransitSchedule, outPathTransitVehicles);
        scenarioCreator.generatePopulation(populationPath);
        scenarioCreator.generateDrtVehicles(drtOuputPath);


//        String networkPath = "scenarios/Manhatten/network_trams.xml";
//        String outputPath = "scenarios/Manhatten/population.xml";
//
//        new DrtFleetVehiclesCreator(4, 0, 26 * 3600, 10, new Random())
//                .run("scenarios/Manhatten/network_trams.xml", "scenarios/Manhatten/drtvehicles.xml");
    }

    public void generateDrtVehicles(String drtOuputPath) {
        new DrtFleetVehiclesCreator(drtCapacity, drtOperationStartTime, drtOperationEndTime, rand,
                travelDistanceDistribution, meanTravelDist, ptSpacingOverMean*meanTravelDist)
                .run(net, drtOuputPath, drtFleetSize);
    }

    public void generateDrtVehicles(String drtOuputPath, String drtOutputBimPath, double zetaCut) {
        new DrtFleetVehiclesCreator(drtCapacity, drtOperationStartTime, drtOperationEndTime, rand,
                travelDistanceDistribution, meanTravelDist, ptSpacingOverMean*meanTravelDist)
                .run(net, drtOuputPath, drtOutputBimPath, zetaCut, drtFleetSize);
    }

    public void generatePopulation(String outPath) {
        PopulationCreatorOsm populationCreatorOsm = new PopulationCreatorOsm(nRequests, requestEndTime, rand,
                transportMode, travelDistanceDistribution, meanTravelDist);
        populationCreatorOsm.createPopulation(outPath, net, hull);
    }

    public void addTramsToNetwork(String networkInPath, String networkOutPath, String transitScheduleOutPath,
                                  String transitVehiclesOutPath) {
        NetworkCleaner networkCleaner = new NetworkCleaner(linkCapacity, freeSpeedCar, numberOfLanes);
        net = networkCleaner.cleanNetwork(networkInPath);
        double normAlpha = getAlpha(networkInPath);
        hull = new AlphaShape(net.getNodes().values(), normAlpha).compute();

        NetworkCreatorFromOsm networkCreatorFromOsm = new NetworkCreatorFromOsm(hull, linkCapacity, freeSpeedTrain,
                numberOfLanes, freeSpeedCar, 0,
                transitEndTime, departureIntervalTime, ptSpacingOverMean * meanTravelDist);
        net = networkCreatorFromOsm.addTramNet(net, networkOutPath, transitScheduleOutPath, transitVehiclesOutPath);
    }

    private double getAlpha(String networkInPath) {
        for (String city : normalizedAlphas.keySet()) {
            if (networkInPath.contains(city)) {
                return normalizedAlphas.get(city);
            }
        }
        return 0.1;
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

    public Function<Double, Double> getTravelDistanceDistribution() {
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

    public void setNet(Network net) {
        this.net = net;
    }
}
