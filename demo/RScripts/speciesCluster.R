#!/usr/bin/env Rscript
# speciesCluster.R
# Version for precomputed clusters (no params needed)
# Called directly by dashboard.R — works identically to old version.

suppressPackageStartupMessages({
  library(dplyr)
  library(tidyr)
  library(ggplot2)
  library(uwot)
  library(plotly)
  library(htmlwidgets)
  library(scales)
  library(readr)
})

# ------------------------------------------------------------
# Main function: identical interface as old version
# ------------------------------------------------------------
run_single_file <- function(file_path,
                            feat_cols = NULL,
                            db_minPts   = 10L,
                            elbow_sens  = 10.0,
                            select_n    = 2000L,
                            out_dir     = "dashboards") {
  
  if (!dir.exists(out_dir)) dir.create(out_dir, recursive = TRUE)
  
  # Derive precomputed cluster CSV path
  cluster_csv <- sub("raw_data_", "clusters_", basename(file_path))
  cluster_csv <- file.path(dirname(file_path), cluster_csv)
  
  if (!file.exists(cluster_csv)) {
    cluster_csv <- file.path("precomputed_clusters", basename(cluster_csv))
  }
  if (!file.exists(cluster_csv)) {
    stop("❌ Could not find cluster file for ", file_path, "\nExpected at ", cluster_csv)
  }
  
  # --------------------------
  # Load data
  # --------------------------
  features_df <- read_csv(file_path, show_col_types = FALSE)
  clusters_df <- read_csv(cluster_csv, show_col_types = FALSE)
  
  if (!all(c("ID","Cluster") %in% names(clusters_df))) {
    stop("Cluster CSV must contain columns: ID, Cluster")
  }
  
  # numeric features only
  numdf <- features_df %>%
    select(ID, where(is.numeric)) %>%
    drop_na()
  
  if (is.null(feat_cols)) {
    feat_cols <- setdiff(names(numdf), "ID")
  }
  
  # join features + clusters
  df <- numdf %>%
    inner_join(clusters_df, by = "ID") %>%
    drop_na(all_of(feat_cols))
  
  # --------------------------
  # Compute UMAP embedding
  # --------------------------
  set.seed(42)
  umap_emb <- uwot::umap(as.matrix(df[, feat_cols, drop = FALSE]), n_components = 2)
  df$UMAP1 <- umap_emb[, 1]
  df$UMAP2 <- umap_emb[, 2]
  df$Cluster <- as.factor(df$Cluster)
  
  # --------------------------
  # Plot setup
  # --------------------------
  lvls <- levels(df$Cluster)
  cols <- setNames(hue_pal()(length(lvls)), lvls)
  if ("0" %in% lvls) cols["0"] <- "lightgrey"
  
  eps <- 0.4881   # placeholder to match subtitle format
  minPts <- db_minPts
  sensitivity <- elbow_sens
  
  g <- ggplot(df, aes(UMAP1, UMAP2, color = Cluster)) +
    geom_point(alpha = 0.6, size = 1.5) +
    scale_color_manual(values = cols, name = "Cluster") +
    theme_minimal() +
    labs(
      title = "DBSCAN Clusters on UMAP Projection (Full Data)",
      subtitle = sprintf("eps = %.4f, MinPts = %d, sensitivity = %.1f",
                         eps, minPts, sensitivity),
      x = "UMAP1", y = "UMAP2"
    ) +
    guides(color = guide_legend(override.aes = list(size = 3), ncol = 2))
  
  # identical interactive frame structure
  frame <- list(
    x = df$UMAP1,
    y = df$UMAP2,
    cluster = as.character(df$Cluster)
  )
  
  # return same output structure as before
  list(g = g, frame = frame)
}

# ------------------------------------------------------------
# Standalone test block (optional)
# ------------------------------------------------------------
if (identical(environment(), globalenv())) {
  # This lets you test manually with: Rscript speciesCluster.R
  file_path <- "umap_res/umap_res_10000.csv"
  feat_cols <- c("lifeSpan", "nkids", "matForKids", "speed", "maxEnergy",
                 "kidEnergy", "sensors", "pgmDeath")
  out <- run_single_file(file_path, feat_cols)
  print(out$g)
  
  htmlwidgets::saveWidget(
    plotly::ggplotly(out$g),
    file = sub("\\.csv$", "_cluster_test.html", basename(file_path)),
    selfcontained = TRUE
  )
  message("✅ Test HTML saved successfully.")
}



