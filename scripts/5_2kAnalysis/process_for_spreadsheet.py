#!/usr/bin/env python3

""" 
Process csv files into one csv file s.t can easily be imported in a spreadsheet.

output:
- 2kanalysis_data.csv

"""

import os
import sys
from glob import glob
import pandas as pd

"""
Args: path to directory containing csv files

"""
if __name__ == "__main__":
    # get path to log directory (directory with date as name)
    log_dir = sys.argv[1]
    log_dir = os.path.abspath(log_dir)

    # create csv file
    csv_file = open("{}/2kanalysis_data.csv".format(log_dir), "w")
    csv_file.write("mw,server,worker,rep,write_rt,read_rt,write_tp,read_tp\n")

    for mw in [1,2]:
        for s in [1,3]:
            f_rt = open("{}/mw_{}_s_{}_mem_rt.csv".format(log_dir,mw,s))
            f_tp = open("{}/mw_{}_s_{}_mem_tp.csv".format(log_dir,mw,s))

            df_rt = pd.read_csv(f_rt)
            df_tp = pd.read_csv(f_tp)

            for w in [8,32]:
                for rep in [1,2,3]:
                    write_rt = df_rt[(df_rt['worker']==w) & (df_rt['rep']==rep)]['write_rt'].values[0]
                    read_rt  = df_rt[(df_rt['worker']==w) & (df_rt['rep']==rep)]['read_rt'].values[0]
                    write_tp = df_tp[(df_tp['worker']==w) & (df_tp['rep']==rep)]['write_tp'].values[0]
                    read_tp  = df_tp[(df_tp['worker']==w) & (df_tp['rep']==rep)]['read_tp'].values[0]

                    csv_file.write("{},{},{},{},{},{},{},{}\n".format(mw,s,w,rep,write_rt,read_rt,write_tp,read_tp))