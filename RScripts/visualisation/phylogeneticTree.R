# ============================================================
# phylogeneticTree.R
# Vertical, time-on-Y phylogeny for the dashboard
# Exposes:
#   build_phylogeny_bundle(all_data, feat_cols, pc_fit, checkpoints,
#                          min_branch_size = 1500L, fallback_k = 4L)
# Returns:
#   list(plot, frames, col_order, t_min, t_max, n_connectors, n_uprights,
#        x_min, x_max)
# ============================================================

suppressPackageStartupMessages({
  library(dplyr)
  library(tidyr)
  library(plotly)
  library(dbscan)
})

# ---- Ensure species labels (uses existing global PCA for embedding) ----
.ensure_embedding_species <- function(df, feat_cols, pc_fit) {
  if ("ancestor" %in% names(df)) {
    df$.species <- as.character(df$ancestor)
    return(df)
  }
  pcs <- predict(pc_fit, newdata = as.matrix(df[, feat_cols, drop = FALSE]))
  pcs <- pcs[, 1:2, drop = FALSE]
  sdx <- stats::sd(pcs[,1]); sdy <- stats::sd(pcs[,2])
  eps <- 0.15 * sqrt(sdx^2 + sdy^2)
  minPts <- max(10L, floor(nrow(df) * 0.001))
  cl <- dbscan::dbscan(pcs, eps = eps, minPts = minPts)
  labs <- cl$cluster; labs[labs == 0] <- NA_integer_
  df$.species <- paste0("sp", ifelse(is.na(labs), "Noise", labs))
  df
}

# ---- Build species meta & edges (time = first snapshot) ----
.build_phylo_core <- function(df, id_col="ID", parent_col="parent") {
  stopifnot(all(c(id_col, parent_col, "snapshot", ".species") %in% names(df)))

  id2sp <- df |> transmute(id = .data[[id_col]], sp = .species)

  edges_raw <- df |>
    filter(!is.na(.data[[parent_col]])) |>
    transmute(child_id  = .data[[id_col]],
              parent_id = .data[[parent_col]],
              child_sp  = .species) |>
    left_join(id2sp, by = c("parent_id" = "id")) |>
    rename(parent_sp = sp) |>
    filter(!is.na(parent_sp), !is.na(child_sp), parent_sp != child_sp)

  sp_first_snap <- df |>
    group_by(sp = .species) |>
    summarise(first_snapshot = suppressWarnings(min(snapshot, na.rm = TRUE)),
              .groups = "drop")

  use_fallback <- nrow(edges_raw) == 0

  if (!use_fallback) {
    edges_sp <- edges_raw |>
      count(parent_sp, child_sp, name = "n_transitions") |>
      left_join(sp_first_snap, by = c("child_sp" = "sp")) |>
      group_by(child_sp) |>
      arrange(desc(n_transitions), first_snapshot, parent_sp) |>
      slice(1) |>
      ungroup() |>
      transmute(from = parent_sp, to = child_sp,
                time = first_snapshot, edge_status = "solid")
  } else {
    ord <- sp_first_snap |> arrange(first_snapshot)
    el  <- list()
    if (nrow(ord) >= 2) {
      for (i in 2:nrow(ord)) {
        el[[i-1]] <- tibble::tibble(
          from = ord$sp[i-1], to = ord$sp[i],
          time = ord$first_snapshot[i], edge_status = "inferred"
        )
      }
    }
    edges_sp <- if (length(el)) bind_rows(el) else
      tibble(from=character(), to=character(), time=integer(), edge_status=character())
  }

  sp_meta <- df |>
    group_by(sp = .species) |>
    summarise(
      first_snapshot = suppressWarnings(min(snapshot, na.rm = TRUE)),
      n_indiv        = n(),
      n_snapshots    = n_distinct(snapshot),
      .groups = "drop"
    ) |>
    rename(name = sp)

  list(edges = edges_sp, meta = sp_meta)
}

# ---- Choose branches by threshold; fallback to top-K ----
.select_branches <- function(meta, min_size = 1500L, fallback_k = 4L, rank_by = c("n_indiv","n_snapshots")) {
  rank_by <- match.arg(rank_by)
  keep <- meta |>
    filter(!is.na(first_snapshot), name != "ROOT",
           .data[[rank_by]] >= ifelse(rank_by == "n_indiv", min_size, min_size)) |>
    pull(name)
  if (length(keep) == 0L) {
    keep <- meta |>
      filter(!is.na(first_snapshot), name != "ROOT") |>
      arrange(desc(.data[[rank_by]]), first_snapshot) |>
      slice_head(n = fallback_k) |>
      pull(name)
  }
  keep
}

