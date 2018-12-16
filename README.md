# Advanced Systems Lab (ASL) project

This project is about the evaluation of the performance of a middleware implemented in Java. Exeperiments and measurements are performed. Furthermore, the system is modeled with the help of queueing networks.

The project focuses on understanding the behavior of the system, modeling its performance, code instrumentation, bottleneck detection, performance optimizations, as well as analytical and statistical modeling. All results are summarized in a report. 

The middleware connects load generating memtier clients with memcached servers. Memcached servers can store key-value pairs in memory. The middleware allows us to do load balancing. The whole system architecture looks as follows:
![alt text](/report/resources/overview.png)


## Overview 
```
# Middleware
/src            -- source code of middleware
/test           -- unit and integration tests of middleware         
/dist           -- jar distribution file of middleware
/lib            -- java libraries 
build.xml       -- Apache Ant buildfile
log4j2.xml      -- Log4j configuration file

# Performance evaluation
/logs           -- experiment log-files
/scripts        -- scripts to run experiments, aggregate, process and plot collected data

# Report
/report         -- project report written in LaTex

```

## Middleware 
To compile the java classes and aggregate them into a jar file, run `ant` in the root of this repository. The jar file can be found in the /dist directory.

The middleware can be started as follows:
```sh
java -jar middleware-fmorath.jar -l localhost -p 11311 -m localhost:11211 -t 64 -s true
```
`-jar`: java jar file. 

`-l`: ip address of the middleware.

`-p`: port the middleware listens to.

`-t`: number of worker threads.

`-s`: sharded mode.

`-m`: list of addresses of memcached servers.

## Experimental data used in the report
This is a table that states which log-files are evaluated in the report. The experiments are identified by the timestamp made when they were being run, their log-files can be found in the /logs folder and the corresponding plots can be found in the /scripts/\<section\>/processed_data folder.

| Section | Experiment timestamp|
| ------------- | ------------- |
| 1_BaselineWithoutMW: |  |
| one server | [2018-10-21_20h12] |
| two servers | [2018-12-06_10h49] |
| 2_BaselineWithMW: | |
| one mw | [2018-12-06_23h08] |
| two mws | [2018-12-07_09h02] | 
| 3_ThroughputForWrites | [2018-11-09_13h02] |
| 4_GetsAndMultigets | [2018-11-22_18h12] | 
| 5_2kAnalysis | [2018-11-15_07h37] | 
| 6_Queuing Model: | | 
| M/M/1 | based on 3_ThroughputForWrites [2018-11-09_13h02] |
| M/M/m | based on 3_ThroughputForWrites [2018-11-09_13h02] |
| NoQ one mw | based on 2_BaselineWithMW one mw [2018-12-06_23h08] |    
| NoQ two mws | based on 2_BaselineWithMW two mws [2018-12-07_09h02] | 

## References
- [Project Description](/report/resources/project_description.pdf)
- Book: Raj Jain, The Art of Computer Systems Performance Analysis: Techniques for Experimental Design, Measurement, Simulation, and Modeling, April 1991, ISBN: 978-0471503361.