#!/usr/bin/env python3

""" 
Process log files to csv files such that extracted values can easily be plotted.

output:
- mem_tp.csv: throughput for sharded and nonsharded loads
- mem_rt.csv: response time for sharded and nonsharded loads
- mem_percentiles.csv: percentiles for sharded and nonsharded loads

"""

import os
import sys
from glob import glob
import json
import numpy as np

def get_percentile_latency(f, percentile):
    distance = 100
    distribution_list = f["ALL STATS"]["GET"]
    best_distribution = distribution_list[0]
    for dist in distribution_list:
	    if(abs(dist['percent'] - percentile) < distance):
		    best_distribution = dist
		    distance = abs(dist['percent'] - percentile)
    return best_distribution['<=msec']


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
    tp_file = open("processed_data/{}/mem_tp.csv".format(date), "w") # throughput
    rt_file = open("processed_data/{}/mem_rt.csv".format(date), "w") # response-time
    perc_file = open("processed_data/{}/mem_percentiles.csv".format(date), "w") 

    tp_file.write("client,worker,ratio,sharded_tp_mean,sharded_tp_std,nonsharded_tp_mean,nonsharded_tp_std\n")
    rt_file.write("client,worker,ratio,sharded_rt_mean,sharded_rt_std,nonsharded_rt_mean,nonsharded_rt_std\n")    
    perc_file.write("client,worker,ratio,s_25_mean,s_25_std,s_50_mean,s_50_std,s_75_mean,s_75_std,s_90_mean,s_90_std,s_99_mean,s_99_std,n_25_mean,n_25_std,n_50_mean,n_50_std,n_75_mean,n_75_std,n_90_mean,n_90_std,n_99_mean,n_99_std\n")

    # extract and compute values
    s_tp_list = []
    ns_tp_list = []
    s_rt_list = []
    ns_rt_list = []

    s_25_list = []
    n_25_list = []
    s_50_list = []
    n_50_list = []    
    s_75_list = []
    n_75_list = []
    s_90_list = []
    n_90_list = []
    s_99_list = []
    n_99_list = []
    
    for vc in vc_list:
        for worker in worker_list:
            for ratio in ratio_list:
                for sharded in sharded_list:
            
                    for rep in rep_list:

                        # want the sum of the tp and the mean of the rt
                        temp_s_tp_list = []
                        temp_ns_tp_list = []
                        temp_s_rt_list = []
                        temp_ns_rt_list = []

                        temp_s_25_list = []
                        temp_n_25_list = []
                        temp_s_50_list = []
                        temp_n_50_list = []    
                        temp_s_75_list = []
                        temp_n_75_list = []
                        temp_s_90_list = []
                        temp_n_90_list = []
                        temp_s_99_list = []
                        temp_n_99_list = []

                        files = glob("{}/client*_*_ratio_{}_vc_{}_worker_{}_rep_{}_sharded_{}_mem.json".format(log_dir, ratio, vc, worker, rep, sharded))
                        assert len(files) == 6
                        for f in files:
                            js = json.load(open(f))
                            
                            if sharded == 'true':
                                temp_s_tp_list.append(js["ALL STATS"]["Gets"]["Ops/sec"])
                                temp_s_rt_list.append(js["ALL STATS"]["Gets"]["Latency"])

                                temp_s_25_list.append(get_percentile_latency(js,25))
                                temp_s_50_list.append(get_percentile_latency(js,50))
                                temp_s_75_list.append(get_percentile_latency(js,75))
                                temp_s_90_list.append(get_percentile_latency(js,90))
                                temp_s_99_list.append(get_percentile_latency(js,99))
                            else:
                                temp_ns_tp_list.append(js["ALL STATS"]["Gets"]["Ops/sec"])
                                temp_ns_rt_list.append(js["ALL STATS"]["Gets"]["Latency"])

                                temp_n_25_list.append(get_percentile_latency(js,25))
                                temp_n_50_list.append(get_percentile_latency(js,50))
                                temp_n_75_list.append(get_percentile_latency(js,75))
                                temp_n_90_list.append(get_percentile_latency(js,90))
                                temp_n_99_list.append(get_percentile_latency(js,99))
                        
                        # put sum for tp and mean for rt into final lists
                        if sharded == 'true':
                            s_tp_list.append(np.sum(temp_s_tp_list))
                            s_rt_list.append(np.mean(temp_s_rt_list))

                            s_25_list.append(np.mean(temp_s_25_list))
                            s_50_list.append(np.mean(temp_s_50_list))
                            s_75_list.append(np.mean(temp_s_75_list))
                            s_90_list.append(np.mean(temp_s_90_list))
                            s_99_list.append(np.mean(temp_s_99_list))
                        else:
                            ns_tp_list.append(np.sum(temp_ns_tp_list))
                            ns_rt_list.append(np.mean(temp_ns_rt_list))

                            n_25_list.append(np.mean(temp_n_25_list))
                            n_50_list.append(np.mean(temp_n_50_list))
                            n_75_list.append(np.mean(temp_n_75_list))
                            n_90_list.append(np.mean(temp_n_90_list))
                            n_99_list.append(np.mean(temp_n_99_list))

                    
                tp_file.write('{},{},{},{},{},{},{}\n'.format(2*3*vc, worker, ratio, np.mean(s_tp_list), np.std(s_tp_list), np.mean(ns_tp_list), np.std(ns_tp_list)))
                rt_file.write('{},{},{},{},{},{},{}\n'.format(2*3*vc, worker, ratio, np.mean(s_rt_list), np.std(s_rt_list), np.mean(ns_rt_list), np.std(ns_rt_list)))
                perc_file.write('{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{},{}\n'.format(2*3*vc, worker, ratio, np.mean(s_25_list), np.std(s_25_list),np.mean(s_50_list), np.std(s_50_list),np.mean(s_75_list), np.std(s_75_list),np.mean(s_90_list), np.std(s_90_list),np.mean(s_99_list), np.std(s_99_list),
                np.mean(n_25_list), np.std(n_25_list),np.mean(n_50_list), np.std(n_50_list),np.mean(n_75_list), np.std(n_75_list),np.mean(n_90_list), np.std(n_90_list),np.mean(n_99_list), np.std(n_99_list)))

                # empty lists
                s_tp_list = []
                ns_tp_list = []
                s_rt_list = []
                ns_rt_list = []

                s_25_list = []
                n_25_list = []
                s_50_list = []
                n_50_list = []    
                s_75_list = []
                n_75_list = []
                s_90_list = []
                n_90_list = []
                s_99_list = []
                n_99_list = []



