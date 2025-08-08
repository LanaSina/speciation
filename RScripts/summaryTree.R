library(dplyr)
library(plotly)
library(htmlwidgets)

plot_summary_tree <- function(
  folder_path,
  trait_y,
  trait_z,
  color_by = c(trait_y, trait_z, "nkids"),
  sample_frac = 0.01,
  keep_parents = TRUE,
  out_file = NULL
) {

  read_summary_individuals <- function(folder_path) {
    files <- list.files(
      folder_path,
      pattern = "^ShadowModel_SummaryIndividuals_\\d+\\.csv$",
      full.names = TRUE
    )
    files <- sort(files)
    cat("Found", length(files), "summary-individual files:\n")
    print(files)
    data_list <- lapply(files, function(f) read.csv(f, stringsAsFactors = FALSE))
    dplyr::bind_rows(data_list, .id = "snapshot_id")
  }

  all_data <- read_summary_individuals(folder_path)

  needed <- unique(c("ID","parent","ancestor","created","speed","nkids", trait_y, trait_z, color_by))
  missing <- setdiff(needed, names(all_data))
  if (length(missing)) stop("Missing required columns: ", paste(missing, collapse = ", "))

  all_data <- all_data %>%
    dplyr::select(dplyr::any_of(needed)) %>%
    mutate(
      across(all_of(unique(c(trait_y, trait_z, color_by, c("speed","nkids")))),
             ~ suppressWarnings(as.numeric(.x)))
    )

  #sample
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

  #colour
  norm01 <- function(v) {
    r <- range(v, na.rm = TRUE)
    if (!is.finite(r[1]) || diff(r) == 0) return(rep(0, length(v)))
    (v - r[1]) / diff(r)
  }


  cb <- unique(color_by)
  if (length(cb) < 3) cb <- c(cb, rep(cb[length(cb)], 3 - length(cb)))
  if (length(cb) > 3) cb <- cb[1:3]

  r_ch <- norm01(world[[cb[1]]])
  g_ch <- norm01(world[[cb[2]]])
  b_ch <- norm01(world[[cb[3]]])
  colors <- rgb(r_ch, g_ch, b_ch)

  #plot
    safe_col <- function(nm) if (nm %in% names(world)) world[[nm]] else NULL

    fig <- plot_ly(
      world,
      x = ~created,
      y = world[[trait_y]],
      z = world[[trait_z]],
      text = ~paste0(
        "ID: ", world$ID,
        "<br>Parent: ", world$parent,
        "<br>Created: ", world$created,
        "<br>", trait_y, ": ", world[[trait_y]],
        "<br>", trait_z, ": ", world[[trait_z]],
        if ("nkids" %in% names(world)) paste0("<br>#Kids: ", world$nkids) else ""
      ),
      hoverinfo = "text",
      type = "scatter3d",
      mode = "markers",
      marker = list(size = 3, color = colors)
    ) %>%
      layout(
        scene = list(
          xaxis = list(title = "created"),
          yaxis = list(title = trait_y),
          zaxis = list(title = trait_z)
        )
      )


  #edge
  edge_x <- c(); edge_y <- c(); edge_z <- c()
  id_to_row <- match(world$parent, world$ID)

  for (i in seq_len(nrow(world))) {
    p <- id_to_row[i]
    if (!is.na(p)) {
      edge_x <- c(edge_x, world$created[i], world$created[p], NA)
      edge_y <- c(edge_y, world[[trait_y]][i], world[[trait_y]][p], NA)
      edge_z <- c(edge_z, world[[trait_z]][i], world[[trait_z]][p], NA)
    }
  }

  if (length(edge_x)) {
    fig <- fig %>%
      add_trace(
        x = edge_x, y = edge_y, z = edge_z,
        type = "scatter3d", mode = "lines",
        line = list(color = "grey", width = 1),
        showlegend = FALSE, hoverinfo = "none", text = NULL
      )
  }

  #save file
  if (is.null(out_file)) {
    out_file <- sprintf("summary_%s_%s.html", trait_y, trait_z)
  }
  saveWidget(fig, out_file, selfcontained = TRUE)
  cat("✅ Saved as", out_file, "\n")

  invisible(fig)
}


# Plot here
plot_summary_tree(
  folder_path = "/Users/hyoyeon/Desktop/Career/Sony/Lana/2025_07_10_03_47/ShadowModel_Summary_Individuals",
  trait_y = "speed",
  trait_z = "sensors",
  color_by = c("speed","maxEnergy","energy"),
  sample_frac = 0.01
)


