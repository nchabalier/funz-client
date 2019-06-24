cat <<EOF > run.R
FUNZ_HOME="dist"
source(file.path(FUNZ_HOME,"Funz.R"))
Funz.init(FUNZ_HOME)
res=Funz_Run("R","src/main/resources/samples/branin.R",input.variables = list(x1=(1:9)/10,x2=(1:9)/10),verbosity=5)
print(unlist(res[['cat']]))
q(save="no")
EOF

rm run.Rout
R CMD BATCH run.R 2>&1 > run.Rout &
PID_R=$!

rm calc.out
LIB=`find dist/lib -name "funz-core-*.jar"`:`find dist/lib -name "funz-calculator-*.jar"`:dist/lib/commons-io-2.4.jar:dist/lib/commons-exec-1.1.jar:dist/lib/commons-lang-2.6.jar:dist/lib/ftpserver-core-1.1.1.jar:dist/lib/ftplet-api-1.1.1.jar:dist/lib/mina-core-2.0.16.jar:dist/lib/sigar-1.6.6.jar:dist/lib/slf4j-api-1.5.2.jar:dist/lib/slf4j-log4j12-1.5.2.jar
java -Dapp.home=dist -classpath $LIB org.funz.calculator.Calculator file:dist/calculator.xml 2>&1 > calc.out &
PID_CALCULATOR=$!

## for loop testing of previous Run only. Comment otherwis
# wait $PID_R
# kill -9 $PID_CALCULATOR

sleep 10

echo "///////////////////////////////// KILL CALCULATOR /////////////////////////////////////" >> run.Rout
echo "" >> run.Rout
sleep 1
kill -9 $PID_CALCULATOR

sleep 3

ok0=`ps | grep $PID_CALCULATOR | wc -l`
if [ ! $ok0 = "0" ]; then echo "FAILED to stop calculation"; kill -9 $PID_R $PID_CALCULATOR; exit -1; fi
echo "OK to stop calculation"


ok1=`ps | grep $PID_R | wc -l`
if [ ! $ok1 = "1" ]; then echo "FAILED to pause client"; kill -9 $PID_R $PID_CALCULATOR; exit 1; fi
echo "OK to pause client"

rm calc.out
java -Dapp.home=dist -classpath $LIB org.funz.calculator.Calculator file:dist/calculator.xml 2>&1 > calc.out &
PID_CALCULATOR=$!

sleep 3

ok2=`ps | grep $PID_R | wc -l`
if [ ! $ok2 = "1" ]; then echo "FAILED to restart calculation"; kill -9 $PID_R $PID_CALCULATOR; exit 2; fi
echo "OK to restart calculation"

sleep 20

ok3=`ps | grep $PID_R | wc -l`
if [ ! $ok3 = "0" ]; then echo "FAILED to finish calculation"; kill -9 $PID_R $PID_CALCULATOR; exit 3; fi
echo "OK to finish calculation"

ok4=`tail -10 run.Rout | grep "136.0767" | wc -l`
if [ ! $ok4 = "1" ]; then echo "FAILED to complete calculation"; kill -9 $PID_R $PID_CALCULATOR; exit 4; fi
echo "OK to complete calculation"

kill -9 $PID_CALCULATOR
