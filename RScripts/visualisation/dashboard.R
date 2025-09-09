if (!requireNamespace("irlba", quietly = TRUE)) {
  install.packages("irlba", repos = "https://cloud.r-project.org")
}
if (!requireNamespace("RANN", quietly = TRUE)) {
  install.packages("RANN", repos = "https://cloud.r-project.org")
}
if (!requireNamespace("dbscan", quietly = TRUE)) {
  install.packages("dbscan", repos = "https://cloud.r-project.org")
}

library(htmltools)
library(htmlwidgets)
library(plotly)
library(dplyr)
library(tidyr)
library(irlba)
library(RANN); library(dbscan)
library(jsonlite)
source("visualisation.R")
source("phylogeneticTree.R")
source("speciesCluster.R")

camera <- list(eye=list(x=1.4,y=-1.6,z=1.0), center=list(x=0,y=0,z=0), up=list(x=0,y=0,z=1))
folder  <- "/Users/hyoyeon/Desktop/Career/Sony/Lana/2025_07_07_23_23/SummaryIndividuals"
folder_name <- "2025_07_07_23_23"

time_range <- c(0L, 8000L)




fig3d <- plot_summary_tree(
  folder_path  = folder,
  folder_name  = folder_name,
  created_range= time_range,
  trait_y      = "maxEnergy",
  trait_z      = "speed",
  color_by     = c("lifeSpan",
                   "speed",
                   "maxEnergy",
                   "kidEnergy",
                   "nkids"),
  dot_sizes = c(1, 15),
  keep_parents = FALSE,
  sample_frac  = 1.0
) %>% layout(scene = list(camera = camera, aspectmode = "manual",
                          aspectratio = list(x = 1.3, y = 1.3, z = 1)))


#Species cluster
read_summary_individuals <- function(folder_path) {
  files <- list.files(folder_path, pattern = "SummaryIndividuals_\\d+\\.csv$", full.names = TRUE)
  stopifnot(length(files) > 0)
  nums  <- as.integer(sub(".*_(\\d+)\\.csv$", "\\1", basename(files)))
  ord   <- order(nums, na.last = TRUE)
  files <- files[ord]; nums <- nums[ord]
  cat("Found", length(files), "summary-individual files (numeric order):\n")
  # print(basename(files))

  dl <- lapply(seq_along(files), function(i) {
    df <- read.csv(files[i], stringsAsFactors = FALSE)
    df$snapshot <- nums[i]; df
  })
  dplyr::bind_rows(dl)
}

all_data <- read_summary_individuals(folder)

limited_data <- all_data %>%
  dplyr::filter(created >= time_range[1], created <= time_range[3])


numdf <- all_data |>
  select(snapshot, where(is.numeric)) |>
  tidyr::drop_na()

nzv <- vapply(numdf |> select(-snapshot), function(x) sd(x, na.rm = TRUE) > 0, logical(1))
feat_cols <- names((numdf |> select(-snapshot))[, nzv, drop = FALSE])

#PCA
set.seed(42)
n_total   <- nrow(numdf)
n_pca_fit <- min(120000L, n_total)
fit_idx   <- sample.int(n_total, n_pca_fit)
pc_fit <- irlba::prcomp_irlba(as.matrix(numdf[fit_idx, feat_cols, drop = FALSE]),
                              n = 2, center = TRUE, scale. = TRUE)
#setting timeline
CHECKPOINTS <- c(0L, 5000L, 10000L, 15000L, 20000L, 25000L, 30000L)
LABELS      <- sprintf("%dk", CHECKPOINTS/1000)


cluster_bundle <- build_cluster_frames(
  numdf       = numdf,
  pc_fit      = pc_fit,
  feat_cols   = feat_cols,
  checkpoints = CHECKPOINTS,
  per_cp_max  = 20000L,
  minpts      = 4L,
  kq          = 0.98
)
frames          <- cluster_bundle$frames
species_cluster <- cluster_bundle$base_plot


#phylogenetic tree
phy <- build_phylogeny_bundle(
  all_data        = all_data,
  feat_cols       = feat_cols,
  pc_fit          = pc_fit,
  checkpoints     = CHECKPOINTS,
  min_branch_size = 1500L,
  fallback_k      = 4L
)
phylo_plot     <- phy$plot
phy_frames     <- phy$frames
MAX_CONNECTORS <- phy$n_connectors
MAX_UPRIGHTS   <- phy$n_uprights
PHY_XMIN       <- phy$x_min
PHY_XMAX       <- phy$x_max


#colours
pal_base    <- c("#E45756","#4C78A8","#54A24B","#F58518","#72B7B2",
                 "#B279A2","#FF9DA6","#9D755D","#ECA400","#7EBDC2",
                 "#A0A7A8","#8E6C8A","#A3A948","#F2C14E","#F78154")
