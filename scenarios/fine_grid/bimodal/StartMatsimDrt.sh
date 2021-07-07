#!/bin/bash

#$ -S /bin/bash
#$ -cwd
# -l h_stack=6g
#$ -q titan.q
# -pe mvapich2-grotrian 10
#$ -j yes
#$ -N out_matsim_drt
#$ -t 1-40

railInterval="24"
#Ndrt=(`seq 450 5 650`)
carGridSpacing="100"
#nReqsList=("10" "100" "1000" "10000" "100000")
freqs=("3.086" "3.23")
outputDir="100GridSpacing4000Mean"
meanOverL="0.4"
seed="342134"
((endTime=4*3600))
diagConnections="true"

nReqsList=()
for (( i=0; i<"${#freqs[@]}; i++")); do
    nReq=$(printf %.$2f $(bc <<< ${freqs[$i]}*$endTime))
    nReqsList+=("$nReq")
done

#for SGE_TASK_ID in $(seq 1 40); do

((idx=SGE_TASK_ID-1))
((mod40=$idx % 40))
((div40=$idx / 40))

#((diff=-40 + 10*$mod40))
nReqs=${nReqsList[$div40]}
#Ndrt=$(printf %.$2f $(bc <<< "0.00432*$nReqs+20+$diff"))
((Ndrt=550+$mod40))

if [[ $Ndrt == "0" ]]; then
    Ndrt="1"
fi

echo "Creating Input for railInterval =" $railInterval ", mode =" $mode ", N_drt =" $Ndrt ", carGridSpacing =" $carGridSpacing ", and N_reqs = " $nReqs
args1="config.xml $railInterval $carGridSpacing $Ndrt $nReqs create-input $meanOverL $seed $endTime $diagConnections $outputDir"
args2="config.xml $railInterval $carGridSpacing $Ndrt $nReqs unimodal $meanOverL $seed $endTime $diagConnections $outputDir"
#echo $args1
#echo $args2
#done

../../../../jdk-11.0.2/bin/java -Xmx6g -jar ../../../../matsim-bimodal-InvGammaDiag-1.0-SNAPSHOT-jar-with-dependencies.jar $args1
../../../../jdk-11.0.2/bin/java -Xmx6g -jar ../../../../matsim-bimodal-InvGammaDiag-1.0-SNAPSHOT-jar-with-dependencies.jar $args2
