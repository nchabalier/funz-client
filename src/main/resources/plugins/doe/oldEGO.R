#help=Efficient Global Optimization (EGO) algorithm
#type=Optimization
#output=Optimum
#options=initBatchSize=4,batchSize=4,iterations=10,bounds='true',trend='y~1',covtype='matern3_2',liar='max',search_min='true'
#require=DiceDesign,DiceKriging,DiceView

#' constructor and initializer of R session
init <- function() {
  library(DiceDesign)
  library(DiceKriging)  
  library(DiceView)
  
  # all parameters are initialy strings, so you have to put as global non-string values
  initBatchSize <<- as.integer(initBatchSize)
  batchSize <<- as.integer(batchSize)
  iterations <<- as.integer(iterations)
  bounds <<- as.logical(bounds)
  
  trend <<- as.formula(trend)
  
  search_min <<- as.logical(search_min)
}

#' first design building. All variables are set in [0,1]. d is the dimension, or number of variables
#' @param d number of variables
#' @returnType matrix
#' @return next design of experiments
initDesign <- function(d) {
  set.seed(1)
  lhs = lhsDesign(n=initBatchSize,dimension=d)$design
  if (bounds) {
    e=c(0,1)
    id=1
    while(id<d){
      e=rbind(cbind(e,0),cbind(e,1))
      id=id+1
    }
    Xinit=rbind(as.matrix(e),as.matrix(lhs))
  } else {
    Xinit=as.matrix(lhs)
  }
  return(Xinit)
}

#' iterated design building.
#' @param X data frame of current doe variables (in [0,1])
#' @param Y data frame of current results
#' @returnType data frame or matrix
#' @return  next doe step
nextDesign <- function(X,Y) {
  if(!exists("iEGO")) iEGO <<- 0
  if (iEGO > iterations) return();
  
  d = dim(X)[2]
  if (dim(Y)[2] == 2) {
    noise.var <<- as.array(Y[,2])^2
  } else {
    noise.var <<- NULL
  }
  
  if (search_min) {y=Y[,1]} else {y=-Y[,1]}
  kmi <<- km(control=list(trace=FALSE),trend,optim.method='BFGS',penalty = NULL,covtype=covtype,nugget.estim = FALSE, noise.var = noise.var,design=X,response=y)
  
  EGOi = NULL
  try(EGOi <- max_qEI(model=kmi,npoints=batchSize,L=liar,lower=rep(0,d),upper=rep(1,d),control=list(trace=FALSE)))
  if (is.null(EGOi)) return(NULL)
  
  Xnext <<- EGOi$par
  
  iEGO <<- iEGO + 1
  return(as.matrix(Xnext))
}

