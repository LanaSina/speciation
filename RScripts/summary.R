
folder = "/Users/lana/Desktop/prgm/old/oee-old-01/0_OEE/2015_10_05_01_19"

# honestly this one seems to be the best even if the video is not great
# folder = "/Users/lana/Desktop/prgm/old/oee-old-01/0_OEE/2015_10_05_01_19"

fileName = paste(folder,"SummaryIndividuals.csv",sep="/")
whole_world <- read.csv(fileName, stringsAsFactors=F, dec =".", header=TRUE)

# plot(x = world$created, y=world$ancestor, main = folder)#, xlim = c(100,500) ,ylim = c(5000,10000))

# create a vector the same length as the dataframe
sample_vector=c(1:nrow(whole_world))

# sample elements from the vector (in this example 30 elements sampled without replacement)
the_sample = sample(sample_vector, 30000, replace=FALSE)

# the vector of randomly selected elements is then used to select rows from the dataframe
world = whole_world[the_sample,]

plot(x = world$created, y=world$speed, main = folder)
plot(x = world$created, y=world$lifeSpan, main = folder)
plot(x = world$created, y=world$sensors, main = folder)
plot(x = world$created, y=world$kidEnergy, main = folder)
plot(x = world$created, y=world$maxEnergy, main = folder)
plot(x = world$created, y=world$nkids, main = folder)
plot(x = world$created, y=world$pgmDeath, main = folder)

#no install.packages("scatterplot3d")
library(scatterplot3d)
# install.packages("rgl")
# library(rgl)

#calculate colors!
colors = rgb(world$speed/max(world$speed), world$maxEnergy/max(world$maxEnergy), world$nkids/max(world$nkids))
scatterplot3d(x= world$created, y=world$maxEnergy, z = world$kidEnergy, color = colors, main = folder)

#plot3d(x= world$created, y=world$maxEnergy, z = world$kidEnergy, col = colors, main = folder)
plot(x = world$created, y=world$maxEnergy, col = colors)

browseURL(paste("file://", writeWebGL(dir=file.path("../3Dplots", "real_branches_tree"), width=800), sep=""))
# 
# writeWebGL(dir = "..\\3Dplots", filename = file.path(dir, "index.html"), 
#            template = system.file(file.path("WebGL", "template.html"), package = "rgl"),
#            prefix = "",
#            snapshot = TRUE, font = "Arial")

me1 = subset(world, maxEnergy>3)
me2 = subset(world, maxEnergy<=3)
plot(x = me1$created, y=me1$kidEnergy, col="blue")
points(x = me2$created, y=me2$kidEnergy, col="red")

plot(x=me1$ID, y = rep.int(1,length(me1$ID)), ylim=c(0,3))
points(x=me2$ID, y= rep.int(2,length(me2$ID)))
for(i in 1:length(me1$ID)){
  parX = subset(me2, ID == me1$parent[i])
  if(length(parX$ID)>0){
    points(x = c(me1$ID[i],parX$ID), y=c(2,1), type="l") 
  }
}


plot(x=world$ID, y=world$speed)

fili = subset(world, ancestor=="46597")
fili = subset(world, created>"4000")
fili = subset(world, ancestor<6000)
fili = subset(fili, ancestor>100000)
fili = subset(fili, ancestor<125000)
fili = subset(fili, created>200)

plot(x = fili$created, y=fili$ancestor)#, ylim=c(40000,50000))
plot(x = fili$created, y=fili$sensors)
plot(x = fili$created, y=fili$kidEnergy)

plot(x = fili$created, y=fili$maxEnergy)
plot(x = fili$created, y=fili$speed)
plot(x = fili$created, y=fili$parent)


plot(x = fili$ID, y=fili$maxEnergy)
#max energy 6 and 7
subA = subset(fili, maxEnergy==6)
subB = subset(fili, maxEnergy==5)
#plot(x = subA$created, y=subB$maxEnergy)

for(i in 1:length(subB$ID)){
  parX = subset(subA, ID == subB$parent[i])
  if(length(parX$ID)>0){
    points(x = c(subB$ID[i],parX$ID), y=c(subB$maxEnergy[i],parX$maxEnergy), type="l", col="red") 
  }
}




abline(v=1457)

notFili = subset(world, ancestor!=5920)
plot(x = notFili$created, y=notFili$lifeSpan)
abline(v=1457)
plot(x = notFili$created, y=notFili$speed)
abline(v=1457)
plot(x = notFili$created, y=notFili$sensors)
abline(v=1457)
plot(x = notFili$created, y=notFili$kidEnergy)
abline(v=1457)
