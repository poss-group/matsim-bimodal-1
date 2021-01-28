# Bimodal Simulation with matsim

[Matsim](https://www.matsim.org/) is an open-source software written in java for agent-based mobility simulations.

## Usage
1. Clone the repository
2. Clone the forked matsim-libs repository
    - Checkout the branch `add_constraints_drt`
    - Cd in root of matsim-libs and type `mvn install -pl :add_constraints_drt -am -DskipTests` to install drt contribution with additional ellipse constraints into your local maven repository
3. Go into matsim-bimodal and uncomment the lines declaring the dependency on `add_constraints_drt` version in the `pom.xml`
4. Type `mvn package` to build matsim and its dependencies with maven
5. Execute the `matsim-bimodal-1.0-SNAPSHOT-jar-with-dependencies` file in the `target` directory
6. Load a config file from the `scenarios` directory
7. Start the simulation
8. For visualisation a tool called [*via*](https://www.simunto.com/via/) (with a free license) is available online
9. To clean the build type `mvn clean`
10. To develope go to `src/main/java/de/mpi/ds/` and run the simulation from the `MatsimMain.java` class.

<!---
## Scenarios
1. **pt_grid**
    - This scenario represents a public transport simulation on a 2d grid with 100 agents train lines.
---
2. **drt_grid**
    - This scenario represents a simulation of demand responsive transport on a 2d grid with 100 agents and 10 transport vehicles.
---
3. **bimodal_grid**
    - This scenario represents a simulation of demand responsive transport combined wit public transport on a 2d grid with 100 agents, 10 transport vehicles and train lines.
---
4. **bimodal_fine_grid**
    - This scenario represents a simulation of demand responsive transport combined wit public transport on a 2d grid with 1000 agents, 20 train lines and 25 drt vehicles.
-->