#' final analysis. All variables are set in [0,1]. Return HTML string
#' @param X data frame of doe variables (in [0,1])
#'q @param Y data frame of results
#' @returnType String
#' @return HTML string of analysis
analyseDesign <- function(X,Y) {
  if(!exists("iEGO")) iEGO <<- 0

  analyse.files <<- paste("sectionview_",iEGO-1,".png",sep="")
  resolution <- 600
  
  if (dim(Y)[2] == 2) {
    noise.var <<- as.array(Y[,2])^2
    yname=paste0("N(",names(Y)[1],",",names(Y)[2])
  } else {
    noise.var <<- NULL
    yname=names(Y)
  }

  if (search_min) {
    m = min(Y[,1])
    x = as.matrix(X)[which(Y[,1]==m),]
    html=paste(sep="<br/>",paste("<HTML>minimum is ",m),paste(sep="","found at ",paste(collapse="<br/>",paste(sep="= ",names(x),x)),"<br/><img src='",analyse.files,"' width='",resolution,"' height='",resolution,"'/></HTML>"))
    plot=paste("<Plot1D name='min'>",m,"</Plot1D>")
    
    d = dim(X)[2]
    if (d == 1) {
      plotx=paste("<Plot1D name='argmin'>",paste(x),"</Plot1D>")
    } else if (d == 2) {
      plotx=paste("<Plot2D name='argmin'>(",paste(collapse=",",x),")</Plot2D>")
    } else {
      plotx=paste("<PlotnD name='argmin'>(",paste(collapse=",",x),")</PlotnD>")
    }
  } else {
    m = max(Y[,1])
    x = as.matrix(X)[which(Y[,1]==m),]
    html=paste(sep="<br/>",paste("<HTML>maximum is ",m),paste(sep="","found at ",paste(collapse="<br/>",paste(sep="= ",names(x),x)),"<br/><img src='",analyse.files,"' width='",resolution,"' height='",resolution,"'/></HTML>"))
    plot=paste("<Plot1D name='max'>",m,"</Plot1D>")
    
    d = dim(X)[2]
    if (d == 1) {
      plotx=paste("<Plot1D name='argmax'>",paste(x),"</Plot1D>")
    } else if (d == 2) {
      plotx=paste("<Plot2D name='argmax'>(",paste(collapse=",",x),")</Plot2D>")
    } else {
      plotx=paste("<PlotnD name='argmax'>(",paste(collapse=",",x),")</PlotnD>")
    }
  }
  
  model <- km(control=list(trace=FALSE),trend,optim.method='BFGS',penalty = NULL,covtype=covtype, noise.var = noise.var,design=X,response=Y[,1])
  png(file=analyse.files,bg="transparent",height=resolution,width = resolution)
  try(sectionview.km(model,center=x,Xname=names(X),yname=yname))
  dev.off()

  #pairs.png <- paste("pairs",i-1,".png",sep="")
  #png(file=pairs.png,bg="transparent",height=height,width = width)
  #y=Y[,1]
  #if (search_min) 
  #red = (as.matrix(y)-min(y))/(max(y)-min(y)) 
  #else red = (max(y)-as.matrix(y))/(max(y)-min(y))
  #alpha = 1
  #try(if (max(y)-min(y)>0) alpha = 1-red else alpha=0)
  #if(any(is.nan(alpha))) alpha=0
  #if(any(is.nan(red))) red=0
  #pairs(X,col=rgb(r=red,g=0,b=1-red,a=alpha))
  #dev.off()
  #analyse.files <<- c(analyse.files,pairs.png)

  return(paste(html,plot,plotx,collapse=';'))
}

#' temporary analysis. All variables are set in [0,1]. Return HTML string
#' @param X data frame of doe variables (in [0,1])
#' @param Y data frame of  results
#' @returnType String
#' @return HTML string of analysis
analyseDesignTmp <- function(X,Y) {
  analyseDesign(X,Y)
}


################################################################################

#' @test X=matrix(runif(10),ncol=1); y=-sin(pi*X); kmi <- km(design=X,response=y); EI(runif(100),kmi)
#' @test X=matrix(runif(10),ncol=1); y=-sin(pi*X); kmi <- km(design=X,response=y); DiceView::sectionview.fun(function(x)EI(x,kmi),dim=1)
#' @test X=matrix(runif(10),ncol=2); y=branin_mod(X); kmi <- km(design=X,response=y); DiceView::contourview.fun(function(x)EI(x,kmi),dim=2)
EI <- function (x, model, plugin=NULL) {
  
  if (is.null(plugin)){ if (model@noise.flag) plugin <- min(model@y-2*sqrt(model@noise.var)) else plugin <- min(model@y) }
  m <- plugin
  
  ########################################################################################
  # Convert x in proper format(s)
  if (!is.matrix(x)) x <- matrix(x,ncol= model@d)
  d <- ncol(x)
  if (d != model@d){ stop("x does not have the right number of columns (",d," instead of ",model@d,")") }
  newdata <- x
  colnames(newdata) = colnames(model@X) 
  
  ########################################################################################
  #cat("predict...")
  predx <- predict.km(object=model, newdata=newdata, type="UK", checkNames = FALSE)
  #cat(" done.\n")
  kriging.mean <- predx$mean
  kriging.sd   <- predx$sd
  
  xcr <- (m - kriging.mean)/kriging.sd
  
  xcr.prob <- pnorm(xcr)
  xcr.dens <- dnorm(xcr)          
  res <- (m - kriging.mean) * xcr.prob + kriging.sd * xcr.dens
  
  too.close = which(kriging.sd/sqrt(model@covariance@sd2) < 1e-06)
  res[too.close] <- max(0,m - kriging.mean)
  
  return(res)
}

