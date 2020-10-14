package de.mpi.ds;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import de.mpi.ds.custom_transit_stop_handler.CustomTransitStopHandlerModule;
import de.mpi.ds.grid_pre_planner.GridPrePlanner;
import org.apache.log4j.Logger;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.pt.counts.PtCountsModule;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

public class MatsimMain {

    private static final Logger LOG = Logger.getLogger(MatsimMain.class.getName());

    public static void main(String[] args) {
        LOG.info("Reading config");
        Config config = ConfigUtils
                .loadConfig(args[0], new MultiModeDrtConfigGroup(), new DvrpConfigGroup(), new OTFVisConfigGroup());
//      Config config = ConfigUtils.loadConfig(args[0], new OTFVisConfigGroup());
//        config.global().setNumberOfThreads(1);

        LOG.info("Starting matsim simulation...");
        run(config, false);
        LOG.info("Simulation finished");
    }


    public static void run(Config config, boolean otfvis) {
        //TODO store occupancy of vehicles
        //TODO make PT deterministic
        //TODO why hermes not walking (threads)

//        PtCountsModule
        // For dvrp/drt
        Controler controler = DrtControlerCreator.createControler(config, otfvis);

        // For only pt
//		Scenario scenario = ScenarioUtils.loadScenario(config);
//		Controler controler = new Controler(scenario);

        // Set up SBB Transit/Raptor
//        controler.addOverridingModule(new SBBTransitModule());
        controler.addOverridingModule(new SwissRailRaptorModule());
//        controler.configureQSimComponents(components -> {
//            SBBTransitEngineQSimModule.configure(components);
//        });

        controler.addOverridingModule(new GridPrePlanner());
//        controler.addOverridingQSimModule(new CustomTransitStopHandlerModule());

//        controler.addOverridingModule(new DrtPlanModifier((DrtPlanModifierConfigGroup) config.getModules().get(DrtPlanModifierConfigGroup.NAME)));

        controler.run();
    }
}