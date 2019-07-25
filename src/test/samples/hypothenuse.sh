#!/bin/bash

SIDE1=&S1 
SIDE2=&S2 

AWKSCRIPT=' { printf( "%3.7f\n", sqrt($1*$1 + $2*$2) ) } '
#            command(s) / parameters passed to awk

sleep 1

# Now, pipe the parameters to awk.
echo -n "Hypotenuse = "
echo $SIDE1 $SIDE2 | awk "$AWKSCRIPT"

exit 0
