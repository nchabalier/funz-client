# This is the Funz_Run call. All calculations are to be launched by Funz
#' @test branin.runshell(data.frame(x1=.5,x2=.3))
branin.runshell <- function(X,...) {
    Funz_Run(model=CODE,input.files=SRC,input.variables=list(x1=X[,1],x2=X[,2]),verbosity=VERBOSITY,archive.dir="tmp/branin.runshell.R",...)$z
}

# this is the reference value for branin evaluation
branin.ref <- function(X) {
	x1 <- X[,1]*15-5
	x2 <- X[,2]*15
	(x2 - 5/(4*pi^2)*(x1^2) + 5/pi*x1 - 6)^2 + 10*(1 - 1/(8*pi))*cos(x1) + 10
}

test1Case <- function() {
    X=data.frame(x1=runif(1),x2=runif(1))

    Y.runshell = branin.runshell(X)
    Y.ref = branin.ref(X)

    if (!isTRUE(all(abs(Y.runshell-Y.ref) < 1e-4))) {
        return("FAILED to match reference and Funz evaluations")
    } else return("OK")
}

test10Cases <- function() {
    X=data.frame(x1=runif(10),x2=runif(10))

    Y.runshell = branin.runshell(X)
    Y.ref = branin.ref(X)

    if (!isTRUE(all(abs(Y.runshell-Y.ref) < 1e-4))) {
        return("FAILED to match reference and Funz evaluations")
    } else return("OK")
}

testDuplicateCases <- function() {
    X=data.frame(x1=runif(10),x2=runif(10))
    X[1,] = c(.5,.5)
    X[3,] = c(.5,.5)
    X[9,] = c(.5,.5)

    Y.runshell = branin.runshell(X)
    Y.ref = branin.ref(X)

    if (!isTRUE(all(abs(Y.runshell-Y.ref) < 1e-4))) {
        return("FAILED to match reference and Funz evaluations")
    } else return("OK")
}

testFail1 <- function() {
    r <<- Funz_Run(model=CODE,input.files=SRC,input.variables=list(x1=c("0.5","abc")),verbosity=VERBOSITY,archive.dir="tmp/testFail1.R")

    try(if (r$state[1]=="done" & r$state[2]=="failed") return("OK"))
    return("FAILED")
}

testFail2 <- function() {
    r <- Funz_Run(model=CODE,input.files=SRC,input.variables=list(x1=c("cde","abc")),verbosity=VERBOSITY,archive.dir="tmp/testFail2.R")

    try(if(r$state[1]=="failed" & r$state[2]=="failed") return("OK"))
    return("FAILED")
}


############################### Run ######################################
## @Before Run funz daemon to launch 'runshell' calculations

FUNZ_HOME="dist"
source(file.path(FUNZ_HOME,"Funz.R"))
Funz.init(FUNZ_HOME,verbosity=10)

source("src/test/RunTest.prop")

t0=Sys.time()
for (t in c("test1Case","test10Cases","testDuplicateCases","testFail1","testFail2")) {
    print("")
    res = eval(parse(text = paste0("try(",t,"(),silent=FALSE)")))
    cat("\n\n")
    print(paste0("Test ",t,": ",res))
}
print(Sys.time()-t0)
