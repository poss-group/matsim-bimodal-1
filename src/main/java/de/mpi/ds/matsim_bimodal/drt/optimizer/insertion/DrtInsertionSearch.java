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

import de.mpi.ds.matsim_bimodal.drt.optimizer.VehicleData;
import de.mpi.ds.matsim_bimodal.drt.optimizer.insertion.InsertionWithDetourData;
import de.mpi.ds.matsim_bimodal.drt.passenger.DrtRequest;

import java.util.Collection;
import java.util.Optional;

/**
 * @author michalm
 */
public interface DrtInsertionSearch<D> {
	Optional<InsertionWithDetourData<D>> findBestInsertion(DrtRequest drtRequest, Collection<VehicleData.Entry> vData);
}
