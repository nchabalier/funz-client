package org.funz.api;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.funz.Project;
import org.funz.doeplugin.Design;
import org.funz.doeplugin.DesignConstants.Decision;
import org.funz.doeplugin.DesignConstants.Status;
import org.funz.doeplugin.DesignHelper;
import org.funz.doeplugin.DesignPluginsLoader;
import org.funz.doeplugin.DesignSession;
import org.funz.doeplugin.DesignedExperiment;
import org.funz.doeplugin.Experiment;
import org.funz.log.Alert;
import org.funz.parameter.Case;
import org.funz.parameter.OutputFunctionExpression;
import org.funz.parameter.Parameter;
import org.funz.parameter.Variable;
import org.funz.util.ASCII;
import static org.funz.util.Data.*;
import static org.funz.util.Format.ArrayMapToMDString;
import static org.funz.util.Format.MapToMDString;

/**
 * Open API to use Funz (possibly iterative) algorithm. It is intended to be
 * used with following sequence of calls from your DesignShell object:<br/>
 * 1. init() - will return initial points to calculate<br/>
 * 2. next(previous points results) - will return next points to calculate<br/>
 * 2.1. [analyseTmp()] - will return a temporary anlysis of the DoE results<br/>
 * 3. next(previous points results) - will return next points to calculate<br/>
 * 3.1. [analyseTmp()] - will return a temporary anlysis of the DoE results<br/>
 * 4. next(previous points results) - will return next points to calculate<br/>
 * 4.1. [analyseTmp()] - will return a temporary anlysis of the DoE results
 * <br/>
 * .
 * ..<br/>
 * n. next(previous points results) - will return next points to calculate<br/>
 * n.1. [analyse()] - will return a final anlysis of the DoE results <br/>
 *
 * @author Y. Richet
 */
public abstract class LoopDesign_v1 {

    static Funz_v1 pv1 = new Funz_v1();
    Project prj;
    public Design design;
    DesignSession session;
    private boolean cache_experiments = false;
    private File archiveDirectory;
    private Design.Observer observer;
    private Case.Observer cobserver;
    public List<Experiment> initialExperiments;
    public List<Experiment> finishedExperiments = new LinkedList<>();
    public List<ShellExperiment> nextExperiments;
    private String status;

    public LoopDesign_v1(Project prj, File archiveDirectory) {
        this(null, null, prj, archiveDirectory);
    }

    public LoopDesign_v1(Design.Observer observer, Case.Observer cobserver, Project prj, File archiveDirectory) {
        this.prj = prj;
        this.observer = observer;
        this.cobserver = cobserver;
        try {
            setArchiveDirectory(archiveDirectory);
        } catch (IOException ex) {
            err(ex, 0);
        }

        if (prj.getMainOutputFunction() == null) {
            List<OutputFunctionExpression> suggests = prj.getPlugin().suggestOutputFunctions();
            if (suggests != null && !suggests.isEmpty()) {
                prj.setMainOutputFunction(suggests.get(0));
            }
        }

        if (prj.getDesigner() == null) {
            setDesigner(prj.getDesignerId());
        }
        update();
    }

    public void setArchiveDirectory(File ad) throws IOException {
        archiveDirectory = ad;
        if (!archiveDirectory.exists()) {
            FileUtils.forceMkdir(archiveDirectory);
        }
        if (archiveDirectory == null) {
            archiveDirectory = prj.getResultsDir();
        } else {
            prj.setResultsDir(archiveDirectory);
            prj.moveCasesSpoolTo(archiveDirectory, prj.getCases());
        }
    }

    public File getArchiveDirectory() {
        return archiveDirectory;
    }

