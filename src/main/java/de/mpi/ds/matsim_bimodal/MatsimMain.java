package de.mpi.ds.matsim_bimodal;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import org.apache.log4j.Logger;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

public class MatsimMain {

    private static final Logger LOG = Logger.getLogger(MatsimMain.class.getName());

	public static void main(String[] args) {
        LOG.info("Starting matsim simulation...");
        Config config = ConfigUtils.loadConfig(args[0], new MultiModeDrtConfigGroup(), new DvrpConfigGroup(), new OTFVisConfigGroup());

        // For dvrp/drt
        Controler controler = DrtControlerCreator.createControler(config, false);

        // For only pt
//      Config config = ConfigUtils.loadConfig(args[0], new OTFVisConfigGroup());
//		Scenario scenario = ScenarioUtils.loadScenario(config);
//		Controler controler = new Controler(scenario);

		// This is the important line:
		controler.addOverridingModule(new SwissRailRaptorModule());

		controler.run();
		LOG.info("Simulation finished...");
	}
}