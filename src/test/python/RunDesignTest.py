import os, math, numpy, sys

failed=False

def testBraninGradientDescent() :
    rundesign = Funz_RunDesign(model=CODE,input_files=SRC,design = DESIGN, design_options = DESIGN_OPT,input_variables = {'x1':"[0,1]",'x2':"[0,1]"},verbosity=VERBOSITY,archive_dir="/tmp")

    if not math.fabs(float(rundesign['analysis.min'][0])-BraninGradientDescent_MIN)<1e-5 :
        global failed
        failed=True
        return("FAILED to match reference and Funz evaluation")
    else : 
        return("OK")
        

def testBraninGradientDescentx2() :
    rundesign = Funz_RunDesign(model=CODE,input_files=SRC,design = DESIGN, design_options = DESIGN_OPT,input_variables = {'x1':[0,1],'x2':"[0,1]"},verbosity=VERBOSITY,archive_dir="/tmp")

    if not (numpy.fabs(numpy.array([ float(m) for m in rundesign['analysis.min'] ])-numpy.array([BraninGradientDescentx2_x1_0_MIN,BraninGradientDescentx2_x1_1_MIN]))<1e-5).all() :
        global failed
        failed=True
        return("FAILED to match reference and Funz evaluation")
    else : 
        return("OK")


############################### Run ######################################
## @Before Run funz daemon to launch 'shell' calculations

FUNZ_HOME=os.path.realpath("dist")
exec(open(os.path.join(FUNZ_HOME,"Funz.py")).read())
Funz_init(FUNZ_HOME=FUNZ_HOME,verbosity=10)

exec(open(os.path.join("src/test/RunDesignTest.prop")).read())
DESIGN_OPT={'nmax':NMAX}

for t in ["testBraninGradientDescent","testBraninGradientDescentx2"] :
    print("Test "+t+": "+eval(t+"()"))

if failed:
    sys.exit(1)
else:
    sys.exit(0)