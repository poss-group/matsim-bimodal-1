package de.mpi.ds.osm_utils;

import de.mpi.ds.utils.ScenarioCreator;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;

public class ScenarioCreatorBuilderOsm {
    private final static Logger LOG = Logger.getLogger(ScenarioCreatorBuilderOsm.class.getName());

    private long linkCapacity = 9999999;
    private double freeSpeedCar = 30 / 3.6;
    private double freeSpeedTrain = 60 / 3.6;
    private double numberOfLanes = 100;

    private int requestEndTime = 9 * 3600;
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

    private String travelDistanceDistribution = "InverseGamma";
    private double travelDistanceMeanOverL = 1/4;

    public ScenarioCreatorBuilderOsm setLinkCapacity(long linkCapacity) {
        this.linkCapacity = linkCapacity;
        return this;
    }

    public ScenarioCreatorBuilderOsm setFreeSpeedCar(double freeSpeedCar) {
        this.freeSpeedCar = freeSpeedCar;
        return this;
    }

    public ScenarioCreatorBuilderOsm setFreeSpeedTrain(double freeSpeedTrain) {
        this.freeSpeedTrain = freeSpeedTrain;
        return this;
    }

    public ScenarioCreatorBuilderOsm setFreeSpeedTrainForSchedule(double freeSpeedTrain) {
        this.freeSpeedTrain = freeSpeedTrain;
        return this;
    }

    public ScenarioCreatorBuilderOsm setNumberOfLanes(double numberOfLanes) {
        this.numberOfLanes = numberOfLanes;
        return this;
    }

    public ScenarioCreatorBuilderOsm setRequestEndTime(int requestEndTime) {
        this.requestEndTime = requestEndTime;
        return this;
    }

    public ScenarioCreatorBuilderOsm setNRequests(int nRequests) {
        this.nRequests = nRequests;
        return this;
    }

    public ScenarioCreatorBuilderOsm setTransitEndTime(double transitEndTime) {
        this.transitEndTime = transitEndTime;
        return this;
    }

    public ScenarioCreatorBuilderOsm setDepartureIntervalTime(double departureIntervalTime) {
        this.departureIntervalTime = departureIntervalTime;
        return this;
    }

    public ScenarioCreatorBuilderOsm setTransitStopLength(double transitStopLength) {
        this.transitStopLength = transitStopLength;
        return this;
    }

    public ScenarioCreatorBuilderOsm setNDrtVehicles(int nDrtVehicles) {
        this.nDrtVehicles = nDrtVehicles;
        return this;
    }

    public ScenarioCreatorBuilderOsm setDrtCapacity(int drtCapacity) {
        this.drtCapacity = drtCapacity;
        return this;
    }

    public ScenarioCreatorBuilderOsm setDrtOperationStartTime(double drtOperationStartTime) {
        this.drtOperationStartTime = drtOperationStartTime;
        return this;
    }

    public ScenarioCreatorBuilderOsm setDrtOperationEndTime(double drtOperationEndTime) {
        this.drtOperationEndTime = drtOperationEndTime;
        return this;
    }

    public ScenarioCreatorBuilderOsm setSeed(long seed) {
        this.seed = seed;
        return this;
    }

    public ScenarioCreatorBuilderOsm setTransportMode(String transportMode) {
        this.transportMode = transportMode;
        return this;
    }

    public ScenarioCreatorBuilderOsm setTravelDistanceDistribution(String travelDistanceDistribution) {
        this.travelDistanceDistribution = travelDistanceDistribution;
        return this;
    }

    public ScenarioCreatorBuilderOsm setTravelDistanceMeanOverL(double travelDistanceMeanOverL) {
        this.travelDistanceMeanOverL = travelDistanceMeanOverL;
        return this;
    }

    public ScenarioCreatorOsm build() {
        printScenarioInfo();
        return new ScenarioCreatorOsm(linkCapacity, freeSpeedCar,
                freeSpeedTrain, numberOfLanes, requestEndTime, nRequests, transitEndTime,
                departureIntervalTime, transitStopLength, nDrtVehicles, drtCapacity, drtOperationStartTime,
                drtOperationEndTime, seed, transportMode,
                travelDistanceDistribution, travelDistanceMeanOverL);
    }

    private void printScenarioInfo() {
        LOG.info("Building Scenario with Specifications:" +
                "\nlinkCapacity: " + linkCapacity +
                "\nfreeSpeedCar: " + freeSpeedCar +
                "\nfreeSpeedTrain: " + freeSpeedTrain +
                "\nnumberOfLanes: " + numberOfLanes +
                "\nrequestEndTime: " + requestEndTime +
                "\nnRequests: " + nRequests +
                "\ntransitEndTime: " + transitEndTime +
                "\ndepartureIntervalTime: " + departureIntervalTime +
                "\ntransitStopLength: " + transitStopLength +
                "\nnDrtVehicles: " + nDrtVehicles +
                "\ndrtCapacity: " + drtCapacity +
                "\ndrtOperationStartTime: " + drtOperationStartTime +
                "\ndrtOperationEndTime: " + drtOperationEndTime +
                "\ndrtOperationStartTime: " + drtOperationStartTime +
                "\nseed: " + seed +
                "\ntransportMode: " + transportMode +
                "\ntravelDistanceDistribution : " + travelDistanceDistribution +
                "\ntravelDistanceMeanOverL : " + travelDistanceMeanOverL
        );
    }
}
