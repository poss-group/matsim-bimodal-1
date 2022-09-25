# Bimodal Simulation with matsim

[Matsim](https://www.matsim.org/) is an open-source software written in java for agent-based mobility simulations.

## Usage
1. Clone the repository
2. Clone the forked [matsim-libs](https://github.com/poss-group/matsim-libs) repository
    - Checkout branch:
        -  `my_13.x` for model grid simulations with periodic BC. The periodic BC are fixed for 10km, the simulated model grid has to have dimensions 10km x 10km. Otherwise some distance functions in the following classes from the matsim-libs repository have to be changed:<br>
        `org/matsim/core/router/util/LandmarkerPieSlices.java`<br>
        `org/matsim/core/utils/geometry/CoordUtils.java`<br>
        `org/matsim/core/utils/collections/QuadTree.java`<br>
        `org/matsim/contrib/util/distance/DistanceUtils.java`
        -  `my_13_osm.x` for simulations on real street networks without periodic BC
    - Cd in root of matsim-libs and type `mvn install` (or `mvn install -DskipTests` to skip testing) to install into your local maven repository
This has to be done only once as long as nothing is changed in the matsim-libs repo.
3. Go into matsim-bimodal repository and choose the `matsim.version` in the `pom.xml`:
    - Either `13.1-MyVersion` for model grid simulations with periodic BC
    - Or `13.1-MyVersionOsm` for simulations on real street networks without periodic BC
5. Type `mvn clean package` to build matsim and its dependencies with maven
6. Execute the `matsim-bimodal-1.0-SNAPSHOT-jar-with-dependencies` file (located in the `target` directory) in a directory with all the necessary input files (see [MATSim User Guide](https://www.matsim.org/docs/userguide/)) with arguments according to the `src/main/java/de/mpi/ds/MatsimMain.java` class.
    - Such directories with the necessary input files are explained in the next section of the readme.
9. For visualisation a tool called [*via*](https://www.simunto.com/via/) is available online

## Scenarios
1. **fine_grid/bimodal**
    - grid model for bimodal simulations.
    - Necessary Matsim Version: `13.1-MyVersion`
2. **fine_grid/unimodal**
    - grid model for bimodal simulations.
    - Necessary Matsim Version: `13.1-MyVersion`
3. **Berlin**
    - Real street network simulation for Berlin (area inside of "Berliner Ringbahn").
    - Necessary Matsim Version: `13.1-MyVersionOsm`
4. **Manhattan**
    - Real street network simulation for Manhattan.
    - Necessary Matsim Version: `13.1-MyVersionOsm`
