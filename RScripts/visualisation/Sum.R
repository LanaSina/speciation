#visualisation automation made for convenience so I wouldn't have to type every file paths
suppressPackageStartupMessages({
  library(fs)
  library(stringr)
})

BASE_DIRS <- c(
  "/Users/hyoyeon/Desktop/Career/Sony/Lana/2025_07_10_03_47/SummaryIndividuals",
  "/Users/hyoyeon/Desktop/Career/Sony/Lana/2024_12_29_17_15/SummaryIndividuals",
  "/Users/hyoyeon/Desktop/Career/Sony/Lana/2025_07_12_00_26/SummaryIndividuals"
)

TRAIT_Y <- "pgmDeath"
TRAIT_Z <- "maxEnergy"

SAMPLE_FRAC  <- 0.008
KEEP_PARENTS <- TRUE

OUT_DIR <- "/Users/hyoyeon/Desktop/Career/Sony/speciation/RScripts/results"

source("visualisation.R")

find_repos_with_summaries <- function(base_dirs) {
  repos <- character(0)
  for (bd in base_dirs) {
    if (!dir_exists(bd)) next
    # all subdirs including bd
    subdirs <- c(bd, dir_ls(bd, type = "directory", recurse = TRUE, fail = FALSE))
    subdirs <- unique(subdirs)

    for (d in subdirs) {
      # look for at least one "SummaryIndividuals_*.csv"
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

repos <- find_repos_with_summaries(BASE_DIRS)

if (!length(repos)) {
  msg <- "No repositories found. Check BASE_DIRS or file names."
  writeLines(msg, log_conn)
  stop(msg)
}

writeLines(sprintf("Found %d repositories:", length(repos)), log_conn)
for (rp in repos) writeLines(paste0(" - ", rp), log_conn)

#render
for (rp in repos) {
  repo_dir_label <- path_file(path_dir(rp))
  out_name  <- sprintf("%s_%s_%s.html", repo_dir_label, TRAIT_Y, TRAIT_Z)
  out_path  <- file.path(OUT_DIR, out_name)

  msg <- sprintf("\n[%s] Rendering %s -> %s", Sys.time(), repo_dir_label, out_path)
  cat(msg, "\n")
  writeLines(msg, log_conn)


  tryCatch({
       plot_summary_tree(
         folder_path  = rp,
         folder_name    = repo_dir_label,
         trait_y      = TRAIT_Y,
         trait_z      = TRAIT_Z,
         color_by     = c(TRAIT_Y, TRAIT_Z, "speed"),
         sample_frac  = SAMPLE_FRAC,
         keep_parents = KEEP_PARENTS,
         out_file     = out_path,
         dot_size_range = c(1, 15)
       )
       ok <- sprintf("SUCCESS: %s", out_path)
       writeLines(ok, log_conn)
     }, error = function(e) {
       err <- sprintf("FAILED: %s\n  -> %s", out_path, conditionMessage(e))
       writeLines(err, log_conn)
       message(err)
     })
   }

cat("\nAll done.\nHTMLs are in: ", OUT_DIR, "\nLog: ", log_file, "\n", sep = "")
