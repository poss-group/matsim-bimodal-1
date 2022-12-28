#!/bin/bash

#$ -S /bin/bash
#$ -cwd
#$ -q grotrian.q
# -pe mvapich2-grotrian 10
#$ -j yes
#$ -N input_creation_out
#$ -l h_stack=8g
#$ -t 1-41

railIntervals="40"
small_railIntervals=(5 10 20)
mode="create-input"
Ndrt="1000"
carGridSpacing="100"
nReqs=(100 500 1000 5000 10000 50000 100000 500000 100000)
d_cuts=(seq 100 100 5000)
outputDir="out1"
mean_travel_dist="2000"
seed="21398"
endTime="36000"
diagonalConnections="True"
ConstDrtDemand="False"
meanspeedscalefactor="1"

((idx_dcut=$SGE_TASK_ID / 27))
((mod_matrix=$SGE_TASK_ID % 27))
((idx_req=$mod_matrix / 3))
((idx_small_railInterval=$mod_matrix % 3))

echo "Creating Input for railInterval =" $railIntervals ",mode =" $mode ", N_drt =" $Ndrt ", carGridSpacing =" $carGridSpacing ", and N_reqs = " ${nReqs[idx_req]}
args="config.xml $mode $mean_travel_dist ${d_cuts[idx_dcut]} $carGridSpacing $railIntervals ${small_railInterval[idx_small_railInterval]} ${Ndrt[$idx_Ndrt]} $nReqs $seed $endTime $diagonalConnections $ConstDrtDemand $meanspeedscalefactor 0 InverseGamma $outputDir"
#echo $args

#done

../../../../jdk-11.0.2/bin/java -Xmx8g -jar ../../../../matsim-bimodal-1.0-SNAPSHOT-jar-with-dependencies.jar $args

echo "Starting simulations for above input"
args="config.xml $mode $mean_travel_dist ${d_cuts[idx_dcut]}$carGridSpacing $railIntervals ${small_railInterval[idx_small_railInterval ${Ndrt[$idx_Ndrt]} $nReqs $seed $endTime $diagonalConnections $ConstDrtDemand $meanspeedscalefactor 0 InverseGamma outputDir"
../../../../jdk-11.0.2/bin/java -Xmx8g -jar ../../../../matsim-bimodal-1.0-SNAPSHOT-jar-with-dependencies.jar $args
