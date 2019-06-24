@echo off
SETLOCAL EnableExtensions
SETLOCAL EnableDelayedExpansion

REM put decimal values in fraction style : 1.2 -> 12/10
set n_mol=&{ $n *1000|#}/1000
set T_kelvin=&{ $(T~300) *1000|#}/1000
set V_m3=&{ $(V~{1,2,3}) *1000|#}/1000

call :calc_ 4 (%n_mol%^)*8314/1000*(%T_kelvin%^)/(%V_m3%^)
echo pressure=!calc_v!
goto :EOF

:calc_
set scale_=1
set calc_v=
for /l %%i in (1,1,%1) do set /a scale_*=10
set /a "calc_v=!scale_!*%2"
set /a calc_v1=!calc_v!/!scale_!
set /a calc_v2=!calc_v!-!calc_v1!*!scale_!
set calc_v=!calc_v1!.!calc_v2!
goto :EOF
