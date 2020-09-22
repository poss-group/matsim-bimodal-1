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

import de.mpi.ds.matsim_bimodal.drt.optimizer.insertion.DetourData;
import de.mpi.ds.matsim_bimodal.drt.optimizer.insertion.InsertionGenerator.Insertion;
import de.mpi.ds.matsim_bimodal.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;

import java.util.List;

/**
 * @author michalm
 */
public interface DetourPathCalculator {
	DetourData<PathData> calculatePaths(DrtRequest drtRequest, List<Insertion> filteredInsertions);
}
