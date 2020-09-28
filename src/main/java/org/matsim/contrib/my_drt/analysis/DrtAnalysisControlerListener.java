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

package org.matsim.contrib.my_drt.analysis;

import org.apache.commons.lang3.tuple.Pair;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.my_drt.analysis.DrtRequestAnalyzer.PerformedRequestEventSequence;
import org.matsim.contrib.my_drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.my_drt.run.DrtConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * @author jbischoff
 */
public class DrtAnalysisControlerListener implements IterationEndsListener {

	private final org.matsim.contrib.my_drt.analysis.DrtPassengerAndVehicleStats drtPassengerStats;
	private final MatsimServices matsimServices;
	private final Network network;
	private final org.matsim.contrib.my_drt.analysis.DrtRequestAnalyzer drtRequestAnalyzer;
	private final org.matsim.contrib.my_drt.run.DrtConfigGroup drtCfg;
	private final QSimConfigGroup qSimCfg;
	private boolean headerWritten = false;
	private boolean vheaderWritten = false;
	private final String runId;
	private final DecimalFormat format = new DecimalFormat();
	private final int maxcap;

	public DrtAnalysisControlerListener(Config config, DrtConfigGroup drtCfg, FleetSpecification fleet,
                                        DrtPassengerAndVehicleStats drtPassengerStats, MatsimServices matsimServices, Network network,
                                        DrtRequestAnalyzer drtRequestAnalyzer) {
		this.drtPassengerStats = drtPassengerStats;
		this.matsimServices = matsimServices;
		this.network = network;
		this.drtRequestAnalyzer = drtRequestAnalyzer;
		this.drtCfg = drtCfg;
		this.qSimCfg = config.qsim();
		runId = config.controler().getRunId();
		maxcap = org.matsim.contrib.my_drt.analysis.DrtTripsAnalyser.findMaxVehicleCapacity(fleet);

		format.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
		format.setMinimumIntegerDigits(1);
		format.setMaximumFractionDigits(2);
		format.setGroupingUsed(false);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		boolean createGraphs = event.getServices().getConfig().controler().isCreateGraphs();

		writeAndPlotWaitTimeEstimateComparison(drtRequestAnalyzer.getPerformedRequestSequences().values(),
				filename(event, "waitTimeComparison", ".png"), filename(event, "waitTimeComparison", ".csv"),
				createGraphs);
		List<DrtTrip> trips = drtPassengerStats.getDrtTrips();

		org.matsim.contrib.my_drt.analysis.DrtTripsAnalyser.collection2Text(drtRequestAnalyzer.getRejectedRequestSequences().values(),
				filename(event, "drt_rejections", ".csv"), "time;personId;fromLinkId;toLinkId;fromX;fromY;toX;toY",
				seq -> {
					DrtRequestSubmittedEvent submission = seq.getSubmitted();
					Coord fromCoord = network.getLinks().get(submission.getFromLinkId()).getCoord();
					Coord toCoord = network.getLinks().get(submission.getToLinkId()).getCoord();
					return submission.getTime()
							+ ";"
							+ submission.getPersonId()
							+ ";"
							+ submission.getFromLinkId()
							+ ";"
							+ submission.getToLinkId()
							+ ";"
							+ fromCoord.getX()
							+ ";"
							+ fromCoord.getY()
							+ ";"
							+ toCoord.getX()
							+ ";"
							+ toCoord.getY();
				});

		double rejectionRate = (double)drtRequestAnalyzer.getRejectedRequestSequences().size()
				/ drtRequestAnalyzer.getRequestSubmissions().size();
		String tripsSummarize = org.matsim.contrib.my_drt.analysis.DrtTripsAnalyser.summarizeTrips(trips, ";");
		double directDistanceMean = org.matsim.contrib.my_drt.analysis.DrtTripsAnalyser.getDirectDistanceMean(trips);
		writeIterationPassengerStats(
				tripsSummarize + ";" + drtRequestAnalyzer.getRejectedRequestSequences().size() + ";" + format.format(
						rejectionRate), event.getIteration());
		double l_d = org.matsim.contrib.my_drt.analysis.DrtTripsAnalyser.getTotalDistance(drtPassengerStats.getVehicleStates()) / (trips.size()
				* directDistanceMean);
		String vehStats = org.matsim.contrib.my_drt.analysis.DrtTripsAnalyser.summarizeVehicles(drtPassengerStats.getVehicleStates(), ";")
				+ ";"
				+ format.format(l_d);
		String occStats = org.matsim.contrib.my_drt.analysis.DrtTripsAnalyser.summarizeDetailedOccupancyStats(drtPassengerStats.getVehicleStates(), ";",
				maxcap);
		writeIterationVehicleStats(vehStats, occStats, event.getIteration());
		if (drtCfg.isPlotDetailedCustomerStats()) {
			org.matsim.contrib.my_drt.analysis.DrtTripsAnalyser.collection2Text(trips, filename(event, "drt_trips", ".csv"), DrtTrip.HEADER,
					DrtTrip::toString);
		}
		org.matsim.contrib.my_drt.analysis.DrtTripsAnalyser.writeVehicleDistances(drtPassengerStats.getVehicleStates(),
				filename(event, "vehicleDistanceStats", ".csv"));
		org.matsim.contrib.my_drt.analysis.DrtTripsAnalyser.analyseDetours(network, trips, drtCfg, filename(event, "drt_detours"), createGraphs);
		org.matsim.contrib.my_drt.analysis.DrtTripsAnalyser.analyseWaitTimes(filename(event, "waitStats"), trips, 1800, createGraphs);

		double endTime = qSimCfg.getEndTime()
				.orElseGet(() -> trips.isEmpty() ?
						qSimCfg.getStartTime().orElse(0) :
						trips.get(trips.size() - 1).getDepartureTime());

		DrtTripsAnalyser.analyzeBoardingsAndDeboardings(trips, ";", qSimCfg.getStartTime().orElse(0), endTime, 3600,
				filename(event, "drt_boardings", ".csv"), filename(event, "drt_alightments", ".csv"), network);
	}

