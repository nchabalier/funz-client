package org.funz.script;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.funz.conf.Configuration;
import org.funz.script.MathExpression.MathException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.math.R.RLog;
import org.math.R.RLogPrintStream;
import org.math.R.RserveDaemon;
import org.math.R.RserverConf;

/**
 *
 * @author richet
 */
public class RserveMathExpressionTest {

    static RMathExpression engine;

    final static String R_SERVER = "R://localhost:6311";

    public static void main(String args[]) throws Exception {
        new RserveDaemon(RserverConf.parse(R_SERVER), new RLog() {

            @Override
            public void log(String string, RLog.Level level) {
                System.err.println("R:[" + level + "] " + string);
            }

            @Override
            public void closeLog() {
            }
        }).start();

        Configuration.readProperties(null);
        Configuration.writeUserProperty = false;
        Configuration.setWWWConnected(true);

        Configuration.setProperty("R.server", R_SERVER);
        engine = new RMathExpression("MathExpressionTest");

        org.junit.runner.JUnitCore.main(RserveMathExpressionTest.class.getName());

        ((RMathExpression) engine).finalizeRsession();
    }

    @After
    public void tearDown() throws InterruptedException {
        System.err.println("    > "
                + engine.getLastMessage()
                + "\n    < "
                + engine.getLastResult()
                + "\n    ! "
                + engine.getLastError());
        //MathExpression.LogFrame.setVisible(false);
    }

    //@Test
    public void testError() throws Exception {
        System.err.println("+++++++++++++++++ testError");

        boolean error = false;
        try {
            engine.eval("stop('!!!')", null);
        } catch (MathException e) {
            error = true;
        }
        assert error : "Error not detected";
    }

    //@Test
    public void testSet() throws Exception {
        System.err.println("+++++++++++++++++ testSet");

        boolean ok = engine.set("a <- 1", null);
        assert ok : "Cannot set a";
        System.err.println("ls: " + Arrays.asList(engine.R.ls()));
        engine.eval("a", null);
        assert engine.eval("a", null) != null : "Cannot eval a in " + engine.listVariables(true, true);
    }

    //@Test
    public void testPrintIn() throws Exception {
        System.err.println("+++++++++++++++++ testPrintIn");

        String s = engine.eval("print('*')", null).toString();
        assert s.equals("*") : "Bad print: " + s;
    }

    //@Test
    public void testDecimal() throws Exception {
        System.err.println("+++++++++++++++++ testDecimal");

        assert engine.eval("paste(0.123)", null).equals("0.123") : "Bad decimal separator used:" + engine.eval("paste(0.123)", null);
    }

    //@Test
    public void testBrent() throws Exception {
        engine.R.source(new File("src/test/plugins/doe/Brent.R"));
        
        engine.R.voidEval("f <- function(X) { return( matrix(  X^2-0.5  ,ncol=1 )) }");

        assert engine.R.ls("f") != null : "Cannot eval f";
        engine.R.voidEval("options = list(ytarget='0',ytol='0.1',xtol='0.01',max_iterations='100')");
        assert engine.R.print("options") != null : "Cannot eval options";

        assert engine.R.voidEval("b = Brent(options)") : "Failed last voidEval: " + engine.R.notebook();
        assert engine.listVariables(true, true).contains("b") : "Cannot get b in envir";

        assert engine.R.voidEval("X0 = getInitialDesign(b,input=list(x=list(min=0,max=1)),NULL)") : "Failed last voidEval: " + engine.R.notebook();
        assert engine.R.voidEval("print(X0)") : "Failed last voidEval: " + engine.R.notebook();
        assert engine.R.voidEval("Y0 = f(X0); Xi = X0; Yi = Y0") : "Failed last voidEval: " + engine.R.notebook();
        assert engine.R.voidEval("print(Y0)") : "Failed last voidEval: " + engine.R.notebook();
        assert engine.listVariables(true, true).contains("Yi") : "Cannot get Yi in envir: " + engine.listVariables(true, true);

        assert engine.R.voidEval("finished = FALSE");
        assert engine.R.voidEval("while (!finished) {\n"
                +"print(Xi)\n"
                +"print(Yi)\n"
                + "     Xj = getNextDesign(b,Xi,Yi)\n"
                + "     if (is.null(Xj) || (length(Xj) == 0)) {\n"
                + "         finished = TRUE\n"
                + "     } else {\n"
                + "         Yj = f(Xj)\n"
                + "         Xi = rbind(Xi,Xj)\n"
                + "         Yi = rbind(Yi,Yj)\n"
                + "     }\n"
                + "}\n") : "Failed last voidEval: " + engine.R.notebook();

        System.err.println(engine.R.print("Xj"));
        System.err.println(engine.R.print("Yj"));
        System.err.println(engine.R.print("displayResults(b,Xi,Yi)"));
        assert engine.R.asDouble(engine.R.eval("min(abs(Yi))")) < 0.1 : "Failed to get root value of f: " + engine.R.eval("min(abs(Yi))");
    }
    
