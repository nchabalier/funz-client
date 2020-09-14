#!/bin/bash
## @Before Run funz daemon to launch 'runshell' calculations

failed="0"

function testDesignParseError {
    $FUNZ_HOME/Funz.sh Design abcdef > testDesignParseError.out 2>&1
    ok=`grep "Unknown option: abcdef" testDesignParseError.out | wc -l`
    if [ $ok = "1" ]; then rm testDesignParseError.out; echo "OK";return 0; else echo "FAILED:"; cat testDesignParseError.out;return 1; fi
}

function testDesign {
    $FUNZ_HOME/Funz.sh Design -d $DESIGN -do nmax=$NMAX epsilon=0.01 delta=$DELTA target=-10 -f ./$FUN -fa x1 x2 -iv x1=[-0.5,-0.1] x2=[0.3,.8] -v 10 -ad tmp/testDesign.sh > testDesign.out 2>&1
    ok=`grep "minimum is $FUN_MIN" testDesign.out | wc -l`
    if [ $ok = "2" ]; then rm testDesign.out; echo "OK";return 0; else echo "FAILED:"; cat testDesign.out;return 1; fi
}

function testDesignPar {
    $FUNZ_HOME/Funz.sh Design -d $DESIGN -do nmax=$NMAX epsilon=0.01 delta=$DELTA target=-10 -f ./$FUN -fa x1 x2 -fp 3 -iv x1=[-0.5,-0.1] x2=[0.3,.8] -v 10 -ad tmp/testDesignPar.sh > testDesignPar.out 2>&1
    ok=`grep "minimum is $FUN_MIN" testDesignPar.out | wc -l`
    if [ $ok = "2" ]; then rm testDesignPar.out; echo "OK";return 0; else echo "FAILED:"; cat testDesignPar.out;return 1; fi
}

FUNZ_HOME="dist"

#source src/test/DesignTest.prop
# Use simplified objective function:
FUN="src/test/samples/mult.sh"
FUN_MIN=-0.4
VERBOSITY=10
DESIGN="GradientDescent"
NMAX=3
DELTA=1

TMP_IN=tmp/branin.R
mkdir tmp
#cp $SRC $TMP_IN

for t in testDesignParseError testDesign testDesignPar; do
    res=`$t`
    if [ $? = "1" ]; then failed="1"; fi
    echo "Test "$t": "$res
done

exit $failed