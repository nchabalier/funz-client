package org.funz.api;

import java.io.File;
import java.util.Map;
import org.funz.Project;
import org.funz.ProjectController;
import org.funz.conf.Configuration;
import static org.funz.doeplugin.DesignConstants.NODESIGNER_ID;
import org.funz.ioplugin.IOPluginInterface;
import org.funz.ioplugin.IOPluginsLoader;
import org.funz.parameter.Case;
import org.funz.parameter.Variable;
import org.funz.parameter.VariableMethods;
import org.funz.run.CalculatorsPool;
import org.funz.script.RMathExpression;
import static org.funz.util.Data.*;
import org.funz.util.Format;
import static org.funz.util.Format.ArrayMapToMDString;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author richet
 */
public class BatchNoRunTest extends org.funz.api.TestUtils {

    public static void main(String args[]) throws Exception {
        //test(BatchNoRunTest.class.getName()); <-- Do not start any calculator !!!
        Configuration.setVerboseLevel(verbose);

        Funz_v1.init(null, new CalculatorsPool(CalculatorsPool.getSocket(1025)), M);

        org.junit.runner.JUnitCore.main(BatchNoRunTest.class.getName());
    }

    @Before
    public void setUp() throws Exception {
        super.setUp(BatchNoRunTest.class.getName());
    }

