cat <<EOF > run.sh
FUNZ_HOME="dist"
\$FUNZ_HOME/Funz.sh Run -m R -if src/test/samples/branin.R -iv x1=.1,.2,.3,.4,.5,.6,.7,.8,.9 x2=.1,.2,.3,.4,.5,.6,.7,.8,.9 -rc blacklistTimeout=1 -v 5
EOF

rm Run.log
rm run.out
sh run.sh 2>&1 > run.out &
PID_RUN=$!

ok1=`ps | grep $PID_RUN | grep Funz.sh | wc -l`
if [ ! $ok1 = "1" ]; then echo "FAILED to start client: $ok1"; ps aux > ps.out; echo "* ps.out"; cat ps.out; echo "* run.out"; cat run.out; echo "* Run.log"; cat Run.log; exit 1; fi
echo "OK started client"

rm calc.out
FUNZ_HOME="../funz-calculator/dist"
LIB=`find $FUNZ_HOME/lib -name "funz-core-*.jar"`:`find $FUNZ_HOME/lib -name "funz-calculator-*.jar"`:`find $FUNZ_HOME/lib -name "commons-io-*.jar"`:`find $FUNZ_HOME/lib -name "commons-exec-*.jar"`:`find $FUNZ_HOME/lib -name "commons-lang-*.jar"`:`find $FUNZ_HOME/lib -name "ftpserver-core-*.jar"`:`find $FUNZ_HOME/lib -name "ftplet-api-*.jar"`:`find $FUNZ_HOME/lib -name "mina-core-*.jar"`:`find $FUNZ_HOME/lib -name "sigar-*.jar"`:`find $FUNZ_HOME/lib -name "slf4j-api-*.jar"`:`find $FUNZ_HOME/lib -name "slf4j-log4j*.jar"`
java -Dapp.home=$FUNZ_HOME -classpath $LIB org.funz.calculator.Calculator file:dist/calculator.xml 2>&1 > calc.out &
PID_CALCULATOR=$!

sleep 2

ok2=`ps | grep $PID_CALCULATOR | grep java | wc -l`
if [ ! $ok2 = "1" ]; then echo "FAILED to start calculator: $ok2"; ps aux > ps.out; echo "* ps.out"; cat ps.out; kill -9 $PID_RUN; echo "* calc.out"; cat calc.out; exit 2; fi
echo "OK started calculator"

## for loop testing of previous Run only. Comment otherwise
# wait $PID_RUN
# kill -9 $PID_CALCULATOR

sleep 10

echo "///////////////////////////////// KILL CALCULATOR /////////////////////////////////////" >> run.out
echo "" >> run.out
sleep 1
kill -9 $PID_CALCULATOR

sleep 3

ok3=`ps | grep $PID_CALCULATOR | grep java | wc -l`
if [ ! $ok3 = "0" ]; then echo "FAILED to stop calculation: $ok3"; kill -9 $PID_RUN $PID_CALCULATOR; cat calc.out; exit 3; fi
echo "OK to stop calculation"

ok4=`ps | grep $PID_RUN | grep Funz.sh | wc -l`
if [ ! $ok4 = "1" ]; then echo "FAILED to let client alive: $ok4"; kill -9 $PID_RUN $PID_CALCULATOR; echo "* run.out"; cat run.out; echo "* Run.log"; cat Run.log; exit 4; fi
echo "OK client alive"

rm calc.out
java -Dapp.home=$FUNZ_HOME -classpath $LIB org.funz.calculator.Calculator file:dist/calculator.xml 2>&1 > calc1.out &
PID_CALCULATOR1=$!
java -Dapp.home=$FUNZ_HOME -classpath $LIB org.funz.calculator.Calculator file:dist/calculator.xml 2>&1 > calc2.out &
PID_CALCULATOR2=$!

sleep 3

ok5=`ps | grep $PID_RUN | grep Funz.sh | wc -l`
if [ ! $ok5 = "1" ]; then echo "FAILED to restart calculation: $ok5"; kill -9 $PID_RUN $PID_CALCULATOR; cat run.out; exit 5; fi
echo "OK to restart calculation"

done=0
i=0
while [ $i -le 10 ]; do
  ok3=`ps | grep $PID_RUN | grep Funz.sh | wc -l`
  if [ $ok3 = "0" ]; then
      break
  fi
  i=$(( $i + 1 ))
  sleep 10
done

ok6=`ps | grep $PID_RUN | grep Funz.sh | wc -l`
if [ ! $ok6 = "0" ]; then echo "FAILED to finish calculation: $ok6"; kill -9 $PID_RUN $PID_CALCULATOR; cat run.out; exit 6; fi
echo "OK to finish calculation"

ok7=`tail -10 run.out | grep "136.0767" | wc -l`
if [ ! $ok7 = "1" ]; then echo "FAILED to complete calculation: $ok7"; kill -9 $PID_RUN $PID_CALCULATOR; cat run.out; exit 7; fi
echo "OK to complete calculation"

kill -9 $PID_CALCULATOR1 $PID_CALCULATOR2
