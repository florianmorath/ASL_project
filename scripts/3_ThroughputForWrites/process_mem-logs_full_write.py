#!/usr/bin/env python3

""" 
Process log files to csv files such that extracted values can easily be plotted.

output:
- full_write_mem_tp.csv: thorughput for write loads
- full_write_mem_rt.csv: response-time for write loads

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
    os.makedirs('processed_data/{}'.format(date), exist_ok=True)

    # experiment config
    ratio_list=['1:0']
    vc_list=[1,32] #[1,4,8,16,24,32,48] 
    worker_list=[64] #[8,16,32,64]
    rep_list=[1,2] #[1,2,3]

    # create csv files (one csv file contains all data that will be plotted in one plot)
    tp_file = open("processed_data/{}/full_write_mem_tp.csv".format(date), "w") # throughput
    rt_file  = open("processed_data/{}/full_write_mem_rt.csv".format(date), "w") # response-time
    tp_file.write("client,worker,write_tp_mean,write_tp_std\n")
    rt_file.write("client,worker,write_rt_mean,write_rt_std\n")

    # extract and compute values
    for vc in vc_list:
        for worker in worker_list:
            write_tp_list = []
            write_rt_list = []

            for rep in rep_list:

                # want the sum of the tp and the mean of the rt
                temp_write_tp_list = []
                temp_write_rt_list = []

                files = glob("{}/client*_*_ratio_*_vc_{}_worker_{}_rep_{}.json".format(log_dir, vc, worker, rep))
                assert len(files) == 6
                for f in files:
                    js = json.load(open(f))

                    if js["configuration"]["ratio"] == "1:0":
                        # set requests
                        temp_write_tp_list.append(js["ALL STATS"]["Sets"]["Ops/sec"])
                        temp_write_rt_list.append(js["ALL STATS"]["Sets"]["Latency"])
                   
                
                # put sum for tp and mean for rt into final lists
                write_tp_list.append(np.sum(temp_write_tp_list))
                write_rt_list.append(np.mean(temp_write_rt_list))

            
            tp_file.write('{},{},{},{}\n'.format(2*3*vc, worker, np.mean(write_tp_list), np.std(write_tp_list)))
            rt_file.write('{},{},{},{}\n'.format(2*3*vc, worker, np.mean(write_rt_list), np.std(write_rt_list)))
