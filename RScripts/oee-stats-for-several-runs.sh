#!/bin/bash


# Usage: $program INPUT_SUPERFOLDER OUTPUT
# In order to work, the program must be run in the same folder as `oee-stats.R`.

# This script generates graphs of:
# - total activity,
# - total normalized activity (original formula),
# - median normalized activity (original formula),
# - new activity (original formula),
# - total normalized activity (alternative formula) and
# - median normalized activity (alternative formula)
# of all runs which the folder of is comprised in INPUT_SUPERFOLDER.
#
# The graphs are generated in the folder OUTPUT.
#
# /!\ INPUT_SUPERFOLDER must only contain folders of runs' data.




if [ "$#" -ne 2 ]; then
    echo "Usage: $0 INPUT_SUPERFOLDER OUTPUT"
    exit 1
fi

INPUT_SUPERFOLDER="$1"
OUTPUT="$2"

if [ ! -d "$INPUT_SUPERFOLDER" ]; then
    echo "Error : the folder $INPUT_SUPERFOLDER does not exist."
    exit 1
fi

for INPUT_FOLDER in "$INPUT_SUPERFOLDER"/*/; do
    if [ -d "$INPUT_FOLDER" ]; then
        echo ""
        echo "Processing data from : $INPUT_FOLDER"
        echo ""
        Rscript oee-stats.R "$INPUT_FOLDER" -o "$OUTPUT"
        echo ""
    fi
done
