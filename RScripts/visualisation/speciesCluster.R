suppressPackageStartupMessages({
  library(RANN)
  library(dbscan)
  library(plotly)
  library(tidyr)
})

build_cluster_frames <- function(
  numdf,                 # data.frame with columns: snapshot, feat_cols...
  pc_fit,                # prcomp/prcomp_irlba object with 2 PCs
  feat_cols,             # character vector of numeric feature columns
  checkpoints,           # numeric vector of snapshot values to render
  per_cp_max = 20000L,   # max points per checkpoint for plotting
  minpts     = 4L,       # DBSCAN minPts (also k for k-distance)
  kq         = 0.98,     # fallback: use k-distance quantile if elbow fails
  seed       = 42L,
  window_size = 1000L
) {
  stopifnot(is.data.frame(numdf), "snapshot" %in% names(numdf))
  stopifnot(all(feat_cols %in% names(numdf)))
  if (!is.null(seed)) set.seed(seed)

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

  .k_distances <- function(X, k) {
    X <- as.matrix(X)
    if (nrow(X) <= k || ncol(X) == 0) return(rep(NA_real_, max(1L, nrow(X))))
    nn <- RANN::nn2(X, X, k = k + 1)$nn.dists[, k + 1]  # skip self
    sort(nn)
  }

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

  frames <- vector("list", length(checkpoints))
  names(frames) <- as.character(checkpoints)

  base_plot <- plotly::plot_ly(type = "scattergl", mode = "markers") |>
    plotly::add_trace(
      x = numeric(0), y = numeric(0),
      hovertext = character(0),
      marker = list(size = 4),
      showlegend = FALSE
    ) |>
    plotly::layout(
      xaxis = list(title = "PC1", fixedrange = TRUE),
      yaxis = list(title = "PC2", fixedrange = TRUE),
      margin = list(l = 40, r = 10, b = 40, t = 10)
    )

  for (i in seq_along(checkpoints)) {
    cp <- checkpoints[i]

    sub <- numdf[
      numdf$created >= (cp - window_size) & numdf$created <= cp,
      c("snapshot", "created", "ID", feat_cols), drop = FALSE
    ]
    sub <- tidyr::drop_na(sub)

    if (nrow(sub) && "ID" %in% names(sub)) {
      sub <- sub |>
        dplyr::mutate(.dist = abs(snapshot - cp)) |>
        dplyr::group_by(ID) |>
        dplyr::slice_min(order_by = .dist, n = 1, with_ties = FALSE) |>
        dplyr::ungroup() |>
        dplyr::select(-.dist)
    }

    if (nrow(sub) < (minpts + 2L)) {
      frames[[i]] <- list(x = numeric(0), y = numeric(0), cluster = character(0))
      next
    }

    # sub <- numdf[numdf$snapshot == cp, c("snapshot", feat_cols), drop = FALSE]
    # sub <- tidyr::drop_na(sub)

    # if (nrow(sub) < (minpts + 2L)) {
    #   frames[[i]] <- list(x = numeric(0), y = numeric(0), cluster = character(0))
    #   next
    # }

    # ε via elbow on k-distance (with quantile fallback)
    X_full  <- .scale_mat(as.matrix(sub[, feat_cols, drop = FALSE]))
    kd      <- .k_distances(X_full, k = minpts)
    eps_hat <- .elbow_kneedle(kd)
    if (!is.finite(eps_hat)) {
      eps_hat <- as.numeric(stats::quantile(kd, probs = kq, na.rm = TRUE, names = FALSE))
      if (!is.finite(eps_hat)) {
        frames[[i]] <- list(x = numeric(0), y = numeric(0), cluster = character(0))
        next
      }
    }

    # DBSCAN
    db <- dbscan::dbscan(X_full, eps = eps_hat, minPts = minpts, borderPoints = TRUE)
    cl <- db$cluster

    plot_idx <- if (nrow(sub) > per_cp_max) sample.int(nrow(sub), per_cp_max) else seq_len(nrow(sub))
    sub_plot <- sub[plot_idx, , drop = FALSE]
    cl_plot  <- cl[plot_idx]

    pcs <- predict(pc_fit, newdata = as.matrix(sub_plot[, feat_cols, drop = FALSE]))
    if (is.null(dim(pcs))) pcs <- cbind(pcs, 0)

    # cl_lab <- ifelse(cl_plot == 0, "noise", as.character(cl_plot))

    frames[[i]] <- list(
      x = as.numeric(pcs[, 1]),
      y = as.numeric(pcs[, 2]),
      cluster = ifelse(cl_plot == 0, "noise", as.character(cl_plot))
    )
  }

  list(frames = frames, base_plot = base_plot)
}
