# This is the Funz_Run call. All calculations are to be launched by Funz
branin.runshell.vec <- function(X,...) {
    unlist(Funz_Run(model=CODE,input.files=SRC,input.variables = list(x1=X[,1],x2=X[,2]),verbosity=VERBOSITY,archive.dir="/tmp/branin.runshell.vec.R",...)$cat)
}
#branin.runshell.novec <- function(x,...) {
#    Funz_Run(model=code,input.files="dist/samples/branin.R",input.variables = list(x1=x[1],x2=x[2]),verbosity=VERBOSITY,archive.dir="/tmp",...)$cat[[1]]
#}

# this is the reference value for branin evaluation
branin.ref.vec <- function(X) {
	x1 <- X[,1]*15-5
	x2 <- X[,2]*15
	y = (x2 - 5/(4*pi^2)*(x1^2) + 5/pi*x1 - 6)^2 + 10*(1 - 1/(8*pi))*cos(x1) + 10
}
branin.ref.novec <- function(x) {
	x1 <- x[1]*15-5
	x2 <- x[2]*15
	y = (x2 - 5/(4*pi^2)*(x1^2) + 5/pi*x1 - 6)^2 + 10*(1 - 1/(8*pi))*cos(x1) + 10
}


testMatchRef <- function() {
    ref = Funz_Design(design = DESIGN, options = DESIGN_OPT,input.variables = list(x1="[0,1]",x2="[0,1]"), fun = branin.ref.vec,verbosity=VERBOSITY,archive.dir="tmp/testMatchRef.ref.R")
    runshell = Funz_Design(design = DESIGN, options = DESIGN_OPT,input.variables = list(x1="[0,1]",x2="[0,1]"), fun = branin.runshell.vec,verbosity=VERBOSITY,archive.dir="tmp/testMatchRef.runshell.R")

    if (abs(as.numeric(ref$min)-as.numeric(runshell$min)) <1e-3) {
        return("OK")
    } else {
        return(paste("FAILED to match reference and Funz design results: ref min=",ref$min," runshell min=",runshell$min))
    }
}

testVectorizeFun <- function(f = branin.ref.vec) {
    ref = Funz_Design(design = DESIGN, options = DESIGN_OPT,input.variables = list(x1="[0,1]",x2="[0,1]"), fun = f,verbosity=VERBOSITY,fun.control=list(vectorize="fun"),archive.dir="tmp/testVectorizeFun.R")

    if (abs(as.numeric(ref$min)-BraninGradientDescent_MIN)<1e-5) {
        return("OK")
    } else {
        return("FAILED to find minimum")
    }
}

testNoVectorize <- function(f = branin.ref.novec) {
    ref = Funz_Design(design = DESIGN, options = DESIGN_OPT,input.variables = list(x1="[0,1]",x2="[0,1]"), fun = f,verbosity=VERBOSITY,fun.control=list(vectorize=NULL),archive.dir="tmp/testNoVectorize.R")

    if (abs(as.numeric(ref$min)-BraninGradientDescent_MIN)<1e-5) {
        return("OK")
    } else {
        return("FAILED to find minimum")
    }
}

testVectorizeForeach <- function(f = branin.ref.novec) {
    require(doMC)
    registerDoMC()
    ref = Funz_Design(design = DESIGN, options = DESIGN_OPT,input.variables = list(x1="[0,1]",x2="[0,1]"), fun = f,verbosity=VERBOSITY,fun.control=list(vectorize="foreach"),archive.dir="tmp/testVectorizeForeach.R")

    if (abs(as.numeric(ref$min)-BraninGradientDescent_MIN)<1e-5) {
        return("OK")
    } else {
        return("FAILED to find minimum")
    }
}

testVectorizeParallel <- function(f = branin.ref.novec) {
    ref <<- Funz_Design(design = DESIGN, options = DESIGN_OPT,input.variables = list(x1="[0,1]",x2="[0,1]"), fun = f,verbosity=VERBOSITY,fun.control=list(vectorize="parallel"),archive.dir="tmp/testVectorizeParallel.R")

    if (abs(as.numeric(ref$min)-BraninGradientDescent_MIN)<1e-5) {
        return("OK")
    } else {
        return("FAILED to find minimum")
    }
}


############################### Run ######################################
## @Before Run funz daemon to launch 'runshell' calculations

FUNZ_HOME="dist"
source(file.path(FUNZ_HOME,"Funz.R"))
Funz.init(FUNZ_HOME,verbosity=10)

source("src/test/DesignTest.prop")
DESIGN_OPT=list(nmax=NMAX,delta=DELTA)

t0=Sys.time()
for (t in c("testMatchRef","testVectorizeFun","testNoVectorize","testVectorizeForeach","testVectorizeParallel")) {
    print("")
    res = eval(parse(text = paste0("try(",t,"(),silent=FALSE)")))
    print("")
    print(paste0("Test ",t,": ",res))
}
print(Sys.time()-t0)