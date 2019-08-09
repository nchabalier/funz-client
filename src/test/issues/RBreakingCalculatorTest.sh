cat <<EOF > run.R
FUNZ_HOME="dist"
source(file.path(FUNZ_HOME,"Funz.R"))
Funz.init(FUNZ_HOME)
res=Funz_Run("R","src/test/samples/branin.R",input.variables = list(x1=(1:9)/10,x2=(1:9)/10),run.control=list(blacklistTimeout=1),verbosity=5)
print(unlist(res[['cat']]))
q(save="no")
EOF

rm run.Rout
R CMD BATCH run.R 2>&1 > run.Rout &
PID_R=$!

rm calc.out
FUNZ_HOME="../funz-calculator/dist"
LIB=`find $FUNZ_HOME/lib -name "funz-core-*.jar"`:`find $FUNZ_HOME/lib -name "funz-calculator-*.jar"`:`find $FUNZ_HOME/lib -name "commons-io-*.jar"`:`find $FUNZ_HOME/lib -name "commons-exec-*.jar"`:`find $FUNZ_HOME/lib -name "commons-lang-*.jar"`:`find $FUNZ_HOME/lib -name "ftpserver-core-*.jar"`:`find $FUNZ_HOME/lib -name "ftplet-api-*.jar"`:`find $FUNZ_HOME/lib -name "mina-core-*.jar"`:`find $FUNZ_HOME/lib -name "sigar-*.jar"`:`find $FUNZ_HOME/lib -name "slf4j-api-*.jar"`:`find $FUNZ_HOME/lib -name "slf4j-log4j*.jar"`
java -Dapp.home=$FUNZ_HOME -classpath $LIB org.funz.calculator.Calculator file:$FUNZ_HOME/calculator.xml 2>&1 > calc.out &
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

ok0=`ps | grep $PID_CALCULATOR | grep java | wc -l`
if [ ! $ok0 = "0" ]; then echo "FAILED to stop calculation: $ok0"; kill -9 $PID_R $PID_CALCULATOR; cat calc.out; exit -1; fi
echo "OK to stop calculation"

ok1=`ps | grep $PID_R | grep sh | wc -l`
if [ ! $ok1 = "1" ]; then echo "FAILED to pause client: $ok1"; kill -9 $PID_R $PID_CALCULATOR; cat run.Rout; exit 1; fi
echo "OK to pause client"

rm calc.out
java -Dapp.home=$FUNZ_HOME -classpath $LIB org.funz.calculator.Calculator file:$FUNZ_HOME/calculator.xml 2>&1 > calc.out &
PID_CALCULATOR=$!

sleep 3

ok2=`ps | grep $PID_R | grep sh | wc -l`
if [ ! $ok2 = "1" ]; then echo "FAILED to restart calculation: $ok2"; kill -9 $PID_R $PID_CALCULATOR; cat run.Rout; exit 2; fi
echo "OK to restart calculation"

sleep 60

ok3=`ps | grep $PID_R | grep sh | wc -l`
if [ ! $ok3 = "0" ]; then echo "FAILED to finish calculation: $ok3"; kill -9 $PID_R $PID_CALCULATOR; cat run.Rout; exit 3; fi
echo "OK to finish calculation"

ok4=`tail -10 run.Rout | grep "136.0767" | wc -l`
if [ ! $ok4 = "1" ]; then echo "FAILED to complete calculation: $ok4"; kill -9 $PID_R $PID_CALCULATOR; cat run.Rout; exit 4; fi
echo "OK to complete calculation"

kill -9 $PID_CALCULATOR
