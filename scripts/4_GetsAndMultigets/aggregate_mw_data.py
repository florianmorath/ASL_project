#!/usr/bin/env python3

""" 
Aggregate reuqests to a single line of data

"""

import os
import sys
import pandas as pd
import numpy as np

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

    df_f = df[(df['requestType'] == 'GET')]
    

    queueLength = np.mean(df_f['queueLength'])
    netthreadTime = np.mean(df_f['timeEnqueued'] - df_f['timeFirstByte'])
    queueTime = np.mean(df_f['timeDequeued'] - df_f['timeEnqueued'])
    workerPreTime = np.mean(df_f['timememcachedSent'] - df_f['timeDequeued'])
    memcachedRTT = np.mean(df_f['timememcachedReceived'] - df_f['timememcachedSent'])
    workerPostTime = np.mean(df_f['timeCompleted'] - df_f['timememcachedReceived'])
    totalRequests = df_f.shape[0]

    aggregated_file.write('{},{},{},{},{},{},{},{}\n'.format('GET', queueLength, netthreadTime, queueTime, workerPreTime, memcachedRTT, workerPostTime, totalRequests))
