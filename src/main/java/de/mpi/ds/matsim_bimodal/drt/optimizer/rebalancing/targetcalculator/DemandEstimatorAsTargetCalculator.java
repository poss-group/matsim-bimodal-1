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

package de.mpi.ds.matsim_bimodal.drt.optimizer.rebalancing.targetcalculator;

import de.mpi.ds.matsim_bimodal.drt.analysis.zonal.DrtZone;
import de.mpi.ds.matsim_bimodal.drt.optimizer.rebalancing.demandestimator.ZonalDemandEstimator;
import de.mpi.ds.matsim_bimodal.drt.optimizer.rebalancing.targetcalculator.RebalancingTargetCalculator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;

import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/**
 * @author michalm
 */
public class DemandEstimatorAsTargetCalculator implements RebalancingTargetCalculator {
	private final ZonalDemandEstimator demandEstimator;

	public DemandEstimatorAsTargetCalculator(ZonalDemandEstimator demandEstimator) {
		this.demandEstimator = demandEstimator;
	}

	@Override
	public ToDoubleFunction<DrtZone> calculate(double time,
			Map<DrtZone, List<DvrpVehicle>> rebalancableVehiclesPerZone) {
		// TODO remove this hidden "+60"
		// XXX this "time+60" (taken from old code) means probably "in the next time bin"
		return demandEstimator.getExpectedDemandForTimeBin(time + 60);
	}
}
