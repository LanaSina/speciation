
# This script generates graphs of:
# - total activity,
# - total normalized activity (original formula),
# - median normalized activity (original formula),
# - new activity (original formula),
# - total normalized activity (alternative formula) and
# - median normalized activity (alternative formula)
# of a given run.
#
# Type `Rscript %program --help` to see the usage.




library(argparse)
library(data.table) # this library allows super fast reading of CSV files!
library(properties)
suppressPackageStartupMessages(library(zoo))




# ============================ CONSTANTS =======================================

# window size of the moving averages (in number of time steps)
AVERAGE_WINDOW_SIZE_IN_TIME_STEPS <- 100000
# color of raw data lines when original ("normal") formula is used
NORMAL_RAW_COL <- "#0072B2"
# color of moving average lines when original ("normal") formula is used
NORMAL_AVERAGE_COL <- "black"
# color of raw data lines when alternative formula is used
ALT_RAW_COL <- "#D55E00"
# color of moving average lines when alternative formula is used
ALT_AVERAGE_COL <- "black"
# width of raw data lines
RAW_LWD <- 1
# width of running average lines
AVERAGE_LWD <- 2

# ==============================================================================








# =========================== PARSE ARGUMENTS ==================================

parser <- ArgumentParser(
  prog = "oee-stats.R",
  description = "A script that saves the graphs of several open-endedness measures of a provided run."
)

parser$add_argument(
  "input_folder",
  help = "The folder of a run's data"
)

parser$add_argument(
  "-o", "--output",
  default = "./tolsim_oee_stats",
  help = "The output folder where the graphs will be saved"
)

args <- parser$parse_args()

# ==============================================================================








# ========================= MAIN VARIABLES =====================================

# input folder
run_folder = args$input_folder
# output folder (folder where to save the plots)
output_folder <- args$output
output_subfolder <- file.path(output_folder, basename(run_folder))

# number of time steps between each save of the deltas
properties_file = file.path(run_folder, "config.properties")
deltas_saved_every = as.numeric(properties::read.properties(properties_file, fields = c("deltas_saved_every")))
# window size of the moving averages (in number of deltas)
average_window_size <- AVERAGE_WINDOW_SIZE_IN_TIME_STEPS / deltas_saved_every

# paths of the deltas files
realMapDeltasFile <- file.path(run_folder,"RealMapDeltas.csv")
shadowMapDeltasFile <- file.path(run_folder,"ShadowMapDeltas.csv")

# ==============================================================================








# ========================== MAIN FUNCTIONS ====================================

#' Plot a statistic with optional moving average
#'
#' @param data dataframe with one column "t" (time) and another arbitrary column
#' @param ycol string, the name of the column to plot on Y axis
#' @param ylab string, label of the y-axis
#' @param window_size integer, window size for moving average (default = NULL, no average)
plot_statistic <- function(data, ycol, ylab, window_size = average_window_size, deltas_save_frequency = deltas_saved_every) {
  plot(
    data$t,
    data[[ycol]],
    type = "l",
    col = NORMAL_RAW_COL,
    lwd = RAW_LWD,
    xlab = "Time",
    ylab = ylab
  )
  grid()
  
  # running average
  if (!is.null(window_size) && window_size > 1) {
    ma <- zoo::rollmean(data[[ycol]], k = window_size, fill = NA, align = "center")
    lines(data$t, ma, col = NORMAL_AVERAGE_COL, lwd = AVERAGE_LWD, lty = 1)
  }
}


#' Saves a plot as a PDF file.
#' 
#' @param filename the name of the PDF file to generate (without the extension)
#' @param plot_function the function that creates the plot
save_plot <- function(filename, plot_function) {
  filepath <- file.path(output_subfolder, paste0(filename, ".pdf"))
  pdf(filepath)
  plot_function()
  grid()
  invisible(dev.off())
  cat("Saved", filepath, "\r\n")
}

# ==============================================================================







# ==============================================================================
#                            PROCEDURE : STEP 1
# ==============================================================================

cat("PROCEDURE : STEP 1\r\n")
cat("Computing total activity...\r\n")

# ------------------------ COMPUTE TOTAL ACTIVITY ------------------------------

# load file
deltas <- fread(realMapDeltasFile, dec = ".")

