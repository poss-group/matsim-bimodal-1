#!/bin/csh

#$ -cwd
#$ -q grotrian.q
# -pe mvapich2-grotrian 10
#$ -j yes
#$ -N matsim_cluster_out
#$ -l h_stack=8g
#$ -t 1-110

# Running 1-110 instead of 1-150 because car mode is independent on Ndrt

set ells = (`seq 500 500 5000`)
set modes = ("bimodal" "unimodal" "car")
set Ndrt = (`seq 100 100 500`)

#foreach SGE_TASK_ID (`seq 1 110`)
@ mod10 = ($SGE_TASK_ID - 1) % 10
@ div10 = ($SGE_TASK_ID - 1) / 10
@ div10mod5 = $div10 % 5
@ div10div5 = $div10 / 5

# Indexes start at 1 in csh
@ idx_ell = $mod10 + 1
@ idx_mode = $div10div5 + 1
@ idx_Ndrt = $div10mod5 + 1
	
#echo $idx_ell $idx_mode $idx_Ndrt
echo "Starting Simulation with ell =" ${ells[$idx_ell]} ",mode =" $modes[$idx_mode] "and N_drt =" ${Ndrt[$idx_Ndrt]}
#end

../../../../jdk-11.0.2/bin/java -Xmx8g -jar ../../../../matsim-bimodal-varyl-1.0-SNAPSHOT-jar-with-dependencies.jar config.xml ${ells[$idx_ell]} ${Ndrt[$idx_Ndrt]} ${modes[$idx_mode]}
