ishigami <- function (x)
{
    A <- 7
    B <- 0.1
    sin(x[, 1]) + A * sin(x[, 2])^2 + B * x[, 3]^4 * sin(x[,1])
}

cat("z = ",ishigami(cbind(!x1 , !x2 , !x3 )),"\n");
