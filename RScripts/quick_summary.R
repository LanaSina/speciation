

  # OEE data analysis

  ## setup


  options(repos = c(CRAN = "https://cloud.r-project.org"))
  library(scatterplot3d)
  if (!requireNamespace("rgl", quietly = TRUE)) {
    install.packages("rgl", type = "source")
  }
  library(rgl)


# replace with your base folder
base_folder <- "/Users/hyoyeon/Desktop/Career/Sony/speciation/data"


## Read agents data
folder <- "2025_07_07_23_23"
fileName <- file.path(base_folder, folder, "SummaryIndividuals/SummaryIndividuals_0.csv")

whole_world <- read.csv(fileName, stringsAsFactors=F, dec =".")#, header=TRUE)
world <- whole_world


## Visualization


# plot tree

# # display data as is, or extract a subset
# world = subset(world, created >90000 & created < 110000 & maxEnergy>50)

# # random subset
sample_vector=c(1:nrow(whole_world))
# sample elements from the vector (in this example 30 elements sampled without replacement)
the_sample = sample(sample_vector, 5000, replace=FALSE)
# the vector of randomly selected elements is then used to select rows from the dataframe
world = whole_world[the_sample,]

colors = rgb(world$speed/max(world$speed), world$maxEnergy/max(world$maxEnergy), world$kidEnergy/max(world$kidEnergy))
close3d()
plot3d(x= world$created, y=world$pgmDeath, z = world$maxEnergy,
      col = colors, main = folder) # xlim=c(0,max(world$created)),
rglwidget()



# old_world <- whole_world




# close3d()
# plot3d(x= old_world$created, y=old_world$pgmDeath, z = old_world$maxEnergy,
#        col = "black")
# points3d(x= whole_world$created, y=whole_world$pgmDeath, z = whole_world$maxEnergy,
#        col = "red", main = folder, xlim = c(0, max(whole_world$created)))
#
# rglwidget()




# plot each variable separately
# par(mfrow=c(2,3))
plot(x = world$created, y=world$speed, main = folder)
plot(x = world$created, y=world$lifeSpan, main = folder)
plot(x = world$created, y=world$sensors, main = folder)
plot(x = world$created, y=world$kidEnergy, main = folder)
plot(x = world$created, y=world$maxEnergy, main = folder)
# plot(x = world$created, y=world$hunger, main = folder)
plot(x = world$created, y=world$nkids, main = folder)
plot(x = world$created, y=world$pgmDeath, main = folder)
#plot(x = world$created, y=world$matForKids, main = folder)



#  Plot number of agents

# library(data.table)
#
# populationFileName = file.path(base_folder, folder,"Population.csv")
# population <- fread(populationFileName, dec =".")
#
# plot(population, type = "l", main="Number of agents")
#
#
# plot(x = world$created, y=world$maxEnergy, main = folder)
# points(n_agents_t$t, n_agents_t$n/10, type = "l", col="blue")
