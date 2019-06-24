#!/bin/bash

n_mol=1
T_kelvin=300
V_m3=1

echo "pressure="`echo "scale=4;$n_mol*8.314*$T_kelvin/$V_m3" | bc`
