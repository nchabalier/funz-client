#help=First-order local optimization algorithm<br/>http://en.wikipedia.org/wiki/Gradient_descent
#type=Optimization
#output=minimum
#options=nmax=100,delta=1,epsilon=0.01,target=-Inf

# iteration index
i = 0

#' constructor and initializer of R session
init <- function() {
# all parameters are initialy strings, so you have to put as global non-string values
	nmax <<- as.integer(nmax)
	delta <<- as.numeric(delta)
	epsilon <<- as.numeric(epsilon)
	target <<- as.numeric(target)
}

askfinitedifferences <- function(x) {
    xd <- matrix(x,nrow=1);
    for (ii in 1:length(x)) {
        xdi <- as.array(x);
        if (xdi[ii] + epsilon > 1.0) {
            xdi[ii] <- xdi[ii] - epsilon;
        } else {
            xdi[ii] <- xdi[ii] + epsilon;
        }
        xd <- rbind(xd,matrix(xdi,nrow=1))
    }
    return(xd)
}

gradient <- function(xd,yd) {
	d = ncol(xd)
	grad = rep(0,d)
	for (ii in 1:d) {
		grad[ii] = (yd[ii+1] - yd[1]) / (xd[ii+1,ii] - xd[1,ii])
	}
	return(grad)
}

#' first design building. All variables are set in [0,1]. d is the dimension, or number of variables
#' @param d number of variables
initDesign <- function(d) {
	return(askfinitedifferences(rep(0.5,d)));
}

#' iterated design building.
#' @param X data frame of current doe variables (in [0,1])
#' @param Y data frame of current results
#' @return data frame or matrix of next doe step
nextDesign <- function(X,Y) {
	if (i>nmax) return();
	if (min(Y[,1])<target) return();

	d = ncol(X)
	n = nrow(X)

	prevXn = X[(n-d):n,]
	prevYn = Y[(n-d):n,1]

	if (i > 1) {
		if (Y[n-d,1] > Y[n-d-d,1]) {
			delta <<- delta / 2
                        prevXn = X[(n-d-d-1):(n-d-1),]
			prevYn = Y[(n-d-d-1):(n-d-1),1]
		}
        }

	grad = gradient(prevXn,prevYn)
	#grad = grad / sqrt(sum(grad * grad))
	xnext = t(prevXn[1,] - (grad * delta))
	for (t in 1:d) {
		if (xnext[t] > 1.0) {
			xnext[t] = 1.0;
		}
		if (xnext[t] < 0.0) {
			xnext[t] = 0.0;
		}
	}

	i <<- i+1
	return(askfinitedifferences(xnext));
}

#' final analysis. All variables are set in [0,1]. Return HTML string
#' @param X data frame of doe variables (in [0,1])
#' @param Y data frame of  results
#' @return HTML string of analysis
analyseDesign <- function(X,Y) {
	Y = Y[,1]
        m = min(Y)
	m.ix=which.min(Y)
        m.ix=m.ix[1]
	x = X[m.ix,]

	resolution <- 600
	d = dim(X)[2]

	if(d>1) {
		analyse.files <<- paste0("pairs_",i-1,".png",sep="")
		png(file=analyse.files,bg="transparent",height=resolution,width = resolution)
		red = (as.matrix(Y)-min(Y))/(max(Y)-min(Y))
		pairs(X,col=rgb(r=red,g=0,b=1-red),Y=Y[[1]],d=d) #,panel=panel.vec)
		dev.off()
	} else {
		analyse.files <<- paste0("plot_",i-1,".png",sep="")
		png(file=analyse.files,bg="transparent",height=resolution,width = resolution)
		red = (as.matrix(Y)-min(Y))/(max(Y)-min(Y))
		plot(x=X[,1],y=Y[,1],xlab=names(X),ylab=names(Y),col=rgb(r=red,g=0,b=1-red))
		dev.off()
	}

    html=paste0(sep='<br/>',
        paste0('<HTML name="minimum">minimum is ',m),
        paste0('found at ',
            paste0(paste(names(X),'=',x)),
            '<br/><img src="',
            analyse.files,
            '" width="',resolution,'" height="',resolution,
            '"/></HTML>'))

    plotmin=paste('<Plot1D name="min">',m,'</Plot1D>')

    if (d == 1) {
        plotx=paste('<Plot1D name="argmin">',paste(x),'</Plot1D>')
    } else if (d == 2) {
        plotx=paste('<Plot2D name="argmin">[',paste(collapse=',',x),']</Plot2D>')
    } else {
        plotx=paste('<PlotnD name="argmin">[',paste(collapse=',',x),']</PlotnD>')
    }

    return(paste(html,plotmin,plotx,collapse=';'))
}

panel.vec <- function(x, y , col, Y, d, ...) {
	#points(x,y,col=col)
	for (i in 1:(length(x)/(d+1))) {
		n0 = 1+(i-1)*(d+1)
		x0 = x[n0]
		y0 = y[n0]
		for (j in 1:d) {
			if (x[n0+j] != x0) {
				dx = (Y[n0]-Y[n0+j])/(max(Y)-min(Y))
				#break;
			}
		}
		for (j in 1:d) {
			if (y[n0+j] != y0) {
				dy = (Y[n0]-Y[n0+j])/(max(Y)-min(Y))
				#break;
			}
		}
		points(x=x0,y=y0,col=col[n0],pch=20)
		lines(x=c(x0,x0+dx),y=c(y0,y0+dy),col=col[n0])
		if (exists("x0p")) {
			lines(x=c(x0p,x0),y=c(y0p,y0),col=col[n0],lty=3)
		}
		x0p=x0
		y0p=y0
	}
	
}

#' temporary analysis. All variables are set in [0,1]. Return HTML string
#' @param X data frame of doe variables (in [0,1])
#' @param Y data frame of  results
#' @returnType String
#' @return HTML string of analysis
analyseDesignTmp <- function(X,Y) {
	analyseDesign(X,Y)
}

################### NEEDED FOR UNIT TEST #########################
f <- function (x1,x2) {
    x1 <- x1 * 15 - 5
    x2 <- x2 * 15
    (x2 - 5/(4 * pi^2) * (x1^2) + 5/pi * x1 - 6)^2 + 10 * (1 - 1/(8 * pi)) * cos(x1) + 10
}
