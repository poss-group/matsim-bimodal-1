# Bimodal Simulation with matsim

[Matsim](https://www.matsim.org/) is an open-source software written in java for agent-based mobility simulations.

## Usage
1. Clone the repository 
2. Type `mvn package` to build matsim and its dependencies with maven
3. Execute the `matsim-bimodal-1.0-SNAPSHOT-jar-with-dependencies` file in the `target` directory
4. Load a config file from the `scenarios` directory
5. Start the simulation
6. For visualisation a tool called [*via*](https://www.simunto.com/via/) (with a free license) is available online
7. To clean the build type `mvn clean`
8. To develope go to `src/main/java/de/mpi/ds/` and run the simulation from the `MatsimMain.java` class.

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