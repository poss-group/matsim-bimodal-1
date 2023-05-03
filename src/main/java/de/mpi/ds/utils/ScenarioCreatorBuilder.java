package de.mpi.ds.utils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;

public class ScenarioCreatorBuilder {
    private final static Logger LOG = Logger.getLogger(ScenarioCreatorBuilder.class.getName());

    private double systemSize = 20000;
    //    private double railGridSpacing = 1000;
    private int railInterval = 2000;
    private int small_railInterval = 500;
    private double carGridSpacing = 1;
    private long linkCapacity = 9999999;
    private double freeSpeedCar = 30 / 3.6;
    private double freeSpeedTrain = 60 / 3.6;
    private double numberOfLanes = 100;

    private int requestEndTime = 1 * 3600;
    private int nRequests = (int) 1e5;
    private String transportMode = TransportMode.pt;
    private Boolean constDrtDemand = false;
    private double fracWithCommonOrigDest = 0;

    private double transitStartTime = 0;
    private double transitEndTime = 12*600;
    private double departureIntervalTime = 10 * 60;
    private double transitStopLength = 0;

    private int drtFleetSize = 200;
    private int drtCapacity = 8;
    private double drtOperationStartTime = 0;
    private double drtOperationEndTime = 12*600;
    private long seed = 42;

    private boolean diagonalConnetions = true;
    private boolean isGridNetwork = true;
    private boolean smallLinksCloseToNodes = false;
    private boolean createTrainLines = true;

    private String travelDistanceDistribution = "InverseGamma";
    private double travelDistanceMean = 2500;

    private double meanAndSpeedScaleFactor = 1;
    private double cutoffDistance = 1;

    private boolean periodic_network;

    public ScenarioCreatorBuilder setRailInterval(int railInterval) {
        this.railInterval = railInterval;
        return this;
    }
    public ScenarioCreatorBuilder setSmall_railInterval(int small_railInterval) {
        this.small_railInterval = small_railInterval;
        return this;
    }

    public ScenarioCreatorBuilder setSystemSize(double systemSize) {
        this.systemSize = systemSize;
        return this;
    }

    public ScenarioCreatorBuilder setCarGridSpacing(double carGridSpacing) {
        this.carGridSpacing = carGridSpacing;
        return this;
    }

    public ScenarioCreatorBuilder setLinkCapacity(long linkCapacity) {
        this.linkCapacity = linkCapacity;
        return this;
    }

    public ScenarioCreatorBuilder setFreeSpeedCar(double freeSpeedCar) {
        this.freeSpeedCar = freeSpeedCar;
        return this;
    }

    public ScenarioCreatorBuilder setFreeSpeedTrain(double freeSpeedTrain) {
        this.freeSpeedTrain = freeSpeedTrain;
        return this;
    }

    public ScenarioCreatorBuilder setFreeSpeedTrainForSchedule(double freeSpeedTrain) {
        this.freeSpeedTrain = freeSpeedTrain;
        return this;
    }

    public ScenarioCreatorBuilder setNumberOfLanes(double numberOfLanes) {
        this.numberOfLanes = numberOfLanes;
        return this;
    }

    public ScenarioCreatorBuilder setRequestEndTime(int requestEndTime) {
        this.requestEndTime = requestEndTime;
        return this;
    }

    public ScenarioCreatorBuilder setNRequests(int nRequests) {
        this.nRequests = nRequests;
        return this;
    }

    public ScenarioCreatorBuilder setTransitEndTime(double transitEndTime) {
        this.transitEndTime = transitEndTime;
        return this;
    }

    public ScenarioCreatorBuilder setDepartureIntervalTime(double departureIntervalTime) {
        this.departureIntervalTime = departureIntervalTime;
        return this;
    }

    public ScenarioCreatorBuilder setTransitStopLength(double transitStopLength) {
        this.transitStopLength = transitStopLength;
        return this;
    }

    public ScenarioCreatorBuilder setdrtFleetSize(int drtFleetSize) {
        this.drtFleetSize = drtFleetSize;
        return this;
    }

    public ScenarioCreatorBuilder setDrtCapacity(int drtCapacity) {
        this.drtCapacity = drtCapacity;
        return this;
    }

    public ScenarioCreatorBuilder setDrtOperationStartTime(double drtOperationStartTime) {
        this.drtOperationStartTime = drtOperationStartTime;
        return this;
    }

    public ScenarioCreatorBuilder setDrtOperationEndTime(double drtOperationEndTime) {
        this.drtOperationEndTime = drtOperationEndTime;
        return this;
    }

    public ScenarioCreatorBuilder setSeed(long seed) {
        this.seed = seed;
        return this;
    }

    public ScenarioCreatorBuilder setTransportMode(String transportMode) {
        this.transportMode = transportMode;
        return this;
    }

    public ScenarioCreatorBuilder setGridNetwork(boolean gridNetwork) {
        isGridNetwork = gridNetwork;
        return this;
    }

    public ScenarioCreatorBuilder setDiagonalConnetions(boolean diagonalConnetions) {
        this.diagonalConnetions = diagonalConnetions;
        return this;
    }

