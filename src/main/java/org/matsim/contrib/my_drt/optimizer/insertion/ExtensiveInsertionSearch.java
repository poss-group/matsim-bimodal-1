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

import org.matsim.contrib.my_drt.optimizer.insertion.InsertionGenerator.Insertion;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;
import org.matsim.contrib.my_drt.optimizer.VehicleData;
import org.matsim.contrib.my_drt.passenger.DrtRequest;
import org.matsim.contrib.my_drt.run.DrtConfigGroup;
import org.matsim.core.mobsim.framework.MobsimTimer;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

/**
 * @author michalm
 */
public class ExtensiveInsertionSearch implements DrtInsertionSearch<PathData> {
	private final org.matsim.contrib.my_drt.optimizer.insertion.ExtensiveInsertionSearchParams insertionParams;

	// step 1: initial filtering out feasible insertions
	private final InsertionCostCalculator<Double> admissibleCostCalculator;
	private final org.matsim.contrib.my_drt.optimizer.insertion.DetourTimesProvider admissibleDetourTimesProvider;

	// step 2: finding best insertion
	private final ForkJoinPool forkJoinPool;
	private final org.matsim.contrib.my_drt.optimizer.insertion.DetourPathCalculator detourPathCalculator;
	private final org.matsim.contrib.my_drt.optimizer.insertion.BestInsertionFinder<PathData> bestInsertionFinder;

	public ExtensiveInsertionSearch(DetourPathCalculator detourPathCalculator, DrtConfigGroup drtCfg, MobsimTimer timer,
                                    ForkJoinPool forkJoinPool, InsertionCostCalculator.PenaltyCalculator penaltyCalculator) {
		this.detourPathCalculator = detourPathCalculator;
		this.forkJoinPool = forkJoinPool;

		insertionParams = (ExtensiveInsertionSearchParams)drtCfg.getDrtInsertionSearchParams();
		admissibleCostCalculator = new InsertionCostCalculator<>(drtCfg, timer, penaltyCalculator, Double::doubleValue);

		// TODO use more sophisticated DetourTimeEstimator
		double admissibleBeelineSpeed = insertionParams.getAdmissibleBeelineSpeedFactor()
				* drtCfg.getEstimatedDrtSpeed() / drtCfg.getEstimatedBeelineDistanceFactor();

		admissibleDetourTimesProvider = new DetourTimesProvider(
				DetourTimeEstimator.createBeelineTimeEstimator(admissibleBeelineSpeed));

		bestInsertionFinder = new BestInsertionFinder<>(
				new InsertionCostCalculator<>(drtCfg, timer, penaltyCalculator, PathData::getTravelTime));
	}

	@Override
	public Optional<org.matsim.contrib.my_drt.optimizer.insertion.InsertionWithDetourData<PathData>> findBestInsertion(DrtRequest drtRequest,
                                                                                                                       Collection<VehicleData.Entry> vEntries) {
		org.matsim.contrib.my_drt.optimizer.insertion.InsertionGenerator insertionGenerator = new InsertionGenerator();
		org.matsim.contrib.my_drt.optimizer.insertion.DetourData<Double> admissibleTimeData = admissibleDetourTimesProvider.getDetourData(drtRequest);
		org.matsim.contrib.my_drt.optimizer.insertion.KNearestInsertionsAtEndFilter kNearestInsertionsAtEndFilter = new org.matsim.contrib.my_drt.optimizer.insertion.KNearestInsertionsAtEndFilter(
				insertionParams.getNearestInsertionsAtEndLimit(), insertionParams.getAdmissibleBeelineSpeedFactor());

		// Parallel outer stream over vehicle entries. The inner stream (flatmap) is sequential.
		List<Insertion> filteredInsertions = forkJoinPool.submit(() -> vEntries.parallelStream()
				//generate feasible insertions (wrt occupancy limits)
				.flatMap(e -> insertionGenerator.generateInsertions(drtRequest, e).stream())
				//map insertions to insertions with admissible detour times (i.e. admissible beeline speed factor)
				.map(admissibleTimeData::createInsertionWithDetourData)
				//optimistic pre-filtering wrt admissible cost function
				.filter(insertion -> admissibleCostCalculator.calculate(drtRequest, insertion)
						< InsertionCostCalculator.INFEASIBLE_SOLUTION_COST)
				//skip insertions at schedule ends (a subset of most promising "insertionsAtEnd" will be added later)
				.filter(kNearestInsertionsAtEndFilter::filter)
				//forget (admissible) detour times
				.map(InsertionWithDetourData::getInsertion).collect(Collectors.toList())).join();
		filteredInsertions.addAll(kNearestInsertionsAtEndFilter.getNearestInsertionsAtEnd());

		DetourData<PathData> pathData = detourPathCalculator.calculatePaths(drtRequest, filteredInsertions);
		//TODO could use a parallel stream within forkJoinPool, however the idea is to have as few filteredInsertions
		// as possible, and then using a parallel stream does not make sense.
		return bestInsertionFinder.findBestInsertion(drtRequest,
				filteredInsertions.stream().map(pathData::createInsertionWithDetourData));
	}
}
