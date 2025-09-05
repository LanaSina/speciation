#this file builds a 3d visualisation of Tree of Life
library(dplyr)
library(plotly)
library(htmlwidgets)

plot_summary_tree <- function(
  folder_path,
  folder_name,
  trait_y,
  trait_z,
  color_by    = c(trait_y, trait_z, "nkids"),
  sample_frac = 0.001,
  keep_parents = TRUE,
  out_file    = NULL,
  colour_by = NULL,
  dot_size_range = c(1, 15)
) {

  # ---------------- helpers ----------------
  read_summary_individuals <- function(folder_path) {
    files <- list.files(
      folder_path,
      pattern = "SummaryIndividuals_\\d+\\.csv$",
      full.names = TRUE
    )

    #file sort
    nums <- as.integer(sub(".*_(\\d+)\\.csv$", "\\1", basename(files)))
    ord  <- order(nums, na.last = TRUE)
    files <- files[ord]

    cat("Found", length(files), "summary-individual files (numeric order):\n")
    print(basename(files))

    data_list <- lapply(files, function(f) {
      df <- read.csv(f, stringsAsFactors = FALSE)
      if ("isLight" %in% names(df)) {
        df <- df %>% dplyr::select(-isLight, -parentIsLight)  #dropped non numerical columns. adjust if necessary
      }
      return(df)
    })

    out <- dplyr::bind_rows(data_list, .id = "snapshot_id")
    out$snapshot_index <- as.integer(out$snapshot_id)
    out
  }

  norm01 <- function(v) {
    r <- range(v, na.rm = TRUE)
    if (!is.finite(r[1]) || diff(r) == 0) return(rep(0, length(v)))
    (v - r[1]) / diff(r)
  }

  rescale_to <- function(v, to = c(4, 20)) {
    p <- norm01(v)
    to[1] + p * (to[2] - to[1])
  }

  pick_col <- function(df, ...) {
    opts <- c(...)
    hits <- opts[opts %in% names(df)]
    if (length(hits)) hits[1] else NULL
  }

  qlabel <- function(v, low="low", mid="medium", high="high") {
    if (is.null(v) || all(is.na(v))) return(factor(rep(mid, length(v))))
    qs <- stats::quantile(v, probs = c(1/3, 2/3), na.rm = TRUE, names = FALSE)
    cut(v, breaks = c(-Inf, qs[1], qs[2], Inf),
        labels = c(low, mid, high), include.lowest = TRUE)
  }

  #data cleaning
  all_data <- read_summary_individuals(folder_path)

  needed <- unique(c("ID", "parent", "ancestor", "created", trait_y, trait_z))
  missing <- setdiff(needed, names(all_data))
  if (length(missing)) stop("Missing required columns: ", paste(missing, collapse = ", "))

  all_data <- all_data %>%
    dplyr::select(
      dplyr::any_of(needed),
      dplyr::any_of(c("lifeSpan","lifeExpectancy","size","maxEnergy","sensors","energy", "speed", "nkids"))
    ) %>%
    mutate(
      across(
        tidyselect::any_of(unique(c(
          "created", trait_y, trait_z, color_by,
          c("speed","nkids","lifeSpan","lifeExpectancy","size","maxEnergy","sensors","energy")
        ))),
        ~ suppressWarnings(as.numeric(.x))
      )
    )

  #sampling
  if (!is.null(sample_frac) && sample_frac < 1) {
    set.seed(123)
    keep_ids <- sample(all_data$ID, size = max(1, ceiling(nrow(all_data) * sample_frac)))
    if (isTRUE(keep_parents)) {
      parent_ids <- all_data$parent[all_data$ID %in% keep_ids]
      world <- all_data %>% filter(ID %in% c(keep_ids, parent_ids))
    } else {
      world <- all_data %>% filter(ID %in% keep_ids)
    }
  } else {
    world <- all_data
  }

  #labels
  species_col <- pick_col(world, "species", "ancestor", "parent")
  size_col    <- pick_col(world, "size", "maxEnergy", trait_z)
  speed_col   <- pick_col(world, "speed")

speed_adj <- if (!is.null(speed_col)) {
  spd <- world[[speed_col]]
  speed_bins <- cut(
    spd,
    breaks = quantile(spd, probs = c(0, 0.33, 0.66, 1), na.rm = TRUE),
    labels = c("slow", "average", "fast"),
    include.lowest = TRUE
  )

  factor(speed_bins, levels = c("slow", "average", "fast"))

} else factor(rep("unknown speed", nrow(world)))

  size_adj <- if (!is.null(size_col)) {
    factor(qlabel(world[[size_col]], "small", "medium", "large"),
           levels = c("small","medium","large"))
  } else factor(rep("unknown size", nrow(world)))

  if ("lifeExpectancy" %in% names(world)) {
    exp_life <- world$lifeExpectancy
  } else if ("lifeSpan" %in% names(world)) {
    grp <- if (!is.null(species_col)) world[[species_col]] else factor(1)
    exp_life <- ave(
      world$lifeSpan, grp,
      FUN = function(x) suppressWarnings(stats::quantile(x, 0.90, na.rm = TRUE))
    )
  } else {
    exp_life <- rep(NA_real_, nrow(world))
  }

  if ("lifeSpan" %in% names(world)) {
    frac <- suppressWarnings(pmax(0, pmin(1, world$lifeSpan / exp_life)))
    pct  <- ifelse(is.na(frac), NA_integer_, round(frac * 100))
    died_phrase <- ifelse(is.na(frac), "lifespan unknown",
                   ifelse(frac < 0.5,  "died very early",
                   ifelse(frac < 0.9,  "died early",
                   ifelse(frac < 0.98, "lived a full life",
                                    "outlived expectations"))))
    died_phrase <- ifelse(is.na(pct), died_phrase,
                          paste0(died_phrase, " at ~", pct, "% of expected lifespan"))
  } else {
    died_phrase <- rep("lifespan unknown", nrow(world))
  }

  species_lbl <- if (!is.null(species_col)) paste0(world[[species_col]]) else rep("unknown", nrow(world))
  nkids_txt   <- if ("nkids" %in% names(world)) world$nkids else rep(NA_integer_, nrow(world))
  kids_phrase <- ifelse(is.na(nkids_txt), "kids: n/a",
                        paste0("had ", nkids_txt, ifelse(nkids_txt==1," kid"," kids")))

  born_line <- if ("created" %in% names(world)) paste0("Born at: ", world$created) else ""

  hover_text <- paste0(
    "Individual ", world$ID, " (species ", species_lbl, ")<br>",
    "was ", as.character(speed_adj), " and ", as.character(size_adj), "-sized; <br> ",
    "it ", kids_phrase, ", and ", died_phrase, ".<br>",
    born_line
  )

  #colour
    feat_cols <- intersect(color_by, names(world))
    if (length(feat_cols) == 0) {
      warning("No color_by columns found in data; using grey.")
      world$feat_color <- "#9E9E9E"
    } else {
      if (length(feat_cols) < 3) feat_cols <- c(feat_cols, rep(tail(feat_cols, 1), 3 - length(feat_cols)))
      feat_cols <- feat_cols[1:3]

      #normalising
      to01 <- function(x) {
        if (all(is.na(x))) return(rep(0.5, length(x)))
        rng <- range(x, na.rm = TRUE)
        if (!is.finite(rng[1]) || diff(rng) == 0) return(rep(0.5, length(x)))
        (x - rng[1]) / diff(rng)
      }

      R <- to01(world[[feat_cols[1]]])
      G <- to01(world[[feat_cols[2]]])
      B <- to01(world[[feat_cols[3]]])

      all_na <- is.na(world[[feat_cols[1]]]) & is.na(world[[feat_cols[2]]]) & is.na(world[[feat_cols[3]]])
      cols   <- grDevices::rgb(R, G, B)
      cols[all_na] <- "#9E9E9E"

      world$feat_color <- cols
    }



  #size
  if ("size" %in% names(world) && any(!is.na(world$lifeSpan))) {
    size_grp <- qlabel(world$lifeSpan, low="s", mid="m", high="l")
    size_map <- c(s = dot_size_range[1], m = mean(dot_size_range), l = dot_size_range[2])
    dot_sizes <- unname(size_map[as.character(size_grp)])
  } else {
    dot_sizes <- rep(mean(dot_size_range), nrow(world))
  }


  fig <- plot_ly(
    world,
    x = ~created,
    y = world[[trait_y]],
    z = world[[trait_z]],
    text = hover_text,
    hoverinfo = "text",
    type = "scatter3d",
    mode = "markers",
    marker = list(
      color = world$feat_color,
      size  = dot_sizes,
      opacity = 0.9
    )
  ) %>%
    layout(
      autosize = TRUE,
      scene = list(
        xaxis = list(title = "created"),
        yaxis = list(title = trait_y),
        zaxis = list(title = trait_z),
        dragmode = "turntable"
      ),
      margin = list(l = 0, r = 0, b = 0, t = 0)
    ) %>%
    config(responsive = TRUE)

  fig <- plotly::partial_bundle(fig)

  fig$sizingPolicy <- htmlwidgets::sizingPolicy(
    browser.fill = TRUE,
    viewer.fill = TRUE,
    padding = 0,
    defaultWidth = "100%",
    defaultHeight = "100%",
    viewer.padding = 0
  )

  # save
  if (is.null(out_file)) out_file <- sprintf("html/%s_summary_%s_%s.html", folder_name, trait_y, trait_z)
  saveWidget(fig, out_file, selfcontained = TRUE)
  cat("Saved as", out_file, "\n")

  invisible(fig)
}

#your setup!
# plot_summary_tree(
#   folder_path = "/Users/hyoyeon/Desktop/Career/Sony/Lana/2024_12_29_17_15/0-30K",
#   trait_y = "pgmDeath",
#   trait_z = "maxEnergy",
#   color_by = c("speed","maxEnergy","pgmDeath"),
#   keep_parents = FALSE,
#   sample_frac = 0.005
# )

