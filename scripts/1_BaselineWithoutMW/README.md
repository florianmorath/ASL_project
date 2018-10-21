# /1_BaselineWithoutMW

### 1. run experiment
```
sh run_experiment.sh run
```

### 2. process logs to csvs
```
python3 process_mem-logs_one_server.py <path-to-directory-to-log-files>
python3 process_dstat-logs_one_server.py <path-to-directory-to-log-files>
```

### 3. create plots (jupyter notebook)
```
plot_mem-logs_one_srver.ipynb
plot_dstat-logs_one_server.ipynb
```