#visualisation automation made for convenience so I wouldn't have to type every file paths
suppressPackageStartupMessages({
  library(fs)
  library(stringr)
})

source("visualisation.R")

BASE_DIR <- "/Users/hyoyeon/Desktop/Career/Sony/Lana"
all_dirs <- list.dirs(path = BASE_DIR, recursive = TRUE, full.names = TRUE)
summary_folders <- all_dirs[
  basename(all_dirs) %in% "SummaryIndividuals"
]

OUT_DIR <- "/Users/hyoyeon/Desktop/Career/Sony/speciation/RScripts/results"

#hyperparams
TRAIT_Y <- "kidEnergy"
TRAIT_Z <- "maxEnergy"
SAMPLE_FRAC  <- 0.008
KEEP_PARENTS <- TRUE


find_repos_with_summaries <- function(base_dir) {
  repos <- character(0)
  for (bd in base_dir) {
    if (!dir_exists(bd)) next
    subdirs <- c(bd, dir_ls(bd, type = "directory", recurse = TRUE, fail = FALSE))
    subdirs <- unique(subdirs)

    for (d in subdirs) {
      csvs <- dir_ls(d, type = "file", glob = "*.csv", fail = FALSE)
      if (length(csvs)) {
        has_any <- any(str_detect(path_file(csvs), "^SummaryIndividuals_\\d+\\.csv$"))
        if (has_any) repos <- c(repos, d)
      }
    }
  }
  unique(repos)
}

dir_create(OUT_DIR)
log_file <- file.path(OUT_DIR, "build_log.txt")
log_conn <- file(log_file, open = "wt")
writeLines(sprintf("Build started: %s", Sys.time()), log_conn)

on.exit({
  writeLines(sprintf("Build finished: %s", Sys.time()), log_conn)
  close(log_conn)
}, add = TRUE)

repos <- find_repos_with_summaries(BASE_DIR)

if (!length(repos)) {
  msg <- "No repositories found. Check BASE_DIR or file names."
  writeLines(msg, log_conn)
  stop(msg)
}

writeLines(sprintf("Found %d repositories:", length(repos)), log_conn)
for (rp in repos) writeLines(paste0(" - ", rp), log_conn)

#render
for (folder in summary_folders) {


   folder_name <- basename(dirname(folder))
    cat(sprintf("Currently accessing %s\n", folder_name))
    cat(sprintf("Plotting data from: %s\n", folder)) # Add this to see the path


  plot_summary_tree(
    folder_path = folder,
    folder_name = folder_name,
    trait_y = TRAIT_Y,
    trait_z = TRAIT_Z,
    color_by = c("speed", "maxEnergy", "pgmDeath"),
    keep_parents = KEEP_PARENTS,
    sample_frac = SAMPLE_FRAC,
    out_file = file.path(OUT_DIR, paste0(folder_name, "_summary_", TRAIT_Y, "_", TRAIT_Z, ".html"))
  )
}

cat("\nAll done.\nHTMLs are in: ", OUT_DIR, "\nLog: ", log_file, "\n", sep = "")

