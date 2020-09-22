/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package de.mpi.ds.matsim_bimodal.drt.schedule;

import de.mpi.ds.matsim_bimodal.drt.schedule.DrtTaskType;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.schedule.DriveTask;

import static de.mpi.ds.matsim_bimodal.drt.schedule.DrtTaskBaseType.DRIVE;

/**
 * @author michalm
 */
public class DrtDriveTask extends DriveTask {
	public static final DrtTaskType TYPE = new DrtTaskType(DRIVE);

	public DrtDriveTask(VrpPathWithTravelData path, DrtTaskType taskType) {
		super(taskType, path);
	}
}
