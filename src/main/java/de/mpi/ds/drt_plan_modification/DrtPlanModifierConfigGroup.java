package de.mpi.ds.drt_plan_modification;

import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Map;

public class DrtPlanModifierConfigGroup extends ReflectiveConfigGroup {
    public final static String NAME = "drtPlanModifier";

    private final static String MODIFY_PLANS = "modifyPlans";
    //    private final static String DRT_MODE = "drtMode";
    private final static String ZETA_CUT = "zetaCut";
    private final static String PRIVATE_CAR_MODE = "privateCarMode";

    private boolean modifyPlans = true;
    //    private String drtMode = "drt";
    private double zetaCut = 0;
    private boolean privateCarMode = false;

    public DrtPlanModifierConfigGroup() {
        super(NAME);
    }

    public DrtPlanModifierConfigGroup(String name, boolean storeUnknownParametersAsStrings) {
        super(name, storeUnknownParametersAsStrings);
    }

    public static DrtPlanModifierConfigGroup get(Config config) {
        return (DrtPlanModifierConfigGroup) config.getModules().get(NAME);
    }

//    public static Map<String, String> getParamMap(Config config) {
//        return config.getModules().get(NAME).getParams();
//    }
//
//    public static void setParamter(Config config, String parameter, String value) {
//        getParamMap(config).remove(parameter);
//        getParamMap(config).put(parameter, value);
//    }

    @Override
    public Map<String, String> getComments() {
        Map<String, String> comments = super.getComments();
        comments.put(MODIFY_PLANS, "true if module should modify initial plans, false (default) if not");
//        comments.put(DRT_MODE, "Defaults to drt, for multimode scenarios it should be smth. else like acc_egr_drt");
        comments.put(ZETA_CUT,
                "Trips with distance longer than gammaCut*l where l is the public transport grid distance do get assigned as pt trips");
        comments.put(PRIVATE_CAR_MODE,
                "If true all plans gets modified so that legs will have travelmode \"car\". Defaults to false.");
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

    @StringGetter(ZETA_CUT)
    public double getZetaCut() {
        return zetaCut;
    }

    @StringSetter(ZETA_CUT)
    public void setZetaCut(double gammaCut) {
        this.zetaCut = gammaCut;
    }

    @StringGetter(PRIVATE_CAR_MODE)
    public boolean getPrivateCarMode() {
        return privateCarMode;
    }

    @StringSetter(PRIVATE_CAR_MODE)
    public void setPrivateCarMode(boolean privateCarMode) {
        this.privateCarMode = privateCarMode;
    }
}
