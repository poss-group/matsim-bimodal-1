package de.mpi.ds;

import de.mpi.ds.DrtTrajectoryAnalyzer.DrtTrajectoryAnalyzer;
import de.mpi.ds.custom_routing.CustomRoutingModule;
import de.mpi.ds.custom_transit_stop_handler.CustomTransitStopHandlerModule;
import de.mpi.ds.drt_plan_modification.DrtPlanModifier;
import de.mpi.ds.drt_plan_modification.DrtPlanModifierConfigGroup;
import de.mpi.ds.my_analysis.MyAnalysisModule;
import de.mpi.ds.osm_utils.ScenarioCreatorBuilderOsm;
import de.mpi.ds.osm_utils.ScenarioCreatorOsm;
import de.mpi.ds.parking_vehicles_tracker.ParkingVehicleTracker;
import de.mpi.ds.utils.ScenarioCreator;
import de.mpi.ds.utils.ScenarioCreatorBuilder;
import org.apache.log4j.Logger;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtControlerCreator;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup; //matsim-libs/contribs
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.vis.otfvis.OTFVisConfigGroup;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

public class MatsimMain {

    private static final Logger LOG = Logger.getLogger(MatsimMain.class.getName());

    /* This is the main method from which can be executed:
    * 1. Grid Model Simulations -> runGridModel(...)
    * 2. Real World Simulations -> rundRealWorld(...)
    * In the scnd. case the matsim version has to specified in pom.xml as:
    *   <matsim.version>13.1-MyVersionOsm</matsim.version>
    * which can be built by the forked matsim-libs repo (branch: my_13_osm.x)
    * ----
    * In the first case the matsim version has to specified in pom.xml as:
    *   <matsim.version>13.1-MyVersion</matsim.version>
    * which can be built by the forked matsim-libs repo (branch: my_13.x)
    * In this case the created network always has a size of 1000m in conventional units,
    * this is important for the periodic boundary conditions - if other sizes are wanted to be
    * simulated the corresponding lengths have to rescaled w.r.t. to this length */

    public static void main(String[] args) {
        LOG.info("Reading config");
        Config config = ConfigUtils.loadConfig(args[0], new MultiModeDrtConfigGroup(), new DvrpConfigGroup(),
                new DrtPlanModifierConfigGroup(), new OTFVisConfigGroup());

        LOG.info("Starting matsim simulation...");
        try {
            runGridModel(config, args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9],
                    args[10], args[11], args[12], args[13], args[14], args[15], args[16]);
//            runRealWorld(config, args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8],
//                    args[9], args[10], args[11]);

//            manuallyStartMultipleDeltaMax(args[0]);
//            run(config, false, false);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        LOG.info("Simulation finished");
    }

