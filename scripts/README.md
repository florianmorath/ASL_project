# /scripts
This folder contains the scripts to lunch the experiments, process the log-files and plot the processed data.  

There is a folder for each section of the report which contains all the scripts listed below, a README file explaining how the scripts can be executed and a /processed_data folder which contains the final csv-files and their corresponding plots.

## Experimental setup and scripts used

- **experiment run scripts**: *run_experiment.sh* \
  Bash scripts are used to run the experiments on Azure. They are executed locally and use ssh to execute commands on the Azure VMs. Almost all experiment runs consists of the following steps: 
  1. Compile middleware locally and upload jar file to Azure
  2. Start the memcached servers
  3. Populate the memcached servers
  4. Do pings measurements
  5. Run experiment: Start middleware and generate load with memtier processes 
  6. Kill all processes
  7. Deallocate VMs

- **aggregation scripts**: *aggregate_<mem/mw>_data.py* \
  Aggregation of middleware data works as follows: For every request the Log4j library writes a new line into a csv-file which contains the request type, all timestamps and the current queue length. After the middleware is shut down, the Python mw aggregation script is executed on the cluster. It removes the reuqests in the warm-up and cool-down phase based on the timestamps, then it aggregates the data by taking the mean over the computed time intervals, the queue lengths and counting the total number of requests. The output file is a csv file.

  Aggregation of memtier data works as follows: Every second Memtier prints a line with the current statistics about the throughput and latency of requests. The first and last 10 lines are ignored because of the warum-up and cool-down phase. The other lines are parsed and aggregated by taking the mean over the metrics. The output file is a json file.

- **processing scripts**: *process_<mem/mw/dstat>-logs.py* \
  The processing scripts take the aggregated data files and process them such that the processed data can directly be plotted. The goal is to have one csv-file for every plot, i.e. a csv file should contain all information that is shown in single plot. The final csv files and the corresponding plots are stored in the folder called /processed_data.

  The main task of the processing scripts is to compute the standard deviation and the mean of metrics over the repetitions. The metrics include throughput, latency, CPU utilization, netsend activity, middleware time intervals and queue length. Note that to get the overall throughput for a single repetition, we always take the sum over the individual throughputs of the memtier instances.

- **jupyter notebooks for plotting and data analysis**: *plot_<mem/mw/dstat>-logs.ipynb* \
  Every ipython notebook starts with a cell where the date of the experiment that is analyzed can be adjusted. Pandas is used to read the processed csv files into a dataframe. Matplotlib is used to plot the data.

note: more details about the histograms, 2k analysis and queueing models can be found in the README file of the corresponding folders. 

## Memtier configuration
Memtier is executed with the following options: 
```
--protocol=memcache_text 
--expiry-range=99999-100000 
--key-maximum=10000 
--data-size=4096 
```

## Dstat configuration
Dstat is executed with the following flags: 
```
-c,         enables cpu stats
-n,         enables network stats
-d,         enables disk stats
-g,         enables page stats
-y,         enables system stats (includes context switches and interrupts)
```