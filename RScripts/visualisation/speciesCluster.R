suppressPackageStartupMessages({
  library(RANN)
  library(dbscan)
  library(plotly)
  library(tidyr)
})

build_cluster_frames <- function(
  numdf,
  pc_fit,
  feat_cols,
  checkpoints,
  per_cp_max = 20000L,
  minpts     = 4L,
  kq         = 0.98,
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

  .draw_elbow <- function(cp, kd, eps_hat, eps_chosen, minpts){
    par(mar = c(4,4,3,1))
    plot(kd, type = "l", xlab = "Sorted neighbors", ylab = "k-distance",
         main = sprintf("Checkpoint %s | minPts = %d", as.character(cp), minpts))
    abline(h = eps_hat,    col = "red", lty = 2, lwd = 2)
    abline(h = eps_chosen, col = "red", lty = 1, lwd = 2)
    legend("topleft",
           legend = c(sprintf("elbow eps_hat = %.4g", eps_hat),
                      sprintf("chosen eps = %.4g", eps_chosen)),
           lty = c(2,1), lwd = 2, col = "red", bty = "n")
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

  elbow_pdf_file <- "all_elbows.pdf"   # set to NULL to disable
  if (!is.null(elbow_pdf_file)) {
    if (!dir.exists(dirname(elbow_pdf_file))) dir.create(dirname(elbow_pdf_file), recursive = TRUE, showWarnings = FALSE)
    grDevices::pdf(elbow_pdf_file, width = 7, height = 5)   # inches
    on.exit(try(grDevices::dev.off(), silent = TRUE), add = TRUE)
  }

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

    # --- Build 2D PCs and standardize for DBSCAN ---
    pcs_full <- predict(pc_fit, newdata = as.matrix(sub[, feat_cols, drop = FALSE]))
    pcs_full <- pcs_full[, 1:2, drop = FALSE]
    X2 <- .scale_mat(pcs_full)

    if (nrow(X2) < (minpts + 1L)) {
      frames[[i]] <- list(x = numeric(0), y = numeric(0), cluster = character(0))
      next
    }

    #k distance
    kd      <- .k_distances(X2, k = minpts)
    eps_hat <- .elbow_kneedle(kd)
    if (!is.finite(eps_hat) || eps_hat <= 0) {
      eps_hat <- as.numeric(stats::quantile(kd, probs = kq, na.rm = TRUE, names = FALSE))
      if (!is.finite(eps_hat) || eps_hat <= 0) {
        frames[[i]] <- list(x = numeric(0), y = numeric(0), cluster = character(0))
        next
      }
    }

    
    mults <- c(1.0)
    best <- NULL
    for (m in mults) {
      eps_try <- eps_hat * m
      fit <- dbscan::dbscan(X2, eps = eps_try, minPts = minpts, borderPoints = TRUE)
      n_clusters <- max(fit$cluster)
      noise_frac <- mean(fit$cluster == 0)

      if (n_clusters >= 1 && noise_frac <= 0.9) {
        best <- list(fit = fit, eps = eps_try, noise_frac = noise_frac, n_clusters = n_clusters)
        break
      }
    }


    if (is.null(best)) {
      fit <- dbscan::dbscan(X2, eps = eps_hat * tail(mults, 1L), minPts = minpts, borderPoints = TRUE)
      best <- list(fit = fit,
                   eps = eps_hat * tail(mults, 1L),
                   noise_frac = mean(fit$cluster == 0),
                   n_clusters = max(fit$cluster))
    }

    cat(sprintf("cp=%s | eps=%.4f | clusters=%d | noise=%.1f%%\n",
                as.character(cp), best$eps, best$n_clusters, 100*best$noise_frac))

    cl <- best$fit$cluster
    .draw_elbow(cp, kd, eps_hat, best$eps, minpts)

    # DBSCANelbow_outdir
    # db <- dbscan::dbscan(X2, eps = eps_hat, minPts = minpts, borderPoints = TRUE)
    # cl <- db$cluster


    plot_idx <- if (nrow(sub) > per_cp_max) sample.int(nrow(sub), per_cp_max) else seq_len(nrow(sub))
    pcs_plot <- pcs_full[plot_idx, , drop = FALSE]
    cl_plot  <- cl[plot_idx]

    frames[[i]] <- list(
      x = as.numeric(pcs_plot[, 1]),
      y = as.numeric(pcs_plot[, 2]),
      cluster = ifelse(cl_plot == 0, "noise", as.character(cl_plot))
    )
  }

    list(frames = frames, base_plot = base_plot)
}
