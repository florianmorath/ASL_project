# /4_GetsAndMultigets

### 1. run experiment
```
sh run_experiment.sh run
```

### 2. process logs to csvs
```
python3 process_dstat-logs.py <path-to-directory-to-log-files>
python3 process_mem-logs.py <path-to-directory-to-log-files>
python3 process_mem-logs_histogram.py <path-to-directory-to-log-files>
python3 process_mw-logs.py <path-to-directory-to-log-files>
python3 process_mw-logs_histogram.py <path-to-directory-to-log-files>
```

### 3. create plots (jupyter notebook)
```
plot_mw-logs.ipynb
plot_dstat-logs.ipynb
plot_mem-logs.ipynb
```

## Histograms

The histogram of the latency distribution measured on the MW is computed as follows:  \
We have a dictionary that maps bucket positions to a weight. Each bucket represents an interval of 0.1ms. Based on the latency of a request, we can associate it to a bucket and increase the corresponding weight by 1.
For the histograms measured on the client, we can directly use the CDF computed by memtier. 

To plot the histogram data, we can use the ``matplotlib.pyplot.histhist()`` method.