# suppressPackageStartupMessages({
#   knitr::opts_chunk$set(echo = TRUE, warning = FALSE, message = FALSE)
#   library(dplyr)
#   library(tidyr)
#   library(plotly)
#   library(grDevices)
#   library(dbscan)
#   library(kneedle)
#   library(umap)
#   library(uwot)
#   library(ggplot2)
#   library(dplyr)
#   library(RColorBrewer)
# })
#
# #helper function
# plot_umap <- function(umap_plot_df){
#   p <- ggplot(umap_plot_df, aes(x = UMAP1, y = UMAP2)) +
#     geom_point(alpha = 0.5) +
#     theme_minimal() +
#     labs(title = "UMAP Projection of Agent Attributes (Current Frame)")
#   print(p)
# }
#
# plot_elbow <- function(k_dist_umap_df, elbow_umap, db_minPts){
#   p <- ggplot(k_dist_umap_df, aes(x = Index, y = Distance)) +
#     geom_line() +
#     geom_hline(yintercept = elbow_umap, linetype = "dashed", color = "red") +
#     theme_minimal() +
#     labs(x = "Sorted Index of UMAP Sample Points",
#          y = paste0(db_minPts, "-Nearest Neighbor Distance in UMAP Space"),
#          title = "K-Distance Plot for Epsilon Selection (UMAP Sample)")
#   print(p)
# }
#
# plot_dbscan <- function(db_umap, umap_plot_df_full, elbow_umap, db_minPts, elbow_sens){
#   primary_colors <- c("red", "blue", "green", "orange", "purple", "cyan")
#   n_clusters_umap <- max(db_umap$cluster)
#
#   #colour sceme (if cluster 0 exists, include lightgrey as a colour for noise)
#   if (any(db_umap$cluster == 0)) {
#     cluster_colors_umap <- c("0" = "lightgrey", setNames(primary_colors[1:n_clusters_umap], 1:n_clusters_umap))
#   } else {
#     cluster_colors_umap <- setNames(primary_colors[1:n_clusters_umap], 1:n_clusters_umap)
#   }
#
#   p <- ggplot(umap_plot_df_full, aes(x = UMAP1, y = UMAP2, color = Cluster)) +
#     geom_point(alpha = 0.6, size = 1.5) +
#     scale_color_manual(values = cluster_colors_umap) +
#     theme_minimal() +
#     labs(
#       title = "DBSCAN Clusters on UMAP Projection (Full Data)",
#       subtitle = paste("eps =", round(elbow_umap, 4), ", MinPts =", db_minPts, "sensitivity = ", elbow_sens)
#     ) +
#     guides(color = guide_legend(override.aes = list(size = 3)))
#   print(p)
# }
#
# elbow_detection <- function(db_minPts, select_n, umap_embedding, elbow_sens){
#   set.seed(42)
#   sample_indices = sample(1:nrow(umap_embedding), select_n)
#   umap_sample = umap_embedding[sample_indices, ]
#   k_distance_umap <- kNNdist(umap_sample, k = db_minPts)
#
#   d_umap = sort(k_distance_umap)
#   k_dist_umap_df <- data.frame(Index = 1:length(d_umap), Distance = d_umap)
#
#   knee_umap <- kneedle(k_dist_umap_df$Index, k_dist_umap_df$Distance, sensitivity = elbow_sens, decreasing = FALSE)
#   elbow_umap <- knee_umap[2] #1st element = index, 2nd = value
#
#   cat(paste("New epsilon (elbow) calculated from UMAP sample:", round(elbow_umap, 4), "\n"))
#   plot_elbow(k_dist_umap_df = k_dist_umap_df, elbow_umap = elbow_umap, db_minPts = db_minPts)
#
#   return(list(epsilon = round(elbow_umap, 4), elbow_umap = elbow_umap))
# }
#
# proceed_single_timeframe <- function(df_stripped, db_minPts, elbow_sens, select_n, verbose = TRUE){
#   umap_embedding <- uwot::umap(df_stripped, n_components = 2)
#
#   umap_plot_df = data.frame(UMAP1 = umap_embedding[,1], UMAP2 = umap_embedding[,2])
#   plot_umap(umap_plot_df = umap_plot_df)
#
#   #detect elbow (epsilon)
#   elbow_result <- elbow_detection(db_minPts = db_minPts, select_n = select_n,
#                                   umap_embedding = umap_embedding, elbow_sens = elbow_sens)
#   elbow_umap <- elbow_result$elbow_umap
#
#
#   #dbscan on full data
#   db_umap = dbscan(umap_embedding, eps = elbow_umap, minPts = db_minPts)
#   umap_plot_df_full <- data.frame(UMAP1 = umap_embedding[,1],
#                                   UMAP2 = umap_embedding[,2]) %>%
#     mutate(Cluster = as.factor(db_umap$cluster))
#
#   n_clusters_umap = max(db_umap$cluster)
#   noise_points_umap = sum(db_umap$cluster == 0)
#   cat(paste("DBSCAN (on full UMAP) found", n_clusters_umap, "clusters and", noise_points_umap, "noise points (Cluster 0).\n"))
#
#   plot_dbscan(db_umap = db_umap, umap_plot_df_full = umap_plot_df_full, elbow_umap = elbow_umap,
#               db_minPts = db_minPts, elbow_sens = elbow_sens)
#
#   return(umap_plot_df_full)
# }
#
# # speciesCluster.R
# run_single_file <- function(file_path,
#                             feat_cols,
#                             db_minPts   = 10L,
#                             elbow_sens  = 10.0,
#                             select_n    = 2000L,
#                             out_dir     = "dashboards") {
#   if (!dir.exists(out_dir)) dir.create(out_dir, recursive = TRUE)
#
#   df <- read.csv(file_path)
#   df_stripped <- df |>
#     dplyr::select(dplyr::all_of(feat_cols)) |>
#     tidyr::drop_na()
#
#   # html_path  <- file.path(out_dir, paste0(tools::file_path_sans_ext(basename(file_path)), "_dbscan.html"))
#   # html_title <- paste("DBSCAN UMAP —", basename(file_path))
#
#   res <- proceed_single_timeframe(df_stripped, db_minPts, elbow_sens, select_n, verbose = verbose)
#   res$frame$file <- basename(file_path)
#   res
# }
#
#
#
# # #main function
# # build_cluster_frames <- function(
# #   all_data,
# #   feat_cols,
# #   checkpoints,
# #   db_minPts      = 10L,
# #   elbow_sens     = 10.0,
# #   select_n       = 2000L,
# #   seed           = 42L,
# #   window_size    = 1000L
# # ) {
# #   if (!is.null(seed)) set.seed(seed)
# #
# #   # plotting preparation
# #   base_plot <- plotly::plot_ly(type = "scattergl", mode = "markers") |>
# #     plotly::add_trace(
# #       x = numeric(0), y = numeric(0),
# #       hovertext = character(0),
# #       marker = list(size = 4),
# #       showlegend = FALSE
# #     ) |>
# #     plotly::layout(
# #       xaxis = list(title = "UMAP1", fixedrange = TRUE),
# #       yaxis = list(title = "UMAP2", fixedrange = TRUE),
# #       margin = list(l = 40, r = 10, b = 40, t = 10)
# #     )
# #
# #   #list for holding checkpoints results
# #   frames <- vector("list", length(checkpoints))
# #   cat(paste("frames: ", frames))
# #   names(frames) <- as.character(checkpoints)
# #
# #   for (i in seq_along(checkpoints)) { #loop through checkpoints and cluster
# #     cp <- checkpoints[i]
# #     cat(paste("\n--- Processing Checkpoint: ", cp, " ---\n"))
# #
# #     current_data <- all_data %>%
# #       filter(created >= cp & created < (cp + window_size)) %>%
# #       select(all_of(feat_cols)) %>% tidyr::drop_na()
# #
# #     umap_df_clustered <- proceed_single_timeframe(df_stripped = current_data, db_minPts = db_minPts,
# #                                                     elbow_sens = elbow_sens, select_n = select_n)
# #
# #     frames[[i]] <- list(
# #       x = umap_df_clustered$UMAP1,
# #       y = umap_df_clustered$UMAP2,
# #       cluster = ifelse(umap_df_clustered$Cluster == "0", "noise", as.character(umap_df_clustered$Cluster))
# #     )
# #   }
# #
# #   return(list(frames = frames, base_plot = base_plot))
# # }