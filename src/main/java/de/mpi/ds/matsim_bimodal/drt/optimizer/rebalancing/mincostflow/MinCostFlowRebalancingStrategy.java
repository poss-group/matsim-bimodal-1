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

package de.mpi.ds.matsim_bimodal.drt.optimizer.rebalancing.mincostflow;

import de.mpi.ds.matsim_bimodal.drt.analysis.zonal.DrtZonalSystem;
import de.mpi.ds.matsim_bimodal.drt.analysis.zonal.DrtZone;
import de.mpi.ds.matsim_bimodal.drt.optimizer.rebalancing.RebalancingParams;
import de.mpi.ds.matsim_bimodal.drt.optimizer.rebalancing.RebalancingStrategy;
import de.mpi.ds.matsim_bimodal.drt.optimizer.rebalancing.RebalancingUtils;
import de.mpi.ds.matsim_bimodal.drt.optimizer.rebalancing.mincostflow.AggregatedMinCostRelocationCalculator.DrtZoneVehicleSurplus;
import de.mpi.ds.matsim_bimodal.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategyParams;
import de.mpi.ds.matsim_bimodal.drt.optimizer.rebalancing.mincostflow.ZonalRelocationCalculator;
import de.mpi.ds.matsim_bimodal.drt.optimizer.rebalancing.targetcalculator.RebalancingTargetCalculator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;

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
	private final DrtZonalSystem zonalSystem;
	private final Fleet fleet;
	private final ZonalRelocationCalculator relocationCalculator;
	private final RebalancingParams params;

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
		Map<DrtZone, List<DvrpVehicle>> rebalancableVehiclesPerZone = RebalancingUtils.groupRebalancableVehicles(
				zonalSystem, params, rebalancableVehicles, time);
		if (rebalancableVehiclesPerZone.isEmpty()) {
			return List.of();
		}
		Map<DrtZone, List<DvrpVehicle>> soonIdleVehiclesPerZone = RebalancingUtils.groupSoonIdleVehicles(zonalSystem,
				params, fleet, time);
		return calculateMinCostRelocations(time, rebalancableVehiclesPerZone, soonIdleVehiclesPerZone);
	}

	private List<Relocation> calculateMinCostRelocations(double time,
			Map<DrtZone, List<DvrpVehicle>> rebalancableVehiclesPerZone,
			Map<DrtZone, List<DvrpVehicle>> soonIdleVehiclesPerZone) {
		ToDoubleFunction<DrtZone> targetFunction = rebalancingTargetCalculator.calculate(time,
				rebalancableVehiclesPerZone);
		var minCostFlowRebalancingStrategyParams = (MinCostFlowRebalancingStrategyParams)params.getRebalancingStrategyParams();
		double alpha = minCostFlowRebalancingStrategyParams.getTargetAlpha();
		double beta = minCostFlowRebalancingStrategyParams.getTargetBeta();

		List<DrtZoneVehicleSurplus> vehicleSurpluses = zonalSystem.getZones().values().stream().map(z -> {
			int rebalancable = rebalancableVehiclesPerZone.getOrDefault(z, List.of()).size();
			int soonIdle = soonIdleVehiclesPerZone.getOrDefault(z, List.of()).size();
			int target = (int)Math.floor(alpha * targetFunction.applyAsDouble(z) + beta);
			int surplus = Math.min(rebalancable + soonIdle - target, rebalancable);
			return new DrtZoneVehicleSurplus(z, surplus);
		}).collect(toList());

		return relocationCalculator.calcRelocations(vehicleSurpluses, rebalancableVehiclesPerZone);
	}
}
