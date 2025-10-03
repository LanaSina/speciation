# The scripts

## A procedure for testing for Tokyo Type 1 Open-Ended Evolution (Channon 2024)

### Generate the statistics' graphs of one run

The script `oee-stats.R` generates graphs of:
- total activity,
- total normalized activity (original formula),
- median normalized activity (original formula),
- new activity (original formula),
- total normalized activity (alternative formula) and
- median normalized activity (alternative formula)

of a given run.

If the folder of the run you want to generate the graphs of is `../../new_data/2025_07_07_17_28/` and you want the graphs to be put in `../../tolsim_oee_stats/`, type:

```sh
Rscript oee-stats.R ../../new_data/2025_07_07_17_28/ -o ../../tolsim_oee_stats/
```

The graphs will be generated at `../../tolsim_oee_stats/2025_07_07_17_28/`.

If the option `-o OUTPUT` is omitted, the default output folder (`./tolsim_oee_stats/`) will be used.

Type `Rscript oee-stats.R --help` to see the usage.


### Generate the statistics' graphs of several runs

The script `oee-stats-for-several-runs.sh` generates the graphs for several runs.

If your runs' folders are in `../../new_data/` and you want the graphs to be put in `../../tolsim_oee_stats/`, type:

```sh
./oee-stats-for-several-runs.sh ../../new_data/ ../../tolsim_oee_stats/
```

The graphs for each run folder `../../new_data/RUN_FOLDER` will be generated at `../../tolsim_oee_stats/RUN_FOLDER/`.

**Warning: all folders in `../../new_data/`:**
- **must be runs' folders**;
- **must contain the data of the deltas**.
