package org.funz.api;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.funz.Project;
import org.funz.conf.Configuration;
import org.funz.doeplugin.DesignConstants;
import org.funz.log.Alert;
import org.funz.log.Log;
import org.funz.parameter.Cache;
import org.funz.parameter.Case;
import org.funz.parameter.VarGroup;
import org.funz.parameter.Variable;
import org.funz.util.ASCII;
import static org.funz.util.Data.*;
import org.funz.util.Disk;

/**
 * Open API to launch Funz grid computations using parametrized input files and
 * wrapped code/model.<br/>
 * Once object instanciated, the run() call will launch all calculations and
 * return a Map of results.
 *
 * @author Y. Richet
 */
public class RunShell_v1 extends AbstractShell {

    public /*private */ BatchRun_v1 batchRun;

    @Override
    public String getDesignerOption(String option) {
        throw new UnsupportedOperationException("Not supported. Use Shell instead of RunShell.");
    }

    @Override
    public String[] getDesignerOptionKeys() {
        throw new UnsupportedOperationException("Not supported. Use Shell instead of RunShell.");
    }

    @Override
    public void setDesignOptions(Map _designOptions) {
        throw new UnsupportedOperationException("Not supported. Use Shell instead of RunShell.");
    }

    @Override
    public void setDesignOption(String key, String val) {
        throw new UnsupportedOperationException("Not supported. Use Shell instead of RunShell.");
    }

    @Override
    public void setDesigner(String designName) {
        if (designName != null && !designName.equals(DesignConstants.NODESIGNER_ID)) {
            throw new UnsupportedOperationException("Not supported. Use Shell instead of RunShell.");
        }
    }

    public RunShell_v1(Project prj) throws Exception {
        setProject(prj);
        setArchiveDirectory(prj.getDirectory());
        initBatchRun();
    }

    public RunShell_v1(String _model, File _input, String... outputExpression) throws Exception {
        this(_model, _input, null, outputExpression);
    }

    public RunShell_v1(String _model, File[] _input, String... outputExpression) throws Exception {
        this(_model, _input, null, outputExpression);
    }

    /**
     * Constructor of grid computing project.
     *
     * @param _model Name of the model/code to use for computing.
     * @param _input List of input files to give as argument to the code/model.
     * These (ASCII) files should contain variables (like $toto).
     */
    public RunShell_v1(String _model, File _input, Map _variableModel, String... outputExpression) throws Exception {
        this(_model, new File[]{_input}, _variableModel, outputExpression);
    }

    public RunShell_v1(String _model, File[] _input, Map _variableModel, String... outputExpression) throws Exception {
        Log.out("Creating RunShell for code " + _model + " with intput files " + asString(_input), 2);

        setInputModel(_model, _input);

        if (_variableModel != null) {
            setInputVariables(_variableModel);
        } else {
            buildParameters();
        }

        setOutputExpressions(outputExpression);

        setDesigner(DesignConstants.NODESIGNER_ID);

        initBatchRun();

        setArchiveDirectory(prj.getInputFiles().get(0).getFile().getParentFile());
    }

    @Override
    public void setInputModel(String _model, File... _input) throws Exception {
        super.setInputModel(_model, _input);
        if (batchRun != null && batchRun.getCache() != null) {
            for (final Cache c : batchRun.getCache()) { // needed to update cache if changing input model form external call
                new Thread() {

                    @Override
                    public void run() {
                        c.init(false);
                    }
                }.start();
            }
        }
    }

    void initBatchRun() {
        batchRun = new BatchRun_v1(this, prj, directory) {

            @Override
            public void out(String string, int i) {
                Log.out(string, i);
            }

            @Override
            public void err(String msg, int i) {
                Log.err(msg, i);
            }

            @Override
            public void err(Exception ex, int i) {
                Log.err(ex, i);
            }
        };
    }

