#title: EGO
#help: Efficient Global Optimization (EGO)
#tags: optimization; sparse
#author: yann.richet@irsn.fr; DiceKriging authors
#require: DiceDesign; DiceKriging; DiceEval; DiceView; pso; jsonlite
#options: search_ymin='true'; initBatchSize='4'; batchSize='4'; iterations='10'; initBatchBounds='true'; trend='y~1'; covtype='matern3_2'; knots='0'; liar='upper95'; seed='1'
#options.help: search_ymin=minimization or maximisation; initBatchSize=Initial batch size; batchSize=iterations batch size; iterations=number of iterations; initBatchBounds=add input variables bounding values (2^d combinations); trend=(Universal) kriging trend; covtype=Kriging covariance kernel; knots=number of non-stationary points for each Xi; liar=liar value for in-batch loop (when batchsize>1); seed=random seed
#input: x=list(min=0,max=1)
#output: y=0.99

EGO <- function(options) {

    library(DiceDesign)
    library(DiceKriging)
    library(DiceView)
    library(pso)
    library(jsonlite)

    ego = new.env()
    ego$i = 0

    ego$search_ymin <- as.logical(options$search_ymin)
    ego$initBatchSize <- as.integer(options$initBatchSize)
    ego$batchSize <- as.integer(options$batchSize)
    ego$iterations <- as.integer(options$iterations)
    ego$initBatchBounds <- as.logical(options$initBatchBounds)
    ego$trend <- as.formula(options$trend)
    ego$covtype <- as.character(options$covtype)
    ego$liar <- as.character(options$liar)
    ego$knots <- as.integer(unlist(strsplit(options$knots,",")))

    ego$seed <- as.integer(options$seed)
    return(ego)
}

getInitialDesign <- function(algorithm, input, output) {
    algorithm$input <- input
    algorithm$output <- output

    set.seed(algorithm$seed)

    d = length(input)
    lhs <- lhsDesign(n = algorithm$initBatchSize, dimension = d,seed=algorithm$seed)$design
    if (isTRUE(algorithm$initBatchBounds)) {
        e = c(0, 1)
        id = 1
        while (id < d) {
            e = rbind(cbind(e, 0), cbind(e, 1))
            id = id + 1
        }
        Xinit = rbind(as.matrix(e), as.matrix(lhs))
    } else {
        Xinit = as.matrix(lhs)
    }

    for (i in 1:d)
        Xinit[,i] = Xinit[,i] * (input[[i]]$max-input[[i]]$min) + input[[i]]$min
    colnames(Xinit) <- names(input)

    return(Xinit)
}

getNextDesign <- function(algorithm, X, Y) {
    if (algorithm$i >= algorithm$iterations) return()

    set.seed(algorithm$seed)

    d = dim(X)[2]
    if (dim(Y)[2] == 2) {
        algorithm$noise.var <- as.array(Y[,2])^2
    } else {
        algorithm$noise.var <- NULL
    }

    if (isTRUE(algorithm$search_ymin)) {
        y = Y[, 1]
    } else {
        y = -Y[, 1]
    }
    y = matrix(y,ncol=1)

    # heurisitc for lower bound of theta : max(1e-6, 0.1 * dX[which.max(dy/rowSums(dX))])
    dX = apply(FUN = dist, X, MARGIN = 2)
    dy = apply(FUN = dist, y, MARGIN = 2)

    # define stantionary-changing points
    all_knots <- generate_knots(knots.number = algorithm$knots, d = d, lower = sapply(algorithm$input, "[[", "min"), upper = sapply(algorithm$input, "[[", "max"))

    algorithm$model <- km(algorithm$trend, optim.method = "BFGS",
                        covtype = algorithm$covtype,
                        design = X, response = y, noise.var = algorithm$noise.var,
                        lower = pmax(1e-06, 0.1 * dX[which.max(dy/rowSums(dX)),]),
                        control = list(trace = FALSE),
                        scaling = is.list(all_knots), knots = all_knots)

    oEGO <- max_qEI(model = algorithm$model, npoints = algorithm$batchSize,
                    L = algorithm$liar,
                    lower = sapply(algorithm$input, "[[", "min"),
                    upper = sapply(algorithm$input, "[[", "max"),
                    control = list(trace = FALSE))

    if (is.null(oEGO))
        return()

    Xnext <- oEGO$par
    algorithm$i <- algorithm$i + 1

    Xnext = as.matrix(Xnext)
    colnames(Xnext) <- names(algorithm$input)
    return(Xnext)
}

