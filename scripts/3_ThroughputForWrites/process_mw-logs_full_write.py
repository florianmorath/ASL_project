#!/usr/bin/env python3

""" 
Process log files to csv files such that extracted values can easily be plotted.

output:
- full_write_mw_tp.csv: thorughput (for write loads)
- full_write_mw_rt.csv: response-time (for write loads)
- full_write_mw_rt_breakdown.csv: breakdown of different response-times in MW (for write loads)
- full_write_mw_queuelength.csv: queue-length (for write loads)
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
    os.makedirs('processed_data/{}'.format(date), exist_ok=True)

    # experiment config
    ratio_list=['1:0']
    vc_list=[1,32] #[1,4,8,16,24,32,48] 
    worker_list=[64] #[8,16,32,64]
    rep_list=[1,2] #[1,2,3]
    test_time=50
    cut_off=10

    # create csv files (one csv file contains all data that will be plotted in one plot)
    tp_file = open("processed_data/{}/full_write_mw_tp.csv".format(date), "w") # throughput
    tp_file.write("client,worker,write_tp_mean,write_tp_std\n")
    
    rt_file = open("processed_data/{}/full_write_mw_rt.csv".format(date), "w") # response-time
    rt_file.write("client,worker,write_rt_mean,write_rt_std\n")

    queue_file = open("processed_data/{}/full_write_mw_queuelength.csv".format(date), "w") # queue-length
    queue_file.write("client,worker,write_queueLength,write_queueLength_std\n")

    break_file = open("processed_data/{}/full_write_mw_rt_breakdown.csv".format(date), "w") # rt-breakdown
    break_file.write("client,worker,write_netthreadTime,write_queueTime,write_workerPreTime,write_memcachedRTT,write_workerPostTime\n")

    
    # extract and compute values
    for vc in vc_list:
        for worker in worker_list:
            
            write_tp_list = []
            write_rt_list = []

            write_queueLength_list = [] 
            
            write_netthreadTime_list = []
            write_queueTime_list = []
            write_workerPreTime_list = []
            write_memcachedRTT_list = []
            write_workerPostTime_list = []

            for rep in rep_list:

                temp_write_tp_list = []
                temp_write_rt_list = []

                temp_write_queueLength_list = [] 
                
                temp_write_netthreadTime_list = []
                temp_write_queueTime_list = []
                temp_write_workerPreTime_list = []
                temp_write_memcachedRTT_list = []
                temp_write_workerPostTime_list = []


                files = glob("{}/mw*_ratio_*_vc_{}_worker_{}_rep_{}.csv".format(log_dir, vc, worker, rep))
                assert len(files) == 2
                for f in files:
                    df = pd.read_csv(f)
                    if df.empty:
                        print(str(f) + ' is empty')
                        continue 

                    if df['requestType'].iloc[0] == 'SET':
                        temp_write_tp_list.append(df[' totalRequests'].iloc[0] / (test_time-2*cut_off)) # divide by test-time
                        temp_write_rt_list.append((df['netthreadTime'].iloc[0] + df['queueTime'].iloc[0] + df['workerPreTime'].iloc[0] + df['memcachedRTT'].iloc[0] + df['workerPostTime'].iloc[0])/1e6) 

                        temp_write_queueLength_list.append(df['queueLength'].iloc[0])

                        temp_write_netthreadTime_list.append(df['netthreadTime'].iloc[0]/1e6)
                        temp_write_queueTime_list.append(df['queueTime'].iloc[0]/1e6)
                        temp_write_workerPreTime_list.append(df['workerPreTime'].iloc[0]/1e6)
                        temp_write_memcachedRTT_list.append(df['memcachedRTT'].iloc[0]/1e6)

                        temp_write_workerPostTime_list.append(df['workerPostTime'].iloc[0]/1e6)
                    
                 
                # for tp, sum together content -> replace list. for rt take mean -> new list
                write_tp_list.append(np.sum(temp_write_tp_list))
                write_rt_list.append(np.mean(temp_write_rt_list))

                write_queueLength_list.append(np.mean(temp_write_queueLength_list))
                
                write_netthreadTime_list.append(np.mean(temp_write_netthreadTime_list))
                write_queueTime_list.append(np.mean(temp_write_queueTime_list))
                write_workerPreTime_list.append(np.mean(temp_write_workerPreTime_list))
                write_memcachedRTT_list.append(np.mean(temp_write_memcachedRTT_list))
                write_workerPostTime_list.append(np.mean(temp_write_workerPostTime_list))
              

            tp_file.write('{},{},{},{}\n'.format(2*3*vc, worker, np.mean(write_tp_list), np.std(write_tp_list)))
            rt_file.write('{},{},{},{}\n'.format(2*3*vc, worker, np.mean(write_rt_list), np.std(write_rt_list)))
            queue_file.write('{},{},{},{}\n'.format(2*3*vc, worker, np.mean(write_queueLength_list), np.std(write_queueLength_list)))
            break_file.write('{},{},'.format(2*3*vc, worker))
            break_file.write('{},{},{},{},{}\n'.format(np.mean(write_netthreadTime_list), np.mean(write_queueTime_list), np.mean(write_workerPreTime_list), np.mean(write_memcachedRTT_list), 
                        np.mean(write_workerPostTime_list)))
