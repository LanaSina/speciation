suppressPackageStartupMessages({
  library(fs)
})

root <- "/Users/hyoyeon/Desktop/Career/Sony/Lana"
root <- path_abs(root)

move_safely <- function(files, dest_dir) {
  if (length(files) == 0) return(invisible(NULL))
  dir_create(dest_dir)
  for (f in files) {
    target <- path(dest_dir, path_file(f))
    if (file_exists(target)) {
      base <- path_ext_remove(path_file(f))
      ext  <- path_ext(f)
      i <- 1L
      repeat {
        candidate <- path(dest_dir, paste0(base, "_", i, if (nzchar(ext)) paste0(".", ext) else ""))
        if (!file_exists(candidate)) { target <- candidate; break }
        i <- i + 1L
      }
    }
    file_move(f, target)
  }
}

process_dir <- function(d) {
  leaf <- path_file(d)
  if (leaf %in% c("predation", "ShadowModel_SummaryIndividuals", "SummaryIndividuals")) return(invisible(NULL))

  files <- dir_ls(d, type = "file", recurse = FALSE)
  if (length(files) == 0) return(invisible(NULL))

  files <- files[!path_has_parent(files, path(d, "predation")) &
                 !path_has_parent(files, path(d, "ShadowModel_SummaryIndividuals")) &
                 !path_has_parent(files, path(d, "SummaryIndividuals"))]

  if (length(files) == 0) return(invisible(NULL))

  fn <- path_file(files)

  is_pred    <- grepl("^predation_\\d+\\.csv$", fn, perl = TRUE)
  is_shadow  <- grepl("^ShadowModel_SummaryIndividuals_\\d+\\.csv$", fn, perl = TRUE)
  is_summary <- grepl("^(?<!ShadowModel_)SummaryIndividuals_\\d+\\.csv$", fn, perl = TRUE)

  pred_files    <- files[is_pred]
  shadow_files  <- files[is_shadow]
  summary_files <- files[is_summary]

  dest_pred    <- path(d, "predation")
  dest_shadow  <- path(d, "ShadowModel_SummaryIndividuals")
  dest_summary <- path(d, "SummaryIndividuals")

  move_safely(pred_files,    dest_pred)
  move_safely(shadow_files,  dest_shadow)
  move_safely(summary_files, dest_summary)

  cat(sprintf("[%-s]\n  • moved %d predation, %d ShadowModel_SummaryIndividuals, %d SummaryIndividuals\n",
              d, length(pred_files), length(shadow_files), length(summary_files)))
}

dirs <- c(root, dir_ls(root, type = "directory", recurse = TRUE))
dirs <- dirs[order(nchar(dirs), dirs)]

invisible(lapply(dirs, process_dir))

cat("Per-directory organization complete.\n")
