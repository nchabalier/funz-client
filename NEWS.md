## Improvements

* package all Funz dist in R and Pypi packages (https://github.com/Funz/Funz.R, https://github.com/Funz/Funz.py)
* .../output dir is now incremented as output.i if already present
* suffix for analysis or temporary values in design are no longer '.n', but '[n]'
* design results now instanciated as text files in repository directory
* add out_filter/out.filter in Funz.py/Funz.R, to reduce memory overload
* refactor "output filter" to only select relevant info by default, regexp supported after strict matching
* support java.control/java_control env var, used to setup java init in Funz.R/Funz.py packages
* keep only usefull vars as results: only outputExpression for Run and only mainOutputExpression (by default first outputexpression) for RunDesign
* Update Rsession to support R-4/Rserve >= 1.7-5

## Fixes

* recursively parse java objects to implent R list in Funz.R
* fix repository path for design results
* check at least one input parameter is available for design
* versbosity>=10 now print most of stacktraces
* support for spaces inside dynamic expressions in ParseExpression: ``
* default R lib path is now setup in $APP_USER_DIR/R instead of ~/.Funz/R
  

# v1.13 - 20/08/2021

# v1.12 - 17/03/2021

# v1.11 - 02/11/2020

# v1.10 - 05/05/2020

# v1.9 - 04/09/2019
