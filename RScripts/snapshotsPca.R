library(dplyr)
library(plotly)
library(htmlwidgets)


read_snapshots <- function(folder_path) {
  files <- list.files(
    folder_path,
    pattern = "ShadowModel_Snapshot.csv",
    recursive = TRUE,
    full.names = TRUE
  )
  files <- sort(files)

  cat("Found", length(files), "snapshot files:\n")
  print(files)

  data_list <- lapply(files, function(f) {
    read.csv(f, skip = 2, stringsAsFactors = FALSE)
  })

  bind_rows(data_list, .id = "snapshot_id")
}

main_folder <- "/Users/hyoyeon/Desktop/Career/Sony/Lana/2025_07_10_03_47"
all_data <- read_snapshots(main_folder)


all_data <- all_data %>%
  select(ID, parent, ancestor, created, everything())
  
set.seed(123)
sample_frac <- 0.5
world <- all_data %>% sample_frac(sample_frac)

# perform pca
numeric_cols <- sapply(world, is.numeric)
trait_data <- world[, numeric_cols]

trait_data <- trait_data[, !(names(trait_data) %in% c("ID", "parent", "ancestor", "created"))]
trait_data <- scale(trait_data)

pca <- prcomp(trait_data, center = TRUE, scale. = TRUE)
world$PC1 <- pca$x[, 1]
world$PC2 <- pca$x[, 2]
world$PC3 <- pca$x[, 3]

#colour
colors <- rgb(
  (world$PC3 - min(world$PC3)) / (max(world$PC3) - min(world$PC3)),
  world$speed / max(world$speed, na.rm = TRUE),
  world$maxEnergy / max(world$maxEnergy, na.rm = TRUE)
)

#plot
fig <- plot_ly(
  world,
  x = ~created,
  y = ~PC1,
  z = ~PC2,
  text = ~paste("ID:", ID,
                "<br>Parent:", parent,
                "<br>Speed:", speed,
                "<br>MaxEnergy:", maxEnergy),
  hoverinfo = "text",
  type = "scatter3d",
  mode = "markers",
  marker = list(
    size = 3,
    color = colors 
  )
)

edge_x <- c()
edge_y <- c()
edge_z <- c()

for (i in seq_len(nrow(world))) {
  parent_idx <- which(world$ID == world$parent[i])
  if (length(parent_idx) > 0) {
    edge_x <- c(edge_x, world$created[i], world$created[parent_idx], NA)
    edge_y <- c(edge_y, world$PC1[i],     world$PC1[parent_idx],     NA)
    edge_z <- c(edge_z, world$PC2[i],     world$PC2[parent_idx],     NA)
  }
}

fig <- fig %>%
  add_trace(
    x = edge_x,
    y = edge_y,
    z = edge_z,
    type = "scatter3d",
    mode = "lines",
    line = list(color = "grey", width = 1),
    showlegend = FALSE,
    hoverinfo = "none",  
    text = NULL          
  )


#save file
saveWidget(fig, "interactive.html", selfcontained = TRUE)