    Case.Observer o = new Case.Observer() {

        @Override
        public void caseModified(int index, int what) {
        }
    };

    
    @Test
    public void test1Case() throws Exception {
        System.err.println("+++++++++++++++++++++++++ test1Case");
        File tmp_in = tmp_in();

        IOPluginInterface plugin = IOPluginsLoader.newInstance(R, tmp_in);
        Project prj = ProjectController.createProject(tmp_in.getName(), tmp_in, R, plugin);

        assert prj.getVariableByName("x1") != null : "Variable x1 not detected";
        assert prj.getVariableByName("x2") != null : "Variable x2 not detected";
        assert prj.getVariableByName("x1").getDefaultValue() == null : "Variable x1 default value not null.";
        assert prj.getVariableByName("x2").getDefaultValue().equals(".5") : "Variable x2 default value not detected.";

        plugin.setFormulaInterpreter(new RMathExpression(tmp_in.getName() + "_" + Configuration.timeDigest(), Configuration.isLog("R") ? new File(prj.getLogDir(), tmp_in.getName() + ".Rlog") : null));
        prj.setMainOutputFunction(plugin.suggestOutputFunctions().get(0));
        prj.setDesignerId(NODESIGNER_ID);

        Variable x1 = prj.getVariableByName("x1");
        x1.setType(Variable.TYPE_REAL);
        x1.setValues(VariableMethods.Value.asValueList(".1"));

        Variable x2 = prj.getVariableByName("x2");
        x2.setType(Variable.TYPE_REAL);
        x2.setValues(VariableMethods.Value.asValueList(".1"));

        prj.buildParameterList();

        prj.resetDiscreteCases(o);

        prj.setCases(prj.getDiscreteCases(), o);

        prj.useCache = false;

        final BatchRun_v1 batchRun = new BatchRun_v1(o, prj, new File("tmp")) {

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

        Thread t = new Thread(new Runnable() {

            public void run() {
                try {
                    assert !batchRun.runBatch() : "Failed to stop batch";

                    Map<String, String[]> results = batchRun.getResultsStringArrayMap();
                    assert ArrayMapToMDString(results).length() > 0 : "Empty results";
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        t.start();

        Thread.sleep(5000);

        System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!! STOP !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        assert batchRun.stopBatch() : "Could not stop batch";

        t.join();

        System.err.println(Format.ArrayMapToMDString(batchRun.getResultsArrayMap()));

        batchRun.shutdown();
    }

    @Test
    public void test1FailedCaseOnPrepare() throws Exception {
        System.err.println("+++++++++++++++++++++++++ test1FailedCaseOnPrepare");
        File tmp_in = tmp_in();

        IOPluginInterface plugin = IOPluginsLoader.newInstance(R, tmp_in);
        Project prj = ProjectController.createProject(tmp_in.getName(), tmp_in, R, plugin);

        assert prj.getVariableByName("x1") != null : "Variable x1 not detected";
        assert prj.getVariableByName("x2") != null : "Variable x2 not detected";
        assert prj.getVariableByName("x1").getDefaultValue() == null : "Variable x1 default value not null.";
        assert prj.getVariableByName("x2").getDefaultValue().equals(".5") : "Variable x2 default value not detected.";

        plugin.setFormulaInterpreter(new RMathExpression(tmp_in.getName() + "_" + Configuration.timeDigest(), Configuration.isLog("R") ? new File(prj.getLogDir(), tmp_in.getName() + ".Rlog") : null));
        prj.setMainOutputFunction(plugin.suggestOutputFunctions().get(0));
        prj.setDesignerId(NODESIGNER_ID);

        Variable x1 = prj.getVariableByName("x1");
        x1.setType(Variable.TYPE_STRING);
        x1.setValues(VariableMethods.Value.asValueList("abc"));

        Variable x2 = prj.getVariableByName("x2");
        x2.setType(Variable.TYPE_STRING);
        x2.setValues(VariableMethods.Value.asValueList("cde"));

        prj.buildParameterList();

        prj.resetDiscreteCases(o);

        prj.setCases(prj.getDiscreteCases(), o);

        final BatchRun_v1 batchRun = new BatchRun_v1(o, prj, new File("tmp")) {

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

        assert batchRun.runBatch() : "Failed to terminate batch";

        Map<String, String[]> results = batchRun.getResultsStringArrayMap();
        assert ArrayMapToMDString(results).length() > 0 : "Empty results";

        System.err.println(Format.ArrayMapToMDString(batchRun.getResultsArrayMap()));

        batchRun.shutdown();
    }

    @Test
    public void testKill() throws Exception {
        System.err.println("+++++++++++++++++++++++++ testKill");
        File tmp_in = tmp_in();

        IOPluginInterface plugin = IOPluginsLoader.newInstance(R, tmp_in);
        Project prj = ProjectController.createProject(tmp_in.getName(), tmp_in, R, plugin);

        plugin.setFormulaInterpreter(new RMathExpression(tmp_in.getName() + "_" + Configuration.timeDigest(), Configuration.isLog("R") ? new File(prj.getLogDir(), tmp_in.getName() + ".Rlog") : null));
        prj.setMainOutputFunction(plugin.suggestOutputFunctions().get(0));
        prj.setDesignerId(NODESIGNER_ID);

        Variable x1 = prj.getVariableByName("x1");
        x1.setType(Variable.TYPE_REAL);
        x1.setValues(VariableMethods.Value.asValueList("0.1", "0.2", "0.3", "0.4", "0.5", "0.6", "0.7", "0.8", "0.9"));

        Variable x2 = prj.getVariableByName("x2");
        x2.setType(Variable.TYPE_REAL);
        x2.setValues(VariableMethods.Value.asValueList("0.1", "0.2", "0.3", "0.4", "0.5", "0.6", "0.7", "0.8", "0.9"));

        prj.buildParameterList();

        prj.resetDiscreteCases(o);

        prj.setCases(prj.getDiscreteCases(), o);

        final BatchRun_v1 batchRun = new BatchRun_v1(o, prj, new File("tmp")) {

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

        Thread t = new Thread(new Runnable() {

            public void run() {
                try {
                    assert !batchRun.runBatch() : "Failed to stop batch";

                    Map<String, String[]> results = batchRun.getResultsStringArrayMap();
                    assert ArrayMapToMDString(results).length() > 0 : "Empty results";
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        t.start();

        Thread.sleep(5000);

        System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!! STOP !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        assert batchRun.stopBatch(): "Failed to stop batch";
        System.err.println("------------------------ STOPED ----------------------------------");

        t.join();

        System.err.println(Format.ArrayMapToMDString(batchRun.getResultsArrayMap()));

        assert batchRun.getArchiveDirectory().exists() : "Did not created archive dir " + batchRun.getArchiveDirectory();

        batchRun.shutdown();
    }

    @Test
    public void testMultipleCases() throws Exception {
        System.err.println("+++++++++++++++++++++++++ testMultipleCases");
        File tmp_in = tmp_in();

        IOPluginInterface plugin = IOPluginsLoader.newInstance(R, tmp_in);
        Project prj = ProjectController.createProject(tmp_in.getName(), tmp_in, R, plugin);

        plugin.setFormulaInterpreter(new RMathExpression(tmp_in.getName() + "_" + Configuration.timeDigest(), Configuration.isLog("R") ? new File(prj.getLogDir(), tmp_in.getName() + ".Rlog") : null));
        prj.setMainOutputFunction(plugin.suggestOutputFunctions().get(0));
        prj.setDesignerId(NODESIGNER_ID);

        Variable x1 = prj.getVariableByName("x1");
        x1.setType(Variable.TYPE_REAL);
        String[] x1_val = new String[]{"0.1", "0.2", "0.3", "0.4", "0.5", "0.6", "0.7", "0.8", "0.9"};
        x1.setValues(VariableMethods.Value.asValueList(x1_val));

        Variable x2 = prj.getVariableByName("x2");
        x2.setType(Variable.TYPE_REAL);
        String[] x2_val = new String[]{"0.1", "0.2", "0.3", "0.4", "0.5", "0.6", "0.7", "0.8", "0.9"};
        x2.setValues(VariableMethods.Value.asValueList(x2_val));

        prj.addGroup("g", "x1", "x2");

        prj.buildParameterList();

        prj.resetDiscreteCases(o);

        prj.setCases(prj.getDiscreteCases(), o);

        prj.useCache = false;

        final BatchRun_v1 batchRun = new BatchRun_v1(o, prj, new File("tmp")) {

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

        Thread t = new Thread(new Runnable() {

            public void run() {
                try {
                    assert !batchRun.runBatch() : "Failed to stop batch";

                    Map<String, String[]> results = batchRun.getResultsStringArrayMap();
                    assert ArrayMapToMDString(results).length() > 0 : "Empty results";
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        t.start();

        Thread.sleep(1000);

        System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!! STOP !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        assert batchRun.stopBatch() : "Could not stop batch";

        t.join();

        System.err.println(Format.ArrayMapToMDString(batchRun.getResultsArrayMap()));

        batchRun.shutdown();
    }

    String[] asStringArray(Object[] o) {
        String[] sa = new String[o.length];
        for (int i = 0; i < sa.length; i++) {
            sa[i] = asString(o[i]);
        }
        return sa;
    }

    @Test
    public void testDuplicateCases() throws Exception {
        System.err.println("+++++++++++++++++++++++++ testDuplicateCases");
        File tmp_in = tmp_in();

        IOPluginInterface plugin = IOPluginsLoader.newInstance(R, tmp_in);
        Project prj = ProjectController.createProject(tmp_in.getName(), tmp_in, R, plugin);

        plugin.setFormulaInterpreter(new RMathExpression(tmp_in.getName() + "_" + Configuration.timeDigest(), Configuration.isLog("R") ? new File(prj.getLogDir(), tmp_in.getName() + ".Rlog") : null));
        prj.setMainOutputFunction(plugin.suggestOutputFunctions().get(0));
        prj.setDesignerId(NODESIGNER_ID);

        Variable x1 = prj.getVariableByName("x1");
        x1.setType(Variable.TYPE_REAL);
        x1.setValues(VariableMethods.Value.asValueList(".1", ".2", ".1", ".1"));

        Variable x2 = prj.getVariableByName("x2");
        x2.setType(Variable.TYPE_REAL);
        x2.setValues(VariableMethods.Value.asValueList(".1", ".2", ".1", ".1"));

        prj.buildParameterList();

        prj.resetDiscreteCases(o);

        prj.setCases(prj.getDiscreteCases(), o);

        final BatchRun_v1 batchRun = new BatchRun_v1(o, prj, new File("tmp")) {

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

        Thread t = new Thread(new Runnable() {

            public void run() {
                try {
                    assert !batchRun.runBatch() : "Failed to stop batch";

                    Map<String, String[]> results = batchRun.getResultsStringArrayMap();
                    assert ArrayMapToMDString(results).length() > 0 : "Empty results";
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        t.start();

        Thread.sleep(1000);

        System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!! STOP !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        assert batchRun.stopBatch() : "Could not stop batch";

        t.join();

        System.err.println(Format.ArrayMapToMDString(batchRun.getResultsArrayMap()));

        batchRun.shutdown();
    }
}
