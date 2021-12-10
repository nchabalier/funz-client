#help: Uniform random sampling
#tags: uncertainties propagation
#options: sample_size='100'
#options.help: sample_size='Size of sample to perform'
#input: x=list(min=0,max=1)
#output: y=0.01

RandomUnif <- function(options) {
    unif = new.env()
    unif$sample_size <- as.integer(options$sample_size)
    return(unif)
}

#' first design building.
#' @param input variables description (min/max, properties, ...)
#' @param output values of interest description
getInitialDesign <- function(unif,input,output) {
  unif$input <- input
  unif$output <- output
  d = length(input)
  x = runif(d*unif$sample_size)
  x = matrix(x,ncol=d)
  names(x) <- names(input)
  return(from01(x,input))
}

## iterated design building.
## @param X data frame of current doe variables
## @param Y data frame of current results
## @return data frame or matrix of next doe step
getNextDesign <- function(unif, X, Y) {
    return(NULL)
}

## final analysis. Return HTML string
## @param X data frame of doe variables
## @param Y data frame of results
## @return HTML string of analysis
displayResults <- function(unif, X, Y) {

    dimY = dim(Y)[2]

    unif$files <- NULL
    html = "<HTML name='Sample'>"
    samples = ""
    sample = ""
    Ynames = colnames(Y)
    if (is.null(Ynames) || !(length(Ynames) == dimY)) {
      for (i in 1:dimY) {
        Ynames[i] = paste0(unif$output,i)
      }
    }

    for (i in 1:dimY) {
        f = paste0("hist_",Ynames[i],".png",sep="")
        unif$files <- c(unif$files,f)
        png(file = f, height = 600, width = 600)
        hist(Y[,i], pch = 20)
        dev.off()

        html <- paste0(html,'<br/>',
            '<img src="',  f,  '" width="600" height="600"/>')

        s <- paste0("<sample_",i," name='",Ynames[i],"'>")
        s <- paste0(s, paste0(collapse=",",Y[,i]))
        s <- paste0(s, "</sample_",i,">")
        samples <- paste0(samples, s)

        sample <- paste0(sample,"[",paste0(collapse=",",Y[,i]),"],")
    }
    html <- paste0(html,"</HTML>")

    return(paste0(html,samples, paste0("<sample>[",sample,"]</sample>")))
}

displayResultsTmp <- displayResults

from01 = function(X, inp) {
  namesX=names(X)
  for (i in 1:ncol(X)) {
    namei = namesX[i]
    X[,i] = X[,i] * (inp[[ namei ]]$max-inp[[ namei ]]$min) + inp[[ namei ]]$min
  }
  return(X)
}

to01 = function(X, inp) {
  for (i in 1:ncol(X)) {
    namei = names(X)[i]
    X[,i] = (X[,i] - inp[[ namei ]]$min) / (inp[[ namei ]]$max-inp[[ namei ]]$min)
  }
  return(X)
}

##############################################################################################
# @test
# f <- function(X) matrix(Vectorize(function(x) {((x+5)/15)^3})(X),ncol=1)
#
# options = list(sample_size=100)
# b = RandomUnif(options)
#
# X0 = getInitialDesign(b, input=list(x=list(min=-5,max=10)), "y")
# Y0 = f(X0)
# Xi = X0
# Yi = Y0
#
# finished = FALSE
# while (!finished) {
#     Xj = getNextDesign(b,Xi,Yi)
#     if (is.null(Xj) | length(Xj) == 0) {
#         finished = TRUE
#     } else {
#         Yj = f(Xj)
#         Xi = rbind(Xi,Xj)
#         Yi = rbind(Yi,Yj)
#     }
# }
#
# print(displayResults(b,Xi,Yi))
