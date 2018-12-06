#!/bin/bash

# server 1 details
export server1_dns="fmorath@storeoipsjti6wookcsshpublicip6.westeurope.cloudapp.azure.com"
export server1_ip="10.0.0.7"
export server1_port=11211

# server 2 details
export server2_dns="fmorath@storeoipsjti6wookcsshpublicip7.westeurope.cloudapp.azure.com"
export server2_ip="10.0.0.10"
export server2_port=11211

# client 1 details
export client1_dns="fmorath@storeoipsjti6wookcsshpublicip1.westeurope.cloudapp.azure.com"

# client 2 details
export client2_dns="fmorath@storeoipsjti6wookcsshpublicip2.westeurope.cloudapp.azure.com"

# client 3 details
export client3_dns="fmorath@storeoipsjti6wookcsshpublicip3.westeurope.cloudapp.azure.com"


function start_memcached_servers {
    echo "start memcached servers ..."

    # stop memcached instance automatically started at startup then start memcached instance in background
    ssh $server1_dns "sudo service memcached stop; memcached -p $server1_port -t 1 &" &
    ssh $server2_dns "sudo service memcached stop; memcached -p $server2_port -t 1 &" &

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

    ssh $client1_dns "./memtier_benchmark-master/memtier_benchmark -s $server2_ip -p $server2_port -n allkeys \
    --protocol=memcache_text --ratio=$ratio --expiry-range=99999-100000 --key-maximum=10000 --hide-histogram \
    --data-size=4096 --key-pattern=S:S" 

    echo "start populating memcached servers finished"
}

function kill_instances_before_experiment {

    # kill instances (may still run)
    ssh $server1_dns "sudo pkill -f memcached" 
    ssh $server2_dns "sudo pkill -f memcached" 
}

function kill_instances {
    echo "start killing memcached instances ..."

    ssh $server1_dns "sudo service memcached stop; sudo pkill -f memcached" 
    ssh $server2_dns "sudo service memcached stop; sudo pkill -f memcached" 

    echo "start killing memcached instances finished"
}


function compile_uplaod_mw {
    echo "start compile_uplaod_mw ..."

    scp "$HOME/Desktop/ASL_project/scripts/1_BaselineWithoutMW/aggregate_mem_data.py" $client1_dns:

    echo "start compile_uplaod_mw finished"
}


function run_baseline_without_mw_two_servers {
    echo "run baseline_without_mw_two_servers ..."

    # log folder setup
    local timestamp=$(date +%Y-%m-%d_%Hh%M)
    mkdir -p "$HOME/Desktop/ASL_project/logs/1_BaselineWithoutMW/two_servers/$timestamp"

    # params
    local test_time=80;
    local threads=1 # thread count (CT)
    local ratio_list=(0:1 1:0)
    local vc_list=(1 4 8 16 24 32 40) # virtual clients per thread (VC)
    local rep_list=(1 2 3)

    for ratio in "${ratio_list[@]}"; do
        for vc in "${vc_list[@]}"; do
            for rep in "${rep_list[@]}"; do

                    echo "lunch ratio_${ratio}_vc_${vc}_rep_${rep} run"

                    file_ext="ratio_${ratio}_vc_${vc}_rep_${rep}"

                    # memtier
                    ssh $client1_dns "./memtier_benchmark-master/memtier_benchmark -s $server1_ip -p $server1_port \
                    --protocol=memcache_text --ratio=$ratio --expiry-range=99999-100000 --key-maximum=10000 --hide-histogram \
                    --clients=$vc --threads=$threads --test-time=$test_time --data-size=4096 --json-out-file=client1_${file_ext}_mem.json &> ${file_ext}_1.log &" &  

                    ssh $client1_dns "./memtier_benchmark-master/memtier_benchmark -s $server2_ip -p $server2_port \
                    --protocol=memcache_text --ratio=$ratio --expiry-range=99999-100000 --key-maximum=10000 --hide-histogram \
                    --clients=$vc --threads=$threads --test-time=$test_time --data-size=4096 --json-out-file=client2_${file_ext}_mem.json &> ${file_ext}_2.log &" & 
        
                    # dstat: cpu, net usage statistics           
                    ssh $client1_dns "dstat -c -n --output dstat_client1_${file_ext}.csv -T 1 $test_time &> /dev/null &" &
                    ssh $server1_dns "dstat -c -n --output dstat_server1_${file_ext}.csv -T 1 $test_time &> /dev/null &" &

                    # wait until experiments are finished
                    sleep $(($test_time + 5))

                     # run python script to aggregate data
                    echo "aggregate mem data ..."
                    ssh $client1_dns "python3 aggregate_mem_data.py ${file_ext}_1.log client1_${file_ext}.json"
                    ssh $client1_dns "python3 aggregate_mem_data.py ${file_ext}_2.log client2_${file_ext}.json"

                    # copy data to local file system and delete on vm
                    echo "copy collected data to local FS ..."
                    scp $client1_dns:client* "$HOME/Desktop/ASL_project/logs/1_BaselineWithoutMW/two_servers/$timestamp"
                    scp $client1_dns:dstat* "$HOME/Desktop/ASL_project/logs/1_BaselineWithoutMW/two_servers/$timestamp"
                    scp $server1_dns:dstat* "$HOME/Desktop/ASL_project/logs/1_BaselineWithoutMW/two_servers/$timestamp"

                    ssh $client1_dns "rm *.json; rm *.csv; rm *.log"
                    ssh $server1_dns "rm *.csv"
            done

        done
    done
            
    echo "run baseline_without_mw_two_servers finished"
} 

if [ "${1}" == "run" ]; then


    # kill instances that may still run
   kill_instances_before_experiment

   # compile and upload mw
   compile_uplaod_mw

   # start memcached servers
   start_memcached_servers

   # populate servers 
   populate_memcached_servers

   # run experiment (two servers) 
   run_baseline_without_mw_two_servers

   # kill instances
   kill_instances

fi