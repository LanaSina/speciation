# Timestamp Branching
## Overview
This code quantifies the branching behaviour in agent populations across time by measuring histogram gaps in key agent characteristics. 

## How to run it
1. Open `timestamp_count.Rmd` file and modify the following variables in the second code block:
     * `base_folder`': It is your path to the directory containing the simulation data. The simulation data should be organised into
      separate subdirectories according to the experimental parameter (`exp_var`).
     * `exp_var`: The experimental parameter you varied during the simulation. (e.g, `birth_dst`, `mut_factor`)
     * `mut_factor_values`: The range of values taken by `exp_var` in the experiment.

2. The third block contains helper functions. Currently the code only counts two or more subsequent empty histogram bins as one gap;
   if you would like to change how it behaves, go to `count_hist_gaps` function and modify the following:
   ```
    sum(r$values & r$lengths >= 2) <-- change the number 2 to @ if you want to count @ or more subsequent empty bins as one gap.
   ```
   Run the code block after modification.

3. The fourth block calculates the mean across all simulation, plots it as a graph and saves them as a pdf file. You can adjust
   the length of time windows by modifying:
   ```
     windows <- list(
    "w1 (0K-25K)" = seq(0, 25000, 5000),
    "w2 (25K-50K)" = seq(30000, 50000, 5000),
    "w3 (50K- 75K)" = seq(55000, 75000, 5000)
    )
   ```

4. The last code block saves the data into a CSV file; it contains `time window`, `timestamp`, `agent_characteristics`, `number of gaps`,
   and `exp_var value`. 
