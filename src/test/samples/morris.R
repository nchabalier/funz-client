b0 <- rnorm(1)
b1 <- c(rep(20, 10), rnorm(10))
b2 <- rbind(cbind(matrix(-15, 6, 6),
                  matrix(rnorm(6 * 14), 6, 14)),
            matrix(rnorm(14 * 20), 14, 20))
b3 <- array(0, c(20, 20, 20))
b3[1 : 5, 1 : 5, 1 : 5] <- - 10
b4 <- array(0, c(20, 20, 20, 20))
b4[1 : 4, 1 : 4, 1 : 4, 1 : 4] <- 5

morris <- function (x)
{
    w <- 2 * (x - 0.5)
    w[, c(3, 5, 7)] <- 2 * (1.1 * x[, c(3, 5, 7)]/(x[, c(3, 5,
        7)] + 0.1) - 0.5)
    y <- b0
    for (i in 1:20) y <- y + b1[i] * w[, i]
    for (i in 1:19) for (j in (i + 1):20) y <- y + b2[i, j] *
        w[, i] * w[, j]
    for (i in 1:18) for (j in (i + 1):19) for (k in (j + 1):20) y <- y +
        b3[i, j, k] * w[, i] * w[, j] * w[, k]
    for (i in 1:17) for (j in (i + 1):18) for (k in (j + 1):19) for (l in (k +
        1):20) y <- y + b4[i, j, k, l] * w[, i] * w[, j] * w[,
        k] * w[, l]
    y
}

#Sys.sleep(5);

cat('z = ',morris(cbind(!x1 , !x2 , !x3 , !x4 , !x5 , !x6 , !x7 , !x8 , !x9 , !x10 , !x11 , !x12 , !x13 , !x14 , !x15 , !x16 , !x17 , !x18 , !x19 , !x20 )),'\n');