noise_color <- "#BDBDBD"
#js
frames_json      <- toJSON(unname(frames), auto_unbox = TRUE)
labels_json      <- toJSON(LABELS,        auto_unbox = TRUE)
palette_json     <- toJSON(pal_base,      auto_unbox = TRUE)
noise_json       <- toJSON(noise_color,   auto_unbox = TRUE)
phy_frames_js    <- toJSON(phy_frames,    auto_unbox = TRUE)
checkpoints_json <- toJSON(CHECKPOINTS,   auto_unbox = TRUE)
phy_xmin_js      <- toJSON(PHY_XMIN,      auto_unbox = TRUE)
phy_xmax_js      <- toJSON(PHY_XMAX,      auto_unbox = TRUE)

js <- sprintf("(function() {
  const frames      = %s;
  const labels      = %s;
  const basePalette = %s;
  const noiseColor  = %s;
  const phyFrames   = %s;
  const CHECKPOINTS = %s;
  const phyXMin     = %s;
  const phyXMax     = %s;

  const slider     = document.getElementById('timeSlider');
  const clusterHost = document.getElementById('clusterPlot');
  const phyloHost   = document.getElementById('phyloPlot');

  function colorize(clusters) {
    const uniq = Array.from(new Set(clusters || [])).filter(c => c !== 'noise').sort();
    const map = new Map();
    map.set('noise', noiseColor);
    uniq.forEach((c, i) => map.set(c, basePalette[i %% basePalette.length]));
    return (clusters || []).map(c => map.get(c) || noiseColor);
  }
  const graphDiv = host => host.querySelector('.js-plotly-plot');

  function drawClusterFrame(i) {
    const g  = graphDiv(clusterHost);
    if (!g) { requestAnimationFrame(() => drawClusterFrame(i)); return; }
    const fr = frames[i] || {x:[], y:[], cluster:[]};
    Plotly.restyle(g, {
      x: [fr.x || []],
      y: [fr.y || []],
      'marker.color': [colorize(fr.cluster)],
      hovertext: [fr.cluster || []]
    }, [0]).then(() => {
      Plotly.relayout(g, {'xaxis.autorange': true, 'yaxis.autorange': true});
      Plotly.Plots.resize(g);
    });
  }

  const MAX_CONNECTORS = %d;
  const MAX_UPRIGHTS   = %d;

  function drawPhyloFrame(i) {
    const g = graphDiv(phyloHost);
    if (!g) { requestAnimationFrame(() => drawPhyloFrame(i)); return; }
    const fr = phyFrames[i] || {trunk:{x:[],y:[]}, connectors:[], uprights:[], labels:{x:[],y:[],text:[]}};

    const xs = [], ys = [], texts = [];
    const idxs = [];
    xs.push(fr.trunk.x); ys.push(fr.trunk.y); idxs.push(0);

    //connectors
    let t = 1;
    for (let c = 0; c < MAX_CONNECTORS; c++) {
      const seg = fr.connectors[c] || {x:[], y:[]};
      xs.push(seg.x); ys.push(seg.y); idxs.push(t + c);
    }
    t += MAX_CONNECTORS;

    for (let u = 0; u < MAX_UPRIGHTS; u++) {
      const seg = fr.uprights[u] || {x:[], y:[]};
      xs.push(seg.x); ys.push(seg.y); idxs.push(t + u);
    }
    t += MAX_UPRIGHTS;

    //labels
    xs.push(fr.labels.x || []); ys.push(fr.labels.y || []);
    texts.push(fr.labels.text || []); idxs.push(t);

    Plotly.restyle(g, {x: xs, y: ys, text: texts}, idxs).then(() => {
      const y = CHECKPOINTS[i] || CHECKPOINTS[0];
      const shapes = [{
        type: 'line',
        x0: phyXMin, x1: phyXMax,
        y0: y, y1: y,
        line: {width: 2, dash: 'dash', color: '#666'}
      }];
      const annotations = [{
        x: phyXMax, y: y,
        text: labels[i] || '',
        xanchor: 'left', yanchor: 'middle',
        showarrow: false,
        bgcolor: 'rgba(255,255,255,0.85)',
        bordercolor: '#ddd', borderwidth: 1, borderpad: 2
      }];

      Plotly.relayout(g, {
        'yaxis.autorange': false,
        'yaxis.range': [CHECKPOINTS[0], CHECKPOINTS[CHECKPOINTS.length-1]],
        'xaxis.fixedrange': true,
        'yaxis.fixedrange': true,
        shapes, annotations
      }).then(() => {
        fitSliderToPlot();
        Plotly.Plots.resize(g);
      });
    });
  }

  function syncFromSlider() {
    const i = parseInt(slider.value, 10) - 1;
    drawClusterFrame(i);
    drawPhyloFrame(i);
  }
   //slider
  function fitSliderToPlot(){
    const host   = phyloHost;
    const g      = graphDiv(host);
    if(!g) return;

    const plotLayer = g.querySelector('.cartesianlayer .gridlayer') || g.querySelector('.cartesianlayer');
    if(!plotLayer) return;

    const plotRect = plotLayer.getBoundingClientRect();
    const wrapRect = host.getBoundingClientRect();

    const topPad    = Math.max(0, plotRect.top - wrapRect.top);
    const bottomPad = Math.max(0, wrapRect.bottom - plotRect.bottom);

    const rail  = host.querySelector('.vslider');
    const input = rail && rail.querySelector('input[type=range]');
    if(rail && input){
      rail.style.top     = topPad + 'px';
      rail.style.bottom  = bottomPad + 'px';
      input.style.height = plotRect.height + 'px';
    }
  }

  window.addEventListener('load', () => {
    const g = graphDiv(phyloHost);
    if (g){
      g.on('plotly_afterplot', fitSliderToPlot);
      g.on('plotly_relayout',   fitSliderToPlot);
    }
    requestAnimationFrame(() => { fitSliderToPlot(); syncFromSlider(); });
  }, {once:true});

  window.addEventListener('resize', fitSliderToPlot);
  slider.addEventListener('input', syncFromSlider);
})();",
  frames_json, labels_json, palette_json, noise_json, phy_frames_js,
  checkpoints_json, phy_xmin_js, phy_xmax_js, MAX_CONNECTORS, MAX_UPRIGHTS)


