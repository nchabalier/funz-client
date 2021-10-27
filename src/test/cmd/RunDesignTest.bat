@echo on
setlocal enableDelayedExpansion
REM @Before Run funz daemon to launch 'runshell' calculations

set failed=0

set FUNZ_HOME="dist"

for /f "delims=" %%p in (src\test\RunDesignTest.prop) do set %%p

set TMP_IN=tmp\branin.R
mkdir tmp
copy %SRC:/=\% %TMP_IN%

for %%t in (testRunDesignParseError testRun1Design testRun2Design testRunDesignFailed testBraninGradientDescent testBraninGradientDescentx2) do (
    @echo off
    call :%%t > res.txt
    @echo on
    set e=!errorlevel!
    (
        set "res1="
        set /p "res1="
        set "res2="
        set /p "res2="
    )<res.txt
    del res.txt
    if not "!e!"=="0" (
        set /a failed+=1
    )
    echo Test %%t: !res1!!res2!
)

echo failed: %failed%
exit %failed%

:testRunDesignParseError
    del /q testRunDesignParseError.out
    call %FUNZ_HOME%\Funz.bat Run abcdef > testRunDesignParseError.out 2>&1
    set ok=0
    for /f %%i in ('%windir%\System32\find /C "Unknown option: abcdef" ^< "testRunDesignParseError.out"') do set ok=%%i
    if %ok% gtr 0 (
        del testRunDesignParseError.out
        echo OK
        exit /b 0
    ) else (
        echo FAILED:
        more testRunDesignParseError.out
        exit /b 1
    )

:testRun1Design
    del /q testRun1Design.out
    call %FUNZ_HOME%\Funz.bat RunDesign -m %CODE% -d %DESIGN% -do nmax=%NMAX% delta=%DELTA% -if %TMP_IN% -iv x1=.3 x2=[0.3,.4] -v %VERBOSITY% -ad tmp/testRun1Design.bat > testRun1Design.out 2>&1
    set ok=0
    for /f %%i in ('%windir%\System32\find /C "done" ^< "testRun1Design.out"') do set ok=%%i
    if %ok% gtr 0 (
        del testRun1Design.out
        echo OK
        exit /b 0
    ) else (
        echo FAILED:
        more testRun1Design.out
        exit /b 1
    )

:testRun2Design
    del /q testRun2Design.out
    call %FUNZ_HOME%\Funz.bat RunDesign -m %CODE% -d %DESIGN% -do nmax=%NMAX% delta=%DELTA% -if %TMP_IN% -iv x1=.3,.5 x2=[0.3,.4] -v %VERBOSITY% -ad tmp/testRun2Design.bat > testRun2Design.out 2>&1
    set ok=0
    for /f %%i in ('%windir%\System32\find /C "done" ^< "testRun2Design.out"') do set ok=%%i
    if %ok% gtr 0 (
        del testRun2Design.out
        echo OK
        exit /b 0
    ) else (
        echo FAILED:
        more testRun2Design.out
        exit /b 1
    )

:testRunDesignFailed
    del /q testRunDesignFailed.out
    call %FUNZ_HOME%\Funz.bat RunDesign -m %CODE% -d %DESIGN% -do nmax=%NMAX% delta=%DELTA% -if %TMP_IN% -iv x1=[-1.1,1] x2=[0.3,.4] -v %VERBOSITY% -ad tmp/testRunDesignFailed.bat -oe x1+min\(cat,10,na.rm=FALSE\) > testRunDesignFailed.out 2>&1
    set ok=0
    for /f %%i in ('%windir%\System32\find /C "RunDesign failed" ^< "testRunDesignFailed.out"') do set ok=%%i
    if %ok% gtr 0 (
        del testRunDesignFailed.out
        echo OK
        exit /b 0
    ) else (
        echo FAILED:
        more testRunDesignFailed.out
        exit /b 1
    )

:testBraninGradientDescent
    del /q testBraninGradientDescent.out
    call %FUNZ_HOME%\Funz.bat RunDesign -m %CODE% -d %DESIGN% -do nmax=%NMAX% delta=%DELTA% -if %TMP_IN% -iv x1=[0,1] x2=[0,1] -v %VERBOSITY% -ad tmp/testBraninGradientDescent.bat > testBraninGradientDescent.out 2>&1
    set ok=0
    for /f %%i in ('%windir%\System32\find /C "%BraninGradientDescent_MIN%" ^< "testBraninGradientDescent.out"') do set ok=%%i
    if %ok% gtr 0 (
        del testBraninGradientDescent.out
        echo OK
        exit /b 0
    ) else (
        echo FAILED:
        more testBraninGradientDescent.out
        exit /b 1
    )

:testBraninGradientDescentx2
    del /q testBraninGradientDescentx2.out
    call %FUNZ_HOME%\Funz.bat RunDesign -m %CODE% -d %DESIGN% -do nmax=%NMAX% delta=%DELTA% -if %TMP_IN% -iv x1=0,1 x2=[0,1] -v %VERBOSITY% -ad tmp/testBraninGradientDescentx2.bat > testBraninGradientDescentx2.out 2>&1
    set ok1=0
    for /f %%i in ('%windir%\System32\find /C "%BraninGradientDescentx2_x1_0_MIN%" ^< "testBraninGradientDescentx2.out"') do set ok1=%%i   
    set ok2=0
    for /f %%j in ('%windir%\System32\find /C "%BraninGradientDescentx2_x1_1_MIN%" ^< "testBraninGradientDescentx2.out"') do set ok2=%%j
    if %ok1% gtr 0 ( if %ok2% gtr 0 (
        del testBraninGradientDescentx2.out
        echo OK
        exit /b 0
    ) else (
        echo FAILED:
        more testBraninGradientDescentx2.out
        exit /b 1
    ))
