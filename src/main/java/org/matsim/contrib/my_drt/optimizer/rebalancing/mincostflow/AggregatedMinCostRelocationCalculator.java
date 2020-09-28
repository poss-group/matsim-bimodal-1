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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.my_drt.analysis.zonal.DrtZone;
import org.matsim.contrib.my_drt.analysis.zonal.DrtZoneTargetLinkSelector;
import org.matsim.contrib.my_drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.util.distance.DistanceUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Computes inter-zonal flows at the zonal (aggregated) level (i.e. without looking into individual vehicles)
 *
 * @author michalm
 */
public class AggregatedMinCostRelocationCalculator implements ZonalRelocationCalculator {
	public static class DrtZoneVehicleSurplus {
		public final org.matsim.contrib.my_drt.analysis.zonal.DrtZone zone;
		public final int surplus;

		public DrtZoneVehicleSurplus(org.matsim.contrib.my_drt.analysis.zonal.DrtZone zone, int surplus) {
			this.zone = zone;
			this.surplus = surplus;
		}
	}

	private final org.matsim.contrib.my_drt.analysis.zonal.DrtZoneTargetLinkSelector targetLinkSelector;

	public AggregatedMinCostRelocationCalculator(DrtZoneTargetLinkSelector targetLinkSelector) {
		this.targetLinkSelector = targetLinkSelector;
	}

	@Override
	public List<org.matsim.contrib.my_drt.optimizer.rebalancing.RebalancingStrategy.Relocation> calcRelocations(List<DrtZoneVehicleSurplus> vehicleSurplus,
                                                                                                                Map<org.matsim.contrib.my_drt.analysis.zonal.DrtZone, List<DvrpVehicle>> rebalancableVehiclesPerZone) {
		return calcRelocations(rebalancableVehiclesPerZone, org.matsim.contrib.my_drt.optimizer.rebalancing.mincostflow.TransportProblem.solveForVehicleSurplus(vehicleSurplus));
	}

	private List<org.matsim.contrib.my_drt.optimizer.rebalancing.RebalancingStrategy.Relocation> calcRelocations(Map<org.matsim.contrib.my_drt.analysis.zonal.DrtZone, List<DvrpVehicle>> rebalancableVehiclesPerZone,
                                                                                                                 List<org.matsim.contrib.my_drt.optimizer.rebalancing.mincostflow.TransportProblem.Flow<org.matsim.contrib.my_drt.analysis.zonal.DrtZone, org.matsim.contrib.my_drt.analysis.zonal.DrtZone>> flows) {
		List<org.matsim.contrib.my_drt.optimizer.rebalancing.RebalancingStrategy.Relocation> relocations = new ArrayList<>();
		for (TransportProblem.Flow<org.matsim.contrib.my_drt.analysis.zonal.DrtZone, DrtZone> flow : flows) {
			List<DvrpVehicle> rebalancableVehicles = rebalancableVehiclesPerZone.get(flow.origin);

			Link targetLink = targetLinkSelector.selectTargetLink(flow.destination);

			for (int f = 0; f < flow.amount; f++) {
				// TODO use BestDispatchFinder (needs to be moved from taxi to dvrp) instead
				DvrpVehicle nearestVehicle = findNearestVehicle(rebalancableVehicles, targetLink);
				relocations.add(new RebalancingStrategy.Relocation(nearestVehicle, targetLink));
				rebalancableVehicles.remove(nearestVehicle);// TODO use map to have O(1) removal
			}
		}
		return relocations;
	}

	private DvrpVehicle findNearestVehicle(List<DvrpVehicle> rebalancableVehicles, Link destinationLink) {
		Coord toCoord = destinationLink.getFromNode().getCoord();
		return rebalancableVehicles.stream()
				.min(Comparator.comparing(v -> DistanceUtils.calculateSquaredDistance(
						Schedules.getLastLinkInSchedule(v).getToNode().getCoord(), toCoord)))
				.get();
	}
}
