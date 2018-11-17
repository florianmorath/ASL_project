# /4_GetsAndMultigets

### 1. run experiment
```
sh run_experiment.sh run

```

### 2. process logs to csvs
```
python3 process_dstat-logs.py <path-to-directory-to-log-files>
python3 process_mem-logs.py <path-to-directory-to-log-files>
python3 process_mw-logs.py <path-to-directory-to-log-files>


```

### 3. create plots (jupyter notebook)
```
plot_mw-logs.ipynb
plot_dstat-logs.ipynb
plot_mem-logs.ipynb


```