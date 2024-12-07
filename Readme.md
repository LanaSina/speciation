# Simulating Speciation

This is a quick getting started guide.

## Setup

Recommended software: 
- Java IDE (eg JetBrains's IntelliJ) to run the simulation
- R and RStudio to run the analysis code

## Running the simulation

In the `startup/Constants.java` fle, modify the following values as appropriate:

Folder where data file will be recorded
``public static String DataPath = "../new_data";``
Whether to record data or not
``public static boolean Save = true;
public static boolean SavePredation = true;``

Build and execute with `startup/Starter.java` as `main` file.
