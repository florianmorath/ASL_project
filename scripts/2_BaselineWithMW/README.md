# /2_BaselineWithMW

### 1. run experiment
```
sh run_experiment.sh run
```

### 2. process logs to csvs
```
# one mw
python3 process_mw-logs_one_mw.py <path-to-directory-to-log-files>
python3 process_dstat-logs_one_mw.py <path-to-directory-to-log-files>

```

### 3. create plots (jupyter notebook)
```
# one mw
plot_mw-logs_one_mw.ipynb


```