    /**
     * Define input sample.
     *
     * @see setInputVariablesGroup(HashMap var_values)
     * @param var_values Combinations of variables (as String keys) and their
     * values (as String[]) to compute.
     * @throws Exception
     */
    @Override
    public void setInputVariablesGroup(String groupName, Map/*<String, String[]>*/ var_values) throws Exception {
        if (var_values != null) { // check consistency of var_values: all vars should have same number of values, else if 1 value
            int n = 0;
            for (Object ov : var_values.keySet()) {
                String v = ov.toString();
                if (var_values.get(v).getClass().isArray()) {
                    int l = java.lang.reflect.Array.getLength(var_values.get(v));
                    if (n == 0 && l > 1) {
                        n = l;
                    } else if (l != 1 && l != n) {
                        throw new Exception("Variable " + ov + " has " + l + " values instead of " + n);
                    }

                } else {
                    n = 1;//throw new Exception("Could not cast " + v + " values, assert an array, while " + var_values.get(v).getClass());
                }
            }
        }

        VarGroup g;
        if (var_values != null && groupName == null) {
            g = new VarGroup(ASCII.cat(",", var_values.keySet().toArray(new String[var_values.size()])));
        } else {
            g = new VarGroup(groupName);
        }
        VarGroup uniq = prj.getGroupByName("uniq");
        if (uniq == null) {
            uniq = new VarGroup("uniq");
        }
        //for (Variable v : prj.getVariables()) {
        if (var_values != null)
        for (Object ov : var_values.keySet()) {
            Variable v = Variable.newVariable(ov.toString(), var_values.get(ov), prj);

            if (v.getValues().size() > 1) {
                if (g.getVariables().size() == 0 || v.getNmOfValues() == g.getNmOfValues()) {
                    v.setGroup(g);
                    g.addVariable(v);
                } else {
                    Log.err("Could not add variable " + v.getName() + " (" + v.getNmOfValues() + " values) to group " + g.getName() + " (" + g.getNmOfValues() + " values)", 2);
                    throw new IllegalArgumentException("Could not add variable " + v.getName() + " (" + v.getNmOfValues() + " values) to group " + g.getName() + " (" + g.getNmOfValues() + " values)");
                }
            } else {
                v.setGroup(uniq);
                uniq.addVariable(v);
            }
        }
        
        if (g.getVariables().size() > 0) {
            prj.addGroup(g);
        }
        if (uniq.getVariables().size() > 0) {
            prj.addGroup(uniq);
        }

        buildParameters();
    }

    @Override
    public void setInputVariable(String varName, Object values) throws Exception {
        boolean found = false;
        for (Variable v : prj.getVariables()) {
            if (v.getName().equals(varName)) {
                found = true;
                Variable.setVariable(varName, values, prj);
            }
        }
        if (!found) {
            throw new Exception("Could not find variable " + varName + " in project.");
        }

        buildParameters();
    }

    public void setInputVariables(Map variable_model) {
        if (variable_model != null) {
            for (Object v : variable_model.keySet()) {
                String varName = v.toString();

                if (variable_model.get(v) instanceof Map) {
                    try {
                        setInputVariablesGroup(varName, (Map) variable_model.get(v));
                    } catch (Exception ex) {
                        Log.err(ex, 0);
                    }
                } else {
                    try {
                        Variable.setVariable(varName, variable_model.get(v), prj);
                    } catch (Exception ex) {
                        Log.err(ex, 0);
                    }
                }
            }
        }
        buildParameters();
    }

    public void addCacheDirectory(File dir) {
        batchRun.addCacheDirectory(dir);
    }

    @Override
    public String getState() {
        return state + " (" + getRunState().trim() + ")";
    }

    public String getRunState() {
        if (super.getState().equals(SHELL_ERROR)) {
            return super.getState();
        }

        return batchRun.getState().replace('\n', ',');
    }

