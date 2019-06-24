FUNZ_HOME="dist"
source(file.path(FUNZ_HOME,"Funz.R"))
Funz.init(FUNZ_HOME)

Funz_Run.info("R","src/main/resources/samples/branin.R")

Funz_Run("R","src/main/resources/samples/branin.R")

Funz_Run("R","src/main/resources/samples/branin.R",input.variables = list(x1=runif(10),x2=runif(10)),verbosity=5)

