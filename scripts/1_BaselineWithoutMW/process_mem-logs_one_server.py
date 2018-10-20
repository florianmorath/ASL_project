#!/usr/bin/env python3

""" 
Process log files to csv files such that extracted values can easily be plotted.

"""


import os
import sys
from glob import glob
import json
import numpy as np

"""
Args: path to directory containing log-files
 
"""
if __name__ == "__main__":

    # get path to log directory (directory with date as name)
    log_dir = sys.argv[1]
    log_dir = os.path.abspath(log_dir)

    # create dir to put processed csv files into (if does not yet exist)
    date = os.path.basename(log_dir)
    os.makedirs('cvs_processed/one_server/{}'.format(date), exist_ok=True)

    # experiment config
    # TODO: run script can write config params into separate file from which they can be read 
    ratio_list=['0:1','1:0']
    vc_list=[2,4] 
    rep_list=[1,2]

    # create csv files (one csv file contains all data that will be plotted in one plot)
    tp_file = open("cvs_processed/one_server/{}/one_server_mem_tp.csv".format(date), "w") # throughput
    rt_file  = open("cvs_processed/one_server/{}/one_server_mem_rt.csv".format(date), "w") # response-time
    tp_file.write("client,write_tp_mean,write_tp_std,read_tp_mean,read_tp_std\n")
    rt_file.write("client,write_rt_mean,write_rt_std,read_rt_mean,read_rt_std\n")

    # extract and compute values
    for vc in vc_list:
        write_tp_list = []
        read_tp_list = []
        write_rt_list = []
        read_rt_list = []

        files = glob("{}/client*_ratio_*_vc_{}_*.json".format(log_dir, vc))
        for f in files:
            js = json.load(open(f))

            # check that no missed occured
            if (js["ALL STATS"]["Gets"]["Misses/sec"] != 0.0 or js["ALL STATS"]["Sets"]["Misses/sec"] != 0.0 ):
                print("warning: get misses in {}".format(f))

            if js["configuration"]["ratio"] == "1:0":
                # set requests
                write_tp_list.append(js["ALL STATS"]["Sets"]["Ops/sec"])
                write_rt_list.append(js["ALL STATS"]["Sets"]["Latency"])
            elif js["configuration"]["ratio"] == "0:1":
                # read requests
                read_tp_list.append(js["ALL STATS"]["Gets"]["Ops/sec"])
                read_rt_list.append(js["ALL STATS"]["Gets"]["Latency"])
        
        tp_file.write('{},{},{},{},{}\n'.format(2*3*vc, np.mean(write_tp_list), np.std(write_tp_list), np.mean(read_tp_list), np.std(read_tp_list)))
        rt_file.write('{},{},{},{},{}\n'.format(2*3*vc, np.mean(write_rt_list), np.std(write_rt_list), np.mean(read_rt_list), np.std(read_rt_list)))
