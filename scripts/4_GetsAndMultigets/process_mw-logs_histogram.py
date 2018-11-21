#!/usr/bin/env python3

""" 
Process log files to csv files such that extracted values can easily be plotted.

output:
- mw_histogram.csv: rt histogram for sharded and nonsharded multigets (ratio = 1:6 ) 
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
    ratio_list=['1:1']#['1:6']
    vc_list=[2] 
    worker_list=[64]
    rep_list=[1] #[1,2,3]
    sharded_list=['true'] #['true','false']
    mw_list = [1,2]

    # create csv files (one csv file contains all data that will be plotted in one plot)
    hist_file = open("processed_data/{}/mw_histogram.csv".format(date), "w")
    hist_file.write("mw,sharded,ratio,rep,latency,weight\n")

    # extract and compute values
    for vc in vc_list:
        for worker in worker_list:
            for ratio in ratio_list:
                for sharded in sharded_list:
                    for rep in rep_list:
                        for mw in mw_list:

                            files = glob("{}/mw{}_ratio_{}_vc_{}_worker_{}_rep_{}_sharded_{}_hist.csv".format(log_dir, mw, ratio, vc, worker, rep, sharded))
                            assert len(files) == 1
                            for f in files:
                                df = pd.read_csv(f)
                                if df.empty:
                                    print(str(f) + ' is empty')
                                    continue 

                                for index, row in df.iterrows():
                                    hist_file.write("{},{},{},{},{},{}\n".format(mw,sharded,ratio,rep,row['latency'],row['weight']))
                            

                                
                 

                      
     
