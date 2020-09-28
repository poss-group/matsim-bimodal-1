/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.my_drt.optimizer.insertion;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.my_drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.my_drt.passenger.DrtRequest;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author michalm
 */
public class BestInsertionFinder<D> {
	private static class InsertionWithCost<D> {
		private final org.matsim.contrib.my_drt.optimizer.insertion.InsertionWithDetourData<D> insertionWithDetourData;
		private final double cost;

		private InsertionWithCost(org.matsim.contrib.my_drt.optimizer.insertion.InsertionWithDetourData<D> insertionWithDetourData, double cost) {
			this.insertionWithDetourData = insertionWithDetourData;
			this.cost = cost;
		}
	}

	private static final Comparator<Insertion> INSERTION_COMPARATOR = Comparator.<Insertion, Id<DvrpVehicle>>comparing(
			insertion -> insertion.vehicleEntry.vehicle.getId()).thenComparingInt(insertion -> insertion.pickup.index)
			.thenComparingInt(insertion -> insertion.dropoff.index);

	private final Comparator<InsertionWithCost<D>> comparator = Comparator.<InsertionWithCost<D>>comparingDouble(
			insertionWithCost -> insertionWithCost.cost).thenComparing(
			insertion -> insertion.insertionWithDetourData.getInsertion(), INSERTION_COMPARATOR);

	private final InsertionCostCalculator<D> costCalculator;

	BestInsertionFinder(InsertionCostCalculator<D> costCalculator) {
		this.costCalculator = costCalculator;
	}

	public Optional<org.matsim.contrib.my_drt.optimizer.insertion.InsertionWithDetourData<D>> findBestInsertion(DrtRequest drtRequest,
                                                                                                                Stream<InsertionWithDetourData<D>> insertions) {
		return insertions.map(
				insertion -> new InsertionWithCost<>(insertion, costCalculator.calculate(drtRequest, insertion)))
				.filter(iWithCost -> iWithCost.cost < InsertionCostCalculator.INFEASIBLE_SOLUTION_COST)
				.min(comparator)
				.map(insertionWithCost -> insertionWithCost.insertionWithDetourData);
	}
}
