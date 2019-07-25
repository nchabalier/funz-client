FUNZ_HOME="dist"
source(file.path(FUNZ_HOME,"Funz.R"))
Funz.init(FUNZ_HOME)
res=Funz_Run("R","src/test/samples/branin.R",input.variables = list(x1=(1:9)/10,x2=(1:9)/10),verbosity=5)
print(unlist(res[['cat']]))
q(save="no")