displayResults <- function(algorithm, X, Y) {
    algorithm$files <- paste("view_", algorithm$i,".png", sep = "")
    resolution <- 600

    if (dim(Y)[2] == 2) {
        noise.var <- as.array(Y[, 2])^2
        yname = paste0("N(", colnames(Y)[1], ",", colnames(Y)[2],")")
    } else {
        noise.var <- NULL
        yname = colnames(Y)
    }

    if (isTRUE(algorithm$search_ymin)) {
        m = min(Y[, 1])
        x = as.matrix(X)[which(Y[, 1] == m), ]
        html = paste0(sep = "<br/>",
                     paste0("<HTML>minimum is ", m),
                     paste0(sep = "",
                           "found at <br/>",
                           paste0(collapse = "<br/>",paste(sep = "= ", names(X), x)),
                           "<br/><img src='", algorithm$files,
                           "' width='", resolution, "' height='", resolution, "'/></HTML>"))
        html = paste0(html,"<min>",m,"</min><argmin>",toJSON(x),"</argmin>")
    } else {
        m = max(Y[, 1])
        x = as.matrix(X)[which(Y[, 1] == m), ]
        html = paste0(sep = "<br/>",
                     paste0("<HTML>maximum is ", m),
                     paste0(sep = "",
                           "found at <br/>",
                           paste0(collapse = "<br/>", paste(sep = "=", names(X), x)),
                           "<br/><img src='",  algorithm$files,
                           "' width='", resolution, "' height='",  resolution, "'/></HTML>"))
        html = paste0(html,"<max>",m,"</max><argmax>",toJSON(x),"</argmax>")
    }

    if (!exists("model",envir = algorithm)) {
        png(file = algorithm$files, bg = "transparent", height = resolution, width = resolution)
        try(pairs(cbind(X,Y)))
        dev.off()
        return(html)
    }

    png(file = algorithm$files, bg = "transparent", height = resolution, width = resolution)
    try(sectionview.km(algorithm$model, center = x, Xname = colnames(X), yname = yname, yscale = ifelse(algorithm$search_ymin,1,-1)))
    dev.off()

    #if (algorithm$i == algorithm$iterations) {
        html = paste0(html,"<data_json>",toJSON(as.data.frame(cbind(X,Y)),dataframe = "columns"),"</data_json>")

        lower = sapply(algorithm$input, "[[", "min")
        upper = sapply(algorithm$input, "[[", "max")
        n = 1000
        set.seed(123) # to get the same points for evaluating model
        Xm = matrix(lower,nrow=n,ncol=length(lower),byrow = T) + matrix(upper-lower,nrow=n,ncol=length(lower),byrow = T) * matrix(runif(n*length(lower)),nrow=n,ncol=length(lower))
        colnames(Xm) <- colnames(X)
        Ym = predict(algorithm$model,newdata = Xm,type = "UK",cov.compute = F, low.memory = T)
        Ym = cbind(ifelse(algorithm$search_ymin,1,-1)*Ym$mean,Ym$sd)
        colnames(Ym) <- c(colnames(Y),paste0("sd_",colnames(Y)))[1:2]

        html = paste0(html,"<model_json>",toJSON(as.data.frame(cbind(Xm,Ym)),dataframe = "columns"),"</model_json>")
    #}

    return(paste0(html,collapse=';'))
}

displayResultsTmp <- displayResults

################### Algorithm dependencies ###################

distXmin <- function (x, Xmin){
    return(min(sqrt(rowSums((Xmin - matrix(x, nrow = nrow(Xmin), ncol = ncol(Xmin), byrow = TRUE))^2))))
}

#' @test X=matrix(runif(10),ncol=1); y=-sin(pi*X); kmi <- km(design=X,response=y); EI(runif(100),kmi)
#' @test X=matrix(runif(10),ncol=1); y=-sin(pi*X); kmi <- km(design=X,response=y); DiceView::sectionview.fun(function(x)EI(x,kmi),dim=1)
#' @test X=matrix(runif(10),ncol=2); y=branin_mod(X); kmi <- km(design=X,response=y); DiceView::contourview.fun(function(x)EI(x,kmi),dim=2)
EI <- function (x, model, plugin = NULL){
    if (is.null(plugin)) {
        if (model@noise.flag)
            plugin <- min(model@y - 2 * sqrt(model@noise.var))
        else plugin <- min(model@y)
    }
    m <- plugin
    if (!is.matrix(x))
        x <- matrix(x, ncol = model@d)
    d <- ncol(x)
    if (d != model@d)
        stop("x does not have the right number of columns (", d, " instead of ", model@d, ")")

    newdata <- x
    colnames(newdata) = colnames(model@X)
    predx <- predict.km(object = model, newdata = newdata, type = "UK", checkNames = FALSE)
    kriging.mean <- predx$mean
    kriging.sd <- predx$sd
    xcr <- (m - kriging.mean)/kriging.sd
    xcr.prob <- pnorm(xcr)
    xcr.dens <- dnorm(xcr)
    res <- (m - kriging.mean) * xcr.prob + kriging.sd * xcr.dens
    too.close = which(kriging.sd/sqrt(model@covariance@sd2) < 1e-06)
    res[too.close] <- max(0, m - kriging.mean)
    return(res)
}

