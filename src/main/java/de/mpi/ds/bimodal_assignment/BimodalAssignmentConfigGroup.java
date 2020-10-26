package de.mpi.ds.bimodal_assignment;

import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.Map;

public class BimodalAssignmentConfigGroup extends ReflectiveConfigGroup {
    public final static String NAME = "bimodalAssignmentModule";

    private final static String GAMMA_CUT = "gammaCut";

    private double gammaCut = 0;

    public BimodalAssignmentConfigGroup() {
        super(NAME);
    }

    public BimodalAssignmentConfigGroup(String name, boolean storeUnknownParametersAsStrings) {
        super(name, storeUnknownParametersAsStrings);
    }

    @Override
    public Map<String, String> getComments() {
        Map<String, String> comments = super.getComments();
        comments.put(GAMMA_CUT,
                "Trips with distance longer than gammaCut*l where l is the public transport grid distance do get assigned as pt trips");
        return comments;
    }

    @StringGetter(GAMMA_CUT)
    public double getGammaCut() {
        return gammaCut;
    }

    @StringSetter(GAMMA_CUT)
    public void setGammaCut(double gammaCut) {
        this.gammaCut = gammaCut;
    }
}
