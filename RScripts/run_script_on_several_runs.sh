#!/usr/bin/env bash


# Usage: $program R_SCRIPT RUNS_FOLDER OUTPUT_FOLDER
#
# In order to work:
# - the program must be run in the same folder as the R_SCRIPT
# - RUNS_FOLDER must only contain runs folders
#
# This script runs the provided R script for each run folder in RUNS_FOLDER.
# It is assumed that the script takes a run folder as its first argument and
# an output folder as its second argument.




if [ "$#" -ne 3 ]; then
    echo "Usage: $0 R_SCRIPT RUNS_FOLDER OUTPUT_FOLDER" >&2
    exit 1
fi

R_SCRIPT="$1"
RUNS_FOLDER="$2"
OUTPUT_FOLDER="$3"

if [ ! -d "$RUNS_FOLDER" ]; then
    echo "Error: the folder $RUNS_FOLDER does not exist." >&2
    exit 1
fi

for FOLDER in "$RUNS_FOLDER"/*/; do
    echo ""
    echo "============================================================="
    echo "Processing data from : $FOLDER"
    echo "============================================================="
    echo ""
    Rscript "$R_SCRIPT" "$FOLDER" -o "$OUTPUT_FOLDER"
    echo ""
done

