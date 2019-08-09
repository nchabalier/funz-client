#!/bin/bash
## @Before Run funz daemon to launch 'runshell' calculations

failed="0"

function testRunDesignParseError {
    $FUNZ_HOME/Funz.sh RunDesign abcdef > testRunDesignParseError.out 2>&1
    ok=`grep "Unknown option: abcdef" testRunDesignParseError.out | wc -l`
    if [ $ok = "1" ]; then rm testRunDesignParseError.out; echo "OK";return 0; else echo "FAILED:"; cat testRunDesignParseError.out;return 1; fi
}

function testRun1Design {
    $FUNZ_HOME/Funz.sh RunDesign -m $CODE -d $DESIGN -do nmax=$NMAX -if $TMP_IN -iv x1=.3 x2=[0.3,.4] -v $VERBOSITY -ad tmp > testRun1Design.out 2>&1
    ok=`grep "minimum is " testRun1Design.out | wc -l`
    if [ $ok = "1" ]; then rm testRun1Design.out; echo "OK";return 0; else echo "FAILED:"; cat testRun1Design.out;return 1; fi
}

function testRun2Design {
    $FUNZ_HOME/Funz.sh RunDesign -m $CODE -d $DESIGN -do nmax=$NMAX -if $TMP_IN -iv x1=.3,.5 x2=[0.3,.4] -v $VERBOSITY -ad tmp > testRun2Design.out 2>&1
    ok=`grep "minimum is " testRun2Design.out | wc -l`
    if [ $ok = "2" ]; then rm testRun2Design.out; echo "OK";return 0; else echo "FAILED:"; cat testRun2Design.out;return 1; fi
}

function testRunDesignFailed {
    $FUNZ_HOME/Funz.sh RunDesign -m $CODE -d $DESIGN -do nmax=$NMAX -if $TMP_IN -iv x1=[-1.1,1] x2=[0.3,.4] -v $VERBOSITY -ad tmp -oe x1+min\(cat,10,na.rm=F\) > testRunDesignFailed.out 2>&1
    ok=`grep "RunDesign failed" testRunDesignFailed.out | wc -l`
    if [ $ok = "1" ]; then rm testRunDesignFailed.out; echo "OK";return 0; else echo "FAILED:"; cat testRunDesignFailed.out;return 1; fi
}

function testBraninGradientDescent {
    $FUNZ_HOME/Funz.sh RunDesign -m $CODE -d $DESIGN -do nmax=$NMAX -if $TMP_IN -iv x1=[0,1] x2=[0,1] -do delta=0.1 -v $VERBOSITY -ad tmp > testBraninGradientDescent.out 2>&1
    ok=`grep "$BraninGradientDescent_MIN" testBraninGradientDescent.out | wc -l`
    if [ $ok = "1" ]; then rm testBraninGradientDescent.out; echo "OK";return 0; else echo "FAILED:"; cat testBraninGradientDescent.out;return 1; fi
}

function testBraninGradientDescentx2 {
    $FUNZ_HOME/Funz.sh RunDesign -m $CODE -d $DESIGN -do nmax=$NMAX -if $TMP_IN -iv x1=0,1 x2=[0,1] -do delta=0.1 -v $VERBOSITY -ad tmp > testBraninGradientDescentx2.out 2>&1
    ok1=`grep "$BraninGradientDescentx2_x1_0_MIN" testBraninGradientDescentx2.out | wc -l`
    ok2=`grep "$BraninGradientDescentx2_x1_1_MIN" testBraninGradientDescentx2.out | wc -l`
    if [ $ok1 = "1" ] && [ $ok2 = "1" ]; then rm testBraninGradientDescentx2.out; echo "OK";return 0; else echo "FAILED:"; cat testBraninGradientDescentx2.out;return 1; fi
}

FUNZ_HOME="dist"

source src/test/RunDesignTest.prop

TMP_IN=tmp/branin.R
mkdir tmp
cp $SRC $TMP_IN

for t in testRunDesignParseError testRun1Design testRun2Design testRunDesignFailed testBraninGradientDescent testBraninGradientDescentx2; do
    res=`$t`
    if [ $? = "1" ]; then failed="1"; fi
    echo "Test "$t": "$res
done

exit $failed