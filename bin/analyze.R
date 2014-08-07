o <- read.csv("out/similarities.csv")

summary(o)

glm.out = glm(same_family ~ common_address_prefix, binomial(logit),
              data = o)
glm.out

png("out/similarity%1d.png", width=720, height=720, pointsize=16)
plot(glm.out)
dev.off()

