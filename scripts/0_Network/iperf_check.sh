#!/bin/bash

# simple outbound network bandwidth test with iperf

# server 1 details
export server1_dns="fmorath@storeoipsjti6wookcsshpublicip6.westeurope.cloudapp.azure.com"
export server1_ip="10.0.0.7"

# client 1 details
export client1_dns="fmorath@storeoipsjti6wookcsshpublicip1.westeurope.cloudapp.azure.com"
export client1_ip="10.0.0.11"

# mw 1 details
export mw1_dns="fmorath@storeoipsjti6wookcsshpublicip4.westeurope.cloudapp.azure.com"
export mw1_ip="10.0.0.9"

# run net bandwidth check for client vm
ssh $server1_dns "iperf -s &"&
sleep 2
ssh $client1_dns "iperf -c $server1_ip > client_net.log"

# kill instances
ssh $server1_dns "pkill -f iperf"
ssh $client1_dns "pkill -f iperf"

# run net bandwidth check for server vm
ssh $client1_dns "iperf -s &"&
sleep 2
ssh $server1_dns "iperf -c $client1_ip > server_net.log"

# kill instances
ssh $server1_dns "pkill -f iperf"
ssh $client1_dns "pkill -f iperf"

# run net bandwidth check for middleware vm
ssh $client1_dns "iperf -s &"&
sleep 2
ssh $mw1_dns "iperf -c $client1_ip > mw_net.log"

# kill instances
ssh $mw1_dns "pkill -f iperf"
ssh $client1_dns "pkill -f iperf"
