package de.mpi.ds;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import de.mpi.ds.custom_transit_stop_handler.CustomTransitStopHandlerModule;
import de.mpi.ds.drt_plan_modification.DrtPlanModifier;
import de.mpi.ds.drt_plan_modification.DrtPlanModifierConfigGroup;
import de.mpi.ds.my_analysis.MyAnalysisModule;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MatsimMain {

    private static final Logger LOG = Logger.getLogger(MatsimMain.class.getName());

    public static void main(String[] args) {
        //TODO why trip success so low for square grid
        LOG.info("Reading config");
        Config config = ConfigUtils
                .loadConfig(args[0], new MultiModeDrtConfigGroup(), new DvrpConfigGroup(), new OTFVisConfigGroup());
//      Config config = ConfigUtils.loadConfig(args[0], new OTFVisConfigGroup());
//        config.global().setNumberOfThreads(1);

        LOG.info("Starting matsim simulation...");
        try {
//            runMultipleOptDrtCount(config, args[1], args[2], args[3], args[4], false);
//            runMultipleConvCrit(config, args[1], args[2], args[3], args[4], false);
//            runMultipleNetworks(config);
            run(config, args[1], false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        LOG.info("Simulation finished");
    }

    private static void runMultipleNetworks(Config config) {
//        Network network = NetworkUtils.readNetwork(config.network().getInputFile());
        //TODO modify network (own network modifier class?)
        //TODO run simulations
    }

    public static void run(Config config, String modifyPlans, boolean otfvis) throws Exception {
        //TODO make all links into acc/egr stations divide into north east ...
        //TODO why worked when small links had long lengths
        if (!modifyPlans.equals("true") && !modifyPlans.equals("false")) {
            throw new Exception("modifyPlans parameter must be \"true\" or \"false\"");
        }
        String vehiclesFile = getVehiclesFile(config);
        LOG.info(
                "STARTING with\npopulation file: " + config.plans().getInputFile() +
                        " and\nvehicles file: " + vehiclesFile + "\n---------------------------");

        // For dvrp/drt
        Controler controler = DrtControlerCreator.createControler(config, otfvis);
        Collection<DrtConfigGroup> modalElements = MultiModeDrtConfigGroup.get(config).getModalElements();
        System.out.println(modalElements.size());

        // For only pt
//		Scenario scenario = ScenarioUtils.loadScenario(config);
//		Controler controler = new Controler(scenario);

        // Set up SBB Transit/Raptor
        controler.addOverridingModule(new SwissRailRaptorModule());

        //Custom Modules
//        controler.addOverridingModule(new BimodalAssignmentModule());
//        controler.addOverridingModule(new GridPrePlanner());
        controler.addOverridingModule(new MyAnalysisModule());
        controler.addOverridingQSimModule(new CustomTransitStopHandlerModule());
        if (modifyPlans.equals("true")) {
//            DrtPlanModifierConfigGroup dpmcp = new DrtPlanModifierConfigGroup();
//            dpmcp.setModifyPlans(true);
//            dpmcp.setGammaCut(23);
            DrtPlanModifierConfigGroup group = ConfigUtils.addOrGetModule(config, DrtPlanModifierConfigGroup.class);
            LOG.error(group);
            controler.addOverridingModule(new DrtPlanModifier(group));
        }
//                (DrtPlanModifierConfigGroup) config.getModules().get(DrtPlanModifierConfigGroup.NAME)));

        controler.run();
    }

    private static void runMultipleOptDrtCount(Config config, String modifyPlans, String popDir, String drtDir,
                                               String appendOutDir, boolean otfvis) throws Exception {
        Pattern patternPop = Pattern.compile("population(.*)\\.xml\\.gz");
        Pattern patternDrt = null;
        Pattern patternDrt2 = null;
        boolean splittedFleet = false;

        if (drtDir.matches(".*/splitted/?")) {
            splittedFleet = true;
            patternDrt = Pattern.compile("drtvehicles_(.*?)_1\\.xml\\.gz");
            patternDrt2 = Pattern.compile("drtvehicles_(.*?)_2\\.xml\\.gz");
        } else
            patternDrt = Pattern.compile("drtvehicles_(.*?)\\.xml\\.gz");

        String[] populationFiles = getFiles(patternPop, popDir);
        String[] drtVehicleFiles = getFiles(patternDrt, drtDir);
        String[] drtVehicleFiles2 = splittedFleet ? getFiles(patternDrt2, drtDir) : null;


        for (int i = 0; i < populationFiles.length; i++) {
            for (int j = 0; j < drtVehicleFiles.length; j++) {
                String populationFile = populationFiles[i];
                String drtVehicleFile = drtVehicleFiles[j];
                String drtVehicleFile2 = splittedFleet ? drtVehicleFiles2[j] : null;
                Matcher matcherPop = patternPop.matcher(populationFile);
                Matcher matcherDrt = patternDrt.matcher(drtVehicleFile);
                matcherPop.find();
                matcherDrt.find();

                MultiModeDrtConfigGroup multiModeConfGroup = MultiModeDrtConfigGroup.get(config);
                Collection<DrtConfigGroup> modalElements = multiModeConfGroup.getModalElements();
                List<DrtConfigGroup> modalElementsList = new ArrayList<>(modalElements);
                config.plans().setInputFile(popDir + populationFile);
                if (splittedFleet) {
                    LOG.error("Two drt modal elements expected in config file for splitted Fleet scenario!");
                    modalElementsList.get(0).setVehiclesFile(Paths.get(drtDir, drtVehicleFile).toString());
                    modalElementsList.get(1).setVehiclesFile(Paths.get(drtDir, drtVehicleFile2).toString());
                } else {
                    LOG.error("Only one drt modal element expected in config file; removing additional one");
                    modalElementsList.get(0).setVehiclesFile(drtDir + drtVehicleFile);
                    try {
                        multiModeConfGroup.removeParameterSet(modalElementsList.get(1));
                    } catch (Exception e) {
                        LOG.warn("Already removed second parameter set");
                    }
                }


//                assert matcherDrt.group(1).equals(matcherPop.group(1)) : "Running with files for different scenarios";
                config.controler()
                        .setOutputDirectory(Paths.get("./output".concat(appendOutDir), matcherDrt.group(1)).toString());
//                System.out.println(populationFile);
//                System.out.println(drtVehicleFile);
//                System.out.println(drtVehicleFile2);
//                System.out.println("./output/" + matcherDrt.group(1));

                run(config, modifyPlans, otfvis);
            }
        }

    }

    private static void runMultipleConvCrit(Config config, String zetas, String popDir, String drtDir,
                                            String appendOutDir, boolean otfvis) throws Exception {

        String[] zetaList = zetas.split(",");
        System.out.println(zetaList);

        String populationPath = Paths.get(popDir, "population_zeta0_0.xml.gz").toString();
        String drtPath = Paths.get(drtDir, "drtvehicles.xml.gz").toString();

        for (String zeta : zetaList) {

            config.plans().setInputFile(populationPath);
            Collection<DrtConfigGroup> modalElements = MultiModeDrtConfigGroup.get(config).getModalElements();
            assert modalElements.size() == 1 : "Only one drt modal element expected in config file";
            modalElements.stream().findFirst().get().setVehiclesFile(drtPath);

            ConfigUtils.addOrGetModule(config, DrtPlanModifierConfigGroup.class).setZetaCut(Double.parseDouble(zeta));
            config.controler()
                    .setOutputDirectory(Paths.get("./output".concat(appendOutDir), "zeta".concat(zeta)).toString());
            System.out.println(populationPath);
            System.out.println(drtPath);
            System.out.println("Output: " + config.controler().getOutputDirectory());

            run(config, "true", otfvis);
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