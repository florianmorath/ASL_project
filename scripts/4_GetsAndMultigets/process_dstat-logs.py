#!/usr/bin/env python3

""" 
Process log files to csv files such that extracted values can easily be plotted.

output:
- dstat_{client, mw or server}_{cpu, netsend or netrecv}_ratio_{0:1 or 1:0}.csv
"""


import os
import sys
from glob import glob
import json
import numpy as np
import pandas as pd

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
    vc_list=[2] 
    worker_list=[64]
    rep_list=[1,2,3]
    sharded_list=[True,False]
    cutt_off = 10

    ratio_list=['1:1','1:3','1:6','1:9']
    operations_list=['cpu','netsend','netrecv']
    vm_list=['client','server','mw']

    for sharded in sharded_list:
        for operation in operations_list:
            for vm in vm_list:
                # fixed file
                f = open("processed_data/{}/dstat_{}_{}_sharded_{}.csv".format(date, vm, operation, sharded), "w")
                f.write("client,worker,sharded,{}_mean,{}_std\n".format(operation, operation))

                # extract and compute values
                for vc in vc_list:
                    for worker in worker_list:
                        for ratio in ratio_list:
                            op_list = []

                            for rep in rep_list:
                                f_log = open("{}/dstat_{}1_ratio_{}_vc_{}_worker_{}_rep_{}_sharded_{}.csv".format(log_dir, vm, ratio, vc, worker, rep, sharded))
                                df = pd.read_csv(f_log, header=5)
                                if operation == 'netsend':
                                    op_list.append(np.mean(df['send'][cutt_off:-cutt_off])) # skip first and last x values (start-up and cool-down phase)
                                elif operation == 'netrecv':
                                    op_list.append(np.mean(df['recv'][cutt_off:-cutt_off]))
                                elif operation == 'cpu':
                                    op_list.append(np.mean(df['idl'][cutt_off:-cutt_off].map(lambda x: 100 - x)))
                                    
                            f.write("{},{},{},{},{}\n".format(3*2*vc, worker, ratio, np.mean(op_list), np.std(op_list)))

                        
                          
                   