    public void update() {
        Map lastoptions = null;
        if (prj.getDesigner() != null) {
            lastoptions = prj.getDesigner().getOptions();
        }

        if (prj.getDesigner() == null || (prj.getDesignerId() == null ? prj.getDesigner().getName() != null : !prj.getDesignerId().equals(prj.getDesigner().getName()))) {
            setDesigner(prj.getDesignerId());
            lastoptions = null;
        }

        List<Parameter> params = prj.getContinuousParameters();
        if (params.isEmpty()) {
            for (Variable variable : prj.getVariables()) {
                if (variable.isContinuous()) {
                    params.add(variable);
                }
            }
        }

        prj.getDesigner().setParameters(params);

        prj.getDesigner().setOutputFunction(prj.getMainOutputFunction());

        try {
            if (lastoptions != null) {
                prj.getDesigner().setOptions(lastoptions);
            }
        } catch (Exception ex) {
            err(ex.getLocalizedMessage(), 0);
        }
    }

    void setDesigner(String _design) {
        prj.setDesigner(DesignPluginsLoader.newInstance(_design));
        if (prj.getDesigner() == null) {
            err("Could not instanciate prj.getDesigner() '" + _design + "'", 0);
        }
    }

    public abstract void out(String string, int i);

    public abstract void err(String msg, int i);

    public abstract void err(Exception ex, int i);

    protected Project getProject() {
        return prj;
    }

    public void setVerbosity(int level) {
        Funz_v1.setVerbosity(level);
    }

    Thread stopRun;

    /**
     * Create a 'design of ecperiments' (DoE) project.
     *
     * @param _design name of the design to use. See Shell.DESIGNS static list
     * of available DoE.
     * @param variable_bounds Map of input variables and their model expression
     * : {x1="[0,1]",x2="[-1,1]",...}
     */
    /**
     * @return list of input variables used in the design
     */
    public String[] getInputVariables() {
        List<String> varnames = new ArrayList<String>();
        for (Variable v : prj.getVariables()) {
            if (v.isContinuous()) {
                varnames.add(v.getName());
            }
        }
        return varnames.toArray(new String[varnames.size()]);
    }

    /**
     * Conveniency method to nicely display options of the DoE
     *
     * @return nice string
     */
    public String information() {
        return "Designer " + prj.getDesigner().getName()
                + "\n  * Type: " + prj.getDesigner().getType()
                + "\n  * Return: " + prj.getDesigner().getDesignOutputTitle()
                + "\n  * Options:\n  " + MapToMDString(prj.getDesigner().getOptions()).replace("\n", "\n  ")
                + "\n  * Parameters: " + ASCII.cat(",", prj.getDesigner().getParameters());
    }

    /**
     * Conveniency method to nicely display next batch of experiments to perform
     *
     * @return nice string
     */
    public String nextExperimentsInformation() {
        return ArrayMapToMDString(ExperimentsToMap(nextExperiments), Arrays.asList(prj.getParameterNamesByStorageOrder()));
    }

    public Map<String, Object[]> nextExperimentsMap() {
        return ExperimentsToMap(nextExperiments);
    }

    /**
     * Conveniency method to nicely display all experiments already performed
     *
     * @return nice string
     */
    public String finishedExperimentsInformation() {
        List<String> v = Arrays.asList(prj.getParameterNamesByStorageOrder());
        if (prj.getMainOutputFunction() != null) {
            v.add(prj.getMainOutputFunctionName());
        }
        return ArrayMapToMDString(ExperimentsToMap(finishedExperiments), v);
    }

    public Map<String, Object[]> finishedExperimentsMap() {
        return ExperimentsToMap(finishedExperiments);
    }

    /**
     * Get options of the DoE
     *
     * @return HashMap<String, String> of options
     */
    public Map<String, String> getDesignerOptions() {
        return prj.getDesigner().getOptions();
    }

    /**
     * Set options of the DoE
     *
     * @param options HashMap of options
     */
    public void setDesignerOptions(Map/*<String, String>*/ options) {
        prj.getDesigner().setOptions(options);
    }

    /**
     * Set one option of the DoE
     *
     * @param option name of option
     * @param val value to set
     */
    public void setDesignerOption(String option, String val) {
        prj.getDesigner().setOption(option, val);
    }

