#!/usr/bin/env python3

""" 
Process log files to csv files such that extracted values can easily be plotted.

output:
- rt_breakdown.csv: breakdown of different response-times in MW (for sharded and nonsharded multigets)
- queuelength.csv: queue-length (for sharded and nonsharded multigets)
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
    ratio_list=['1:1','1:3','1:6','1:9']
    vc_list=[2] 
    worker_list=[64]
    rep_list=[1,2,3]
    sharded_list=['true','false']

    # create csv files (one csv file contains all data that will be plotted in one plot)
    queue_file = open("processed_data/{}/queuelength.csv".format(date), "w") # queue-length
    queue_file.write("client,worker,ratio,sharded,queueLength,queueLength_std\n")

    break_file = open("processed_data/{}/rt_breakdown.csv".format(date), "w") # rt-breakdown
    break_file.write("client,worker,ratio,sharded,netthreadTime,queueTime,workerPreTime,memcachedRTT,workerPostTime\n")

    # extract and compute values
    for vc in vc_list:
        for worker in worker_list:
            for ratio in ratio_list:
                for sharded in sharded_list:
 
                    read_queueLength_list = []      
                    read_netthreadTime_list = []
                    read_queueTime_list = []
                    read_workerPreTime_list = []
                    read_memcachedRTT_list = []
                    read_workerPostTime_list = []

                    for rep in rep_list:

                        temp_read_tp_list = []
                        temp_read_rt_list = []

                        temp_read_queueLength_list = []
                        
                        temp_read_netthreadTime_list = []
                        temp_read_queueTime_list = []
                        temp_read_workerPreTime_list = []
                        temp_read_memcachedRTT_list = []
                        temp_read_workerPostTime_list = []


                        files = glob("{}/mw*_ratio_{}_vc_{}_worker_{}_rep_{}_sharded_{}.csv".format(log_dir, ratio, vc, worker, rep, sharded))
                        assert len(files) == 2
                        for f in files:
                            df = pd.read_csv(f)
                            if df.empty:
                                print(str(f) + ' is empty')
                                continue 

                              
                            temp_read_queueLength_list.append(df['queueLength'].iloc[0])
                            temp_read_netthreadTime_list.append(df['netthreadTime'].iloc[0]/1e6)
                            temp_read_queueTime_list.append(df['queueTime'].iloc[0]/1e6)
                            temp_read_workerPreTime_list.append(df['workerPreTime'].iloc[0]/1e6)
                            temp_read_memcachedRTT_list.append(df['memcachedRTT'].iloc[0]/1e6)
                            temp_read_workerPostTime_list.append(df['workerPostTime'].iloc[0]/1e6)

                     
                        read_queueLength_list.append(np.mean(temp_read_queueLength_list))
                        read_netthreadTime_list.append(np.mean(temp_read_netthreadTime_list))
                        read_queueTime_list.append(np.mean(temp_read_queueTime_list))
                        read_workerPreTime_list.append(np.mean(temp_read_workerPreTime_list))
                        read_memcachedRTT_list.append(np.mean(temp_read_memcachedRTT_list))
                        read_workerPostTime_list.append(np.mean(temp_read_workerPostTime_list))
                    

                    queue_file.write('{},{},{},{},{},{}\n'.format(2*3*vc, worker, ratio, sharded, np.mean(read_queueLength_list), np.std(read_queueLength_list)))
                    break_file.write('{},{},{},{},{},{},{},{},{}\n'.format(2*3*vc, worker, ratio, sharded, np.mean(read_netthreadTime_list), np.mean(read_queueTime_list), np.mean(read_workerPreTime_list), np.mean(read_memcachedRTT_list), 
                                np.mean(read_workerPostTime_list)))
     
