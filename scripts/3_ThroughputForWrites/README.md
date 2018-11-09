# /3_ThroughputForWrites

### 1. run experiment
```
sh run_experiment.sh run

```

### 2. process logs to csvs
```
python3 process_mw-logs_full_write.py <path-to-directory-to-log-files>
python3 process_dstat-logs_full_write.py <path-to-directory-to-log-files>
python3 process_mem-logs_full_write.py <path-to-directory-to-log-files>


```

### 3. create plots (jupyter notebook)
```
plot_mw-logs_full_write.ipynb
plot_dstat-logs_full_write.ipynb
plot_mem-logs_full_write.ipynb


```