package de.mpi.ds.matsim_bimodal.drt_plan_modification;

import org.apache.log4j.Logger;
import org.matsim.core.controler.AbstractModule;

/* This Module Modifies the plans in the sense that for people who do not start at a node with pt availability a breadth
first search is computed from that node on to look for the closest pt station
*/
public class DrtPlanModifier extends AbstractModule {
    private final static Logger LOG = Logger.getLogger(DrtPlanModifier.class.getName());
    final DrtPlanModifierConfigGroup configGroup;

    public DrtPlanModifier(DrtPlanModifierConfigGroup config) {
        this.configGroup = config;
    }

    @Override
    public void install() {
        LOG.info("Initiating");
        if (!this.getConfig().getModules().containsKey(DrtPlanModifierConfigGroup.NAME)) {
            this.getConfig().addModule(this.configGroup);
        }
        if (this.configGroup.isModifyPlans()) {
            this.addControlerListenerBinding().to(DrtPlanModifierStartupListener.class);
        }
        LOG.info("Finalizing");
    }
}
