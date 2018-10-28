#!/usr/bin/env python3

""" 
Process log files to csv files such that extracted values can easily be plotted.

output:
- one_mw_tp.csv: thorughput for read and write loads
- one_mw_rt.csv: response-time for read and write loads
"""

import os
import sys
from glob import glob
import pandas as pd
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
    ratio_list=['0:1','1:0']
    vc_list=[2,4] 
    worker_list=[8,16]
    rep_list=[1,2]

    # create csv files (one csv file contains all data that will be plotted in one plot)
    tp_file = open("processed_data/one_mw/{}/one_mw_tp.csv".format(date), "w") # throughput
    rt_file = open("processed_data/one_mw/{}/one_mw_rt.csv".format(date), "w") # response-time
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

                files = glob("{}/mw1_ratio_*_vc_{}_worker_{}_rep_{}.csv".format(log_dir, vc, worker, rep))
                assert len(files) == 2
                for f in files:
                    df = pd.read_csv(f)

                    if df['requestType'].iloc[0] == 'SET':
                        write_tp_list.append(df[' totalRequests'].iloc[0] / 5) # divide by test-time
                        write_rt_list.append(df['netthreadTime'].iloc[0] + df['queueTime'].iloc[0] + df['workerPreTime'].iloc[0] + df['memcachedRTT'].iloc[0] + df['workerPostTime'].iloc[0]) 
                    
                    if df['requestType'].iloc[0]  == 'GET':
                        read_tp_list.append(df[' totalRequests'].iloc[0] / 5) # divide by test-time
                        read_rt_list.append(df['netthreadTime'].iloc[0] + df['queueTime'].iloc[0] + df['workerPreTime'].iloc[0]+ df['memcachedRTT'].iloc[0] + df['workerPostTime'].iloc[0])

            tp_file.write('{},{},{},{},{},{}\n'.format(vc, worker, np.mean(write_tp_list), np.std(write_tp_list), np.mean(read_tp_list), np.std(read_tp_list)))
            rt_file.write('{},{},{},{},{},{}\n'.format(vc, worker, np.mean(write_rt_list), np.std(write_rt_list), np.mean(read_rt_list), np.std(read_rt_list)))
