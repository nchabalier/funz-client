package org.funz.api;

import java.io.File;
import java.util.Map;
import org.funz.Project;
import static org.funz.api.DesignShell_v1.DEFAULT_FUNCTION_NAME;
import org.funz.doeplugin.Design;
import org.funz.ioplugin.ExtendedIOPlugin;
import org.funz.parameter.Case;
import org.funz.parameter.OutputFunctionExpression;
import org.funz.parameter.Variable;
import org.funz.script.RMathExpression;
import static org.funz.util.Data.newMap;
import org.junit.Before;
import org.junit.Test;
import org.math.R.RLog;
import org.math.array.DoubleArray;

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
    }

    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main(LoopDesignTest.class.getName());
    }

    DesignShell_v1.Function f = new DesignShell_v1.Function(DEFAULT_FUNCTION_NAME, "x1", "x2") {
        @Override
        public Map f(Object... strings) {
            double[] vals = new double[strings.length];
            for (int i = 0; i < vals.length; i++) {
                vals[i] = Double.parseDouble(strings[i].toString());
            }
            return newMap(f.fname, DoubleArray.sum(vals));
        }
    };

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
        prj.setMainOutputFunction(new OutputFunctionExpression.Numeric(f.fname));
        prj.setDesignerId(ALGO);

        Variable x1 = new Variable(prj, "x1");
        prj.getVariables().add(x1);
        x1.setType(Variable.TYPE_CONTINUOUS);
        x1.setLowerBound(0);
        x1.setUpperBound(1);

        Variable x2 = new Variable(prj, "x2");
        prj.getVariables().add(x2);
        x2.setType(Variable.TYPE_CONTINUOUS);
        x2.setLowerBound(0);
        x2.setUpperBound(1);

        prj.buildParameterList();
        prj.resetDiscreteCases(o);

        LoopDesign_v1 loopDesign = new LoopDesign_v1(oo, o, prj, new File("tmp")) {

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

        prj.setDesign(loopDesign.design, 0);
        prj.addDesignCases(loopDesign.initialExperiments, o, 0);

        Map<String, Object[]> X = loopDesign.initDesign();
        Map Y = f.F(X);

        while ((X = loopDesign.nextDesign(Y)) != null) {
            prj.addDesignCases(loopDesign.nextExperiments, o, 0);
            Y = f.F(X);
            System.err.println(loopDesign.getResultsTmp());
        }
        System.err.println(loopDesign.getResults().keySet());
        System.err.println(loopDesign.getResults().get("analysis"));

        assert loopDesign.getResults().getOrDefault("analysis.min", "").trim().equals("0") : "Failed to find minimum ! \n" + loopDesign.getResults().keySet();
    }

    @Test
    public void testOldGradientDescent() throws Exception {
        System.err.println("++++++++++++++++++++++++++ testOldGradientDescent");
        Project prj = new Project("testOldGradientDescent");
        prj.setPlugin(new ExtendedIOPlugin());
        prj.setMainOutputFunction(new OutputFunctionExpression.Numeric(f.fname));
        prj.setDesignerId(OLDALGO);

        Variable x1 = new Variable(prj, "x1");
        prj.getVariables().add(x1);
        x1.setType(Variable.TYPE_CONTINUOUS);
        x1.setLowerBound(0);
        x1.setUpperBound(1);

        Variable x2 = new Variable(prj, "x2");
        prj.getVariables().add(x2);
        x2.setType(Variable.TYPE_CONTINUOUS);
        x2.setLowerBound(0);
        x2.setUpperBound(1);

        prj.buildParameterList();
        prj.resetDiscreteCases(o);

        LoopDesign_v1 loopDesign = new LoopDesign_v1(oo, o, prj, new File("tmp")) {

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

        prj.setDesign(loopDesign.design, 0);
        prj.addDesignCases(loopDesign.initialExperiments, o, 0);

        Map<String, Object[]> X = loopDesign.initDesign();
        Map Y = f.F(X);

        while ((X = loopDesign.nextDesign(Y)) != null) {
            prj.addDesignCases(loopDesign.nextExperiments, o, 0);
            Y = f.F(X);
            System.err.println(loopDesign.getResultsTmp());
        }
        System.err.println(loopDesign.getResults().keySet());
        System.err.println(loopDesign.getResults().getOrDefault("analysis.min", ""));
        assert loopDesign.getResults().getOrDefault("analysis.min", "").trim().equals("0") : "Failed to find minimum ! \n" + loopDesign.getResults().keySet();
    }

    @Test
    public void testCalcError() throws Exception {
        System.err.println("++++++++++++++++++++++++++ testCalcError");
        Project prj = new Project("testCalcError");
        prj.setPlugin(new ExtendedIOPlugin());
        prj.setMainOutputFunction(new OutputFunctionExpression.Numeric(f.fname));

        prj.setDesignerId(ALGO);

        Variable x1 = new Variable(prj, "x1");
        prj.getVariables().add(x1);
        x1.setType(Variable.TYPE_CONTINUOUS);
        x1.setLowerBound(0);
        x1.setUpperBound(1);

        Variable x2 = new Variable(prj, "x2");
        prj.getVariables().add(x2);
        x2.setType(Variable.TYPE_CONTINUOUS);
        x2.setLowerBound(0);
        x2.setUpperBound(1);

        prj.buildParameterList();
        prj.resetDiscreteCases(o);

        LoopDesign_v1 loopDesign = new LoopDesign_v1(oo, o, prj, new File("tmp")) {

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
            Map<String, Object[]> Y = f.F(X);
            int i = 0;
            while ((X = loopDesign.nextDesign(Y)) != null) {
                prj.addDesignCases(loopDesign.nextExperiments, o, 0);
                Y = f.F(X);
                if (i++ > 2) {
                    Object[] y = Y.get(f.fname);
                    y[y.length-1] = Double.NaN;
                    Y.put(f.fname, y);
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
        prj.setMainOutputFunction(new OutputFunctionExpression.Numeric(f.fname));

        prj.setDesignerId(ALGO);

        Variable x1 = new Variable(prj, "x1");
        prj.getVariables().add(x1);
        x1.setType(Variable.TYPE_CONTINUOUS);
        x1.setLowerBound(0);
        x1.setUpperBound(1);

        Variable x2 = new Variable(prj, "x2");
        prj.getVariables().add(x2);
        x2.setType(Variable.TYPE_CONTINUOUS);
        x2.setLowerBound(0);
        x2.setUpperBound(1);

        prj.buildParameterList();
        prj.resetDiscreteCases(o);

        LoopDesign_v1 loopDesign = new LoopDesign_v1(oo, o, prj, new File("tmp")) {

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
            Map Y = f.F(X);
            int i = 0;
            while ((X = loopDesign.nextDesign(Y)) != null) {
                if (i++ > 2) {
                    throw new Exception("Exception in nextDesign: force failed.");
                }
                prj.addDesignCases(loopDesign.nextExperiments, o, 0);
                Y = f.F(X);
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
