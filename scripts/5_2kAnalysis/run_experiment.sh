#!/bin/bash

# gmail-account

# server 1 details
export server1_dns="fmorath@storezbhg5w3nih76usshpublicip6.westeurope.cloudapp.azure.com"
export server1_ip="10.0.0.6"
export server1_port=11211

# server 2 details
export server2_dns="fmorath@storezbhg5w3nih76usshpublicip7.westeurope.cloudapp.azure.com"
export server2_ip="10.0.0.10"
export server2_port=11211

# server 3 details
export server3_dns="fmorath@storezbhg5w3nih76usshpublicip8.westeurope.cloudapp.azure.com"
export server3_ip="10.0.0.7"
export server3_port=11211

# client 1 details
export client1_dns="fmorath@storezbhg5w3nih76usshpublicip1.westeurope.cloudapp.azure.com"
export client1_ip="10.0.0.8"

# client 2 details
export client2_dns="fmorath@storezbhg5w3nih76usshpublicip2.westeurope.cloudapp.azure.com"
export client2_ip="10.0.0.4"

# client 3 details
export client3_dns="fmorath@storezbhg5w3nih76usshpublicip3.westeurope.cloudapp.azure.com"
export client3_ip="10.0.0.9"

# mw 1 details
export mw1_dns="fmorath@storezbhg5w3nih76usshpublicip4.westeurope.cloudapp.azure.com"
export mw1_ip="10.0.0.11"
export mw1_port=16379

# mw 2 details
export mw2_dns="fmorath@storezbhg5w3nih76usshpublicip5.westeurope.cloudapp.azure.com"
export mw2_ip="10.0.0.5"
export mw2_port=16379


function start_memcached_servers {
    echo "start memcached servers ..."

    # stop memcached instance automatically started at startup then start memcached instance in background
    ssh $server1_dns "sudo service memcached stop; memcached -p $server1_port -t 1 &" &
    ssh $server2_dns "sudo service memcached stop; memcached -p $server2_port -t 1 &" &
    ssh $server3_dns "sudo service memcached stop; memcached -p $server3_port -t 1 &" &

    # sleep to be sure memcached servers started completely
    sleep 2

    echo "start memcached servers finished"
}

function populate_memcached_servers {
    echo "start populating memcached servers ..."

    local ratio="1:0"; # set requests

    ssh $client1_dns "./memtier_benchmark-master/memtier_benchmark -s $server1_ip -p $server1_port -n allkeys \
    --protocol=memcache_text --ratio=$ratio --expiry-range=99999-100000 --key-maximum=10000 --hide-histogram \
    --data-size=4096 --key-pattern=S:S"  

    ssh $client2_dns "./memtier_benchmark-master/memtier_benchmark -s $server2_ip -p $server2_port -n allkeys \
    --protocol=memcache_text --ratio=$ratio --expiry-range=99999-100000 --key-maximum=10000 --hide-histogram \
    --data-size=4096 --key-pattern=S:S" 

    ssh $client3_dns "./memtier_benchmark-master/memtier_benchmark -s $server3_ip -p $server3_port -n allkeys \
    --protocol=memcache_text --ratio=$ratio --expiry-range=99999-100000 --key-maximum=10000 --hide-histogram \
    --data-size=4096 --key-pattern=S:S" 

    echo "start populating memcached servers finished"
}

function kill_instances_before_experiment {

    # kill instances (may still run)
    ssh $server1_dns "sudo pkill -f memcached" 
    ssh $server2_dns "sudo pkill -f memcached" 
    ssh $server3_dns "sudo pkill -f memcached" 
    ssh $mw1_dns "sudo pkill -f middleware"
    ssh $mw2_dns "sudo pkill -f middleware"
}

function kill_instances {
    echo "start killing instances ..."

    ssh $server1_dns "sudo service memcached stop; sudo pkill -f memcached" 
    ssh $server2_dns "sudo service memcached stop; sudo pkill -f memcached" 
    ssh $server3_dns "sudo service memcached stop; sudo pkill -f memcached" 
    ssh $mw1_dns "sudo pkill -f middleware"
    ssh $mw2_dns "sudo pkill -f middleware"

    echo "start killing instances finished"
}