    //@Test
    public void testGradientDescent() throws Exception {
        System.err.println("+++++++++++++++++ testGradientDescent");

        engine.R.source(new File("src/test/plugins/doe/GradientDescent.R"));

        engine.R.voidEval("f <- function(X) matrix(apply(X,1,function (x) {\n"
                + "     x1 <- x[1] * 15 - 5\n"
                + "     x2 <- x[2] * 15\n"
                + "     (x2 - 5/(4 * pi^2) * (x1^2) + 5/pi * x1 - 6)^2 + 10 * (1 - 1/(8 * pi)) * cos(x1) + 10\n"
                + " }),ncol=1)");

        assert engine.R.ls("f") != null : "Cannot eval f";
        engine.R.voidEval("options = list(minimization='true', nmax = 10, delta = 1, epsilon = 0.01, target=0)");
        assert engine.R.print("options") != null : "Cannot eval options";

        assert engine.R.voidEval("gd = GradientDescent(options)") : "Failed last voidEval: " + engine.R.notebook();
        assert engine.listVariables(true, true).contains("gd") : "Cannot get gd in envir";

        assert engine.R.voidEval("X0 = getInitialDesign(gd,input=list(x1=list(min=0,max=1),x2=list(min=0,max=1)),NULL)") : "Failed last voidEval: " + engine.R.notebook();
        assert engine.R.voidEval("Y0 = f(X0); Xi = X0; Yi = Y0") : "Failed last voidEval: " + engine.R.notebook();
        assert engine.listVariables(true, true).contains("Yi") : "Cannot get Yi in envir: " + engine.listVariables(true, true);

        assert engine.R.voidEval("finished = FALSE");
        assert engine.R.voidEval("while (!finished) {\n"
                + "     Xj = getNextDesign(gd,Xi,Yi)\n"
                + "     if (is.null(Xj) || (length(Xj) == 0)) {\n"
                + "         finished = TRUE\n"
                + "     } else {\n"
                + "         Yj = f(Xj)\n"
                + "         Xi = rbind(Xi,Xj)\n"
                + "         Yi = rbind(Yi,Yj)\n"
                + "     }\n"
                + "}\n") : "Failed last voidEval: " + engine.R.notebook();

        System.err.println(engine.R.print("Xj"));
        System.err.println(engine.R.print("Yj"));
        System.err.println(engine.R.print("displayResults(gd,Xi,Yi)"));
        assert engine.R.asDouble(engine.R.eval("min(Yi)")) < 10.0 : "Failed to get minimum value of f: " + engine.R.eval("min(Yi)");
    }

    @Test
    public void testRandomUnif() throws Exception {
        System.err.println("+++++++++++++++++ testRandomUnif");

        engine.R.source(new File("src/test/plugins/doe/RandomUnif.R"));

        engine.R.voidEval("f = function(X) matrix(apply(X,1,function (x) {\n"
                + "     x1 <- x[1] * 15 - 5\n"
                + "     x2 <- x[2] * 15\n"
                + "     (x2 - 5/(4 * pi^2) * (x1^2) + 5/pi * x1 - 6)^2 + 10 * (1 - 1/(8 * pi)) * cos(x1) + 10\n"
                + " }),ncol=1)");

        assert engine.R.ls("f") != null : "Cannot eval f";
        engine.R.voidEval("options = list(sample_size=100)");
        assert engine.R.print("options") != null : "Cannot eval options";

        assert engine.R.voidEval("unif <- RandomUnif(options)") : "Failed last voidEval: " + engine.R.notebook();
        assert engine.listVariables(true, true).contains("unif") : "Cannot get unif in envir";

        assert engine.R.voidEval("X0 = getInitialDesign(unif,input=list(x1=list(min=0,max=1),x2=list(min=0,max=1)),'y')") : "Failed last voidEval: " + engine.R.notebook();
        assert engine.R.voidEval("Y0 = f(X0); Xi = X0; Yi = Y0") : "Failed last voidEval: " + engine.R.notebook();
        assert engine.listVariables(true, true).contains("Yi") : "Cannot get Yi in envir: " + engine.listVariables(true, true);

        System.err.println(engine.R.print("Xi"));
        System.err.println(engine.R.print("Yi"));

        assert engine.R.voidEval("finished = FALSE");
        assert engine.R.voidEval("while (!finished) {\n"
                + "     Xj = getNextDesign(unif,Xi,Yi)\n"
                + "     if (is.null(Xj) || (length(Xj) == 0)) {\n"
                + "         finished = TRUE\n"
                + "     } else {\n"
                + "         Yj = f(Xj)\n"
                + "         Xi = rbind(Xi,Xj)\n"
                + "         Yi = rbind(Yi,Yj)\n"
                + "     }\n"
                + "}\n") : "Failed last voidEval: " + engine.R.notebook();

        System.err.println(engine.R.print("Xj"));
        System.err.println(engine.R.print("Yj"));
        System.err.println(engine.R.print("displayResults(unif,Xi,Yi)"));
        //assert engine.R.asDouble(engine.R.eval("min(Yi)")) < 10.0 : "Failed to get minimum value of f: " + engine.R.eval("min(Yi)");
    }

