#!/bin/bash

#$ -S /bin/bash
#$ -cwd
# -l h_stack=6g
#$ -q titan.q
# -pe mvapich2-titan 40
#$ -j yes
#$ -N out_NoQueueCheck
#$ -t 1-20

railInterval="50"
#Ndrt=(`seq 450 5 650`)
carGridSpacing="100"
#nReqsList=("10" "100" "1000" "10000" "100000")
nReqsList=("100000")
outputDir="100GridSpacingTTConstraint3Seed324NoQueueCheck"
meanOverL="0.2"
seed="324"
((endTime=10*3600))
diagConnections="true"

#nReqsList=()
#for (( i=0; i<"${#freqs[@]}; i++")); do
#    nReqElem=$(printf %.$2f $(bc <<< "${freqs[$i]}*($endTime-3600)"))
#    nReqsList+=("$nReqElem")
#done
#echo $nReqsList

#for SGE_TASK_ID in $(seq 1 80); do

((idx=SGE_TASK_ID-1))
((mod40=$idx % 40))
((div40=$idx / 40))

#((diff=-100 + $mod40*10))
nReqs=${nReqsList[0]}
#Ndrt=$(printf %.$2f $(bc <<< "0.00456*$nReqs+18+$diff-400"))
((Ndrt=200+5*$idx))

#echo "Creating Input for railInterval =" $railInterval ", mode =" $mode ", N_drt =" $Ndrt ", carGridSpacing =" $carGridSpacing ", and N_reqs = " $nReqs
args1="config.xml $railInterval $carGridSpacing $Ndrt $nReqs create-input $meanOverL $seed $endTime $diagConnections $outputDir"
args2="config.xml $railInterval $carGridSpacing $Ndrt $nReqs unimodal $meanOverL $seed $endTime $diagConnections $outputDir"
echo "Args1: $args1"
echo "Args2: $args2"
#done

/scratch01.local/hheuer/jdk-11.0.2/bin/java -Xmx6g -jar /scratch01.local/hheuer/matsim-bimodal-InverseGammaNoQueueCheck-1.0-SNAPSHOT-jar-with-dependencies.jar $args1
/scratch01.local/hheuer/jdk-11.0.2/bin/java -Xmx6g -jar /scratch01.local/hheuer/matsim-bimodal-InverseGammaNoQueueCheck-1.0-SNAPSHOT-jar-with-dependencies.jar $args2
