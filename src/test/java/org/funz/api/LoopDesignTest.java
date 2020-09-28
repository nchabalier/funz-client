package org.funz.api;

import java.util.Map;
import org.funz.Project;
import org.funz.doeplugin.Design;
import org.funz.ioplugin.ExtendedIOPlugin;
import org.funz.parameter.Case;
import org.funz.parameter.OutputFunctionExpression;
import org.funz.parameter.Variable;
import org.funz.script.RMathExpression;
import org.junit.Before;
import org.junit.Test;
import org.math.R.RLog;

/**
 *
 * @author richet
 */
public class LoopDesignTest extends org.funz.api.TestUtils {

    String ALGO = "GradientDescent";
    String OLDALGO = "oldgradientdescent";

    @Before
    public void setUp() throws Exception {
        Funz_v1.init();
        //Funz_v1.POOL.stop();
        ((RMathExpression) (RMathExpression.GetDefaultInstance())).R.addLogger(new RLog() {

            @Override
            public void log(String string, RLog.Level level) {
                System.err.println("R: " + level + " : " + string);
            }

            @Override
            public void closeLog() {
            }
        });
        //((R2jsSession)(((RMathExpression) (RMathExpression.GetDefaultInstance())).R)).debug_js = true;
    }

    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main(LoopDesignTest.class.getName());
    }

    Case.Observer o = new Case.Observer() {

        @Override
        public void caseModified(int index, int what) {
            System.err.println("caseModified");
        }
    };
    Design.Observer oo = new Design.Observer() {

        @Override
        public void designUpdated(int i) {
            System.err.println("designUpdated " + i);
        }
    };

    @Test
    public void testGradientDescent() throws Exception {
        System.err.println("++++++++++++++++++++++++++ testGradientDescent");
        Project prj = new Project("testGradientDescent");
        prj.setPlugin(new ExtendedIOPlugin());
        prj.setMainOutputFunction(new OutputFunctionExpression.Numeric(mult.fname));
        prj.setDesignerId(ALGO);

        Variable x1 = new Variable(prj, "x1");
        prj.getVariables().add(x1);
        x1.setType(Variable.TYPE_CONTINUOUS);
        x1.setLowerBound(mult_x1_min);
        x1.setUpperBound(mult_x1_max);

        Variable x2 = new Variable(prj, "x2");
        prj.getVariables().add(x2);
        x2.setType(Variable.TYPE_CONTINUOUS);
        x2.setLowerBound(mult_x2_min);
        x2.setUpperBound(mult_x2_max);

        prj.buildParameterList();
        prj.resetDiscreteCases(o);

        LoopDesign_v1 loopDesign = new LoopDesign_v1(oo, o, prj, newTmpDir("testGradientDescent")) {

            @Override
            public void out(String string, int i) {
                TestUtils.out(string, i);
            }

            @Override
            public void err(String msg, int i) {
                TestUtils.err(msg, i);
            }

            @Override
            public void err(Exception ex, int i) {
                TestUtils.err(ex, i);
            }
        };

        loopDesign.setDesignerOption("nmax", "3");
        loopDesign.update();
        loopDesign.buildDesign(prj.getDesignSession(0));
        //((R2jsSession)(((RDesign)(loopDesign.design)).R)).debug_js = true;
        prj.setDesign(loopDesign.design, 0);
        prj.addDesignCases(loopDesign.initialExperiments, o, 0);

        Map<String, Object[]> X = loopDesign.initDesign();
        Map Y = mult.F(X);

        while ((X = loopDesign.nextDesign(Y)) != null) {
            prj.addDesignCases(loopDesign.nextExperiments, o, 0);
            Y = mult.F(X);
            System.err.println(loopDesign.getResultsTmp());
        }
        System.err.println(loopDesign.getResults().keySet());
        System.err.println(loopDesign.getResults().get("analysis"));

        assert loopDesign.getResults().getOrDefault("analysis.min", "").trim().equals("" + mult_min) : "Failed to find minimum ! \n" + loopDesign.getResults().get("analysis.min");
    }

    @Test
    public void testOldGradientDescent() throws Exception {
        if (RMathExpression.GetEngineName().contains("R2js")) {
            System.err.println("Using R2js, so skipping test");
            return;
        } // Do not run if using R2js...

        System.err.println("++++++++++++++++++++++++++ testOldGradientDescent");
        Project prj = new Project("testOldGradientDescent");
        prj.setPlugin(new ExtendedIOPlugin());
        prj.setMainOutputFunction(new OutputFunctionExpression.Numeric(mult.fname));
        prj.setDesignerId(OLDALGO);

        Variable x1 = new Variable(prj, "x1");
        prj.getVariables().add(x1);
        x1.setType(Variable.TYPE_CONTINUOUS);
        x1.setLowerBound(mult_x1_min);
        x1.setUpperBound(mult_x1_max);

        Variable x2 = new Variable(prj, "x2");
        prj.getVariables().add(x2);
        x2.setType(Variable.TYPE_CONTINUOUS);
        x2.setLowerBound(mult_x2_min);
        x2.setUpperBound(mult_x2_max);

        prj.buildParameterList();
        prj.resetDiscreteCases(o);

        LoopDesign_v1 loopDesign = new LoopDesign_v1(oo, o, prj, newTmpDir("testOldGradientDescent")) {

            @Override
            public void out(String string, int i) {
                TestUtils.out(string, i);
            }

            @Override
            public void err(String msg, int i) {
                TestUtils.err(msg, i);
            }

            @Override
            public void err(Exception ex, int i) {
                TestUtils.err(ex, i);
            }
        };

        loopDesign.setDesignerOption("nmax", "10");
        loopDesign.update();
        loopDesign.buildDesign(prj.getDesignSession(0));
        //((RSession) (((OldRDesign) (loopDesign.design)).R)).debug = true;
        prj.setDesign(loopDesign.design, 0);
        prj.addDesignCases(loopDesign.initialExperiments, o, 0);

        Map<String, Object[]> X = loopDesign.initDesign();
        Map Y = mult.F(X);

        while ((X = loopDesign.nextDesign(Y)) != null) {
            prj.addDesignCases(loopDesign.nextExperiments, o, 0);
            Y = mult.F(X);
            System.err.println(loopDesign.getResultsTmp());
        }
        System.err.println(loopDesign.getResults().keySet());
        System.err.println(loopDesign.getResults().getOrDefault("analysis.min", ""));
        assert loopDesign.getResults().getOrDefault("analysis.min", "").trim().equals("" + mult_min) : "Failed to find minimum ! \n" + loopDesign.getResults().get("analysis.min");
    }

    @Test
    public void testCalcError() throws Exception {
        System.err.println("++++++++++++++++++++++++++ testCalcError");
        Project prj = new Project("testCalcError");
        prj.setPlugin(new ExtendedIOPlugin());
        prj.setMainOutputFunction(new OutputFunctionExpression.Numeric(mult.fname));

        prj.setDesignerId(ALGO);

        Variable x1 = new Variable(prj, "x1");
        prj.getVariables().add(x1);
        x1.setType(Variable.TYPE_CONTINUOUS);
        x1.setLowerBound(mult_x1_min);
        x1.setUpperBound(mult_x1_max);

        Variable x2 = new Variable(prj, "x2");
        prj.getVariables().add(x2);
        x2.setType(Variable.TYPE_CONTINUOUS);
        x2.setLowerBound(mult_x2_min);
        x2.setUpperBound(mult_x2_max);

        prj.buildParameterList();
        prj.resetDiscreteCases(o);

        LoopDesign_v1 loopDesign = new LoopDesign_v1(oo, o, prj, newTmpDir("testCalcError")) {

            @Override
            public void out(String string, int i) {
                TestUtils.out(string, i);
            }

            @Override
            public void err(String msg, int i) {
                TestUtils.err(msg, i);
            }

            @Override
            public void err(Exception ex, int i) {
                TestUtils.err(ex, i);
            }
        };

        loopDesign.setDesignerOption("nmax", "10");
        loopDesign.setDesignerOption("target", "-10");
        loopDesign.update();
        loopDesign.buildDesign(prj.getDesignSession(0));

        prj.setDesign(loopDesign.design, 0);
        prj.addDesignCases(loopDesign.initialExperiments, o, 0);

        try {
            Map<String, Object[]> X = loopDesign.initDesign();
            Map<String, Object[]> Y = mult.F(X);
            int i = 0;
            while ((X = loopDesign.nextDesign(Y)) != null) {
                prj.addDesignCases(loopDesign.nextExperiments, o, 0);
                Y = mult.F(X);
                if (i++ > 2) {
                    Object[] y = Y.get(mult.fname);
                    y[y.length - 1] = Double.NaN;
                    Y.put(mult.fname, y);
                }
                System.err.println(loopDesign.getResultsTmp());
            }
        } catch (Exception e) {
            assert e.getMessage().contains("NaN") : "Did not correclty failed " + e.getMessage();
            return;
        }
        assert false : "Did not detected call failure: " + loopDesign.getResults();

        System.err.println(loopDesign.getResults());
    }

    @Test
    public void testAlgError() throws Exception {
        System.err.println("++++++++++++++++++++++++++ testAlgError");
        Project prj = new Project("testAlgError");
        prj.setPlugin(new ExtendedIOPlugin());
        prj.setMainOutputFunction(new OutputFunctionExpression.Numeric(mult.fname));

        prj.setDesignerId(ALGO);

        Variable x1 = new Variable(prj, "x1");
        prj.getVariables().add(x1);
        x1.setType(Variable.TYPE_CONTINUOUS);
        x1.setLowerBound(mult_x1_min);
        x1.setUpperBound(mult_x1_max);

        Variable x2 = new Variable(prj, "x2");
        prj.getVariables().add(x2);
        x2.setType(Variable.TYPE_CONTINUOUS);
        x2.setLowerBound(mult_x2_min);
        x2.setUpperBound(mult_x2_max);

        prj.buildParameterList();
        prj.resetDiscreteCases(o);

        LoopDesign_v1 loopDesign = new LoopDesign_v1(oo, o, prj, newTmpDir("testAlgError")) {

            @Override
            public void out(String string, int i) {
                TestUtils.out(string, i);
            }

            @Override
            public void err(String msg, int i) {
                TestUtils.err(msg, i);
            }

            @Override
            public void err(Exception ex, int i) {
                TestUtils.err(ex, i);
            }
        };

        loopDesign.setDesignerOption("nmax", "3");
        loopDesign.setDesignerOption("target", "-10");
        loopDesign.update();
        loopDesign.buildDesign(prj.getDesignSession(0));

        prj.setDesign(loopDesign.design, 0);
        prj.addDesignCases(loopDesign.initialExperiments, o, 0);

        try {
            Map<String, Object[]> X = loopDesign.initDesign();
            Map Y = mult.F(X);
            int i = 0;
            while ((X = loopDesign.nextDesign(Y)) != null) {
                if (i++ > 2) {
                    throw new Exception("Exception in nextDesign: force failed.");
                }
                prj.addDesignCases(loopDesign.nextExperiments, o, 0);
                Y = mult.F(X);
                System.err.println(loopDesign.getResultsTmp());
            }
        } catch (Exception e) {
            assert e.getMessage().contains("Exception in nextDesign: force failed.") : "Did not correclty failed " + e.getMessage();
            return;
        }
        assert false : "Did not detected call failure";

        System.err.println(loopDesign.getResults());
    }
}
