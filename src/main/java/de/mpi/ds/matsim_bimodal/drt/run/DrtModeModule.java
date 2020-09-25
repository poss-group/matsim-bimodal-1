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

package de.mpi.ds.matsim_bimodal.drt.run;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Population;
import de.mpi.ds.matsim_bimodal.drt.analysis.zonal.DrtModeZonalSystemModule;
import de.mpi.ds.matsim_bimodal.drt.optimizer.rebalancing.NoRebalancingStrategy;
import de.mpi.ds.matsim_bimodal.drt.optimizer.rebalancing.RebalancingParams;
import de.mpi.ds.matsim_bimodal.drt.optimizer.rebalancing.RebalancingStrategy;
import de.mpi.ds.matsim_bimodal.drt.optimizer.rebalancing.mincostflow.DrtModeMinCostFlowRebalancingModule;
import de.mpi.ds.matsim_bimodal.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategyParams;
import de.mpi.ds.matsim_bimodal.drt.routing.*;
import de.mpi.ds.matsim_bimodal.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.FleetModule;
import org.matsim.contrib.dvrp.router.*;
import org.matsim.contrib.dvrp.router.DvrpRoutingModule.AccessEgressFacilityFinder;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.run.ModalProviders;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.FastAStarLandmarksFactory;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTrees;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import java.net.URL;
import java.util.List;

/**
 * @author michalm (Michal Maciejewski)
 */
public final class DrtModeModule extends AbstractDvrpModeModule {

	private final DrtConfigGroup drtCfg;
	private final static Logger LOG = Logger.getLogger(DrtModeModule.class.getName());

	public DrtModeModule(DrtConfigGroup drtCfg) {
		super(drtCfg.getMode());
		this.drtCfg = drtCfg;
	}

	@Override
	public void install() {
		DvrpModes.registerDvrpMode(binder(), getMode());
		install(new DvrpModeRoutingNetworkModule(getMode(), drtCfg.isUseModeFilteredSubnetwork()));
		bindModal(TravelDisutilityFactory.class).toInstance(TimeAsTravelDisutility::new);

		install(new FleetModule(getMode(), drtCfg.getVehiclesFileUrl(getConfig().getContext()),
				drtCfg.isChangeStartLinkToLastLinkInSchedule()));

		if (drtCfg.getRebalancingParams().isPresent()) {
			RebalancingParams rebalancingParams = drtCfg.getRebalancingParams().get();
			install(new DrtModeZonalSystemModule(drtCfg));

			if (rebalancingParams.getRebalancingStrategyParams() instanceof MinCostFlowRebalancingStrategyParams) {
				install(new DrtModeMinCostFlowRebalancingModule(drtCfg));
			} else {
				throw new RuntimeException(
						"Unsupported rebalancingStrategyParams: " + rebalancingParams.getRebalancingStrategyParams());
			}
			LOG.info("Rebalancing strategy installed");
		} else {
			bindModal(RebalancingStrategy.class).to(NoRebalancingStrategy.class).asEagerSingleton();
			LOG.info("No rebalancing strategy installed");
		}

		//this is a customised version of DvrpModeRoutingModule.install()
		addRoutingModuleBinding(getMode()).toProvider(new DvrpRoutingModuleProvider(getMode()));// not singleton
		modalMapBinder(DvrpRoutingModuleProvider.Stage.class, RoutingModule.class).addBinding(
				DvrpRoutingModuleProvider.Stage.MAIN)
				.toProvider(new DvrpModeRoutingModule.DefaultMainLegRouterProvider(getMode()));// not singleton
		bindModal(DefaultMainLegRouter.RouteCreator.class).toProvider(
				new DrtRouteCreatorProvider(drtCfg));// not singleton

		bindModal(DrtStopNetwork.class).toProvider(new DrtStopNetworkProvider(getConfig(), drtCfg)).asEagerSingleton();

		if (drtCfg.getOperationalScheme() == DrtConfigGroup.OperationalScheme.door2door) {
			bindModal(AccessEgressFacilityFinder.class).toProvider(
					modalProvider(getter -> new DecideOnLinkAccessEgressFacilityFinder(getter.getModal(Network.class))))
					.asEagerSingleton();
		} else {
			bindModal(AccessEgressFacilityFinder.class).toProvider(modalProvider(
					getter -> new ClosestAccessEgressFacilityFinder(drtCfg.getMaxWalkDistance(),
							getter.get(Network.class),
							QuadTrees.createQuadTree(getter.getModal(DrtStopNetwork.class).getDrtStops().values()))))
					.asEagerSingleton();
		}

		bindModal(DrtRouteUpdater.class).toProvider(new ModalProviders.AbstractProvider<>(getMode()) {
			@Inject
			@Named(DvrpTravelTimeModule.DVRP_ESTIMATED)
			private TravelTime travelTime;

			@Inject
			private Population population;

			@Inject
			private Config config;

			@Override
			public DefaultDrtRouteUpdater get() {
				Network network = getModalInstance(Network.class);
				return new DefaultDrtRouteUpdater(drtCfg, network, travelTime,
						getModalInstance(TravelDisutilityFactory.class), population, config);
			}
		}).asEagerSingleton();

		addControlerListenerBinding().to(modalKey(DrtRouteUpdater.class));
	}

