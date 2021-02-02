package de.mpi.ds.custom_routing;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.AStarEuclideanFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;

public class CustomRoutingModule extends AbstractModule {

    private final static Logger LOG = Logger.getLogger(CustomRoutingModule.class.getName());

    @Override
    public void install() {
        LOG.info("Initiating");
        this.addTravelDisutilityFactoryBinding(TransportMode.car).toInstance(new DistanceAsTravelDisutilityFactory());
//        this.bind(LeastCostPathCalculatorFactory.class).to(AStarEuclideanFactory.class);
        LOG.info("Finalizing");
    }
}
