branin <- function(x) {
	x1 <- x[1]*15-5   
	x2 <- x[2]*15     
	(x2 - 5/(4*pi^2)*(x1^2) + 5/pi*x1 - 6)^2 + 10*(1 - 1/(8*pi))*cos(x1) + 10
}

t=0

Sys.sleep(t);
cat("1/3");
Sys.sleep(t);
cat("2/3");
Sys.sleep(t);
cat("3/3");
Sys.sleep(t);

#!: f <- function(x) {x}

cat("X = ",c( ?x1 , ?[x2~.5]),"\n");

if ( ?x1 >= 0 ) {
  z = branin(c( ?x1 , !{f(?x2)+0.5+1-1.5} ))
} else {
  z = '?'
}

cat('z = ',z,'\n')