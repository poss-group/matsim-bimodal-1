package de.mpi.ds.custom_routing;

import org.matsim.contrib.dvrp.router.DistanceAsTravelDisutility;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

public class DistanceAsTravelDisutilityFactory implements TravelDisutilityFactory {
    @Override
    public TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
        return new DistanceAsTravelDisutility();
    }
}
