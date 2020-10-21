package de.mpi.ds;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import de.mpi.ds.bimodal_assignment.BimodalAssignmentModule;
import org.apache.log4j.Logger;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collection;
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
        try {
            runMultiple(config, false, "drt");
        } catch (Exception e) {
            System.out.println(e);
        }

//        run(config, false);
        LOG.info("Simulation finished");
    }

    public static void run(Config config, boolean otfvis) {
        //TODO make PT deterministic
        //TODO why hermes not walking (threads)
        //TODO check transitschedule

        String vehiclesFile = getVehiclesFile(config);
        LOG.info(
                "STARTING with\npopulation file: " + config.plans().getInputFile() +
                        "and\nvehicles file: " + vehiclesFile + "\n---------------------------");


        // For dvrp/drt
        Controler controler = DrtControlerCreator.createControler(config, otfvis);

        // For only pt
//		Scenario scenario = ScenarioUtils.loadScenario(config);
//		Controler controler = new Controler(scenario);

        // Set up SBB Transit/Raptor
//        controler.addOverridingModule(new SwissRailRaptorModule());

        //Custom Modules
//        controler.addOverridingModule(new BimodalAssignmentModule());
//        controler.addOverridingModule(new GridPrePlanner());
//        controler.addOverridingQSimModule(new CustomTransitStopHandlerModule());
//        controler.addOverridingModule(new DrtPlanModifier((DrtPlanModifierConfigGroup) config.getModules().get
//        (DrtPlanModifierConfigGroup.NAME)));

        controler.run();
    }

    private static void runMultiple(Config config, boolean otfvis, String mode) throws Exception {
        if (!mode.equals("pt") && !mode.equals("drt")) {
            throw new Exception("Mode has to be drt or pt");
        }
        String populationDir = "../populations_24h/";
        String vehiclesDir = "../drtvehicles_1_percent_reqs/";
        Pattern patternPop = Pattern.compile("population_(.*)_" + mode + "\\.xml");
        Pattern patternDrt = Pattern.compile("drtvehicles_(.*).xml");
        String[] populationFiles = getFiles(patternPop, populationDir);
        String[] drtVehicleFiles = getFiles(patternDrt, vehiclesDir);

        for (int i = 0; i < populationFiles.length; i++) {
            String populationFile = populationFiles[i];
            String drtVehicleFile = drtVehicleFiles[i];
                Matcher matcherPop = patternPop.matcher(populationFile);
                Matcher matcherDrt = patternPop.matcher(populationFile);
                matcherPop.find();
                matcherDrt.find();

                config.plans().setInputFile(populationDir + populationFile);
                Collection<DrtConfigGroup> modalElements = MultiModeDrtConfigGroup.get(config).getModalElements();
                assert modalElements.size() == 1 : "Only one drt modal element expected in config file";
                modalElements.stream().findFirst().get().setVehiclesFile(vehiclesDir + drtVehicleFile);

                assert matcherDrt.group(1).equals(matcherPop.group(1)) : "Running with files for different scenarios";
                config.controler().setOutputDirectory("./output/" + matcherPop.group(1));
//                System.out.println(populationFile);
//                System.out.println(drtVehicleFile);

                run(config, otfvis);
        }
    }


    private static String getVehiclesFile(Config config) {
        Collection<DrtConfigGroup> modalElements = MultiModeDrtConfigGroup.get(config).getModalElements();
        assert modalElements.size() == 1 : "Only one drt modal element expected in config file";
        return modalElements.stream().findFirst().get().getVehiclesFile();
    }

    private static String[] getFiles(Pattern pattern, String Dir) {
        File dir = new File(Dir);
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return pattern.matcher(name).matches();
            }
        };
        String[] files = dir.list(filter);
        assert files != null;
        Arrays.sort(files);

        return files;
    }
}