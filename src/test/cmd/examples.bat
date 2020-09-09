FUNZ_HOME=dist
$FUNZ_HOME/FunzDaemon.sh &

$FUNZ_HOME/Funz.sh Run -m R -if samples/branin.R

$FUNZ_HOME/Funz.sh Run -m R -if samples/branin.R -iv x1=.1,.2,.3 x2=.1,.2,.3 -v 5

$FUNZ_HOME/Funz.sh Run -m R -if samples/branin.R -iv x1=.1,.2,.3 x2=.1,.2,.3 -v 5 -rc regexpCalculators=localhost*

$FUNZ_HOME/Funz.sh Run -m darwin22 -if samples/test_apollo_darwin -v 5 -rc regexpCalculators=localhost*
