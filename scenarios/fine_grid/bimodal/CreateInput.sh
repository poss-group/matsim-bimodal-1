#!/bin/csh

#$ -cwd
#$ -q grotrian.q
# -pe mvapich2-grotrian 10
#$ -j yes
#$ -N input_creation_out
#$ -l h_stack=8g
#$ -t 1-50

set ells = (`seq 500 500 5000`)
set mode = "create-input"
set Ndrt = (`seq 100 100 500`)

#foreach SGE_TASK_ID (`seq 1 40`)
@ mod10 = ($SGE_TASK_ID - 1) % 10 + 1
@ div10 = ($SGE_TASK_ID - 1) / 10 + 1

echo "Creating Input for ell =" ${ells[$mod10]} ",mode =" $mode "and N_drt =" ${Ndrt[$div10]}
#end

../../../../jdk-11.0.2/bin/java -Xmx8g -jar ../../../../matsim-bimodal-varyl-1.0-SNAPSHOT-jar-with-dependencies.jar config.xml ${ells[$mod10]} ${Ndrt[$div10]} $mode