#layout
dashboard <- tagList(
  tags$html(
    tags$head(
      tags$style(HTML("
        body { font-family: Arial, sans-serif; margin: 0; background: #000000; }
        .header { padding: 15px; text-align: center; font-size: 16px; }
        .container {
          display: grid;
          grid-template-columns: 2fr 1fr;
          grid-template-rows: minmax(0,1fr) minmax(0,1fr);
          gap: 20px; padding: 20px; align-items: stretch;
          height: calc(100vh - 90px);
        }
        .box {
          background: white; border-radius: 12px;
          box-shadow: 0 2px 8px rgba(0,0,0,0.1);
          padding: 10px; display: flex; flex-direction: column; min-height: 0;
        }
        h3 { margin: 0 0 8px 0; font-weight: 600; }
        .caption { font-size: 13px; color: #555; margin-top: 8px; }
        .fill { flex: 1; min-height: 0; overflow: hidden; }
        .phylo-wrap { position: relative; width: 100%; height: 100%; }
        .vslider{
          position:absolute; right:6px; top:0; bottom:0;  /* JS will set exact insets */
          width:24px; display:flex; justify-content:center; align-items:center;
          pointer-events:auto;
        }
        .vslider input[type=range]{
          writing-mode: bt-lr;
          -webkit-appearance: slider-vertical; appearance: slider-vertical;
          width:22px; height:100%; margin:0; padding:0; background:transparent;
        }
      "))
    ),
    tags$body(
      div(class="header","Individuals → clusters → branches"),
      div(class="container",
        div(class="box",
          style="grid-row: 1 / span 2; box-sizing: border-box; padding: 6px 10px 10px 10px;",
          div(
            style="transform: translate(6px, 4px);",
            h3("3D Visualisation"),
            div(class="caption","This is a 3D visualisation showcasing the canonical result of the simulation. Each dot represents and individual agent.
            You can explore detailed characteristics of an agent with a narrative story by hovering.")
          ),
            div(class="fill", style="transform: scale(0.93); transform-origin: top middle;",
                as.tags(fig3d))

        ),

        div(class="box",
          h3("Species Clusters"),
          div(class="fill",
            div(id="clusterPlot", style="width:100%; height:100%;", as.tags(species_cluster))
          )
        ),

        div(class="box",
          h3("Phylogenetic Tree (time on y)"),
          div(class="fill phylo-wrap",
            div(id="phyloPlot", style="width:100%; height:100%;", as.tags(phylo_plot)),
            div(class="vslider",
              tags$input(id="timeSlider", type="range", min="1", max=7, step="1", value="1")
            )
          ),
          div(class="caption","Slider controls both cluster view and phylogeny to the same timeframe.")
        )

      ),
      tags$script(HTML(js))
    )
  )
)

htmltools::save_html(dashboard, "dashboard.html", background = "white", libdir = "dashboard_libs")
cat("Dashboard saved as dashboard.html\n")
