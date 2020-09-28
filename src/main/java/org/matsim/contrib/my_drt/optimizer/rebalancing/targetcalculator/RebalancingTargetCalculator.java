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

package org.matsim.contrib.my_drt.optimizer.rebalancing.targetcalculator;

import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.my_drt.analysis.zonal.DrtZone;

import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

/**
 * @author Michal Maciejewski (michalm)
 */
public interface RebalancingTargetCalculator {
	ToDoubleFunction<org.matsim.contrib.my_drt.analysis.zonal.DrtZone> calculate(double time, Map<DrtZone, List<DvrpVehicle>> rebalancableVehiclesPerZone);
}
