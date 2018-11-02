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

# mw 1 details
export mw1_dns="fmorath@storeoipsjti6wookcsshpublicip4.westeurope.cloudapp.azure.com"
export mw1_ip="10.0.0.9"
export mw1_port=16379

# mw 2 details
export mw2_dns="fmorath@storeoipsjti6wookcsshpublicip4.westeurope.cloudapp.azure.com"


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

    local test_time=10 #60; # ca. 1k ops per second (we have 10k keys)
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
    echo "start killing instances ..."

    ssh $server1_dns "sudo service memcached stop; sudo pkill -f memcached" 
    ssh $mw1_dns "sudo pkill -f middleware"

    # remove jar file
    ssh $mw1_dns "rm *.jar"

    echo "start killing instances finished"
}

function compile_uplaod_mw {
    echo "start compile_uplaod_mw ..."

    # compile
    cd $HOME/Desktop/ASL_project/
    ant

    # upload
    scp "$HOME/Desktop/ASL_project/dist/middleware-fmorath.jar" $mw1_dns:
    scp "$HOME/Desktop/ASL_project/scripts/2_BaselineWithMW/aggregate_mw_data.py" $mw1_dns:

    echo "start compile_uplaod_mw finished"
}


function run_baseline_with_one_mw {
    echo "run run_baseline_with_one_mw ..."

    # log folder setup
    local timestamp=$(date +%Y-%m-%d_%Hh%M)
    mkdir -p "$HOME/Desktop/ASL_project/logs/2_BaselineWithMW/one_mw/$timestamp"

    # params
    local test_time=5 #90;
    local threads=2 # thread count (CT)
    local ratio_list=(1:0 0:1)
    local vc_list=(2 4) #(2 4 8 16 24 32 40 48 56) # virtual clients per thread (VC)
    local rep_list=(1 2) #(1 2 3)
    local worker_list=(8 16) #(8 16 32 64)

    for vc in "${vc_list[@]}"; do
        for ratio in "${ratio_list[@]}"; do
            for worker in "${worker_list[@]}"; do

                for rep in "${rep_list[@]}"; do

                        echo "lunch ratio_${ratio}_vc_${vc}_worker_${worker}_rep_${rep} run"

                        file_ext="ratio_${ratio}_vc_${vc}_worker_${worker}_rep_${rep}"

                        # middleware 
                        ssh $mw1_dns "java -jar middleware-fmorath.jar -l $mw1_ip -p $mw1_port -m ${server1_ip}:${server1_port} -t $worker -s false &> /dev/null &" &
                        sleep 2

                        # memtier
                        ssh $client1_dns "./memtier_benchmark-master/memtier_benchmark -s $mw1_ip -p $mw1_port \
                        --protocol=memcache_text --ratio=$ratio --expiry-range=9999-10000 --key-maximum=10000 --hide-histogram \
                        --clients=$vc --threads=$threads --test-time=$test_time --data-size=4096 --json-out-file=client1_${file_ext}.json &> /dev/null &" &  

                        ssh $client2_dns "./memtier_benchmark-master/memtier_benchmark -s $mw1_ip -p $mw1_port \
                        --protocol=memcache_text --ratio=$ratio --expiry-range=9999-10000 --key-maximum=10000 --hide-histogram \
                        --clients=$vc --threads=$threads --test-time=$test_time --data-size=4096 --json-out-file=client2_${file_ext}.json &> /dev/null &" & 

                        ssh $client3_dns "./memtier_benchmark-master/memtier_benchmark -s $mw1_ip -p $mw1_port \
                        --protocol=memcache_text --ratio=$ratio --expiry-range=9999-10000 --key-maximum=10000 --hide-histogram \
                        --clients=$vc --threads=$threads --test-time=$test_time --data-size=4096 --json-out-file=client3_${file_ext}.json &> /dev/null &" & 

                        # dstat: cpu, net usage statistics           
                        ssh $client1_dns "dstat -c -n --output dstat_client1_${file_ext}.csv -T 1 $test_time &> /dev/null &" &
                        ssh $mw1_dns "dstat -c -n -d --output dstat_mw1_${file_ext}.csv -T 1 $test_time &> /dev/null &" &
                        ssh $server1_dns "dstat -c -n --output dstat_server1_${file_ext}.csv -T 1 $test_time &> /dev/null &" &

                        # wait until experiments are finished
                        sleep $(($test_time + 5))

                        # kill middleware
                        ssh $mw1_dns "sudo pkill -f middleware"
                        echo "killed mw"
                        sleep 5

                        # run python script to aggregate data
                        echo "aggregate mw data ..."
                        ssh $mw1_dns "python3 aggregate_mw_data.py mw.csv mw1_${file_ext}.csv"
                                       
                        # copy data to local file system and delete on vm
                        echo "copy collected data to local FS ..."
                        scp $client1_dns:client* "$HOME/Desktop/ASL_project/logs/2_BaselineWithMW/one_mw/$timestamp"
                        scp $client1_dns:dstat* "$HOME/Desktop/ASL_project/logs/2_BaselineWithMW/one_mw/$timestamp"
                        scp $client2_dns:client* "$HOME/Desktop/ASL_project/logs/2_BaselineWithMW/one_mw/$timestamp"
                        scp $client3_dns:client* "$HOME/Desktop/ASL_project/logs/2_BaselineWithMW/one_mw/$timestamp"

                        scp $mw1_dns:mw1* "$HOME/Desktop/ASL_project/logs/2_BaselineWithMW/one_mw/$timestamp"
                        scp $mw1_dns:dstat* "$HOME/Desktop/ASL_project/logs/2_BaselineWithMW/one_mw/$timestamp"

                        scp $server1_dns:dstat* "$HOME/Desktop/ASL_project/logs/2_BaselineWithMW/one_mw/$timestamp"

                        ssh $client1_dns "rm *.json; rm *.csv"
                        ssh $client2_dns "rm *.json"
                        ssh $client3_dns "rm *.json"
                        ssh $mw1_dns "rm *.csv"
                        ssh $server1_dns "rm *.csv"
                done

            done
        done
    done
            
    echo "run run_baseline_with_one_mw finished"
} 



if [ "${1}" == "run" ]; then

   # compile and upload mw
   compile_uplaod_mw 

   # start memcached servers
   start_memcached_servers

   # populate memcached servers with key-value pairs
   populate_memcached_servers

   # run experiment 
   run_baseline_with_one_mw 

   # kill instances
   kill_instances
fi