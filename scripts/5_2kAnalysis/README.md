# /5_2kAnalysis

### 1. run experiment
```
sh run_experiment.sh run
```

### 2. process logs to csvs
```
python3 process_mem-logs_mw_1_s_1.py <path-to-directory-to-log-files>
python3 process_mem-logs_mw_1_s_3.py <path-to-directory-to-log-files>
python3 process_mem-logs_mw_2_s_1.py <path-to-directory-to-log-files>
python3 process_mem-logs_mw_2_s_3.py <path-to-directory-to-log-files>
```

### 3. open spreadsheets

## Explanation
The processing works as usual but now we have some additional steps: The ``process_for_spreadsheet.py`` script collects all the processed data used as an input to the 2k analysis. It puts it into a csv file called ``2kanalysis_data.csv``. Then the data can be imported into a spreadsheet where the 2k analysis is done. The spreadsheet (.xlsx) can be found in the /processed_data folder. 

