package de.mpi.ds.drt_plan_modification;

import com.google.common.base.Verify;
import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Map;

import static de.mpi.ds.utils.GeneralUtils.doubleCloseToZero;

public class DrtPlanModifierConfigGroup extends ReflectiveConfigGroup {
    public final static String NAME = "drtPlanModifier";

    private final static String MODIFY_PLANS = "modifyPlans";
    //    private final static String DRT_MODE = "drtMode";
    private final static String ZETA_CUT = "zetaCut";
    private final static String MODE = "mode";
    private final static String PERIODICITY = "periodicity";

    private boolean modifyPlans = true;
    //    private String drtMode = "drt";
    private double zetaCut = 0;
    private String mode = "bimodal";
    private double periodicity = 0.;

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
        comments.put(MODE,
                "Transport mode (bimodal/unimodal/car)");
        comments.put(PERIODICITY, "SystemSize if system has periodic BC, otherwise set to a negative value");
        return comments;
    }

    @Override
    protected void checkConsistency(Config config) {
        super.checkConsistency(config);
        Verify.verify(mode.equals("bimodal") || mode.equals("unimodal") || mode.equals("car"),
                "Mode must be one of bimodal,unimodal,car");
        Verify.verify(!doubleCloseToZero(periodicity),
                "Periodicity should either be positive (periodic BC) or negative (no periodic BC");
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

    @StringGetter(PERIODICITY)
    public double getPeriodicity() {
        return periodicity;
    }

    @StringSetter(ZETA_CUT)
    public void setZetaCut(double gammaCut) {
        this.zetaCut = gammaCut;
    }

    @StringGetter(MODE)
    public String getMode() {
        return mode;
    }

    @StringSetter(MODE)
    public void setMode(String mode) {
        this.mode = mode;
    }

    @StringSetter(PERIODICITY)
    public void setPeriodicity(double periodicity) {
        this.periodicity = periodicity;
    }
}
