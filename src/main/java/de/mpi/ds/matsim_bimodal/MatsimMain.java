package de.mpi.ds.matsim_bimodal;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import de.mpi.ds.matsim_bimodal.drt_plan_modification.DrtPlanModifier;
import de.mpi.ds.matsim_bimodal.drt_plan_modification.DrtPlanModifierConfigGroup;
import org.apache.log4j.Logger;
//import org.matsim.contrib.drt.run.DrtControlerCreator;
//import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.my_drt.run.DrtControlerCreator;
import org.matsim.contrib.my_drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

public class MatsimMain {

    private static final Logger LOG = Logger.getLogger(MatsimMain.class.getName());

	public static void main(String[] args) {
	    LOG.info("Reading config");
        Config config = ConfigUtils.loadConfig(args[0], new MultiModeDrtConfigGroup(), new DvrpConfigGroup(), new OTFVisConfigGroup(), new DrtPlanModifierConfigGroup());
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        config.global().setNumberOfThreads(1);

        LOG.info("Starting matsim simulation...");
        run(config, false);
		LOG.info("Simulation finished");
	}



    public static void run(Config config, boolean otfvis) {
        // For dvrp/drt
        Controler controler = DrtControlerCreator.createControler(config, otfvis);

        // For only pt
//      Config config = ConfigUtils.loadConfig(args[0], new OTFVisConfigGroup());
//		Scenario scenario = ScenarioUtils.loadScenario(config);
//		Controler controler = new Controler(scenario);

        controler.addOverridingModule(new SwissRailRaptorModule());
        controler.addOverridingModule(new DrtPlanModifier((DrtPlanModifierConfigGroup) config.getModules().get(DrtPlanModifierConfigGroup.NAME)));

        controler.run();
    }
}