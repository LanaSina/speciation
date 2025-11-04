library(htmltools)

CHECKPOINTS <- seq(0L, 75000L, by = 1000L)

parent_js <- HTML(sprintf("
(function(){
  const CHECKPOINTS = %s;
  const clusterFrame = null;
  const phyloFrame   = null;

  function getEls() {
    // late binding so we also work after load
    return {
      clusterFrame: document.getElementById('clusterIframe'),
      phyloFrame: document.getElementById('phyloIframe'),
    };
  }

  function syncChildren(){
    const {clusterFrame, phyloFrame} = getEls();
    const idx = parseInt(slider.value, 10) - 1;
    const cp  = CHECKPOINTS[idx];

    // tell clusters iframe to redraw for this checkpoint
    if (clusterFrame && clusterFrame.contentWindow &&
        typeof clusterFrame.contentWindow.setCheckpoint === 'function') {
      clusterFrame.contentWindow.setCheckpoint(cp);
    }

    // tell phylo iframe to redraw for this checkpoint
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

      "))
    ),
    tags$body(
      div(class="header", "Tree of Life Simulation",
      tags$div(
        tags$button(
          "Info",
          onclick = "window.location.href='../html/details.html';",
          style = "font-size:12px; margin-top:8px; padding:4px 8px;"
        )
      )),
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
            div(class="fill",
                style="transform: scale(0.93); transform-origin: top middle;",
                div(class="iframe-wrap",
                    tags$iframe(
                      src = "../html/tree3d.html",
                      class = "iframe-inner",
                      allowfullscreen = "true"
                    )
                )
            )
          ),
          #species clusters
          # species clusters
          div(class="box",
              h3("Species Clusters"),
              div(class="caption",
                  "This is a 2D cluster of species of a particular timestamp. Slide the time slider on the right panel of the phylogenetic tree dashboard below to update this plot."
              ),
              div(class="fill",
                  div(id="clusterPlot",
                      class="iframe-wrap",
                      tags$iframe(
                        id   = "clusterIframe",
                        src  = "../cluster_html/clusters_cp_0.html",
                        class= "iframe-inner",
                        style = "object-fit: contain;"
                      )
                  )
              ),
              tags$script(HTML("
              (function(){
                const img    = document.getElementById('clusterIframe');
                const INTERVAL_MS = 15000;
                const MAX_IDX = 15;  
                let idx = 0;         
        
                function setImage(){
                 img.src = '../cluster_html/clusters_cp_' + idx * 5000 + '.html?v=' + Date.now(); // cache-bust
                }   

                function advance(){
                  idx = (idx % MAX_IDX) + 1;
                  setImage();
                }

                setImage();
                setInterval(advance, INTERVAL_MS);

              })();
            "))),
          
          
          #phylogenetic tree
          div(class="box",
              h3("Phylogenetic Tree"),
              div(class="caption",
                  "Branches over evolutionary time. Red line marks selected checkpoint."),
              div(class="fill phylo-wrap",
                  div(id="phyloPlot", class="iframe-wrap",
                      tags$img(
                        id   = "phyloIframe",
                        src  = "../phylogentic_images/1.png",
                        class= "iframe-inner",
                        style = "object-fit: contain;"
                      )
                  )
              )
          )
      ),
      tags$script(HTML("
      (function(){
        const img = document.getElementById('phyloIframe');
        const INTERVAL_MS = 15000;
        const MAX_IDX = 16;
        let idx = 1;
    
        function setImage(){
          img.src = '../phylogenetic_images/' + idx + '.png';
        }
    
        function advance(){
          idx++;
          if (idx > MAX_IDX) idx = 1;
          setImage();
        }
    
        setImage();
        setInterval(advance, INTERVAL_MS);
      })();
    "))
    )
  )
)

htmltools::save_html(
  dashboard,
  "../html/dashboard.html",
  background = "white",
  libdir = "dashboard_libs"
)
cat("Dashboard saved as dashboard.html\n")
