cat <<EOF > run.sh
FUNZ_HOME="dist"
\$FUNZ_HOME/Funz.sh Run -m R -if src/test/samples/branin.R -iv x1=.1,.2,.3,.4,.5,.6,.7,.8,.9 x2=.1,.2,.3,.4,.5,.6,.7,.8,.9 -all -v 5
EOF

# rm run.out
# sh run.sh 2>&1 > run.out &
# PID_RUN=$!

rm calc.out
LIB=`find dist/lib -name "funz-core-*.jar"`:`find dist/lib -name "funz-calculator-*.jar"`:dist/lib/commons-io-2.4.jar:dist/lib/commons-exec-1.1.jar:dist/lib/commons-lang-2.6.jar:dist/lib/ftpserver-core-1.1.1.jar:dist/lib/ftplet-api-1.1.1.jar:dist/lib/mina-core-2.0.16.jar:dist/lib/sigar-1.6.6.jar:dist/lib/slf4j-api-1.5.2.jar:dist/lib/slf4j-log4j12-1.5.2.jar
java -Dapp.home=dist -classpath $LIB org.funz.calculator.Calculator file:calculator.xml 2>&1 > calc.out &
PID_CALCULATOR=$!

## for loop testing of previous Run only. Comment otherwis
# wait $PID_RUN
# kill -9 $PID_CALCULATOR

ok3="1"
while true
do
    rm run.out
    sh run.sh 2>&1 > run.out &
    PID_RUN=$!
    wait $PID_RUN

    ok3=`tail -100 run.out | grep "Batch over" | wc -l`
    if [ ! $ok3 = "1" ]; then echo "FAILED to complete calculation"; exit 3; fi
done

# sleep 10

# kill -9 $PID_CALCULATOR

# sleep 3

# ok1=`ps | grep $PID_RUN | wc -l`
# if [ ! $ok1 = "1" ]; then echo "FAILED to stop calculation"; exit 1; fi

# rm calc.out
# java -Dapp.home=dist -classpath $LIB org.funz.calculator.Calculator file:calculator.xml 2>&1 > calc.out &
# PID_CALCULATOR=$!

# sleep 10

# kill -9 $PID_CALCULATOR

# ok2=`ps | grep $PID_RUN | wc -l`
# if [ ! $ok2 = "0" ]; then echo "FAILED to finish calculation"; exit 2; fi

# ok3=`grep " 147.92420" run.out | wc -l`
# if [ ! $ok3 = "1" ]; then echo "FAILED to complete calculation"; exit 3; fi
