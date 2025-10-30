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
          grid-template-columns: 2fr 1fr;
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
          position:absolute;
          bottom:6px; left:0; right:0;
          height:30px;
          display:flex; justify-content:center; align-items:center;
          pointer-events:auto;
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
          div(class="box",
              style="grid-row: 1 / span 2; box-sizing: border-box; padding: 6px 10px 10px 10px;",
              div(
                style="transform: translate(6px, 4px);",
                h3("トルシム惑星の生命の樹の進化"),
                div(class="caption",
                    "一つの点が一匹の生き物を表している。時間が経つと、体の大きさ、動きの速さ、寿命、賢さ、子供の数が進化によって変化する。ここでは、体の大きさと寿命を表示している。１つの種(スピーシーズ)から新種が進化したところに、木が枝分かれをしている。"
                    )
              ),
              div(class="fill",
                  style="transform: scale(0.93); transform-origin: top middle;",
                  div(class="iframe-wrap",
                      tags$iframe(
                        src = "results/tree3d.html",
                        class = "iframe-inner",
                        allowfullscreen = "true"
                      )
                  )
              )
          ),
          #species clusters
          div(class="box",
              h3("新種の誕生"),
              div(class="caption",
                  "突然変異によって、生き物は自分と少し違う子供を産む。その「ちょっと違う」が生存に役立つ変異の場合、子供が生き残って、自分の子供を産んで、数世代で「大分違う」生き物になる。"),
              div(class="fill",
                  div(id="clusterPlot", class="iframe-wrap",
                      tags$img(
                        id   = "clusterIframe",
                        src  = "speciesCluster.png",
                        class= "iframe-inner"
                        #allowfullscreen = "true"
                      )
                  )
              )
          ),
          
          #phylogenetic tree
          div(class="box",
              h3("Phylogenetic Tree"),
              div(class="caption",
                  "系統樹。生物の進化の道筋"),
              div(class="fill phylo-wrap",
                  div(id="phyloPlot", class="iframe-wrap",
                      tags$img(
                        id   = "phyloIframe",
                        src  = "phylogenetic_tree.png",
                        class= "iframe-inner"
                        #allowfullscreen = "true"
                      )
                  ),
                  div(class="vslider",
                      tags$input(
                        id="timeSlider", type="range",
                        min="1", max=length(CHECKPOINTS), step="1", value="1")
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