function deallocate_vms {
    echo "start deallocating vms ..."

     # deallocate azure vms
    az vm deallocate --resource-group ASL_project --name Client1
    az vm deallocate --resource-group ASL_project --name Client2
    az vm deallocate --resource-group ASL_project --name Client3
    az vm deallocate --resource-group ASL_project --name Server1
    az vm deallocate --resource-group ASL_project --name Server2
    az vm deallocate --resource-group ASL_project --name Server3
    az vm deallocate --resource-group ASL_project --name Middleware1
    az vm deallocate --resource-group ASL_project --name Middleware2

    echo "start deallocating vms finished"
}


function compile_uplaod_mw {
    echo "start compile_uplaod_mw ..."

    # compile
    cd $HOME/Desktop/ASL_project/
    ant

    # upload
    scp "$HOME/Desktop/ASL_project/dist/middleware-fmorath.jar" $mw1_dns:
    scp "$HOME/Desktop/ASL_project/dist/middleware-fmorath.jar" $mw2_dns:

    scp "$HOME/Desktop/ASL_project/scripts/5_2kAnalysis/aggregate_mem_data.py" $client1_dns:
    scp "$HOME/Desktop/ASL_project/scripts/5_2kAnalysis/aggregate_mem_data.py" $client2_dns:
    scp "$HOME/Desktop/ASL_project/scripts/5_2kAnalysis/aggregate_mem_data.py" $client3_dns:

    echo "start compile_uplaod_mw finished"
}


