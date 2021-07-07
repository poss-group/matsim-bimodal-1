#!/bin/bash

#$ -S /bin/bash
#$ -cwd
# -l h_stack=6g
#$ -q titan.q
# -pe mvapich2-titan 40
#$ -j yes
#$ -N out_matsim_drt
#$ -t 1-40

railInterval="50"
#Ndrt=(`seq 450 5 650`)
carGridSpacing="100"
#nReqsList=("10" "100" "1000" "10000" "100000")
nReqsList=("100000")
outputDir="100GridSpacingVaryReqs465FleetSize"
meanOverL="0.2"
seed="324"
((endTime=10*3600))
diagConnections="true"
Ndrt="465"

declare -a existing_sims
for f in `ls "output/${outputDir}/"`; do
    existing_sims+=("${f/reqs/}")
    #echo ${f/reqs/}
done

#nReqsList=()
#for (( i=0; i<"${#freqs[@]}; i++")); do
#    nReqElem=$(printf %.$2f $(bc <<< "${freqs[$i]}*($endTime-3600)"))
#    nReqsList+=("$nReqElem")
#done
#echo $nReqsList

#for SGE_TASK_ID in $(seq 1 100); do

((idx=SGE_TASK_ID-1))
#((mod40=$idx % 40))
#((div40=$idx / 40))

#((diff=-100 + $mod40*10))
#nReqs=${nReqsList[$div40]}
#Ndrt=$(printf %.$2f $(bc <<< "0.00456*$nReqs+18+$diff-400"))
#((Ndrt=600+5*$mod40))
((nReqs=105000+100*idx))

if [[ ! " ${existing_sims[@]} " =~ " $nReqs " ]]; then
    #echo "Creating Input for railInterval =" $railInterval ", mode =" $mode ", N_drt =" $Ndrt ", carGridSpacing =" $carGridSpacing ", and N_reqs = " $nReqs
    args1="config.xml $railInterval $carGridSpacing $Ndrt $nReqs create-input $meanOverL $seed $endTime $diagConnections $outputDir"
    args2="config.xml $railInterval $carGridSpacing $Ndrt $nReqs unimodal $meanOverL $seed $endTime $diagConnections $outputDir"
    echo "Args1: $args1"
    echo "Args2: $args2"
    #echo $Ndrt
    ../../../../jdk-11.0.2/bin/java -Xmx6g -jar ../../../../matsim-bimodal-InverseGamma-1.0-SNAPSHOT-jar-with-dependencies.jar $args1
    ../../../../jdk-11.0.2/bin/java -Xmx6g -jar ../../../../matsim-bimodal-InverseGamma-1.0-SNAPSHOT-jar-with-dependencies.jar $args2
fi

#done
exit
