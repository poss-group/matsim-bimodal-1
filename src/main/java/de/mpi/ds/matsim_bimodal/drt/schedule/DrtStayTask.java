/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.network.Link;
import de.mpi.ds.matsim_bimodal.drt.schedule.DrtTaskType;
import org.matsim.contrib.dvrp.schedule.StayTask;

import static de.mpi.ds.matsim_bimodal.drt.schedule.DrtTaskBaseType.STAY;

/**
 * @author michalm
 */
public class DrtStayTask extends StayTask {
	public static final DrtTaskType TYPE = new DrtTaskType(STAY);

	public DrtStayTask(double beginTime, double endTime, Link link) {
		super(TYPE, beginTime, endTime, link);
	}
}