    public ScenarioCreatorBuilder setSmallLinksCloseToNodes(boolean smallLinksCloseToNodes) {
        this.smallLinksCloseToNodes = smallLinksCloseToNodes;
        return this;
    }

    public ScenarioCreatorBuilder setCreateTrainLines(boolean createTrainLines) {
        this.createTrainLines = createTrainLines;
        return this;
    }

    public ScenarioCreatorBuilder setTravelDistanceDistribution(String travelDistanceDistribution) {
        this.travelDistanceDistribution = travelDistanceDistribution;
        return this;
    }

    public ScenarioCreatorBuilder setTravelDistanceMean(double travelDistanceMean) {
        this.travelDistanceMean = travelDistanceMean;
        return this;
    }

    public ScenarioCreatorBuilder setMeanAndSpeedScaleFactor(double meanAndSpeedScaleFactor) {
        this.meanAndSpeedScaleFactor = meanAndSpeedScaleFactor;
        return this;
    }

    public ScenarioCreatorBuilder setTransitStartTime(double transitStartTime) {
        this.transitStartTime = transitStartTime;
        return this;
    }

    public ScenarioCreatorBuilder setCutoffDistance(double cutoffDistance) {
        this.cutoffDistance = cutoffDistance;
        return this;
    }

    public ScenarioCreatorBuilder setConstDrtDemand(Boolean constDrtDemand) {
        this.constDrtDemand = constDrtDemand;
        return this;
    }

    public ScenarioCreatorBuilder setFracWithCommonOrigDest(double fracWithCommonOrigDest) {
        this.fracWithCommonOrigDest = fracWithCommonOrigDest;
        return this;
    }

    public ScenarioCreatorBuilder setPeriodicNetwork(boolean periodic_network){
        this.periodic_network = periodic_network;
        return this;
    }

    public ScenarioCreatorBuilder setFreespeedTrain(int small_railInterval, double travelDistanceMean, double carGridSpacing){
        double Vm = 3;
        double max_speed = Vm*freeSpeedCar;
        double ta = 0.05*travelDistanceMean/freeSpeedCar;
        double V_tm;
        if (small_railInterval*carGridSpacing>max_speed*ta){
            V_tm = small_railInterval*carGridSpacing/(small_railInterval*carGridSpacing/max_speed + ta + ta);
        } else{
            V_tm = small_railInterval*carGridSpacing/(2*Math.sqrt(small_railInterval*carGridSpacing*ta/max_speed) + ta);
        }
        this.freeSpeedTrain = V_tm;
        return this;
    }

    public ScenarioCreator build() {
        printScenarioInfo();
        return new ScenarioCreator(systemSize, railInterval, small_railInterval, carGridSpacing, linkCapacity, freeSpeedCar,
                freeSpeedTrain, numberOfLanes, requestEndTime, nRequests, transitStartTime, transitEndTime,
                departureIntervalTime, transitStopLength, drtFleetSize, drtCapacity, drtOperationStartTime,
                drtOperationEndTime, seed, transportMode, isGridNetwork, diagonalConnetions, smallLinksCloseToNodes,
                createTrainLines, travelDistanceDistribution, travelDistanceMean, meanAndSpeedScaleFactor,
                cutoffDistance, constDrtDemand, fracWithCommonOrigDest, periodic_network);
    }

    private void printScenarioInfo() {
        LOG.info("Building Scenario with Specifications:" +
                "\nsystemSize: " + systemSize +
                "\nrailInterval: " + railInterval +
                "\nsmall_railInterval:" + small_railInterval +
                "\ncarGridSpacing: " + carGridSpacing +
                "\nlinkCapacity: " + linkCapacity +
                "\nfreeSpeedCar: " + freeSpeedCar +
                "\nfreeSpeedTrain: " + freeSpeedTrain +
                "\nnumberOfLanes: " + numberOfLanes +
                "\nrequestEndTime: " + requestEndTime +
                "\nnRequests: " + nRequests +
                "\ntransitEndTime: " + transitEndTime +
                "\ndepartureIntervalTime: " + departureIntervalTime +
                "\ntransitStopLength: " + transitStopLength +
                "\ndrtFleetSize: " + drtFleetSize +
                "\ndrtCapacity: " + drtCapacity +
                "\ndrtOperationStartTime: " + drtOperationStartTime +
                "\ndrtOperationEndTime: " + drtOperationEndTime +
                "\ndrtOperationStartTime: " + drtOperationStartTime +
                "\nseed: " + seed +
                "\ntransportMode: " + transportMode +
                "\nisGridNetwork: " + isGridNetwork +
                "\ndiagonalConnetions: " + diagonalConnetions +
                "\nsmallLinksCloseToNodes : " + smallLinksCloseToNodes +
                "\ncreateTrainLines : " + createTrainLines +
                "\ntravelDistanceDistribution : " + travelDistanceDistribution +
                "\ntravelDistanceMean : " + travelDistanceMean +
                "\nmeanAndSpeedScaleFactor : " + meanAndSpeedScaleFactor +
                "\ncutoffDistance : " + cutoffDistance +
                "\nconstDrtDemand : " + constDrtDemand +
                "\nfracWithCommonOrigDest :" + fracWithCommonOrigDest +
                "\nfreeSpeedTrain :" + freeSpeedTrain
        );
    }
}