# ------------------------------
# INITIALIZE TEMPORARY VARIABLES
# ------------------------------
# number of lines
nbLines <- nrow(deltas)
# the components' names
components <- setdiff(names(deltas), "t")
# the number of components
nbComponents <- length(components)
# current accumulations of deltas
currDeltasAccumulations <- setNames(rep(0, nbComponents), components)
# current cumulative evolutionary activities (a)
currCumulativeActivities <- numeric(nbComponents)

# -----------------------------------
# INITIALIZE THE RESULT DATA STRUCTURE
# -----------------------------------
# total cumulative activities (A_cum)
totalActivities <- data.table(t = integer(nbLines), totalActivity = numeric(nbLines))

# -----------------------------------------
# LOOP THAT FILLS THE RESULT DATA STRUCTURE
# -----------------------------------------
start_time <- proc.time() # to measure the time that the computation takes
for (line in 1:nbLines) {
  # read the time
  t <- deltas[line, t]
  # print the elapsed computation time since the start of this loop
  if (t %% 100000 == 0) {
    elapsed <- (proc.time() - start_time)[["elapsed"]]
    cat(sprintf("\rt = %d ; elapsed time = %.1f seconds", t, elapsed))
  }
  # compute and store the results
  currDeltas <- as.numeric(deltas[line, ..components])
  currDeltasAccumulations <- currDeltasAccumulations + currDeltas
  currCumulativeActivities <- ifelse(currDeltas == 1, currDeltasAccumulations, 0)
  totalActivities[line, ] <- list(t, sum(currCumulativeActivities))
}

cat("\r\n")

# ------------------------- PLOT TOTAL ACTIVITY --------------------------------

dir.create(output_folder, showWarnings = FALSE)
dir.create(output_subfolder, showWarnings = FALSE)

save_plot("total_activity", function() plot_statistic(totalActivities, "totalActivity", "Total activity"))

# ==============================================================================








# ==============================================================================
#                            PROCEDURE : STEP 2
# ==============================================================================

cat("PROCEDURE : STEP 2\r\n")
cat("Computing total normalized activity, median normalized activity & new activity...\r\n")


# --- COMPUTE TOTAL NORMALIZED ACTIVITY, MEDIAN NORMALIZED ACTIVITY & NEW ACTIVITY ---

# load files
realDeltas <- fread(realMapDeltasFile, dec = ".")
shadowDeltas <- fread(shadowMapDeltasFile, dec = ".")

# ------------------------------
# INITIALIZE TEMPORARY VARIABLES
# ------------------------------
# the number of lines in each file
nbLines <- nrow(realDeltas)
# the real model's components' names
realComponents <- setdiff(names(realDeltas), "t")
# the shadow model's components' names
shadowComponents <- setdiff(names(shadowDeltas), "t")
# union of real components' names and shadow components' names
allComponents <- union(realComponents, shadowComponents)
# current accumulations of normalized deltas (original formula)
currNormDeltasAccs <- setNames(rep(0, length(allComponents)), allComponents)
# current accumulations of normalized deltas (alternative formula)
altCurrNormDeltasAccs <- setNames(rep(0, length(realComponents)), realComponents)
# indicates which component has already been considered new
hasBeenNew <- setNames(rep(FALSE, length(realComponents)), realComponents)

# -------------------------------------
# INITIALIZE THE RESULT DATA STRUCTURES
# -------------------------------------
# total normalized activities (original formula)
normTotalActivities <- data.table(t = integer(nbLines), normTotalActivity = numeric(nbLines))
# median normalized activities (original formula)
normMedianActivities <- data.table(t = integer(nbLines), normMedianActivity = numeric(nbLines))
# total normalized activities (alternative formula)
altNormTotalActivities <- data.table(t = integer(nbLines), normTotalActivity = numeric(nbLines))
# median normalized activities (alternative formula)
altNormMedianActivities <- data.table(t = integer(nbLines), normMedianActivity = numeric(nbLines))
# new normlized activities
normNewActivities <- data.table(t = integer(nbLines), normNewActivity = numeric(nbLines))