	private String filename(IterationEndsEvent event, String prefix) {
		return filename(event, prefix, "");
	}

	private String filename(IterationEndsEvent event, String prefix, String extension) {
		return matsimServices.getControlerIO()
				.getIterationFilename(event.getIteration(), prefix + "_" + drtCfg.getMode() + extension);
	}

	/**
	 * @param summarizeTrips
	 * @param it             iteration
	 */
	private void writeIterationPassengerStats(String summarizeTrips, int it) {
		try (var bw = getAppendingBufferedWriter("drt_customer_stats", ".csv")) {
			if (!headerWritten) {
				headerWritten = true;
				bw.write(
						"runId;iteration;rides;wait_average;wait_max;wait_p95;wait_p75;wait_median;inVehicleTravelTime_mean;distance_m_mean;directDistance_m_mean;totalTravelTime_mean;rejections;rejectionRate");
				bw.newLine();
			}
			bw.write(runId + ";" + it + ";" + summarizeTrips);
			bw.newLine();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @param summarizeVehicles
	 * @param it                iteration
	 */
	private void writeIterationVehicleStats(String summarizeVehicles, String vehOcc, int it) {
		try (var bw = getAppendingBufferedWriter("drt_vehicle_stats", ".csv")) {
			if (!vheaderWritten) {
				bw.write(
						"runId;iteration;vehicles;totalDistance;totalEmptyDistance;emptyRatio;totalRevenueDistance;averageDrivenDistance;averageEmptyDistance;averageRevenueDistance;d_r/d_t;l_det");
				bw.newLine();
			}
			bw.write(runId + ";" + it + ";" + summarizeVehicles);
			bw.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try (var bw = getAppendingBufferedWriter("drt_detailed_distanceStats", ".csv")) {
			if (!vheaderWritten) {
				vheaderWritten = true;
				bw.write("runId;iteration");
				for (int i = 0; i <= maxcap; i++) {
					bw.write(";" + i + " pax distance_m");
				}
				bw.newLine();
			}
			bw.write(runId + ";" + it + vehOcc);
			bw.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeAndPlotWaitTimeEstimateComparison(
			Collection<PerformedRequestEventSequence> performedRequestEventSequences, String plotFileName,
			String textFileName, boolean createChart) {
		try (var bw = IOUtils.getBufferedWriter(textFileName)) {
			XYSeries times = new XYSeries("waittimes", true, true);

			bw.append("RequestId;actualWaitTime;estimatedWaitTime;deviate");
			for (PerformedRequestEventSequence seq : performedRequestEventSequences) {
				bw.newLine();
				double actualWaitTime = seq.getPickedUp().getTime() - seq.getSubmitted().getTime();
				double estimatedWaitTime = seq.getScheduled().getPickupTime() - seq.getSubmitted().getTime();

				bw.append(seq.getSubmitted().getRequestId() + "").append(";").append(actualWaitTime + "")
						.append(";")
						.append(estimatedWaitTime + "")
						.append(";")
						.append((actualWaitTime - estimatedWaitTime) + "");
				times.add(actualWaitTime, estimatedWaitTime);
			}

			if (createChart) {
				final JFreeChart chart2 = DensityScatterPlots.createPlot("Wait times", "Actual wait time [s]",
						"Initially planned wait time [s]", times, Pair.of(0., drtCfg.getMaxWaitTime()));
				//			xAxis.setLowerBound(0);
				//			yAxis.setLowerBound(0);
				ChartUtils.writeChartAsPNG(new FileOutputStream(plotFileName), chart2, 1500, 1500);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private BufferedWriter getAppendingBufferedWriter(String prefix, String extension) {
		return IOUtils.getAppendingBufferedWriter(
				matsimServices.getControlerIO().getOutputFilename(prefix + "_" + drtCfg.getMode() + extension));
	}
}
