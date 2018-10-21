#!/bin/bash

# server 1 details
export server1_dns="fmorath@storeoipsjti6wookcsshpublicip6.westeurope.cloudapp.azure.com"
export server1_ip="10.0.0.7"
export server1_port=11211

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

    # sleep to be sure memcached servers started completely
    sleep 2

    echo "start memcached servers finished"
}

function populate_memcached_servers {
    echo "start populating memcached servers ..."

    local test_time=20; # ca. 1k ops per second (we have 10k keys)
    local ratio="1:0"; # set requests
    local threads=1; # thread count (CT)
    local clients=1; # virtual clients per thread (VC)

    # note: use &> /dev/null to not write output to console 
    ssh $client1_dns "./memtier_benchmark-master/memtier_benchmark -s $server1_ip -p $server1_port \
    --protocol=memcache_text --ratio=$ratio --expiry-range=9999-10000 --key-maximum=10000 --hide-histogram \
    --clients=$clients --threads=$threads --test-time=$test_time --data-size=4096 &> /dev/null &" & 

    # wait a little until finished + cool down
    sleep $(($test_time + 5))

    echo "start populating memcached servers finished"
}

function kill_instances {
    echo "start killing memcached instances ..."

    ssh $server1_dns "sudo service memcached stop; sudo pkill -f memcached" 

    echo "start killing memcached instances finished"
}



function run_baseline_without_mw_one_server {
    echo "run baseline_without_mw ..."

    # log folder setup
    local timestamp=$(date +%Y-%m-%d_%Hh%M)
    mkdir -p "$HOME/Desktop/ASL_project/logs/1_BaselineWithoutMW/one_server/$timestamp"

    # params
    local test_time=10;
    local threads=1 # thread count (CT)
    local ratio_list=(0:1 1:0)
    local vc_list=(2 4) # virtual clients per thread (VC)
    local rep_list=(1 2)

    for vc in "${vc_list[@]}"; do
        for ratio in "${ratio_list[@]}"; do
            for rep in "${rep_list[@]}"; do

                    echo "lunch ratio_${ratio}_vc_${vc}_rep_${rep} run"

                    file_ext="ratio_${ratio}_vc_${vc}_rep_${rep}"

                    # memtier
                    ssh $client1_dns "./memtier_benchmark-master/memtier_benchmark -s $server1_ip -p $server1_port \
                    --protocol=memcache_text --ratio=$ratio --expiry-range=9999-10000 --key-maximum=10000 --hide-histogram \
                    --clients=$vc --threads=$threads --test-time=$test_time --data-size=4096 --json-out-file=client1_${file_ext}.json &> /dev/null &" &  

                    ssh $client2_dns "./memtier_benchmark-master/memtier_benchmark -s $server1_ip -p $server1_port \
                    --protocol=memcache_text --ratio=$ratio --expiry-range=9999-10000 --key-maximum=10000 --hide-histogram \
                    --clients=$vc --threads=$threads --test-time=$test_time --data-size=4096 --json-out-file=client2_${file_ext}.json &> /dev/null &" &   

                    ssh $client3_dns "./memtier_benchmark-master/memtier_benchmark -s $server1_ip -p $server1_port \
                    --protocol=memcache_text --ratio=$ratio --expiry-range=9999-10000 --key-maximum=10000 --hide-histogram \
                    --clients=$vc --threads=$threads --test-time=$test_time --data-size=4096 --json-out-file=client3_${file_ext}.json &> /dev/null &" &   

                    # dstat: cpu, net usage statistics           
                    ssh $client1_dns "dstat -c -n --output dstat_client.csv -T 1 $test_time &> /dev/null &" &
                    ssh $server1_dns "dstat -c -n --output dstat_server.csv -T 1 $test_time &> /dev/null &" &

                    # wait until experiments are finished
                    sleep $(($test_time + 5))
            done

            # copy data to local file system and delete on vm
            echo "copy collected data to local FS ..."
            scp $client1_dns:client* "$HOME/Desktop/ASL_project/logs/1_BaselineWithoutMW/one_server/$timestamp"
            scp $client2_dns:client* "$HOME/Desktop/ASL_project/logs/1_BaselineWithoutMW/one_server/$timestamp"
            scp $client3_dns:client* "$HOME/Desktop/ASL_project/logs/1_BaselineWithoutMW/one_server/$timestamp"
            ssh $client1_dns "rm *.json"
            ssh $client2_dns "rm *.json"
            ssh $client3_dns "rm *.json"

        done
    done
            
    echo "run baseline_without_mw finished"
} 


if [ "${1}" == "run" ]; then

   # start memcached servers
   start_memcached_servers

   # populate memcached servers with key-value pairs
   populate_memcached_servers

   # run experiment (one server)
   run_baseline_without_mw_one_server

   # run experiment (two servers) 
   # TODO

   # kill instances
   kill_instances
fi