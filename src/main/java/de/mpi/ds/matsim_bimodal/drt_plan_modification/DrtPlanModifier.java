package de.mpi.ds.matsim_bimodal.drt_plan_modification;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.AbstractModule;

public class DrtPlanModifier extends AbstractModule {
    private final static Logger LOG = Logger.getLogger(DrtPlanModifier.class.getName());
    final DrtPlanModifierConfigGroup configGroup;

    public DrtPlanModifier(DrtPlanModifierConfigGroup config) {
        this.configGroup = config;
    }
//    @Inject
//    Scenario sc;

    @Override
    public void install() {
        LOG.info("Initiating");
//        LOG.warn("sc: " + sc);
        if (!this.getConfig().getModules().containsKey(DrtPlanModifierConfigGroup.NAME)) {
            this.getConfig().addModule(this.configGroup);
        }
        if (this.configGroup.isModifyPlans()) {
            this.addControlerListenerBinding().to(DrtPlanModifierStartupListener.class);
        }
        LOG.info("Finalizing");
    }
}
