#!/bin/bash


export server1_ip="10.0.0.7";
export server1_port="11211";
export server1_dns="fmorath@storeoipsjti6wookcsshpublicip6.westeurope.cloudapp.azure.com";


# start memcached servers
function start_memcached_servers {
    echo "Start memcached servers ..."

    # stop memcached instance automatically started at startup then start memcached instance in background
    ssh $server1_dns "sudo service memcached stop; memcached -p $server1_port -t 1 &" &


    # sleep to be sure memcached servers started completly
    sleep 2

    echo "start memcached servers finished"
}



function run_baseline_without_mw {
    echo "run baseline_without_mw ..."

} 


if [ "${1}" == "run" ]; then

   # start memcached servers
   start_memcached_servers;
   # populate memcached servers with key-value pairs
   # run experiment
   # kill instances
fi