    /**
     * Initialization of the DoE workflow.
     *
     * @return HashMap<String, String[]> list of first experiments to be
     * performed
     */
    public Map<String, Object[]> initDesign() {
        return ExperimentsToMap(initialExperiments);
    }

    public void buildDesign(DesignSession session) throws Exception {
        this.session = session;
        setState("Build first iteration");

        try {
            design = prj.getDesigner().createDesign(session);
        } catch (Exception ex) {
            err(ex, 0);
            Alert.showException(ex);
        }
        design.setObserver(observer);

        out("Using:\n" + information(), 1);
        design.init(new File(archiveDirectory, session.getDiscreteCase().getRelativePath()));

        iteration = 1;
        initialExperiments = new LinkedList<Experiment>();
        Status stat = null;
        try {
            stat = design.buildInitialDesign(initialExperiments);
        } catch (Exception e) {
            setState("Exception at first iteration: " + e.getMessage());
            err("Exception at first iteration: " + e.getLocalizedMessage(), 0);
            Alert.showException(e);
            throw new Exception("Exception at first iteration: " + e.getLocalizedMessage());
        }

        if (stat.getDecision() == Decision.ERROR) {
            setState(DESIGN_ERROR + " at first iteration: " + stat.getMessage());
            err(DESIGN_ERROR + " at first iteration: " + stat.getMessage(), 0);
            Alert.showError(DESIGN_ERROR + " at first iteration: " + stat.getMessage());
            throw new Exception(DESIGN_ERROR + " at first iteration: " + stat.getMessage());
        }

        setState("Iteration 1" + (stat.getMessage() != null ? " (" + stat.getMessage() + ")" : ""));
        out("First design: " + stat.getDecision() + ": " + stat.getMessage(), 2);

        nextExperiments = new ArrayList<ShellExperiment>(initialExperiments.size());
        for (Experiment e : initialExperiments) {
            nextExperiments.add(new ShellExperiment(e));
        }

        out(Experiment.toString_ExperimentArray("Initial design", nextExperiments), 1);
    }

    private int iteration = 0;

    public List<Experiment> getFinishedExperiments() {
        return asExperimentsList(finishedExperiments);
    }

    public List<Experiment> getNextExperiments() {
        return asExperimentsList(nextExperiments);
    }

    public List<Experiment> getInitialExperiments() {
        return asExperimentsList(initialExperiments);
    }

    private static List<Experiment> asExperimentsList(List<? extends Experiment> sexps) {
        List<Experiment> exps = new ArrayList<Experiment>(sexps.size());
        exps.addAll(sexps);
        return exps;
    }

    /**
     * Do we use a cache for experiments (to not recalculate already performed
     * experiments) ?
     *
     * @return usage of a cache of experiments
     */
    public boolean cacheExperiments() {
        return cache_experiments;
    }

    /**
     * Set the usage of a cache of experiments
     *
     * @param cache_experiments usage of a cache of experiemnts
     */
    public void setCacheExperiments(boolean cache_experiments) {
        this.cache_experiments = cache_experiments;
    }

    /**
     * @return the iteration
     */
    public int getIteration() {
        return iteration;
    }

    public String getState() {
        return status;
    }

    /**
     * @param status the status to set
     */
    private void setState(String status) {
        this.status = StringUtils.rightPad(status, 80);
    }

    public class ShellExperiment extends DesignedExperiment {

        Experiment parent;
        Map<String, Object> output = new HashMap<String, Object>();

        @Override
        public DesignSession getDesignSession() {
            return session;
        }

        public ShellExperiment(Experiment e) {
            super(e.getInputValues().size(), e.prj);
            parent = e;
        }

        @Override
        public String getValueExpression(int paramIdx) {
            return parent.getValueExpression(paramIdx);
        }

        @Override
        public Map<String, Object> getInputValues() {
            return parent.getInputValues();
        }

        /**
         * No result for default experiences.
         */
        @Override
        public Map<String, Object> getOutputValues() {
            return output;
        }

