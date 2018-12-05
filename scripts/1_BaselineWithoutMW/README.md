# /1_BaselineWithoutMW

### 1. run experiment
```
sh run_experiment_one_server.sh run
sh run_experiment_two_servers.sh run
```

### 2. process logs to csvs
```
# one server
python3 process_mem-logs_one_server.py <path-to-directory-to-log-files>
python3 process_dstat-logs_one_server.py <path-to-directory-to-log-files>

# two servers
python3 process_mem-logs_two_servers.py <path-to-directory-to-log-files>
python3 process_dstat-logs_two_servers.py <path-to-directory-to-log-files>
```

### 3. create plots (jupyter notebook)
```
# one server
plot_mem-logs_one_server.ipynb
plot_dstat-logs_one_server.ipynb

# two servers
plot_mem-logs_two_servers.ipynb
plot_dstat-logs_two_servers.ipynb
```