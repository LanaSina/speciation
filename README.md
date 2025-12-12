## Instructions

1. Download `phylogenetic_images` from [here](https://drive.google.com/drive/u/0/folders/1bdElZ2bmuq3oTnYUewzuvmNv5dkze5vB).

2. Place the folder inside the following directory: `speciation/demo`

3. Run the dashboard by executing the following command in your terminal:
```bash
Rscript RScripts/dashboard.R
```
This will create `dashboard.html` in `speciation/demo/html`.


----

Deploying to github pages:

1. Create a copy of `dashboard.html` as `index.html`

To push a subfolder as root of the website:

2. `git subtree push --prefix demo origin gh-pages`