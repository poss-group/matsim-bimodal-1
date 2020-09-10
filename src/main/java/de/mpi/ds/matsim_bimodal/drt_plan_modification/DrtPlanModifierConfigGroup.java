package de.mpi.ds.matsim_bimodal.drt_plan_modification;

import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Map;

public class DrtPlanModifierConfigGroup extends ReflectiveConfigGroup {
    public final static String NAME = "drtPlanModifier";

    private final static String MODIFY_PLANS = "modifyPlans";

    private boolean modifyPlans = true;

    public DrtPlanModifierConfigGroup() {
        super(NAME);
    }

    public DrtPlanModifierConfigGroup(String name, boolean storeUnknownParametersAsStrings) {
        super(name, storeUnknownParametersAsStrings);
    }

    @Override
    public Map<String, String> getComments() {
        Map<String, String> comments =  super.getComments();
        comments.put(MODIFY_PLANS, "true if module should modify initial plans, false (default) if not");
        return comments;
    }

    @StringGetter(MODIFY_PLANS)
    public boolean isModifyPlans() {
        return modifyPlans;
    }

    @StringSetter(MODIFY_PLANS)
    public void setModifyPlans(boolean modifyPlans) {
        this.modifyPlans = modifyPlans;
    }
}