    private static void runRealWorld(Config config, String mode, String meanDistString, String dCutString,
                                             String ptSpacingString, String nDrtString, String nReqsString,
                                             String seedString, String endTimeString, String trainDepartureInterval,
                                             String drtSpeedString, String outFolder) throws Exception {
        String basicOutPath = config.controler().getOutputDirectory();
        if (!outFolder.equals("")) {
            basicOutPath = basicOutPath.concat("/" + outFolder);
        }
        double ptSpacing = Double.parseDouble(ptSpacingString);
        int nReqs = Integer.parseInt(nReqsString);
        double meanDist = Double.parseDouble(meanDistString);
        double trainFreq = Double.parseDouble(trainDepartureInterval);
        double dCut = Double.parseDouble(dCutString);
        int nDrt = Integer.parseInt(nDrtString);
        double drtSpeed = Double.parseDouble(drtSpeedString);
        String nReqsOutPath = Paths.get(basicOutPath, nReqsString.concat("reqs")).toString();
        String meanDistOutPath = Paths.get(nReqsOutPath, meanDistString.concat("dist")).toString();
        String nDrtOutPath = Paths.get(meanDistOutPath, nDrtString.concat("drt")).toString();
        String dCutOutPath = Paths.get(nDrtOutPath, dCutString.concat("dcut")).toString();
        String lOutPaht = Paths.get(dCutOutPath, ptSpacingString.concat("l")).toString();

        String inputPath = Paths.get(lOutPaht, "input").toString();
        String networkPathIn = "network.xml";
        String networkPathOut = Paths.get(inputPath, "network_input.xml.gz").toString();
        String populationPath = Paths.get(inputPath, "population_input.xml.gz").toString();
        String transitSchedulePath = Paths.get(inputPath, "transitSchedule_input.xml.gz").toString();
        String transitVehiclesPath = Paths.get(inputPath, "transitVehicles_input.xml.gz").toString();
        String drtFleetPath = Paths.get(inputPath, "drtvehicles_input.xml.gz").toString();
        String drtFleetPath2 = Paths.get(inputPath, "acc_egr_drtvehicles_input.xml.gz").toString();

        config.network().setInputFile(networkPathOut);
        config.plans().setInputFile(populationPath);
        config.transit().setTransitScheduleFile(transitSchedulePath);
        config.transit().setVehiclesFile(transitVehiclesPath);

        List<DrtConfigGroup> drtConfigGroups = new ArrayList<>(MultiModeDrtConfigGroup.get(config).getModalElements());
        drtConfigGroups.get(0).setVehiclesFile(drtFleetPath);
        if (drtConfigGroups.size() == 2) {
            drtConfigGroups.get(1).setVehiclesFile(drtFleetPath2);
        }

        String outPath = null;
        if (mode.equals("create-input")) {

            OutputDirectoryLogging.initLoggingWithOutputDirectory(inputPath);

            double endTime = Double.parseDouble(endTimeString);

            ScenarioCreatorOsm scenarioCreatorOsm = new ScenarioCreatorBuilderOsm()
                    .setNRequests(nReqs).setTravelDistanceDistribution("InverseGamma")
                    .setSeed(Long.parseLong(seedString)).setMeanTravelDist(meanDist)
                    .setRequestEndTime((int) (endTime - 3600)).setTransitEndTime(endTime)
                    .setDrtOperationEndTime(endTime).setPtSpacing(ptSpacing)
                    .setCutoffDistance(dCut)
                    .setFreeSpeedCar(drtSpeed)
                    .setdrtFleetSize(nDrt).setDepartureIntervalTime(trainFreq).build();
            double mu = 1. / scenarioCreatorOsm.getDepartureIntervalTime();
            double nu = 1. / scenarioCreatorOsm.getRequestEndTime();
            LOG.info("Q: " + mu / (nu * scenarioCreatorOsm.getnRequests() * scenarioCreatorOsm.getTravelDistanceMean() *
                    scenarioCreatorOsm.getTravelDistanceMean()));
            LOG.info("Creating network");
            scenarioCreatorOsm
                    .addTramsToNetwork(networkPathIn, networkPathOut, transitSchedulePath, transitVehiclesPath);
            LOG.info("Finished creating network\nCreating population for network");
            scenarioCreatorOsm.generatePopulation(populationPath);
            LOG.info("Finished creating population\nCreating transit Schedule");
            if (drtConfigGroups.size() == 1) {
                scenarioCreatorOsm.generateDrtVehicles(drtFleetPath);
            } else if (drtConfigGroups.size() == 2) {
                scenarioCreatorOsm.generateDrtVehicles(drtFleetPath, drtFleetPath2, dCut);
            }
            LOG.info("Finished creating drt fleet");
            return;
        } else if (mode.equals("bimodal")) {
            ConfigUtils.addOrGetModule(config, DrtPlanModifierConfigGroup.class)
                    .setdCut(dCut);
//            MultiModeDrtConfigGroup.get(config).getModalElements().stream().findFirst().orElseThrow()
//                    .setMaxDetour(deltaMax);
            outPath = Paths.get(lOutPaht, mode).toString();
        } else if (mode.equals("unimodal")) {
//            MultiModeDrtConfigGroup.get(config).getModalElements().stream().findFirst().orElseThrow()
//                    .setMaxDetour(deltaMax);
            outPath = Paths.get(nDrtOutPath, mode).toString();
            config.transit().setUseTransit(false);
        } else if (mode.equals("car")) {
            outPath = Paths.get(meanDistOutPath, mode).toString();
            config.transit().setUseTransit(false);
        }
        DrtPlanModifierConfigGroup.get(config).setMode(mode);

        config.controler().setOutputDirectory(outPath);
        LOG.info("Running simulation");
        run(config, false, true);
        LOG.info("Finished simulation");
    }

