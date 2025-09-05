# ============================================================
# DBSCAN per snapshot with elbow-picked epsilon + species stitching
# Dependencies: dbscan, RANN, dplyr (base graphics for quick plots)
# ============================================================

suppressPackageStartupMessages({
  library(dbscan)
  library(RANN)
  library(dplyr)
})

# ---------- utils ----------
# 1) Kneedle-style elbow on a sorted kNN distance vector
.elbow_kneedle <- function(d_sorted) {
  n <- length(d_sorted)
  if (n < 5L || !any(is.finite(d_sorted))) return(NA_real_)
  rng <- max(d_sorted, na.rm = TRUE) - min(d_sorted, na.rm = TRUE)
  if (!is.finite(rng) || rng <= 0) return(NA_real_)
  y <- (d_sorted - min(d_sorted, na.rm = TRUE)) / (rng + 1e-12)
  x <- seq_len(n); x <- (x - 1) / (n - 1 + 1e-12)
  i <- which.max(y - x)
  d_sorted[i]
}

# 2) Fast kth-NN distances using RANN (returns sorted vector)
.k_distances <- function(X, k) {
  X <- as.matrix(X)
  if (nrow(X) <= k || ncol(X) == 0) return(rep(NA_real_, max(1L, nrow(X))))
  nn <- RANN::nn2(X, X, k = k + 1)$nn.dists[, k + 1]  # skip self (col 1)
  sort(nn)
}

# 3) Robust feature scaling (mean/sd with zero-var guards)
.scale_mat <- function(M) {
  if (!is.matrix(M)) M <- as.matrix(M)
  if (ncol(M) == 0) return(M)
  mu  <- colMeans(M, na.rm = TRUE)
  sdv <- apply(M, 2, sd, na.rm = TRUE)
  sdv[!is.finite(sdv) | sdv == 0] <- 1
  M <- sweep(M, 2, mu, "-")
  M <- sweep(M, 2, sdv, "/")
  M
}

# ---------- epsilon selection ----------
# choose eps per timestamp via elbow; optional smoothing
choose_eps_per_time <- function(df, time_col, feat_cols, k = NULL,
                                sample_n = 5000, scale_features = TRUE,
                                smooth = TRUE, smooth_span = 0.6, seed = NULL) {
  if (is.null(k)) k <- max(4L, 2L * length(feat_cols))  # common heuristic
  times <- sort(unique(df[[time_col]]))
  eps <- numeric(length(times)); names(eps) <- times

  if (!is.null(seed)) set.seed(seed)

  for (ti in seq_along(times)) {
    t <- times[ti]
    sub <- df[df[[time_col]] == t, feat_cols, drop = FALSE]
    sub <- sub[stats::complete.cases(sub), , drop = FALSE]
    if (nrow(sub) < (k + 5)) { eps[ti] <- NA_real_; next }

    if (!is.null(sample_n) && nrow(sub) > sample_n) {
      idx <- sample.int(nrow(sub), sample_n)
      sub <- sub[idx, , drop = FALSE]
    }
    if (scale_features) sub <- .scale_mat(sub)

    kd <- .k_distances(sub, k = k)
    eps[ti] <- .elbow_kneedle(kd)
  }

  # optional LOESS smoothing over time (helps stability)
  if (smooth && sum(is.finite(eps)) >= 4) {
    df_s <- data.frame(t = as.numeric(times), e = eps)
    fit <- stats::loess(e ~ t, data = df_s, span = smooth_span, na.action = stats::na.exclude)
    eps <- as.numeric(stats::predict(fit, df_s$t))
    names(eps) <- times
  }
  eps
}

