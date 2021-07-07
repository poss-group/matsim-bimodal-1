#!/bin/bash

#$ -S /bin/bash
#$ -cwd
# -l h_stack=6g
#$ -q titan.q
# -pe mvapich2-grotrian 10
#$ -j yes
#$ -N out_matsim_manhatten
#$ -t 1-22

ptSpacingOverMeanList=($(seq 0.1 0.1 2))
Ndrt="1000"
nReqs="150000"
meanDist="2000"
deltaMax="1.5"
seed="42"
endTime="36000"
departureIntervalTime="1800"
outputDir="CI_1800muStopDuration1"

#for SGE_TASK_ID in $(seq 21 22); do

((idx=SGE_TASK_ID-1))
#((mod40=$idx % 40))
#((div40=$idx / 40))

if [[ $idx == 20 ]]; then
    ptSpacingOverMean="2.0"
    mode="unimodal"
elif [[ $idx == 21 ]]; then 
    ptSpacingOverMean="2.0"
    mode="car"
else
    ptSpacingOverMean=${ptSpacingOverMeanList[$idx]}
    mode="bimodal"
fi

#echo "Creating Input for railInterval =" $railInterval ", mode =" $mode ", N_drt =" $Ndrt ", carGridSpacing =" $carGridSpacing ", and N_reqs = " $nReqs
args2="config.xml $ptSpacingOverMean $Ndrt $nReqs $mode $meanDist $deltaMax $seed $endTime $departureIntervalTime $outputDir"
echo $args2
#done

/scratch01.local/hheuer/jdk-11.0.2/bin/java -Xmx6g -jar /scratch01.local/hheuer/matsim-bimodal-Manhatten-1.0-SNAPSHOT-jar-with-dependencies.jar $args2
