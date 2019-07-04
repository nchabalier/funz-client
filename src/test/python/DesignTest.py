import os, math, numpy, sys

failed=0

# @test branin_runshell({'x1':.5,'x2':.3})
def branin_runshell_vec(X=None) :
    return(Funz_Run(model=CODE,input_files=SRC,input_variables=X,verbosity=VERBOSITY,archive_dir="/tmp")['cat'])


# this is the reference value for branin evaluation
# @test branin_ref_vec({'x1':.5,'x2':.3})
# @test branin_ref_vec({'x1':[.5,.6],'x2':[.2,.3]})
def branin_ref_vec(X) :
    x1 = numpy.array(X['x1'])*15-5
    x2 = numpy.array(X['x2'])*15
    return( ((x2 - 5/(4*math.pi**2)*(x1**2) + 5/math.pi*x1 - 6)**2 + 10*(1 - 1/(8*math.pi))*numpy.cos(x1) + 10 ).tolist())
# @test branin_ref_novec({'x1':.5,'x2':.3})
def branin_ref_novec(X) :
    x1 = X['x1']*15-5
    x2 = X['x2']*15
    return( ((x2 - 5/(4*math.pi**2)*(x1**2) + 5/math.pi*x1 - 6)**2 + 10*(1 - 1/(8*math.pi))*math.cos(x1) + 10 ))


def testMatchRef() :
    ref = Funz_Design(design = DESIGN, options = DESIGN_OPT,input_variables = {'x1':"[0,1]",'x2':"[0,1]"}, fun = branin_ref_vec,verbosity=VERBOSITY,archive_dir="/tmp")
    runshell = Funz_Design(design = DESIGN, options = DESIGN_OPT,input_variables = {'x1':"[0,1]",'x2':"[0,1]"}, fun = branin_runshell_vec,verbosity=VERBOSITY,archive_dir="/tmp")

    if not (numpy.fabs(numpy.array(float(runshell['analysis.min'])).transpose()-numpy.array(float(ref['analysis.min']))) < 1e-4).all() :
        global failed
        failed=1
        return("FAILED to match reference and Funz design results: ref min="+ref['analysis.min']+" runshell min="+runshell['analysis.min'])
    else : 
        return("OK")


def testVectorizeFun() :
    ref = Funz_Design(design = DESIGN, options = DESIGN_OPT,input_variables = {'x1':"[0,1]",'x2':"[0,1]"},fun=branin_ref_vec,fun_control={'vectorize':"fun"},verbosity=VERBOSITY,archive_dir="/tmp")

    if not math.fabs(float(ref['analysis.min'])-BraninGradientDescent_MIN)<1e-5 :
        global failed
        failed=1
        return("FAILED to match reference and Funz evaluation")
    else : 
        return("OK")


def testNoVectorize() :
    ref = Funz_Design(design = DESIGN, options = DESIGN_OPT,input_variables = {'x1':"[0,1]",'x2':"[0,1]"}, fun = branin_ref_novec,verbosity=VERBOSITY,fun_control={'vectorize':None},archive_dir="/tmp")

    if not math.fabs(float(ref['analysis.min'])-BraninGradientDescent_MIN)<1e-5 :
        global failed
        failed=1
        return("FAILED to match reference and Funz evaluation")
    else : 
        return("OK")


############################### Run ######################################
## @Before Run funz daemon to launch 'runshell' calculations

FUNZ_HOME=os.path.realpath("dist")
exec(open(os.path.join(FUNZ_HOME,"Funz.py")).read())
Funz_init(FUNZ_HOME=FUNZ_HOME,verbosity=10)

exec(open(os.path.join("src/test/DesignTest.prop")).read())
DESIGN_OPT={'nmax':NMAX}

for t in ["testMatchRef","testVectorizeFun","testNoVectorize"] :
    print("Test "+t+": "+eval(t+"()"))


sys.exit(failed)