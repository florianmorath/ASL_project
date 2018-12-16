# /2_BaselineWithMW

### 1. run experiment
```
# one mw
sh run_experiment_one_mw.sh run

# two mws
sh run_experiment_two_mws.sh run
```

### 2. process logs to csvs
```
# one mw
python3 process_mw-logs_one_mw.py <path-to-directory-to-log-files>
python3 process_dstat-logs_one_mw.py <path-to-directory-to-log-files>
python3 process_mem-logs_one_mw.py <path-to-directory-to-log-files>

# two mws
python3 process_dstat-logs_two_mws.py <path-to-directory-to-log-files>
python3 process_mw-logs_two_mws.py <path-to-directory-to-log-files>
python3 process_mem-logs_two_mws.py <path-to-directory-to-log-files>
```

### 3. create plots (jupyter notebook)
```
# one mw
plot_mw-logs_one_mw.ipynb
plot_dstat-logs_one_mw.ipynb
plot_mem-logs_one_mw.ipynb

# two mws
plot_mw-logs_two_mws.ipynb
plot_dstat-logs_two_mws.ipynb
plot_mem-logs_two_mws.ipynb
```