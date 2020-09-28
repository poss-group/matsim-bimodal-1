/*
 * *********************************************************************** *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.my_drt.optimizer.rebalancing.mincostflow;

import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.my_drt.optimizer.rebalancing.demandestimator.PreviousIterationDRTDemandEstimator;
import org.matsim.contrib.my_drt.optimizer.rebalancing.demandestimator.ZonalDemandEstimator;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.my_drt.analysis.zonal.DrtZonalSystem;
import org.matsim.contrib.my_drt.analysis.zonal.DrtZoneTargetLinkSelector;
import org.matsim.contrib.my_drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.my_drt.optimizer.rebalancing.RebalancingStrategy;
import org.matsim.contrib.my_drt.optimizer.rebalancing.targetcalculator.DemandEstimatorAsTargetCalculator;
import org.matsim.contrib.my_drt.run.DrtConfigGroup;

/**
 * @author michalm
 */
public class DrtModeMinCostFlowRebalancingModule extends AbstractDvrpModeModule {
	private final org.matsim.contrib.my_drt.run.DrtConfigGroup drtCfg;

	public DrtModeMinCostFlowRebalancingModule(DrtConfigGroup drtCfg) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
	}

	@Override
	public void install() {
		RebalancingParams params = drtCfg.getRebalancingParams().orElseThrow();
		MinCostFlowRebalancingStrategyParams strategyParams = (MinCostFlowRebalancingStrategyParams)params.getRebalancingStrategyParams();

		installQSimModule(new AbstractDvrpModeQSimModule(getMode()) {
			@Override
			protected void configureQSim() {
				bindModal(RebalancingStrategy.class).toProvider(modalProvider(
						getter -> new MinCostFlowRebalancingStrategy(getter.getModal(org.matsim.contrib.my_drt.optimizer.rebalancing.targetcalculator.RebalancingTargetCalculator.class),
								getter.getModal(org.matsim.contrib.my_drt.analysis.zonal.DrtZonalSystem.class), getter.getModal(Fleet.class),
								getter.getModal(ZonalRelocationCalculator.class), params))).asEagerSingleton();

				switch (strategyParams.getRebalancingTargetCalculatorType()) {
					case EstimatedDemand:
						bindModal(org.matsim.contrib.my_drt.optimizer.rebalancing.targetcalculator.RebalancingTargetCalculator.class).toProvider(modalProvider(
								getter -> new DemandEstimatorAsTargetCalculator(
										getter.getModal(ZonalDemandEstimator.class)))).asEagerSingleton();
						break;

					case EqualRebalancableVehicleDistribution:
						bindModal(org.matsim.contrib.my_drt.optimizer.rebalancing.targetcalculator.RebalancingTargetCalculator.class).toProvider(modalProvider(
								getter -> new org.matsim.contrib.my_drt.optimizer.rebalancing.targetcalculator.EqualRebalancableVehicleDistributionTargetCalculator(
										getter.getModal(ZonalDemandEstimator.class),
										getter.getModal(org.matsim.contrib.my_drt.analysis.zonal.DrtZonalSystem.class)))).asEagerSingleton();
						break;

					case EqualVehicleDensity:
						bindModal(org.matsim.contrib.my_drt.optimizer.rebalancing.targetcalculator.RebalancingTargetCalculator.class).toProvider(modalProvider(
								getter -> new org.matsim.contrib.my_drt.optimizer.rebalancing.targetcalculator.EqualVehicleDensityTargetCalculator(getter.getModal(org.matsim.contrib.my_drt.analysis.zonal.DrtZonalSystem.class),
										getter.getModal(FleetSpecification.class)))).asEagerSingleton();
						break;

					case EqualVehiclesToPopulationRatio:
						bindModal(org.matsim.contrib.my_drt.optimizer.rebalancing.targetcalculator.RebalancingTargetCalculator.class).toProvider(modalProvider(
								getter -> new org.matsim.contrib.my_drt.optimizer.rebalancing.targetcalculator.EqualVehiclesToPopulationRatioTargetCalculator(
										getter.getModal(org.matsim.contrib.my_drt.analysis.zonal.DrtZonalSystem.class), getter.get(Population.class),
										getter.getModal(FleetSpecification.class)))).asEagerSingleton();
						break;

					default:
						throw new IllegalArgumentException("Unsupported rebalancingTargetCalculatorType="
								+ strategyParams.getZonalDemandEstimatorType());
				}

				bindModal(ZonalRelocationCalculator.class).toProvider(modalProvider(
						getter -> new AggregatedMinCostRelocationCalculator(
								getter.getModal(DrtZoneTargetLinkSelector.class)))).asEagerSingleton();
			}
		});

		switch (strategyParams.getZonalDemandEstimatorType()) {
			case PreviousIterationDemand:
				bindModal(PreviousIterationDRTDemandEstimator.class).toProvider(modalProvider(
						getter -> new PreviousIterationDRTDemandEstimator(getter.getModal(DrtZonalSystem.class),
								drtCfg))).asEagerSingleton();
				bindModal(ZonalDemandEstimator.class).to(modalKey(PreviousIterationDRTDemandEstimator.class));
				addEventHandlerBinding().to(modalKey(PreviousIterationDRTDemandEstimator.class));
				break;

			case None:
				break;

			default:
				throw new IllegalArgumentException(
						"Unsupported zonalDemandEstimatorType=" + strategyParams.getZonalDemandEstimatorType());
		}
	}
}
