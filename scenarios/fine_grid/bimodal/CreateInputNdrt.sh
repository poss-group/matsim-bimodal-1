#!/bin/bash

#$ -S /bin/bash
#$ -cwd
#$ -q grotrian.q
# -pe mvapich2-grotrian 10
#$ -j yes
#$ -N input_creation_out
#$ -l h_stack=8g
#$ -t 1-41

railIntervals="24"
mode="create-input"
Ndrt=(`seq 450 5 650`)
carGridSpacing="200"
nReqs="125000"
outputDir="out1"

#for SGE_TASK_ID in $(seq 0 1 40); do

((mod100=$SGE_TASK_ID % 100))
((div100=$SGE_TASK_ID / 100))

((idx_railInterval = 0))
((idx_Ndrt = $mod100))

echo "Creating Input for railInterval =" ${railIntervals[$idx_railInterval]} ",mode =" $mode ", N_drt =" ${Ndrt[$idx_Ndrt]} ", carGridSpacing =" $carGridSpacing ", and N_reqs = " $nReqs
args="config.xml ${railIntervals[$idx_railInterval]} $carGridSpacing ${Ndrt[$idx_Ndrt]} $nReqs $mode $outputDir"
#echo $args

#done

../../../../jdk-11.0.2/bin/java -Xmx8g -jar ../../../../matsim-bimodal-varyl2000MeanInvGammaDiag-1.0-SNAPSHOT-jar-with-dependencies.jar $args
