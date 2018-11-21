#!/usr/bin/env python3

""" 
Process log files to csv files such that extracted values can easily be plotted.

output:
- mem_histogram.csv: rt histogram for sharded and nonsharded loads

"""

import os
import sys
from glob import glob
import json
import numpy as np
import math

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
    ratio_list=['1:6']
    vc_list=[2] 
    worker_list=[64]
    rep_list=[1,2,3]
    sharded_list=['true','false']
    mw_list = [1,2]
    client_list = [1,2,3]

    # create csv files (one csv file contains all data that will be plotted in one plot)
    hist_file = open("processed_data/{}/mem_histogram.csv".format(date), "w")
    hist_file.write("client,mw,sharded,ratio,latency,weight\n")

    # extract and compute values    
    for vc in vc_list:
        for worker in worker_list:
            for ratio in ratio_list:
                for sharded in sharded_list:
                    for mw in mw_list:
                        for client in client_list:

                            distr_dict = {}
                            for i in range(100):
                                distr_dict.update({i+1:0})

                            for rep in rep_list:

                                files = glob("{}/client{}_{}_ratio_{}_vc_{}_worker_{}_rep_{}_sharded_{}_mem.json".format(log_dir, client, mw, ratio, vc, worker, rep, sharded))
                                assert len(files) == 1
                                for f in files:
                                    js = json.load(open(f))
                                    distr_list = js['ALL STATS']['GET']
                                    entry_count = len(distr_list)

                                    for i in range(entry_count-1):
                                        distance = distr_list[i+1].get('<=msec')-distr_list[i].get('<=msec')
                                        weight = distr_list[i+1].get('percent')-distr_list[i].get('percent')
                      
                                        if round(distance,1) <= 0.1:
                                            bucket_position = math.ceil(distr_list[i+1].get('<=msec')/0.1) 
                                            curr = distr_dict.get(bucket_position)
                                            distr_dict.update({bucket_position:curr + weight})

                                
                            # write data to csv
                            for latency, weight in distr_dict.items():
                                hist_file.write("{},{},{},{},{},{}\n".format(client,mw,sharded,ratio,latency,weight))

