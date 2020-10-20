package de.mpi.ds.utils;

public interface UtilComponent {
    //Drt variables
    static final int numberOfDrtVehicles = 200;
    static final int seatsPerDrtVehicle = 4; //this is important for DRT, value is not used by taxi
    static final double operationStartTime = 0;
    static final double operationEndTime = 26 * 60 * 60; //24h

    //Population variables
    static final int MAX_END_TIME = 24 * 3600;
    static final int N_REQUESTS = 20000;
    static double delta_xy = 100;

    //Network variables
    // capacity at all links
    static final long CAP_MAIN = 1000; // [veh/h]
    // link length for all links
    static final long LINK_LENGTH = 100; // [m]
    // link freespeed for all links
    static final double FREE_SPEED = 30 / 3.6;
    static final double FREE_SPEED_TRAIN = 60 / 3.6;
    // This is neccessary so that trains are running according to their schedule
    static final double FREE_SPEED_TRAIN_FOR_SCHEDULE = 60 / 3.6 * 1.4;
    static final double NUMBER_OF_LANES = 4.;

    //Transit Schedule variables/shared variables
    static final int pt_interval = 10;
    static final double delta_x = 100;
    static final double delta_y = 100;
    static final double transitEndTime = 26 * 60 * 60;
    //TODO modify dis
    static final double transitIntervalTime = pt_interval * delta_x * 9 / FREE_SPEED_TRAIN * 2;
    //    static final double transitIntervalTime = 643;  //configured so that new trains start from both sides when old
    // one reaches end of grid
    static final double transitStopLength = 0;
    static final int n_xy = 101;
    static final int n_x = 101;
    static final int n_y = 101;
}