# ---- Make an empty Plotly scaffold with enough traces ----
.make_plot_scaffold <- function(col_order, t_min, t_max, n_connectors, n_uprights,
                                y_ticks, y_text) {
  make_line <- function() {
    list(
      x = numeric(0), y = numeric(0),
      type = "scatter", mode = "lines",
      line = list(width = 1.8, color = "#444"),
      hoverinfo = "skip", showlegend = FALSE
    )
  }
  traces <- list()

  # [0] trunk (fixed at x = 0)
  tr <- make_line(); tr$line$width <- 2.2; tr$line$color <- "#666"
  traces[[length(traces)+1]] <- tr
  # [1..C] connectors
  for (i in seq_len(max(1L, n_connectors))) traces[[length(traces)+1]] <- make_line()
  # [C+1..C+U] uprights
  for (i in seq_len(max(1L, n_uprights)))   traces[[length(traces)+1]] <- make_line()
  # [last] labels (text scatter)
  traces[[length(traces)+1]] <- list(
    x = numeric(0), y = numeric(0), text = character(0),
    type = "scatter", mode = "text",
    textposition = "top center", textfont = list(size = 12),
    hoverinfo = "skip", showlegend = FALSE
  )

  p <- plot_ly()
  for (tr in traces) {
    p <- do.call(add_trace, c(list(p), tr))
  }
  p |>
    layout(
      xaxis = list(
        title = NULL,
        tickmode = "array",
        tickvals = c(0, col_order$x),
        ticktext = c("Trunk", col_order$name),
        zeroline = FALSE,
        fixedrange = TRUE
      ),
      yaxis = list(
        title = "Time (snapshot)",
        range = c(t_min, t_max),
        autorange = FALSE,
        zeroline = FALSE,
        tickmode = "array",
        tickvals = y_ticks,
        ticktext = y_text,
        fixedrange = TRUE
      ),
      margin = list(l = 60, r = 20, t = 10, b = 40)
    ) |>
    config(staticPlot = TRUE, displayModeBar = FALSE, responsive = TRUE)
}

# ---- Precompute frames per checkpoint (time-on-Y; x fixed) ----
.prepare_frames <- function(edges_join, col_order, t_min, t_max, checkpoints) {
  lapply(checkpoints, function(t_cut) {
    # trunk from t_min up to t_cut at x=0
    trunk <- list(x = c(0, 0), y = c(t_min, t_cut))

    # horizontal connectors appear at each branch time <= t_cut
    cons <- edges_join |> filter(div_time <= t_cut)
    connectors <- if (nrow(cons)) {
      Map(function(x0, x1, y) list(x = c(x0, x1), y = c(y, y)),
          cons$x_parent, cons$x_child, cons$div_time)
    } else list()

    # uprights from each child x up to current t_cut, start at branch time
    upr <- edges_join |> filter(div_time <= t_cut)
    uprights <- if (nrow(upr)) {
      Map(function(x, y0) list(x = c(x, x), y = c(y0, t_cut)),
          upr$x_child, upr$div_time)
    } else list()

    labels <- col_order |> mutate(y = t_cut)

    list(
      trunk = trunk,
      connectors = connectors,
      uprights = uprights,
      labels = list(x = labels$x, y = labels$y, text = labels$name)
    )
  })
}

# ---- Public entrypoint ----
build_phylogeny_bundle <- function(all_data, feat_cols, pc_fit, checkpoints,
                                   min_branch_size = 1500L, fallback_k = 4L) {
  stopifnot(all(c("snapshot") %in% names(all_data)))
  # 1) species labelling (stable across whole dataset)
  df <- .ensure_embedding_species(all_data, feat_cols, pc_fit)

  # 2) core edges + meta
  core  <- .build_phylo_core(df)
  edges <- core$edges
  meta  <- core$meta

  # 3) choose branches to keep (x positions determined by first appearance order)
  keep <- .select_branches(meta, min_size = min_branch_size, fallback_k = fallback_k, rank_by = "n_indiv")
  meta_keep <- meta |> filter(name %in% keep) |> arrange(first_snapshot, name)

  # columns (fixed x for each species; never moves)
  col_order <- meta_keep |>
    mutate(col = row_number(),
           x   = as.numeric(col)) |>
    select(name, x, first_snapshot)

  # 4) join edges to x positions and branch times (child first appearance)
  edges_join <- edges |>
    inner_join(col_order, by = c("from" = "name")) |>
    rename(x_parent = x) |>
    inner_join(col_order, by = c("to" = "name")) |>
    rename(x_child = x) |>
    transmute(x_parent, x_child, div_time = time)

  # 5) y range is directly in snapshot units (match slider checkpoints)
  t_min <- min(checkpoints, na.rm = TRUE)
  t_max <- max(checkpoints, na.rm = TRUE)

  # x-range used for the horizontal time line (shapes)
  x_min <- 0 - 0.5
  x_max <- max(col_order$x, 1) + 0.5

  # 6) precompute frames
  frames <- .prepare_frames(edges_join, col_order, t_min, t_max, checkpoints)

  # 7) build a stable, pre-sized scaffold
  n_connectors <- max(1L, nrow(edges_join))
  n_uprights   <- max(1L, nrow(edges_join))
  p <- .make_plot_scaffold(
    col_order, t_min, t_max, n_connectors, n_uprights,
    y_ticks = checkpoints, y_text = sprintf("%dk", checkpoints/1000)
  )

  list(
    plot = p,
    frames = frames,
    col_order = col_order,
    t_min = t_min,
    t_max = t_max,
    n_connectors = n_connectors,
    n_uprights = n_uprights,
    x_min = x_min,
    x_max = x_max
  )
}
