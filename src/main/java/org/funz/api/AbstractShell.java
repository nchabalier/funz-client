package org.funz.api;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.funz.Project;
import org.funz.ProjectController;
import static org.funz.api.DesignShell_v1.DEFAULT_FUNCTION_NAME;
import org.funz.conf.Configuration;
import org.funz.doeplugin.Design;
import org.funz.doeplugin.DesignConstants;
import org.funz.doeplugin.DesignPluginsLoader;
import org.funz.doeplugin.DesignSession;
import org.funz.doeplugin.Designer;
import org.funz.doeplugin.DesignerInterface;
import org.funz.doeplugin.Experiment;
import org.funz.ioplugin.IOPluginInterface;
import org.funz.ioplugin.IOPluginsLoader;
import org.funz.log.Log;
import org.funz.log.LogFile;
import org.funz.parameter.Case;
import org.funz.parameter.OutputFunctionExpression;
import org.funz.parameter.Parameter;
import org.funz.parameter.VarGroup;
import org.funz.parameter.Variable;
import org.funz.parameter.VariableMethods;
import org.funz.results.RendererHelper;
import org.funz.script.RMathExpression;
import static org.funz.util.Data.asString;

import org.funz.util.ASCII;
import org.funz.util.Disk;
import org.funz.util.Format.XML;

/**
 *
 * @author richet
 */
public abstract class AbstractShell implements UnifiedShell, Case.Observer {

    File[] input;
    public Project prj;
    protected File directory;

    public void setProject(Project prj) {
        this.prj = prj;
        prj.setShell(this);
    }

    public AbstractShell() {
        Funz.addShell(this);
    }

    @Override
    public Map<String, String[]> getCalculatorsInformation() {
        return org.funz.api.Funz_v1.getCalculatorsInformation();
    }

    @Override
    public String[] getModelList() {
        return Funz_v1.getModelList();
    }

    @Override
    public String[] getDesignList() {
        return Funz_v1.getDesignList();
    }

    @Override
    public void setVerbosity(int level) {
        Funz_v1.setVerbosity(level);
    }

    protected Project getProject() {
        return prj;
    }