    @Override
    public File getCalculationPointContent(String calculationPointId) {
        for (Case c : prj.getCases()) {
            if (c.getName().equals(calculationPointId)) {
                if (batchRun.getState().equals(BatchRun_v1.BATCH_OVER)) {
                    return new File(getArchiveDirectory(), prj.getResultCaseRelativePath(c));
                } else {
                    return prj.getCaseTmpDir(c);
                }
            }
        }
        return null;
    }

    @Override
    public boolean startComputationAndWait() {
        state = SHELL_RUNNING;
        prj.resetDiscreteCases(this);
        prj.setCases(prj.getDiscreteCases(), this);

        try {
            batchRun.setArchiveDirectory(this.directory);
            boolean ran = batchRun.runBatch();
            if (ran) 
                state = SHELL_OVER; 
            else 
                state = SHELL_ERROR;
            return ran;
        } catch (Exception ex) {
            Log.err(ex, 0);
            Alert.showException(ex);
            state = SHELL_EXCEPTION;
            return false;
        }
    }

    @Override
    public boolean stopComputation() {
        return batchRun.stopBatch();
    }

    public void setArchiveDirectory(File directory) {
        super.setArchiveDirectory(directory);
        if (batchRun != null) {
            try {
                batchRun.setArchiveDirectory(this.directory);
            } catch (IOException ex) {
                Log.err(ex, 0);
                Alert.showException(ex);
            }
        }
    }

    public void caseModified(int index, int what) {
        Log.out("case " + index + " modified (" + Case.MODIFIED_STRINGS[what] + ")", 5);
    }

    @Override
    public Map<String, String[]> getResultsStringArrayMap() {
        if (batchRun == null) {
            return null;
        }
        return batchRun.getResultsStringArrayMap();
    }

    @Override
    public Map<String, Object[]> getResultsArrayMap() {
        if (batchRun == null) {
            return null;
        }
        return batchRun.getResultsArrayMap();
    }

    @Override
    public void shutdown() {
        if (batchRun != null) {
            batchRun.shutdown();
        }
        super.shutdown();
    }

    /*public static void main(String[] args) throws Exception {
        Utils.startCalculator(1);
        Utils.startCalculator(2);
        Utils.startCalculator(3);
        Utils.startCalculator(4);
        Utils.startCalculator(5);
        Utils.startCalculator(6);
        Utils.startCalculator(7);
        Utils.startCalculator(8);
        Utils.startCalculator(9);
        Utils.startCalculator(10);

        Configuration.setVerboseLevel(10);

        Funz_v1.init();
        System.err.println(Funz_v1.CONF.toString());

        File tmp_in = new File("tmp/branin.R");
        if (tmp_in.exists()) {
            tmp_in.delete();
        }
        Disk.copyFile(new File("src/main/resources/samples/branin.R"), tmp_in);

        RunShell_v1 sac = new RunShell_v1("R", tmp_in, null); // R should be dedected by plugin automatically.
        Funz.setVerbosity(4);
        //sac.trap("INT");
        //redirectOutErr(new File("RunShell.err"), new File("RunShell.out"));

        HashMap<String, String[]> X = new HashMap<>();
        int n = 20;
        String[] x1 = new String[n];//{"0.1", "0.2", "0.3", "0.4", "0.5"};
        String[] x2 = new String[n];//{"0.3", "0.4", "0.5", "0.2", "0.3"};
        for (int i = 0; i < n; i++) {
            x1[i] = "" + Math.random();
            x2[i] = "" + Math.random();
        }
        X.put("x1", x1);
        X.put("x2", x2);
        sac.setInputVariablesGroup("x", X);
        sac.setOutputExpressions("cat");
        sac.startComputationAndWait();
        Map<String, String[]> results = sac.getResultsStringArrayMap();
        System.out.println("Results:\n=========");
        for (String r : results.keySet()) {
            System.out.println(r + "\n  " + ASCII.cat(",", results.get(r)));
        }

        sac.shutdown();
    }*/

}
