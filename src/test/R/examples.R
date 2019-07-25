FUNZ_HOME="dist"
source(file.path(FUNZ_HOME,"Funz.R"))
Funz.init(FUNZ_HOME)

Funz_Run.info("R","src/test/samples/branin.R")

Funz_Run("R","src/test/samples/branin.R")

Funz_Run("R","src/test/samples/branin.R",input.variables = list(x1=runif(10),x2=runif(10)),verbosity=5)