    @Override
    public String[] getProjectPropertyKeys() {
        List<String> props = new ArrayList<>();
        Field[] fs = prj.getClass().getFields();
        for (Field field : fs) {
            try {
                if (Modifier.isPublic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers()) && !Modifier.isStatic(field.getModifiers())) {
                    if (field.getType().equals(Integer.TYPE) || field.getType().equals(Double.TYPE) || field.getType().equals(Long.TYPE) || field.getType().equals(Boolean.TYPE) || field.getType().equals(String.class)) {
                        props.add(field.getName());
                    } else {
                        Log.err("Type " + field.getType() + " not supported.", 2);
                    }
                }
            } catch (IllegalArgumentException ex) {
                Log.err(ex, 2);
            }
        }
        return props.toArray(new String[props.size()]);
    }

    @Override
    public String getProjectProperty(String property) {
        Field[] fs = prj.getClass().getFields();
        List<Field> fs_nostatic = new LinkedList<>();
        for (Field field : Arrays.asList(fs)) {
            if (Modifier.isPublic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers()) && !Modifier.isStatic(field.getModifiers()))
                fs_nostatic.add(field);
        }
        for (Field field : fs_nostatic) {
            if (field.getName().equals(property)) {
                try {
                    if (field.getType().equals(Integer.TYPE)) {
                        return "" + field.getInt(this);
                    } else if (field.getType().equals(Double.TYPE)) {
                        return "" + field.getDouble(this);
                    } else if (field.getType().equals(Long.TYPE)) {
                        return "" + field.getLong(this);
                    } else if (field.getType().equals(Boolean.TYPE)) {
                        return "" + field.getBoolean(this);
                    } else if (field.getType().equals(String.class)) {
                        return field.get(this).toString();
                    } else {
                        throw new IllegalArgumentException("Type " + field.getType() + " not supported.");
                    }
                } catch (IllegalArgumentException ex) {
                    Log.err(ex, 2);
                } catch (IllegalAccessException ex) {
                    Log.err(ex, 3);
                }
            }
        }
        throw new IllegalArgumentException("Property " + property + " not supported.");
    }

    @Override
    public void setProjectProperty(String property, String value) {
        Field[] fs = prj.getClass().getFields();
        List<Field> fs_nostatic = new LinkedList<>();
        for (Field field : Arrays.asList(fs)) {
            if (Modifier.isPublic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers()) && !Modifier.isStatic(field.getModifiers()))
                fs_nostatic.add(field);
        }
        for (Field field : fs_nostatic) {
            if (field.getName().equals(property)) {
                try {
                    Object cast_value = null;
                    if (field.getType().equals(Integer.TYPE)) {
                        cast_value = Integer.parseInt(value);
                    } else if (field.getType().equals(Double.TYPE)) {
                        cast_value = Double.parseDouble(value);
                    } else if (field.getType().equals(Long.TYPE)) {
                        cast_value = Long.parseLong(value);
                    } else if (field.getType().equals(Boolean.TYPE)) {
                        cast_value = Boolean.parseBoolean(value);
                    } else if (field.getType().equals(String.class)) {
                        cast_value = value;
                    } else {
                        throw new IllegalArgumentException("Type " + field.getType() + " not supported.");
                    }
                    field.set(prj, cast_value);
                    return;
                } catch (IllegalArgumentException ex) {
                    Log.err(ex, 2);
                } catch (IllegalAccessException ex) {
                    Log.err(ex, 3);
                }
            }
        }
        Log.err(("Property "+property+" not supported (in "+Arrays.asList(fs)+").").replace("org.funz.Project.", "").replace("public", ""), 2);
        throw new IllegalArgumentException(("Property "+property+" not supported (in "+Arrays.asList(fs)+").").replace("org.funz.Project.", "").replace("public", ""));
    }

    @Override
    public void setInputModel(String _model, File... _input) throws Exception {
        this.input = _input;
        if (input == null || input.length == 0) {
            Log.err("No input file given.", 1);
        }

        String model;
        if (_model == null || _model.length() == 0) {
            IOPluginInterface iop = IOPluginsLoader.getFirstSuitablePlugin(input[0]);
            if (iop == null) {
                Log.err("Could not automatically select a plugin for " + input[0] + " input file.", 0);
                throw new Exception("Could not automatically select a plugin for " + input[0] + " input file.");
            }
            model = Configuration.getModel(iop);
            if (model == null) {
                Log.err("Could not automatically select a model for " + input[0] + " input file.", 0);
                throw new Exception("Could not automatically select a model for " + input[0] + " input file.");
            } else {
                Log.out("Automatic selection of model " + model, 0);
            }
        } else {
            model = _model;
        }

        String name = input[0].getName() + "_" + Configuration.timeDigest() + "_" + hashCode();
        IOPluginInterface plugin = IOPluginsLoader.newInstance(model, input);
        Log.out("Using model " + model + " by plugin " + plugin, 0);

        if (prj == null) {
            prj = ProjectController.createProject(name, input[0], model, plugin);
        } else {
            ProjectController.setupProject(prj, name, input[0], model, plugin);
        }
        plugin.setFormulaInterpreter(new RMathExpression(input[0].getName() + "_" + Configuration.timeDigest(), Configuration.isLog("R") ? new File(prj.getLogDir(), name + ".Rlog") : null));

        if (input.length > 1) {
            for (int i = 1; i < input.length; i++) {
                if (input[i].exists()) {
                    Log.err("Input file/dir " + input[i].getName() + " does not exist.", 2);
                }
                try {
                    prj.importFileOrDir(input[i]);
                } catch (Exception ex) {
                    Log.err(ex, 0);
                }
            }
            prj.buildParameterList();
            try {
                prj.saveInSpool();
            } catch (Exception ex) {
                Log.err(ex, 0);
            }
        }
    }

    @Override
    public String getModel() {
        return prj.getModel();
    }

    @Override
    public String getDesigner() {
        return prj.getDesignerId();
    }

    @Override
    public void setDesigner(String designName) {
        if (designName == null || designName.length() == 0 || designName == Designer.NODESIGNER_ID) {
            prj.setDesigner(null);
        } else {
            prj.setDesignerId(designName);
            prj.setDesigner(DesignPluginsLoader.newInstance(designName));
            if (prj.getDesigner() == null) {
                Log.err("Could not instanciate prj.getDesigner() '" + designName + "'", 0);
            }
        }
    }

    protected Map _designOptions = new HashMap(); // this is temp storage to avoid mod by LoopDesign. Will override initialisation values of LoopDesign

    public void setDesignOptions(Map designOptions) {
        if (designOptions == null) {
            _designOptions = new HashMap();
        } else {
            this._designOptions = designOptions;
        }
        prj.setDesignOptions(prj.getDesignerId(), designOptions);
    }

    @Override
    public void setDesignOption(String key, String val) {
        this._designOptions.put(key, val);
        if (prj.getDesignOptions(prj.getDesignerId()) == null) prj.setDesignOptions(prj.getDesignerId(),new HashMap());
        prj.getDesignOptions(prj.getDesignerId()).put(key, val);
    }

    @Override
    public File[] getInputFiles() {
        return input;
    }

    /**
     * Provide information about available input.
     *
     * @return List of variables available in the input files with given
     * code/model.
     */
    @Override
    public String[] getInputVariables() {
        List<Variable> vars = prj.getVariables();
        List<String> vars_str = new ArrayList<>(vars.size());
        for (Variable v : vars) {
            vars_str.add(v.getName());
        }
        return vars_str.toArray(new String[vars_str.size()]);
    }

    public void setInputVariablesFactorial(Map/*<String, String[]>*/ var_values) throws Exception {
        for (Object var : var_values.keySet()) {
            if (var.toString().contains(",")) {
                setInputVariablesGroup(var.toString(), Project.splitGroupedVar(var.toString(), (String[]) var_values.get(var)));
            } else {
                setInputVariable(var.toString(), var_values.get(var));
            }
        }
    }

    @Override
    public void setInputVariableProperty(String varName, String property, String value) {
        for (Variable v : prj.getVariables()) {
            if (v.getName().equals(varName)) {
                if (property.equals("type")) {
                    v.setType(value);
                } else {
                    throw new IllegalArgumentException("Property " + property + "not supported");
                }
            }
        }
    }

    /**
     * Define input sample.
     *
     * @param var_values Combinations of variables (as String keys) and their
     * values (as String[]) to compute.
     * @param group True means that all multiple var_values values are
     * synchronized with others. False means that number of experiments is the
     * product of all values sizes.
     * @throws Exception
     */
    void buildParameters() {
        // first, complete undefined variables
        LinkedList<Variable> vars = prj.getVariables();
        for (Variable v : vars) {
            if (v.getNmOfValues() == 0) {
                if (v.getDefaultModel() != null && v.getDefaultModel().contains("{")) {
                    v.setDefaultModel(v.getDefaultModel(), true);
                    continue;
                } else if (v.getDefaultValue() != null) {
                    LinkedList<VariableMethods.Value> vals = new LinkedList<>();
                    vals.add(new VariableMethods.Value(v.getDefaultValue()));
                    v.setValues(vals);
                } else {
                    LinkedList<VariableMethods.Value> vals = new LinkedList<>();
                    vals.add(new VariableMethods.Value(""));
                    v.setValues(vals);
                }
            }

            if (v.getName().contains(".") && v.getGroup() == null) {
                String prefix = v.getName().substring(0, v.getName().indexOf("."));
                VarGroup g = prj.getGroupByName(prefix);
                if (g == null) {
                    g = new VarGroup(prefix);
                    prj.addGroup(g);
                }
                g.addVariable(v);
                v.setGroup(g);
            }
        }

        prj.buildParameterList();

        //System.err.println(prj.getVariables());
        //System.err.println(prj.getVariableAt(0).toInfoString());
        //System.err.println(prj.getCases());
    }

    public String[] getUndefinedInputVariables() {
        List<Variable> vars = prj.getVariables();
        List<String> vars_str = new ArrayList<String>(vars.size());
        for (Variable v : vars) {
            if (v.getValues().size() == 0) {
                vars_str.add(v.getName());
            }
        }
        return vars_str.toArray(new String[vars_str.size()]);
    }

    public Map<String, String[]> getInputDesign() {
        Map<String, String[]> cases = new HashMap<String, String[]>();
        for (Variable v : prj.getVariables()) {
            cases.put(v.getName(), new String[prj.getCases().size()]);
        }
        for (Case c : prj.getCases()) {
            for (Variable v : prj.getVariables()) {
                cases.get(v.getName())[c.getIndex()] = c.getValueExpression(v.getIndex());
            }
        }
        return cases;
    }

    /**
     * Define output
     *
     * @param custom_outs Math. expressions of output of interest to compute.
     * Possibly including input variables.
     */
    public void setOutputExpressions(String... custom_outs) {
        prj.getOutputFunctionsList().clear();

        if (custom_outs != null && custom_outs.length == 1 && custom_outs[0] == null) {
            custom_outs = null;
        }

        if (custom_outs == null || custom_outs.length == 0) {
            LinkedList<OutputFunctionExpression> outs = prj.getPlugin().suggestOutputFunctions();
            if (outs != null && outs.size() > 0) {
                for (int i = outs.size() - 1; i >= 0; i--) {
                    prj.setMainOutputFunction(outs.get(i));
                }
            } else {
                prj.setMainOutputFunction(new OutputFunctionExpression.Numeric("1"));
            }
        } else /*if (custom_outs != null && custom_outs.length > 0)*/ {
            for (int i = custom_outs.length - 1; i >= 0; i--) {
                if (custom_outs[i] != null) {
                    prj.setMainOutputFunction(OutputFunctionExpression.read(custom_outs[i]));
                }
            }
        }
    }

    /**
     * Provide information about available output.
     *
     * @return List of available output from input files and given code/model.
     */
    public String[] getOutputAvailable() {
        return prj.getPlugin().getOutputNames();
    }

    public String[] getOutputExpressions() {
        String[] oes = new String[prj.getOutputFunctionsList().size()];
        for (int i = 0; i < oes.length; i++) {
            oes[i] = prj.getOutputFunctionsList().get(i).toNiceSymbolicString();
        }
        return oes;
    }

    public Map<String, String> getCalculationPointsStatus() {
        Map<String, String> status = new HashMap<String, String>(prj.getCases().size());
        for (Case c : prj.getCases()) {
            status.put(c.getName(), c.getStateString());
        }
        return status;
    }

    public String getCalculationPointsStatus(String calculationPointID) {
        return getCalculationPointsStatus().get(calculationPointID);
    }

    public Map<String, String> askCalculationPointsProgress() {
        Map<String, String> status = new HashMap<String, String>(prj.getCases().size());
        for (Case c : prj.getCases()) {
            status.put(c.getName(), asString(c.getStatusInformation()));
        }
        return status;
    }

    public String askCalculationPointProgress(String calculationPointID) {
        return askCalculationPointsProgress().get(calculationPointID);
    }

    public Object getCalculationPointProperty(String calculationPointID, String propertyName) {
        for (Case c : prj.getCases()) {
            if (c.getName().equals(calculationPointID)) {
                String methods = "";
                for (Method m : c.getClass().getMethods()) {
                    methods += "," + m.getName();
                    if (m.getName().equals("get" + propertyName) & m.getParameterTypes().length == 0) {
                        try {
                            return m.invoke(c);
                        } catch (Exception ex) {
                            return ex;
                        }
                    }
                }
                return "Property " + propertyName + " not found. (Available: " + methods.replace("get", "") + ")";
            }
        }
        return "Case " + calculationPointID + " not found.";
    }

    public Map<String, Object> getCalculationPointProperties(String calculationPointID) {
        Map<String, Object> props = new HashMap<String, Object>(prj.getCases().size());
        for (Case c : prj.getCases()) {
            if (c.getName().equals(calculationPointID)) {
                String methods = "";
                for (Method m : c.getClass().getMethods()) {
                    methods += "," + m.getName();
                    if (m.getName().startsWith("get") & m.getParameterTypes().length == 0) {
                        try {
                            props.put(m.getName().substring(2), m.invoke(c));
                        } catch (Exception ex) {
                            props.put(m.getName().substring(2), null);
                        }
                    }
                }
                return props;

            }
        }
        return null;
    }

    public Map<String, String> getCalculationPointData(String calculationPointID) {
        for (Case c : prj.getCases()) {
            if (c.getName().equals(calculationPointID)) {
                Map<String, String[]> resarray = getResultsStringArrayMap();
                Map<String, String> res = new HashMap<String, String>();
                for (String r : resarray.keySet()) {
                    res.put(r, resarray.get(r)[c.getIndex()]);
                }
                return res;
            }
        }
        return null;
    }

    public String getCalculationPointData(String calculationPointID, String resultName) {
        return getCalculationPointData(calculationPointID).get(resultName);
    }

    public String[] getCalculationPointsNames() {
        String[] names = new String[prj.getCases().size()];
        for (Case c : prj.getCases()) {
            names[c.getIndex()] = c.getName();
        }
        return names;
    }

    @Override
    public String getState() {
        return state;
    }

    public abstract File getCalculationPointContent(String calculationPointId);

    /**
     * Define default input sample using default var values/model
     *
     * @param group True means that all multiple var_values values are
     * synchronized with others. False means that number of experiments is the
     * product of all values sizes.
     * @throws Exception
     */
    public void setDefaultInput() throws Exception {
        //setInputVariablesGroup((Map) null);
        buildParameters();
    }

    public final static String SHELL_NOTSTARTED = "Not started.", SHELL_RUNNING = "Running...", SHELL_OVER = "Over.", SHELL_ERROR = "Failed!", SHELL_EXCEPTION = "Exception!!";
    protected volatile String state = SHELL_NOTSTARTED;

    /**
     * If project is correctly and enough set, launch calculation process.
     * FunzV2: client must be connected.
     *
     * @return if computation started correctly.
     */
    @Override
    public boolean startComputation() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                state = SHELL_RUNNING;
                if (startComputationAndWait()) {
                    state = SHELL_OVER;
                } else {
                    state = SHELL_ERROR;
                }
            }
        }).start();
        return true;
    }

    @Override
    public Object[] getResultsArray(String dataName) {
        return getResultsArrayMap().get(dataName);
    }

    @Override
    public String[] getResultsStringArray(String dataName) {
        return getResultsStringArrayMap().get(dataName);
    }

    public abstract boolean startComputationAndWait();

    @Override
    public abstract boolean stopComputation();

    public void copyResultsIn(File dir, String prefix) throws IOException {
        saveResults();

        Log.out("Copying results in " + dir + "...", 3);

        Disk.copyFile(new File(getArchiveDirectory(), Project.CASE_LIST_FILE), new File(dir, prefix + "." + Project.CASE_LIST_FILE));

        if (!prj.getDesignerId().equals(DesignConstants.NODESIGNER_ID)) {
            Disk.copyFile(new File(getArchiveDirectory(), Project.DESIGN_SESSIONS_FILE), new File(dir, prefix + "." + Project.DESIGN_SESSIONS_FILE));
        }

        Disk.copyFile(new File(getArchiveDirectory(), Project.REPORT_FILE), new File(dir, prefix + "." + Project.REPORT_FILE));
    }

    public void saveResults() {
        if (prj==null) {Log.err("Cannot write results:  project null",3); return;}
        Log.out("Writing results...", 3);
        ProjectController.RendererParamHolder rparams = new ProjectController.RendererParamHolder();
        ProjectController.prepareRenderingDiscreteParams(prj, rparams);
        prj.saveCases(new File(getArchiveDirectory(), Project.CASE_LIST_FILE));
        
        if (!prj.getDesignerId().equals(DesignConstants.NODESIGNER_ID)) {

            OutputFunctionExpression anything = RendererHelper.buildAnythingOutputFunctionExpression(prj);

            for (int discCaseIdx = 0; discCaseIdx < rparams.values.size(); discCaseIdx++) {
                DesignSession ds = prj.getDesignSession(discCaseIdx);
                try {
                    List<Case> goodcases = ProjectController.getContinousCases(prj, discCaseIdx);
                    ArrayList<Experiment> goodexps = new ArrayList<Experiment>(goodcases.size());
                    for (Case ca : goodcases) {
                        Map<String, Object> outputValues = ProjectController.getCaseOutputs(prj, ca.getIndex(), false);
                        ca.setOutputValues(outputValues);
                        goodexps.add(ca.getContinuousExperiment());
                    }

                    Designer d = prj.getDesigner();
                    assert d != null : "Project designer not set!";

                    Design design = prj.getDesign(discCaseIdx);
                    assert design != null : "Project design not set!";
                    //design.init(prj.getDisCaseResultDir(discCaseIdx));

                    String res = ds.getAnalysis();

                    ArrayList<LinkedList<String>> anythings = new ArrayList<LinkedList<String>>();
                    for (int i = 0; i < rparams.values.size(); i++) {
                        anythings.add(new LinkedList<String>());
                    }
                    for (Case ca : goodcases) {
                        Map<String, Object> outputValues = ProjectController.getCaseOutputs(prj, ca.getIndex(), false);
                        Map<String, Object> inputValues = ProjectController.getCaseInputs(prj, ca.getIndex());
                        Map<String, Object> interValues = ProjectController.getCaseIntermediates(prj, ca.getIndex());
                        anythings.get(discCaseIdx).add(anything.getResultRendererData(prj.getPlugin().getFormulaInterpreter(), outputValues, inputValues, interValues));
                    }
                    if (res == null) {
                        ds.setAnalysis(XML.merge(RendererHelper.ARRAY_SEP, anythings.get(discCaseIdx)));
                    } else {
                        StringBuilder format_res = new StringBuilder();
                        format_res.append(RendererHelper.tryHTMLize(res, d.getDesignOutputTitle())).append(XML.merge(RendererHelper.ARRAY_SEP, anythings.get(discCaseIdx)));
                        ds.setAnalysis(format_res.toString());
                    }
                } catch (Exception e) {
                    Log.err(e, 2);
                    if (Log.level >= 10) e.printStackTrace();
                    ds.setAnalysis(e.getMessage());
                }
            }
            prj.saveDesignSessions(new File(getArchiveDirectory(), Project.DESIGN_SESSIONS_FILE));
        }
        Log.out("Results written.", 3);
        prj.saveReport(new File(getArchiveDirectory(), Project.REPORT_FILE));
    }

    public void shutdown() {
        Log.out("Ask for shutdown.", 3);

        saveResults();

        if (prj != null && prj.getPlugin() != null && prj.getPlugin().getFormulaInterpreter() != null) {
            if (prj.getPlugin().getFormulaInterpreter() instanceof RMathExpression) {
                Log.out("Shutdown R interpreter", 3);
                ((RMathExpression) prj.getPlugin().getFormulaInterpreter()).finalizeRsession();
            }
            Log.out("Destroy formula interpreter", 3);
            prj.getPlugin().setFormulaInterpreter(null);
        }

        Log.out("Shutdown project", 3);
        prj = null;

        System.gc();
    }

    public File defaultArchiveDirectory() {
        if (input == null || input.length == 0) {
            return new File(".");
        }
        return new File(input[0].getParent(), prj.getName() + "." + Project.RESULTS_DIR);
    }

    public void setArchiveDirectory(File directory) {
        if (directory == null) {
            this.directory = defaultArchiveDirectory();
        } else {
            this.directory = directory;
        }
    }

    public File getArchiveDirectory() {
        if (this.directory == null) {
            return null;
        }
        if (this.directory.getParentFile() != null && (!this.directory.getParentFile().isDirectory() || !this.directory.getParentFile().canWrite())) {
            this.directory.getParentFile().mkdirs();
        }
        if (!this.directory.isDirectory()) {
            this.directory.mkdirs();
        }

        return this.directory;
    }

    public void setArchiveDirectory(String directory) {
        if (directory == null) {
            setArchiveDirectory(defaultArchiveDirectory());
        } else {
            setArchiveDirectory(new File(directory));
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    public static Map DesignOptions(String designer) throws Exception {
        return DesignOptions(designer, new String[]{"x"}, DEFAULT_FUNCTION_NAME);
    }

    public static Map DesignOptions(String designer, String[] variables, String output) throws Exception {
        for (DesignerInterface d : DesignPluginsLoader.doeplugins) {
            if (d.getName().equals(designer)) {
                Parameter[] params = null;
                if (variables != null) {
                    params = new Parameter[variables.length];
                    for (int i = 0; i < params.length; i++) {
                        params[i] = new Variable(null, variables[i]);
                        ((Variable) params[i]).setLowerBound(0);
                        ((Variable) params[i]).setUpperBound(1);
                    }
                    d.setParameters(params);
                }

                OutputFunctionExpression f = null;
                if (output != null) {
                    if (output.contains(OutputFunctionExpression.TYPE_DELIMITER)) {
                        f = OutputFunctionExpression.read(output);
                    } else {
                        f = OutputFunctionExpression.read(OutputFunctionExpression.Numeric.class.getSimpleName() + OutputFunctionExpression.TYPE_DELIMITER + output);
                    }

                    d.setOutputFunction(f);
                }

                if (f != null && params != null) {
                    String msg = d.isValid(Arrays.asList(params), f);
                    if (!msg.equals(Designer.VALID)) {
                        throw new IllegalArgumentException("Failed to instanciate prj.getDesigner(): " + designer + ", with params=" + Arrays.asList(params) + ", output=" + f.toString() + ":  \n" + msg);
                    }
                }

                return d.getOptions();
            }
        }
        //Log.err("Unknonw prj.getDesigner(): " + designer, 2);
        return null;
    }

    public static boolean any(boolean[] values) {
        for (int i = 0; i < values.length; i++) {
            if (values[i]) {
                return true;
            }
        }
        return false;
    }

    public static boolean all(boolean[] values) {
        for (int i = 0; i < values.length; i++) {
            if (!values[i]) {
                return false;
            }
        }
        return true;
    }

    public static List<String> notused(String[] expressions, List<String> vars) {
        //System.err.println("expressions: "+Arrays.toString(expressions));
        //System.err.println("vars: "+vars);
        if (expressions==null || expressions.length==0) return vars;
        List<String> used = new LinkedList();
        for (String var : vars) {
            for (String expr : expressions) {
                if (expr.contains(var)) {
                    used.add(var);
                    break;
                }
            }
        }
        //System.err.println("Used "+used+" from "+vars+" in "+Arrays.toString(expressions));
        vars.removeAll(used);
        return vars;
    }

    public void setProjectDir(File dir) throws IOException {
        Log.out("Moving project in " + dir, 0);
        File prjdir = dir;
        if (prjdir.exists()) {
            Disk.copyDir(prj.getRootDirectory(), prjdir);
            Disk.removeDir(prj.getRootDirectory());
            prj.setDirectory(prjdir);
        } else {
            Disk.moveDir(prj.getRootDirectory(), prjdir);
            prj.setDirectory(prjdir);
        }
    }

    public void redirectOutErr() throws FileNotFoundException {
        redirectOutErr(new File(getArchiveDirectory(), input[0].getName() + ".out"));
    }

    public void redirectOutErr(File f) throws FileNotFoundException {
        Log.setCollector(new LogFile(f));
    }

}
