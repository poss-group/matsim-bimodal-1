package de.mpi.ds.utils;

import org.matsim.api.core.v01.TransportMode;

public class ScenarioCreatorBuilder {
    private double systemSize = 10000;
    private double railGridSpacing = 1000;
    private double carGridSpacing = 100;
    private long linkCapacity = 9999999;
    private double freeSpeedCar = 30 / 3.6;
    private double freeSpeedTrain = 60 / 3.6;
    private double freeSpeedTrainForSchedule = 60 / 3.6 * 1.4;
    private double numberOfLanes = 100;

    private int requestEndTime = 23 * 3600;
    private int nRequests = (int) 1e5;
    private String transportMode = TransportMode.pt;

    private double transitEndTime = 24 * 3600;
    private double departureIntervalTime = 15 * 60;
    private double transitStopLength = 0;

    private int nDrtVehicles = 200;
    private int drtCapacity = 8;
    private double drtOperationStartTime = 0;
    private double drtOperationEndTime = 24 * 3600;
    private long seed = 42;

    private boolean diagonalConnetions = true;
    private boolean isGridNetwork = true;

    public ScenarioCreatorBuilder setRailGridSpacing(double railGridSpacing) {
        this.railGridSpacing = railGridSpacing;
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

    public ScenarioCreatorBuilder setnRequests(int nRequests) {
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

    public ScenarioCreatorBuilder setnDrtVehicles(int nDrtVehicles) {
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

    public ScenarioCreator build() {
        return new ScenarioCreator(systemSize, railGridSpacing, carGridSpacing, linkCapacity, freeSpeedCar,
                freeSpeedTrain, freeSpeedTrainForSchedule, numberOfLanes, requestEndTime, nRequests, transitEndTime,
                departureIntervalTime, transitStopLength, nDrtVehicles, drtCapacity, drtOperationStartTime,
                drtOperationEndTime, seed, transportMode, isGridNetwork, diagonalConnetions);
    }
}
