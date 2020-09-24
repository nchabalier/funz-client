sobol <- function (x)
{
    a <- c(0, 1, 4.5, 9, 99, 99, 99, 99)
    y <- 1
    for (j in 1:8) y <- y * (abs(4 * x[, j] - 2) + a[j])/(1 +
        a[j])
    y
}

cat('z = ',sobol(cbind(!x1 , !x2 , !x3 , !x4 , !x5 , !x6 , !x7 , !x8 )),'\n');