    private static void runGridModel(Config config, String mode, String travelDistMeanString,
                                            String dCutString,
                                            String carGridSpacingString, String railIntervalString, String small_railIntervalString, String N_drt,
                                            String nReqsString, String seedString,
                                            String endTimeString, String diagConnections, String constDrtDemandString,
                                            String meanAndSpeedScaleFactorString, String fracWithCommonOrigDestString,
                                            String travelDistanceDistributionString, String outFolder) throws
            Exception {
        // 20000000 requests for varying dcut from 200 to 10000; resulting requests: 11922 to 130679
        String basicOutPath = config.controler().getOutputDirectory();

        //if outFolder is not empty then create a subdirectory
        if (!outFolder.equals("")) {
            basicOutPath = basicOutPath.concat("/" + outFolder);
        }

        //read and typecast the control parameters
        int railInterval = Integer.parseInt(railIntervalString);
        int small_railInterval = Integer.parseInt(small_railIntervalString);
        int nReqs = Integer.parseInt(nReqsString);
        double carGridSpacing = Double.parseDouble(carGridSpacingString);
//        double deltaMax = Double.parseDouble(deltaMaxString);
        double dCut = Double.parseDouble(dCutString);
        double travelDistMean = Double.parseDouble(travelDistMeanString);
        double meanAndSpeedScaleFactor = Double.parseDouble(meanAndSpeedScaleFactorString);
        boolean constDrtDemand = Boolean.parseBoolean(constDrtDemandString);
        double fracWithCommonOrigDest = Double.parseDouble(fracWithCommonOrigDestString);
        //if system size is not modified
//        String iterationSpecificPath = Paths.get(basicOutPath,
//                nReqsString.concat("reqs"),
//                N_drt.concat("drt"),
//                dCutString.concat("dcut"),
//                travelDistMeanString.concat("dist"),
//                (int) (railInterval * carGridSpacing) + "l")
//                .toString();

        //create output path objects
        String nReqsOutPath = Paths.get(basicOutPath, nReqsString.concat("reqs")).toString();
        String meanDistOutPath = Paths.get(nReqsOutPath, travelDistMeanString.concat("dist")).toString();
        String fracComOrigDestPath = Paths
                .get(meanDistOutPath, fracWithCommonOrigDestString.concat("frac_comm_orig_dest")).toString();
        String nDrtOutPath = Paths.get(fracComOrigDestPath, N_drt.concat("drt")).toString();
        String dCutOutPath = Paths.get(nDrtOutPath, dCutString.concat("dcut")).toString();
        String small_lOutPath = Paths.get(dCutOutPath,small_railIntervalString.concat("small_railInterval")).toString();
        String lOutPath = Paths.get(small_lOutPath, (int) (railInterval * carGridSpacing) + "l").toString();

        //create input path objects
        String inputPath = Paths.get(lOutPath, "input").toString();
        String networkPath = Paths.get(inputPath, "network_input.xml.gz").toString();
        String populationPath = Paths.get(inputPath, "population_input.xml.gz").toString();
        String transitSchedulePath = Paths.get(inputPath, "transitSchedule_input.xml.gz").toString();
        String transitVehiclesPath = Paths.get(inputPath, "transitVehicles_input.xml.gz").toString();
        String drtFleetPath = Paths.get(inputPath, "drtvehicles_input.xml.gz").toString();

        //configure network,plans,vehicles,populations
        config.network().setInputFile(networkPath);
        config.plans().setInputFile(populationPath);
        config.transit().setTransitScheduleFile(transitSchedulePath);
        config.transit().setVehiclesFile(transitVehiclesPath);
        MultiModeDrtConfigGroup.get(config).getModalElements().stream().findFirst().orElseThrow()
                .setVehiclesFile(drtFleetPath);

        String outPath = null;
        if (mode.equals("create-input")) {

            OutputDirectoryLogging.initLoggingWithOutputDirectory(inputPath);

            double endTime = Double.parseDouble(endTimeString);

            ScenarioCreator scenarioCreator = new ScenarioCreatorBuilder().setCarGridSpacing(carGridSpacing)
                    .setRailInterval(railInterval).setSmall_railInterval(small_railInterval).setNRequests(nReqs)
                    .setTravelDistanceDistribution(travelDistanceDistributionString)
                    .setSeed(Long.parseLong(seedString)).setTravelDistanceMean(travelDistMean)
                    .setRequestEndTime((int) (endTime - 3600)).setTransitEndTime(endTime)
                    .setDrtOperationEndTime(endTime)
                    .setFracWithCommonOrigDest(fracWithCommonOrigDest)
                    .setDiagonalConnetions(Boolean.parseBoolean(diagConnections))
                    .setMeanAndSpeedScaleFactor(meanAndSpeedScaleFactor)
                    .setConstDrtDemand(constDrtDemand)
                    .setCutoffDistance(dCut)
                    .setSmallLinksCloseToNodes(false).setdrtFleetSize(Integer.parseInt(N_drt)).build();
//            double mu = 1. / scenarioCreator.getDepartureIntervalTime();
//            double nu = 1. / scenarioCreator.getRequestEndTime();
//            double E = scenarioCreator.getnRequests() /
//                    (scenarioCreator.getSystemSize() * scenarioCreator.getSystemSize());
//            double avDist = scenarioCreator.getSystemSize() * scenarioCreator.getTravelDistanceMeanOverL();
//            LOG.info("Q: " + mu / (nu * scenarioCreator.getnRequests() * scenarioCreator.getTravelDistanceMean() *
//                    scenarioCreator.getTravelDistanceMean()));
            LOG.info("Creating network / Transit");
            scenarioCreator.createNetwork(networkPath, transitSchedulePath, transitVehiclesPath);
            LOG.info("Finished creating network\nCreating population for network");
            scenarioCreator.createPopulation(populationPath, networkPath);
            LOG.info("Finished creating population");
            LOG.info("Finished creating population\nCreating drt fleet");
            scenarioCreator.createDrtFleet(networkPath, drtFleetPath);
            LOG.info("Finished creating drt fleet");
            OutputDirectoryLogging.closeOutputDirLogging();
            return;
        } else if (mode.equals("bimodal")) {
//            MultiModeDrtConfigGroup.get(config).getModalElements().stream().findFirst().orElseThrow()
//                    .setMaxDetour(deltaMax);
            ConfigUtils.addOrGetModule(config, DrtPlanModifierConfigGroup.class)
                    .setdCut(dCut);
            outPath = Paths.get(lOutPath, mode).toString();
        } else if (mode.equals("unimodal")) {
//            MultiModeDrtConfigGroup.get(config).getModalElements().stream().findFirst().orElseThrow()
//                    .setMaxDetour(deltaMax);
            outPath = Paths.get(nDrtOutPath, mode).toString();
            config.transit().setUseTransit(false);
        } else if (mode.equals("car")) {
            outPath = Paths.get(meanDistOutPath, mode).toString();
        }
        DrtPlanModifierConfigGroup.get(config).setMode(mode);
//        if (mode.equals("car")) {
//            config.removeModule(MultiModeDrtConfigGroup.GROUP_NAME);
//        }

        config.controler().setOutputDirectory(outPath);
        LOG.info("Running simulation");
        run(config, false, true);
        LOG.info("Finished simulation!");
//        LOG.info("Deleting input directory");
    }


