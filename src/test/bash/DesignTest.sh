#!/bin/bash
## @Before Run funz daemon to launch 'runshell' calculations

function testDesignParseError {
    $FUNZ_HOME/Funz.sh Design abcdef > testDesignParseError.out 2>&1
    ok=`grep "Unknown option: abcdef" testDesignParseError.out | wc -l`
    if [ $ok = "1" ]; then rm testDesignParseError.out; echo "OK"; else echo "FAILED:"; cat testDesignParseError.out; fi
}

function testDesign {
    $FUNZ_HOME/Funz.sh Design -d $DESIGN -do nmax=$NMAX epsilon=0.000000001 delta=0.01 target=-10 -f ./$FUN -fa x1 x2 -iv x1=[-0.5,.0001] x2=[-0.3,.8] -v 10 > testDesign.out 2>&1
    ok=`grep "minimum is $FUN_MIN" testDesign.out | wc -l`
    if [ $ok = "2" ]; then rm testDesign.out; echo "OK"; else echo "FAILED:"; cat testDesign.out; fi
}

function testDesignPar {
    $FUNZ_HOME/Funz.sh Design -d $DESIGN -do nmax=$NMAX epsilon=0.000000001 delta=0.01 target=-10 -f ./$FUN -fa x1 x2 -fp 3 -iv x1=[-0.5,.0001] x2=[-0.3,.8] -v 10 > testDesignPar.out 2>&1
    ok=`grep "minimum is $FUN_MIN" testDesignPar.out | wc -l`
    if [ $ok = "2" ]; then rm testDesignPar.out; echo "OK"; else echo "FAILED:"; cat testDesignPar.out; fi
}

FUNZ_HOME="dist"

source src/test/DesignTest.prop

TMP_IN=tmp/branin.R
mkdir tmp
cp $SRC $TMP_IN

for t in testDesignParseError testDesign testDesignPar; do
    res=`$t`
    echo "Test "$t": "$res
done
