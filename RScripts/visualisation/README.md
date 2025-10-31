This directory contains files that are needed for building visualisation dashboard html file.
- umap.Rmd: Creates species clusters with a umap based method.
- visualisation.Rmd: Creates 3D Tree of Life visualisation
- visualisation.R: Creates 3D Tree of Life visualisaion for dashboard.R
- speciesCluster.R: Creates species cluster with a dbscan based method for dashboard.R
- phylogeneticTree.R: Creates a phylogenetic tree for dashboard.R; currently fails to build it 
properly.

Directory structure:
  - results: HTML file that contains 3D Tree of Life visualisation will be stored here.

To publish on github pages: git subtree push --prefix RScripts/visualisation origin gh-pages
Use your username and token (not pw)