#' @test set.seed(1); X=matrix(runif(20),ncol=2); y=branin(X); kmi <- km(design=X,response=y); kmi=km(design=X,response=y); DiceView::contourview.fun(function(x)EI(x,kmi),dim=2); points(max_EI(kmi,lower=c(0,0),upper=c(1,1))$par)
max_EI <-function(model,  lower, upper, control=NULL) {
  
  d <- ncol(model@X)
  
  if (is.null(control$print.level)) control$print.level <- 1
  if (is.null(control$max.parinit.iter)) control$max.parinit.iter <- 10^d
  if(d<=6) N <- 10*2^d else N <- 100*d 
  if (is.null(control$pop.size))  control$pop.size <- N
  if (is.null(control$solution.tolerance))  control$solution.tolerance <- 1e-15  
  
  pars=NULL
  for (i in 1:d) pars=cbind(pars,matrix(runif(N,lower[i],upper[i]),ncol=1))
  
  #t=Sys.time()
  ei <- EI(pars,model)
  #print(capture.output(Sys.time()-t))
  print(cbind(pars,ei))
  
  good_start = which(ei==max(ei,na.rm=T))
  par0=matrix(pars[good_start[sample(1:length(good_start),1)],],nrow=1)
  
  o <- psoptim(par=par0,fn=function(x){
    EI(x,model)
  },lower=lower,upper=upper,
  #control=list(vectorize=TRUE, fnscale=-1, trace=control$print.level, hybrid=FALSE, s=control$pop.size, abstol=control$solution.tolerance,maxit=10*d))
  control=list( fnscale=-1, trace=control$print.level,maxit=10*d))
  
  o$par <- t(as.matrix(o$par))
  colnames(o$par) <- colnames(model@X)
  o$value <- as.matrix(o$value)
  colnames(o$value) <- "EI"
  
  print(o)
  
  return(list(par=o$par, value=o$value, counts=o$counts,par.all=o$par.all))
}

#' @test set.seed(1); X=matrix(runif(20),ncol=2); y=apply(FUN=branin,X,1); kmi <- km(design=X,response=y);  kmi=km(design=X,response=y); DiceView::contourview.fun(function(x)EI(x,kmi),dim=2); points(max_qEI(kmi,npoints=5,L="upper95",lower=c(0,0),upper=c(1,1))$par)
max_qEI <- function(model, npoints, L,  lower, upper,  control=NULL, ...) {
  n1 <- nrow(model@X)
  for (s in 1:npoints) {
    oEGO <- max_EI(model=model, lower=lower, upper=upper, control, ...)
    
    if (distXmin(oEGO$par,model@X)<=prod(upper-lower)*1E-10) {warning("Proposed a point already in design !");npoints=s-1;break;}
    
    model@X <- rbind(model@X, oEGO$par)
    
    if (L=="min")
      l = min(model@y)
    else if (L=="max")
      l = max(model@y)
    else if (L=="upper95") 
      l = predict.km(object = model,newdata = oEGO$par,type="UK",light.return = TRUE)$upper95
    else if (L=="lower95")
      l = predict.km(object = model,newdata = oEGO$par,type="UK",light.return = TRUE)$lower95
    else l = L
    
    model@y <- rbind(model@y, l, deparse.level=0)
    
    model@F <- trendMatrix.update(model, Xnew=data.frame(oEGO$par))
    if (model@noise.flag) { 
      model@noise.var = c(model@noise.var, 0) # here is the fix!
    }
    newmodel = NULL
    try(newmodel <- computeAuxVariables(model))
    if (is.null(newmodel)) {warning("Unable to update model !");npoints=s-1;break;}
    model = newmodel
    
  }
  #cat("  /max_qEI\n")
  return(list(par = model@X[(n1+1):(n1+npoints),, drop=FALSE], value = model@y[(n1+1):(n1+npoints),, drop=FALSE])) 
}

distXmin <- function(x,Xmin) {
  return(min(sqrt(rowSums((Xmin-matrix(x,nrow=nrow(Xmin),ncol=ncol(Xmin),byrow=TRUE))^2))))
}   


######################################################################################

