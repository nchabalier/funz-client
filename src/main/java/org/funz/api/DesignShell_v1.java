package org.funz.api;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.funz.Constants;
import org.funz.Project;
import static org.funz.api.Utils.*;
import org.funz.conf.Configuration;
import org.funz.doeplugin.Design;
import org.funz.doeplugin.DesignConstants;
import org.funz.doeplugin.Experiment;
import org.funz.ioplugin.ExtendedIOPlugin;
import org.funz.log.Alert;
import org.funz.log.Log;
import org.funz.parameter.Case;
import org.funz.parameter.CaseList;
import org.funz.parameter.OutputFunctionExpression;
import org.funz.parameter.Parameter;
import org.funz.parameter.Variable;
import org.funz.util.ASCII;
import org.funz.util.Data;
import static org.funz.util.Data.*;
import org.funz.util.Format;
import static org.funz.util.Format.ArrayMapToMDString;
import org.math.array.DoubleArray;

/**
 * @author Y. Richet
 */
public class DesignShell_v1 extends AbstractShell implements Design.Observer {

    public LoopDesign_v1 loopDesign;
    private Function f;
    private Map<String, Object> currentresult;
    private boolean notStopped = true;
    public String output = DEFAULT_FUNCTION_NAME;

    public DesignShell_v1(Project prj) throws Exception {
        setProject(prj);
    }

    /**
     * @param f Function to implement as DesignShell_v1.Function
     * @param _designer name of the design to use. See Shell.DESIGNS static list
     * of available DoE.
     * @param _variableBounds Map of input variables and their model expression
     * : {x1="[0,1]",x2="[-1,1]",...}
     */
    public DesignShell_v1(Function f, String _designer, Map _variableBounds, Map _designOptions) throws Exception {
        Log.out("Creating DesignShell for function " + f + " with design " + _designer + " " + asString(_designOptions) + " in " + asString(_variableBounds), 2);

        this.f = f;

        setArchiveDirectory((File) null);

        setInputModel(_designer, new File(_designer));

        prj.setMainOutputFunction(OutputFunctionExpression.read(output));
        prj.setCode("Function: `" + f + "`");

        setDesigner(_designer);

        setDesignOptions(_designOptions);

        setInputVariables(_variableBounds);
    }

    @Override
    public void setInputModel(String _model, File... _input) throws Exception {
        input = _input;
        prj = new Project(_model + "_" + Configuration.timeDigest() + "_" + hashCode());
        prj.setPlugin(new ExtendedIOPlugin());
    }

    @Override
    public void setInputVariable(String varName, Object model) throws Exception {
        setInputVariables(newMap(varName, model));
    }

    @Override
    public void setInputVariablesGroup(String n, Map var_model) throws Exception {
        throw new UnsupportedOperationException("Not supported for design shell.");
    }

    public void setInputVariables(Map _variableBounds) {
        if (_variableBounds != null) {
            for (Object v : _variableBounds.keySet()) {
                String varName = v.toString();

                Variable variable = prj.getVariableByName(varName);

                if (variable == null) {
                    variable = new Variable(prj, varName);
                    prj.getVariables().add(variable);
                }

                variable.setType(Variable.TYPE_CONTINUOUS);
                double[] bounds = (double[]) toObject(_variableBounds.get(v).toString());
                variable.setLowerBound(bounds[0]);
                variable.setUpperBound(bounds[1]);
            }
        }
        buildParameters();
    }

    public List<Case> buildDesign() throws Exception {
        //System.out.println("makeInitialDesign");
        List<Parameter> contparams = prj.getContinuousParameters();

        prj.resetDiscreteCases(this);
        if (contparams.isEmpty()) {
            //_prj.getCases() = _prj.getDiscreteCases();
            prj.setCases(prj.getDiscreteCases(), this);
            return null;
        }

        prj.setCases(new CaseList(0), this);
// split discrete and continous parameters
        CaseList discases = prj.getDiscreteCases();
        assert discases.size() == 1 : "More than one discrete case not supported";

        if (loopDesign == null) {
            loopDesign = new LoopDesign_v1(this, this, prj, directory) {
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
                    err(ex.getMessage(), i);
                }
            };
            loopDesign.setArchiveDirectory(this.getArchiveDirectory());
        }

