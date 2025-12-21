# The scripts




## Plot evolutionary activity statistics of **one** run

To generate plots of statistics related to evolutionary activity, use the `oee-stats.R` script.
Here is an example command:

```sh
Rscript oee-stats.R ../../new_data/2025_07_07_17_28 -o ../../tolsim_oee_stats
```

The previous command will generate plots of:
- total activity,
- total normalized activity (original formula),
- median normalized activity (original formula),
- new activity (original formula),
- total normalized activity (alternative formula) and
- median normalized activity (alternative formula)

of the run whose folder is `../../new_data/2025_07_07_17_28` in `../../tolsim_oee_stats/2025_07_07_17_28/`.

The usage is:
```sh
Rscript oee-stats.R RUN_FOLDER -o OUTPUT_FOLDER
```




## Plot non-evolutionary activity statistics of **one** run

To generate plots of statistics **not** related to evolutionary activity, use the `normal-stats.R`.
Here is an example command:

```sh
Rscript normal-stats.R ../../new_data/2025_07_07_17_28 -o ../../tolsim_normal_stats
```

The previous command will generate plots of:
- population or demography,
- speed,
- maxEnergy,
- nkids,
- pgmDeath

of the run whose folder is `../../new_data/2025_07_07_17_28` in `../../tolsim_normal_stats/2025_07_07_17_28/`.

The usage is:
```sh
Rscript normal-stats.R RUN_FOLDER -o OUTPUT_FOLDER
```




## Generate plots of **several** runs

Use `run_script_on_several_runs.sh`.

Here are example commands:
```sh
# generate plots of evolutionary activity statistics
./run_script_on_several_runs.sh oee-stats.R ../../new_data ../../tolsim_oee_stats
# generate plots of NON evolutionary activity statistics
./run_script_on_several_runs.sh normal-stats.R ../../new_data ../../tolsim_normal_stats
```

The previous commands will generate plots for every run whose folder is in `../../new_data`, and will put them in `../../tolsim_oee_stats` or `../../tolsim_normal_stats`.
More precisely, the plots of each run folder `../../new_data/RUN_FOLDER` will be generated in `../../tolsim_{oee,normal}_stats/RUN_FOLDER`.

Usage:

```sh
./run_script_on_several_runs.sh R_SCRIPT RUNS_FOLDER OUTPUT_FOLDER
```

**Warning: all folders in `RUNS_FOLDER`:**
- **must be runs' folders**;
- **must contain the data of the deltas (for plotting evolutionary activity statistics) and/or of the dead individuals (`SummaryIndividuals_*.csv`, for the **non** evolutionary activity statistics)**.
