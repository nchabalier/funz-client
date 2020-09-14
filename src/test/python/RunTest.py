import os, math, numpy, sys

failed=0

# @test branin_runshell({'x1':.5,'x2':.3})
def branin_runshell(X=None) :
    return(Funz_Run(model=CODE,input_files=SRC,input_variables=X,verbosity=VERBOSITY,archive_dir="tmp/branin_runshell.py")['cat'])


# this is the reference value for branin evaluation
# @test branin_ref({'x1':.5,'x2':.3})
# @test branin_ref({'x1':[.5,.6],'x2':[.2,.3]})
def branin_ref(X) :
	x1 = numpy.array(X['x1'])*15-5
	x2 = numpy.array(X['x2'])*15
	return( ((x2 - 5/(4*math.pi**2)*(x1**2) + 5/math.pi*x1 - 6)**2 + 10*(1 - 1/(8*math.pi))*numpy.cos(x1) + 10 ).tolist())


def test1Case() :
    X={'x1':numpy.random.uniform(size=1),'x2':numpy.random.uniform(size=1)}

    Y_runshell = branin_runshell(X)[0][0]
    Y_ref = branin_ref(X)[0]

    if not math.fabs(Y_runshell-Y_ref) < 1e-4 :
        global failed
        failed=1
        return("FAILED to match reference and Funz evaluation")
    else : 
        return("OK")


def test10Cases() :
    X={'x1':numpy.random.uniform(size=10),'x2':numpy.random.uniform(size=10)}

    Y_runshell = branin_runshell(X)
    Y_ref = branin_ref(X)
    
    if not (numpy.fabs(numpy.array(Y_runshell).transpose()-numpy.array(Y_ref)) < 1e-4).all() :
        global failed
        failed=1
        return("FAILED to match reference and Funz evaluation")
    else : 
        return("OK")


def testDuplicateCases() :
    X={'x1':numpy.random.uniform(size=10),'x2':numpy.random.uniform(size=10)}
    X['x1'][1] = .5
    X['x1'][3] = .5
    X['x1'][9] = .5
    X['x2'][1] = .5
    X['x2'][3] = .5
    X['x2'][9] = .5
    
    Y_runshell = branin_runshell(X)
    Y_ref = branin_ref(X)

    if not (numpy.fabs(numpy.array(Y_runshell).transpose()-numpy.array(Y_ref)) < 1e-4).all() :
        global failed
        failed=1
        return("FAILED to match reference and Funz evaluation")
    else : 
        return("OK")


def testFail1() :
    r = Funz_Run(model=CODE,input_files=SRC,input_variables={'x1':["0.5","abc"]},verbosity=VERBOSITY,archive_dir="tmp/testFail1.py")

    if not (r['state'][0]=="done") & (r['state'][1]=="failed") :
        global failed
        failed=1
        return("FAILED")
    else : 
        return("OK")


def testFail2() :
    r = Funz_Run(model=CODE,input_files=SRC,input_variables={'x1':["def","abc"]},verbosity=VERBOSITY,archive_dir="tmp/testFail2.py")

    if not (r['state'][0]=="failed") & (r['state'][1]=="failed") :
        global failed
        failed=1
        return("FAILED")
    else : 
        return("OK")


############################### Run ######################################
## @Before Run funz daemon to launch 'runshell' calculations

FUNZ_HOME=os.path.realpath("dist")
exec(open(os.path.join(FUNZ_HOME,"Funz.py")).read())
Funz_init(FUNZ_HOME=FUNZ_HOME,verbosity=10)

exec(open(os.path.join("src/test/RunTest.prop")).read())

for t in ["test1Case","test10Cases","testDuplicateCases","testFail1","testFail2"] :
    print("")
    res = eval(t+"()")
    print("")
    print("Test "+t+": "+res)

sys.exit(failed)