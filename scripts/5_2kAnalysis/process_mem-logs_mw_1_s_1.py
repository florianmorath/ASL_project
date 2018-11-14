#!/usr/bin/env python3

""" 
Process log files to csv files such that extracted values can easily be plotted.

output:
- mw_1_s_1_mem_tp.csv: thorughput for read and write loads
- mw_1_s_1_mem_rt.csv: response-time for read and write loads

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
    date = log_dir.split('/')[-2]
    os.makedirs('processed_data/{}'.format(date), exist_ok=True)

    # experiment config
    # TODO: run script can write config params into separate file from which they can be read 
    ratio_list=['0:1','1:0']
    vc_list=[32] 
    worker_list=[8] #[8,32]
    rep_list=[1,2] #[1,2,3]

    # create csv files (one csv file contains all data that will be plotted in one plot)
    tp_file = open("processed_data/{}/mw_1_s_1_mem_tp.csv".format(date), "w") # throughput
    rt_file  = open("processed_data/{}/mw_1_s_1_mem_rt.csv".format(date), "w") # response-time
    tp_file.write("client,worker,rep,write_tp,read_tp\n")
    rt_file.write("client,worker,rep,write_rt,read_rt\n")

    # extract and compute values
    for vc in vc_list:
        for worker in worker_list:
            for rep in rep_list:

                write_tp_list = []
                read_tp_list = []
                write_rt_list = []
                read_rt_list = []

                files = glob("{}/client*_ratio_*_vc_{}_worker_{}_rep_{}.json".format(log_dir, vc, worker, rep))
                assert len(files) == 6
                for f in files:
                    js = json.load(open(f))

                    if js["configuration"]["ratio"] == "1:0":
                        # set requests
                        write_tp_list.append(js["ALL STATS"]["Sets"]["Ops/sec"])
                        write_rt_list.append(js["ALL STATS"]["Sets"]["Latency"])
                    elif js["configuration"]["ratio"] == "0:1":
                        # read requests
                        read_tp_list.append(js["ALL STATS"]["Gets"]["Ops/sec"])
                        read_rt_list.append(js["ALL STATS"]["Gets"]["Latency"])
                
                tp_file.write('{},{},{},{},{}\n'.format(2*3*vc, worker, rep, np.sum(write_tp_list), np.sum(read_tp_list)))
                rt_file.write('{},{},{},{},{}\n'.format(2*3*vc, worker, rep, np.mean(write_rt_list), np.mean(read_rt_list)))
