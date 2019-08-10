testBraninGradientDescent <- function() {
    rundesign <- Funz_RunDesign(model=CODE, input.files=SRC, output.expressions="cat", design = DESIGN, design.options = DESIGN_OPT,input.variables = list(x1="[0,1]",x2="[0,1]"), verbosity=VERBOSITY,archive.dir="tmp")

    if (abs(as.numeric(rundesign$analysis.min[[1]])-BraninGradientDescent_MIN)<1e-5) {
        return("OK")
    } else {
        return("FAILED to find minimum")
    }
}

testBraninGradientDescentx2 <- function() {
    rundesign2 <- Funz_RunDesign(model=CODE, input.files=SRC, output.expressions="cat", design = DESIGN, design.options = DESIGN_OPT,input.variables = list(x1=c(0,1),x2="[0,1]"), verbosity=VERBOSITY,archive.dir="tmp")

    if (all(abs(as.numeric(rundesign2$analysis.min)-c(BraninGradientDescentx2_x1_0_MIN,BraninGradientDescentx2_x1_1_MIN))<1e-5)) {
        return("OK")
    } else {
        return("FAILED to find minimums")
    }
}

#testBraninNoDesign <- function() {
#    ref = Funz_RunDesign(model=CODE, input.files=SRC, output.expressions="cat",design=NULL, design.options = NULL,input.variables = list(x1=runif(5),x2=runif(6)), verbosity=VERBOSITY,archive.dir="/tmp")
#
#    if (length(ref$cat)==30) {
#        return("OK")
#    } else {
#        warning("Failed to calculate no design")
#        return("FAILED")
#    }
#}

#testBraninGroupNoDesign <- function() {
#     ref <<- Funz_RunDesign(model=CODE, input.files=SRC, output.expressions="cat",design=NULL, design.options = NULL,input.variables = list(g=list(x1=runif(5),x2=runif(5))), verbosity=VERBOSITY,archive.dir="/tmp")
#
#    if (length(ref$cat)==5) {
#        return("OK")
#    } else {
#        warning("Failed to calculate group")
#        return("FAILED")
#    }
#}


############################### Run ######################################
## @Before Run funz daemon to launch 'runshell' calculations

FUNZ_HOME="dist"
source(file.path(FUNZ_HOME,"Funz.R"))
Funz.init(FUNZ_HOME,verbosity=10)

source("src/test/RunDesignTest.prop")
DESIGN_OPT=list(nmax=NMAX,delta=DELTA)

t0=Sys.time()
for (t in c("testBraninGradientDescent","testBraninGradientDescentx2")) {
    print("")
    res = eval(parse(text = paste0("try(",t,"(),silent=FALSE)")))
    print("")
    print(paste0("Test ",t,": ",res))
}
print(Sys.time()-t0)