function run_experiment {
    echo "run run_experiment ..."

    # time-stamp
    local timestamp=$(date +%Y-%m-%d_%Hh%M)
    mkdir -p "$HOME/Desktop/ASL_project/logs/5_2kAnalysis/$timestamp"

    # params
    local test_time=90
    local ratio_list=(0:1 1:0) 
    local vc_list=(32) # virtual clients per thread (VC)
    local rep_list=(1 2 3)
    local worker_list=(8 32)

    ## mw_1_s_1
    echo "start mw_1_s_1"

    mkdir -p "$HOME/Desktop/ASL_project/logs/5_2kAnalysis/$timestamp/mw_1_s_1"

    local threads=2 # thread count (CT)

    for vc in "${vc_list[@]}"; do
        for ratio in "${ratio_list[@]}"; do
            for worker in "${worker_list[@]}"; do

                for rep in "${rep_list[@]}"; do

                        echo "lunch ratio_${ratio}_vc_${vc}_worker_${worker}_rep_${rep} run"

                        file_ext="ratio_${ratio}_vc_${vc}_worker_${worker}_rep_${rep}"

                        # middleware 
                        ssh $mw1_dns "java -jar middleware-fmorath.jar -l $mw1_ip -p $mw1_port -m ${server1_ip}:${server1_port} -t $worker -s false &> /dev/null &" &

                        sleep 2

                        # memtier connection to mw1
                        ssh $client1_dns "./memtier_benchmark-master/memtier_benchmark -s $mw1_ip -p $mw1_port \
                        --protocol=memcache_text --ratio=$ratio --expiry-range=99999-100000 --key-maximum=10000 --hide-histogram \
                        --clients=$vc --threads=$threads --test-time=$test_time --data-size=4096 --json-out-file=client1_${file_ext}_mem.json &> ${file_ext}.log &" &  

                        ssh $client2_dns "./memtier_benchmark-master/memtier_benchmark -s $mw1_ip -p $mw1_port \
                        --protocol=memcache_text --ratio=$ratio --expiry-range=99999-100000 --key-maximum=10000 --hide-histogram \
                        --clients=$vc --threads=$threads --test-time=$test_time --data-size=4096 --json-out-file=client2_${file_ext}_mem.json &> ${file_ext}.log &" & 

                        ssh $client3_dns "./memtier_benchmark-master/memtier_benchmark -s $mw1_ip -p $mw1_port \
                        --protocol=memcache_text --ratio=$ratio --expiry-range=99999-100000 --key-maximum=10000 --hide-histogram \
                        --clients=$vc --threads=$threads --test-time=$test_time --data-size=4096 --json-out-file=client3_${file_ext}_mem.json &> ${file_ext}.log &" & 

                        # wait until experiments are finished
                        sleep $(($test_time + 5))

                        # kill middleware
                        ssh $mw1_dns "sudo pkill -f middleware"
                        echo "killed mw"
                        sleep 5

                        # run python script to aggregate data
                        echo "aggregate mem data ..."
                        ssh $client1_dns "python3 aggregate_mem_data.py ${file_ext}.log client1_${file_ext}.json"
                        ssh $client2_dns "python3 aggregate_mem_data.py ${file_ext}.log client2_${file_ext}.json"
                        ssh $client3_dns "python3 aggregate_mem_data.py ${file_ext}.log client3_${file_ext}.json"

                        # copy data to local file system and delete on vm
                        echo "copy collected data to local FS ..."
                        scp $client1_dns:client* "$HOME/Desktop/ASL_project/logs/5_2kAnalysis/$timestamp/mw_1_s_1"
                        scp $client2_dns:client* "$HOME/Desktop/ASL_project/logs/5_2kAnalysis/$timestamp/mw_1_s_1"
                        scp $client3_dns:client* "$HOME/Desktop/ASL_project/logs/5_2kAnalysis/$timestamp/mw_1_s_1"

                        ssh $client1_dns "rm *.json; rm *.log"
                        ssh $client2_dns "rm *.json; rm *.log"
                        ssh $client3_dns "rm *.json; rm *.log"
                        ssh $mw1_dns "rm *.csv"
                done

            done
        done
    done

    ## mw_1_s_3
    echo "start mw_1_s_3"

    mkdir -p "$HOME/Desktop/ASL_project/logs/5_2kAnalysis/$timestamp/mw_1_s_3"

    local threads=2 # thread count (CT)

    for vc in "${vc_list[@]}"; do
        for ratio in "${ratio_list[@]}"; do
            for worker in "${worker_list[@]}"; do

                for rep in "${rep_list[@]}"; do

                        echo "lunch ratio_${ratio}_vc_${vc}_worker_${worker}_rep_${rep} run"

                        file_ext="ratio_${ratio}_vc_${vc}_worker_${worker}_rep_${rep}"

                        # middleware 
                        ssh $mw1_dns "java -jar middleware-fmorath.jar -l $mw1_ip -p $mw1_port -m ${server1_ip}:${server1_port} ${server2_ip}:${server2_port} ${server3_ip}:${server3_port} \
                        -t $worker -s false &> /dev/null &" &

                        sleep 2

                        # memtier connection to mw1
                        ssh $client1_dns "./memtier_benchmark-master/memtier_benchmark -s $mw1_ip -p $mw1_port \
                        --protocol=memcache_text --ratio=$ratio --expiry-range=99999-100000 --key-maximum=10000 --hide-histogram \
                        --clients=$vc --threads=$threads --test-time=$test_time --data-size=4096 --json-out-file=client1_${file_ext}_mem.json &> ${file_ext}.log &" &  

                        ssh $client2_dns "./memtier_benchmark-master/memtier_benchmark -s $mw1_ip -p $mw1_port \
                        --protocol=memcache_text --ratio=$ratio --expiry-range=99999-100000 --key-maximum=10000 --hide-histogram \
                        --clients=$vc --threads=$threads --test-time=$test_time --data-size=4096 --json-out-file=client2_${file_ext}_mem.json &> ${file_ext}.log &" & 

                        ssh $client3_dns "./memtier_benchmark-master/memtier_benchmark -s $mw1_ip -p $mw1_port \
                        --protocol=memcache_text --ratio=$ratio --expiry-range=99999-100000 --key-maximum=10000 --hide-histogram \
                        --clients=$vc --threads=$threads --test-time=$test_time --data-size=4096 --json-out-file=client3_${file_ext}_mem.json &> ${file_ext}.log &" & 

                        # wait until experiments are finished
                        sleep $(($test_time + 5))

                        # kill middleware
                        ssh $mw1_dns "sudo pkill -f middleware"
                        echo "killed mw"
                        sleep 5

                        # run python script to aggregate data
                        echo "aggregate mem data ..."
                        ssh $client1_dns "python3 aggregate_mem_data.py ${file_ext}.log client1_${file_ext}.json"
                        ssh $client2_dns "python3 aggregate_mem_data.py ${file_ext}.log client2_${file_ext}.json"
                        ssh $client3_dns "python3 aggregate_mem_data.py ${file_ext}.log client3_${file_ext}.json"

                        # copy data to local file system and delete on vm
                        echo "copy collected data to local FS ..."
                        scp $client1_dns:client* "$HOME/Desktop/ASL_project/logs/5_2kAnalysis/$timestamp/mw_1_s_3"
                        scp $client2_dns:client* "$HOME/Desktop/ASL_project/logs/5_2kAnalysis/$timestamp/mw_1_s_3"
                        scp $client3_dns:client* "$HOME/Desktop/ASL_project/logs/5_2kAnalysis/$timestamp/mw_1_s_3"

                        ssh $client1_dns "rm *.json; rm *.log"
                        ssh $client2_dns "rm *.json; rm *.log"
                        ssh $client3_dns "rm *.json; rm *.log"
                        ssh $mw1_dns "rm *.csv"
                done

            done
        done
    done

      
    echo "start mw_2_s_1"

    mkdir -p "$HOME/Desktop/ASL_project/logs/5_2kAnalysis/$timestamp/mw_2_s_1"

    local threads=1 # thread count (CT)

    for vc in "${vc_list[@]}"; do
        for ratio in "${ratio_list[@]}"; do
            for worker in "${worker_list[@]}"; do

                for rep in "${rep_list[@]}"; do

                        echo "lunch ratio_${ratio}_vc_${vc}_worker_${worker}_rep_${rep} run"

                        file_ext="ratio_${ratio}_vc_${vc}_worker_${worker}_rep_${rep}"

                        # middleware 
                        ssh $mw1_dns "java -jar middleware-fmorath.jar -l $mw1_ip -p $mw1_port -m ${server1_ip}:${server1_port} -t $worker -s false &> /dev/null &" &
                        ssh $mw2_dns "java -jar middleware-fmorath.jar -l $mw2_ip -p $mw2_port -m ${server1_ip}:${server1_port} -t $worker -s false &> /dev/null &" &

                        sleep 2

                         # memtier connection to mw1
                        ssh $client1_dns "./memtier_benchmark-master/memtier_benchmark -s $mw1_ip -p $mw1_port \
                        --protocol=memcache_text --ratio=$ratio --expiry-range=99999-100000 --key-maximum=10000 --hide-histogram \
                        --clients=$vc --threads=$threads --test-time=$test_time --data-size=4096 --json-out-file=client1_1_${file_ext}_mem.json &> ${file_ext}_1.log &" &  

                        ssh $client2_dns "./memtier_benchmark-master/memtier_benchmark -s $mw1_ip -p $mw1_port \
                        --protocol=memcache_text --ratio=$ratio --expiry-range=99999-100000 --key-maximum=10000 --hide-histogram \
                        --clients=$vc --threads=$threads --test-time=$test_time --data-size=4096 --json-out-file=client2_1_${file_ext}_mem.json &> ${file_ext}_1.log &" & 

                        ssh $client3_dns "./memtier_benchmark-master/memtier_benchmark -s $mw1_ip -p $mw1_port \
                        --protocol=memcache_text --ratio=$ratio --expiry-range=99999-100000 --key-maximum=10000 --hide-histogram \
                        --clients=$vc --threads=$threads --test-time=$test_time --data-size=4096 --json-out-file=client3_1_${file_ext}_mem.json &> ${file_ext}_1.log &" & 

                        # memtier connection to mw2
                        ssh $client1_dns "./memtier_benchmark-master/memtier_benchmark -s $mw2_ip -p $mw2_port \
                        --protocol=memcache_text --ratio=$ratio --expiry-range=99999-100000 --key-maximum=10000 --hide-histogram \
                        --clients=$vc --threads=$threads --test-time=$test_time --data-size=4096 --json-out-file=client1_2_${file_ext}_mem.json &> ${file_ext}_2.log &" &  

                        ssh $client2_dns "./memtier_benchmark-master/memtier_benchmark -s $mw2_ip -p $mw2_port \
                        --protocol=memcache_text --ratio=$ratio --expiry-range=99999-100000 --key-maximum=10000 --hide-histogram \
                        --clients=$vc --threads=$threads --test-time=$test_time --data-size=4096 --json-out-file=client2_2_${file_ext}_mem.json &> ${file_ext}_2.log &" & 

                        ssh $client3_dns "./memtier_benchmark-master/memtier_benchmark -s $mw2_ip -p $mw2_port \
                        --protocol=memcache_text --ratio=$ratio --expiry-range=99999-100000 --key-maximum=10000 --hide-histogram \
                        --clients=$vc --threads=$threads --test-time=$test_time --data-size=4096 --json-out-file=client3_2_${file_ext}_mem.json &> ${file_ext}_2.log &" & 

                        # wait until experiments are finished
                        sleep $(($test_time + 5))

                        # kill middleware
                        ssh $mw1_dns "sudo pkill -f middleware"
                        ssh $mw2_dns "sudo pkill -f middleware"

                        echo "killed mw"
                        sleep 5

                        # run python script to aggregate data
                        echo "aggregate mem data ..."
                        ssh $client1_dns "python3 aggregate_mem_data.py ${file_ext}_1.log client1_1_${file_ext}.json"
                        ssh $client2_dns "python3 aggregate_mem_data.py ${file_ext}_1.log client2_1_${file_ext}.json"
                        ssh $client3_dns "python3 aggregate_mem_data.py ${file_ext}_1.log client3_1_${file_ext}.json"

                        ssh $client1_dns "python3 aggregate_mem_data.py ${file_ext}_2.log client1_2_${file_ext}.json"
                        ssh $client2_dns "python3 aggregate_mem_data.py ${file_ext}_2.log client2_2_${file_ext}.json"
                        ssh $client3_dns "python3 aggregate_mem_data.py ${file_ext}_2.log client3_2_${file_ext}.json"

                        # copy data to local file system and delete on vm
                        echo "copy collected data to local FS ..."
                        scp $client1_dns:client* "$HOME/Desktop/ASL_project/logs/5_2kAnalysis/$timestamp/mw_2_s_1"
                        scp $client2_dns:client* "$HOME/Desktop/ASL_project/logs/5_2kAnalysis/$timestamp/mw_2_s_1"
                        scp $client3_dns:client* "$HOME/Desktop/ASL_project/logs/5_2kAnalysis/$timestamp/mw_2_s_1"

                        ssh $client1_dns "rm *.json; rm *.log"
                        ssh $client2_dns "rm *.json; rm *.log"
                        ssh $client3_dns "rm *.json; rm *.log"
                        ssh $mw1_dns "rm *.csv"
                        ssh $mw2_dns "rm *.csv"

                done

            done
        done
    done

    echo "start mw_2_s_3"

    mkdir -p "$HOME/Desktop/ASL_project/logs/5_2kAnalysis/$timestamp/mw_2_s_3"

    local threads=1 # thread count (CT)

    for vc in "${vc_list[@]}"; do
        for ratio in "${ratio_list[@]}"; do
            for worker in "${worker_list[@]}"; do

                for rep in "${rep_list[@]}"; do

                        echo "lunch ratio_${ratio}_vc_${vc}_worker_${worker}_rep_${rep} run"

                        file_ext="ratio_${ratio}_vc_${vc}_worker_${worker}_rep_${rep}"

                        # middleware 
                        ssh $mw1_dns "java -jar middleware-fmorath.jar -l $mw1_ip -p $mw1_port -m ${server1_ip}:${server1_port} ${server2_ip}:${server2_port} ${server3_ip}:${server3_port} \
                        -t $worker -s false &> /dev/null &" &
                        ssh $mw2_dns "java -jar middleware-fmorath.jar -l $mw2_ip -p $mw2_port -m ${server1_ip}:${server1_port} ${server2_ip}:${server2_port} ${server3_ip}:${server3_port} \
                        -t $worker -s false &> /dev/null &" &

                        sleep 2

                         # memtier connection to mw1
                        ssh $client1_dns "./memtier_benchmark-master/memtier_benchmark -s $mw1_ip -p $mw1_port \
                        --protocol=memcache_text --ratio=$ratio --expiry-range=99999-100000 --key-maximum=10000 --hide-histogram \
                        --clients=$vc --threads=$threads --test-time=$test_time --data-size=4096 --json-out-file=client1_1_${file_ext}_mem.json &> ${file_ext}_1.log &" &  

                        ssh $client2_dns "./memtier_benchmark-master/memtier_benchmark -s $mw1_ip -p $mw1_port \
                        --protocol=memcache_text --ratio=$ratio --expiry-range=99999-100000 --key-maximum=10000 --hide-histogram \
                        --clients=$vc --threads=$threads --test-time=$test_time --data-size=4096 --json-out-file=client2_1_${file_ext}_mem.json &> ${file_ext}_1.log &" & 

                        ssh $client3_dns "./memtier_benchmark-master/memtier_benchmark -s $mw1_ip -p $mw1_port \
                        --protocol=memcache_text --ratio=$ratio --expiry-range=99999-100000 --key-maximum=10000 --hide-histogram \
                        --clients=$vc --threads=$threads --test-time=$test_time --data-size=4096 --json-out-file=client3_1_${file_ext}_mem.json &> ${file_ext}_1.log &" & 

                        # memtier connection to mw2
                        ssh $client1_dns "./memtier_benchmark-master/memtier_benchmark -s $mw2_ip -p $mw2_port \
                        --protocol=memcache_text --ratio=$ratio --expiry-range=99999-100000 --key-maximum=10000 --hide-histogram \
                        --clients=$vc --threads=$threads --test-time=$test_time --data-size=4096 --json-out-file=client1_2_${file_ext}_mem.json &> ${file_ext}_2.log &" &  

                        ssh $client2_dns "./memtier_benchmark-master/memtier_benchmark -s $mw2_ip -p $mw2_port \
                        --protocol=memcache_text --ratio=$ratio --expiry-range=99999-100000 --key-maximum=10000 --hide-histogram \
                        --clients=$vc --threads=$threads --test-time=$test_time --data-size=4096 --json-out-file=client2_2_${file_ext}_mem.json &> ${file_ext}_2.log &" & 

                        ssh $client3_dns "./memtier_benchmark-master/memtier_benchmark -s $mw2_ip -p $mw2_port \
                        --protocol=memcache_text --ratio=$ratio --expiry-range=99999-100000 --key-maximum=10000 --hide-histogram \
                        --clients=$vc --threads=$threads --test-time=$test_time --data-size=4096 --json-out-file=client3_2_${file_ext}_mem.json &> ${file_ext}_2.log &" & 

                        # wait until experiments are finished
                        sleep $(($test_time + 5))

                        # kill middleware
                        ssh $mw1_dns "sudo pkill -f middleware"
                        ssh $mw2_dns "sudo pkill -f middleware"

                        echo "killed mw"
                        sleep 5

                        # run python script to aggregate data
                        echo "aggregate mem data ..."
                        ssh $client1_dns "python3 aggregate_mem_data.py ${file_ext}_1.log client1_1_${file_ext}.json"
                        ssh $client2_dns "python3 aggregate_mem_data.py ${file_ext}_1.log client2_1_${file_ext}.json"
                        ssh $client3_dns "python3 aggregate_mem_data.py ${file_ext}_1.log client3_1_${file_ext}.json"

                        ssh $client1_dns "python3 aggregate_mem_data.py ${file_ext}_2.log client1_2_${file_ext}.json"
                        ssh $client2_dns "python3 aggregate_mem_data.py ${file_ext}_2.log client2_2_${file_ext}.json"
                        ssh $client3_dns "python3 aggregate_mem_data.py ${file_ext}_2.log client3_2_${file_ext}.json"

                        # copy data to local file system and delete on vm
                        echo "copy collected data to local FS ..."
                        scp $client1_dns:client* "$HOME/Desktop/ASL_project/logs/5_2kAnalysis/$timestamp/mw_2_s_3"
                        scp $client2_dns:client* "$HOME/Desktop/ASL_project/logs/5_2kAnalysis/$timestamp/mw_2_s_3"
                        scp $client3_dns:client* "$HOME/Desktop/ASL_project/logs/5_2kAnalysis/$timestamp/mw_2_s_3"

                        ssh $client1_dns "rm *.json; rm *.log"
                        ssh $client2_dns "rm *.json; rm *.log"
                        ssh $client3_dns "rm *.json; rm *.log"
                        ssh $mw1_dns "rm *.csv"
                        ssh $mw2_dns "rm *.csv"

                done

            done
        done
    done
    
            
    echo "run run_experiment finished"
} 



if [ "${1}" == "run" ]; then

   # kill instances that may still run
   kill_instances_before_experiment

   # compile and upload mw
   compile_uplaod_mw

   # start memcached servers
   start_memcached_servers

   # populate memcached servers with key-value pairs
   populate_memcached_servers

   # run experiment
   run_experiment

   # kill instances
   kill_instances

   # deallocate
   #deallocate_vms
fi