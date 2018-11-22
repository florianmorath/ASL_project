#!/usr/bin/env python3

""" 
Aggregate reuqests to a single line of data: <new_file_name>.csv
Aggregate data for histogram: <new_file_name>_hist.csv

"""

import os
import sys
import pandas as pd
import numpy as np
import math

"""
Args: 1) file containing request data 2) new file name


"""
if __name__ == "__main__":

    # get path to log file 
    log_file = sys.argv[1]

    new_file_name = sys.argv[2]

    # create final csv file
    aggregated_file = open(new_file_name, "w") 
    aggregated_file.write("requestType,queueLength,netthreadTime,queueTime,workerPreTime,memcachedRTT,workerPostTime, totalRequests\n")

    f = open(log_file)
    df = pd.read_csv(f)

    # maybe add cuttoff: smoother histogram (but different from client histogram?)
    df_f = df[(df['requestType'] == 'GET')]
    

    queueLength = np.mean(df_f['queueLength'])
    netthreadTime = np.mean(df_f['timeEnqueued'] - df_f['timeFirstByte'])
    queueTime = np.mean(df_f['timeDequeued'] - df_f['timeEnqueued'])
    workerPreTime = np.mean(df_f['timememcachedSent'] - df_f['timeDequeued'])
    memcachedRTT = np.mean(df_f['timememcachedReceived'] - df_f['timememcachedSent'])
    workerPostTime = np.mean(df_f['timeCompleted'] - df_f['timememcachedReceived'])
    totalRequests = df_f.shape[0]

    aggregated_file.write('{},{},{},{},{},{},{},{}\n'.format('GET', queueLength, netthreadTime, queueTime, workerPreTime, memcachedRTT, workerPostTime, totalRequests))

    # histogram file 
    f_name_hist = new_file_name[:-4] + "_hist.csv"
    hist_file = open(f_name_hist, "w")
    hist_file.write("latency,weight\n")

    weight_dict = {}
    for i in range(200):
        weight_dict.update({i+1:0})

    notincl = 0
    
    for index, row in df_f.iterrows():
        exact_latency_ms = (row['timeCompleted'] - row['timeFirstByte'])/1e6
        bucket_pos = math.ceil(exact_latency_ms/0.1)

        curr = weight_dict.get(bucket_pos)
        if curr is not None:
            weight_dict.update({bucket_pos:curr + 1})
        else:
            notincl += 1

    for latency, weight in weight_dict.items():
        hist_file.write('{},{}\n'.format(latency,weight))
    
    print("not included: " + str(notincl))
