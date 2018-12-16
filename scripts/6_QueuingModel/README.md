# /6_QueuingModel

### M/M/1
```
MM1.ipynb
```

### M/M/m
```
MMm.ipynb
```

### Network of Queues
```
# one mw
NoQ_one_mw.ipynb
NoQ_one_mw_read.m
NoQ_one_mw_write.m

# two mws
NoQ_two_mws.ipynb
NoQ_two_mws_read.m
NoQ_two_mws_write.m
```

## Explanation
Since this section is based on the data of previous sections, we can directly read the processed csv-files into dataframes and do the calculations on them. This is done via jupyter notebooks. They collect the needed data, calculate the predictions based on the queueing models and compare them with the measured data. For the network of queues the predictions are done in Octave using the [queueing package](https://www.moreno.marzolla.name/software/queueing/). 