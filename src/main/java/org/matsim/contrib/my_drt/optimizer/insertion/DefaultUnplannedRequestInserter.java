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

import org.apache.log4j.Logger;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;
import org.matsim.contrib.my_drt.optimizer.VehicleData;
import org.matsim.contrib.my_drt.passenger.DrtRequest;
import org.matsim.contrib.my_drt.run.DrtConfigGroup;
import org.matsim.contrib.my_drt.scheduler.RequestInsertionScheduler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimTimer;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

/**
 * @author michalm
 */
public class DefaultUnplannedRequestInserter implements UnplannedRequestInserter {
	private static final Logger log = Logger.getLogger(DefaultUnplannedRequestInserter.class);
	public static final String NO_INSERTION_FOUND_CAUSE = "no_insertion_found";

	private final org.matsim.contrib.my_drt.run.DrtConfigGroup drtCfg;
	private final Fleet fleet;
	private final MobsimTimer mobsimTimer;
	private final EventsManager eventsManager;
	private final org.matsim.contrib.my_drt.scheduler.RequestInsertionScheduler insertionScheduler;
	private final org.matsim.contrib.my_drt.optimizer.VehicleData.EntryFactory vehicleDataEntryFactory;

	private final ForkJoinPool forkJoinPool;
	private final DrtInsertionSearch<PathData> insertionSearch;

	public DefaultUnplannedRequestInserter(DrtConfigGroup drtCfg, Fleet fleet, MobsimTimer mobsimTimer,
                                           EventsManager eventsManager, RequestInsertionScheduler insertionScheduler,
                                           org.matsim.contrib.my_drt.optimizer.VehicleData.EntryFactory vehicleDataEntryFactory, DrtInsertionSearch<PathData> insertionSearch,
                                           ForkJoinPool forkJoinPool) {
		this.drtCfg = drtCfg;
		this.fleet = fleet;
		this.mobsimTimer = mobsimTimer;
		this.eventsManager = eventsManager;
		this.insertionScheduler = insertionScheduler;
		this.vehicleDataEntryFactory = vehicleDataEntryFactory;
		this.forkJoinPool = forkJoinPool;
		this.insertionSearch = insertionSearch;
	}

	@Override
	public void scheduleUnplannedRequests(Collection<org.matsim.contrib.my_drt.passenger.DrtRequest> unplannedRequests) {
		if (unplannedRequests.isEmpty()) {
			return;
		}

		org.matsim.contrib.my_drt.optimizer.VehicleData vData = new VehicleData(mobsimTimer.getTimeOfDay(), fleet.getVehicles().values().stream(),
				vehicleDataEntryFactory, forkJoinPool);

		Iterator<org.matsim.contrib.my_drt.passenger.DrtRequest> reqIter = unplannedRequests.iterator();
		while (reqIter.hasNext()) {
			DrtRequest req = reqIter.next();
			Optional<org.matsim.contrib.my_drt.optimizer.insertion.InsertionWithDetourData<PathData>> best = insertionSearch.findBestInsertion(req,
					vData.getEntries());
			if (best.isEmpty()) {
				eventsManager.processEvent(
						new PassengerRequestRejectedEvent(mobsimTimer.getTimeOfDay(), drtCfg.getMode(), req.getId(),
								req.getPassengerId(), NO_INSERTION_FOUND_CAUSE));
				log.debug("No insertion found for drt request "
						+ req
						+ " from passenger id="
						+ req.getPassengerId()
						+ " fromLinkId="
						+ req.getFromLink().getId());
			} else {
				InsertionWithDetourData<PathData> insertion = best.get();
				insertionScheduler.scheduleRequest(req, insertion);
				vData.updateEntry(insertion.getVehicleEntry().vehicle);
				eventsManager.processEvent(
						new PassengerRequestScheduledEvent(mobsimTimer.getTimeOfDay(), drtCfg.getMode(), req.getId(),
								req.getPassengerId(), insertion.getVehicleEntry().vehicle.getId(),
								req.getPickupTask().getEndTime(), req.getDropoffTask().getBeginTime()));
			}
			reqIter.remove();
		}
	}
}
