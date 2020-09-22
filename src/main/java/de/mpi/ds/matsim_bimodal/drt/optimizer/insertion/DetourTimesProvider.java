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

package de.mpi.ds.matsim_bimodal.drt.optimizer.insertion;

import org.matsim.api.core.v01.network.Link;
import de.mpi.ds.matsim_bimodal.drt.optimizer.insertion.DetourData;
import de.mpi.ds.matsim_bimodal.drt.optimizer.insertion.DetourTimeEstimator;
import de.mpi.ds.matsim_bimodal.drt.passenger.DrtRequest;

import java.util.function.Function;

/**
 * @author michalm
 */
public class DetourTimesProvider {
	private final DetourTimeEstimator detourTimeEstimator;

	public DetourTimesProvider(DetourTimeEstimator detourTimeEstimator) {
		this.detourTimeEstimator = detourTimeEstimator;
	}

	public DetourData<Double> getDetourData(DrtRequest drtRequest) {
		//TODO add departure/arrival times to improve estimation
		Function<Link, Double> timesToPickup = link -> detourTimeEstimator.estimateTime(link, drtRequest.getFromLink());
		Function<Link, Double> timesFromPickup = link -> detourTimeEstimator.estimateTime(drtRequest.getFromLink(),
				link);
		Function<Link, Double> timesToDropoff = link -> detourTimeEstimator.estimateTime(link, drtRequest.getToLink());
		Function<Link, Double> timesFromDropoff = link -> detourTimeEstimator.estimateTime(drtRequest.getToLink(),
				link);
		return new DetourData<>(timesToPickup, timesFromPickup, timesToDropoff, timesFromDropoff);
	}
}
