/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
 * *********************************************************************** */

package org.matsim.contrib.my_drt.optimizer.rebalancing.mincostflow;

import org.matsim.contrib.my_drt.optimizer.rebalancing.targetcalculator.RebalancingTargetCalculator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.my_drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.my_drt.analysis.zonal.DrtZone;
import org.matsim.contrib.my_drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.my_drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.my_drt.optimizer.rebalancing.RebalancingUtils;

import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * @author michalm
 */
public class MinCostFlowRebalancingStrategy implements RebalancingStrategy {

	private final RebalancingTargetCalculator rebalancingTargetCalculator;
	private final org.matsim.contrib.my_drt.analysis.zonal.DrtZonalSystem zonalSystem;
	private final Fleet fleet;
	private final org.matsim.contrib.my_drt.optimizer.rebalancing.mincostflow.ZonalRelocationCalculator relocationCalculator;
	private final org.matsim.contrib.my_drt.optimizer.rebalancing.RebalancingParams params;

	public MinCostFlowRebalancingStrategy(RebalancingTargetCalculator rebalancingTargetCalculator,
                                          DrtZonalSystem zonalSystem, Fleet fleet, ZonalRelocationCalculator relocationCalculator,
                                          RebalancingParams params) {
		this.rebalancingTargetCalculator = rebalancingTargetCalculator;
		this.zonalSystem = zonalSystem;
		this.fleet = fleet;
		this.relocationCalculator = relocationCalculator;
		this.params = params;
	}

	@Override
	public List<Relocation> calcRelocations(Stream<? extends DvrpVehicle> rebalancableVehicles, double time) {
		Map<org.matsim.contrib.my_drt.analysis.zonal.DrtZone, List<DvrpVehicle>> rebalancableVehiclesPerZone = org.matsim.contrib.my_drt.optimizer.rebalancing.RebalancingUtils.groupRebalancableVehicles(
				zonalSystem, params, rebalancableVehicles, time);
		if (rebalancableVehiclesPerZone.isEmpty()) {
			return List.of();
		}
		Map<org.matsim.contrib.my_drt.analysis.zonal.DrtZone, List<DvrpVehicle>> soonIdleVehiclesPerZone = RebalancingUtils.groupSoonIdleVehicles(zonalSystem,
				params, fleet, time);
		return calculateMinCostRelocations(time, rebalancableVehiclesPerZone, soonIdleVehiclesPerZone);
	}

	private List<Relocation> calculateMinCostRelocations(double time,
			Map<org.matsim.contrib.my_drt.analysis.zonal.DrtZone, List<DvrpVehicle>> rebalancableVehiclesPerZone,
			Map<org.matsim.contrib.my_drt.analysis.zonal.DrtZone, List<DvrpVehicle>> soonIdleVehiclesPerZone) {
		ToDoubleFunction<DrtZone> targetFunction = rebalancingTargetCalculator.calculate(time,
				rebalancableVehiclesPerZone);
		var minCostFlowRebalancingStrategyParams = (MinCostFlowRebalancingStrategyParams)params.getRebalancingStrategyParams();
		double alpha = minCostFlowRebalancingStrategyParams.getTargetAlpha();
		double beta = minCostFlowRebalancingStrategyParams.getTargetBeta();

		List<AggregatedMinCostRelocationCalculator.DrtZoneVehicleSurplus> vehicleSurpluses = zonalSystem.getZones().values().stream().map(z -> {
			int rebalancable = rebalancableVehiclesPerZone.getOrDefault(z, List.of()).size();
			int soonIdle = soonIdleVehiclesPerZone.getOrDefault(z, List.of()).size();
			int target = (int)Math.floor(alpha * targetFunction.applyAsDouble(z) + beta);
			int surplus = Math.min(rebalancable + soonIdle - target, rebalancable);
			return new AggregatedMinCostRelocationCalculator.DrtZoneVehicleSurplus(z, surplus);
		}).collect(toList());

		return relocationCalculator.calcRelocations(vehicleSurpluses, rebalancableVehiclesPerZone);
	}
}