	private static class DrtRouteCreatorProvider extends ModalProviders.AbstractProvider<DrtRouteCreator> {
		@Inject
		@Named(DvrpTravelTimeModule.DVRP_ESTIMATED)
		private TravelTime travelTime;

		private final LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;

		private final DrtConfigGroup drtCfg;

		private DrtRouteCreatorProvider(DrtConfigGroup drtCfg) {
			super(drtCfg.getMode());
			this.drtCfg = drtCfg;
			leastCostPathCalculatorFactory = new FastAStarLandmarksFactory(drtCfg.getNumberOfThreads());
		}

		@Override
		public DrtRouteCreator get() {
			return new DrtRouteCreator(drtCfg, getModalInstance(Network.class), leastCostPathCalculatorFactory,
					travelTime, getModalInstance(TravelDisutilityFactory.class));
		}
	}

	private static class DrtStopNetworkProvider extends ModalProviders.AbstractProvider<DrtStopNetwork> {

		private final DrtConfigGroup drtCfg;
		private final Config config;

		private DrtStopNetworkProvider(Config config, DrtConfigGroup drtCfg) {
			super(drtCfg.getMode());
			this.drtCfg = drtCfg;
			this.config = config;
		}

		@Override
		public DrtStopNetwork get() {
			switch (drtCfg.getOperationalScheme()) {
				case door2door:
					return ImmutableMap::of;
				case stopbased:
					return createDrtStopNetworkFromTransitSchedule(config, drtCfg);
				case serviceAreaBased:
					return createDrtStopNetworkFromServiceArea(config, drtCfg, getModalInstance(Network.class));
				default:
					throw new RuntimeException("Unsupported operational scheme: " + drtCfg.getOperationalScheme());
			}
		}
	}

	private static DrtStopNetwork createDrtStopNetworkFromServiceArea(Config config, DrtConfigGroup drtCfg,
			Network drtNetwork) {
		final List<PreparedGeometry> preparedGeometries = ShpGeometryUtils.loadPreparedGeometries(
				drtCfg.getDrtServiceAreaShapeFileURL(config.getContext()));
		ImmutableMap<Id<DrtStopFacility>, DrtStopFacility> drtStops = drtNetwork.getLinks()
				.values()
				.stream()
				.filter(link -> ShpGeometryUtils.isCoordInPreparedGeometries(link.getToNode().getCoord(),
						preparedGeometries))
				.map(DrtStopFacilityImpl::createFromLink)
				.collect(ImmutableMap.toImmutableMap(DrtStopFacility::getId, f -> f));
		return () -> drtStops;
	}

	private static DrtStopNetwork createDrtStopNetworkFromTransitSchedule(Config config, DrtConfigGroup drtCfg) {
		URL url = drtCfg.getTransitStopsFileUrl(config.getContext());
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new TransitScheduleReader(scenario).readURL(url);
		ImmutableMap<Id<DrtStopFacility>, DrtStopFacility> drtStops = scenario.getTransitSchedule()
				.getFacilities()
				.values()
				.stream()
				.map(DrtStopFacilityImpl::createFromIdentifiableFacility)
				.collect(ImmutableMap.toImmutableMap(DrtStopFacility::getId, f -> f));
		return () -> drtStops;
	}
}
