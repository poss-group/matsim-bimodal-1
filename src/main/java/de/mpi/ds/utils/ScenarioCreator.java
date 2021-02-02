package de.mpi.ds.utils;

import org.apache.log4j.Logger;
import org.jfree.util.Log;

public class ScenarioCreator {
    private final static Logger LOG = Logger.getLogger(ScenarioCreator.class.getName());

    private final double cellLength;
    private final int gridLengthInCells;
    private final int ptInterval;
    private final long linkCapacity;
    private final double freeSpeedCar;
    private final double freeSpeedTrain;
    private final double freeSpeedTrainForSchedule;
    private final double numberOfLanes;

    private final double requestEndTime;
    private final int nRequests;

    private final double transitEndTime;
    private final double departureIntervalTime;
    private final double transitStopLength;

    public ScenarioCreator() {
        LOG.warn("Taking default values for scenario parameters");
        cellLength = 1000;
        gridLengthInCells = 10;
        ptInterval = 4;
        linkCapacity = 1000; // [veh/h]
        freeSpeedCar = 30 / 3.6;
        freeSpeedTrain = 60 / 3.6;
        freeSpeedTrainForSchedule = 60 / 3.6 * 1.4;
        numberOfLanes = 4.;

        requestEndTime = 24 * 3600;
        nRequests = 1000;

        transitEndTime = 26 * 60 * 60;
        departureIntervalTime = 15 * 60;
        transitStopLength = 0;
    }

    public ScenarioCreator(double cellLength, int gridLengthInCells, int ptInterval, long linkCapacity,
                           double freeSpeedCar,
                           double freeSpeedTrain, double freeSpeedTrainForSchedule, int numberOfLanes,
                           double requestEndTime, int nRequests, double transitEndTime,
                           double departureIntervalTime, double transitStopLength) {

        this.cellLength = cellLength;
        this.gridLengthInCells = gridLengthInCells;
        this.ptInterval = ptInterval;
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
    }

    public static void main(String... args) {
        ScenarioCreator scenarioCreator = new ScenarioCreator();
    }

    public void createNetwork(String path, boolean createTrainLines) {
        NetworkCreator networkCreator = new NetworkCreator(cellLength, gridLengthInCells, ptInterval);
        networkCreator.createGridNetwork(path, createTrainLines);
    }

    public void createPopulation() {

    }
}