apply <- function(X, MARGIN, FUN, mode="base",...) {
  if (exists("vectorized_apply") && isTRUE(vectorized_apply)) mode="vectorized"
  if(mode=="base") y=base::apply(X,MARGIN,FUN, ...)
  else if(mode=="vectorized"){
    if (MARGIN==2) y=FUN(t(X),...)
    if (MARGIN==1) y=FUN(X,...)
  } else stop("mode ",mode," not supported")
  y
}

#' @test psoptim(c(.5,.5),branin)
psoptim <- function (par, fn, gr = NULL, ..., lower=-1, upper=1,
                     control = list()) {
  
  vectorized_apply <<- TRUE
  
  fn1 <- function(par) fn(par, ...)/p.fnscale
  mrunif <- function(n,m,lower,upper) {
    return(matrix(runif(n*m,0,1),nrow=n,ncol=m)*(upper-lower)+lower)
  }
  norm <- function(x) sqrt(sum(x*x))
  rsphere.unif <- function(n,r) {
    temp <- runif(n)
    return((runif(1,min=0,max=r)/norm(temp))*temp)
  }
  svect <- function(a,b,n,k) {
    temp <- rep(a,n)
    temp[k] <- b
    return(temp)
  }
  mrsphere.unif <- function(n,r) {
    m <- length(r)
    temp <- matrix(runif(n*m),n,m)
    return(temp%*%diag(runif(m,min=0,max=r)/apply(temp,2,norm)))
  }
  npar <- length(par)
  lower <- as.double(rep(lower, ,npar))
  upper <- as.double(rep(upper, ,npar))
  con <- list(trace = 0, fnscale = 1, maxit = 1000L, maxf = Inf,
              abstol = -Inf, reltol = 0, REPORT = 10,
              s = NA, k = 3, p = NA, w = 1/(2*log(2)),
              c.p = .5+log(2), c.g = .5+log(2), d = NA,
              v.max = NA, rand.order = TRUE, max.restart=Inf,
              maxit.stagnate = Inf,
              vectorize=FALSE, hybrid = FALSE, hybrid.control = NULL,
              trace.stats = FALSE, type = "SPSO2007")
  nmsC <- names(con)
  con[(namc <- names(control))] <- control
  if (length(noNms <- namc[!namc %in% nmsC])) 
    warning("unknown names in control: ", paste(noNms, collapse = ", "))
  ## Argument error checks
  if (any(upper==Inf | lower==-Inf))
    stop("fixed bounds must be provided")
  
  p.type <- pmatch(con[["type"]],c("SPSO2007","SPSO2011"))-1
  if (is.na(p.type)) stop("type should be one of \"SPSO2007\", \"SPSO2011\"")
  str(con)
  p.trace <- con[["trace"]]>0L # provide output on progress?
  p.fnscale <- con[["fnscale"]] # scale funcion by 1/fnscale
  p.maxit <- con[["maxit"]] # maximal number of iterations
  p.maxf <- con[["maxf"]] # maximal number of function evaluations
  p.abstol <- con[["abstol"]] # absolute tolerance for convergence
  p.reltol <- con[["reltol"]] # relative minimal tolerance for restarting
  p.report <- as.integer(con[["REPORT"]]) # output every REPORT iterations
  p.s <- ifelse(is.na(con[["s"]]),ifelse(p.type==0,floor(10+2*sqrt(npar)),40),
                con[["s"]]) # swarm size
  p.p <- ifelse(is.na(con[["p"]]),1-(1-1/p.s)^con[["k"]],con[["p"]]) # average % of informants
  p.w0 <- con[["w"]] # exploitation constant
  if (length(p.w0)>1) {
    p.w1 <- p.w0[2]
    p.w0 <- p.w0[1]
  } else {
    p.w1 <- p.w0
  }
  p.c.p <- con[["c.p"]] # local exploration constant
  p.c.g <- con[["c.g"]] # global exploration constant
  p.d <- ifelse(is.na(con[["d"]]),norm(upper-lower),con[["d"]]) # domain diameter
  p.vmax <- con[["v.max"]]*p.d # maximal velocity
  p.randorder <- as.logical(con[["rand.order"]]) # process particles in random order?
  p.maxrestart <- con[["max.restart"]] # maximal number of restarts
  p.maxstagnate <- con[["maxit.stagnate"]] # maximal number of iterations without improvement
  p.vectorize <- as.logical(con[["vectorize"]]) # vectorize?
  if (is.character(con[["hybrid"]])) {
    p.hybrid <- pmatch(con[["hybrid"]],c("off","on","improved"))-1
    if (is.na(p.hybrid)) stop("hybrid should be one of \"off\", \"on\", \"improved\"")
  } else {
    p.hybrid <- as.integer(as.logical(con[["hybrid"]])) # use local BFGS search
  }
  p.hcontrol <- con[["hybrid.control"]] # control parameters for hybrid optim
  if ("fnscale" %in% names(p.hcontrol))
    p.hcontrol["fnscale"] <- p.hcontrol["fnscale"]*p.fnscale
  else
    p.hcontrol["fnscale"] <- p.fnscale
  p.trace.stats <- as.logical(con[["trace.stats"]]) # collect detailed stats?
  
  if (p.trace) {
    message("S=",p.s,", K=",con[["k"]],", p=",signif(p.p,4),", w0=",
            signif(p.w0,4),", w1=",
            signif(p.w1,4),", c.p=",signif(p.c.p,4),
            ", c.g=",signif(p.c.g,4))
    message("v.max=",signif(con[["v.max"]],4),
            ", d=",signif(p.d,4),", vectorize=",p.vectorize,
            ", hybrid=",c("off","on","improved")[p.hybrid+1])
    if (p.trace.stats) {
      stats.trace.it <- c()
      stats.trace.error <- c()
      stats.trace.f <- NULL
      stats.trace.x <- NULL
    }
  }
  ## Initialization
  if (p.reltol!=0) p.reltol <- p.reltol*p.d
  if (p.vectorize) {
    lowerM <- matrix(lower,nrow=npar,ncol=p.s)
    upperM <- matrix(upper,nrow=npar,ncol=p.s)
  }
  X <- mrunif(npar,p.s,lower,upper)
  if (!any(is.na(par)) && all(par>=lower) && all(par<=upper)) X[,1] <- par
  if (p.type==0) {
    V <- (mrunif(npar,p.s,lower,upper)-X)/2
  } else { ## p.type==1
    V <- matrix(runif(npar*p.s,min=as.vector(lower-X),max=as.vector(upper-X)),npar,p.s)
    p.c.p2 <- p.c.p/2 # precompute constants
    p.c.p3 <- p.c.p/3
    p.c.g3 <- p.c.g/3
    p.c.pg3 <- p.c.p3+p.c.g3
  }
  if (!is.na(p.vmax)) { # scale to maximal velocity
    temp <- apply(V,2,norm)
    temp <- pmin.int(temp,p.vmax)/temp
    V <- V%*%diag(temp)
  }
  f.x <- apply(X,2,fn1) # first evaluations
  stats.feval <- p.s
  P <- X
  f.p <- f.x
  P.improved <- rep(FALSE,p.s)
  i.best <- which.min(f.p)
  error <- f.p[i.best]
  init.links <- TRUE
  if (p.trace && p.report==1) {
    message("It 1: fitness=",signif(error,4))
    if (p.trace.stats) {
      stats.trace.it <- c(stats.trace.it,1)
      stats.trace.error <- c(stats.trace.error,error)
      stats.trace.f <- c(stats.trace.f,list(f.x))
      stats.trace.x <- c(stats.trace.x,list(X))
    }
  }
  ## Iterations
  stats.iter <- 1
  stats.restart <- 0
  stats.stagnate <- 0
  while (stats.iter<p.maxit && stats.feval<p.maxf && error>p.abstol &&
         stats.restart<p.maxrestart && stats.stagnate<p.maxstagnate) {
    stats.iter <- stats.iter+1
    if (p.p!=1 && init.links) {
      links <- matrix(runif(p.s*p.s,0,1)<=p.p,p.s,p.s)
      diag(links) <- TRUE
    }
    ## The swarm moves
    if (!p.vectorize) {
      if (p.randorder) {
        index <- sample(p.s)
      } else {
        index <- 1:p.s
      }
      for (i in index) {
        if (p.p==1)
          j <- i.best
        else
          j <- which(links[,i])[which.min(f.p[links[,i]])] # best informant
        temp <- (p.w0+(p.w1-p.w0)*max(stats.iter/p.maxit,stats.feval/p.maxf))
        V[,i] <- temp*V[,i] # exploration tendency
        if (p.type==0) {
          V[,i] <- V[,i]+runif(npar,0,p.c.p)*(P[,i]-X[,i]) # exploitation
          if (i!=j) V[,i] <- V[,i]+runif(npar,0,p.c.g)*(P[,j]-X[,i])
        } else { # SPSO 2011
          if (i!=j)
            temp <- p.c.p3*P[,i]+p.c.g3*P[,j]-p.c.pg3*X[,i] # Gi-Xi
          else
            temp <- p.c.p2*P[,i]-p.c.p2*X[,i] # Gi-Xi for local=best
          V[,i] <- V[,i]+temp+rsphere.unif(npar,norm(temp))
        }
        if (!is.na(p.vmax)) {
          temp <- norm(V[,i])
          if (temp>p.vmax) V[,i] <- (p.vmax/temp)*V[,i]
        }
        X[,i] <- X[,i]+V[,i]
        ## Check bounds
        temp <- X[,i]<lower
        if (any(temp)) {
          X[temp,i] <- lower[temp]
          V[temp,i] <- 0
        }
        temp <- X[,i]>upper
        if (any(temp)) {
          X[temp,i] <- upper[temp]
          V[temp,i] <- 0
        }
        ## Evaluate function
        if (p.hybrid==1) {
          temp <- optim(X[,i],fn,gr,...,method="L-BFGS-B",lower=lower,
                        upper=upper,control=p.hcontrol)
          V[,i] <- V[,i]+temp$par-X[,i] # disregards any v.max imposed
          X[,i] <- temp$par
          f.x[i] <- temp$value
          stats.feval <- stats.feval+as.integer(temp$counts[1])
        } else {
          f.x[i] <- fn1(X[,i])
          stats.feval <- stats.feval+1
        }
        if (f.x[i]<f.p[i]) { # improvement
          P[,i] <- X[,i]
          f.p[i] <- f.x[i]
          if (f.p[i]<f.p[i.best]) {
            i.best <- i
            if (p.hybrid==2) {
              temp <- optim(X[,i],fn,gr,...,method="L-BFGS-B",lower=lower,
                            upper=upper,control=p.hcontrol)
              V[,i] <- V[,i]+temp$par-X[,i] # disregards any v.max imposed
              X[,i] <- temp$par
              P[,i] <- temp$par
              f.x[i] <- temp$value
              f.p[i] <- temp$value
              stats.feval <- stats.feval+as.integer(temp$counts[1])
            }
          }
        }
        if (stats.feval>=p.maxf) break
      }
    } else {
      if (p.p==1)
        j <- rep(i.best,p.s)
      else # best informant
        j <- sapply(1:p.s,function(i)
          which(links[,i])[which.min(f.p[links[,i]])]) 
      temp <- (p.w0+(p.w1-p.w0)*max(stats.iter/p.maxit,stats.feval/p.maxf))
      V <- temp*V # exploration tendency
      if (p.type==0) {
        V <- V+mrunif(npar,p.s,0,p.c.p)*(P-X) # exploitation
        temp <- j!=(1:p.s)
        V[,temp] <- V[,temp]+mrunif(npar,sum(temp),0,p.c.p)*(P[,j[temp]]-X[,temp])
      } else { # SPSO 2011
        temp <- j==(1:p.s)
        temp <- P%*%diag(svect(p.c.p3,p.c.p2,p.s,temp))+
          P[,j]%*%diag(svect(p.c.g3,0,p.s,temp))-
          X%*%diag(svect(p.c.pg3,p.c.p2,p.s,temp)) # G-X
        V <- V+temp+mrsphere.unif(npar,apply(temp,2,norm))
      }
      if (!is.na(p.vmax)) {
        temp <- apply(V,2,norm)
        temp <- pmin.int(temp,p.vmax)/temp
        V <- V%*%diag(temp)
      }
      X <- X+V
      ## Check bounds
      temp <- X<lowerM
      if (any(temp)) {
        X[temp] <- lowerM[temp] 
        V[temp] <- 0
      }
      temp <- X>upperM
      if (any(temp)) {
        X[temp] <- upperM[temp]
        V[temp] <- 0
      }
      ## Evaluate function
      if (p.hybrid==1) { # not really vectorizing
        for (i in 1:p.s) {
          temp <- optim(X[,i],fn,gr,...,method="L-BFGS-B",lower=lower,
                        upper=upper,control=p.hcontrol)
          V[,i] <- V[,i]+temp$par-X[,i] # disregards any v.max imposed
          X[,i] <- temp$par
          f.x[i] <- temp$value
          stats.feval <- stats.feval+as.integer(temp$counts[1])
        }
      } else {
        #f.x <- fn1(t(X))
        f.x <- apply(X,2,fn1)
        stats.feval <- stats.feval+p.s
      }
      temp <- sapply(isTRUE,X=as.numeric(f.x)<f.p)
      if (any(temp)) { # improvement
        P[,temp] <- X[,temp]
        f.p[temp] <- f.x[temp]
        i.best <- which.min(f.p)
        if (temp[i.best] && p.hybrid==2) { # overall improvement
          temp <- optim(X[,i.best],fn,gr,...,method="L-BFGS-B",lower=lower,
                        upper=upper,control=p.hcontrol)
          V[,i.best] <- V[,i.best]+temp$par-X[,i.best] # disregards any v.max imposed
          X[,i.best] <- temp$par
          P[,i.best] <- temp$par
          f.x[i.best] <- temp$value
          f.p[i.best] <- temp$value
          stats.feval <- stats.feval+as.integer(temp$counts[1])
        }
      }
      if (stats.feval>=p.maxf) break
    }
    if (p.reltol!=0) {
      d <- X-P[,i.best]
      d <- sqrt(max(colSums(d*d)))
      if (d<p.reltol) {
        X <- mrunif(npar,p.s,lower,upper)
        V <- (mrunif(npar,p.s,lower,upper)-X)/2
        if (!is.na(p.vmax)) {
          temp <- apply(V,2,norm)
          temp <- pmin.int(temp,p.vmax)/temp
          V <- V%*%diag(temp)
        }
        stats.restart <- stats.restart+1
        if (p.trace) message("It ",stats.iter,": restarting")
      }
    }
    init.links <- f.p[i.best]==error # if no overall improvement
    stats.stagnate <- ifelse(init.links,stats.stagnate+1,0)
    error <- f.p[i.best]
    if (p.trace && stats.iter%%p.report==0) {
      if (p.reltol!=0) 
        message("It ",stats.iter,": fitness=",signif(error,4),
                ", swarm diam.=",signif(d,4))
      else
        message("It ",stats.iter,": fitness=",signif(error,4))
      if (p.trace.stats) {
        stats.trace.it <- c(stats.trace.it,stats.iter)
        stats.trace.error <- c(stats.trace.error,error)
        stats.trace.f <- c(stats.trace.f,list(f.x))
        stats.trace.x <- c(stats.trace.x,list(X))
      }
    }
  }
  if (error<=p.abstol) {
    msg <- "Converged"
    msgcode <- 0
  } else if (stats.feval>=p.maxf) {
    msg <- "Maximal number of function evaluations reached"
    msgcode <- 1
  } else if (stats.iter>=p.maxit) {
    msg <- "Maximal number of iterations reached"
    msgcode <- 2
  } else if (stats.restart>=p.maxrestart) {
    msg <- "Maximal number of restarts reached"
    msgcode <- 3
  } else {
    msg <- "Maximal number of iterations without improvement reached"
    msgcode <- 4
  }
  if (p.trace) message(msg)
  o <- list(par=P[,i.best],value=f.p[i.best],
            counts=c("function"=stats.feval,"iteration"=stats.iter,
                     "restarts"=stats.restart),
            convergence=msgcode,message=msg)
  if (p.trace && p.trace.stats) o <- c(o,list(stats=list(it=stats.trace.it,
                                                         error=stats.trace.error,
                                                         f=stats.trace.f,
                                                         x=stats.trace.x)))
  vectorized_apply <<- FALSE
  
  return(o)
}
