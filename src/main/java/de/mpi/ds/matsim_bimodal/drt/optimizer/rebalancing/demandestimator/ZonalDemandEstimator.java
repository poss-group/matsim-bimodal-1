/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** *
 */

/**
 *
 */
package de.mpi.ds.matsim_bimodal.drt.optimizer.rebalancing.demandestimator;

import de.mpi.ds.matsim_bimodal.drt.analysis.zonal.DrtZone;

import java.util.function.ToDoubleFunction;

/**
 * @author jbischoff
 */
public interface ZonalDemandEstimator {
	ToDoubleFunction<DrtZone> getExpectedDemandForTimeBin(double time);
}