        public void setOutput(Map<String, Object> y) {
            output = y;
        }
    }

    static Object[] asObjectArray(Double[] a) {
        Object[] oa = new Object[a.length];
        for (int i = 0; i < a.length; i++) {
            oa[i] = a[i];
        }
        return oa;
    }

    static Object[] asObjectArray(double[] a) {
        Object[] oa = new Object[a.length];
        for (int i = 0; i < a.length; i++) {
            oa[i] = a[i];
        }
        return oa;
    }

    static Object[] asObjectArray(String[] a) {
        Object[] oa = new Object[a.length];
        for (int i = 0; i < a.length; i++) {
            oa[i] = a[i];
        }
        return oa;
    }

    /**
     * Perform next design in the DoE workflow.
     *
     * @param Y previous batch results.
     * @return next batch of experiments to perform.
     */
    public Map<String, Object[]> nextDesign(Map<String, ?> Y_tocastarray) throws Exception {
        Map<String, Object[]> Y = new HashMap<>();
        for (String k : Y_tocastarray.keySet()) {
            Object o = Y_tocastarray.get(k);
            if (o instanceof Object[]) {
                Y.put(k, (Object[]) o);
            } else if (o instanceof double[]) {
                Y.put(k, asObjectArray((double[]) o));
            } else if (o instanceof Double[]) {
                Y.put(k, asObjectArray((Double[]) o));
            } else if (o instanceof String[]) {
                Y.put(k, asObjectArray((String[]) o));
            } else {
                throw new IllegalArgumentException("Could not cast " + o + "(" + o.getClass() + ") to Object[]");
            }
        }

        try {
            if (cacheExperiments()) {
                return _nextWithCache(Y);
            } else {
                return _next(Y);
            }
        } catch (Exception e) {
            err(e, 0);
            Alert.showException(e);
            throw new Exception("Exception in nextDesign: " + e.getMessage());
        }
    }

    public static Map<String, Object>[] splitMapArray(Map<String, Object[]> maparray) {
        Set<String> keys = maparray.keySet();
        int n = 0;
        for (String k : keys) {
            n = Math.max(n, maparray.get(k).length);
        }

        Map<String, Object>[] splitmap = new Map[n];
        for (int i = 0; i < n; i++) {
            splitmap[i] = new HashMap<>();
            for (String k : keys) {
                Object[] o = maparray.get(k);
                splitmap[i].put(k, o.length > i ? o[i] : null);
            }
        }
        return splitmap;
    }

    /**
     * Check all values in "in" are also inside "m" and are the same (using
     * equals)
     *
     * @param m Map
     * @param in Map to check as a subset of m
     * @return
     */
    public static boolean equalsIn(Map m, Map in) {
        for (Object k : in.keySet()) {
            if (!m.containsKey(k)) {
                //System.err.println("!containsKey "+k);
                return false;
            }
            if (!m.get(k).equals(in.get(k))) {
                //System.err.println("!equals "+in.get(k).getClass()+" "+m.get(k).getClass());
                return false;
            }
        }
        return true;
    }

