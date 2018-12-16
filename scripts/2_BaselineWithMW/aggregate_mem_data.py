#!/usr/bin/env python3

""" 
Aggregate memtier output and write to json file.

"""

import os
import sys
import numpy as np
import re
import json

"""
Args: 1) file containing request data 2) new file name


"""

# configs
cut_off_phase = 10

if __name__ == "__main__":

    # get path to log file 
    log_file = sys.argv[1]

    new_file_name = sys.argv[2]

    # create final json file
    aggregated_file = open(new_file_name, "w") 

    pos = log_file.find('ratio')
    ratio = log_file[pos+6:pos+9]
    if ratio == '0:1':
        op = "Gets"
    else:
        op = "Sets"

    f = open(log_file)

    valid = re.compile(r"\[RUN #1\s+\d+%,\s+\d+ secs\]")
    
    # get number of valid lines = test time
    test_time = 0
    for line in f:
        line.strip()
        if valid.match(line):
            test_time += 1
    
    f.seek(0)

    tps = []
    rts = []
    rep = 0
    for line in f:
        line.strip()
        if valid.match(line):
            if rep >= cut_off_phase and rep < test_time - cut_off_phase:
                parts = re.split(r"\s", line)
                parts = list(filter(None, parts))
                tps.append(int(parts[9]))
                rts.append(float(parts[16]))
            rep += 1

    tps = np.asarray(tps)
    rts = np.asarray(rts)
  
    stat_dict = {
        "configuration":{
            "ratio": ratio
            }

        ,"ALL STATS":{
            op:{
                "Ops/sec": np.mean(tps)
                ,"Latency": np.mean(rts)
                }
            }
        }

    json.dump(stat_dict, aggregated_file)
