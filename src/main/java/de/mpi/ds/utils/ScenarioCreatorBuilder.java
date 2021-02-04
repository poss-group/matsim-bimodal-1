package de.mpi.ds.utils;

public class ScenarioCreatorBuilder {
    private double cellLength = 1000;
    private int gridLengthInCells = 10;
    private int ptInterval = 4;
    private long linkCapacity = 1000;
    private double freeSpeedCar= 30/3.6;
    private double freeSpeedTrain = 60/3.6;
    private double freeSpeedTrainForSchedule = 60/3.6*1.4;
    private double numberOfLanes = 4;

    private int requestEndTime = 24*3600;
    private int nRequests = (int) 1e5;

    private double transitEndTime = 26*3600;
    private double departureIntervalTime = 15*60;
    private double transitStopLength = 0;

    private int nDrtVehicles = 200;
    private int drtCapacity = 4;
    private double drtOperationStartTime = 0;
    private double drtOperationEndTime = 26*3600;
    private long seed = 42;

    public ScenarioCreatorBuilder setCellLength(double cellLength) {
        this.cellLength = cellLength;
        return this;
    }

    public ScenarioCreatorBuilder gridLengthInCells(int gridLengthInCells) {
        this.gridLengthInCells = gridLengthInCells;
        return this;
    }

    public ScenarioCreatorBuilder setPtInterval(int ptInterval) {
        this.ptInterval = ptInterval;
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

    public ScenarioCreator build() {
        return new ScenarioCreator(cellLength, gridLengthInCells, ptInterval, linkCapacity, freeSpeedCar,
                freeSpeedTrain, freeSpeedTrainForSchedule, numberOfLanes, requestEndTime, nRequests, transitEndTime,
                departureIntervalTime, transitStopLength, nDrtVehicles, drtCapacity, drtOperationStartTime,
                drtOperationEndTime, seed);
    }
}
