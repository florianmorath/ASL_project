#!/usr/bin/env python3

""" 
Process log files to csv files such that extracted values can easily be plotted.

output:
- one_mw_mem_tp.csv: thorughput for read and write loads
- one_mw_mem_rt.csv: response-time for read and write loads

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
    os.makedirs('processed_data/one_mw/{}'.format(date), exist_ok=True)

    # experiment config
    # TODO: run script can write config params into separate file from which they can be read 
    ratio_list=['0:1','1:0']
    vc_list=[2,4,8,16,24,32,40,48,56] 
    worker_list=[8,16,32,64]
    rep_list=[1,2,3]

    # create csv files (one csv file contains all data that will be plotted in one plot)
    tp_file = open("processed_data/one_mw/{}/one_mw_mem_tp.csv".format(date), "w") # throughput
    rt_file  = open("processed_data/one_mw/{}/one_mw_mem_rt.csv".format(date), "w") # response-time
    tp_file.write("client,worker,write_tp_mean,write_tp_std,read_tp_mean,read_tp_std\n")
    rt_file.write("client,worker,write_rt_mean,write_rt_std,read_rt_mean,read_rt_std\n")

    # extract and compute values
    for vc in vc_list:
        for worker in worker_list:
            write_tp_list = []
            read_tp_list = []
            write_rt_list = []
            read_rt_list = []

            for rep in rep_list:

                # want the sum of the tp and the mean of the rt
                temp_write_tp_list = []
                temp_read_tp_list = []
                temp_write_rt_list = []
                temp_read_rt_list = []

                files = glob("{}/client*_ratio_*_vc_{}_worker_{}_rep_{}.json".format(log_dir, vc, worker, rep))
                assert len(files) == 6
                for f in files:
                    js = json.load(open(f))

                    # check that no missed occured
                    if (js["ALL STATS"]["Gets"]["Misses/sec"] != 0.0 or js["ALL STATS"]["Sets"]["Misses/sec"] != 0.0 ):
                        print("warning: get or set misses in {}".format(f))

                    if js["configuration"]["ratio"] == "1:0":
                        # set requests
                        temp_write_tp_list.append(js["ALL STATS"]["Sets"]["Ops/sec"])
                        temp_write_rt_list.append(js["ALL STATS"]["Sets"]["Latency"])
                    elif js["configuration"]["ratio"] == "0:1":
                        # read requests
                        temp_read_tp_list.append(js["ALL STATS"]["Gets"]["Ops/sec"])
                        temp_read_rt_list.append(js["ALL STATS"]["Gets"]["Latency"])
                
                # put sum for tp and mean for rt into final lists
                write_tp_list.append(np.sum(temp_write_tp_list))
                read_tp_list.append(np.sum(temp_read_tp_list))
                write_rt_list.append(np.mean(temp_write_rt_list))
                read_rt_list.append(np.mean(temp_read_rt_list))

            
            tp_file.write('{},{},{},{},{},{}\n'.format(2*3*vc, worker, np.mean(write_tp_list), np.std(write_tp_list), np.mean(read_tp_list), np.std(read_tp_list)))
            rt_file.write('{},{},{},{},{},{}\n'.format(2*3*vc, worker, np.mean(write_rt_list), np.std(write_rt_list), np.mean(read_rt_list), np.std(read_rt_list)))