# ---------- DBSCAN per time ----------
dbscan_per_time <- function(df, time_col, feat_cols,
                            k = NULL, minPts = NULL,
                            sample_n_elbow = 5000,
                            scale_features = TRUE,
                            smooth_eps = TRUE, smooth_span = 0.6,
                            seed = NULL, borderPoints = TRUE) {

  if (is.null(k)) k <- max(4L, 2L * length(feat_cols))
  if (is.null(minPts)) minPts <- k

  eps_map <- choose_eps_per_time(
    df, time_col, feat_cols, k = k,
    sample_n = sample_n_elbow,
    scale_features = scale_features,
    smooth = smooth_eps, smooth_span = smooth_span,
    seed = seed
  )

  out_cluster <- integer(nrow(df)); out_cluster[] <- 0L
  times <- sort(unique(df[[time_col]]))

  for (t in times) {
    eps_t <- eps_map[as.character(t)]
    idx <- which(df[[time_col]] == t)
    if (!is.finite(eps_t) || length(idx) < (minPts + 1)) next

    X <- as.matrix(df[idx, feat_cols, drop = FALSE])
    X <- X[stats::complete.cases(X), , drop = FALSE]
    if (nrow(X) < (minPts + 1)) next

    # keep a mask to put results back in place (in case of dropped rows)
    rowmask <- rep(FALSE, length(idx))
    rowmask[which(stats::complete.cases(df[idx, feat_cols, drop = FALSE]))] <- TRUE

    if (scale_features) X <- .scale_mat(X)

    res <- dbscan::dbscan(X, eps = eps_t, minPts = minPts, borderPoints = borderPoints)
    # write back cluster labels only for complete rows
    tmp <- integer(length(idx)); tmp[] <- 0L
    tmp[rowmask] <- res$cluster
    out_cluster[idx] <- tmp
  }

  df$cluster <- out_cluster
  list(data = df, eps_by_time = eps_map, k = k, minPts = minPts)
}

# ---------- diagnostics ----------
plot_eps_over_time <- function(eps_map, main = "DBSCAN ε (knee) over time") {
  tt <- suppressWarnings(as.numeric(names(eps_map)))
  ee <- as.numeric(eps_map)
  keep <- is.finite(tt) & is.finite(ee)
  if (!any(keep)) { message("No finite eps/time to plot."); return(invisible(NULL)) }
  graphics::plot(tt[keep], ee[keep], type = "l", xlab = "time", ylab = "eps (knee)", main = main)
}

plot_knee_for_time <- function(df, time_col, feat_cols, t, k = NULL, scale_features = TRUE) {
  sub <- df[df[[time_col]] == t, feat_cols, drop = FALSE]
  sub <- sub[stats::complete.cases(sub), , drop = FALSE]
  if (nrow(sub) < 10) { message("Too few points at this time."); return(invisible(NULL)) }
  if (is.null(k)) k <- max(4L, 2L * length(feat_cols))
  if (scale_features) sub <- .scale_mat(sub)
  kd <- .k_distances(sub, k = k)
  knee <- .elbow_kneedle(kd)
  graphics::plot(seq_along(kd), kd, type = "l",
                 xlab = "sorted points", ylab = sprintf("k=%d distance", k),
                 main = sprintf("K-distance knee at time=%s (eps≈%.4g)", as.character(t), knee))
  if (is.finite(knee)) graphics::abline(h = knee, lty = 2)
}

