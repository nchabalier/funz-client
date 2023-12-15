# v1.16 - ??/??/2023

## Fixes

* support missing output/input in Python wrapper
* update Rsession to 3.1.8 to support R>=4.3

# v1.15 - 23/11/2022

## Improvements

* support .csv output file reading as DataFrame/List/Dict/Map (using CSV('coldelim') in outputexpression)
* support DataFrame/List/Dict/Map object in output expression interpreter
* remove 'sequence' type in outputexpressions. Should be replaced by an array.
* check input variables are well defined in Funz.sh/bat, otherwise warn that default model is used
* add sensitivity analysis Morris algorithm: https://github.com/Funz/algorithm-Sensitivity
* add simple uniform sampling (no need for more R package): https://github.com/Funz/algorithm-RandomSampling
* up Rsession to support "list[['argname']]" syntax in R2js
* define default columns to diplay in plugin
* define output number format in plugin

## Fixes

* add tests for multi-output algorithms
* try force cast string[] output to double[], even when ioplugin returns string
* fix escaping char in outputexpression ('\"' was considered as 2 chars)
* change order of cases status : intact/preparing/running/error/failed/done
* remove (heavy) display of all results when design error
* remove some varargs that should block gc... may reduce heap usage
* fix some failure reporting 


# v1.14 - 04/12/2021

## Improvements

* .../output dir is now incremented as output.i if already exists
* suffix for analysis or temporary values in design are no longer '.n', but '[n]'
* design results now instanciated as text files in repository directory: min.txt, argmin.txt, ...
* add arguments out_filter/out.filter in Funz.py/Funz.R, to reduce memory overload
* support java.control/java_control env var, used to setup java init in Funz.R/Funz.py packages
* keep only usefull vars as results: only outputExpression for Run and only mainOutputExpression (by default first outputexpression) for RunDesign
* Update Rsession (to 3.1.5) to support R-4/Rserve >= 1.7-5 & 1.8-9 (legacy CRAN Rserve)
* R package available on CRAN: `install.packages('Funz')`
* sources (.java files) are now shipped alongside classes inside funz-*.jar files.

## Fixes

* refactor "output filter" to only select relevant info by default, both strict matching and (then) regexp supported
* recursively parse java objects to implement R list in Funz.R
* fix repository path for design results
* check at least one input parameter is available for design
* versbosity>=10 now print most of stacktraces
* support for spaces inside dynamic expressions in ParseExpression: ``
* remove jmatphlot dependency
* Rserve process should not persist more than its parent Funz call


# v1.13 - 20/08/2021

## Improvements

* package all Funz dist in R and Pypi packages (https://github.com/Funz/Funz.R, https://github.com/Funz/Funz.py)
* Python package available on Pypi: `pip install Funz`
* return also available codes in Funz_GridStatus() call for Funz.R & Funz.py
* default R lib path is now setup in $APP_USER_DIR/R instead of ~/.Funz/R
* force flush console in Funz.R, to better sync within RStudio/Jupyter front-end
* add history.txt in case directory (to log all com. with calculator)
* add path.txt file in case directory
* also add "path" output for each case
* implement Restart/Stop action for running cases (now available in Promethee)
* sort cases by their index
* support "R.repos" key in Funz.conf to setup CRAN repository(still http://cloud.r-project.org by default)

## Fixes

* fix missing output_expression in Funz.py RunDesign()
* fix Funz.R Funz.init() java.control arg setup
* do not repeat "Funz models..." and "Funz design..." messages in Funz.R & Funz.py
* more robust tests in bash


# v1.12 - 17/03/2021

## Improvements

* move to GHA instead of Travis
* extend CI matrix & fixes in tests
* faster shutdown (if required) in batch of running cases, at startup
* up to Rsession 3.1.3
* force refreshing computers pool

## Fixes

* retreive ressources (like images) from remote R session (with Rserve)
* avoid wrashing Funz.R on windows if ctrl+C breaking
* fix tests of Windows cmd.exe Funz.bat call


# v1.11 - 02/11/2020

* Bundled in CRISTAL frontend "LATEC 1.5" (https://www.cristal-package.org/)
* Bundled in Promethee2 frontend (https://gitlab.com/irsn/promethee2)


# v1.10 - 05/05/2020

# v1.9 - 04/09/2019
