package org.funz.api;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.funz.Project;
import org.funz.ProjectController;
import org.funz.doeplugin.Design;
import org.funz.doeplugin.Designer;
import org.funz.log.Alert;
import org.funz.log.Log;
import org.funz.parameter.Cache;
import org.funz.parameter.Case;
import org.funz.parameter.CaseList;
import org.funz.parameter.Parameter;
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
public class Shell_v1 extends AbstractShell implements Design.Observer {

    public /*private*/ BatchRun_v1[] batchRuns;
    public /*private*/ LoopDesign_v1[] loopDesigns;
    private boolean notStopped = true;

    public Shell_v1(Project prj) throws Exception {
        setProject(prj);
    }

    /**
     * Constructor of grid computing project.
     *
     * @param _model Name of the model/code to use for computing.
     * @param _input List of input files to give as argument to the code/model.
     * These (ASCII) files should contain variables (like $toto).
     * @param _output
     * @param _designer
     * @throws java.lang.Exception
     */
    public Shell_v1(String _model, File _input, String _output, String _designer, Map _variableModel, Map _designOptions) throws Exception {
        this(_model, new File[]{_input}, new String[]{_output}, _designer, _variableModel, _designOptions);
    }

    public Shell_v1(String _model, File[] _input, String _output, String _designer, Map _variableModel, Map _designOptions) throws Exception {
        this(_model, _input, new String[]{_output}, _designer, _variableModel, _designOptions);
    }

    public Shell_v1(String _model, File _input, String[] _output, String _designer, Map _variableModel, Map _designOptions) throws Exception {
        this(_model, new File[]{_input}, _output, _designer, _variableModel, _designOptions);
    }

    public Shell_v1(String _model, File[] _input, String[] _output, String _designer, Map _variableModel, Map _designOptions) throws Exception {
        Log.out("Creating Shell for code " + _model + " with intput files " + Arrays.asList(_input) + " with design " + _designer + asString(_designOptions) + " in " + asString(_variableModel), 2);

        setArchiveDirectory((File) null);

        setInputModel(_model, _input);

        setOutputExpressions(_output);

        setInputVariables(_variableModel);//buildParameters();//setInputVariablesGroup((Map) null);

        setDesigner(_designer);

        setDesignOptions(_designOptions);

    }

    public void setOutputExpression(String custom_outs) {
        super.setOutputExpressions(custom_outs);
    }

