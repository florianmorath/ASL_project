# /logs
Contains all relevant log-files collected during the experiments.
All log-files that belong to one experiment run are stored inside the same folder and the folders name is set to the timestamp of its creation.

## Files and naming conventions

- **dstat files**: \
  *dstat_<vm_name>\_ratio_<workload_type>\_vc_<#clients>\_worker_<#workers>\_rep_<repetition_number>.csv*
  
  contain resource usage statistics of VMs: CPU usage, network send and receive activity, paging activity, context switches, ...

- **ping files**: \
  *\<source\>_\<destination>_ping.log*

  contains ping measurements between two VMs.

- **middleware files**: \
  *\<vm_name>\_ratio_<workload_type>\_vc_<#clients>\_worker_<#workers>\_rep_<repetition_number>_sharded\_<sharded_mode>.csv*

  contains request type, queue length, netthread time, queue time, worker preprocessing time, memcached RTT, worker postprocessing time and total number of requests.

- **memtier automatic json files**: \
  *\<client_instance>\_ratio_<workload_type>\_vc_<#clients>\_worker\_<#workers>\_rep_<repetition_number>_sharded\_<sharded_mode>_mem.json*

  automatically generated json file by memtier. Can be created with --json-out-file option of memtier. Contains throughput, latency and misses for Set and Get requests. May also contain latency histogram if --hide-histogram was set to false. 

- **memtier manual json files**: \
   *\<client_instance>\_ratio_<workload_type>\_vc_<#clients>\_worker\_<#workers>\_rep_<repetition_number>_sharded\_<sharded_mode>.json*

   basically the same as the automatically created one but parsing is done with own scripts such that we have more control over it. For more details see /scripts folder.


note: some parts of the file-names may be omitted depending on the data we had to collect in the specific section.