        if (_designOptions != null) {
            prj.getDesigner().setOptions(_designOptions);
        }

        File repository = prj.getDisCaseTmpDir(0);
        if (!repository.exists()) {
            if (!repository.mkdirs()) {
                Log.err("Unable to create design repository:" + repository.getAbsolutePath(), 1);
                throw new Exception("Unable to create design repository:" + repository.getAbsolutePath());
            }
        }

        loopDesign.update();
        loopDesign.buildDesign(prj.getDesignSession(0));

        prj.setDesign(loopDesign.design, 0);

        return prj.addDesignCases(loopDesign.initialExperiments, this, 0);
        //}
    }

    public List<Case> addExperiments(List<Experiment> exps) {
        return prj.addDesignCases(exps, this, 0);
    }

    public void setCacheExperiments(boolean cache) {
        loopDesign.setCacheExperiments(cache);
    }

    @Override
    public void caseModified(int index, int what) {
        Log.out("case " + index + " modified (" + Case.MODIFIED_STRINGS[what] + ")", 5);
    }

    @Override
    public String getDesignerOption(String option) {
        return loopDesign.getDesignerOptions().get(option);
    }

    @Override
    public String[] getDesignerOptionKeys() {
        return loopDesign.getDesignerOptions().keySet().toArray(new String[loopDesign.getDesignerOptions().size()]);
    }

    public LoopDesign_v1 getLoopDesign() {
        return loopDesign;
    }

    @Override
    public Map<String, Object[]> getResultsArrayMap() {
        Map<String, Object[]> all = new WeakHashMap<>();
        if (currentresult != null) {
            for (String k : currentresult.keySet()) {
                all.put(k, new Object[]{currentresult.get(k)});
            }
        }
        return all;
    }

    @Override
    public Map<String, String[]> getResultsStringArrayMap() {
        Map<String, String[]> all = new WeakHashMap<>();
        if (currentresult != null) {
            for (String k : currentresult.keySet()) {
                all.put(k, new String[]{asString(currentresult.get(k))});
            }
        }
        return all;
    }

    @Override
    public String getState() {
        return state + " (" + getDesignState().trim() + ")";
    }

    public String getDesignState() {
        if (super.getState().equals(SHELL_ERROR)) {
            return super.getState();
        }

        return loopDesign.getState().replace('\n', ',');
    }

    @Override
    public File getCalculationPointContent(String calculationPointId) {
        return null;
    }

    List<Map<String, Object[]>> allX;
    List<Map<String, Object[]>> allY;

    @Override
    public void setArchiveDirectory(File directory) {
        super.setArchiveDirectory(directory);
        if (loopDesign != null) {
            try {
                loopDesign.setArchiveDirectory(this.directory);
            } catch (IOException ex) {
                Log.err(ex, 0);
                Alert.showException(ex);
            }
        }
    }

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

    static Map<String, Object[]> merge(List<Map<String, Object[]>> cases) {
        if (cases == null || cases.isEmpty()) {
            return null;
        }

        Map<String, Object[]> all = new HashMap<String, Object[]>();
        for (String k : cases.get(0).keySet()) {
            List<Object> all_array = new LinkedList<Object>();
            for (Map<String, Object[]> content : cases) {
                all_array.addAll(Arrays.asList(content.get(k)));
            }
            all.put(k, all_array.toArray(new Object[all_array.size()]));
        }

        return all;
    }

    @Override
    public void saveResults() {
        if (prj == null) {
            Log.out("Not writing design results (project null).", 3);
            return;
        }

        assert !prj.getDesignerId().equals(DesignConstants.NODESIGNER_ID) : "no designer for a Design shell !";

        super.saveResults();
    }
    
    @Override
    public boolean startComputationAndWait() {
        state = SHELL_RUNNING;
        int time = 0;
        try {
            currentresult = new HashMap<>();
            currentresult.put("time", time);

            List<Case> news = buildDesign();

            Map<String, Object[]> X = loopDesign.initDesign();
            currentresult.put("state", loopDesign.getState());

            allX = new LinkedList<Map<String, Object[]>>();
            allX.add(X);

            for (Case c : news) {
                File d = prj.getCaseResultDir(c);
                if (!d.isDirectory() && !d.mkdirs()) {
                    throw new Exception("Could not create case directory " + d.getAbsolutePath());
                }
                ASCII.saveFile(new File(d, Constants.INPUT_DIR), Format.MapToJSONString(c.getInputValues()));
            }

            Map<String, Object[]> Y = f.F(X);
            allY = new LinkedList<Map<String, Object[]>>();
            allY.add(Y);

            for (Case c : news) {
                File d = prj.getCaseResultDir(c);
                if (!d.exists()) {
                    throw new Exception("Directory not available " + d.getAbsolutePath());
                }
                int[] i = Data.indexOf(Y, c.getInputValues());
                if (i == null || i.length == 0) {
                    throw new Exception("No matching cases.");
                }
                if (i.length > 1) {
                    throw new Exception("Too much matching cases: " + Arrays.toString(i));
                }
                c.setOutputValues(Data.get(Y, i[0]));
                c.setStart((long) Data.get(Y,i[0]).get(Case.PROP_START));
                c.setEnd((long) Data.get(Y,i[0]).get(Case.PROP_END));

                ASCII.saveFile(new File(d, Constants.OUTPUT_DIR), Format.MapToJSONString(c.getOutputValues()));
            }

            currentresult.putAll(addSuffix(X, "." + time));
            currentresult.putAll(addSuffix(Y, "." + time));

            while ((X = loopDesign.nextDesign(Y)) != null && notStopped) {
                time = time + 1;
                currentresult.put("time", time);

                currentresult.put("state", loopDesign.getState());

                allX.add(X);

                news = addExperiments(loopDesign.getNextExperiments());
                for (Case c : news) {
                    File d = prj.getCaseResultDir(c);
                    if (!d.isDirectory() && !d.mkdirs()) {
                        throw new Exception("Could not create case directory " + d);
                    }
                    ASCII.saveFile(new File(d, Constants.INPUT_DIR), Format.MapToJSONString(c.getInputValues()));
                }

                Y = f.F(X);
                allY.add(Y);

                for (Case c : news) {
                    File d = prj.getCaseResultDir(c);
                    if (!d.exists()) {
                        throw new Exception("Directory not available " + d);
                    }
                    int[] i = Data.indexOf(Y, c.getInputValues());
                    if (i == null || i.length == 0) {
                        throw new Exception("No matching cases.");
                    }
                    if (i.length > 1) {
                        throw new Exception("Too much matching cases: " + Arrays.toString(i));
                    }
                    c.setOutputValues(Data.get(Y,i[0]));
                    c.setStart((long) Data.get(Y,i[0]).get(Case.PROP_START));
                    c.setEnd((long) Data.get(Y,i[0]).get(Case.PROP_END));

                    ASCII.saveFile(new File(d, Constants.OUTPUT_DIR), Format.MapToJSONString(c.getOutputValues()));
                }

                currentresult.putAll(addSuffix(X, "." + time));
                currentresult.putAll(addSuffix(Y, "." + time));

                currentresult.putAll(addSuffix2(loopDesign.getResultsTmp(), "." + time));
            }

            time = time + 1;
            currentresult.put("time", time);

            if (notStopped) {
                currentresult.putAll(loopDesign.getResults());
                currentresult.putAll(merge(allX));
                currentresult.putAll(merge(allY));
                state = SHELL_OVER;
            } else state = SHELL_ERROR;

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.err(e, 0);
            currentresult.put("error", e.getMessage());
            Alert.showException(e);
            state = SHELL_EXCEPTION;
            return false;
        }
    }

    @Override
    public boolean stopComputation() {
        notStopped = false;
        return true;
    }

    @Override
    public void designUpdated(int n) {
        Log.out("design updated " + n + " cases", 5);
    }

    public final static String DEFAULT_FUNCTION_NAME = "f";
    
    public static abstract class Function {

        public String[] args;
        int nparallel = 0;
        String fname = DEFAULT_FUNCTION_NAME;

        public Function(String fname, String... xnames) {
            this(fname, 1, xnames);
        }

        public Function(String fname, int nparallel, String... xnames) {
            args = xnames;
            this.fname = fname;
            this.nparallel = nparallel;
            init();
        }

        public void init() {
        } // constructor callback for running issues (create working dir, ...)

        protected abstract Map f(Object... x) throws Exception;

        public Map<String, Object> f(Map<String, Object> x) {
            if (x == null || x.isEmpty()) {
                return null;
            }
            Object[] X = new Object[x.size()];
            Map y = new HashMap();
            for (int i = 0; i < args.length; i++) {
                X[i] = x.get(args[i]);
                y.put(args[i], X[i]);

            }
            try {
                y.putAll(f(X));
            } catch (Exception ex) {
                y.put("exception", ex.getMessage());
            }
            return y;
        }

        public Object[] getArgs(Map<String, Object> x) {
            Object[] xi = new Object[args.length];
            for (int j = 0; j < xi.length; j++) {
                xi[j] = x.get(args[j]);
            }
            return xi;
        }

        public List<Object[]> getAllArgs(Map<String, Object[]> x) {
            List<Object[]> all = new LinkedList<>();
            for (int i = 0; i < x.get(args[0]).length; i++) {
                Object[] xi = new Object[args.length];
                for (int j = 0; j < xi.length; j++) {
                    xi[j] = x.get(args[j])[i];
                }
                all.add(xi);
            }
            return all;
        }

        public Map<String, Object[]> F(final Map<String, Object[]> x) {
            if (x == null || x.isEmpty()) {
                return null;
            }
            final Map[] y = new Map[x.get(args[0]).length];
            final boolean[] end = new boolean[nparallel];

            for (int p = 0; p < nparallel; p++) {
                final int pp = p;
                Thread t = new Thread(new Runnable() {

                    @Override
                    public void run() {

                        for (int i = pp; i < y.length; i = i + nparallel) {
                            Object[] xi = new Object[args.length];
                            y[i] = new HashMap();
                            y[i].put(Case.PROP_CODE, fname);
                            y[i].put(Case.PROP_START, System.currentTimeMillis());
                            for (int j = 0; j < xi.length; j++) {
                                xi[j] = x.get(args[j])[i];
                                y[i].put(args[j], xi[j]);
                            }

                            try {
                                y[i].putAll(f(xi));
                            } catch (Exception ex) {
                                y[i].put("exception", ex.getMessage());
                            }
                            y[i].put(Case.PROP_END, System.currentTimeMillis());
                        }

                        synchronized (end) {
                            end[pp] = true;
                            end.notify();
                        }
                    }
                });
                t.start();
            }

            while (!all(end)) {
                try {
                    synchronized (end) {
                        end.wait();
                    }
                } catch (InterruptedException ex) {
                }
            }

            return mergeArrayMap(y);
        }
    }

    /*public static void main(String[] args) throws Exception {
        Funz_v1.init();
        Funz_v1.setVerbosity(10);
        DesignShell_v1.Function f = new DesignShell_v1.Function(DEFAULT_FUNCTION_NAME,"x1", "x2") {
            @Override
            public Map f(Object... strings) {
                double[] vals = new double[strings.length];
                for (int i = 0; i < vals.length; i++) {
                    vals[i] = Double.parseDouble(strings[i].toString());
                }
                return newMap(DEFAULT_FUNCTION_NAME, DoubleArray.sum(vals));
            }
        };
        HashMap<String, String> _variableBounds = new HashMap<String, String>();
        _variableBounds.put("x1", "[0,1]");
        _variableBounds.put("x2", "[0,1]");
        DesignShell_v1 shell = new DesignShell_v1(f, "GradientDescent", _variableBounds, null);
        shell.setVerbosity(10);
        shell.setDesignOption("nmax", "4");
        shell.setVerbosity(2);
        //Funz_v1.setMathConsoleVisible(true);

        shell.startComputationAndWait();
        System.err.println(ArrayMapToMDString(shell.getResultsStringArrayMap()));
    }*/

}
