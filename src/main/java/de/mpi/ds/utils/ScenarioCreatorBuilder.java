package de.mpi.ds.utils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;

public class ScenarioCreatorBuilder {
    private final static Logger LOG = Logger.getLogger(ScenarioCreatorBuilder.class.getName());

    private double systemSize = 10000;
    //    private double railGridSpacing = 1000;
    private int railInterval = 5;
    private double carGridSpacing = 100;
    private long linkCapacity = 9999999;
    private double freeSpeedCar = 30 / 3.6;
    private double freeSpeedTrain = 60 / 3.6;
    private double freeSpeedTrainForSchedule = 60 / 3.6;
    private double numberOfLanes = 100;

    private int requestEndTime = 10 * 3600;
    private int nRequests = (int) 1e5;
    private String transportMode = TransportMode.pt;

    private double transitEndTime = 10 * 3600;
    private double departureIntervalTime = 15 * 60;
    private double transitStopLength = 0;

    private int nDrtVehicles = 200;
    private int drtCapacity = 8;
    private double drtOperationStartTime = 0;
    private double drtOperationEndTime = 10 * 3600;
    private long seed = 42;

    private boolean diagonalConnetions = true;
    private boolean isGridNetwork = true;
    private boolean smallLinksCloseToNodes = false;
    private boolean createTrainLines = true;

    private String travelDistanceDistribution = "InverseGamma";
    private double travelDistanceMeanOverL = 1/4;

    public ScenarioCreatorBuilder setRailInterval(int railInterval) {
        this.railInterval = railInterval;
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

    public ScenarioCreatorBuilder setFreeSpeedTrainForSchedule(double freeSpeedTrainForSchedule) {
        this.freeSpeedTrainForSchedule = freeSpeedTrainForSchedule;
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

    public ScenarioCreatorBuilder setNDrtVehicles(int nDrtVehicles) {
        this.nDrtVehicles = nDrtVehicles;
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

    public ScenarioCreatorBuilder setTravelDistanceMeanOverL(double travelDistanceMeanOverL) {
        this.travelDistanceMeanOverL = travelDistanceMeanOverL;
        return this;
    }

    public ScenarioCreator build() {
        printScenarioInfo();
        return new ScenarioCreator(systemSize, railInterval, carGridSpacing, linkCapacity, freeSpeedCar,
                freeSpeedTrain, freeSpeedTrainForSchedule, numberOfLanes, requestEndTime, nRequests, transitEndTime,
                departureIntervalTime, transitStopLength, nDrtVehicles, drtCapacity, drtOperationStartTime,
                drtOperationEndTime, seed, transportMode, isGridNetwork, diagonalConnetions, smallLinksCloseToNodes,
                createTrainLines, travelDistanceDistribution, travelDistanceMeanOverL);
    }

    private void printScenarioInfo() {
        LOG.info("Building Scenario with Specifications:\n" +
                "systemSize: " + systemSize +
                "railInterval: " + railInterval +
                "carGridSpacing: " + carGridSpacing +
                "linkCapacity: " + linkCapacity +
                "freeSpeedCar: " + freeSpeedCar +
                "freeSpeedTrain: " + freeSpeedTrain +
                "freeSpeedTrainForSchedule: " + freeSpeedTrainForSchedule +
                "numberOfLanes: " + numberOfLanes +
                "requestEndTime: " + requestEndTime +
                "nRequests: " + nRequests +
                "transitEndTime: " + transitEndTime +
                "departureIntervalTime: " + departureIntervalTime +
                "transitStopLength: " + transitStopLength +
                "nDrtVehicles: " + nDrtVehicles +
                "drtCapacity: " + drtCapacity +
                "drtOperationStartTime: " + drtOperationStartTime +
                "drtOperationEndTime: " + drtOperationEndTime +
                "drtOperationStartTime: " + drtOperationStartTime +
                "seed: " + seed +
                "transportMode: " + transportMode +
                "isGridNetwork: " + isGridNetwork +
                "diagonalConnetions: " + diagonalConnetions +
                "smallLinksCloseToNodes : " + smallLinksCloseToNodes +
                "createTrainLines : " + createTrainLines +
                "travelDistanceDistribution : " + travelDistanceDistribution +
                "travelDistanceMeanOverL : " + travelDistanceMeanOverL
        );
    }
}
