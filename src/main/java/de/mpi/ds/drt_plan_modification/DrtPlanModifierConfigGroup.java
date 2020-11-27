package de.mpi.ds.drt_plan_modification;

import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Map;

public class DrtPlanModifierConfigGroup extends ReflectiveConfigGroup {
    public final static String NAME = "drtPlanModifier";

    private final static String MODIFY_PLANS = "modifyPlans";
    private final static String DRT_MODE = "drtMode";

    private boolean modifyPlans = true;
    private String drtMode = "drt";

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
        comments.put(DRT_MODE, "Defaults to drt, for multimode scenarios it should be smth. else like acc_egr_drt");
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

    @StringGetter(DRT_MODE)
    public String getDrtMode() {
        return drtMode;
    }

    @StringSetter(DRT_MODE)
    public void setDrtMode(String drtMode) {
        this.drtMode = drtMode;
    }
}