    Map<String, Object[]> _next(Map<String, Object[]> Y) throws Exception {
        design.saveNotebook();

        iteration++;
        setState("Iteration " + iteration);

        Map<String, Object>[] Ys = splitMapArray(Y);
        for (ShellExperiment e : nextExperiments) {
            boolean found = false;
            for (int i = 0; i < Ys.length; i++) {
                if (equalsIn(Ys[i], e.getInputValues())) {
                    e.setOutput(Ys[i]);
                    found = true;
                    break;
                }
            }
            if (!found) {
                err("Could not find case " + e.getInputValues() + " in results !", 1);
            }
        }

        finishedExperiments.addAll(nextExperiments);

        List<Experiment> new_exps = new LinkedList<>();
        Status stat = null;

        try {
            stat = design.buildNextDesign(asExperimentsList(finishedExperiments), new_exps);
        } catch (Exception e) {
            setState(DESIGN_EXCEPTION + " at iteration " + iteration + ": " + e.getMessage());
            err(e, 0);
            Alert.showException(e);
            throw new Exception(DESIGN_EXCEPTION + " at " + getIteration() + "th design: " + e.getMessage());
        }

        out("Design " + getIteration() + ": " + stat.getDecision() + (stat.getMessage() != null ? " (" + stat.getMessage() + ")" : ""), 2);

        if (stat.getDecision() == Decision.ERROR) {
            setState(DESIGN_ERROR + " at iteration " + iteration + ": " + stat.getMessage() + "\nY=" + asString(Y));
            err(DESIGN_ERROR + " at " + getIteration() + "th design: " + stat.getMessage() + "\nY=" + asString(Y), 0);
            Alert.showError(DESIGN_ERROR + " at " + getIteration() + "th design: " + stat.getMessage() + "\nY=" + asString(Y));
            throw new Exception(DESIGN_ERROR + " at " + getIteration() + "th design: " + stat.getMessage() + "\nY=" + asString(Y) + "\nExperiments=\n" + asExperimentsList(finishedExperiments));
        }

        if (stat.getDecision() == Decision.DESIGN_OVER) {
            setState(DESIGN_OVER + (stat.getMessage() != null ? " (" + stat.getMessage() + ")" : ""));
            out("Design ended " + (stat.getMessage() != null ? " (" + stat.getMessage() + ")" : ""), 1);
            return null;
        }

        setState("Iteration " + iteration + (stat.getMessage() != null ? " (" + stat.getMessage() + ")" : ""));

        nextExperiments.clear();
        for (Experiment e : new_exps) {
            nextExperiments.add(new ShellExperiment(e));
        }

        out(Experiment.toString_ExperimentArray(getIteration() + "th design", nextExperiments), 1);

        return ExperimentsToMap(new_exps);
    }

    Map<String, Object[]> _nextWithCache(Map<String, Object[]> Y) throws Exception {
        design.saveNotebook();

        iteration++;
        setState("Iteration " + iteration);

        Map<String, Object>[] Ys = splitMapArray(Y);
        // restore output from already cached experiments
        int iY = 0;
        for (int i = 0; i < nextExperiments.size(); i++) {
            boolean found = false;
            for (int j = 0; j < finishedExperiments.size(); j++) {
                if (finishedExperiments.get(j).getInputValues().equals(nextExperiments.get(i).getInputValues())) {
                    out("Using case " + finishedExperiments.get(j) + " in cache", 2);
                    nextExperiments.get(i).setOutput(finishedExperiments.get(j).getOutputValues());
                    found = true;
                    break;
                }
            }
            if (!found) {
                nextExperiments.get(i).setOutput(Ys[iY++]);
            }
        }
        finishedExperiments.addAll(nextExperiments);

        List<Experiment> new_exps = new LinkedList<Experiment>();
        Status stat = null;
        try {
            stat = design.buildNextDesign(asExperimentsList(finishedExperiments), new_exps);
        } catch (Exception e) {
            e.printStackTrace();
            setState(DESIGN_EXCEPTION + " at iteration " + iteration + ": " + e.getMessage());
            err(e, 0);
            throw new Exception(DESIGN_EXCEPTION + " at " + getIteration() + "th design: " + e.getMessage());
        }

        out("Design " + getIteration() + ": " + stat.getDecision() + ": " + stat.getMessage(), 2);

        if (stat.getDecision() == Decision.ERROR) {
            setState(DESIGN_ERROR + " at iteration " + iteration + ": " + stat.getMessage());
            err(DESIGN_ERROR + " at " + getIteration() + "th design: " + stat.getMessage(), 0);
            throw new Exception(DESIGN_ERROR + " at " + getIteration() + "th design: " + stat.getMessage() + "\nY=" + asString(Y) + "\nExperiments=\n" + asExperimentsList(finishedExperiments));
        }

        if (stat.getDecision() == Decision.DESIGN_OVER) {
            setState(DESIGN_OVER + (stat.getMessage() != null ? " (" + stat.getMessage() + ")" : ""));
            out("Design ended: " + stat.getMessage(), 1);
            return null;
        }

        setState("Iteration " + iteration + (stat.getMessage() != null ? " (" + stat.getMessage() + ")" : ""));

        nextExperiments.clear();
        for (Experiment e : new_exps) {
            nextExperiments.add(new ShellExperiment(e));
        }

        // delete already cached experiments
        List<Experiment> clean_new_exps = new LinkedList<Experiment>();
        for (int i = 0; i < new_exps.size(); i++) {
            boolean found = false;
            for (int j = 0; j < finishedExperiments.size(); j++) {
                if (finishedExperiments.get(j).getInputValues().equals(new_exps.get(i).getInputValues())) {
                    out("Selecting case " + finishedExperiments.get(j) + " as cache", 2);
                    found = true;
                    break;
                }
            }
            if (!found) {
                clean_new_exps.add(new_exps.get(i));
            }
        }

        out(Experiment.toString_ExperimentArray(getIteration() + "th design", nextExperiments), 1);

        return ExperimentsToMap(clean_new_exps);
    }
    public static final String DESIGN_OVER = "Design over";
    public static final String DESIGN_ERROR = "Design error";
    public static final String DESIGN_EXCEPTION = "Design exception";

