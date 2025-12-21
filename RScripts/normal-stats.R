#!/usr/bin/env Rscript

library(argparse)
library(data.table) # this library allows super fast reading of CSV files!
library(properties)
suppressPackageStartupMessages(library(zoo))




# ============================ CONSTANTS =======================================

# window size of the running averages (in number of time steps)
RUNNING_AVERAGE_WINDOW_SIZE_IN_TIME_STEPS <- 100000
# population sample rate (e.g. 20 means that 1/20th of the data in the file are used for the plot)
POPULATION_SAMPLE_RATE <- 20
# dead individuals sample rate (e.g. 100 means that 1/100th of the data in the files are used for the plot)
DEAD_INDIVIDUALS_SAMPLE_RATE <- 100
# color of raw data lines
RAW_DATA_COLOR <- "#0072B2"
# color of running average curves
RUNNING_AVERAGE_COLOR <- "black"
# width of raw data curves
RAW_DATA_LWD <- 1
# width of running average curves
RUNNING_AVERAGE_LWD <- 2
# plots height in inches
PLOT_HEIGHT <- 5
# plots width in inches
PLOT_WIDTH <- 10
# default output folder
DEFAULT_OUTPUT_FOLDER <- "./tolsim_normal_stats"

# ==============================================================================








# =========================== PARSE ARGUMENTS ==================================

parser <- ArgumentParser(
  prog = "normal-stats.R",
  description = "A script that saves the graphs of several OEE-unrelated measures of a provided run."
)

parser$add_argument(
  "input_folder",
  help = "The folder of a run's data"
)

parser$add_argument(
  "-o", "--output",
  default = DEFAULT_OUTPUT_FOLDER,
  help = paste("The output folder where the graphs will be saved (default is ", DEFAULT_OUTPUT_FOLDER, ")")
)

args <- parser$parse_args()

# ==============================================================================








# ========================= MAIN VARIABLES =====================================

# input folder
run_folder <- args$input_folder
# output folder (folder where to save the plots)
output_folder <- args$output
output_subfolder <- file.path(output_folder, basename(run_folder))

# window size of the running averages (in number of POPULATION_SAMPLE_RATE-steps leaps)
average_window_size <- RUNNING_AVERAGE_WINDOW_SIZE_IN_TIME_STEPS / POPULATION_SAMPLE_RATE

# path of the population file
populationFile <- file.path(run_folder, "Population.csv")
# paths of the dead individuals files
deadAgentsFiles <- list.files(
  path       = run_folder,
  pattern    = "^SummaryIndividuals_[0-9]+\\.csv$",
  full.names = TRUE
)

# ==============================================================================








# ========================== MAIN FUNCTIONS ====================================

#' Plots a temporal statistic, i.e. a statistic whose x-axis is time
#' (represented by the column `t`), with optional running average.
#'
#' @param data dataframe with one column "t" (time) and another arbitrary column
#' @param ycol string, the name of the column to plot on Y axis
#' @param ylab string, label of the y-axis
#' @param color string, the color of the raw data curve
#' @param window_size integer, window size for running average (default = NULL, no average)
plot_linear_data <- function(data, ycol, ylab, color, window_size = average_window_size) {
  plot(
    data$t,
    data[[ycol]],
    type = "l",
    col  = color,
    lwd  = RAW_DATA_LWD,
    xlab = "Time",
    ylab = ylab
  )
  grid()
  
  # running average
  if (!is.null(window_size) && window_size > 1) {
    ma <- zoo::rollmean(data[[ycol]], k = window_size, fill = NA, align = "center")
    lines(data$t, ma, col = RUNNING_AVERAGE_COLOR, lwd = RUNNING_AVERAGE_LWD, lty = 1)
  }
}


#' Plot scatter chart for two variables.
#' 
#' @param xdata data of the x-axis
#' @param ydata data of the y-axis
#' @param xlab string, label of the x-axis
#' @param ylab string, label of the y-axis
plot_scatter_data <- function(xdata, ydata, xlab, ylab) {
  plot(
    xdata,
    ydata,
    xlab = "Time",
    ylab = ylab,
    type = "p", # points
    pch  = "." # make points small
  )
  grid()
}


#' Saves a plot as a PDF file.
#' 
#' @param filename the name of the PDF file to generate (without the extension)
#' @param plot_function the function that creates the plot
save_plot <- function(filename, plot_function) {
  filepath <- file.path(output_subfolder, paste0(filename, ".pdf"))
  pdf(filepath, height = PLOT_HEIGHT, width = PLOT_WIDTH)
  plot_function()
  grid()
  invisible(dev.off())
  cat("  ↳ saved to ", filepath, "\r\n")
}

# ==============================================================================








cat("Loading population (i.e. demography)...\r\n")
population <- fread(populationFile, dec =".")
# sample a fraction of all time stamps
population <- population[t %% POPULATION_SAMPLE_RATE == 0]

# create the directories
dir.create(output_folder, showWarnings = FALSE)
dir.create(output_subfolder, showWarnings = FALSE)

cat("Plotting population (i.e. demography)...\r\n")
save_plot("population", function() plot_linear_data(population, "population", "Population", RAW_DATA_COLOR))








cat("\r\n")

cat("Loading dead agents...\r\n")
deadAgents <- rbindlist(
  lapply(deadAgentsFiles, fread, dec = "."),
  use.names = TRUE,
  fill = TRUE
)
# sample a fraction of all saved dead individuals
deadAgents <- deadAgents[sample.int(.N, .N %/% DEAD_INDIVIDUALS_SAMPLE_RATE)]

cat("Plotting speed...\r\n")
save_plot("speed", function() plot_scatter_data(deadAgents$created, deadAgents$speed, "Created", "Speed"))

cat("Plotting maxEnergy...\r\n")
save_plot("maxEnergy", function() plot_scatter_data(deadAgents$created, deadAgents$maxEnergy, "Created", "Maximum energy"))

cat("Plotting nkids...\r\n")
save_plot("nkids", function() plot_scatter_data(deadAgents$created, deadAgents$nkids, "Created", "Maximum number of Kids"))

cat("Plotting pgmDeath...\r\n")
save_plot("pgmDeath", function() plot_scatter_data(deadAgents$created, deadAgents$pgmDeath, "Created", "Programmed death"))




