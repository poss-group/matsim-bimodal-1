package de.mpi.ds;

import de.mpi.ds.DrtTrajectoryAnalyzer.DrtTrajectoryAnalyzer;
import de.mpi.ds.custom_routing.CustomRoutingModule;
import de.mpi.ds.custom_transit_stop_handler.CustomTransitStopHandlerModule;
import de.mpi.ds.drt_plan_modification.DrtPlanModifier;
import de.mpi.ds.drt_plan_modification.DrtPlanModifierConfigGroup;
import de.mpi.ds.my_analysis.MyAnalysisModule;
import de.mpi.ds.utils.ScenarioCreator;
import de.mpi.ds.utils.ScenarioCreatorBuilder;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.mpi.ds.utils.GeneralUtils.doubleCloseToZero;
import static de.mpi.ds.utils.GeneralUtils.getNetworkDimensionsMinMax;

public class MatsimMain {

    private static final Logger LOG = Logger.getLogger(MatsimMain.class.getName());

    public static void main(String[] args) {
        LOG.info("Reading config");
        Config config = ConfigUtils.loadConfig(args[0], new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
                new DrtPlanModifierConfigGroup(), new OTFVisConfigGroup());
//      Config config = ConfigUtils.loadConfig(args[0], new OTFVisConfigGroup());
//        config.global().setNumberOfThreads(1);

        LOG.info("Starting matsim simulation...");
        try {
//            runMultipleOptDrtCount(config, args[1], args[2], args[3], false);
//            runMultipleConvCrit(config, args[1], args[2], args[3], args[4], false);
            runMultipleNetworks(config);
//            runRealWorldScenario(config);
//            run(config, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        LOG.info("Simulation finished");
    }

    private static void runRealWorldScenario(Config config) throws Exception {
        ScenarioCreator scenarioCreator = new ScenarioCreatorBuilder().setnDrtVehicles(20).setnRequests(10000)
                .setDrtOperationEndTime(3600).setTransportMode(TransportMode.drt).setGridNetwork(false).build();
        scenarioCreator.createPopulation("population.xml.gz", "network.xml");
        scenarioCreator.createDrtFleet("network.xml", "drtVehicles.xml");

        config.qsim().setEndTime(3600);

        run(config, false);
    }

    private static void runMultipleNetworks(Config config) throws Exception {
        String basicOutPath = config.controler().getOutputDirectory();
        String mode = "L_l_";
        for (int x : new int[]{2, 3, 4, 5, 6, 7, 8, 9, 10}) {
            String iterationSpecificPath = Paths.get(basicOutPath, mode + x).toString();
            String inputPath = Paths.get(iterationSpecificPath, "input").toString();
            String networkPath = Paths.get(inputPath, "network_input.xml.gz").toString();
            String populationPath = Paths.get(inputPath, "population_input.xml.gz").toString();
            String transitSchedulePath = Paths.get(inputPath, "transitSchedule_input.xml.gz").toString();
            String transitVehiclesPath = Paths.get(inputPath, "transitVehicles_input.xml.gz").toString();
            String drtFleetPath = Paths.get(inputPath, "drtvehicles_input.xml.gz").toString();

            // Varying drt grid size w.r.t. pt grid size
//                int SysOvPt = 10;
//                ScenarioCreator scenarioCreator = new ScenarioCreatorBuilder().setSystemSizeOverGridSize(x*SysOvPt)
//                        .setSystemSizeOverPtGridSize(SysOvPt).build();
            // Varying pt grid size w.r.t. system grid size
            ScenarioCreator scenarioCreator = new ScenarioCreatorBuilder().setSystemSizeOverGridSize(100)
                    .setSystemSizeOverPtGridSize(x).build();
            LOG.info("Creating network");
            scenarioCreator.createNetwork(networkPath, true);
            LOG.info("Finished creating network\nCreating population for network");
            scenarioCreator.createPopulation(populationPath, networkPath);
            LOG.info("Finished creating population\nCreating transit Schedule");
            scenarioCreator.createTransitSchedule(networkPath, transitSchedulePath, transitVehiclesPath);
            LOG.info("Finished creating transit schedule\nCreating drt fleet");
            scenarioCreator.createDrtFleet(networkPath, drtFleetPath);
            LOG.info("Finished creating drt fleet");

            config.network().setInputFile(networkPath);
            config.plans().setInputFile(populationPath);
            config.transit().setTransitScheduleFile(transitSchedulePath);
            config.transit().setVehiclesFile(transitVehiclesPath);
            MultiModeDrtConfigGroup.get(config).getModalElements().stream().findFirst().orElseThrow()
                    .setVehiclesFile(drtFleetPath);

            for (String bim : new String[]{"bimodal", "car"}) {
                String outPath = Paths.get(iterationSpecificPath, bim).toString();
                if (bim.equals("bimodal")) {
                    DrtPlanModifierConfigGroup.get(config).setPrivateCarMode(false);
                } else if (bim.equals("car")) {
                    DrtPlanModifierConfigGroup.get(config).setPrivateCarMode(true);
//                    MultiModeDrtConfigGroup.get(config).getModalElements().clear();
                }
//                DrtPlanModifierConfigGroup.get(config).setZetaCut(0);

                config.controler().setOutputDirectory(outPath);
                LOG.info("Running simulation");
                run(config, false);
                LOG.info("Finished simulation with " + mode + " = " + x);
            }
        }
    }

    public static void run(Config config, boolean otfvis) throws Exception {
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
//        controler.addOverridingModule(new SwissRailRaptorModule());

        //Custom Modules
//        controler.addOverridingModule(new BimodalAssignmentModule());
//        controler.addOverridingModule(new GridPrePlanner());
        controler.addOverridingModule(new MyAnalysisModule());
        controler.addOverridingQSimModule(new CustomTransitStopHandlerModule());
        controler.addOverridingModule(new DrtTrajectoryAnalyzer());
        DrtPlanModifierConfigGroup drtGroup = ConfigUtils.addOrGetModule(config, DrtPlanModifierConfigGroup.class);
        controler.addOverridingModule(new DrtPlanModifier(drtGroup));
        controler.addOverridingModule(new CustomRoutingModule());

        double[] netDims = getNetworkDimensionsMinMax(controler.getScenario().getNetwork(), true);
        assert (doubleCloseToZero(netDims[0]) && doubleCloseToZero(netDims[1] - 10000)) :
                "You have to change L (" + netDims[0] + "," + netDims[1] + ") in: " +
                        "org/matsim/core/router/util/LandmarkerPieSlices.java; " +
                        "org/matsim/core/utils/geometry/CoordUtils.java; " +
                        "org/matsim/core/utils/collections/QuadTree.java" +
                        "org/matsim/contrib/util/distance/DistanceUtils.java";

        controler.run();
    }

    private static void runMultipleOptDrtCount(Config config, String popDir, String drtDir,
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

                run(config, otfvis);
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