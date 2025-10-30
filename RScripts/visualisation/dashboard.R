library(htmltools)

CHECKPOINTS <- seq(0L, 75000L, by = 1000L)

parent_js <- HTML(sprintf("
(function(){
  const CHECKPOINTS = %s;
  const slider       = null;
  const clusterFrame = null;
  const phyloFrame   = null;

  function getEls() {
    // late binding so we also work after load
    return {
      slider: document.getElementById('timeSlider'),
      clusterFrame: document.getElementById('clusterIframe'),
      phyloFrame: document.getElementById('phyloIframe'),
    };
  }

  function syncChildren(){
    const { slider, clusterFrame, phyloFrame } = getEls();
    const idx = parseInt(slider.value, 10) - 1;
    const cp  = CHECKPOINTS[idx];

    // tell clusters iframe to redraw for this checkpoint
    if (clusterFrame && clusterFrame.contentWindow &&
        typeof clusterFrame.contentWindow.setCheckpoint === 'function') {
      clusterFrame.contentWindow.setCheckpoint(cp);
    }

    // tell phylo iframe to move its red line to this checkpoint
    if (phyloFrame && phyloFrame.contentWindow &&
        typeof phyloFrame.contentWindow.setCheckpoint === 'function') {
      phyloFrame.contentWindow.setCheckpoint(cp);
    }
  }

  window.addEventListener('load', () => {
    const { slider } = getEls();
    slider.addEventListener('input', syncChildren);
    // initialise once
    syncChildren();
  }, {once:true});
})();
", jsonlite::toJSON(CHECKPOINTS, auto_unbox = TRUE)))

dashboard <- tagList(
  tags$html(
    tags$head(
      tags$style(HTML("
        body { font-family: Arial, sans-serif; margin: 0; background: #000000; }
        .header { padding: 15px; text-align: center; font-size: 16px; color: white; }
        .container {
          display: grid;
          grid-template-columns: 1.8fr 1.2fr;
          grid-template-rows: minmax(0,1fr) minmax(0,1fr);
          gap: 20px; padding: 20px; align-items: stretch;
          height: calc(100vh - 90px);
          box-sizing: border-box;
        }
        .box {
          background: white; border-radius: 12px;
          box-shadow: 0 2px 8px rgba(0,0,0,0.1);
          padding: 10px; display: flex; flex-direction: column; min-height: 0;
        }
        h3 { margin: 0 0 8px 0; font-weight: 600; }
        .caption { font-size: 13px; color: #555; margin-top: 8px; }
        .fill { flex: 1; min-height: 0; overflow: hidden; }
        .iframe-wrap {
          width: 100%;
          height: 100%;
          border: 0;
          border-radius: 8px;
          overflow: hidden;
          background: #fff;
        }
        .iframe-inner {
          width: 100%;
          height: 100%;
          border: 0;
        }
        .phylo-wrap { position: relative; width: 100%; height: 100%; }
        .vslider{
            margin-top: auto;
            padding-top: 6px;
            height: 30px;
            display: flex;
            justify-content: center;
            align-items: center;
          }

        .vslider input[type=range]{
          -webkit-appearance: none;
          appearance: none;
          width:90%;
          height:6px;
        }

      "))
    ),
    tags$body(
      div(class="header","Open-Ended Evolution Dashboard"),
      
      div(class="container",
          
          # LEFT big panel: 3D Tree of Life iframe
          div(
            class = "box",
            style = "grid-row: 1 / span 2; box-sizing: border-box; padding: 6px 10px 10px 10px;",
            div(
              style = "transform: translate(6px, 4px);",
              h3("Tree of Life"),
              div(
                class = "caption",
                "Each node typically represents an individual organism, and each branch represents mutation and divergence in species. Hover over each agent for descriptive information."
              )
            ),
            div(
              class = "fill",
              style = "transform: scale(0.93); transform-origin: top center;",
              div(
                class = "iframe-wrap",
                tags$video(
                  src = "movie.mp4",
                  type = "video/mp4",
                  autoplay = NA,
                  loop = NA,
                  muted = NA,
                  playsinline = NA,  # allows autoplay on mobile
                  width = "100%",
                  style = "transform: translateY(-90px) scale(0.93); transform-origin: top center;"
                )
              )
            )
          ),
          #species clusters
          # species clusters
          div(class="box",
              h3("Species Clusters"),
              div(class="caption",
                  "2D cluster of species per timestamp. Slide the time slider on the right side of the phylogenetic tree dashboard below to update this plot."
              ),
              div(class="fill",
                  div(id="clusterPlot", class="iframe-wrap",
                      tags$img(
                        id   = "clusterIframe",
                        src  = "cluster_images/1.png",
                        class= "iframe-inner",
                        style = "width: 75%; margin:auto; display: block;"
                      )
                  )
              ),
              div(class="vslider",
                  tags$input(
                    id="timeSlider",
                    type="range",
                    min="1", max="16", step="1", value="1"
                  )
              ),
              # ← add this part below
              tags$script(HTML("
      (function(){
        const slider = document.getElementById('timeSlider');
        const img    = document.getElementById('clusterIframe');
        const INTERVAL_MS = 15000;

        function setImage(){
          const idx = parseInt(slider.value, 10);
          img.src = 'cluster_images/' + idx + '.png';
        }

        function advance(){
          let idx = parseInt(slider.value, 10) + 1;
          if (idx > parseInt(slider.max, 10)) idx = parseInt(slider.min, 10);
          slider.value = idx;
          setImage();
        }

        setImage();
        slider.addEventListener('input', setImage);
        setInterval(advance, INTERVAL_MS);
      })();
    "))
          ),
          
          
          #phylogenetic tree
          div(class="box",
              h3("Phylogenetic Tree"),
              div(class="caption",
                  "Branches over evolutionary time. Red line marks selected checkpoint."),
              div(class="fill phylo-wrap",
                  div(id="phyloPlot", class="iframe-wrap",
                      tags$img(
                        id   = "phyloIframe",
                        src  = "phylogenetic_tree.png",
                        class= "iframe-inner"
                        #allowfullscreen = "true"
                      )
                  )
              )
          )
      ),
      
      tags$script(parent_js)
    )
  )
)

htmltools::save_html(
  dashboard,
  "dashboard.html",
  background = "white",
  libdir = "dashboard_libs"
)
cat("Dashboard saved as dashboard.html\n")
