# Tree of Life Simulation (ToLSim)

Tree of Life Simulation (ToLSim) is a program that simulates speciation dynamics.


## Requirements
 
- Maven
- Java
- R


## Running the simulation

### Configure

In [`src/main/java/com/alife/tolsim/startup/Constants.java`](src/main/java/com/alife/tolsim/startup/Constants.java), modify the following values as appropriate:

| Constant                                             | Description                                                                                                      |
|------------------------------------------------------|------------------------------------------------------------------------------------------------------------------|
| `public static String DataPath = "../new_data";`     | Folder where data of the runs will be recorded.                                                                  |
| `public static boolean Save = false;`                | Whether to record data on individuals and predation or not. (Note: for now, deltas are systematically recorded.) |
| `public static final boolean EnableDisplay = false;` | Whether to show a display or not. Disabling the display leads to better performances.                            |
| `public static final boolean RunShadowModel = true;` | Whether a shadow of the simulation must be run in parallel.                                                      |

In [`src/main/resources/config.properties`](src/main/resources/config.properties), modify the following as appropriate:
```properties
# -----------------------
# OEE analysis properties
# -----------------------

# number of time steps between two shadow model resets
shadow_model_reset_every=1000
# number of time steps between two saves of the deltas
deltas_saved_every=1000
```

### Build

```shell
mvn clean package
```

### Run

```shell
java -jar target/tolsim.jar
```

Add the option `--stop-at` or `-s` to safely stop the program at the provided time step:

```shell
java -jar target/tolsim.jar --stop-at 1000000
```


## Data analysis

[`RScripts/summary.Rmd`](RScripts/summary.Rmd) provides scripts to analyze the data produced by the program from the point of view of *open-endedness*.


## Notes

- Direct children of Light creatures are not displayed.