FUNZ_HOME=os.path.realpath("dist")
exec(open(os.path.join(FUNZ_HOME,"Funz.py")).read())
Funz_init(FUNZ_HOME,verbosity=10)

Funz_Run_info("R","dist/samples/branin.R")

Funz_Run("R","dist/samples/branin.R")

Funz_Run("R","dist/samples/branin.R",input_variables={'x1':[.1,.2,.3,.4],'x2':[.1,.2,.3,.4]},verbosity=5)