generate_knots <- function (knots.number = NULL, d, lower = NULL, upper = NULL){
    if (is.null(lower))
        lower <- rep(0, times = d)
    if (is.null(upper))
        upper <- rep(1, times = d)
    if (is.null(knots.number))
        return(NULL)
    if (length(knots.number) == 1) {
        if (knots.number > 1) {
            knots.number <- rep(knots.number, times = d)
        } else {
            return(NULL)
        }
    }
    if (length(knots.number) != d) {
        print("Error in function generate_knots. The size of the vector knots.number needs to be equal to d")
        return(NULL)
    }
    knots.number <- pmax(1, knots.number)
    thelist <- NULL
    for (i in 1:d) {
        thelist[[i]] <- seq(from = lower[i], to = upper[i], length = knots.number[i])
    }
    return(thelist)
}

#' @test set.seed(1); X=matrix(runif(20),ncol=2); y=branin(X); kmi <- km(design=X,response=y); kmi=km(design=X,response=y); DiceView::contourview.fun(function(x)EI(x,kmi),dim=2); points(max_EI(kmi,lower=c(0,0),upper=c(1,1))$par)
max_EI <- function (model, lower, upper, control = NULL){
    d <- ncol(model@X)
    if (is.null(control$print.level))
        control$print.level <- 1
    if (is.null(control$max.parinit.iter))
        control$max.parinit.iter <- 10^d
    if (d <= 6)
        N <- 10 * 2^d
    else N <- 100 * d
    if (is.null(control$pop.size))
        control$pop.size <- N
    if (is.null(control$solution.tolerance))
        control$solution.tolerance <- 1e-15
    pars = NULL
    for (i in 1:d) pars = cbind(pars, matrix(runif(N, lower[i], upper[i]), ncol = 1))
    ei <- EI(pars, model)
    good_start = which(ei == max(ei, na.rm = T))
    par0 = matrix(pars[good_start[sample(1:length(good_start), 1)], ], nrow = 1)
    o <- psoptim(par = par0, fn = function(x) {
        EI(x, model)
    }, lower = lower, upper = upper,
    control = list(fnscale = -1, trace = control$print.level, maxit = 10 * d))
    o$par <- t(as.matrix(o$par))
    colnames(o$par) <- colnames(model@X)
    o$value <- as.matrix(o$value)
    colnames(o$value) <- "EI"
    return(list(par = o$par, value = o$value, counts = o$counts,par.all = o$par.all))
}

#' @test set.seed(1); X=matrix(runif(20),ncol=2); y=apply(FUN=branin,X,1); kmi <- km(design=X,response=y);  kmi=km(design=X,response=y); DiceView::contourview.fun(function(x)EI(x,kmi),dim=2); points(max_qEI(kmi,npoints=5,L="upper95",lower=c(0,0),upper=c(1,1))$par)
max_qEI <- function (model, npoints, L, lower, upper, control = NULL, ...){
    n1 <- nrow(model@X)
    for (s in 1:npoints) {
        oEGO <- max_EI(model = model, lower = lower, upper = upper,
                       control, ...)
        if (distXmin(oEGO$par, model@X) <= prod(upper - lower) * 1e-10) {
            warning("Proposed a point already in design !")
            npoints = s - 1
            break
        }
        model@X <- rbind(model@X, oEGO$par)
        if (L == "min")
            l = min(model@y)
        else if (L == "max")
            l = max(model@y)
        else if (L == "upper95")
            l = predict.km(object = model, newdata = oEGO$par,
                           type = "UK", light.return = TRUE)$upper95
        else if (L == "lower95")
            l = predict.km(object = model, newdata = oEGO$par,
                           type = "UK", light.return = TRUE)$lower95
        else l = L
        model@y <- rbind(model@y, l, deparse.level = 0)
        model@F <- trendMatrix.update(model, Xnew = data.frame(oEGO$par))
        if (model@noise.flag) {
            model@noise.var = c(model@noise.var, 0)
        }
        newmodel = NULL
        try(newmodel <- computeAuxVariables(model))
        if (is.null(newmodel)) {
            warning("Unable to update model !")
            npoints = s - 1
            break
        }
        model = newmodel
    }
    if (npoints == 0)
        return()
    return(list(par = model@X[(n1 + 1):(n1 + npoints), , drop = FALSE],
                value = model@y[(n1 + 1):(n1 + npoints), , drop = FALSE]))
}
