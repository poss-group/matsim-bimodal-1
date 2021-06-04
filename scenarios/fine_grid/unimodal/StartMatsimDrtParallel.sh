#!/bin/bash

#$ -S /bin/bash
#$ -V
#$ -cwd
#$ -pe mvapich2-titan 32
#$ -j yes
#$ -N parallel_out
#$ -v OMP_NUM_THREADS=4

# All the args for my executable
railInterval="50"
carGridSpacing="100"
outputDir="100GridSpacingVaryReqsParallel"
meanOverL="0.2"
seed="324"
((endTime=10*3600))
diagConnections="true"
Ndrt="100"

for i in $(seq 0 7); do
    ((threads_from=$i*4))
    ((threads_to=$i*4+3))

    # The argument which I vary
    ((nReqs=1000+100*$i))

    # Merging args and echoing them
    args1="config.xml $railInterval $carGridSpacing $Ndrt $nReqs create-input $meanOverL $seed $endTime $diagConnections $outputDir"
    args2="config.xml $railInterval $carGridSpacing $Ndrt $nReqs unimodal $meanOverL $seed $endTime $diagConnections $outputDir"
    echo "Args1: $args1"
    echo "Args2: $args2"

    #/scratch01.local/hheuer/jdk-11.0.2/bin/java -Xmx6g -jar /scratch01.local/hheuer/matsim-bimodal-InverseGamma-1.0-SNAPSHOT-jar-with-dependencies.jar $args1 > "parallel_job${i}_0.log" 2>&1
    taskset -c ${threads_from}-${threads_to} /scratch01.local/hheuer/jdk-11.0.2/bin/java -Xmx6g -jar /scratch01.local/hheuer/matsim-bimodal-InverseGamma-1.0-SNAPSHOT-jar-with-dependencies.jar $args2 > "parallel_job${i}_1.log" 2>&1 &

done

echo "Jobs: "
jobs

wait
exit
