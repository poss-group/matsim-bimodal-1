package de.mpi.ds;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.run.gui.Gui;

import de.mpi.ds.utils.NetworkUtil;
import de.mpi.ds.utils.PopulationUtil;

import static de.mpi.ds.utils.UtilComponent.N_REQUESTS;

public class RunGUI {

    private static final Logger LOG = Logger.getLogger(RunGUI.class.getName());

    public static void main(String... args) {
        if (args.length == 2) {
            LOG.info("Running creation of sample configurations");
            NetworkUtil.createGridNetwork("./output/".concat(args[1]), true);
            PopulationUtil.createPopulation("./output/".concat(args[0]), "./output/".concat(args[1]), N_REQUESTS,
                    TransportMode.pt);
        } else if (args.length == 0) {
            LOG.info("Starting matsim...");
            Gui.show("MATSim Bimodal", MatsimMain.class);
        } else {
            LOG.error("Usage: <program> [population-file.xml network-file.xml]");
        }
    }
}