    /**
     * temporary anlysis of the DoE.
     *
     * @return HashMap<String, String> with all conclusion data from the DoE
     * project. The keys of this resultshould include HTML strings, or numerical
     * values.
     */
    public Map<String, String> getResultsTmp() {
        HashMap<String, String> results = new HashMap<>();

        try {
            List<String> data_order = new ArrayList<>(prj.getContinuousParameters().size() + 1);
            data_order.addAll(Arrays.asList(prj.getContinuousParametersNames()));
            if (prj.getMainOutputFunction() != null) {
                data_order.add(prj.getMainOutputFunctionName());
            }
            results.put("experiments", ArrayMapToMDString(ExperimentsToMap(finishedExperiments), data_order));

            String content = design.displayResultsTmp(asExperimentsList(finishedExperiments));

            results.put("analysis", content);
            if (content.startsWith("<")) {
                results.putAll(XMLToMap(content, "analysis"));
            }

            return results;
        } catch (Exception e) {
            results.putAll(newMap("exception", e.getMessage()));
        }
        return results;
    }

    /**
     * Final anlysis of the DoE.
     *
     * @return HashMap<String, String> with all conclusion data from the DoE
     * project. The keys of this resultshould include HTML strings, or numerical
     * values.
     */
    public Map<String, String> getResults() {
        HashMap<String, String> results = new HashMap<String, String>();

        try {
            results.put("information", information());

            results.put("state", status);

            List<String> data_order = new ArrayList<String>(prj.getContinuousParameters().size() + 1);
            data_order.addAll(Arrays.asList(prj.getContinuousParametersNames()));
            if (prj.getMainOutputFunction() != null) {
                data_order.add(prj.getMainOutputFunctionName());
            }
            results.put("experiments", ArrayMapToMDString(ExperimentsToMap(finishedExperiments), data_order));

            String content = design.displayResults(asExperimentsList(finishedExperiments));
            design.saveNotebook();
            session.setAnalysis(content);

            results.put("analysis", content);
            if (content.startsWith("<")) {
                results.putAll(XMLToMap(content, "analysis"));
            }
            return results;
        } catch (Exception e) {
            results.putAll(newMap("exception", e.getMessage()));
        }
        return results;
    }