# ---------- species stitching (stable labels across snapshots) ----------
stitch_species <- function(df, time_col, feat_cols, cluster_col = "cluster",
                           min_points = 5L, noise_label = 0L) {
  times <- sort(unique(df[[time_col]]))
  df$species <- NA_integer_
  next_species_id <- 1L

  # helper: centroids by cluster at one time (for clusters with >= min_points)
  centroids_at <- function(d) {
    d |>
      dplyr::filter(.data[[cluster_col]] != noise_label) |>
      dplyr::group_by(.data[[cluster_col]]) |>
      dplyr::mutate(.n = dplyr::n()) |>
      dplyr::ungroup() |>
      dplyr::filter(.n >= min_points) |>
      dplyr::group_by(.data[[cluster_col]]) |>
      dplyr::summarise(dplyr::across(dplyr::all_of(feat_cols), ~mean(.x, na.rm = TRUE)),
                       .groups = "drop")
  }

  # map cluster -> species for first time
  t0 <- times[1]
  d0 <- df[df[[time_col]] == t0, , drop = FALSE]
  c0 <- centroids_at(d0)
  if (nrow(c0) > 0) {
    species_map <- stats::setNames(seq(from = next_species_id, length.out = nrow(c0)),
                                   c0[[cluster_col]])
    next_species_id <- next_species_id + nrow(c0)
    idx0 <- df[[time_col]] == t0 & df[[cluster_col]] %in% names(species_map)
    df$species[idx0] <- species_map[as.character(df[[cluster_col]][idx0])]
  } else {
    species_map <- stats::setNames(integer(0), character(0))
  }

  # iterate over subsequent times
  for (ti in seq(2, length(times))) {
    t_cur  <- times[ti]
    t_prev <- times[ti - 1]
    d_prev <- df[df[[time_col]] == t_prev, , drop = FALSE]
    d_cur  <- df[df[[time_col]] == t_cur,  , drop = FALSE]
    c_prev <- centroids_at(d_prev)
    c_cur  <- centroids_at(d_cur)

    if (nrow(c_cur) == 0) next

    # keep only prev clusters that already have a species
    prev_species_cl <- unique(d_prev[[cluster_col]][!is.na(d_prev$species)])
    c_prev <- c_prev[c_prev[[cluster_col]] %in% prev_species_cl, , drop = FALSE]

    if (nrow(c_prev) > 0) {
      M_prev <- as.matrix(c_prev[, feat_cols, drop = FALSE])
      M_cur  <- as.matrix(c_cur[,  feat_cols, drop = FALSE])
      dmat <- as.matrix(stats::dist(rbind(M_prev, M_cur)))
      dmat <- dmat[seq_len(nrow(M_prev)), nrow(M_prev) + seq_len(nrow(M_cur)), drop = FALSE]

      # greedy nearest matching
      assigned_prev <- rep(FALSE, nrow(c_prev))
      assigned_cur  <- rep(FALSE, nrow(c_cur))
      pairs <- list()

      repeat {
        dmask <- dmat
        dmask[assigned_prev, ] <- Inf
        dmask[, assigned_cur]  <- Inf
        mval <- min(dmask)
        if (!is.finite(mval)) break
        wh <- which(dmask == mval, arr.ind = TRUE)[1, , drop = TRUE]
        i <- wh[1]; j <- wh[2]
        assigned_prev[i] <- TRUE; assigned_cur[j] <- TRUE
        pairs[[length(pairs) + 1L]] <- c(i, j)
      }

      # inherit species for matched pairs
      for (p in pairs) {
        prev_cl <- c_prev[[cluster_col]][p[1]]
        cur_cl  <- c_cur[[cluster_col]][p[2]]
        sid <- unique(d_prev$species[d_prev[[cluster_col]] == prev_cl])
        sid <- sid[!is.na(sid)][1]
        if (length(sid) == 1) {
          df$species[df[[time_col]] == t_cur & df[[cluster_col]] == cur_cl] <- sid
        }
      }

      # assign new species IDs to unmatched current clusters
      new_cur <- c_cur[[cluster_col]][!assigned_cur]
      if (length(new_cur)) {
        new_ids <- seq(from = next_species_id, length.out = length(new_cur))
        next_species_id <- next_species_id + length(new_cur)
        for (k in seq_along(new_cur)) {
          df$species[df[[time_col]] == t_cur & df[[cluster_col]] == new_cur[k]] <- new_ids[k]
        }
      }
    } else {
      # no previous species to match; assign fresh IDs to all current clusters
      new_ids <- seq(from = next_species_id, length.out = nrow(c_cur))
      next_species_id <- next_species_id + nrow(c_cur)
      for (k in seq_len(nrow(c_cur))) {
        cl <- c_cur[[cluster_col]][k]
        df$species[df[[time_col]] == t_cur & df[[cluster_col]] == cl] <- new_ids[k]
      }
    }
  }

  # noise -> 0; any unresolved -> 0
  df$species[is.na(df$species) | df[[cluster_col]] == noise_label] <- 0L
  df
}

# ---------- one-call pipeline ----------
run_dbscan_species <- function(df, time_col, feat_cols,
                               k = NULL, minPts = NULL,
                               sample_n_elbow = 5000,
                               scale_features = TRUE,
                               smooth_eps = TRUE, smooth_span = 0.6,
                               seed = NULL, borderPoints = TRUE,
                               stitch = TRUE, min_points_stitch = 5L) {

  res <- dbscan_per_time(
    df = df, time_col = time_col, feat_cols = feat_cols,
    k = k, minPts = minPts,
    sample_n_elbow = sample_n_elbow,
    scale_features = scale_features,
    smooth_eps = smooth_eps, smooth_span = smooth_span,
    seed = seed, borderPoints = borderPoints
  )

  out_df <- res$data
  if (stitch) {
    out_df <- stitch_species(
      out_df, time_col = time_col, feat_cols = feat_cols,
      cluster_col = "cluster", min_points = min_points_stitch, noise_label = 0L
    )
  }
  list(
    data = out_df,
    eps_by_time = res$eps_by_time,
    k = res$k,
    minPts = res$minPts,
    stitched = stitch
  )
}

