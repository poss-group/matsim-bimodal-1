package de.mpi.ds;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import org.apache.log4j.Logger;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        //TODO make PT deterministic
        //TODO why hermes not walking (threads)

        String populationDir = "../populations/";
        Pattern pattern = Pattern.compile("population_(.*)_pt.xml");
        String[] populationFiles = getPopFiles(pattern, populationDir);

        for (String populationFile : populationFiles) {
            LOG.info(
                    "STARTING with population file: " + populationFile + "\n---------------------------\n" +
                            "---------------------------");
            Matcher matcher = pattern.matcher(populationFile);
            matcher.find();
            config.plans().setInputFile("../populations/" + populationFile);
            config.controler().setOutputDirectory("./output/" + matcher.group(1));

            // For dvrp/drt
            Controler controler = DrtControlerCreator.createControler(config, otfvis);

            // For only pt
//		Scenario scenario = ScenarioUtils.loadScenario(config);
//		Controler controler = new Controler(scenario);

            // Set up SBB Transit/Raptor
        controler.addOverridingModule(new SwissRailRaptorModule());

            //Custom Modules
//        controler.addOverridingModule(new GridPrePlanner());
//        controler.addOverridingQSimModule(new CustomTransitStopHandlerModule());
//        controler.addOverridingModule(new DrtPlanModifier((DrtPlanModifierConfigGroup) config.getModules().get
//        (DrtPlanModifierConfigGroup.NAME)));

            controler.run();
        }
    }

    private static String[] getPopFiles(Pattern pattern, String populationDir) {
        File dir = new File(populationDir);
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return pattern.matcher(name).matches();
            }
        };
        String[] populationFiles = dir.list(filter);
        assert populationFiles != null;

        return populationFiles;
    }
}