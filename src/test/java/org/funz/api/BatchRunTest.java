package org.funz.api;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.funz.Project;
import org.funz.ProjectController;
import org.funz.conf.Configuration;
import static org.funz.doeplugin.DesignConstants.NODESIGNER_ID;
import org.funz.ioplugin.IOPluginInterface;
import org.funz.ioplugin.IOPluginsLoader;
import org.funz.parameter.Case;
import org.funz.parameter.OutputFunctionExpression;
import org.funz.parameter.Variable;
import org.funz.parameter.VariableMethods;
import org.funz.script.RMathExpression;
import org.funz.util.ASCII;
import org.funz.util.Data;
import static org.funz.util.Data.*;
import org.funz.util.Disk;
import org.funz.util.Format;
import static org.funz.util.Format.ArrayMapToMDString;
import org.funz.util.ParserUtils;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author richet
 */
public class BatchRunTest extends org.funz.api.TestUtils {

    public static void main(String args[]) throws Exception {
        test(BatchRunTest.class.getName());
    }

    @Before
    public void setUp() throws Exception {
        super.setUp(BatchRunTest.class.getName());
    }

    Case.Observer o = new Case.Observer() {

        @Override
        public void caseModified(int index, int what) {
        }
    };

    @Test
    public void testOutputExpr() throws Exception {
        System.err.println("+++++++++++++++++++++++++ testOutputExpr");
        File tmp_in = tmp_in();

        IOPluginInterface plugin = IOPluginsLoader.newInstance(R, tmp_in);
        Project prj = ProjectController.createProject(tmp_in.getName(), tmp_in, R, plugin);

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

        BatchRun_v1 batchRun = new BatchRun_v1(o, prj, new File("tmp")) {

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

        prj.setMainOutputFunction(new OutputFunctionExpression.Numeric("cat+1"));

        assert batchRun.runBatch() : "Failed to run batch";

        Map results = batchRun.getResultsStringArrayMap();
        assert ArrayMapToMDString(results).length() > 0 : "Empty results";

        //System.err.println(ArrayMapToMDString(results));
        //System.err.println(ASCII.cat("\n",(String[])results.get("info")));
        assert results.containsKey("cat") : "No-expr output not present";
        assert ((String[]) results.get("cat"))[0].equals("[136.0767]") : "Bad evaluation of cat: " + Data.asString(results.get("cat"));
        assert results.containsKey("cat+1") : "Expr output not present: "+ results.toString();
        assert ((String[]) results.get("cat+1"))[0].contains("137.0767") : "Bad evaluation of cat+1: " + Data.asString(results.get("cat+1"));

        batchRun.shutdown();
    }
    
    @Test
    public void testOutputExprGaussian() throws Exception {
        System.err.println("+++++++++++++++++++++++++ testOutputExpr");
        File tmp_in = tmp_in();

        IOPluginInterface plugin = IOPluginsLoader.newInstance(R, tmp_in);
        Project prj = ProjectController.createProject(tmp_in.getName(), tmp_in, R, plugin);

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

        BatchRun_v1 batchRun = new BatchRun_v1(o, prj, new File("tmp")) {

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

        //prj.setMainOutputFunction(new OutputFunctionExpression.Numeric("cat+1"));
        prj.setMainOutputFunction(new OutputFunctionExpression.GaussianDensity("cat[1]+1","1"));

        assert batchRun.runBatch() : "Failed to run batch";

        Map results = batchRun.getResultsStringArrayMap();
        assert ArrayMapToMDString(results).length() > 0 : "Empty results";

        System.err.println(ArrayMapToMDString(results));
        
        //System.err.println(ArrayMapToMDString(results));
        //System.err.println(ASCII.cat("\n",(String[])results.get("info")));
        assert results.containsKey("cat") : "No-expr output not present";
        assert ((String[]) results.get("cat"))[0].equals("[136.0767]") : "Bad evaluation of cat: " + Data.asString(results.get("cat"));
        assert results.containsKey("N(cat[1]+1,1)") : "Expr output not present: "+ results.toString();
        assert ((String[]) results.get("N(cat[1]+1,1)"))[0].contains("137.0767") : "Bad evaluation of N(cat+1,1): " + Data.asString(results.get("cat+1"));

        batchRun.shutdown();
    }

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

        BatchRun_v1 batchRun = new BatchRun_v1(o, prj, new File("tmp")) {

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

        assert batchRun.runBatch() : "Failed to run batch";

        assert ArrayMapToMDString(batchRun.getResultsStringArrayMap()).length() > 0 : "Empty results";

        System.err.println(ArrayMapToMDString(batchRun.getResultsStringArrayMap()));

        batchRun.shutdown();
    }

    @Test
    public void test1CaseLongExec() throws Exception {
        System.err.println("+++++++++++++++++++++++++ test1CaseLongExec");
        File tmp_in = tmp_in();
        // will exceed tcp timeout for readResponse (10 s.)
        ASCII.saveFile(tmp_in, ParserUtils.getASCIIFileContent(new File("src/main/resources/samples/branin.R")).replace("t=0", "t=15"));

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

        BatchRun_v1 batchRun = new BatchRun_v1(o, prj, new File("tmp")) {

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

        assert batchRun.runBatch() : "Failed to run batch";

        assert ArrayMapToMDString(batchRun.getResultsStringArrayMap()).length() > 0 : "Empty results";

        System.err.println(ArrayMapToMDString(batchRun.getResultsStringArrayMap()));
        
        assert batchRun.getResultsStringArrayMap().get("output.cat")!=null:"Null result";
        assert batchRun.getResultsStringArrayMap().get("output.cat").length==1:"Not 1 result";
        assert batchRun.getResultsStringArrayMap().get("output.cat")[0].trim().length()>0 : "No output.cat result";

        batchRun.shutdown();
    }
    
    @Test
    public void test20Cases() throws Exception {
        System.err.println("+++++++++++++++++++++++++ test20Cases");
        File tmp_in = tmp_in();
        // will exceed tcp timeout for readResponse (10 s.)
        ASCII.saveFile(tmp_in, ParserUtils.getASCIIFileContent(new File("src/main/resources/samples/branin.R")).replace("t=0", "t=1"));

        IOPluginInterface plugin = IOPluginsLoader.newInstance(R, tmp_in);
        Project prj = ProjectController.createProject(tmp_in.getName(), tmp_in, R, plugin);
        prj.blacklistTimeout = 10000; //10 s. timeout after blacklisting

        assert prj.getVariableByName("x1") != null : "Variable x1 not detected";
        assert prj.getVariableByName("x2") != null : "Variable x2 not detected";
        assert prj.getVariableByName("x1").getDefaultValue() == null : "Variable x1 default value not null.";
        assert prj.getVariableByName("x2").getDefaultValue().equals(".5") : "Variable x2 default value not detected.";

        plugin.setFormulaInterpreter(new RMathExpression(tmp_in.getName() + "_" + Configuration.timeDigest(), Configuration.isLog("R") ? new File(prj.getLogDir(), tmp_in.getName() + ".Rlog") : null));
        prj.setMainOutputFunction(plugin.suggestOutputFunctions().get(0));
        prj.setDesignerId(NODESIGNER_ID);

        Variable x1 = prj.getVariableByName("x1");
        x1.setType(Variable.TYPE_REAL);
        x1.setValues(VariableMethods.Value.asValueList(".1",".2",".3",".4",".5"));

        Variable x2 = prj.getVariableByName("x2");
        x2.setType(Variable.TYPE_REAL);
        x2.setValues(VariableMethods.Value.asValueList(".1",".2",".3",".4"));

        prj.buildParameterList();

        prj.resetDiscreteCases(o);

        prj.setCases(prj.getDiscreteCases(), o);

        prj.useCache = false;

        BatchRun_v1 batchRun = new BatchRun_v1(o, prj, new File("tmp")) {

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

        assert batchRun.runBatch() : "Failed to run batch";

        assert ArrayMapToMDString(batchRun.getResultsStringArrayMap()).length() > 0 : "Empty results";
        System  .err.println(ArrayMapToMDString(batchRun.getResultsStringArrayMap()));
        
        System.err.println(Arrays.asList(batchRun.getResultsStringArrayMap().get("output")));
        
        String[] output = batchRun.getResultsStringArrayMap().get("output");
        List<Integer> failed = new LinkedList<>();
        for (int i = 0; i < output.length; i++) {
            if (output[i].contains("null")) {
                failed.add(i);
                System.err.println("Some null result: case "+i+"\n"+batchRun.getResultsStringArrayMap().get("info")[i].replace("\\n", "\n"));
            }
        }
        assert failed.isEmpty() : "Some failed cases : "+failed;

        batchRun.shutdown();
    }
   
    @Test
    public void test20CasesLongExec() throws Exception {
        System.err.println("+++++++++++++++++++++++++ test20CasesLongExec");
        File tmp_in = tmp_in();
        // will exceed tcp timeout for readResponse (10 s.)
        ASCII.saveFile(tmp_in, ParserUtils.getASCIIFileContent(new File("src/main/resources/samples/branin.R")).replace("t=0", "t=5"));

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
        x1.setValues(VariableMethods.Value.asValueList(".1",".2",".3",".4",".5"));

        Variable x2 = prj.getVariableByName("x2");
        x2.setType(Variable.TYPE_REAL);
        x2.setValues(VariableMethods.Value.asValueList(".1",".2",".3",".4"));

        prj.buildParameterList();

        prj.resetDiscreteCases(o);

        prj.setCases(prj.getDiscreteCases(), o);

        prj.useCache = false;

        BatchRun_v1 batchRun = new BatchRun_v1(o, prj, new File("tmp")) {

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

        assert batchRun.runBatch() : "Failed to run batch";

        assert ArrayMapToMDString(batchRun.getResultsStringArrayMap()).length() > 0 : "Empty results";
        System  .err.println(ArrayMapToMDString(batchRun.getResultsStringArrayMap()));
        
        System.err.println(Arrays.asList(batchRun.getResultsStringArrayMap().get("output")));
        
        String[] output = batchRun.getResultsStringArrayMap().get("output");
        List<Integer> failed = new LinkedList<>();
        for (int i = 0; i < output.length; i++) {
            if (output[i].contains("null")) {
                failed.add(i);
                System.err.println("Some null result: case "+i+"\n"+batchRun.getResultsStringArrayMap().get("info")[i].replace("\\n", "\n"));
            }
        }
        assert failed.isEmpty() : "Some failed cases : "+failed;

        batchRun.shutdown();
    }


    @Test
    public void test1CaseWithBinFile() throws Exception {
        System.err.println("+++++++++++++++++++++++++ test1CaseWithBinFile");

        File tmp_in = tmp_in();

        File bigtmp = new File("tmp/big");
        if (bigtmp.isDirectory()) {
            FileUtils.deleteDirectory(bigtmp);
        }
        assert new File("tmp/big").mkdirs() : "Could not create tmp/big dir";
        File tmp_in2 = new File("tmp/big/some.bin");
        if (tmp_in2.exists()) {
            tmp_in2.delete();
        }
        Disk.copyFile(new File("src/main/resources/samples/some.bin"), tmp_in2);

        IOPluginInterface plugin = IOPluginsLoader.newInstance(R, tmp_in);
        Project prj = ProjectController.createProject(tmp_in.getName(), tmp_in, R, plugin);
        prj.importFileOrDir(bigtmp);

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

        BatchRun_v1 batchRun = new BatchRun_v1(o, prj, new File("tmp")) {

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

        batchRun.setArchiveDirectory(new File("tmp/archivedir"));

        assert batchRun.runBatch() : "Failed to run batch";

        assert ArrayMapToMDString(batchRun.getResultsStringArrayMap()).length() > 0 : "Empty results";

        assert new File(batchRun.getArchiveDirectory(), "x2=0.1/x1=0.1/input/big/some.bin_md5").exists() : "Could not find md5 file";
        assert !new File(batchRun.getArchiveDirectory(), "x2=0.1/x1=0.1/input/big/some.bin").exists() : "Found binary file (not only md5 !)";
        assert !new File(batchRun.getArchiveDirectory(), "x2=0.1/x1=0.1/output/big/some.bin_md5").exists() : "Found md5 file in output";

        //System.err.println(ArrayMapToMDString(batchRun.getResultsStringArrayMap()));
        batchRun.shutdown();
    }

    @Test
    public void testDefinedVarValues() throws Exception {
        System.err.println("+++++++++++++++++++++++++ testDefinedVarValues");
        File tmp_in = tmp_in();
        ASCII.saveFile(tmp_in, ParserUtils.getASCIIFileContent(new File("src/main/resources/samples/branin.R")).replaceFirst("\\?x1", "\\?\\[x1~{.6}\\]"));
        ASCII.saveFile(tmp_in, ParserUtils.getASCIIFileContent(tmp_in).replaceFirst("\\?[x2~.5]", "\\?\\[x2~{.5}\\]"));

        IOPluginInterface plugin = IOPluginsLoader.newInstance(R, tmp_in);
        Project prj = ProjectController.createProject(tmp_in.getName(), tmp_in, R, plugin);

        assert prj.getVariableByName("x1") != null : "Variable x1 not detected";
        assert prj.getVariableByName("x2") != null : "Variable x2 not detected";
        assert prj.getVariableByName("x1").getValueArray().length == 1 : "Variable x1 must have 1 value.";
        assert prj.getVariableByName("x2").getValueArray().length == 1 : "Variable x2 must have 1 value.";

        plugin.setFormulaInterpreter(new RMathExpression(tmp_in.getName() + "_" + Configuration.timeDigest(), Configuration.isLog("R") ? new File(prj.getLogDir(), tmp_in.getName() + ".Rlog") : null));
        prj.setMainOutputFunction(plugin.suggestOutputFunctions().get(0));
        prj.setDesignerId(NODESIGNER_ID);

        prj.buildParameterList();

        prj.resetDiscreteCases(o);

        prj.setCases(prj.getDiscreteCases(), o);

        BatchRun_v1 batchRun = new BatchRun_v1(o, prj, new File("tmp")) {

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

        assert batchRun.runBatch() : "Failed to run batch";

        assert ArrayMapToMDString(batchRun.getResultsStringArrayMap()).length() > 0 : "Empty results";

        //System.err.println(ArrayMapToMDString(batchRun.getResultsStringArrayMap()));
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

        BatchRun_v1 batchRun = new BatchRun_v1(o, prj, new File("tmp")) {

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

        assert batchRun.runBatch() : "Failed to run batch";

        assert ArrayMapToMDString(batchRun.getResultsStringArrayMap()).length() > 0 : "Empty results";

        System.err.println(ArrayMapToMDString(batchRun.getResultsStringArrayMap()));
        batchRun.shutdown();
    }

    @Test
    public void test1FailedCaseOnRun() throws Exception {
        System.err.println("+++++++++++++++++++++++++ test1FailedCaseOnRun");
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
        x2.setValues(VariableMethods.Value.asValueList(".1"));

        prj.buildParameterList();

        prj.resetDiscreteCases(o);

        prj.setCases(prj.getDiscreteCases(), o);

        BatchRun_v1 batchRun = new BatchRun_v1(o, prj, new File("tmp")) {

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

        assert batchRun.runBatch() : "Failed to run batch";

        assert ArrayMapToMDString(batchRun.getResultsStringArrayMap()).length() > 0 : "Empty results";

        System.err.println(ArrayMapToMDString(batchRun.getResultsStringArrayMap()));
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
        x1.setValues(VariableMethods.Value.asValueList(".1", ".2", ".3", ".4"));

        Variable x2 = prj.getVariableByName("x2");
        x2.setType(Variable.TYPE_REAL);
        x2.setValues(VariableMethods.Value.asValueList(".1", ".2", ".3", ".4"));

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
        batchRun.stopBatch();

        t.join();

        System.err.println(Format.ArrayMapToMDString(batchRun.getResultsArrayMap()));

        assert batchRun.getArchiveDirectory().exists() : "Did not created archive dir " + batchRun.getArchiveDirectory();

        batchRun.shutdown();
    }

    @Test
    public void testConcurrency() throws Exception {
        System.err.println("+++++++++++++++++++++++++ testConcurrency");
        final boolean[] tests = new boolean[]{false, false, false, false};
        final boolean[] done = new boolean[tests.length];
        for (int i = 0; i < tests.length; i++) {
            done[i] = false;
        }
        Funz.setVerbosity(verbose);
        for (int i = 0; i < tests.length; i++) {
            final int I = i;
            new Thread(new Runnable() {

                public void run() {
                    System.err.println("Starting instance " + I);
                    final File tmp_in = new File("tmp/branin." + I + ".R");
                    if (tmp_in.exists()) {
                        tmp_in.delete();
                    }
                    while (true) {
                        try {
                            Disk.copyFile(new File("src/main/resources/samples/branin.R"), tmp_in);
                            break;
                        } catch (Exception e) {
                            System.err.println("Retrying initialization of test ...");
                        }
                    }
                    try {
                        IOPluginInterface plugin = IOPluginsLoader.newInstance(R, tmp_in);
                        Project prj = ProjectController.createProject(tmp_in.getName(), tmp_in, R, plugin);

                        plugin.setFormulaInterpreter(new RMathExpression(tmp_in.getName() + "_" + Configuration.timeDigest(), Configuration.isLog("R") ? new File(prj.getLogDir(), tmp_in.getName() + ".Rlog") : null));
                        prj.setMainOutputFunction(plugin.suggestOutputFunctions().get(0));
                        prj.setDesignerId(NODESIGNER_ID);

                        Variable x1 = prj.getVariableByName("x1");
                        x1.setType(Variable.TYPE_REAL);
                        String[] x1_val = new String[]{"0.1", "0.2", "0.3"};
                        x1.setValues(VariableMethods.Value.asValueList(x1_val));

                        Variable x2 = prj.getVariableByName("x2");
                        x2.setType(Variable.TYPE_REAL);
                        String[] x2_val = new String[]{"0.1", "0.2", "0.3"};
                        x2.setValues(VariableMethods.Value.asValueList(x2_val));

                        prj.addGroup("g", "x1", "x2");

                        System.err.println("resetDiscreteCases");
                        prj.resetDiscreteCases(o);
                        System.err.println("setCases");
                        prj.setCases(prj.getDiscreteCases(), o);
                        System.err.println("prj " + prj.getCases());

                        BatchRun_v1 batchRun = new BatchRun_v1(o, prj, new File("tmp")) {

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
                        batchRun.setArchiveDirectory( new File(batchRun.getArchiveDirectory(), "" + I));

                        assert batchRun.runBatch() : "Failed to run batch (Computing instance " + I + ")";

                        Map<String, Object[]> results = batchRun.getResultsArrayMap();
                        System.err.println(" results (Computing instance " + I + "): \n" + ArrayMapToMDString(results));

                        assert Arrays.deepEquals(asStringArray(results.get("x1")), x1_val) : "x1: " + Arrays.toString(results.get("x1")) + " != " + Arrays.toString(x1_val);
                        assert Arrays.deepEquals(asStringArray(results.get("x2")), x2_val) : "x2: " + Arrays.toString(results.get("x2")) + " != " + Arrays.toString(x2_val);
                        String[] z = f(x1_val, x2_val);//new String[]{"214.8081", "167.0327", "146.7375"};
                        assert Arrays.deepEquals(round2(asStringArray(results.get("cat"))), z) : "cat: " + Arrays.toString(round2(asStringArray(results.get("cat")))) + " != " + Arrays.toString(z);

                        batchRun.shutdown();

                        synchronized (tests) {
                            tests[I] = true;
                            done[I] = true;
                        }
                    } catch (Exception e) {
                        System.err.println("ERROR shell " + I + " \n" + e.getLocalizedMessage());
                        e.printStackTrace();
                        synchronized (tests) {
                            tests[I] = false;
                            done[I] = true;
                        }
                    } catch (AssertionError e) {
                        System.err.println("FAILED shell " + I + " \n" + e.getLocalizedMessage());
                        e.printStackTrace();
                        synchronized (tests) {
                            tests[I] = false;
                            done[I] = true;
                        }
                    }
                }
            }).start();
        }

        boolean alltrue = false;
        while (!alltrue) {
            Thread.sleep(2000);
            System.err.println("?");
            //System.err.println("============================\n" + Print.gridStatusInformation() + "============================\n");
            synchronized (tests) {
                alltrue = alltrue(done);
            }
        }

        assert alltrue(tests) : "One concurency run failed !: " + Arrays.toString(tests);
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

        BatchRun_v1 batchRun = new BatchRun_v1(o, prj, new File("tmp")) {

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

        assert batchRun.runBatch() : "Failed to run batch";

        Map<String, Object[]> results = batchRun.getResultsArrayMap();
        System.err.println("results: " + ASCII.cat(results));

        assert ArrayMapToMDString(batchRun.getResultsStringArrayMap()).length() > 0 : "Empty results";

        assert Arrays.deepEquals(asStringArray(results.get("x1")), x1_val) : "x1: " + Arrays.toString(results.get("x1")) + " != " + Arrays.toString(x1_val);
        assert Arrays.deepEquals(asStringArray(results.get("x2")), x2_val) : "x2: " + Arrays.toString(results.get("x2")) + " != " + Arrays.toString(x2_val);
        String[] z = f(x1_val, x2_val);//new String[]{"214.8081", "167.0327", "146.7375"};
        assert Arrays.deepEquals(round2(asStringArray(results.get("cat"))), z) : "cat: " + Arrays.toString(round2(asStringArray(results.get("cat")))) + " != " + Arrays.toString(z);

        batchRun.shutdown();
    }

    @Test
    public void testSomeCasesNotSelected() throws Exception {
        System.err.println("+++++++++++++++++++++++++ testSomeCasesNotSelected");
        File tmp_in = tmp_in();

        IOPluginInterface plugin = IOPluginsLoader.newInstance(R, tmp_in);
        Project prj = ProjectController.createProject(tmp_in.getName(), tmp_in, R, plugin);

        plugin.setFormulaInterpreter(new RMathExpression(tmp_in.getName() + "_" + Configuration.timeDigest(), Configuration.isLog("R") ? new File(prj.getLogDir(), tmp_in.getName() + ".Rlog") : null));
        prj.setMainOutputFunction(plugin.suggestOutputFunctions().get(0));
        prj.setDesignerId(NODESIGNER_ID);

        Variable x1 = prj.getVariableByName("x1");
        x1.setType(Variable.TYPE_REAL);
        String[] x1_val = new String[]{"0.1", "0.2"};
        x1.setValues(VariableMethods.Value.asValueList(x1_val));

        Variable x2 = prj.getVariableByName("x2");
        x2.setType(Variable.TYPE_REAL);
        String[] x2_val = new String[]{"0.1", "0.2"};
        x2.setValues(VariableMethods.Value.asValueList(x2_val));

        prj.buildParameterList();

        prj.resetDiscreteCases(o);

        prj.setCases(prj.getDiscreteCases(), o);
        prj.getCases().get(0).setSelected(false);

        BatchRun_v1 batchRun = new BatchRun_v1(o, prj, new File("tmp")) {

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

        assert batchRun.runBatch() : "Failed to run batch";

        Map<String, Object[]> results = batchRun.getResultsArrayMap();
        System.err.println("results: " + ASCII.cat(results));

        assert ArrayMapToMDString(batchRun.getResultsStringArrayMap()).length() > 0 : "Empty results";

        assert asStringArray(results.get("cat")).length == 3 : "length: " + round2(asStringArray(results.get("cat"))) + " != 3";
        assert round2(asStringArray(results.get("cat"))[0]).equals("73.0") : "cat: " + round2(asStringArray(results.get("cat"))[1]) + " != " + "73.0";

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
    public void testSomeFailedCases() throws Exception {
        System.err.println("+++++++++++++++++++++++++ testSomeFailedCases");
        File tmp_in = tmp_in();

        IOPluginInterface plugin = IOPluginsLoader.newInstance(R, tmp_in);
        Project prj = ProjectController.createProject(tmp_in.getName(), tmp_in, R, plugin);

        plugin.setFormulaInterpreter(new RMathExpression(tmp_in.getName() + "_" + Configuration.timeDigest(), Configuration.isLog("R") ? new File(prj.getLogDir(), tmp_in.getName() + ".Rlog") : null));
        prj.setMainOutputFunction(plugin.suggestOutputFunctions().get(0));
        prj.setDesignerId(NODESIGNER_ID);

        Variable x1 = prj.getVariableByName("x1");
        x1.setType(Variable.TYPE_REAL);
        x1.setValues(VariableMethods.Value.asValueList(".1", ".2", ".3", ".4"));

        Variable x2 = prj.getVariableByName("x2");
        x2.setType(Variable.TYPE_STRING);
        x2.setValues(VariableMethods.Value.asValueList(".1", ".2", ".3", "abc"));

        prj.buildParameterList();

        prj.resetDiscreteCases(o);

        prj.setCases(prj.getDiscreteCases(), o);

        BatchRun_v1 batchRun = new BatchRun_v1(o, prj, new File("tmp")) {

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

        assert batchRun.runBatch() : "Failed to run batch";

        assert ArrayMapToMDString(batchRun.getResultsStringArrayMap()).length() > 0 : "Empty results";

        System.err.println(ArrayMapToMDString(batchRun.getResultsStringArrayMap()));

        batchRun.shutdown();
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

        BatchRun_v1 batchRun = new BatchRun_v1(o, prj, new File("tmp")) {

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

        assert batchRun.runBatch() : "Failed to run batch";

        assert ArrayMapToMDString(batchRun.getResultsStringArrayMap()).length() > 0 : "Empty results";

        batchRun.shutdown();
    }

    @Test
    public void testIterativeDuplicateCases() throws Exception {
        System.err.println("+++++++++++++++++++++++++ testIterativeDuplicateCases");
        File tmp_in = tmp_in();

        IOPluginInterface plugin = IOPluginsLoader.newInstance(R, tmp_in);
        Project prj = ProjectController.createProject(tmp_in.getName(), tmp_in, R, plugin);

        plugin.setFormulaInterpreter(new RMathExpression(tmp_in.getName() + "_" + Configuration.timeDigest(), Configuration.isLog("R") ? new File(prj.getLogDir(), tmp_in.getName() + ".Rlog") : null));
        prj.setMainOutputFunction(plugin.suggestOutputFunctions().get(0));
        prj.setDesignerId(NODESIGNER_ID);

        Variable x1 = prj.getVariableByName("x1");
        x1.setType(Variable.TYPE_REAL);
        x1.setValues(VariableMethods.Value.asValueList(".1", ".2"));

        Variable x2 = prj.getVariableByName("x2");
        x2.setType(Variable.TYPE_REAL);
        x2.setValues(VariableMethods.Value.asValueList(".1", ".2"));

        prj.buildParameterList();

        prj.resetDiscreteCases(o);

        prj.setCases(prj.getDiscreteCases(), o);

        BatchRun_v1 batchRun = new BatchRun_v1(o, prj, new File("tmp")) {

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

        assert batchRun.runBatch() : "Failed to run batch";

        Map<String, Object[]> results = batchRun.getResultsArrayMap();

        assert ArrayMapToMDString(results).length() > 0 : "Empty results";

        List<String[]> newExps = new LinkedList<>();
        newExps.add(new String[]{"x1=0.1", "x2=0.1"});
        newExps.add(new String[]{"x1=0.1", "x2=0.2"});
        prj.addCases(newExps, o);

        assert batchRun.runBatch() : "Failed to run 2nd batch";

        Map<String, Object[]> results2 = batchRun.getResultsArrayMap();

        assert results2.get("cat").length == 6 : "All cases not run !\n" + Data.asString(results2);

        System.err.println(ArrayMapToMDString(results));
        System.err.println("===================================================================================");
        System.err.println(ArrayMapToMDString(results2));

        batchRun.shutdown();
    }

    @Test
    public void testVectorize() throws Exception {
        System.err.println("+++++++++++++++++++++++++ testVectorize");
        File tmp_in = tmp_in();

        IOPluginInterface plugin = IOPluginsLoader.newInstance(R, tmp_in);
        Project prj = ProjectController.createProject(tmp_in.getName(), tmp_in, R, plugin);

        plugin.setFormulaInterpreter(new RMathExpression(tmp_in.getName() + "_" + Configuration.timeDigest(), Configuration.isLog("R") ? new File(prj.getLogDir(), tmp_in.getName() + ".Rlog") : null));
        prj.setMainOutputFunction(plugin.suggestOutputFunctions().get(0));
        prj.setDesignerId(NODESIGNER_ID);

        Variable x1 = prj.getVariableByName("x1");
        x1.setType(Variable.TYPE_REAL);
        String[] x1_val = new String[]{"0.1", "0.2", "0.3"};
        x1.setValues(VariableMethods.Value.asValueList(x1_val));

        Variable x2 = prj.getVariableByName("x2");
        x2.setType(Variable.TYPE_REAL);
        String[] x2_val = new String[]{"0.1", "0.2", "0.3"};
        x2.setValues(VariableMethods.Value.asValueList(x2_val));

        prj.addGroup("g", "x1", "x2");

        System.err.println(prj.getGroupByName("g"));

        prj.buildParameterList();

        prj.resetDiscreteCases(o);

        prj.setCases(prj.getDiscreteCases(), o);

        BatchRun_v1 batchRun = new BatchRun_v1(o, prj, new File("tmp")) {

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

        assert batchRun.runBatch() : "Failed to run batch";

        Map<String, Object[]> results = batchRun.getResultsArrayMap();
        assert ArrayMapToMDString(results).length() > 0 : "Empty results";

        assert Arrays.deepEquals(asStringArray(results.get("x1")), x1_val) : "x1: " + Arrays.toString(results.get("x1")) + " != " + Arrays.toString(x1_val);
        assert Arrays.deepEquals(asStringArray(results.get("x2")), x2_val) : "x2: " + Arrays.toString(results.get("x2")) + " != " + Arrays.toString(x2_val);
        String[] z = f(x1_val, x2_val);//new String[]{"214.8081", "167.0327", "146.7375"};
        assert Arrays.deepEquals(round2(asStringArray(results.get("cat"))), z) : "cat: " + Arrays.toString(round2(asStringArray(results.get("cat")))) + " != " + Arrays.toString(z);

        batchRun.shutdown();
    }

    @Test
    public void testStopRestart() throws Exception {
        System.err.println("+++++++++++++++++++++++++ testStopRestart");
        File tmp_in = tmp_in();
        ASCII.saveFile(tmp_in, ParserUtils.getASCIIFileContent(new File("src/main/resources/samples/branin.R")).replace("t=0", "t=1"));

        IOPluginInterface plugin = IOPluginsLoader.newInstance(R, tmp_in);
        Project prj = ProjectController.createProject(tmp_in.getName(), tmp_in, R, plugin);

        plugin.setFormulaInterpreter(new RMathExpression(tmp_in.getName() + "_" + Configuration.timeDigest(), Configuration.isLog("R") ? new File(prj.getLogDir(), tmp_in.getName() + ".Rlog") : null));
        prj.setMainOutputFunction(plugin.suggestOutputFunctions().get(0));
        prj.setDesignerId(NODESIGNER_ID);

        Variable x1 = prj.getVariableByName("x1");
        x1.setType(Variable.TYPE_REAL);
        x1.setValues(VariableMethods.Value.asValueList(".1", ".2"));

        Variable x2 = prj.getVariableByName("x2");
        x2.setType(Variable.TYPE_REAL);
        x2.setValues(VariableMethods.Value.asValueList(".1", ".2"));

        prj.buildParameterList();

        prj.resetDiscreteCases(o);

        prj.setCases(prj.getDiscreteCases(), o);

        prj.setMaxCalcs(1);

        final BatchRun_v1 batchRun = new BatchRun_v1(o, prj, new File("tmp")) {

            @Override
            public void out(String string, int i) {
                TestUtils.out("O " + string, i);
            }

            @Override
            public void err(String msg, int i) {
                TestUtils.err("E " + msg, i);
            }

            @Override
            public void err(Exception ex, int i) {
                TestUtils.err("X " + ex, i);
            }
        };
        batchRun.getCache().clear();

        Thread t = new Thread(new Runnable() {

            public void run() {
                try {
                    assert !batchRun.runBatch() : "Failed to break batch";

                    Map<String, String[]> results = batchRun.getResultsStringArrayMap();
                    assert results != null : "Null results";
                    assert ArrayMapToMDString(results).length() > 0 : "Empty results";
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        t.start();

        while(batchRun.getResultsArrayMap().get("cat") == null) Thread.sleep(1000); // should not exceed too much t=1000*3 used in branin.R
        System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!! KILL !!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        assert batchRun.stopBatch() : "Failed to stop batch";

        t.join();

        Map<String, Object[]> results = batchRun.getResultsArrayMap();
        assert results != null : "1st results null !";
        //assert results.get("cat")!=null : "null cat results";
        assert results.get("cat") != null : "1st output null : " + ArrayMapToMDString(results);
        String rescat = ASCII.cat(" , ", results.get("cat")).trim();
        assert rescat.contains("?") | rescat.startsWith(",") | rescat.endsWith(",") : "All cases run before breaking !\n"+ASCII.cat(" , ", results.get("cat"));
        assert results.get("cat").length >= 0 : "No cases has run before breaking !";

        System.err.println(ArrayMapToMDString(results));
        System.err.println("===================================================================================");
        assert batchRun.runBatch() : "Failed to run 2nd batch";

        Map<String, Object[]> results2 = batchRun.getResultsArrayMap();
        assert results2.get("cat") != null : "null cat results2";

        System.err.println(ArrayMapToMDString(results2));

        assert ArrayMapToMDString(results2).length() > 0 : "Empty 2nd results";

        //assert results2.get("cat").length < 9 : "All cases run in 2nd batch !\n"+ArrayMapToMDString(results2);
        assert results2.get("cat").length + results.get("cat").length >= 4 : "Not all cases run in 2 batches ! (" + (results2.get("cat").length + results.get("cat").length) + "/4)\n" + batchRun.prj.getCases();

        batchRun.shutdown();
    }

    @Test
    public void test2StopRestart() throws Exception {
        System.err.println("+++++++++++++++++++++++++ test2StopRestart");
        File tmp_in0 = new File("tmp/branin0.R");
        if (tmp_in0.exists()) {
            tmp_in0.delete();
        }
        ASCII.saveFile(tmp_in0, ParserUtils.getASCIIFileContent(new File("src/main/resources/samples/branin.R")).replace("t=0", "t=1"));

        IOPluginInterface plugin0 = IOPluginsLoader.newInstance(R, tmp_in0);
        Project prj0 = ProjectController.createProject(tmp_in0.getName(), tmp_in0, R, plugin0);

        plugin0.setFormulaInterpreter(new RMathExpression(tmp_in0.getName() + "_" + Configuration.timeDigest(), Configuration.isLog("R") ? new File(prj0.getLogDir(), tmp_in0.getName() + ".Rlog") : null));
        prj0.setMainOutputFunction(plugin0.suggestOutputFunctions().get(0));
        prj0.setDesignerId(NODESIGNER_ID);

        Variable x10 = prj0.getVariableByName("x1");
        x10.setType(Variable.TYPE_REAL);
        x10.setValues(VariableMethods.Value.asValueList(".01", ".02"));

        Variable x20 = prj0.getVariableByName("x2");
        x20.setType(Variable.TYPE_REAL);
        x20.setValues(VariableMethods.Value.asValueList(".01", ".02"));

        prj0.buildParameterList();
        prj0.resetDiscreteCases(o);
        prj0.setCases(prj0.getDiscreteCases(), o);
        prj0.setMaxCalcs(1);

        final BatchRun_v1 batchRun0 = new BatchRun_v1(o, prj0, new File("tmp")) {

            @Override
            public void out(String string, int i) {
                TestUtils.out("0:O " + string, i);
            }

            @Override
            public void err(String msg, int i) {
                TestUtils.err("0:E " + msg, i);
            }

            @Override
            public void err(Exception ex, int i) {
                TestUtils.err("0:X " + ex, i);
            }
        };
        batchRun0.getCache().clear();

        Thread t0 = new Thread(new Runnable() {

            public void run() {
                try {
                    assert !batchRun0.runBatch() : "0: Failed to break batch";

                    Map<String, String[]> results = batchRun0.getResultsStringArrayMap();
                    assert results != null : "0: Null results";
                    assert ArrayMapToMDString(results).length() > 0 : "0: Empty results";
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        t0.start();

        while(batchRun0.getResultsArrayMap().get("cat") == null) Thread.sleep(1000); // should not exceed too much t=1000*3 used in branin.R
        System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!! KILL !!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        assert batchRun0.stopBatch() : "0: Failed to stop batch";

        //t0.join();
        Map<String, Object[]> results0 = batchRun0.getResultsArrayMap();
        assert results0 != null : "0: 1st results null !";
        //assert results.get("cat")!=null : "null cat results";
        assert results0.get("cat") != null : "0: 1st output null : " + ArrayMapToMDString(results0);
        assert ASCII.cat(" , ", results0.get("cat")).contains("?") : "0: All cases run before breaking !";
        assert results0.get("cat").length >= 0 : "0: No cases has run before breaking !";

        System.err.println(ArrayMapToMDString(results0));
        System.err.println("===================================================================================");

        Thread.sleep(1000);

        System.err.println("!!!!!!!!!!!!!!!!!!!!!!!!!! START 2nd Project !!!!!!!!!!!!!!!!!!!!!!!!!!!!");

        File tmp_in = tmp_in();
        ASCII.saveFile(tmp_in, ParserUtils.getASCIIFileContent(new File("src/main/resources/samples/branin.R")).replace("t=0", "t=1"));

        IOPluginInterface plugin = IOPluginsLoader.newInstance(R, tmp_in);
        Project prj = ProjectController.createProject(tmp_in.getName(), tmp_in, R, plugin);

        plugin.setFormulaInterpreter(new RMathExpression(tmp_in.getName() + "_" + Configuration.timeDigest(), Configuration.isLog("R") ? new File(prj.getLogDir(), tmp_in.getName() + ".Rlog") : null));
        prj.setMainOutputFunction(plugin.suggestOutputFunctions().get(0));
        prj.setDesignerId(NODESIGNER_ID);

        Variable x1 = prj.getVariableByName("x1");
        x1.setType(Variable.TYPE_REAL);
        x1.setValues(VariableMethods.Value.asValueList(".1", ".2"));

        Variable x2 = prj.getVariableByName("x2");
        x2.setType(Variable.TYPE_REAL);
        x2.setValues(VariableMethods.Value.asValueList(".1", ".2"));

        prj.buildParameterList();
        prj.resetDiscreteCases(o);
        prj.setCases(prj.getDiscreteCases(), o);
        prj.setMaxCalcs(1);

        final BatchRun_v1 batchRun = new BatchRun_v1(o, prj, new File("tmp")) {

            @Override
            public void out(String string, int i) {
                TestUtils.out("1:O " + string, i);
            }

            @Override
            public void err(String msg, int i) {
                TestUtils.err("1:E " + msg, i);
            }

            @Override
            public void err(Exception ex, int i) {
                ex.printStackTrace();
                TestUtils.err("1:X " + ex, i);
            }
        };
        batchRun.getCache().clear();

        assert batchRun.runBatch() : "1: Failed to run batch";

        Map<String, Object[]> results = batchRun.getResultsArrayMap();
        assert results != null : "1: 1st results null !";
        //assert results.get("cat")!=null : "null cat results";
        assert results.get("cat") != null : "1: 1st output null : " + ArrayMapToMDString(results);
        assert !ASCII.cat(" , ", results.get("cat")).contains("?") : "1: Not all cases run !";
        assert results.get("cat").length >= 0 : "1: No cases has run !";

        System.err.println(ArrayMapToMDString(results));
        System.err.println("===================================================================================");

        batchRun0.shutdown();
        batchRun.shutdown();

    }

    @Test
    public void testAddCases() throws Exception {
        System.err.println("+++++++++++++++++++++++++ testAddCases");
        File tmp_in = tmp_in();

        IOPluginInterface plugin = IOPluginsLoader.newInstance(R, tmp_in);
        Project prj = ProjectController.createProject(tmp_in.getName(), tmp_in, R, plugin);

        plugin.setFormulaInterpreter(new RMathExpression(tmp_in.getName() + "_" + Configuration.timeDigest(), Configuration.isLog("R") ? new File(prj.getLogDir(), tmp_in.getName() + ".Rlog") : null));
        prj.setMainOutputFunction(plugin.suggestOutputFunctions().get(0));
        prj.setDesignerId(NODESIGNER_ID);

        Variable x1 = prj.getVariableByName("x1");
        x1.setType(Variable.TYPE_REAL);
        x1.setValues(VariableMethods.Value.asValueList(".1", ".2"));

        Variable x2 = prj.getVariableByName("x2");
        x2.setType(Variable.TYPE_REAL);
        x2.setValues(VariableMethods.Value.asValueList(".1", ".2"));

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

        assert batchRun.runBatch() : "Failed to run batch";

        Map<String, Object[]> results = batchRun.getResultsArrayMap();

        System.err.println(ArrayMapToMDString(results));

        assert results.get("cat").length == 4 : "All cases not run !";

        List<String[]> newExps = new LinkedList<>();
        newExps.add(new String[]{"x1=0.5", "x2=0.5"});
        newExps.add(new String[]{"x1=0.6", "x2=0.6"});
        prj.addCases(newExps, o);

        assert batchRun.runBatch() : "Failed to run 2nd batch";

        Map<String, Object[]> results2 = batchRun.getResultsArrayMap();

        assert results2.get("cat").length == 6 : "All cases not run !";

        System.err.println(ArrayMapToMDString(results));
        System.err.println("===================================================================================");
        System.err.println(ArrayMapToMDString(results2));

        batchRun.shutdown();
    }

    @Test
    public void testCodeNoResult() throws Exception {
        System.err.println("+++++++++++++++++++++++++ testCodeError");
        File tmp_in = tmp_in();

        IOPluginInterface plugin = IOPluginsLoader.newInstance(R, tmp_in);
        Project prj = ProjectController.createProject(tmp_in.getName(), tmp_in, R, plugin);

        plugin.setFormulaInterpreter(new RMathExpression(tmp_in.getName() + "_" + Configuration.timeDigest(), Configuration.isLog("R") ? new File(prj.getLogDir(), tmp_in.getName() + ".Rlog") : null));
        prj.setMainOutputFunction(plugin.suggestOutputFunctions().get(0));
        prj.setDesignerId(NODESIGNER_ID);

        Variable x1 = prj.getVariableByName("x1");
        x1.setType(Variable.TYPE_REAL);
        x1.setValues(VariableMethods.Value.asValueList("-.1", ".2"));

        Variable x2 = prj.getVariableByName("x2");
        x2.setType(Variable.TYPE_REAL);
        x2.setValues(VariableMethods.Value.asValueList(".1", ".2"));

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

        assert batchRun.runBatch() : "Failed to run batch";

        Map<String, Object[]> results = batchRun.getResultsArrayMap();
        System.err.println("res " + ArrayMapToMDString(results));

        assert results.get("cat").length == 4 : "Not all cases run !";

        String[] good_res = {"[NaN]", "[73.07538]", "[NaN]", "[50.75626]"};
        assert Arrays.deepEquals(asStringArray(results.get("cat")), asStringArray(good_res)) : Arrays.toString(asStringArray(results.get("cat"))) + " != " + Arrays.toString(asStringArray(good_res));

        System.err.println(ArrayMapToMDString(results));

        batchRun.shutdown();
    }

}
