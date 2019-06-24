#!/bin/bash

n_mol=%n
T_kelvin=%(T~300)
V_m3=%(V~{1,2,3})

echo "pressure="`echo "scale=4;$n_mol*8.314*$T_kelvin/$V_m3" | bc`
