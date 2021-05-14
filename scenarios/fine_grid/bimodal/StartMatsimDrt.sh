#!/bin/bash

#$ -S /bin/bash
#$ -cwd
#$ -q grotrian.q
# -pe mvapich2-grotrian 10
#$ -j yes
#$ -N out_matsim_drt
#$ -l h_stack=8g
#$ -t 1-40

railInterval="24"
#Ndrt=(`seq 450 5 650`)
carGridSpacing="100"
#nReqsList=("10" "100" "1000" "10000" "100000")
nReqsList=("100000")
outputDir="100GridSpacing"

#for SGE_TASK_ID in $(seq 1 40); do

((idx=SGE_TASK_ID-1))
((mod40=$idx % 40))
((div40=$idx / 40))

#((diff=-40 + 10*$mod40))
nReqs=${nReqsList[$div40]}
#Ndrt=$(printf %.$2f $(bc <<< "0.00432*$nReqs+20+$diff"))
((Ndrt=550-20+$mod40))

if [[ $Ndrt == "0" ]]; then
    Ndrt="1"
fi

echo "Creating Input for railInterval =" $railInterval ", mode =" $mode ", N_drt =" $Ndrt ", carGridSpacing =" $carGridSpacing ", and N_reqs = " $nReqs
args1="config.xml $railInterval $carGridSpacing $Ndrt $nReqs create-input $outputDir"
args2="config.xml $railInterval $carGridSpacing $Ndrt $nReqs unimodal $outputDir"
#echo $args1
#echo $args2
#echo " "
#done

../../../../jdk-11.0.2/bin/java -Xmx8g -jar ../../../../matsim-bimodal-varyl2000MeanInvGammaDiag-1.0-SNAPSHOT-jar-with-dependencies.jar $args1
../../../../jdk-11.0.2/bin/java -Xmx8g -jar ../../../../matsim-bimodal-varyl2000MeanInvGammaDiag-1.0-SNAPSHOT-jar-with-dependencies.jar $args2
