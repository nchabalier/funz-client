@echo on

java -version

setlocal enableDelayedExpansion
REM @Before Run funz daemon to launch 'runshell' calculations

set failed=0

set FUNZ_HOME="dist"

for /f "delims=" %%p in (src\test\RunTest.prop) do set %%p

set TMP_IN=tmp\branin.R
mkdir tmp
copy %SRC:/=\% %TMP_IN%

for %%t in (testRunParseError testRun1 testOutputExpression testRun9) do (
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

:testRunParseError
    del /q testRunParseError.out
    call %FUNZ_HOME%\Funz.bat Run abcdef > testRunParseError.out 2>&1
    set ok=0
    for /f %%i in ('%windir%\System32\find /C "Unknown option: abcdef" ^< "testRunParseError.out"') do set ok=%%i
    if "%ok%"=="1" (
        del testRunParseError.out
        echo OK
        exit /b 0
    ) else (
        echo FAILED:
        more testRunParseError.out
        exit /b 1
    )

:testRun1
    del /q testRun1.out
    call %FUNZ_HOME%\Funz.bat Run -m %CODE% -if %TMP_IN% -iv x1=.5 x2=0.3 -v %VERBOSITY% -ad tmp > testRun1.out 2>&1
    set ok=0
    for /f %%i in ('%windir%\System32\find /C "done" ^< "testRun1.out"') do set ok=%%i
    if "%ok%"=="2" (
        del testRun1.out
        echo OK
        exit /b 0
    ) else (
        echo FAILED:
        more testRun1.out
        exit /b 1
    )

:testOutputExpression
    del /q testOutputExpression.out
    call %FUNZ_HOME%\Funz.bat Run -m %CODE% -if %TMP_IN% -iv x1=.5 x2=0.3 -v %VERBOSITY% -ad tmp -oe 1+cat > testOutputExpression.out 2>&1
    set ok=0
    for /f %%i in ('%windir%\System32\find /C "6.154316" ^< "testOutputExpression.out"') do set ok=%%i
    if "%ok%"=="1" (
        del testOutputExpression.out
        echo OK
        exit /b 0
    ) else (
        echo FAILED:
        more testOutputExpression.out
        exit /b 1
    )

:testRun9
    del /q testRun9.out
    call %FUNZ_HOME%\Funz.bat Run -m %CODE% -if %TMP_IN% -all -iv x1=.5,.6,.7 x2=0.3,.4,.5 -v %VERBOSITY% -ad tmp > testRun9.out 2>&1
    set ok=0
    for /f %%i in ('%windir%\System32\find /C "done" ^< "testRun9.out"') do set ok=%%i
    if "%ok%"=="10" (
        del testRun9.out
        echo OK
        exit /b 0
    ) else (
        echo FAILED:
        more testRun9.out
        exit /b 1
    )