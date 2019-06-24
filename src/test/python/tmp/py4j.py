import py4j.java_gateway

#port = py4j.java_gateway.launch_gateway(classpath="",redirect_stdout=None,redirect_stderr=open("err.txt","w"))
#_gateway = py4j.java_gateway.JavaGateway(gateway_parameters= py4j.java_gateway.GatewayParameters(port=port,auto_convert=True))
#J = _gateway.jvm
#
#s=J.java.lang.System
#sout = s.out
#sout.println("sdsf") Not working



#
#import os
#
#_FUNZ_HOME="/home/richet/Sync/Travail/Funz/1.9/applications/funz-client/dist"
#classpath = [ f for f in os.listdir(os.path.join(_FUNZ_HOME,"lib")) if (os.path.isfile(os.path.join(os.path.join(_FUNZ_HOME,"lib"), f)) & ((os.path.splitext(f)[1])==".jar")) ]
#classpath.append("/home/richet/opt/anaconda3/share/py4j/py4j0.10.6.jar")
#
#parameters=[]
#port = py4j.java_gateway.launch_gateway(classpath=":".join(os.path.join(_FUNZ_HOME,"lib",str(j)) for j in classpath),javaopts=parameters,redirect_stdout=open("out","w"),redirect_stderr=open("err","w"))
#_gateway = py4j.java_gateway.JavaGateway(gateway_parameters=py4j.java_gateway.GatewayParameters(port=port,auto_convert=True))
#J = _gateway.jvm
#
#class _PAlertCollector():
#
#    def __init__(self, gateway):
#        self.gateway = gateway
#
#    def showInformation(self, string):
#        print("[Info] "+string)
#
#    def showError(self,  string):
#        print("[Error] "+string)
#
#    def showException(self, e):
#        print("[Exception] "+e)
#
#    class Java:
#        implements = ["org.funz.log.AlertCollector"]
#
##_JToP_callback = J.py4j.GatewayServer(J.org.funz.log.Alert(),port)
##_JToP_callback.start()
##_gateway.entry_point.setCollector(alert)
#
#alert = _PAlertCollector(_gateway)
#a = J.org.funz.log.Alert 
#a.setCollector(alert)
#a.showInformation("OK !") Not working...









import os
from py4j.compat import Queue

_FUNZ_HOME="/home/richet/Sync/Travail/Funz/1.9/applications/funz-client/dist"
classpath = [ f for f in os.listdir(os.path.join(_FUNZ_HOME,"lib")) if (os.path.isfile(os.path.join(os.path.join(_FUNZ_HOME,"lib"), f)) & ((os.path.splitext(f)[1])==".jar")) ]
classpath.append("/home/richet/opt/anaconda3/share/py4j/py4j0.10.6.jar")


class Sysout(Queue) :
    
    def put(self,o):
        print(o)

qout = Sysout()
qerr = Sysout()


parameters=[]
port = py4j.java_gateway.launch_gateway(classpath=":".join(os.path.join(_FUNZ_HOME,"lib",str(j)) for j in classpath),javaopts=parameters,redirect_stdout=qout,redirect_stderr=qerr)
_gateway = py4j.java_gateway.JavaGateway(gateway_parameters=py4j.java_gateway.GatewayParameters(port=port,auto_convert=True))
J = _gateway.jvm

J.org.funz.log.Alert.showInformation("OK !")
J.org.funz.log.Alert.showError("OK !!!!")