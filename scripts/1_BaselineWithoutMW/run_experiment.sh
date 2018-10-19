#!/bin/bash

# server 1 details
export server1_dns="fmorath@storeoipsjti6wookcsshpublicip6.westeurope.cloudapp.azure.com"
export server1_ip="10.0.0.7"
export server1_port=11211

# client 1 details
export client1_dns="fmorath@storeoipsjti6wookcsshpublicip1.westeurope.cloudapp.azure.com"


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

    local test_time=10;
    local ratio="1:0"; # set requests
    local threads=1; # thread count (CT)
    local clients=1; # virtual clients per thread (VC)

    ssh $client1_dns "./memtier_benchmark-master/memtier_benchmark -s $server1_ip -p $server1_port --protocol=memcache_text --ratio=$ratio --expiry-range=9999-10000 --key-maximum=10000 --hide-histogram --clients=$clients --threads=$threads --test-time=$test_time --data-size=4096"    
    
    # wait a little (cool down)
    sleep 5

    echo "start populating memcached servers finished"
}

function kill_instances {
    ssh $server1_dns "sudo service memcached stop; sudo pkill -f memcached" 
}



function run_baseline_without_mw {
    echo "run baseline_without_mw ..."

} 


if [ "${1}" == "run" ]; then

   # start memcached servers
   start_memcached_servers

   # populate memcached servers with key-value pairs
   populate_memcached_servers

   # run experiment
   # kill instances
   kill_instances
fi