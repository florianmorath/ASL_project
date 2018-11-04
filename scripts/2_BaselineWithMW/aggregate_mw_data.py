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

    queueLength = np.mean(df['queueLength'])
    netthreadTime = np.mean(df['timeEnqueued'] - df['timeFirstByte'])
    queueTime = np.mean(df['timeDequeued'] - df['timeEnqueued'])
    workerPreTime = np.mean(df['timememcachedSent'] - df['timeDequeued'])
    memcachedRTT = np.mean(df['timememcachedReceived'] - df['timeFirstByte'])
    workerPostTime = np.mean(df['timeCompleted'] - df['timememcachedReceived'])
    totalRequests = df.shape[0]

    aggregated_file.write('{},{},{},{},{},{},{},{}\n'.format(df['requestType'].iloc[0], queueLength, netthreadTime, queueTime, workerPreTime, memcachedRTT, workerPostTime, totalRequests))