    @Override
    public void setInputVariable(String varName, Object variable_model) {
        setInputVariables(newMap(varName, variable_model));
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
        for (Object ov : var_values.keySet()) {
            Variable v = Variable.setVariable(ov.toString(), var_values.get(ov), prj);

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
    void buildParameters() {
        super.buildParameters();
    }

    void buildDesign() throws Exception {
        String cv = prj.checkVariablesAreValid();
        if (cv != null) {
            throw new Exception("Input variables are not correclty set: " + cv);
        }

        List<Parameter> contparams = prj.getContinuousParameters();
        prj.resetDiscreteCases(this);

        if (haveNoDesign()) {
            buildWithoutDesign();
        } else {
            buildWithDesign();
        }
    }

    void buildWithDesign() throws Exception {
        String ov = prj.checkOutputFunctionIsValid();
        if (ov != null) {
            throw new Exception("Target of design not correclty set: " + prj.getMainOutputFunctionName() + ": " + ov);
        }

        prj.setCases(new CaseList(0), this);
        // split discrete and continous parameters
        CaseList discases = prj.getDiscreteCases();
        if (batchRuns == null || prj.getDiscreteCases().size() != batchRuns.length) {
            batchRuns = new BatchRun_v1[prj.getDiscreteCases().size()];
            loopDesigns = new LoopDesign_v1[prj.getDiscreteCases().size()];

            currentresult = new Map[prj.getDiscreteCases().size()];

            for (int j = 0; j < batchRuns.length; j++) {
                final int jj = j;

                batchRuns[j] = new BatchRun_v1(this, prj, new File(directory, prj.getDiscreteCases().get(jj).getRelativePath())) {

                    @Override
                    public List<Cache> getCache() {
                        return cache;
                    }

                    @Override
                    public List<Case> getSelectedCases() {
                        return ProjectController.getContinousCases(prj, jj);
                    }

                    @Override
                    public void out(String string, int i) {
                        Log.out("#" + jj + " " + string, i);
                    }

                    @Override
                    public void err(String msg, int i) {
                        Log.err("#" + jj + " " + msg, i);
                    }

                    @Override
                    public void err(Exception ex, int i) {
                        Log.err("#" + jj + " " + ex.getMessage(), i);
                    }

                    @Override
                    void afterRunCases() {
                        //super.afterRunCases(); No, so it will avoid to pause CalculatorsPool, continuing to refresh calculators list...
                    }

                };
                batchRuns[j].setArchiveDirectory(this.getArchiveDirectory());
                loopDesigns[j] = new LoopDesign_v1(this, this, prj, new File(directory, prj.getDiscreteCases().get(jj).getRelativePath())) {

                    @Override
                    public void out(String string, int i) {
                        Log.out("#" + jj + " " + string, i);
                    }

                    @Override
                    public void err(String msg, int i) {
                        Log.err("#" + jj + " " + msg, i);
                    }

                    @Override
                    public void err(Exception ex, int i) {
                        Log.err("#" + jj + " " + ex.getMessage(), i);
                    }
                };
                loopDesigns[j].setArchiveDirectory(this.getArchiveDirectory());
            }
        }

        if (_designOptions != null) {
            prj.getDesigner().setOptions(_designOptions);
        }

        for (Case c : discases) {
            int discCaseIdx = c.getDiscreteCaseId();

            File repository = prj.getDisCaseTmpDir(discCaseIdx);
            if (!repository.exists()) {
                if (!repository.mkdirs()) {
                    Log.err("Unable to create design repository:" + repository.getAbsolutePath(), 1);
                    throw new Exception("Unable to create design repository:" + repository.getAbsolutePath());
                }
            }

            loopDesigns[discCaseIdx].update();
            loopDesigns[discCaseIdx].buildDesign(prj.getDesignSession(discCaseIdx));
            prj.setDesign(loopDesigns[discCaseIdx].design, discCaseIdx);
        }

    }

    public /*private*/ boolean haveNoDesign() {
        return prj.getDesignerId() == null || prj.getDesignerId().equals(Designer.NODESIGNER_ID);
    }

    void buildWithoutDesign() throws Exception {
        prj.resetDiscreteCases(this);

        currentresult = new Map[prj.getDiscreteCases().size()];

        // split discrete and continous parameters
        CaseList discases = prj.getDiscreteCases();
        prj.setCases(discases, this);
        if (batchRuns == null) {
            batchRuns = new BatchRun_v1[1];

            batchRuns[0] = new BatchRun_v1(this, prj, directory) {

                @Override
                public List<Cache> getCache() {
                    return cache;
                }

                @Override
                public void out(String string, int i) {
                    Log.out("[@" + 0 + "]" + string, i);
                }

                @Override
                public void err(String msg, int i) {
                    Log.err("[@" + 0 + "]" + msg, i);
                }

                @Override
                public void err(Exception ex, int i) {
                    Log.err("[@" + 0 + "]" + ex.getMessage(), i);
                }
            };
            batchRuns[0].setArchiveDirectory(this.getArchiveDirectory());
        }
    }

    Case.Observer caseModifiedListener;

    public void setCaseModifiedListener(Case.Observer o) {
        caseModifiedListener = o;
    }

    @Override
    public void caseModified(int index, int what) {
        if (caseModifiedListener == null) {
            Log.out("case " + index + " modified (" + Case.MODIFIED_STRINGS[what] + ")", 5);
        } else {
            caseModifiedListener.caseModified(index, what);
        }
    }

    Design.Observer designUpdatedListener;

    public void setDesignUpdatedListener(Design.Observer o) {
        designUpdatedListener = o;
    }

    @Override
    public void designUpdated(int n) {
        if (designUpdatedListener == null) {
            Log.out("design updated", 5);
        } else {
            designUpdatedListener.designUpdated(n);
        }
    }

    @Override
    public Map<String, Object[]> getResultsArrayMap() {
        if (haveNoDesign()) {
            if (batchRuns != null && batchRuns.length == 1 && batchRuns[0] != null) {
                return batchRuns[0].getResultsArrayMap();
            } else {
                return null;
            }
        } else {
            if (currentresult != null) {
                return mergeArrayMap(currentresult);
            } else {
                return null;
            }
        }
    }

    @Override
    public Map<String, String[]> getResultsStringArrayMap() {
        if (haveNoDesign()) {
            if (batchRuns != null && batchRuns.length == 1 && batchRuns[0] != null) {
                return batchRuns[0].getResultsStringArrayMap();
            } else {
                return null;
            }
        } else {
            if (currentresult != null) {
                return mergeStringArrayMap(currentresult);
            } else {
                return null;
            }
        }
    }

    @Override
    public String getDesignerOption(String option) {
        return getDesignerOptions().get(option);
    }

    public Map<String, String> getDesignerOptions() {
        return prj.getDesigner().getOptions();
    }

    @Override
    public String[] getDesignerOptionKeys() {
        return prj.getDesigner().getOptions().keySet().toArray(new String[prj.getDesigner().getOptions().size()]);
    }

    @Override
    public String getState() {
        if (super.getState().equals(SHELL_ERROR)) {
            return super.getState();
        }

        if (haveNoDesign()) {
            return batchRuns[0].getState();
        } else {
            List<String> states = new LinkedList<>();

            for (int i = 0; i < currentresult.length; i++) {
                if (currentresult[i] != null) {
                    states.add(i, (currentresult.length > 1 ? currentresult[i].get("case") + " " : "")
                            + loopDesigns[i].getState().replace('\n', ',') + " "
                            + batchRuns[i].getState().replace('\n', ','));
                } else {
                    states.add(i, "?");
                }
            }
            return ASCII.cat("\n", states.toArray(new String[states.size()]));
        }
    }

    @Override
    public File getCalculationPointContent(String calculationPointId) {
        for (Case c : prj.getCases()) {
            if (c.getName().equals(calculationPointId)) {
                if (batchRuns[c.getDiscreteCaseId()].getState().equals(BatchRun_v1.BATCH_OVER)) {
                    return new File(getArchiveDirectory(), prj.getResultCaseRelativePath(c));
                } else {
                    return prj.getCaseTmpDir(c);
                }
            }
        }
        return null;
    }

    public /*private*/ Map<String, Object>[] currentresult;

    @Override
    public boolean startComputationAndWait() {
        try {
            //tic("buildDesign");
            buildDesign();
            //toc("buildDesign");
        } catch (Exception e) {
            Log.err("Error in buildDesign: " + e.getLocalizedMessage(), 0);
            Alert.showException(e);
            return false;
        }

        if (haveNoDesign()) {
            return startDiscComputationAndWait();
        } else {
            Thread[] computations = new Thread[prj.getDiscreteCases().size()];
            final boolean[] computations_success = new boolean[prj.getDiscreteCases().size()];
            for (int i = 0; i < computations.length; i++) {
                final int ii = i;
                computations[i] = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        boolean success = startDiscComputationAndWait(ii);
                        synchronized (computations_success) {
                            computations_success[ii] = success;
                        }
                    }
                });
                computations[i].start();
            }

            for (Thread computation : computations) {
                try {
                    computation.join();
                } catch (InterruptedException ex) {
                    Log.err(ex, 0);
                }
            }

            return all(computations_success);
        }
    }

    /*static Map<String, Object> putInTimeTree(Map<String, Object> last, int time) {
     Map<String, Object> newMap = new HashMap<>();
     newMap.putAll(last);
     for (String k : last.keySet()) {
     if (!k.matches(".+\\.(\\d+)") && !k.equals("case")) {
     newMap.put(k + "." + time, last.get(k));
     }
     }
     return newMap;
     }*/
    Map<String, String> addSuffix2(Map<String, String> m, String suffix) {
        Map<String, String> newm = new HashMap<String, String>();
        for (String k : m.keySet()) {
            newm.put(k + suffix, m.get(k));
        }
        return newm;
    }

    Map<String, Object[]> addSuffix(Map<String, Object[]> m, String suffix) {
        Map<String, Object[]> newm = new HashMap<>();
        for (String k : m.keySet()) {
            newm.put(k + suffix, m.get(k));
        }
        return newm;
    }

    public boolean startDiscComputationAndWait(int disc) {
        int time = 0;
        try {
            currentresult[disc] = new HashMap<>();
            currentresult[disc].put("time", time);

            currentresult[disc].put("case", prj.getDiscreteCases().get(disc).getName());

            Map<String, Object[]> X = loopDesigns[disc].initDesign();
            //currentresult[disc].putAll(asMapStringString(X));

            //System.err.println(disc + " X:\n" + ArrayMapToMDString(X));
            prj.addDesignCases(loopDesigns[disc].initialExperiments, this, disc);

            currentresult[disc].putAll(/*asMapStringString*/addSuffix(batchRuns[disc].getResultsArrayMap(), "." + time));//StringArrayMap()));
            currentresult[disc].put("state", loopDesigns[disc].getState() + " " + batchRuns[disc].getState());

            if (batchRuns[disc].runBatch()) {
                currentresult[disc].put("state", loopDesigns[disc].getState() + " " + batchRuns[disc].getState());
            } else {
                currentresult[disc].put("state", loopDesigns[disc].getState() + " " + batchRuns[disc].getState() + "... Failed");
            }

            currentresult[disc].putAll(/*asMapStringString*/addSuffix(batchRuns[disc].getResultsArrayMap(), "." + time));//StringArrayMap()));

            Map<String, Object[]> Y = batchRuns[disc].getResultsArrayMap();//.get(prj.getMainOutputFunctionName());

            while ((X = loopDesigns[disc].nextDesign(Y)) != null && notStopped) {
                time = time + 1;
                currentresult[disc].put("time", time);

                prj.addDesignCases(loopDesigns[disc].nextExperiments, this, disc);

                currentresult[disc].putAll(addSuffix(batchRuns[disc].getResultsArrayMap(), "." + time));//StringArrayMap()));
                currentresult[disc].put("state", loopDesigns[disc].getState() + " " + batchRuns[disc].getState());

                if (batchRuns[disc].runBatch()) {
                    currentresult[disc].put("state", loopDesigns[disc].getState() + " " + batchRuns[disc].getState());
                } else {
                    currentresult[disc].put("state", loopDesigns[disc].getState() + " " + batchRuns[disc].getState() + "... Failed");
                }

                currentresult[disc].putAll(addSuffix(batchRuns[disc].getResultsArrayMap(), "." + time));//StringArrayMap()));

                Y = batchRuns[disc].getResultsArrayMap();

                currentresult[disc].putAll(addSuffix2(loopDesigns[disc].getResultsTmp(), "." + time));
            }

            time = time + 1;
            currentresult[disc].put("time", time);

            if (notStopped) {
                currentresult[disc].putAll(loopDesigns[disc].getResults());
                currentresult[disc].putAll(batchRuns[disc].getResultsArrayMap());
            }

            return true;
        } catch (Exception e) {
            Log.err(e, 0);
            currentresult[disc].put("error", e.getMessage());
            return false;
        }
    }

    public boolean startDiscComputationAndWait() {
        try {
            return batchRuns[0].runBatch();
        } catch (Exception ex) {
            Log.err(ex, 0);
            return false;
        }
        /*currentresult[0] = new HashMap<>();
            
         currentresult[0].putAll((batchRuns[0].getResultsArrayMap()));//StringArrayMap()));
         currentresult[0].put("run", batchRuns[0].getState());
         boolean done = batchRuns[0].runBatch();
         if (batchRuns[0].runBatch()) {
         currentresult[0].put("run", batchRuns[0].getState());
         } else {
         currentresult[0].put("run", batchRuns[0].getState() + "... Failed");
         }
         currentresult[0].putAll(batchRuns[0].getResultsArrayMap()));//StringArrayMap());
            
         return done;*/
    }

    public boolean stopComputation() {
        boolean success = true;
        notStopped = false;
        for (BatchRun_v1 b : batchRuns) {
            success = success & b.stopBatch();
        }
        return success;
    }

    @Override
    public void shutdown() {
        if (batchRuns != null) {
            for (BatchRun_v1 b : batchRuns) {
                if (b != null) {
                    b.shutdown();
                }
            }
        }
        super.shutdown();
    }

    public static void main(String[] args) throws Exception {
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

        Funz_v1.init();

        File tmp_in = new File("tmp/branin.R");
        if (tmp_in.exists()) {
            tmp_in.delete();
        }
        Disk.copyFile(new File("src/main/resources/samples/branin.R"), tmp_in);

        Shell_v1 sac = new Shell_v1("R", new File[]{tmp_in}, new String[]{"cat"}, "GradientDescent", newMap("x1", "[0,1]", "x2", "[0,1]"), newMap("nmax", "3"));
        sac.setInputVariable("x1", new double[]{.1, .2, .3});
        //sac.redirectOutErr();

        sac.startComputationAndWait();

        Map<String, String[]> results = sac.getResultsStringArrayMap();

        sac.shutdown();

        //System.out.println(ArrayMapToMDString(results));
        System.out.println(asString(results.get("analysis")));

    }

    private List<Cache> cache = new LinkedList<>();

    public void addCacheDirectory(File dir) {
        if (!dir.isDirectory()) {
            Log.err(dir + " is not a directory. Not caching.", 2);
            return;
        }
        Log.out("Using cache directory " + dir, 3);
        cache.add(new Cache(Collections.singletonList(dir), false, true, new Cache.CacheActivityReport() {
            public void report(String s) {
                Log.out(s, 3);
            }
        }));
    }
}
