package de.mpi.ds.custom_routing;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.corelisteners.PlansReplanning;
import org.matsim.withinday.replanning.replanners.InitialReplanner;

public class CustomRoutingModule extends AbstractModule {

    private final static Logger LOG = Logger.getLogger(CustomRoutingModule.class.getName());

    @Override
    public void install() {
        LOG.info("Initiating");
        this.addTravelDisutilityFactoryBinding(TransportMode.car).toInstance(new DistanceAsTravelDisutilityFactory());

//        HandleRejectedDrtRequestedEvent handleRejectedDrtRequestedEvent = new HandleRejectedDrtRequestedEvent();
//        this.addMobsimListenerBinding().toInstance(handleRejectedDrtRequestedEvent);
//        this.addEventHandlerBinding().toInstance(handleRejectedDrtRequestedEvent);
//        this.bind(LeastCostPathCalculatorFactory.class).to(AStarEuclideanFactory.class);
        LOG.info("Finalizing");
    }
}