# -----------------------------------------
# LOOP THAT FILLS THE RESULT DATA STRUCTURES
# -----------------------------------------
start_time <- proc.time() # to measure the time the computation takes
for (line in 1:nbLines) {

  # read the time
  t <- realDeltas[line, t]

  # print the elapsed computation time
  if (t %% 10000 == 0) {
    elapsed <- (proc.time() - start_time)[["elapsed"]]
    cat(sprintf("\rt = %d ; elapsed time = %.1f seconds", t, elapsed))
  }

  # names of components that currently exist in the real model
  realActiveComps <- realComponents[which(realDeltas[line, ..realComponents] == 1)]

  # === READ/COMPUTE DELTAS ===

  currRealDeltas <- setNames(rep(0, length(allComponents)), allComponents)
  currShadowDeltas <- setNames(rep(0, length(allComponents)), allComponents)
  if (length(realComponents) > 0) {
    currRealDeltas[realComponents] <- as.numeric(realDeltas[line, ..realComponents])
  }
  if (length(shadowComponents) > 0) {
    currShadowDeltas[shadowComponents] <- as.numeric(shadowDeltas[line, ..shadowComponents])
  }
  currNormDeltas <- currRealDeltas - currShadowDeltas
  names(currNormDeltas) <- allComponents

  # === COMPUTE TOTAL AND MEDIAN ACTIVITIES (ORIGINAL FORMULA) ===

  currNormDeltasAccs <- currNormDeltasAccs + currNormDeltas
  currNormCumActivities <- ifelse(currRealDeltas == 1, currNormDeltasAccs, 0)
  normTotalActivities[line] <- list(t, sum(currNormCumActivities[realActiveComps]))
  normMedianActivities[line] <- list(t, if (length(realActiveComps) > 0) median(currNormCumActivities[realActiveComps]) else NA)

  # === COMPUTE TOTAL AND MEDIAN ACTIVITIES (ALTERNATIVE FORMULA) ===

  altCurrNormDeltasAccs[realActiveComps] <- altCurrNormDeltasAccs[realActiveComps] + currNormDeltas[realActiveComps]
  altCurrNormCumActivities <- ifelse(currRealDeltas[realComponents] == 1, altCurrNormDeltasAccs, 0)
  altNormTotalActivities[line]   <- list(t, sum(altCurrNormCumActivities[realActiveComps]))
  altNormMedianActivities[line] <- list(t, if (length(realActiveComps) > 0) median(altCurrNormCumActivities[realActiveComps]) else NA)

  # === COMPUTE NEW ACTIVITIES ===

  # compute minimum cumulative activity of the real components
  currNormCumActivitiesOfRealComps <- currNormCumActivities[realComponents]
  minNormCumulativeActivity <- min(currNormCumActivitiesOfRealComps)
  # compute current diversity in the real model
  realDiversity <- sum(currRealDeltas[realComponents])
  # initialize the sum of normalized cumulative activities of new components
  sumOfNormCumActivitiesOfNewComps <- 0
  # determine new components then add their normalized cumulative activities to the previous sum
  if (minNormCumulativeActivity < 0) {
    # threshold beyond which a component is considered adaptively significant
    aN0 <- abs(minNormCumulativeActivity)
    # determine components that are new (or "newly adaptively significant")
    isNewComponent <- currNormCumActivitiesOfRealComps > aN0 & !hasBeenNew[realComponents]
    newComponents <- names(currNormCumActivitiesOfRealComps)[isNewComponent]
    hasBeenNew[newComponents] <- TRUE
    # update the sum
    sumOfNormCumActivitiesOfNewComps <- sum(currNormCumActivitiesOfRealComps[newComponents])
  }
  # compute new activities
  normNewActivities[line] <- list(t, if (realDiversity > 0) sumOfNormCumActivitiesOfNewComps/realDiversity else NA)

}

cat("\r\n")


# --- PLOT TOTAL NORMALIZED ACTIVITY, MEDIAN NORMALIZED ACTIVITY & NEW ACTIVITY ---

# total normalized activity (original formula)
save_plot("total_normalized_activity", function() plot_statistic(normTotalActivities, "normTotalActivity", "Total normalized activity"))

# median normalized activity (original formula)
save_plot("median_normalized_activity", function() plot_statistic(normMedianActivities, "normMedianActivity", "Median normalized activity"))

# new activity (original formula)
save_plot("new_activity", function() plot_statistic(normNewActivities, "normNewActivity", "New activity"))

# total normalized activity (alternative formula)
save_plot("total_normalized_activity_alt", function() plot_statistic(altNormTotalActivities, "normTotalActivity", "Total normalized activity"))

# median normalized activity (alternative formula)
save_plot("median_normalized_activity_alt", function() plot_statistic(altNormMedianActivities, "normMedianActivity", "Median normalized activity"))

# new activity (alternative formula)
# TODO