    Map<String, String> XMLToMap(String xml, String prefix) {
        Map<String, String> map = new HashMap<>();

        int i = 0;
        while (i < xml.length() && i >= 0) {
            int end_type = xml.indexOf(">", i);
            String tag = xml.substring(i + 1, end_type);
            String res_type = tag;
            if (res_type.contains(" ")) {
                res_type = res_type.substring(0, res_type.indexOf(" "));
            }
            String name = tag;
            if (name.contains(" name='")) {
                name = name.substring(name.indexOf(" name='") + 7);
                name = name.substring(0, name.indexOf("'"));
            } else if (name.contains(" name=\"")) {
                name = name.substring(name.indexOf(" name=\"") + 7);
                name = name.substring(0, name.indexOf("\""));
            }
            if (tag.endsWith("/")) {
                //map.put(prefix+"."+name, "");
                i = end_type + 1;
            } else {
                String end_tag = "</" + res_type + ">";
                int end_res = xml.indexOf(end_tag, end_type + 1);
                if (end_res > (end_type + 1)) {
                    map.put(prefix + "." + name, xml.substring(end_type + 1, end_res).replace(DesignHelper.BASE, archiveDirectory.getPath()));
                    i = xml.indexOf("<", end_res + end_tag.length());
                } else {
                    i = xml.length();
                }
            }
        }
        return map;
    }

    public Map<String, Object[]> ExperimentsToMap(List<? extends Experiment> exps) {
        LinkedHashMap<String, Object[]> doe = new LinkedHashMap<>(prj.getDesigner().getParameters().length);
        if (exps == null) {
            return doe;
        }

        for (Parameter param : prj.getDesigner().getParameters()) {
            Object[] values = new Object[exps.size()];
            for (int i = 0; i < exps.size(); i++) {
                Experiment exp = exps.get(i);
                values[i] = /*Utils.toString*/ (exp.getInputValues().get(param.getName()));
            }
            doe.put(param.getName(), values);
        }

        String[] output = null;
        for (int i = 0; i < exps.size(); i++) {
            Experiment exp = exps.get(i);
            if (exp.getOutputValues() != null && !exp.getOutputValues().isEmpty()) {
                if (output == null) {
                    output = new String[exps.size()];
                }
                output[i] = Utils.toString(exp.getOutputValues().get(prj.getMainOutputFunctionName()));
            }
        }
        if (output != null) {
            doe.put(prj.getMainOutputFunctionName(), output);
        }

        return doe;
    }

    /*public static void main(String[] args) {
        Funz_v1.init();

        Observer o = new Case.Observer() {

            @Override
            public void caseModified(int index, int what) {
                System.err.println("caseModified " + index + " " + what);
            }
        };

        Design.Observer _o = new Design.Observer() {

            @Override
            public void designUpdated(int n) {
                System.err.println("designUpdated " + n);
            }

        };

        DesignShell_v1.Function f = new DesignShell_v1.Function(DEFAULT_FUNCTION_NAME, "x1", "x2") {
            @Override
            public Map f(Object... strings) {
                double[] vals = new double[strings.length];
                for (int i = 0; i < vals.length; i++) {
                    vals[i] = Double.parseDouble(strings[i].toString());
                }
                return newMap(DEFAULT_FUNCTION_NAME, DoubleArray.sum(vals));
            }
        };

        Project prj = new Project("test");
        prj.setPlugin(new ExtendedIOPlugin());
        prj.setMainOutputFunction(new OutputFunctionExpression.Numeric(DEFAULT_FUNCTION_NAME));
        prj.setDesignerId("GradientDescent");

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

        LoopDesign_v1 loopDesign = new LoopDesign_v1(_o, o, prj, new File("tmp")) {

            @Override
            public void out(String string, int i) {
                System.out.println(i + ": " + string);
            }

            @Override
            public void err(String msg, int i) {
                System.err.println(i + "! " + msg);
            }

            @Override
            public void err(Exception ex, int i) {
                System.err.println(i + "! " + ex.getLocalizedMessage());
            }
        };

        loopDesign.setDesignerOption("nmax", "10");
        loopDesign.update();
        try {
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
        } catch (Exception e) {
        }
        System.err.println(loopDesign.getResults());
    }*/
}