    ////@Test
    public void testConcurrentEval() throws Exception {
        System.err.println("+++++++++++++++++ testConcurrentEval");

        engine.set("f <- function(x){Sys.sleep(rpois(1,4));return(x)}");
        int n = 10;
        final boolean[] test = new boolean[n];
        final boolean[] done = new boolean[n];
        for (int i = 0; i < n; i++) {
            done[i] = false;
        }

        Thread[] t = new Thread[n];
        for (int i = 0; i < n; i++) {
            final int I = i;
            t[i] = new Thread(new Runnable() {

                public void run() {
                    double x = Math.random();
                    try {
                        System.err.println("x= " + x);
                        Map<String, Object> map = new HashMap<String, Object>();
                        map.put("x", x);
                        double fx = (Double) engine.eval("f(x)", map);
                        System.err.println(fx + " =?= " + x);
                        boolean ok = Math.abs(fx - x) < 0.00001;
                        synchronized (test) {
                            test[I] = ok;
                        }
                    } catch (MathException ex) {
                        synchronized (test) {
                            test[I] = false;
                        }
                    }
                    synchronized (done) {
                        done[I] = true;
                    }
                }
            });
            t[i].start();
        }
        while (!alltrue(done)) {
            Thread.sleep(100);
            System.err.print("R");
        }

        for (int i = 0; i < n; i++) {
            t[i].join();
        }

        assert alltrue(test) : "One concurrent eval failed !";
    }

    static boolean alltrue(boolean[] a) {
        for (int i = 0; i < a.length; i++) {
            if (!a[i]) {
                return false;
            }
        }
        return true;
    }

    //@Test
    public void testSimpleEval() throws Exception {
        System.err.println("+++++++++++++++++ testSimpleEval");

        // MathExpression.SetDefaultInstance(RMathExpression.class);
        assert Math.abs((Double) engine.eval("1+pi", null) - Math.PI - 1) < 0.00001 : "bad evaluation of 1+pi";
        assert (Double) engine.eval("sum(runif(10))", null) < 10 : "bad evaluation of sum(runif(10))";
    }

    //@Test
    public void testPrintEval() throws Exception {
        System.err.println("+++++++++++++++++ testPrintEval");

        // reseting the MathExpression.defaultInstance will disconnect local Rserver !
        //MathExpression.SetDefaultInstance(RMathExpression.class);
        assert engine.eval("if (1<2) print(\"ok\") else print(\"no!!!\")", null).toString().equals("ok") : engine.eval("if (1<2) print(\"ok\") else print(\"no!!!\")", null);
    }

    //@Test
    public void testls() throws Exception {
        System.err.println("+++++++++++++++++ testls");

        engine.reset();
        engine.set("a <- 1+pi");
        String list = (engine.listVariables(true, true)).toString();
        assert list.equals("[a]") : "failed to listVariables: " + list;
    }

    //@Test
    public void testSplitEval() throws Exception {
        System.err.println("+++++++++++++++++ testSplitEval");

        engine.set("a <- 1+pi");
        assert Math.abs((Double) engine.eval("a", null) - Math.PI - 1) < 0.00001 : "bad evaluation of 1+pi";

        engine.set("f <- function(x){b <- 1+pi \n d <- 1 \n return(d)}");
        assert Arrays.asList(engine.R.ls()).contains("f") : "f not found";

        engine.set("b <- 1+pi ; d <- 1");
        assert Arrays.asList(engine.R.ls()).contains("b") : "b not found";
        assert Arrays.asList(engine.R.ls()).contains("d") : "d not found";

        engine.set("ff <- function(){b=1+pi ; 1}");
        assert Arrays.asList(engine.R.ls()).contains("ff") : "ff not found";
    }

    //@Test
    public void testSimpleFunction() throws MathException {
        System.err.println("+++++++++++++++++ testSimpleFunction");

        engine.set("f <- function(x){-x}");
        assert (Double) engine.eval("f(0.12313265465)", null) == -0.12313265465 : "bad evaluation of f";
    }
}
