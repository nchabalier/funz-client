#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
This file holds the Python wrapper using the Funz open API.
It allows Funz to be used directly from Python.
 Funz_Run(...) to launch remote calculations (providing input files + code name).
 Funz_Design(...) to call Funz DoE plugin.
 Funz_RunDesign(...) to call Funz DoE plugin over remote calculations (providing input files + code name).

@license: BSD
@author: Y. Richet
"""

################################## _Internals ######################################

from __future__ import print_function
import sys, os, time, re, locale, warnings
import numpy
import py4j.java_gateway

# @test X = {'a':[1,2,3],'b':[4,5,6]}; _getInMapArray(X,1)
def _getInMapArray(x,i) :
    xi={}
    for k in x.keys():
        xi[k] = x[k][i]
    return(xi)

# @test X = {'a':[1,2,3],'b':[4,5,6]}; _up(X,{'c':0.213})
def _up(m,u) :
    mu = m
    mu.update(u)
    return(mu)


def _PArray(o):
    if isinstance(o,list):
        return(o)
    elif o is None:
        return([])
    else:
        return([o])

# @test _gateway = py4j.java_gateway.JavaGateway(gateway_parameters= py4j.java_gateway.GatewayParameters(port=py4j.java_gateway.launch_gateway()))
# @test J=_gateway.jvm
def _JArray(jobjects,jclass=None):
    if jclass is None:
        jclass=str(jobjects[0].getClass())[6:]
    jarray = _gateway.new_array(py4j.java_collections.JavaClass(jclass,_gateway),len(jobjects))
    for i in list(range(0,len(jobjects))):
        if jobjects[i] is None:
            jarray[i] = None
        elif isinstance(jobjects[i],list):
            if len(jobjects[i])>0 :
                jarray[i] = _JArray(jobjects[i],_PTypeToJClass(jobjects[i][0]))
            else:
                jarray[i] = []
        elif isinstance(jobjects[i],dict):
            jarray[i] = _PMapToJMap(jobjects[i])
        else:
            jarray[i] = jobjects[i]
    return(jarray)

# @test _JArrayToPArray(g.new_array(J.double,10))==[0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]
def _JArrayToPArray(a):
    pa = []
    for e in a:
        if isinstance(e,py4j.java_collections.JavaArray):
            e = _JArrayToPArray(e)
        elif isinstance(e,py4j.java_collections.JavaList):
            e = _JArrayToPArray(_JListToJArray(e))
        elif isinstance(e,py4j.java_collections.JavaMap):
            e = _JMapToPMap(e)
        pa.append(e)
    return(pa)
    
def _JMapToPMap(m):
    p={}
    for k in m.keys():
        if isinstance(m[k],py4j.java_collections.JavaArray):
            p[k] = _JArrayToPArray(m[k])
        elif isinstance(m[k],py4j.java_collections.JavaList):
            p[k] = _JArrayToPArray(_JListToJArray(m[k]))
        elif isinstance(m[k],py4j.java_collections.JavaMap):
            p[k] = _JMapToPMap(_JListToJArray(m[k]))
        else:
            p[k] = m[k]
            
    return(p)    
    
# @test X = {'a':1,'b':[4,5,6],'c':{'a':1,'b':[1,2]},'d':"abc"}; _PMapToJMap(X)
# @test X = {'a':1,'b':[4,5,6],'c':{'a':1,'b':[1,2]},'d':"abc",'e':[[1,2,3]]}; _PMapToJMap(X)
# @test X = {'a':[1,2,3],'b':[4,5,6]}; _PMapToJMap(X)
# @test X = {'a':[[1,2,3]],'b':[[4,5,6]]}; _PMapToJMap(X)
def _PMapToJMap(m) :
    jm = _jclassHashMap()
    for k in m.keys():
        if m[k] is None:
            values = None
        elif isinstance(m[k],list):
            if len(m[k])>0 :
                values = _JArray(m[k],_PTypeToJClass(m[k][0]))
            else:
                values = []
        elif isinstance(m[k],dict):
            values = _PMapToJMap(m[k])
        else:
            values = m[k]
        jm.put(k, values)
    return(jm)

def _PTypeToJClass(object) :
    if isinstance(object, int):
        return("java.lang.Integer")
    elif isinstance(object, float):
        return("java.lang.Double")
    elif isinstance(object, str):
        return("java.lang.String")
    elif isinstance(object, list):
        if len(object)>0:
            return(("[L"+_PTypeToJClass(object[0])+";").replace("L[","[").replace(";;",";"))
        else:
            warnings.warn(" !!! empty array")
            return("null")     
    else:
        warnings.warn(" !!! unsupported class")
        return("null")       

# may be replaced by _gateway = JavaGateway(gateway_parameters=GatewayParameters(auto_convert=True)) in init()
#def _PArrayToJArray(a):
#    return(py4j.java_collections.ListConverter().convert(a, _gateway))

def _JListToJArray(l):
    py4j.java_collections.JavaArray(l)

def _asJObject(string) :
    if (string is None): return(None)
    jo = None
    try: 
        jo = _jclassData.asObject(string) 
    except:
        pass
    if jo is None: return(None)
    array = None
    try: 
        array = _JArrayToPArray(jo) 
    except:
        pass
    if not array is None: 
        return(array)
    else: 
        return(jo)

# @test _PFileArrayToJFileArray([".","./dist/Funz.R","dist/Funz.py"])
def _PFileArrayToJFileArray(files):
    jlist_files = []
    for i in _PArray(files):
        files_i = os.path.abspath(i)
        jfiles_i = _jclassFile(files_i)
        found = jfiles_i.isFile() | jfiles_i.isDirectory()
        if not found:
            raise Exception("File/dir "+files_i+" not found.")
        jlist_files.append(jfiles_i)
    return(_JArray(jlist_files))

def _jdelete(jo):
    _jclassUtils.delete(jo)


###################################### Init ###################################
_dir = None
if 'FUNZ_HOME' in globals(): _dir = FUNZ_HOME
if _dir is None: _dir = os.getenv('FUNZ_HOME',None)
if _dir is None: _dir = os.path.dirname(os.path.realpath(sys.argv[0]))
## Initialize Funz environment.
# @param FUNZ_HOME set to Funz installation path.
# @param verbosity verbosity of Funz workbench.
# @param verbose_level deprecated verbosity
# @param java_control list of JVM startup parameters (like -D...=...).
# @param jvmargs optional parameters passed to 'java' call.
# @example FUNZ_HOME="c:\Program Files\Funz";Funz_init(FUNZ_HOME)
def Funz_init(FUNZ_HOME=_dir, java_control={'Xmx':"512m",'Xss':"256k"} if sys.platform=="Windows" else {'Xmx':"512m"}, verbosity=0, verbose_level=None, **jvmargs) :
    if (not verbose_level is None) & (verbosity != verbose_level) : verbosity = verbose_level

    if FUNZ_HOME is None:
        raise Exception("FUNZ_HOME environment variable not set.\nPlease setup FUNZ_HOME to your Funz installation path.")

#    FUNZ_HOME=normalizePath(FUNZ_HOME)
    global _FUNZ_HOME
    _FUNZ_HOME = FUNZ_HOME

    if not os.path.isdir(_FUNZ_HOME):
        raise Exception("FUNZ_HOME environment variable not correctly set: FUNZ_HOME="+_FUNZ_HOME+"\nPlease setup FUNZ_HOME to your Funz installation path.\n(you can get Funz freely at https://funz.github.io/funz.org/)")

    parameters = ["-Dapp.home="+_FUNZ_HOME,"-Duser.language=en","-Duser.country=US","-Dverbosity="+str(verbosity),"-Douterr=.Funz"]
    for p in java_control.keys():
        if p[0]=="X":
            parameters.append("-"+p+java_control[p])
        else:
            parameters.append("-D"+p+"="+java_control[p])

    parameters.append("-Djava.awt.headless=True -Dnashorn.args='--no-deprecation-warning'")
    
    classpath = [ f for f in os.listdir(os.path.join(_FUNZ_HOME,"lib")) if (os.path.isfile(os.path.join(os.path.join(_FUNZ_HOME,"lib"), f)) & ((os.path.splitext(f)[1])==".jar")) ]
    
    class SysOut(py4j.compat.Queue) :
        def put(self,o):
            super(SysOut,self).put(o)
            print(str(o))
        
    class SysErr(py4j.compat.Queue) :
        def put(self,o):
            super(SysErr,self).put(o)
            print(str(o))
            
    if verbosity>3:
        print("  Initializing JVM ...\n    " + "\n    ".join(parameters))
    print("  Initializing Gateway ...")
    port = py4j.java_gateway.launch_gateway(classpath=":".join(os.path.join(_FUNZ_HOME,"lib",str(j)) for j in classpath),javaopts=parameters,redirect_stdout=SysOut(),redirect_stderr=SysErr(),die_on_exit=True)
    print("                       ... port "+str(port))
    global _gateway
    _gateway = py4j.java_gateway.JavaGateway(gateway_parameters= py4j.java_gateway.GatewayParameters(port=port,auto_convert=True))
    global J
    J = _gateway.jvm

    locale.setlocale(locale.LC_NUMERIC, "C") # otherwise, the locale may be changed by Java, so LC_NUMERIC is no longer "C"

    if verbosity>3:
        print("  Loading java/lang/System ...")
    global _jclassSystem
    _jclassSystem = J.java.lang.System

    if verbosity>3:
        print("Java "+ _jclassSystem.getProperty("java.runtime.name")+"\n version "+_jclassSystem.getProperty("java.version")+"\n from path "+_jclassSystem.getProperty("java.home"))

    if verbosity>3:
        print("  Loading org/funz/Constants ...")
    global _jclassConstants 
    _jclassConstants = J.org.funz.Constants

    if verbosity>0:
        print("Funz "+_jclassConstants.APP_VERSION+" <build "+_jclassConstants.APP_BUILD_DATE+">")

    if verbosity>3:
        print("  Loading org/funz/api/Funz_v1 ...")
    global _jclassFunz 
    _jclassFunz = J.org.funz.api.Funz_v1

    if (not verbosity is None) : _jclassFunz.setVerbosity(verbosity)    
    
    global _Funz_Models
    _Funz_Models = None
    global _Funz_Designs
    _Funz_Designs = None

    if verbosity>3:
        print("  Initializing Funz...")
    _jclassFunz.init()

    _Funz_Models = _JArrayToPArray(_jclassFunz.getModelList())
    if verbosity>0:
        print("  Funz models (port "+str(_jclassFunz.POOL.getPort())+"): " + " ".join(_Funz_Models))
    _Funz_Designs = _JArrayToPArray(_jclassFunz.getDesignList())
    if verbosity>0:
        print("  Funz designs (engine "+_jclassFunz.MATH.getEngineName()+"): " + " ".join(_Funz_Designs))

    # pre-load some class objects from funz API
    global _jclassData 
    _jclassData = J.org.funz.util.Data
    global _jclassFormat 
    _jclassFormat = J.org.funz.util.Format
    global _jclassUtils 
    _jclassUtils = J.org.funz.api.Utils
    global _jclassPrint
    _jclassPrint = J.org.funz.api.Print
    global _jclassDesignShell
    _jclassDesignShell = J.org.funz.api.DesignShell_v1
    global _jclassRunShell 
    _jclassRunShell = J.org.funz.api.RunShell_v1
    global _jclassShell 
    _jclassShell= J.org.funz.api.Shell_v1

    global _jclassLinkedHashMap 
    _jclassLinkedHashMap = J.java.util.LinkedHashMap # in order to guarantee order of keys
    global _jclassHashMap
    _jclassHashMap = J.java.util.HashMap

    global _jclassFile
    _jclassFile = J.java.io.File


###################################### Design ###################################

## Apply a design of experiments through Funz environment on a response surface.
# @param design Design of Expetiments (DoE) given by its name (for instance ""). See _Funz_Designs global var for a list of possible values.
# @param input_variables list of variables definition in a String (for instance x1="[-1,1]")
# @param options list of options to pass to the DoE. All options not given are set to their default values. Note that '_' char in names will be replaced by ' '.
# @param fun response surface as a target (say objective when optimization) function of the DoE. This should include calls to Funz_Run() function.
# @param fun_control.cache set to True if you wish to search in previous evaluations of fun befaore launching a new experiment. Sometimes useful when design asks for same experiments many times. Always False if fun is not repeatible.
# @param fun_control.vectorize Set to "fun" if fun accepts nrows>1 input. Set to "for" (by default) to use a for loop over argument arrays, "multiprocessing" if delegating to 'multiprocessing' the parallelization of separate 'fun' calls (packages multiprocessing required).
# @param fun_control.vectorize_by set the number of parallel execution. By default, set to the number of core of your computer (if known, otherwise set to 4).
# @param monitor_control.results_tmp lsit of design results to deisplay at each batch. True means "all", None/False means "none".
# @param archive_dir define an arbitrary output directory where results (log, images) are stored.
# @param verbosity print (lot of) information while running.
# @param verbose_level deprecated verbosity
# @param vargs optional parameters passed to 'fun'
# @return list of results from this DoE.
# @example def f(x): return(x['a']*x['b']) ; Funz_Design(f,design = "GradientDescent", options = {'nmax':10},input_variables = {'a':"[0,1]",'b':"[1,2]"})
def Funz_Design(fun,design,options=None,input_variables=None,fun_control={'cache':False,'vectorize':"for",'vectorize_by':1},monitor_control={'results_tmp':True},archive_dir=None,verbosity=0,verbose_level=None,log_file=True,*vargs):
    if (not verbose_level is None) & (verbosity != verbose_level) : verbosity = verbose_level

    global _Funz_Last_design
    _Funz_Last_design = {'design':design,'options':options,'fun':fun,'input_variables':input_variables,'fun_control':{'cache':fun_control.get('cache',False),'vectorize':fun_control.get('vectorize',"for"),'vectorize_by':fun_control.get('vectorize_by',1)},'monitor_control':{'results_tmp':monitor_control.get('results_tmp',True)},'archive_dir':archive_dir,'verbosity':verbosity,'log_file':log_file,'optargs':vargs}

    if design is None:
        raise Exception("Design 'design' must be specified.\n Available: "+str(_Funz_Designs))

    if '_Funz_Designs' in globals():
        if (not design is None) & (not design in _Funz_Designs):
            raise Exception("Design "+design+" is not available in this Funz workbench ("+str(_Funz_Designs)+")")

    if input_variables is None:
        raise Exception("Input variables 'input_variables' must be specified.")

    if fun is None:
        raise Exception("Function 'fun' must be specified.")

    if "vectorize" in fun_control.keys() :
        if (fun_control['vectorize']=="multiprocessing") :
            import multiprocessing
        if not "vectorize_by" in fun_control.keys() :
            fun_control['vectorize_by']=4

    init = Funz_Design_init(design,options,input_variables,archive_dir,verbosity,log_file)
    X = init['X']
    designshell = init['designshell']
    
    designshell.setCacheExperiments(("cache" in fun_control.keys()) and (fun_control['cache']==True))

    it = 1
    global _Funz_done 
    _Funz_done = False
    while True:
        X = Funz_Design_next(designshell,X,fun,fun_control,*vargs)

        if X is None:
            break

        if "results_tmp" in monitor_control.keys():
            jresultstmp = designshell.getLoopDesign().getResultsTmp()
            if not jresultstmp is None:
                resultstmp = _JMapToPMap(jresultstmp )
                _Funz_Last_design['resultstmp'] = resultstmp
                if verbosity>0:
                    if monitor_control['results_tmp'] == True :
                        for i in resultstmp.keys():
                            print(i+"\n  "+resultstmp[i]+"\n",end='')
                    else:
                        if len(monitor_control['results_tmp']) > 0 : 
                            for i in monitor_control['results_tmp'] :
                                print(i+"\n  "+resultstmp[i]+"\n",end='')

        #print(end='', _jcall(designshell,JNI.String,"finishedExperimentsInformation"));
        if verbosity>0: 
            print(str(it)+"th iteration\n",end='')
            print(designshell.getLoopDesign().nextExperimentsInformation(),end='')
        it = it+1;
    _Funz_done = True

    return(Funz_Design_results(designshell))


## Initialize a design of experiments through Funz environment.
# @param design Design of Expetiments (DoE) given by its name (for instance ""). See _Funz_Designs global var for a list of possible values.
# @param input_variables list of variables definition in a String (for instance 'x1':"[-1,1]")
# @param options list of options to pass to the DoE. All options not given are set to their default values. Note that '_' char in names will be replaced by ' '.
# @param archive_dir define an arbitrary output directory where results (log, images) are stored.
# @param verbosity print (lot of) information while running.
# @return list of experiments to perform ("X"), and Java shell obejct.
def Funz_Design_init(design,options=None,input_variables=None,archive_dir=None,verbosity=0,log_file=True) :
    if not '_Funz_Last_design' in globals():
        global _Funz_Last_design
        _Funz_Last_design = {}

    # Build input as a HashMap<String, String>
    jinput_variables = _jclassHashMap()
    for key in input_variables.keys():
        if input_variables[key] is None:
            values = "[0,1]"
        else:
            values = input_variables[key]
        jinput_variables.put(key, values)

    # Set design options
    joptions = _jclassHashMap()
    if not options is None:
        for key in options.keys():
            joptions.put(key, J.java.lang.String(str(options[key])))
    else:
        if verbosity>0: 
            print("Using default options\n",end='')
    

    # Let's instanciate the workbench
    designshell = _jclassDesignShell(None,design,jinput_variables,joptions)
    _Funz_Last_design['designshell'] = designshell
    designshell.setVerbosity(verbosity)

    # If no output dir is provided, use current one
    if archive_dir is None: 
        archive_dir = os.getcwd()
    archive_dir = os.path.realpath(archive_dir)
    designshell.setArchiveDirectory(archive_dir)
    if verbosity>0:
        print("Using archive directory: ", end='')
        print(archive_dir)
        
    if log_file:
        # Then redirect output/error streams in the archive dir
        designshell.redirectOutErr() # to keep log of in/err streams
    elif isinstance(log_file, str):
        runshell.redirectOutErr(_jclassFile(log_file))

    if not joptions is None :
        designshell.setDesignOptions(joptions)

    designshell.buildDesign()
    X = _JMapToPMap(designshell.getLoopDesign().initDesign())
    _Funz_Last_design['initDesign'] = X
    if verbosity>0: 
        print("Initial design\n",end='')
        print(designshell.getLoopDesign().nextExperimentsInformation(),end='')
        
    _Funz_Last_design['X'] = X

    return({'X':X,'designshell':designshell})


## Continue a design of experiments through Funz environment on a response surface.
# @param designshell Java shell object holding the design of expetiments.
# @param fun response surface as a target (say objective when optimization) function of the DoE. This should include calls to Funz_Run() function.
# @param fun_control.cache set to True if you wish to search in previous evaluations of fun before launching a new experiment. Sometimes useful when design asks for same experiments many times. Always False if fun is not repeatible.
# @param fun_control.vectorize Set to "fun" (by default) if fun accepts nrows>1 input. Set to "for" to use a for loop over argument arrays, "multiprocessing" if delegating to 'multiprocessing' the parallelization of separate 'fun' calls (packages multiprocessing required).
# @param fun_control.vectorize_by set the number of parallel execution. By default, set to the number of core of your computer (if known, otherwise set to 4).
# @param vargs optional parapeters passed to 'fun'
# @return next experiments to perform in this DoE, None if the design is finished.
def Funz_Design_next(designshell,X,fun,fun_control={'cache':False,'vectorize':"for",'vectorize_by':4},verbosity=0,*vargs) :
    if not '_Funz_Last_design' in globals():
        global _Funz_Last_design
        _Funz_Last_design = {}
        
    designshell.addExperiments(designshell.getLoopDesign().getNextExperiments())

    n = len(X[list(X.keys())[0]])
    if n > 0 :
        if (fun_control['vectorize']=="for") | (fun_control['vectorize']==None) :
            if  vargs is None :
                Y = [fun(_getInMapArray(X,i)) for i in range(n)]
            else :
                Y = [fun(_up(_getInMapArray(X,i),vargs)) for i in range(n)]
        elif (fun_control['vectorize']=="fun") :
            if  vargs is None or len(vargs) == 0 :
                Y = fun(X)
            else :
                Y = fun(X.update(vargs))
        elif fun_control['vectorize']=="thread" :
            raise Exception("thread vectorize not yet implemented")
        elif fun_control['vectorize']=="multiprocessing" :
            raise Exception("multiprocessing vectorize not yet implemented")            
        else :
            raise Exception("fun_control.vectorize type '"+fun_control['vectorize']+"' not supported.")
    else :
        Y = []
    Y = {'f':Y}
    _Funz_Last_design['Y'] = Y

    if (Y['f'] is None) | (len(Y['f']) != n) :
        raise Exception("Failed to evaluate 'fun' on experiment sample X: "+print(Y))

    XY = Y
    XY.update(X)
    X = None
    X = designshell.getLoopDesign().nextDesign(_PMapToJMap(XY))
    if not X is None:
        X = _JMapToPMap(X)
    _Funz_Last_design['nextDesign'] = X
    if verbosity>0: 
        print("Next design\n",end='')
        print(designshell.getLoopDesign().nextExperimentsInformation(),end='')

    _Funz_Last_design['X'] = X

    return(X)


## Analyze a design of experiments through Funz environment.
# @param designshell Java shell object holding the design of experiments.
# @return HTML analysis of the DoE.
def Funz_Design_results(designshell) :
    if not '_Funz_Last_design' in globals():
        global _Funz_Last_design
        _Funz_Last_design = {}
        
    results = _JMapToPMap(designshell.getLoopDesign().getResults())
    _Funz_Last_design['results'] = results

    experiments = designshell.getLoopDesign().finishedExperimentsMap()
    _Funz_Last_design['experiments'] = experiments

    results['design'] = _JMapToPMap(experiments)

    _jdelete(designshell)

    return(results)
    

## Conveniency method giving information about a design available as Funz_Design() arg.
# @return information about this design.
def Funz_Design_info(design, input_variables) :
    if design is None:
        raise Exception("Design 'design' must be specified.\n Available: "+str(_Funz_Designs))

    if '_Funz_Designs' in globals():
        if (not design is None) & (not design in _Funz_Designs):
            raise Exception("Design "+design+" is not available in this Funz workbench ("+str(_Funz_Designs)+")")

    # Build input as a HashMap<String, String>
    if input_variables is None:
        raise Exception("Input variables 'input_variables' must be specified.")
    jinput_variables = _jclassHashMap()
    for key in input_variables.keys():
        if input_variables[key] is None:
            values = "[0,1]"
        else:
            values = input_variables[key]
        jinput_variables.put(key, values)

    # Let's instanciate the workbench
    designshell = _jclassDesignShell(None,design,jinput_variables)
    
    info = designshell.information()

    _jdelete(designshell)

    return(info)


    
###################################### Run ######################################

## Call an external (to R) code wrapped through Funz environment.
# @param model name of the code wrapper to use. See _Funz.Models global var for a list of possible values.
# @param input_files list of files to give as input for the code.
# @param input_variables data.frame of input variable values. If more than one experiment (i.e. nrow >1), experiments will be launched simultaneously on the Funz grid.
# @param all_combinations if False, input_variables variables are grouped (default), else, if True experiments are an expaanded grid of input_variables
# @param output_expressions list of interest output from the code. Will become the names() of return list.
# @param archive_dir define an arbitrary output directory where results (cases, csv files) are stored.
# @param verbosity print (lot of) information while running.
# @param verbose_level deprecated verbosity
# @param run_control.force_retry
# @param run_control.cache_dir
# @param monitor_control.sleep delay time between two checks of results.
# @param monitor_control.display_fun a function to display project cases status. Argument passed to is the data.frame of DoE state.
# @return list of array results from the code, arrays size being equal to input_variables arrays size.
# @example Funz_Run("R", os.path.join(_FUNZ_HOME,"samples","branin.R"),{'x1':numpy.random.uniform(size=10), 'x2':numpy.random.uniform(size=10)}, "cat")
def Funz_Run(model=None, input_files=None, input_variables=None, all_combinations=False, output_expressions=None, run_control={'force_retry':2, 'cache_dir':None}, archive_dir=None, verbosity=0, verbose_level=None, log_file=True, monitor_control={'sleep':5, 'display_fun':None}):   
    if input_files is None: raise Exception("Input files has to be defined")
    if not isinstance(input_files, list): input_files = [input_files]

    if (not verbose_level is None) & (verbosity != verbose_level) : verbosity = verbose_level

    global _Funz_Last_run
    _Funz_Last_run = {'model':model,'input_files':input_files,'input_variables':input_variables,'output_expressions':output_expressions,'archive_dir':archive_dir,'run_control':{'force_retry':run_control.get('force_retry',2),'cache_dir':run_control.get('cache_dir',None)},'verbosity':verbosity,'log_file':log_file,'monitor_control':{'sleep':monitor_control.get('sleep',5),'display_fun':monitor_control.get('display_fun',None)}}

    if '_Funz_Models' in globals():
        if (not model is None) & (not model in _Funz_Models):
            raise Exception("Model "+model+" is not available in this Funz workbench ("+str(_Funz_Models)+")")

    if model is None:
        model = ""
        if verbosity>0: print("Using default model.")

    runshell = Funz_Run_start(model,input_files,input_variables,all_combinations,output_expressions,run_control,archive_dir,verbosity,log_file)

    #runshell.setRefreshingPeriod(_jlong(1000*monitor_control.sleep))

    finished = False
    pointstatus = "-"
    new_pointstatus = "-"
    while not finished:
        global _Funz_done
        try:
             _Funz_done = False
             time.sleep(monitor_control['sleep'])
             state = runshell.getState()

             if bool(re.search('Failed!',state)):
                 raise Exception("Run failed:\n"+ _jclassFormat.ArrayMapToMDString(runshell.getResultsArrayMap()))

             finished = (bool(re.search('Over.',state)) | bool(re.search('Failed!',state)) | bool(re.search('Exception!!',state)))

             if verbosity>0: 
                 print("\r" + state,end="") 

             if callable(monitor_control['display_fun']):
                 new_pointstatus = runshell.getCalculationPointsStatus()
                 if new_pointstatus != pointstatus :
                     monitor_control['display_fun'](new_pointstatus)
                     pointstatus = new_pointstatus
             _Funz_done = True
        except KeyboardInterrupt:
            if verbosity>0: print("Interrupt !")
            runshell.stopComputation()
        except:
            pass
        #finally:
        # if(not _Funz_done) {
        #    print(end='', "Terminating run...")
        #    runshell.shutdown()
        #    print(end='', " ok.\n")

    results = Funz_Run_results(runshell,verbosity)

    try: 
        runshell.shutdown() 
    except: 
        pass

    return(results)

## Initialize a Funz shell to perform calls to an external code.
# @param model name of the code wrapper to use. See _Funz.Models global var for a list of possible values.
# @param input_files list of files to give as input for the code.
# @param input_variables data.frame of input variable values. If more than one experiment (i.e. nrow >1), experiments will be launched simultaneously on the Funz grid.
# @param all_combinations if False, input_variables variables are grouped (default), else, if True experiments are an expaanded grid of input_variables
# @param output_expressions list of interest output from the code. Will become the names() of return list.
# @param archive_dir define an arbitrary output directory where results (cases, csv files) are stored.
# @param verbosity print (lot of) information while running.
# @param run_control.force_retry
# @param run_control.cache_dir
# @return a Java shell object, which calculations are started.
def Funz_Run_start(model,input_files,input_variables=None,all_combinations=False,output_expressions=None,run_control={'force_retry':2,'cache_dir':None},archive_dir=None,verbosity=0,log_file=True) :
    if not '_Funz_Last_run' in globals():
        global _Funz_Last_run
        _Funz_Last_run = {}

    # Check (and wrap to Java) input files.
    JArrayinput_files = _PFileArrayToJFileArray(input_files)

    # First, process the input design, because if it includes a call to Funz itself (compisition of Funz functions), it will lock Funz as long as nothing is returned.
    if not input_variables is None:
        JMapinput_variables = _jclassLinkedHashMap()
        for key in input_variables.keys():
            vals = input_variables[key]
            if isinstance(vals,numpy.ndarray): # convert to standard python arrays
                vals = vals.tolist()
            if isinstance(vals,list):
                if len(vals)>0:
                    JMapinput_variables.put(key, _JArray([str(v) for v in vals],"java.lang.String"))
                else:
                    JMapinput_variables.put(key, _JArray([],"java.lang.String"))
            else:
                JMapinput_variables.put(key, _JArray([str(vals)],"java.lang.String"))
    else:
        JMapinput_variables = None
        if verbosity>0: 
            print("Using default input design.")

    # Let's instanciate the workbench
    if "_Funz_Last_run" in globals(): 
        if 'runshell' in _Funz_Last_run.keys():
            if verbosity>0: print("Terminating previous run...", end='')
            try: 
                _Funz_Last_run['runshell'].shutdown()
            except: 
                pass
            if verbosity>0: print(" ok.")

    runshell = J.org.funz.api.RunShell_v1(model,JArrayinput_files,_gateway.new_array(J.java.lang.String,0))
    runshell.setVerbosity(verbosity)
    global _Funz_Last_run_runshell
    _Funz_Last_run['runshell'] = runshell
    #try: runshell.trap("INT")) # to not allow ctrl-c to stop whole JVM, just this runshell

    # Manage the output : if nothing is provided, use default one from plugin
    if output_expressions is None: 
        output_expressions = runshell.getOutputAvailable()
        if verbosity>0:
            print("Using default output expressions: ", end='')
            print(_JArrayToPArray(output_expressions))
    runshell.setOutputExpressions(_JArray(output_expressions,"java.lang.String"))
    _Funz_Last_run['output_expressions'] = _JArrayToPArray(output_expressions)

    # If no output dir is provided, use current one
    if archive_dir is None: 
        archive_dir = os.getcwd()
    archive_dir = os.path.realpath(archive_dir)
    runshell.setArchiveDirectory(archive_dir)
    if verbosity>0:
        print("Using archive directory: ", end='')
        print(archive_dir)

    if log_file:
        # Then redirect output/error streams in the archive dir
        runshell.redirectOutErr() # to keep log of in/err streams
    elif isinstance(log_file, str):
        runshell.redirectOutErr(_jclassFile(log_file))

    # Now, if input design was provided, use it. Instead, default parameters values will be used.
    if not JMapinput_variables is None:
        if not all_combinations:
            runshell.setInputVariablesGroup(".g",JMapinput_variables)
        else:
            runshell.setInputVariables(JMapinput_variables)

    # load project properties, retries, cacheDir, minCPU, _..
    if not run_control is None:
        for rc in run_control.keys():
            if rc=="force_retry": # Set number of retries
                runshell.setProjectProperty("retries",str(run_control['force_retry']))
            elif rc=="cache_dir": # Finally, adding cache if needed
                for cdir in _PArray(run_control['cache_dir']):
                    print(cdir)
                    runshell.addCacheDirectory(_jclassFile(cdir))
            else:
                runshell.setProjectProperty(rc,run_control[rc])

    # Everything is ok. let's run calculations now ! 
    runshell.startComputation()

    return(runshell)
    

## Parse a Java shell object to get its results.
# @param runshell Java shell object to parse.
# @param verbosity print (lot of) information while running.
# @return list of array design and results from the code, arrays size being equal to input_variables arrays size.
def Funz_Run_results(runshell,verbosity):
    if not '_Funz_Last_run' in globals():
        global _Funz_Last_run
        _Funz_Last_run = {}
    
    results = _JMapToPMap(runshell.getResultsArrayMap())
    _Funz_Last_run['results'] = results

    return(results)


## Conveniency test & information of Funz_Run model & input.
# @return general information concerning this model/input combination.
def Funz_Run_info(model=None,input_files=None):
    if "_Funz_Models" in globals():
        if (not model is None) & (not model in _Funz_Models):
            raise Exception("Model " + model +" is not available in this Funz workbench.")

    if model is None:
        model = ""

    # Check (and wrap to Java) input files.
    JArrayinput_files = _PFileArrayToJFileArray(input_files)

    # Let's instanciate the workbench
    shell = J.org.funz.api.RunShell_v1(model,JArrayinput_files,_gateway.new_array(J.java.lang.String,0)) #new(_jclassRunShell,model,JArrayinput_files)

    # Get default variables & results from plugin
    info = _jclassPrint.projectInformation(shell)
    input_ = shell.getInputVariables()
    output = shell.getOutputAvailable()

    return({'info':info,'input':input_,'output':output})


################################## Grid #################################

## Conveniency overview of Funz grid status.
# @return String list of all visible Funz daemons running on the network.
def Funz_GridStatus():
    comps = [l.split('|')[2:9] for l in _jclassPrint.gridStatusInformation().replace("\t","").split("\n")]
    l = {}
    for t in range(0,len(comps[0])):
        l[comps[0][t].strip()] = [c[t].strip() for c in comps[1:len(comps)-1]]
    return(l)


################################## Utils #################################
#
## Conveniency method to find variables & related info. in parametrized file.
# @param model name of the code wrapper to use. See _Funz.Models global var for a list of possible values.
# @param input_files files to give as input for the code.
# @return list of variables & their possible default value
# @example Funz_ParseInput("R", os.path.join(_FUNZ_HOME,"samples","branin.R"))
def Funz_ParseInput(model,input_files):
    if '_Funz_Models' in globals():
        if (not model is None) & (not model in _Funz_Models):
            raise Exception("Model "+model+" is not available in this Funz workbench ("+str(_Funz_Models)+")")

    # Check (and wrap to Java) input files.
    JArrayinput_files = _PFileArrayToJFileArray(input_files)

    return(_jclassUtils.findVariables("" if model is None else model,JArrayinput_files))


## Conveniency method to compile variables in parametrized file.
# @param model name of the code wrapper to use. See _Funz.Models global var for a list of possible values.
# @param input_files files to give as input for the code.
# @param input_values list of variable values to compile.
# @param output_dir directory where to put compiled files.
# @example Funz_CompileInput(model = "R", input_files = os.path.join(_FUNZ_HOME,"samples","branin.R"),input_values = {'x1':1, 'x2':.5},output_dir=".")
# @example Funz.CompileInput("R", os.path.join(_FUNZ_HOME,"samples","branin.R"),{'x1':[1,2], 'x2':[.3,.5]},".")
def Funz_CompileInput(model,input_files,input_values,output_dir=".") :
    if '_Funz_Models' in globals():
        if (not model is None) & (not model in _Funz_Models):
            raise Exception("Model "+model+" is not available in this Funz workbench ("+str(_Funz_Models)+")")

    # Check (and wrap to Java) input files.
    JArrayinput_files = _PFileArrayToJFileArray(input_files)

    # Process the input values
    JMapinput_values = _jclassLinkedHashMap()
    for key in input_values.keys():
        vals = input_values[key]
        if isinstance(vals,numpy.array): # convert to standard python arrays
            vals = vals.tolist()
        if isinstance(vals,list):
            if len(vals)>0:
                JMapinput_values.put(key, _JArray([str(v) for v in vals],"java.lang.String"))
            else:
                JMapinput_values.put(key, _JArray([],"java.lang.String"))
        else:
            JMapinput_values.put(key, _JArray([str(vals)],"java.lang.String"))
        #JMapinput_values.put(key, str(input_values[key]).replace("[","{").replace("]","}")) # because funz waits for the array of values between{}

    output_dir = os.path.realpath(output_dir)

    return(_jclassUtils.compileVariables("" if model is None else model,JArrayinput_files,JMapinput_values,_jclassFile(output_dir)))


## Conveniency method to find variables & related info. in parametrized file.
# @param model name of the code wrapper to use. See _Funz.Models global var for a list of possible values.
# @param input_files files given as input for the code.
# @param output_dir directory where calculated files are.
# @return list of outputs & their value
# @example Funz_ReadOutput("R", os.path.join(_FUNZ_HOME,"samples","branin.R"), os.path.join(_FUNZ_HOME,"samples"))
def Funz_ReadOutput(model, input_files, output_dir) :
    if '_Funz_Models' in globals():
        if (not model is None) & (not model in _Funz_Models):
            raise Exception("Model "+model+" is not available in this Funz workbench ("+str(_Funz_Models)+")")

    # Check (and wrap to Java) input files.
    JArrayinput_files = _PFileArrayToJFileArray(input_files)

    return(_JMapToPMap(_jclassUtils.readOutputs("" if model is None else model,JArrayinput_files,_jclassFile(output_dir))))


################################## Run & Design #################################

# Call an external (to R) code wrapped through Funz environment.
#' @param TODO
#' @return list of array design and results from the code.
#' @example Funz_RunDesign(model="R", input_files=os.path.join(FUNZ_HOME,"samples","branin.R"), output_expressions="cat", design = "gradientdescent", design_options = {nmax=5),input_variables = {x1="[0,1]",x2="[0,1]"))
#' @example Funz_RunDesign("R", os.path.join(FUNZ_HOME,"samples","branin.R"), "cat", "gradientdescent", {nmax:5}, {x1:"[0,1]",x2:[0,1]})
def Funz_RunDesign(model=None,input_files=None,output_expressions=None,design=None,input_variables=None,design_options=None,run_control={'force_retry':2,'cache_dir':None},monitor_control={'results_tmp':True,'sleep':5,'display_fun':None},archive_dir=None,verbosity=0,verbose_level=None,log_file=True) :
    if input_files is None: raise Exception("Input files has to be defined")
    if not isinstance(input_files, list): input_files = [input_files]

    if (not verbose_level is None) & (verbosity != verbose_level) : verbosity = verbose_level

    global _Funz_Last_rundesign
    _Funz_Last_rundesign = {'model':model,'input_files':input_files,'input_variables':input_variables,'output_expressions':output_expressions,'design':design,'design_options':design_options,'input_variables':input_variables,'run_control':{'force_retry':run_control.get('force_retry',2),'cache_dir':run_control.get('cache_dir',None)},'monitor_control':{'results_tmp':monitor_control.get('results_tmp',True),'sleep':monitor_control.get('sleep',5),'display_fun':monitor_control.get('display_fun',None)},'archive_dir':archive_dir,'verbosity':verbosity,'log_file':log_file}

    if '_Funz_Models' in globals():
        if (not model is None) & (not model in _Funz_Models):
            raise Exception("Model "+model+" is not available in this Funz workbench ("+str(_Funz_Models)+")")

    if model is None:
        model = ""
        if verbosity>0: print("Using default model.")

    if design is None:
        raise Exception("Design 'design' must be specified.\n Available: "+str(_Funz_Designs))

    if '_Funz_Designs' in globals():
        if (not design is None) & (not design in _Funz_Designs):
            raise Exception("Design "+design+" is not available in this Funz workbench ("+str(_Funz_Designs)+")")

    if input_variables is None:
        raise Exception("Input variables 'input_variables' must be specified.")

    shell = Funz_RunDesign_start(model,input_files,output_expressions,design,input_variables,design_options,run_control,archive_dir,verbosity,log_file)

    #shell.setRefreshingPeriod(_jlong(1000*monitor_control.sleep))
    
    finished = False
    state = ""
    status = "-"
    new_status = "-"
    while not finished:
        global _Funz_done
        try: 
             _Funz_done = False
             time.sleep(monitor_control['sleep'])
             state = shell.getState()

             if bool(re.search('Failed!',state)):
                 raise Exception("Run failed:\n"+ _jclassFormat.ArrayMapToMDString(runshell.getResultsArrayMap()))

             finished = (bool(re.search('Over.',state)) | bool(re.search('Failed!',state)) | bool(re.search('Exception!!',state)))

             if verbosity>0: 
                 print("\r" + state.replace("\n"," | "),end="") 

             if callable(monitor_control['display_fun']):
                 new_status = shell.getCalculationPointsStatus()
                 if new_status != status :
                     monitor_control['display_fun'](new_status)
                     status = new_status
             _Funz_done = True
        except KeyboardInterrupt:
            if verbosity>0: print("Interrupt !")
            shell.stopComputation()
        except:
            pass
        #finally:
        # if(not _Funz_done) {
        #    print(end='', "Terminating run...")
        #    runshell.shutdown()
        #    print(end='', " ok.\n")

    results = Funz_RunDesign_results(shell,verbosity)

    try: 
        shell.shutdown() 
    except: 
        pass

    return(results)


#' Initialize a Funz shell to perform calls to an external code.
#' @param
#' @return a Java shell object, which calculations are started.
#' @example Funz_RunDesign_start("[R]", os.path.join(FUNZ_HOME,"samples","branin.R"),"z","Conjugate Gradient",{a:numpy.random.uniform(size=10), b:"[0,1]"},{Maximum_iterations:10))
def Funz_RunDesign_start(model,input_files,output_expressions=None,design=None,input_variables=None,design_options=None,run_control={'force_retry':2,'cache_dir':None},archive_dir=None,verbosity=0,log_file=True) :
    if not '_Funz_Last_rundesin' in globals():
        global _Funz_Last_rundesign
        _Funz_Last_rundesign = {}

    # Check (and wrap to Java) input files.
    JArrayinput_files = _PFileArrayToJFileArray(input_files)

    # First, process the input design, because if it includes a call to Funz itself (compisition of Funz functions), it will lock Funz as long as nothing is returned.
    if not input_variables is None:
        JMapinput_variables = _jclassLinkedHashMap()
        for key in input_variables.keys():
            vals = input_variables[key]
            if vals is None:
                JMapinput_variables.put(key, "[0,1]")
            if isinstance(vals,numpy.ndarray): # convert to standard python arrays
                vals = vals.tolist()
            if isinstance(vals,list):
                if len(vals)>0:
                    JMapinput_variables.put(key, _JArray([str(v) for v in vals],"java.lang.String"))
                else:
                    JMapinput_variables.put(key, _JArray([],"java.lang.String"))
            else:
                JMapinput_variables.put(key, str(vals))
    else:
        JMapinput_variables = None
        if verbosity>0: 
            print("Using default input values.")

    if design is None: design="No design of experiments"
#    # Set design options
    joptions = _jclassHashMap()
    if not design_options is None:
        for key in design_options.keys():
            joptions.put(key, J.java.lang.String(str(design_options[key])))
    else:
        if verbosity>0: 
            print("Using default options\n",end='')
    # Let's instanciate the workbench
    if "_Funz_Last_rundesin" in globals(): 
        if 'shell' in _Funz_Last_rundesign.keys():
            if verbosity>0: print("Terminating previous run...", end='')
            try: 
                _Funz_Last_rundesign['shell'].shutdown()
            except: 
                pass
            if verbosity>0: print(" ok.")

    shell = J.org.funz.api.Shell_v1(model,JArrayinput_files,output_expressions, design, JMapinput_variables, joptions)
    shell.setVerbosity(verbosity)
    _Funz_Last_rundesign['shell'] = shell
    #try: shell.trap("INT")) # to not allow ctrl-c to stop whole JVM, just this runshell
    
    # Manage the output : if nothing is provided, use default one from plugin
    if output_expressions is None: 
        output_expressions = shell.getOutputAvailable()
        if verbosity>0:
            print("Using default output expressions: ", end='')
            print(_JArrayToPArray(output_expressions))
        shell.setOutputExpressions(_JArray(output_expressions,"java.lang.String"))
    _Funz_Last_rundesign['output_expressions'] = output_expressions
    
    # If no output dir is provided, use current one
    if archive_dir is None: 
        archive_dir = os.getcwd()
    archive_dir = os.path.realpath(archive_dir)
    shell.setArchiveDirectory(archive_dir)
    if verbosity>0:
        print("Using archive directory: ", end='')
        print(archive_dir)
    
    if log_file:
        # Then redirect output/error streams in the archive dir
        shell.redirectOutErr() # to keep log of in/err streams
    elif isinstance(log_file, str):
        shell.redirectOutErr(_jclassFile(log_file))

    # load project properties, retries, cacheDir, minCPU, _..
    if not run_control is None:
        for rc in run_control.keys():
            if rc=="force_retry": # Set number of retries
                shell.setProjectProperty("retries",str(run_control['force_retry']))
            elif rc=="cache_dir": # Finally, adding cache if needed
                for cdir in _PArray(run_control['cache_dir']):
                    print(cdir)
                    shell.addCacheDirectory(_jclassFile(cdir))
            else:
                shell.setProjectProperty(rc,run_control[rc])

    # Everything is ok. let's run calculations now ! 
    shell.startComputation()

    return(shell)


#' Parse a Java shell object to get its results.
#' @param shell Java shell object to parse.
#' @param verbosity print (lot of) information while running.
#' @return list of array design and results from the code.
#' @example TODO
def Funz_RunDesign_results(shell,verbosity) :
    if not '_Funz_Last_rundesign' in globals():
        global _Funz_Last_rundesign
        _Funz_Last_rundesign = {}

    results = _JMapToPMap(shell.getResultsArrayMap())
    _Funz_Last_rundesign['results'] = results

    return(results)
 
 
###############################################################################
 


#Funz_init('/home/richet/Sync/Open/Funz/1.9/applications/funz-client/dist',verbosity=10)

#def f(x): return(x['a']*x['b']) ; 

#f=Funz_RunDesign(model="R",input_files="dist/samples/branin.R",output_expressions="cat",design = "GradientDescent", design_options = {'nmax':3},input_variables= {'x1':[0,1],'x2':"[1,2]"},verbosity=10)