    public static void run(Config config, boolean otfvis, boolean isGridAndPt) throws Exception {
        // For dvrp/drt
        Controler controler = DrtControlerCreator.createControler(config, otfvis);
        Collection<DrtConfigGroup> modalElements = MultiModeDrtConfigGroup.get(config).getModalElements();

        // For only pt
//		Scenario scenario = ScenarioUtils.loadScenario(config);
//		Controler controler = new Controler(scenario);

        // Set up SBB Transit/Raptor
//        controler.addOverridingModule(new SBBTransitModule());
////        controler.addOverridingModule(new SwissRailRaptorModule());
//        controler.configureQSimComponents(components -> {SBBTransitEngineQSimModule.configure(components);});


        //Custom Modules
//        controler.addOverridingModule(new BimodalAssignmentModule());
//        controler.addOverridingModule(new GridPrePlanner());
        controler.addOverridingModule(new MyAnalysisModule());
        controler.addOverridingQSimModule(new CustomTransitStopHandlerModule());

        controler.addOverridingModule(new DrtTrajectoryAnalyzer());
        DrtPlanModifierConfigGroup drtGroup = ConfigUtils.addOrGetModule(config, DrtPlanModifierConfigGroup.class);
        controler.addOverridingModule(new DrtPlanModifier(drtGroup));
        controler.addOverridingModule(new CustomRoutingModule());
        controler.addOverridingModule(new ParkingVehicleTracker());

        controler.run();
    }

}
