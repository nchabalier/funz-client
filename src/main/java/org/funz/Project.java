package org.funz;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.io.FileUtils;
import static org.funz.Constants.*;
import static org.funz.XMLConstants.*;
import org.funz.api.AbstractShell;
import org.funz.api.Funz_v1;
import org.funz.conf.Configuration;
import org.funz.doeplugin.Design;
import org.funz.doeplugin.DesignConstants;
import org.funz.doeplugin.DesignPluginsLoader;
import org.funz.doeplugin.DesignSession;
import org.funz.doeplugin.Designer;
import org.funz.doeplugin.Experiment;
import org.funz.ioplugin.IOPluginInterface;
import org.funz.ioplugin.IOPluginsLoader;
import org.funz.log.Alert;
import org.funz.log.Log;
import org.funz.log.LogCollector.SeverityLevel;
import org.funz.parameter.*;
import org.funz.parameter.VariableMethods.BadSyntaxException;
import org.funz.parameter.VariableMethods.ParseEvalException;
import org.funz.script.MathExpression;
import org.funz.util.ASCII;
import org.funz.util.Disk;
import org.funz.util.Format;

import static org.funz.util.Format.fromHTML;
import static org.funz.util.Format.toHTML;
import static org.funz.util.ParserUtils.ASCIIFilesAreIdentical;
import static org.funz.util.ParserUtils.getASCIIFileContent;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Project is responsible for parameters control and save/load operations. It
 * keeps a list of files, a list variables and their groups. There are two case
 * lists: _disCases - cases from discrete variables and _cases - all cases. If
 * there are both disc and cont variables each case in _cases has a discrete
 * case index in _disCases because all continuous combinations are erpeted for
 * each discrete case.
 */
public class Project {

    @Override
    public String toString() {
        return getName();
    }
    public static int DEFAULT_waitingTimeout = 10 * 60;
    public static boolean DEFAULT_useCache = true;
    public static String DEFAULT_archiveFilter = "(.*)";
    public static int DEFAULT_retries = 3;
    public static long DEFAULT_minMemory = 0L;
    public static long DEFAULT_minDisk = 0L;
    public static double DEFAULT_minCPU = 0;
    public static String DEFAULT_regexpCalculators = null;
    public static boolean DEFAULT_patchInputWhenFailed = false;
    public static long DEFAULT_blacklistTimeout = 60;

    public long blacklistTimeout = DEFAULT_blacklistTimeout;
    public int waitingTimeout = DEFAULT_waitingTimeout;
    public boolean useCache = DEFAULT_useCache;
    public int maxCalcs = Configuration.defaultCalcs();
    public String archiveFilter = DEFAULT_archiveFilter;
    public int retries = DEFAULT_retries;
    public long minMemory = DEFAULT_minMemory;
    public long minDisk = DEFAULT_minDisk;
    public double minCPU = DEFAULT_minCPU;
    public String regexpCalculators = DEFAULT_regexpCalculators;
    public boolean patchInputWhenFailed = DEFAULT_patchInputWhenFailed;
    private AbstractShell shell;

    /**
     * @return the shell
     */
    public AbstractShell getShell() {
        return shell;
    }

    /**
     * @param shell the shell to set
     */
    public void setShell(AbstractShell shell) {
        this.shell = shell;
    }

    public int getMaxRetry() {
        return retries;
    }

    public boolean allParametersAreDiscrete() {
        for (Parameter p : _params) {
            if (p.isContinuous()) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return the maxCalcs
     */
    public int getMaxCalcs() {
        return maxCalcs;
    }

    /**
     * @param maxCalcs the maxCalcs to set
     */
    public void setMaxCalcs(int maxCalcs) {
        this.maxCalcs = maxCalcs;
    }

    /**
     * @param maxCalcs the maxCalcs to set
     */
    public void setMaxRetries(int retries) {
        this.retries = retries;
    }

    public String getArchiveFilter() {
        return archiveFilter;
    }

    public void setArchiveFilter(String filter) {
        this.archiveFilter = filter;
    }
    
    public void setBlacklistTimeout(int seconds) {
        blacklistTimeout = seconds;
    }

    public long getBlacklistTimeout() {
        return blacklistTimeout;
    }

    /* too much dangerous : InputFile are not updated nor checked, so when calling Project.cleanParameters, var will disappear...
     public void replaceVariable(Variable x) {
     Variable x_old = getVariableByName(x.getName());
     if (x_old == null) {
     Alert.showError("Cannot find existing " + x + " variable.");
     } else {
     _vars.remove(x_old);
     for (Iterator it = _groups.iterator(); it.hasNext();) {
     ((VarGroup) it.next()).removeVariable(x_old);
     }
     removeParameter(x_old);
     _vars.add(x);
     buildParameterList();
     }
     }*/
    /**
     * Project modification callback interface.
     */
    public static interface Listener {

        /**
         * Invoked each time the project is modified.
         */
        public void projectModified(Project p);

        /**
         * Invoked each time the project is saved.
         */
        public void projectSaved(Project p);
    }

    /**
     * Parameter iist modification callback interface.
     */
    public static interface ParameterListListener {

        /**
         * Invoked each time the parameter list is modified
         */
        public void parameterListModified();
    }
    public static final int MV_PARAM_TOP = 0, MV_PARAM_UP = 1, MV_PARAM_DOWN = 2, MV_PARAM_BOTTOM = 3;
    public static final Parameter PARAMETER_VOID = new Parameter() {
        public String getName() {
            return SINGLE_PARAM_NAME;
        }

        public int getNmOfValues() {
            return 1;
        }

        public int getParameterType() {
            return PARAM_TYPE_VOID;
        }

        public Variable.Value[] getValueArray() {
            return VOID_VALUES;
        }

        public String getValueAsPathAt(int pos) {
            return "no-vars";
        }

        public String getValueNode(int pos) {
            return "no-vars";
        }

        public LinkedList<org.funz.parameter.Parameter> getGroupComponents() {
            return null;
        }

        public double getMinValue() {
            return 0.0;
        }

        public double getMaxValue() {
            return 1.0;
        }

        public double getLowerBound() {
            return 0.0;
        }

        public double getUpperBound() {
            return 1.0;
        }

        public boolean isGroup() {
            return false;
        }

        public boolean isContinuous() {
            return false;
        }

        public boolean isReal() {
            return false;
        }

        public boolean isInteger() {
            return false;
        }

        public int getIndex() {
            return 0;
        }

        public void setIndex(int idx) {
        }
    };
    public static String PROJECT_SPACE = "projects", FILES_DIR = "files", SPOOL_DIR = "spool", LOG_DIR = "log", TMP_DIR = "tmp", RESULTS_DIR = "res", OLD_DIR = "old", LAST_DIR = "last", PROJECT_FILE = "project.xml", REPORT_FILE = "report.md", SINGLE_PARAM_NAME = "all-in-one", CASE_LIST_FILE = "cases.xml", DESIGN_SESSIONS_FILE = "sessions.xml";
    public static final LinkedList VOID_PARAMS = new LinkedList();
    final static Variable.Value VOID_VALUES[] = new Variable.Value[]{new Variable.Value("")};

    static {
        VOID_PARAMS.add(PARAMETER_VOID);
    }

    /**
     * returns the default project repository directory.
     */
    public static String getDefaultRepository() {
        return org.funz.Constants.APP_USER_DIR + File.separator + PROJECT_SPACE;
    }

    /**
     * Constructs a directory for a project.
     */
    public static File getDirectoryForProject(String name) {
        return new File(getDefaultRepository() + File.separator + name);
    }

    /**
     * Says whether the project name syntax is correct.
     */
    public static boolean nameIsValid(String name) {
        return Variable.isCorrectAlias(name);
    }

    public Map<String, String> getDesignOptions(String designerId) {
        Map<String, String> o = _doeOptions.get(designerId == null ? _designerId : designerId);
        Log.logMessage(this, SeverityLevel.INFO, false, "Project " + getName() + "> designerId=" + designerId + " options=" + o);
        return o;
    }

    public void setDesignOptions(String designerId, Map<String, String> options) {
        Log.logMessage(this, SeverityLevel.INFO, false, "Project " + getName() + "< designerId=" + designerId + " options=" + options);
        if (designerId == null) {
            return;
        }
        if (options == null) {
            _doeOptions.remove(designerId);
        } else {
            _doeOptions.put(designerId, options);
        }
    }

    public DesignSession getDesignSession(int discCaseId) {
        return _designSessions != null ? _designSessions.get(discCaseId) : null;
    }

    public ArrayList<DesignSession> getDesignSessions() {
        return _designSessions;
    }
    private CaseList _cases, _disCases;
    private ArrayList<DesignSession> _designSessions;
    private Designer _designer;
    private String _code = "", _designerId = DesignConstants.NODESIGNER_ID;
    private Map<String, Map<String, String>> _doeOptions = new HashMap<String, Map<String, String>>();
    private File _dir;
    private final LinkedList<InputFile> _inputfiles = new LinkedList<InputFile>();
    private final LinkedList<VarGroup> _groups = new LinkedList<VarGroup>();
    private final LinkedList _listeners = new LinkedList();
    private boolean _modified = false;
    private String _name = "";
    int _mainOutputFunctionIndex = -1;
    private LinkedList<OutputFunctionExpression> _outputFunctions = new LinkedList<OutputFunctionExpression>();
    //private String[] _outputNames;
    private ParameterListListener _parameterListListener;
    final LinkedList<Parameter> _params = new LinkedList<Parameter>();
    private IOPluginInterface _plugin;// = new DefaultIOPlugin();
    Map<String, String> _tvalues = new HashMap<String, String>();
    private String _model = "";
    private final LinkedList<Variable> _vars = new LinkedList<Variable>();
    /**
     * Returns the directory of a result namespace
     */
    /*public File getResultDir(String name) {
     return new File(_dir, RESULTS_DIR + File.separator + name);
     }*/
    private LinkedList<File> _resultsCache = new LinkedList<File>();

    /**
     * Returns the array of all available result directories.
     */
    /*public File[] getResultDirs() {
     return new File(_dir, RESULTS_DIR).listFiles();
     }*/
    /**
     * contructs a project from an existing project file.
     */
    public Project(File file) {
        try {
            load(file);
            _modified = false;
        } catch (Exception e) {
            e.printStackTrace();
            Log.logException(true, e);
        }
    }

    public Project(String name) {
        this(name, null);
    }

    /**
     * Contructs a new project within the default repository directory. No check
     * are done about the existance of old project.
     *
     * @param name project name
     */
    public Project(String name, File dir) {
        _name = name;
        if (dir == null) {
            _dir = new File(getDefaultRepository() + File.separator + name);
        } else {
            _dir = dir;
        }

        setDirectory(_dir);
    }

    public void setDirectory(File dir) {
        _dir = dir;

        try {
            buildResultsDir(null);
        } catch (IOException ex) {
            Log.err("Could not backup old results", 1);
        }

        File f = getFilesDirectory();//new File(_dir + File.separator + FILES_DIR);
        if ((!f.exists()) && (!f.mkdirs())) {
            Alert.showError("Could not create project directory " + f);
        }
    }

    public File getDirectory() {
        return _dir;
    }

    /**
     * Adds a new group into the project.
     */
    public void addGroup(VarGroup g) {
        //System.err.println("addGroup "+g.getName()+" "+g.getVariablesString());
        _groups.add(g);
        buildParameterList();
    }

    public void addGroup(String g, String... x) {
        VarGroup grp = new VarGroup(g);
        for (String xi : x) {
            Variable v = getVariableByName(xi);
            if (xi == null) {
                Alert.showError("Variable " + xi + " not found. Cannot add in group " + g + ", so skipping.");
            } else {
                grp.addVariable(v);
                v.setGroup(grp);
            }
        }
        addGroup(grp);
    }

    /**
     * Adds a project listener.
     */
    public void addListener(Listener l) {
        if (l != null) {
            _listeners.add(l);
        }
    }

    /**
     * Chechs whether all varaiables are valid (have values introduced).
     */
    public String checkVariablesAreValid() {
        String s = "";
        for (Variable v : _vars) {
            String c = v.checkValid();
            if (c != null) {
                s = s + "\n" + v.getName() + ": " + c;
            }
        }
        if (s.length() == 0) {
            return null;
        } else {
            return s;
        }
    }

    // to invoke after loading case list
    public void setCases(CaseList cases, Case.Observer observer) {
        _cases = cases;
        if (cases == null) {
            return;
        }

        for (Case c : cases) {
            c.setObserver(observer);
        }
    }

    public static enum ParametersStorageOrder {
        BIG_BRANCHES_BEFORE,
        BIG_BRANCHES_AFTER,
        ALPHA,
        ARBITRARY
    }
    List<String> arbitraryOrder;
    private ParametersStorageOrder paramOrder = ParametersStorageOrder.BIG_BRANCHES_BEFORE;

    /**
     * @return the paramOrder
     */
    public ParametersStorageOrder getParametersStorageOrder() {
        return paramOrder;
    }

    /**
     * @param paramOrder the paramOrder to set
     */
    public void setParametersStorageOrder(ParametersStorageOrder paramOrder) {
        this.paramOrder = paramOrder;
    }

    public void setArbitraryParametersStorageOrder(List<String> arbitraryOrder) {
        this.arbitraryOrder = arbitraryOrder;
        paramOrder = ParametersStorageOrder.ARBITRARY;
    }

    public void setArbitraryParametersStorageOrder(String... arbitraryOrder) {
        setArbitraryParametersStorageOrder(Arrays.asList(arbitraryOrder));
    }

    static CaseList makeDiscreteCaseList(List params, Case.Observer observer, final Project caller, ParametersStorageOrder policy) {
        //System.err.println("makeDiscreteCaseList");
        int nCases = 1;
        int nParams = params.size();
        CaseList cases;

        if (nParams == 0) {
            cases = new CaseList(1);
            Case c = new Case(0, new Case.Node[]{new Case.Node(/*0, */SINGLE_PARAM_NAME/*,"",""*/, caller)}, caller);
            c.setObserver(observer);
            cases.add(c);
            return cases;
        }

        for (Iterator it = params.iterator(); it.hasNext();) {
            Parameter p = (Parameter) it.next();
            if (p.isContinuous()) {
                throw new IllegalArgumentException("Parameter " + p + " is contiuous !");
            }
            int nv = p.getNmOfValues();
            nCases *= nv;
        }

        List orderedParams = sort(params, policy, caller.arbitraryOrder);

        //System.err.println("Order " + orderedParams);
        /// 3 parameters a,b,c in this order, with resp. na,nb,nc values. index of case is i.
        /// ic' = i,  ic = ic' % nc
        /// ib' = (i-ic)/nc',  ib = ib' % nb'
        /// ia' = (i-ic-ib*nc)/(nb*nc) = ib'/nb - ib/nb
        /// ...
        /// in general, i(p) = (i(p-1)' - i(p-1))/n(p+1)
        cases = new CaseList(nCases);
        for (int i = 0; i < nCases; i++) {
            Case.Node nodes[] = new Case.Node[nParams];
            Parameter last = (Parameter) orderedParams.get(nParams - 1);
            int ii = i;
            nodes[nParams - 1] = new Case.Node(last.getValueNode(ii % last.getNmOfValues()), caller);
            int nn = last.getNmOfValues();
            for (int i_p = nParams - 2; i_p >= 0; i_p--) {
                Parameter p = (Parameter) orderedParams.get(i_p);
                ii = (ii - ii % nn) / nn;
                nodes[i_p] = new Case.Node(p.getValueNode(ii % p.getNmOfValues()), caller);
                nn = p.getNmOfValues();
            }
            Case c = new Case(i, nodes, caller);
            c.setObserver(observer);
            cases.add(c);
        }

        return cases;
    }

    /**
     * Return order based on policy. Except for continuous parameters, which are
     * placed in the end.
     */
    static List sort(final List params, ParametersStorageOrder policy, final List<String> arbitraryOrder) {

        List orderedParams = new LinkedList(params);
        switch (policy) {
            case ALPHA:
                orderedParams.sort(new Comparator() {
                    @Override
                    public int compare(Object o1, Object o2) {
                        Parameter p1 = (Parameter) o1;
                        Parameter p2 = (Parameter) o2;
                        if (p1.isContinuous()) {
                            if (p2.isContinuous()) {
                                return params.indexOf(o1) - params.indexOf(o1); // do not change anything between p1 and p2
                            } else {
                                return 1;
                            }
                        } else if (p2.isContinuous()) {
                            return -1;
                        }
                        return p1.getName().compareTo(p2.getName());
                    }
                });
                break;
            case ARBITRARY:
                orderedParams.sort(new Comparator() {
                    @Override
                    public int compare(Object o1, Object o2) {
                        Parameter p1 = (Parameter) o1;
                        Parameter p2 = (Parameter) o2;
                        if (p1.isContinuous()) {
                            if (p2.isContinuous()) {
                                return params.indexOf(o1) - params.indexOf(o1); // do not change anything between p1 and p2
                            } else {
                                return 1;
                            }
                        } else if (p2.isContinuous()) {
                            return -1;
                        }
                        if (arbitraryOrder != null && arbitraryOrder.contains(p1.getName()) && arbitraryOrder.contains(p2.getName())) {
                            return Integer.compare(arbitraryOrder.indexOf(p1.getName()), arbitraryOrder.indexOf(p2.getName()));
                        } else if (arbitraryOrder != null && arbitraryOrder.contains(p1.getName())) {
                            return 1;
                        } else if (arbitraryOrder != null && arbitraryOrder.contains(p2.getName())) {
                            return -1;
                        } else {
                            return 0;
                        }
                    }
                });
                break;
            case BIG_BRANCHES_AFTER:
                orderedParams.sort(new Comparator() {
                    @Override
                    public int compare(Object o1, Object o2) {
                        Parameter p1 = (Parameter) o1;
                        Parameter p2 = (Parameter) o2;
                        if (p1.isContinuous()) {
                            if (p2.isContinuous()) {
                                return params.indexOf(o1) - params.indexOf(o1); // do not change anything between p1 and p2
                            } else {
                                return 1;
                            }
                        } else if (p2.isContinuous()) {
                            return -1;
                        }
                        return Integer.compare(p1.getNmOfValues(), p2.getNmOfValues());
                    }
                });
                break;
            case BIG_BRANCHES_BEFORE:
                orderedParams.sort(new Comparator() {
                    @Override
                    public int compare(Object o1, Object o2) {
                        Parameter p1 = (Parameter) o1;
                        Parameter p2 = (Parameter) o2;
                        if (p1.isContinuous()) {
                            if (p2.isContinuous()) {
                                return params.indexOf(o1) - params.indexOf(o1); // do not change anything between p1 and p2
                            } else {
                                return 1;
                            }
                        } else if (p2.isContinuous()) {
                            return -1;
                        }
                        return Integer.compare(-p1.getNmOfValues(), -p2.getNmOfValues());
                    }
                });
                break;
        }
        return orderedParams;
    }

    /**
     * Builds cases based on discrete parameters.
     */
    public void resetDiscreteCases(Case.Observer observer) {
        //System.err.println("resetDiscreteCases");
        _disCases = null;
        _disCases = makeDiscreteCaseList(getDiscreteParameters(), observer, this, getParametersStorageOrder());
        resetDesignSessions();
    }

    public void resetDesignSessions() {
        //System.err.println("resetDesignSessions");
        if (!getDesignerId().equals(DesignConstants.NODESIGNER_ID)) {
            _designSessions = new ArrayList<DesignSession>(_disCases.size());
            for (Case c : _disCases) {
                _designSessions.add(c.getDiscreteCaseId(), new DesignSession(c.getDiscreteCaseId()));
            }
        }
    }

    /**
     * Build the parameters list from varaiables and groups. This MUST be called
     * prior any calculation starts.
     */
    public void buildParameterList() {
        //System.err.println("buildParameterList");
        _params.clear();

        for (Variable var : _vars) {
            if (var.getGroup() == null && (var.isSticky() || var.getNmOfValues() > 0 || var.isContinuous()) && !_params.contains(var)) {
                if (var.isContinuous()) {
                    _params.addLast(var);
                } else {
                    _params.addFirst(var);
                    //System.err.println("Accept " + var);
                }
            }
        }

        for (VarGroup g : _groups) {
            if (!_params.contains(g) && g.getVariables().size() > 0) {
                //System.out.println("Group: " + g);
                _params.addFirst(g);
            }
        }

        int i = 0;
        for (Parameter p : _params) {
            p.setIndex(i++);
        }

        if (_parameterListListener != null) {
            _parameterListListener.parameterListModified();
        }

        // This seems not really needed, but slowing down var editing when too much disc. cases.
        //buildDiscreteCases(null);
    }

    /**
     * Copies the temporary result directory into a definit one
     */
    public void buildResultsDir(Disk.CopyProgressObserver o) throws IOException {
        if (getResultsDir().exists()) {
            Disk.moveDir(getResultsDir(), new File(getOldResultsDir(), Configuration.timeDigest()));
            Alert.showInformation("Project " + getName() + " previous results directory was moved");
        }
        if (getSpoolDir().isDirectory()) {
            Disk.moveDir(getSpoolDir(), getResultsDir(), o);
        }
        saveProject(_dir, null);
    }

    public void saveReport(File report) {
        Log.logMessage(this, SeverityLevel.INFO, false, "Adding report file : " + report.getAbsolutePath());
        if (report.exists()) {
            report.delete();
            Log.logMessage(this, SeverityLevel.INFO, false, "Deleted previous report file : " + report.getAbsolutePath());
        }

        try {
            String report_txt = Report.buildReport(this);
            if (report_txt == null) {
                throw new Exception("Could not create report...");
            }
            ASCII.saveFile(report, report_txt);
        } catch (Exception e) {
            e.printStackTrace();
            Log.logMessage(getName(), SeverityLevel.WARNING, false, "Failed to build report");
        }
    }

    public void clearCases() {
        _cases = null;
        _disCases = null;
    }

    public void cleanOutputs() {
        if (_plugin != null && getFilesAsArray().length > 0) {
            String[] oldoutput = _plugin.getOutputNames();
            //System.out.println("oldoutput =" + oldoutput.length);

            try {
                _plugin.setInputFiles(getFilesAsArray());
            } catch (IllegalArgumentException i) {
                Log.logException(true, i);
            }            // setOutputNames(_plugin.getOutputNames());

            LinkedList<String> newoutput = new LinkedList<String>();
            newoutput.addAll(Arrays.asList(_plugin.getOutputNames()));

            boolean oldoutstillavailable = oldoutput != null && oldoutput.length > 0;
            if (oldoutstillavailable) {
                for (String o : oldoutput) {
                    if (!newoutput.contains(o)) {
                        oldoutstillavailable = false;
                        break;
                    }
                }
            }
            // System.out.println("oldoutstillavailable=" + oldoutstillavailable);
            if (!oldoutstillavailable || _outputFunctions.size() == 0) {
                cleanOutputFunctions();
            }
        }
    }

    public void cleanOutputFunctions() {
        _outputFunctions = _plugin.suggestOutputFunctions();

        if (_outputFunctions == null) {
            _outputFunctions = new LinkedList<OutputFunctionExpression>();
        }

        if (_outputFunctions.size() <= _mainOutputFunctionIndex) {
            _mainOutputFunctionIndex = 0;
        }

        if (_outputFunctions.size() == 0 && getOutputNames().length > 0) {
            for (String o : getOutputNames()) {
                _outputFunctions.add(new OutputFunctionExpression.Text(o));
            }
        }
    }

    /**
     * removes all unreferred variables & groups from the project.
     */
    public void cleanParameters() {
        //System.out.println("cleanParameters");
        boolean found = false;

        LinkedList<Variable> toRemove = new LinkedList<Variable>();
        for (Iterator vit = _vars.iterator(); vit.hasNext();) {
            Variable var = (Variable) vit.next();
            if (var.getFiles().size() == 0) {
                vit.remove();
                if (var.getGroup() != null) {
                    var.getGroup().removeVariable(var);
                }
                toRemove.add(var);
                found = true;
            }
        }

        if (!toRemove.isEmpty()) {
            Log.logMessage(this, SeverityLevel.INFO, false, "Will remove variables " + toRemove);
        }
        for (Variable variable : toRemove) {
            removeVariable(variable);
        }

        LinkedList<VarGroup> toRemoveG = new LinkedList<VarGroup>();
        for (VarGroup grp : _groups) {
            if (grp.getVariables().size() <= 0) {
                toRemoveG.add(grp);
                found = true;
                continue;
            }
            //clean unused groups
            boolean used = false;
            for (Variable var : _vars) {
                if (var.getGroup() == grp) {
                    used = true;
                    break;
                }
            }
            if (!used) {
                toRemoveG.add(grp);
            }
        }
        for (VarGroup g : toRemoveG) {
            removeGroup(g);
        }

        // PERF if (found) {
        buildParameterList();
        //  }
    }

    /**
     * Counts the variables which may be potentially grouped.
     */
    public int countGroupableVariables() {
        int counter = 0;
        for (Iterator vit = _vars.iterator(); vit.hasNext();) {
            Variable var = (Variable) vit.next();
            if (var.getGroup() == null && !var.isSticky()) {
                counter++;
            }
        }
        return counter;
    }

    /**
     * Discard the modification flag
     */
    public void discardModified() {
        _modified = false;
    }

    /**
     * Returns variable group with name name
     *
     * @return VarGroup with name name or null if not found
     */
    public VarGroup findGroup(String name) {
        for (Iterator it = _groups.iterator(); it.hasNext();) {
            VarGroup g = (VarGroup) it.next();
            if (g.getName().equals(name)) {
                return g;
            }
        }
        return null;
    }

    /**
     * Returns variable with name name.
     *
     * @return Variable or null if not found
     */
    public Variable findVariable(String name) {
        for (Iterator it = _vars.iterator(); it.hasNext();) {
            Variable var = (Variable) it.next();
            if (var.getName().equals(name)) {
                return var;
            }
        }
        return null;
    }

    /**
     * Returns the case array. This may be called only after #buildCases() is
     * invoked.
     */
    public synchronized CaseList getCases() {
        //assert _cases != null;
        return _cases;
    }

    public CaseList getDiscreteCases() {
        //assert _disCases != null;
        return _disCases;
    }

    public void setInitialDesign(List<? extends Experiment> exps, Case.Observer observer) {
        _cases = new CaseList(exps.size());
        addDesignCases(exps, observer);
    }

    public List<Case> addDesignCases(List<? extends Experiment> exps, Case.Observer observer) {
        return addDesignCases(exps, observer, 0);
    }

    /* Add new experiments coming from Design.
     They are added for a given combination of disrete variables (discCaseIdx)
     */
    public synchronized List<Case> addDesignCases(List<? extends Experiment> exps, Case.Observer observer, int discCaseIdx) {
        if (_cases == null) {
            _cases = new CaseList(exps.size());
        }
        int i = _cases.size();
        boolean havenew = false;
        List<Case> news = new LinkedList<>();
        for (Experiment e : exps) {
            Case c = new Case(i++, e, _disCases.get(discCaseIdx), this);
            boolean isnew = true;
            for (int j = 0; j < _cases.size(); j++) {
                if (c.getName().equals(_cases.get(j).getName())) { // check if there is any clone. if this is the case 
                    //System.err.println(ASCII.repeat(discCaseIdx + 1, "|", "        ") + "found " + c.toString() + " at " + j);
                    //c = _cases.get(j);
                    isnew = false;
                    break;
                }
            }
            _cases.add(c);
            if (isnew) {
                news.add(c);
                c.setObserver(observer);
                havenew = true;
            }
        }

        return news;
        //System.out.println(Experiment.toString_ExperimentArray("Project.addDesign ALL", _cases));
    }

    public synchronized boolean addCases(List<String[]> nodes, Case.Observer observer) {
        if (_cases == null) {
            _cases = new CaseList(nodes.size());
        }
        int i = _cases.size();
        boolean havenew = false;
        for (String[] n : nodes) {
            Case.Node[] nn = new Case.Node[n.length];
            for (int j = 0; j < nn.length; j++) {
                nn[j] = new Case.Node(n[j], this);

            }
            Case c = new Case(i++, nn, this);
            boolean isnew = true;
            for (int j = 0; j < _cases.size(); j++) {
                if (c.getName().equals(_cases.get(j).getName())) { // check if there is any clone. if this is the case 
                    //System.err.println(ASCII.repeat(discCaseIdx + 1, "|", "        ") + "found " + c.toString() + " at " + j);
                    //c = _cases.get(j);
                    isnew = false;
                    break;
                }
            }
            _cases.add(c);
            if (isnew) {
                c.setObserver(observer);
                havenew = true;
            }
        }

        return havenew;
        //System.out.println(Experiment.toString_ExperimentArray("Project.addDesign ALL", _cases));
    }

    /*public void setDesign(ArrayList<Experiment> exps, Case.Observer observer, int discCaseIdx) {
     //force reinit _cases
     //_cases = new CaseList(exps.size());
     addDesign(exps, observer, discCaseIdx);
     }*/
    /**
     * Returns the case's tmp directory after its calculation is over. Append
     * INPUT_DIR or OUTPUT_DIR to acces to input and outpt directories.
     */
    public File getCaseTmpDir(Case c) {
        return new File(_dir, getTmpCaseRelativePath(c));
    }

    /**
     * Returns the case's tmp directory after its calculation is over.
     */
    public File getCaseTmpDir(int caseIdx) {
        return getCaseTmpDir(_cases.get(caseIdx));
    }

    public File getDisCaseTmpDir(int dcaseIdx) {
        return getCaseTmpDir(_disCases.get(dcaseIdx));
    }

    public File getCaseResultDir(Case c) {
        return new File(getResultsDir(), c.getRelativePath());
    }

    /**
     * Returns the case's directory within resDir space. Append INPUT_DIR or
     * OUTPUT_DIR to acces to input and outpt directories.
     */
    public File getCaseResultDir(int caseIdx/*, String resDir*/) {
        return getCaseResultDir(_cases.get(caseIdx));
    }

    public File getDisCaseResultDir(int dcaseIdx/*, String resDir*/) {
        return getCaseResultDir(_disCases.get(dcaseIdx));

    }

    /**
     * Returns the case's directory within resDir space. Append INPUT_DIR or
     * OUTPUT_DIR to acces to input and outpt directories.
     */
    /*public String getCaseRelativePath(Case c) {
     StringBuffer sb = new StringBuffer();
     LinkedList<String> ns = c.getAllCaseNodes();
     for (String n : ns) {
     sb.append(File.separator);
     sb.append(n);
     }
    
     return sb.toString();
     }*/
    public String getResultCaseRelativePath(Case c) {
        StringBuilder sb = new StringBuilder();
        sb.append(RESULTS_DIR);
        sb.append(c.getRelativePath());
        return sb.toString();
    }

    public String getTmpCaseRelativePath(Case c) {
        StringBuilder sb = new StringBuilder();
        sb.append(SPOOL_DIR);
        sb.append(c.getRelativePath());
        return sb.toString();
    }

    /*public String getResultCaseHTMLPath(Case c) {
     StringBuffer sb = new StringBuffer();
     sb.append(RESULTS_DIR);
     LinkedList<String> ns = getAllCaseNodes(c);
     for (String n : ns) {
     sb.append("/");
     sb.append(n);
     }
     return sb.toString();
     }*/

 /*private static LinkedList<String> getAllCaseNodes(Case c) {
     LinkedList<String> sb = new LinkedList<String>();
    
     Case.Node nodes[] = c.getNodes();
     for (int i = 0; i < nodes.length; i++) {
     if (!nodes[i].isSingle()) {
     sb.add(nodes[i].name);
     }
     }
     return sb;
     }*/
    /**
     * Returns the results root directory
     */
    private File resultsdir = null; // this will allow to use an external results dir, for instance outside .Funz/projects/myproject. Needed by Shell*

    public void setResultsDir(File newresultsdir) {
        resultsdir = newresultsdir;
    }

    public File getResultsDir() {
        if (resultsdir != null) {
            return resultsdir;
        } else {
            return new File(_dir, RESULTS_DIR);
        }
    }

    /**
     * Returns the old results root directory
     */
    public File getOldResultsDir() {
        File olddir = new File(_dir, OLD_DIR);
        if (!olddir.exists()) {
            olddir.mkdir();
        }
        return olddir;
    }

    /**
     * Returns the results root directory
     */
    public LinkedList<File> getCacheDirs() {
        LinkedList<File> cache = new LinkedList<File>();
        if (getOldResultsDir().exists()) {
            cache.add(getOldResultsDir());
        }
        if (_resultsCache != null && _resultsCache.size() > 0) {
            cache.addAll(_resultsCache);
        }
        //if (pool.size() == 0) {
        //    return null;
        //} else {
        return cache;
        //}
    }

    /**
     * Returns the variable set and their values for a case.
     */
    public Map<String, String> getCaseParameters(Case c) {
        if (!caseParametersCache.containsKey(c)) {
            buildCaseParameters(c);
        }
        return caseParametersCache.get(c);

    }
    Map<Case, Map<String, String>> caseParametersCache = new HashMap<Case, Map<String, String>>();

    public void buildCaseParameters(Case c) {
        //System.err.println("Project.getCaseVariables " + c.getName());

        HashMap<String, String> vars = new HashMap<String, String>();

        if (_params.size() == 0) {
            for (Iterator vit = _vars.iterator(); vit.hasNext();) {
                Variable v = (Variable) vit.next();
                vars.put(v.getName(), v.evaluate(v.getValueAt(0)));
                //System.err.println(" vars.put(" + v.getName() + "," + v.getValueAt(0) + ")");
            }
            caseParametersCache.put(c, vars);
            return;
        }

        Case.Node nodes[] = c.getNodes();
        //System.err.println("  Case " + c.getName());
        for (int i = 0; i < nodes.length; i++) {

            Case.Node n = nodes[i];
            //System.err.println("    Node " + n.name);

            if (!n.isGroup()) {// is not in a group
                vars.put(n.getParamName(), getVariableByName(n.getParamName()).evaluate(n.getParamValue()));
            } else {// is in a group
                VarGroup g = null;
                for (VarGroup gtest : _groups) {
                    //System.err.println("gtest = "+gtest.getName()+" "+gtest.getVariablesString());
                    if (gtest.getVariablesString().equals(n.getParamName()) || ("@" + gtest.getName()).equals(n.getParamName())) {
                        g = gtest;
                        break;
                    }
                }
                assert g != null : "Group " + n.getParamName() + " not found";

                if (n.getParamName().startsWith("@")) {
                    vars.put(n.getParamName().substring(1), n.getParamValue());
                } else {
                    vars.put(n.getParamName(), n.getParamValue());
                }

                for (int j = 0; j < g.getVariables().size(); j++) {
                    vars.put(n.getGroupVarName(j), getVariableByName(n.getGroupVarName(j)).evaluate(n.getGroupVarValue(j)));
                }
            }
        }

        // add default values for unspecified variables
        for (Variable v : _vars) {
            if (!vars.containsKey(v.getName())) {
                Log.logMessage(this, SeverityLevel.INFO, false, "The variable " + v.getName() + " is not taken into account. Using its default value.");
                vars.put(v.getName(), v.getDefaultValue());
            }
        }

        //System.err.println(ASCII.cat(vars));
        caseParametersCache.put(c, vars);
    }

    /**
     * Returns the code name.
     */
    public String getCode() {
        return _code;
    }

    public double getMinCPU() {
        return minCPU;
    }

    public long getMinMEM() {
        return minMemory;
    }

    public long getMinDISK() {
        return minDisk;
    }

    public String getRegexpCalculators() {
        return regexpCalculators;
    }

    public void setMinCPU(double cpu) {
        minCPU = cpu;
    }

    public void setMinMEM(long mem) {
        minMemory = mem;
    }

    public void setMinDISK(long disk) {
        minDisk = disk;
    }

    public void setRegexpCalcualtors(String r) {
        regexpCalculators = r;
    }

    /**
     * Retunrs the input file descriptor list of type FileInfo.
     */
    public LinkedList<InputFile> getInputFiles() {
        return _inputfiles;
    }

    public InputFile[] getInputFilesAsArray() {
        InputFile files[] = new InputFile[_inputfiles.size()];
        int i = 0;
        for (Iterator it = _inputfiles.iterator(); it.hasNext(); i++) {
            files[i] = ((InputFile) it.next());
        }
        return files;
    }

    /**
     * Returns the input file array.
     */
    public File[] getFilesAsArray() {
        File files[] = new File[_inputfiles.size()];
        int i = 0;
        for (Iterator it = _inputfiles.iterator(); it.hasNext(); i++) {
            files[i] = ((InputFile) it.next()).getFile();
        }
        return files;
    }

    /**
     * Returns the directory with source files.
     */
    public File getFilesDirectory() {
        return new File(_dir + File.separator + FILES_DIR);
    }

    /**
     * Returns the directory with source files.
     */
    public File getFilesCopyDirectory() {
        return new File(_dir + File.separator + FILES_DIR + ".copy");
    }

    /**
     * Returns the formula syntax rules
     */
    public SyntaxRules getFormulaSyntax() {
        if (_plugin == null) {
            return SyntaxRules.NoSyntax;
        }
        return new SyntaxRules(_plugin.getFormulaStartSymbol(), _plugin.getFormulaLimit());
    }

    /**
     * Returns the pos-th group
     */
    public VarGroup getGroupAt(int pos) {
        return _groups.get(pos);
    }

    /**
     * Returns the group list of type VarGroup
     */
    public LinkedList<VarGroup> getGroups() {
        return _groups;
    }

    /**
     * Returns the group list of type VarGroup
     */
    public VarGroup getGroupByName(String grpname) {
        for (VarGroup g : _groups) {
            if (g.getName().equals(grpname)) {
                return g;
            }
        }
        return null;
    }

    /**
     * Returns the last calculated project dir . Used for the fault tolerance.
     */
    public File getLastProjectDir() {
        return new File(_dir, LAST_DIR);
    }

    /**
     * Returns the project name.
     */
    public String getName() {
        return _name;
    }

    /**
     * gets the output function name
     */
    public OutputFunctionExpression getMainOutputFunction() {
        if (_outputFunctions == null || _outputFunctions.size() == 0) {
            return null;
        }
        if (_mainOutputFunctionIndex < 0 || _mainOutputFunctionIndex > _outputFunctions.size() - 1) {
            return null;        //if (_OutputFunction == null) {
            //    System.err.println("_OutputFunction is null, switch to Default OutputFunction");
            //    _OutputFunction = OutputFunctionExpression.getDefaultOutputFunction();
            //}
            //System.err.println("getMainOutputFunction > "+_OutputFunction);
        }
        return _outputFunctions.get(_mainOutputFunctionIndex);
    }

    public String getMainOutputFunctionName() {
        if (_outputFunctions == null || _mainOutputFunctionIndex < 0 || _outputFunctions.size() <= _mainOutputFunctionIndex || _outputFunctions.get(_mainOutputFunctionIndex) == null) {
            return null;
        }
        return _outputFunctions.get(_mainOutputFunctionIndex).toNiceSymbolicString();
    }

    /**
     * Gets the output fields names
     */
    public String[] getOutputNames() {
        if (_plugin == null) {
            return null;
        }
        return _plugin.getOutputNames();
    }

    public String[] getVisibleParameterNames() {
        List<Parameter> dp = null;
        if (_designer == null || _designer.viewManagedParams()) {
            dp = getParameters();
        } else {
            dp = getDiscreteParameters();
        }

        int np = dp.size();

        String name[] = new String[np];
        int i = 0;
        for (Parameter p : dp) {
            if (p.isGroup()) {
                name[i] = p.getName() + " ( ";
                for (Parameter v : ((VarGroup) p).getGroupComponents()) {
                    name[i] += v.getName() + " ";
                }
                name[i] += ")";
            } else {
                name[i] = p.getName();
            }
            i++;
        }
        return name;

    }

    /**
     * Returns the parameter name array
     */
    public String[] getParameterNamesByStorageOrder() {
        int np = _params.size();

        if (np == 0) {
            return new String[]{SINGLE_PARAM_NAME};
        }
        String name[] = new String[np];

        int i = 0;
        for (Object p : sort(_params, paramOrder, arbitraryOrder)) {
            name[i] = ((Parameter) p).getName();
            i++;
        }

        return name;
    }

    /**
     * Return the parameter list of type Parameter. A project has always a
     * parameter list even if the are no variables.
     */
    public LinkedList<Parameter> getParameters() {
        return _params.size() == 0 ? VOID_PARAMS : _params;
    }

    public LinkedList<Parameter> getParametersByStorageOrder() {
        return _params.size() == 0 ? VOID_PARAMS : (LinkedList<Parameter>) sort(_params, paramOrder, arbitraryOrder);
    }

    public HashMap<String, Object> getVoidInput() {
        HashMap<String, Object> voidI = new HashMap<String, Object>();
        for (Variable v : getVariables()) {
            String dv = v.getDefaultValue();
            Object voidval = null;
            if (v.getType().equals(Variable.TYPE_CONTINUOUS) || v.getType().equals(Variable.TYPE_REAL)) {
                Double val = Double.NaN;
                try {
                    val = Double.parseDouble(dv);
                } catch (Exception ne) {
                    val = 0.0;
                }
                voidval = val;
            } else if (v.getType().equals(Variable.TYPE_INT)) {
                Integer val = 0;
                try {
                    val = Integer.parseInt(dv);
                } catch (Exception ne) {
                    val = 0;
                }
                voidval = val;
            } else if (v.getType().equals(Variable.TYPE_STRING)) {
                if (dv == null || dv.length() == 0) {
                    voidval = "anystring";
                } else {
                    voidval = dv;
                }
            } else if (v.getType().equals(Variable.TYPE_TEXTFILE)) {
                if (dv == null || dv.length() == 0) {
                    voidval = "anyfile";
                } else {
                    File f = new File(dv);
                    if (f.exists()) {
                        voidval = getASCIIFileContent(f);
                    } else {
                        voidval = "anycontent";
                    }
                }
            }
            voidI.put(v.getName(), voidval);
        }
        return voidI;
    }

    public Parameter getParameterbyName(String n) {
        for (Parameter p : _params) {
            if (p.getName().equals(n)) {
                return p;
            }
        }

        return null;
    }

    public List<Parameter> getDiscreteParameters() {
        LinkedList<Parameter> params = new LinkedList<Parameter>();
        for (Parameter p : _params) {
            if (!p.isContinuous()) {
                params.add(p);
            }
        }
        return params;
    }

    public List<Parameter> getContinuousParameters() {
        LinkedList<Parameter> params = new LinkedList<Parameter>();
        if (_params != null) {
            for (Parameter p : _params) {
                if (p.isContinuous()) {
                    params.add(p);
                }
            }
        }
        return params;
    }

    public String[] getContinuousParametersNames() {
        LinkedList<String> params = new LinkedList<String>();
        if (_params != null) {
            for (Parameter p : _params) {
                if (p.isContinuous()) {
                    params.add(p.getName());
                }
            }
        }
        return params.toArray(new String[params.size()]);
    }

    /**
     * Returns the project type.
     */
    public IOPluginInterface getPlugin() {
        return _plugin;
    }

    /**
     * Project's base directory.
     */
    public File getRootDirectory() {
        return _dir;
    }

    /**
     * Returns the tagged value list of type TValue.
     */
    public Map<String, String> getTaggedValues() {
        return _tvalues;
    }

    /**
     * Gets the directory for temporary files during calculations.
     */
    public File getSpoolDir() {
        return new File(_dir, SPOOL_DIR);
    }

    public File getTmpDir() {
        File tmpdir = new File(_dir, TMP_DIR);
        if (!tmpdir.exists()) {
            tmpdir.mkdirs();
        }
        return tmpdir;
    }

    public File getLogDir() {
        File logdir = new File(_dir, LOG_DIR);
        if (!logdir.exists()) {
            logdir.mkdirs();
        }
        return logdir;
    }

    /**
     * Returns the total number of cases to calculate.
     */
    public int getNmOfDiscreteCases(boolean reset) {
        if (!reset) {
            if (_disCases != null) {
                return _disCases.size();
            }
        }

        if (_params == null) {
            return 0;
        }

        int ret = 1;
        for (Parameter p : _params) {
            if (!p.isContinuous()) {
                ret *= p.getNmOfValues();
            }
        }

        return ret;
    }

    /**
     * Returns the total number of cases to calculate.
     */
    public int getNmOfDiscreteCases() {
        return getNmOfDiscreteCases(false);
    }

    /**
     * Returns the total number of cases to calculate.
     */
    public int getTotalNmOfCases() {
        if (_cases != null) {
            return _cases.size();
        }

        int ret = 1;
        for (Iterator it = _params.iterator(); it.hasNext();) {
            ret *= ((Parameter) it.next()).getNmOfValues();
        }
        return ret;
    }

    /**
     * Returns the project type.
     */
    public String getModel() {
        return _model;
    }

    /**
     * Registers the project type.
     */
    public void setModel(String model) {
        _model = model;
        //modified();
    }

    /**
     * Returns the pos-th variable
     */
    public Variable getVariableAt(int pos) {
        return _vars.get(pos);
    }

    /**
     * Returns the pos-th variable
     */
    public Variable getVariableByName(String vname) {
        for (Variable v : _vars) {
            if (v.getName().equals(vname)) {
                return v;
            }
        }
        Log.err("Could not find variable " + vname, 1);
        return null;
    }

    /**
     * Returns the variable list of type Variable
     */
    public LinkedList<Variable> getVariables() {
        return _vars;
    }

    /**
     * Returns the variable syntax rules
     */
    public SyntaxRules getVariableSyntax() {
        if (_plugin == null) {
            return SyntaxRules.NoSyntax;
        }
        return new SyntaxRules(_plugin.getVariableStartSymbol(), _plugin.getVariableLimit());
    }

    public int countFreeContinuousVariables() {
        int ret = 0;
        for (Variable v : _vars) {
            if (v.isContinuous() && v.getGroup() == null) {
                ret++;
            }
        }
        return ret;
    }

    public ArrayList<Variable> getFreeContinuousVariables() {
        ArrayList<Variable> ret = new ArrayList<Variable>();
        for (Variable v : _vars) {
            if (v.isContinuous() && v.getGroup() == null) {
                ret.add(v);
            }
        }
        return ret;
    }

    /**
     * Says whether the project has file with name name.
     */
    public boolean hasFile(InputFile fi) {
        for (InputFile fii : _inputfiles) {
            if (fi.equals(fii)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Imports a file and adds automatically all found variables.
     */
    public void importFileOrDir(File src, String... path) throws Exception {
        if (!ASCII.removeAccents(src.getName()).equals(src.getName())) {
            Alert.showInformation("Warning, file name " + src.getName() + " may be incompatible with remote code execution!");
        }

        InputFile[] fis = _plugin.importFileOrDir(src);
        for (InputFile fi : fis) {
            if (fi.getParentPath() == null && path != null && path.length > 0) {
                fi.setParentPath(path);
            }

            if (_inputfiles.contains(fi)) {
                for (Iterator<InputFile> it = _inputfiles.iterator(); it.hasNext();) {
                    InputFile tmp = it.next();
                    if (tmp.equals(fi)) {
                        fi = tmp;
                        break;
                    }
                }

            } else {
                _inputfiles.add(fi);
            }
        }
        try {
            _plugin.setInputFiles(getFilesAsArray());
        } catch (IllegalArgumentException i) {
            Alert.showException(i);
        }

        try {
            readInputVariables();
        } catch (Exception e) {
            Alert.showException(e);
        }

        modified();
    }

    public void removeFile(InputFile fi) throws Exception {
        _inputfiles.remove(fi);
        fi.getFile().delete();
        for (Iterator vit = _vars.iterator(); vit.hasNext();) {
            Variable var = (Variable) vit.next();
            var.removeFile(fi);
        }
        saveInSpool();

        try {
            _plugin.setInputFiles(getFilesAsArray());
        } catch (IllegalArgumentException i) {
            Alert.showException(i);
        }

        readInputVariables();

        modified();
    }

    /**
     * Compares the project with the latest calculated one.
     */
    public boolean isIdenticalToLast() {
        //System.out.println("isIdenticalToLast");
        File last = new File(getLastProjectDir() + File.separator + PROJECT_FILE);

        if (last.exists()) {
            try {
                File old = new File(last + ".old");
                last.renameTo(old);
                saveProject(getLastProjectDir(), null);
                boolean ret = ASCIIFilesAreIdentical(last, old);
                old.delete();
                return ret;
            } catch (Exception e) {
                // do nothing
            }
        }

        return false;
    }

    /**
     * Says whether the project has been modified since the last change.
     */
    public boolean isModified() {
        return _modified;
    }

    /**
     * Loads a project file.
     */
    private void load(File file) throws Exception {
        if (!file.exists()) {
            throw new IOException("File " + file + " not found.");
        }
        Document d = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
        Element e = d.getDocumentElement();
        if (!e.getTagName().equals(ELEM_FUNZ)) {
            throw new Exception("wrong XML element " + e.getTagName() + " in file " + file);
        }

        _name = e.getAttribute(ATTR_NAME);
        _model = e.hasAttribute(ATTR_MODEL) ? e.getAttribute(ATTR_MODEL) : e.getAttribute(ATTR_TYPE); // for back compat...
        _code = e.getAttribute(ATTR_CODE);

        _designerId = e.getAttribute(ATTR_DESIGNER_ID);
        if (!_designerId.equals(Designer.NODESIGNER_ID)) {
            setDesigner(DesignPluginsLoader.newInstance(_designerId));
        }
        //_designer = DesignPluginsLoader.newInstance(_designerId, this);

        _dir = file.getParentFile();
        File dir = getFilesDirectory();

        NodeList tvalues = e.getElementsByTagName(ELEM_TVALUE);
        for (int i = 0; i < tvalues.getLength(); i++) {
            Element tv = (Element) tvalues.item(i);
            _tvalues.put(tv.getAttribute(ATTR_KEY), fromHTML(tv.getAttribute(ATTR_VALUE)));
        }

        NodeList files = e.getElementsByTagName(ELEM_FILE);
        for (int i = 0; i < files.getLength(); i++) {
            _inputfiles.add(new InputFile(dir, (Element) files.item(i)));
        }

        _plugin = IOPluginsLoader.newInstance(e.getAttribute(ATTR_PLUGIN), _inputfiles.get(0).getFile());
        _plugin.setProject(this);
        _plugin.setVariableSyntax(SyntaxRules.getStartSymbolIdxByName(e.getAttribute(ATTR_VAR_START)), SyntaxRules.getLimitsIdxByName(e.getAttribute(ATTR_VAR_LIM)));
        _plugin.setFormulaSyntax(SyntaxRules.getStartSymbolIdxByName(e.getAttribute(ATTR_FRM_START)), SyntaxRules.getLimitsIdxByName(e.getAttribute(ATTR_FRM_LIM)));

        _mainOutputFunctionIndex = Integer.parseInt(fromHTML(e.getAttribute(ATTR_OUTFN)));

        String ofl = e.getAttribute(ATTR_OUTFN_LIST);
        if (ofl == null || ofl.length() == 0) {
            _outputFunctions = _plugin.suggestOutputFunctions();
        } else {
            _outputFunctions = new LinkedList<OutputFunctionExpression>();
            String[] ofs = ofl.split(LISTSEPARATOR);
            for (String of : ofs) {
                _outputFunctions.add(OutputFunctionExpression.read(fromHTML(of)));
            }
        }
        //_outputNames = XMLConverter.fromHTML(e.getAttribute(ATTR_OUTS)).split(LISTSEPARATOR);

        NodeList groups = e.getElementsByTagName(ELEM_GROUP);
        for (int i = 0; i < groups.getLength(); i++) {
            _groups.add(new VarGroup((Element) groups.item(i)));
        }

        NodeList vars = e.getElementsByTagName(ELEM_VARIABLE);
        for (int i = 0; i < vars.getLength(); i++) {
            Variable v = new Variable(this, (Element) vars.item(i));
            _vars.add(v);
        }

        for (Iterator it = _inputfiles.iterator(); it.hasNext();) {
            InputFile fi = (InputFile) it.next();
            LinkedList varnames = fi.getVarNames();
            for (Iterator vit = varnames.iterator(); vit.hasNext();) {
                Variable v = findVariable((String) vit.next());
                fi.addVariable(v);
                v.addFile(fi);
            }
        }

        for (Iterator it = _groups.iterator(); it.hasNext();) {
            VarGroup g = (VarGroup) it.next();
            LinkedList varnames = g.getVarNames();
            for (Iterator vit = varnames.iterator(); vit.hasNext();) {
                Variable v = findVariable((String) vit.next());
                g.addVariable(v);
                v.setGroup(g);
            }
            g.setParameterVariable(findVariable(g.getParameterVariableName()));
        }

        NodeList params = e.getElementsByTagName(ELEM_PARAM);
        for (int i = 0; i < params.getLength(); i++) {
            Element p = (Element) params.item(i);
            String name = p.getAttribute(ATTR_NAME);
            int type = Integer.parseInt(p.getAttribute(ATTR_TYPE));
            _params.add(type == Parameter.PARAM_TYPE_VARIABLE ? (Parameter) findVariable(name) : (Parameter) findGroup(name));
        }

        NodeList designers = e.getElementsByTagName(ELEM_DESIGNER_ID);
        for (int i = 0; i < designers.getLength(); i++) {
            Element designer = (Element) designers.item(i);
            String name = designer.getAttribute(ATTR_NAME);
            HashMap<String, String> opts = new HashMap<String, String>();

            NodeList options = designer.getElementsByTagName(ELEM_OPTION);
            for (int j = 0; j < options.getLength(); j++) {
                Element o = (Element) options.item(j);
                String key = o.getAttribute(ATTR_KEY);
                String val = fromHTML(o.getAttribute(ATTR_VALUE));
                opts.put(key, val);
            }
            _doeOptions.put(name, opts);
        }
    }

    /**
     * Notifies all registerred listeners about a modification.
     */
    public void modified() {
        _modified = true;
        for (Iterator it = _listeners.iterator(); it.hasNext();) {
            ((Listener) it.next()).projectModified(this);
        }
        try {
            saveInSpool();
        } catch (Exception e) {
            Log.logException(false, e);
        }
    }

    /**
     * Moves a parameter within the parameter list. Invoking
     * #buildParameterList() will reset the list.
     */
    public void moveParameter(int idx, int where) {
        Parameter p = _params.get(idx);
        _params.remove(p);
        if (where == MV_PARAM_TOP) {
            _params.add(0, p);
        } else if (where == MV_PARAM_UP) {
            _params.add(idx - 1, p);
        }
        if (where == MV_PARAM_DOWN) {
            _params.add(idx + 1, p);
        } else if (where == MV_PARAM_BOTTOM) {
            _params.add(p);
        }
        if (_parameterListListener != null) {
            _parameterListListener.parameterListModified();
        }
    }

    /**
     * Chechs whether all varaiables are valid (have values introduced).
     */
    public String checkOutputFunctionIsValid() {
        OutputFunctionExpression mof = getMainOutputFunction();
        if (mof == null) {
            return "Output function is null";
        } else {
            return mof.checkValidExpression();
        }
    }

    // ['c','a,b'] -> ['1','2,3'] => ['c','a','b'] -> ['1','2','3']
    public static Map<String, String[]> splitGroupedVar(String var, String[] val) {
        Map<String, String[]> splittedvars = new HashMap<String, String[]>();
        String[] splitvar = var.split(",");
        String[][] splitvals = new String[val.length][];

        for (int j = 0; j < val.length; j++) {
            splitvals[j] = val[j].split(",");
            if (splitvals[j].length != splitvar.length) {
                splittedvars.put(var, val);
                return splittedvars;
            }
        }

        for (int i = 0; i < splitvar.length; i++) {
            String[] splitvali = new String[splitvals.length];
            for (int j = 0; j < splitvali.length; j++) {
                splitvali[j] = splitvals[j][i];
            }
            splittedvars.put(splitvar[i], splitvali);
        }
        return splittedvars;
    }

    public static Map<String, String[]> splitGroupedVars(Map<String, String[]> vars_values) {
        Map<String, String[]> splittedvars = new HashMap<String, String[]>(vars_values.size() * 2);
        for (String var : vars_values.keySet()) {
            splittedvars.putAll(splitGroupedVar(var, vars_values.get(var)));
        }
        return splittedvars;
    }

    public void compileFileIn(File in, Map<String, String> vars_value, File outdir) throws Exception {
        if (in.isDirectory()) {
            for (File f : in.listFiles()) {
                compileFileIn(f, vars_value, new File(outdir, in.getName()));
            }
            return;
        }

        long tic = Calendar.getInstance().getTimeInMillis();

        if (vars_value == null) {
            vars_value = new HashMap<>();
            Log.logMessage(this, SeverityLevel.WARNING, false, "Variables model undefined.");
        }

        Map<String, String> splittedvars = new HashMap<String, String>(vars_value.size() * 2);
        for (String var : vars_value.keySet()) {
            Map<String, String[]> splittedvar = splitGroupedVar(var, new String[]{vars_value.get(var)});
            for (String v : splittedvar.keySet()) {
                splittedvars.put(v, splittedvar.get(v)[0]);
            }
        }
        vars_value = splittedvars;

        for (int counter = 3; !outdir.exists() && counter > 0; counter--) {
            if (!outdir.mkdirs()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }

        if (!outdir.exists()) {
            Log.logMessage(this, SeverityLevel.WARNING, false, "Could not create directory " + outdir);
            Alert.showError("Check your disk space:\n" + outdir.getAbsolutePath());
            throw new Exception("could not create directory " + outdir);
        }
        synchronized (_plugin.getFormulaInterpreter()) {
            try {
                VariableMethods.compileFile("" + _plugin.getCommentLine(), getVariableSyntax(), getFormulaSyntax(), in, outdir, vars_value, _plugin.getFormulaInterpreter());
            } catch (Exception ex) {
                Log.logMessage(this, SeverityLevel.WARNING, false, "Parsing error:\n" + ex.getMessage());
                Alert.showError("Parsing error, check the following expressions:\n" + ex.getMessage());
                throw ex;
            }

        }
        long toc = Calendar.getInstance().getTimeInMillis();
        Log.logMessage("Project.compileFileIn", SeverityLevel.INFO, false, "compile file in " + ((toc - tic) / 1000.0) + " s.");
    }

    public Map findInputVariables(File in) throws Exception {
        if (in.isDirectory()) {
            Map merge = new HashMap<String, String>();
            for (File f : in.listFiles()) {
                merge.putAll(findInputVariables(f));
            }
            return merge;
        }

        Map<String, String> var_models = new HashMap<String, String>();
        long tic = Calendar.getInstance().getTimeInMillis();
        HashSet hs;
        synchronized (_plugin.getFormulaInterpreter()) {
            try {
                hs = VariableMethods.parseFile("" + _plugin.getCommentLine(), getVariableSyntax(), getFormulaSyntax(), in, null, null, null, null, null, null, null, var_models, _plugin.getFormulaInterpreter());
            } catch (Exception ex) {
                Log.logMessage(this, SeverityLevel.WARNING, false, "Parsing error:\n" + ex.getMessage());
                Alert.showError("Parsing error, check the following expressions:\n" + ex.getMessage());
                throw ex;
            }
        }
        long toc = Calendar.getInstance().getTimeInMillis();
        Log.logMessage("Project.findInputVariables", SeverityLevel.INFO, false, "parse file in " + ((toc - tic) / 1000.0) + " s.");
        if (hs != null) {
            for (Object object : hs) {
                if (!var_models.containsKey(object.toString())) {
                    var_models.put(object.toString(), null);
                }
            }
        }
        return var_models;
    }

    public File[] prepareCaseFiles(Case c) throws Exception {
        File dir = new File(getCaseTmpDir(c), INPUT_DIR);

        if (dir.exists()) {
            //System.err.println("Case " + c.getName() + " files already created in " + dir);
            throw new IOException("Case " + c.getName() + " files already created in " + dir);
            /*File files[] = new File[_files.size()];
             int i = 0;
             for (Iterator it = _files.iterator(); it.hasNext(); i++) {
             FileInfo fi = (FileInfo) it.next();
             files[i] = new File(dir + File.separator + fi.getFile().getName());
             }
             return files;*/
        } else {
            long tic = Calendar.getInstance().getTimeInMillis();
            for (int counter = 3; !dir.exists() && counter > 0; counter--) {
                if (!dir.mkdirs()) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }

            if (!dir.exists()) {
                Log.logMessage(this, SeverityLevel.WARNING, false, "Could not create directory " + dir);
                Alert.showError("Could not create directory " + dir + ", check your disk space:\n" + dir.getAbsolutePath());
                throw new IOException("could not create directory " + dir);
            }

            buildCaseParameters(c);

            Map<String, String> vars = getCaseParameters(c);

            File files[] = new File[_inputfiles.size()];
            int i = 0;
            for (Iterator it = _inputfiles.iterator(); it.hasNext(); i++) {
                InputFile fi = (InputFile) it.next();
                File fidir = dir;
                if (fi.getParentPath() != null) {
                    for (String d : fi.getParentPath()) {
                        fidir = new File(fidir, d);
                    }
                }
                fidir.mkdirs();
                if (!fi.hasParameters) {
                    files[i] = new File(fidir, fi.getFile().getName() + "_md5");
                    ASCII.saveFile(files[i], fi.getSum());
                } else {
                    try {// alread sync on getFormulaInterpreter inside  VariableMethods.compileFile
                        files[i] = VariableMethods.compileFile("" + _plugin.getCommentLine(), getVariableSyntax(), getFormulaSyntax(), fi.getFile(), fidir, vars, _plugin.getFormulaInterpreter());
                    } catch (Exception ex) {
                        Log.logMessage(this, SeverityLevel.WARNING, false, "Parsing error:\n" + ex.getMessage());
                        Alert.showError("Parsing error, check the following expressions:\n" + ex.getMessage());
                        throw ex;
                    }
                    synchronized (_plugin.getFormulaInterpreter()) {
                        HashMap<String, Object> ivars = new HashMap<String, Object>();
                        try {
                            for (String iv : _plugin.getFormulaInterpreter().listVariables(false, false)) {
                                try {
                                    ivars.put(iv, _plugin.getFormulaInterpreter().eval(iv, null));
                                } catch (Exception ex) {
                                    Log.logMessage(this, SeverityLevel.WARNING, false, "Intermediate variable evaluation error:\n" + iv + "\n" + ex.getLocalizedMessage());
                                    Alert.showError("Intermediate variable evaluation error, check the following expressions:\n" + iv + "\n" + ex.getLocalizedMessage());
                                    throw ex;
                                }
                            }
                        } catch (Exception ex) {
                            Log.logMessage(this, SeverityLevel.WARNING, false, "List of variable evaluation error:\n" + ex.getLocalizedMessage());
                            Alert.showError("List of variable evaluation error:\n" + ex.getLocalizedMessage());
                            throw ex;
                        }
                        c.addIntermediateValues(ivars);
                    }
                }
            }
            long toc = Calendar.getInstance().getTimeInMillis();
            Log.logMessage("Project.prepareCaseFiles", SeverityLevel.INFO, false, "prepare case files in " + ((toc - tic) / 1000.0) + " s.");
            return files;
        }
    }

    /**
     * Removes group from the project. Be sure to unlink varaibles first.
     */
    public void removeGroup(VarGroup g) {
        //System.err.println("removeGroup "+g.getName()+" "+g.getVariablesString());
        _groups.remove(g);
        for (Iterator it = _vars.iterator(); it.hasNext();) {
            ((Variable) it.next()).setGroup(null);
        }

        removeParameter(g);
    }

    /**
     * Removes a variable from the project. Be sure to remove the files and
     * groups too.
     */
    public void removeVariable(Variable v) {
        _vars.remove(v);
        for (Iterator it = _groups.iterator(); it.hasNext();) {
            ((VarGroup) it.next()).removeVariable(v);
        }

        removeParameter(v);
    }

    /**
     * Removes a parameter from the project.
     */
    public void removeParameter(Parameter p) {
        if (_params.remove(p) && _parameterListListener != null) {
            _parameterListListener.parameterListModified();
        }
    }
    HashMap<String, String> defaultvalues_cache = new HashMap<String, String>();

    public void readInputVariables() throws VariableMethods.ParseEvalException,
            UnsupportedEncodingException,
            BadSyntaxException,
            FileNotFoundException,
            IOException,
            MathExpression.MathException {
        long tic = Calendar.getInstance().getTimeInMillis();

        setVariableSyntax(new SyntaxRules(_plugin.getVariableStartSymbol(), _plugin.getVariableLimit()));
        setFormulaSyntax(new SyntaxRules(_plugin.getFormulaStartSymbol(), _plugin.getFormulaLimit()));

        //clean files vars
        for (int i = 0; i < _inputfiles.size(); i++) {
            InputFile fi = _inputfiles.get(i);
            if (!fi.hasParameters) {
                continue;
            }
            fi._vars.clear();
            fi.releaseVarNames();
        }

        // store all default values
        for (Variable v : _vars) {
            if (v.getDefaultValue() == null || v.getDefaultValue().length() == 0) {
                if (v.getValues().size() > 0) {
                    defaultvalues_cache.put(v.getName(), v.getValueAt(0));
                } else {
                    defaultvalues_cache.put(v.getName(), "" + Math.random());
                }
            } else {
                defaultvalues_cache.put(v.getName(), v.getDefaultValue());
            }
        }

        //clean vars
        LinkedList<Variable> oldvars = new LinkedList<Variable>();//keep old vars to recall their values
        for (Iterator vit = _vars.iterator(); vit.hasNext();) {
            Variable var = (Variable) vit.next();
            oldvars.add(var);
            vit.remove();
            removeParameter(var);
            if (var.getGroup() != null && _groups.contains(var.getGroup())) {
                _groups.remove(var.getGroup());
                removeParameter(var.getGroup());
            }
        }

        MathExpression engine = MathExpression.GetDefaultInstance();
        if (_plugin.getFormulaInterpreter() != null) {
            engine = _plugin.getFormulaInterpreter();
        }
        //reparse files
        intermediate.clear();
        for (int i = 0; i < _inputfiles.size(); i++) {
            InputFile fi = _inputfiles.get(i);
            if (!fi.hasParameters) {
                continue;
            }
            HashSet vars = null;
            HashMap<String, String> default_models = new HashMap<String, String>();
            LinkedList<Replaceable> vars_n_forms = new LinkedList<Replaceable>();
            boolean varsModified = false;
            try {
                Log.logMessage(this, SeverityLevel.INFO, false, "Parsing " + fi.getFile().getAbsolutePath());
                vars = VariableMethods.parseFileVars(getVariableSyntax(), fi.getFile(), null, null, vars_n_forms, default_models);
            } catch (BadSyntaxException ex) {
                Log.logMessage(this, SeverityLevel.WARNING, false, "Syntax error:\n" + ex.toString());
                // restore old variables
                for (Variable v : oldvars) {
                    _vars.add(v);
                }
                buildParameterList();
                throw ex;
            } catch (Exception ex) {
                Log.logMessage(this, SeverityLevel.ERROR, false, "Error:\n" + ex.toString());
                throw ex;
            }

            if (vars != null) {
                for (Iterator it = vars.iterator(); it.hasNext();) {
                    String v = (String) it.next();
                    boolean oldvar = false;
                    for (Variable var : _vars) {
                        if (var.getName().equals(v)) {
                            var.addFile(fi);
                            fi.addVariable(var);
                            varsModified = true;
                            oldvar = true;
                            if (default_models.containsKey(var.getName())) {
                                var.setDefaultModel(default_models.get(var.getName()), false);
                            }
                            break;
                        }
                    }
                    if (!oldvar) { // means that the variable was just inserted
                        Variable var = new Variable(this, v);
                        var.addFile(fi);
                        fi.addVariable(var);
                        _vars.add(var);
                        varsModified = true;
                        if (default_models.containsKey(var.getName())) {
                            var.setDefaultModel(default_models.get(var.getName()), true);
                        }

                        if (v.indexOf(".") > 0) {
                            //System.err.println("Found a grouped variable: "+v);
                            String prefix = v.substring(0, v.indexOf("."));
                            VarGroup g = getGroupByName(prefix);
                            if (g == null) {
                                g = new VarGroup(prefix);
                                _groups.add(g);
                            }
                            g.addVariable(var);
                            var.setGroup(g);
                        } //else System.err.println("Variable "+v+" is not grouped");
                    }
                }
            }

            //if (Configuration.getBoolProperty("testFormulas")) {
            // searching bad formulas
            //  first instanciate variables default values
            for (Variable v : _vars) {
                if (v.getDefaultValue() == null || v.getDefaultValue().length() == 0) {
                    defaultvalues_cache.put(v.getName(), "" + Math.random());
                } else {
                    defaultvalues_cache.put(v.getName(), v.getDefaultValue());
                }
            }

            try {
                File tmp = File.createTempFile("___" + fi.getFile().getName(), "_tmp", getTmpDir());
                Log.logMessage(this, SeverityLevel.INFO, false, "Instanciating with default values " + fi.getFile().getAbsolutePath() + " in " + tmp.getAbsolutePath());
                vars = VariableMethods.parseFile(_plugin.getCommentLine() + "", getVariableSyntax(), getFormulaSyntax(), fi.getFile(), tmp,
                        defaultvalues_cache, null, null, vars_n_forms, null, null, null, engine);
                for (String iv : engine.listVariables(false, false)) {
                    intermediate.put(iv, engine.eval(iv, null));
                }
            } catch (ParseEvalException pse) {
                String expr = vars_n_forms.getLast().name;
                for (String v : defaultvalues_cache.keySet()) {
                    if (expr.contains(defaultvalues_cache.get(v))) {
                        expr = expr.substring(0, expr.indexOf(defaultvalues_cache.get(v)));
                    }
                }
                throw new ParseEvalException(fi.getFile(), pse.line, pse.column, expr, engine.getLastMessage());
            } catch (BadSyntaxException bse) {
                String expr = vars_n_forms.getLast().name;
                for (String v : defaultvalues_cache.keySet()) {
                    if (expr.contains(defaultvalues_cache.get(v))) {
                        expr = expr.substring(0, expr.indexOf(defaultvalues_cache.get(v)));
                    }
                }
                throw new BadSyntaxException(fi.getFile(), bse.line, bse.txt, expr);
            }
            //}
        }

        buildParameterList();

        //get old vars values, if possible, do not support groups, ...
        for (int i = 0; i < oldvars.size(); i++) {
            for (int j = 0; j < _vars.size(); j++) {
                if (_vars.get(j).getName().equals(oldvars.get(i).getName())) {
                    _vars.get(j).setType(oldvars.get(i).getType());
                    /*if (oldvars.get(i).getDefaultModel() != null) {
                     _vars.get(j).setDefaultModel(oldvars.get(i).getDefaultModel(), false);
                     }*/
                    if (_vars.get(j).isContinuous()) {
                        _vars.get(j).setLowerBound(oldvars.get(i).getLowerBound());
                        _vars.get(j).setUpperBound(oldvars.get(i).getUpperBound());
                    } else {
                        if (oldvars.get(i).getGroup() != null) {
                            boolean found = false;
                            for (VarGroup g : _groups) {
                                if (g.getName().equals(oldvars.get(i).getGroupName())) {
                                    g.addVariable(_vars.get(j));
                                    _vars.get(j).setGroup(g);
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                VarGroup g = new VarGroup(oldvars.get(i).getGroupName());
                                g.addVariable(_vars.get(j));
                                _vars.get(j).setGroup(g);
                                _groups.add(g);
                            }
                        }
                        _vars.get(j).setValues(oldvars.get(i).getValues());

                    }
                }
            }
        }

        modified();

        long toc = Calendar.getInstance().getTimeInMillis();
        Log.logMessage("Project.reparseFiles", SeverityLevel.INFO, false, "found vars " + _vars + ", reparse files in " + ((toc - tic) / 1000.0) + " s.");
    }
    Map<String, Object> intermediate = new HashMap<String, Object>();

    public Map<String, Object> getVoidIntermediate() {
        return intermediate;
    }

    /**
     * Saves the project
     *
     * @throws Exception
     */
    public void saveInSpool() throws Exception {
        cleanParameters();

        saveProject(_dir, null);

        for (Iterator it = _listeners.iterator(); it.hasNext();) {
            ((Listener) it.next()).projectSaved(this);
        }
        _modified = false;
    }

    /**
     * Saves the latest project version prior calculation. Later on it is
     * compared to see whether we resume the same project.
     */
    public void saveLastVersion() {
        try {
            File dir = getLastProjectDir();
            dir.mkdirs();
            saveProject(dir, null);
        } catch (Exception e) {
        }
    }

    /**
     * Saves the project into specified directory.
     */
    public void saveProject(File dir, String name) {
        if (name == null) {
            name = _name;
        }
        Log.logMessage(this, SeverityLevel.INFO, false, "Saving project " + getName() + " in " + dir.getAbsolutePath());

        //Now in PanelPostProcess.writeResults
        //saveCases(new File(dir, CASE_LIST_FILE));
        //saveDesignSessions(new File(dir, DESIGN_SESSIONS_FILE));
        if (_plugin == null) {
            Log.logMessage(this, SeverityLevel.ERROR, false, "No plugin defined !");
        }
        //throw new IllegalArgumentException("No plugin defined !");
        if (_outputFunctions == null || _outputFunctions.size() < 1) {
            Log.logMessage(this, SeverityLevel.WARNING, false, "No output function defined !");
        }
        //throw new IllegalArgumentException("No output function defined !");
        //if (_outputNames == null) {
        //    System.err.println("No output available !");
        //}
        //throw new IllegalArgumentException("No output available !");

        //System.out.println("_plugin="+_plugin);
        //System.out.println("_functionExpression="+ASCII.cat(" ",_functionExpression));
        //System.out.println("_outputNames=" + ASCII.cat(" ", _outputNames));
        PrintStream ps = null;
        FileOutputStream fs = null;
        try {
            FileUtils.forceMkdir(dir);
            fs = new FileOutputStream(dir + File.separator + PROJECT_FILE);
            ps = new PrintStream(fs);
            ps.println("<?xml version=\"1.0\" encoding=\"" + ASCII.CHARSET + "\"?>\n"
                    + "<!DOCTYPE " + ELEM_FUNZ + " [\n"
                    + "  <!ENTITY eacute \"&#233;\">\n"
                    + "  <!ENTITY ldquo \"&#8220;\">\n"
                    + "  <!ENTITY rdquo \"&#8221;\">\n"
                    + "  <!ENTITY lsquo \"&#8216;\">\n"
                    + "  <!ENTITY rsquo \"&#8217;\">\n"
                    + "  <!ENTITY egrave \"&#232;\">\n"
                    + "  <!ENTITY agrave \"&#224;\">\n"
                    + "  <!ENTITY ugrave \"&#249;\">\n]>");

            StringBuilder ofl = new StringBuilder();
            if (_outputFunctions != null) {
                for (OutputFunctionExpression of : _outputFunctions) {
                    ofl.append(of.write()).append(LISTSEPARATOR);
                }
            }

            ps.println("<" + ELEM_FUNZ + "\n\t"
                    + ATTR_NAME + "=\"" + toHTML(name) + "\"\n\t"
                    + ATTR_TYPE + "=\"" + toHTML(_model) + "\"\n\t"
                    + ATTR_DESIGNER_ID + "=\"" + toHTML(_designerId) + "\"\n\t"
                    + ATTR_CODE + "=\"" + toHTML(_code) + "\"\n\t"
                    + ATTR_OUTFN + "=\"" + (_mainOutputFunctionIndex) + "\"\n\t"
                    + ATTR_OUTFN_LIST + "=\"" + (ofl.length() > 0 ? toHTML(ofl.substring(0, ofl.length() - LISTSEPARATOR.length())) : "") + "\"\n\t" /* + ATTR_OUTS + "=\"" + XMLConverter.toHTML(ASCII.cat(LISTSEPARATOR, _outputNames)) + "\"\n\t" */
                    + ATTR_PLUGIN + "=\"" + toHTML(_plugin != null ? _plugin.getID() : "") + "\"\n\t"
                    + ATTR_VAR_START + "=\"" + SyntaxRules.START_SYMBOLS[getVariableSyntax().getStartSymbolIdx()].NAME + "\"\n\t"
                    + ATTR_VAR_LIM + "=\"" + SyntaxRules.LIMITS[getVariableSyntax().getLimitsIdx()].NAME + "\"\n\t"
                    + ATTR_FRM_START + "=\"" + SyntaxRules.START_SYMBOLS[getFormulaSyntax().getStartSymbolIdx()].NAME + "\"\n\t"
                    + ATTR_FRM_LIM + "=\"" + SyntaxRules.LIMITS[getFormulaSyntax().getLimitsIdx()].NAME + "\"\n\t" + ">");

            for (String k : _tvalues.keySet()) {
                ps.println("\t<" + ELEM_TVALUE + " " + ATTR_KEY + "=\"" + k + "\" " + ATTR_VALUE + "=\"" + toHTML(_tvalues.get(k)) + "\"/>\n\t");
            }

            for (Iterator<InputFile> it = _inputfiles.iterator(); it.hasNext();) {
                (it.next()).save(ps);
            }

            for (Iterator<Variable> it = _vars.iterator(); it.hasNext();) {
                (it.next()).save(ps);
            }

            for (Iterator<VarGroup> it = _groups.iterator(); it.hasNext();) {
                (it.next()).save(ps);
            }

            for (Parameter p : _params) {
                ps.println("\t<" + ELEM_PARAM + " " + ATTR_NAME + "=\"" + p.getName() + "\" " + ATTR_TYPE + "=\"" + p.getParameterType() + "\" />");
            }

            org.funz.log.Alert.showInformation("DOE="+_doeOptions.toString());
            for (String designerId : _doeOptions.keySet()) {
                Map<String, String> op = _doeOptions.get(designerId);
                if (op != null && op.size() > 0) {
                    ps.print("\t<" + ELEM_DESIGNER_ID + " " + ATTR_NAME + "=\"" + designerId + "\">\n\t");
                    for (String key : op.keySet()) {
                        ps.print("\t\t<" + ELEM_OPTION + " " + ATTR_KEY + "=\"" + key + "\" " + ATTR_VALUE + "=\"" + toHTML(op.get(key)) + "\"/>\n\t");
                    }
                    ps.print("\t</" + ELEM_DESIGNER_ID + ">\n");
                }
            }

            ps.println("</" + ELEM_FUNZ + ">");
        } catch (IOException ioe) {
            Alert.showException(ioe);
        } finally {
            try {
                fs.close();
                ps.close();
            } catch (Exception ee) {
                ee.printStackTrace();
            }
        }

    }

    /*public void saveCasesAndDesignSessions(File dir) {
     saveDesignSessions(new File(dir, DESIGN_SESSIONS_FILE));
     saveCases(new File(dir, CASE_LIST_FILE));
     }*/
    public void saveCases(File file) {
        Log.logMessage(this, SeverityLevel.INFO, false, "Saving cases");
        if (_cases != null) {
            _cases.save(file);
        }
        Log.logMessage(this, SeverityLevel.INFO, false, "Cases saved.");
    }

    public void saveDesignSessions(File file) {
        Log.logMessage(this, SeverityLevel.INFO, false, "Saving design sessions");
        if (_designSessions == null) {
            return;
        }
        PrintStream ps = null;
        try {
            ps = new PrintStream(new FileOutputStream(file));
            ps.println("<?xml version=\"1.0\" encoding=\"" + ASCII.CHARSET + "\"?>");
            ps.println("<" + ELEM_SESSIONS + ">");
            for (DesignSession s : _designSessions) {
                s.save(ps);
                s.getDesign().saveNotebook();
            }
            ps.println("</" + ELEM_SESSIONS + ">");
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
                ps.close();
            } catch (Exception ee) {
                ee.printStackTrace();
            }
        }
        Log.logMessage(this, SeverityLevel.INFO, false, "Design sessions saved.");
    }

    public void moveCasesSpoolTo(File to, List<Case> cases) {
        if (cases != null) {
            for (Case c : cases) {
                try {
                    //LogUtils.tic("prj.getSpoolDir(), c.getRelativePath()).exists()");
                    if (new File(getSpoolDir(), c.getRelativePath()).exists()) {

                        File archiveCasePath = new File(to, c.getRelativePath());
                        if (archiveCasePath.exists()) {
                            File[] inspool = new File(getSpoolDir(), c.getRelativePath()).listFiles();
                            for (File f : inspool) {
                                String f_from_spool = f.getAbsolutePath().replace(getSpoolDir().getAbsolutePath(), "");
                                for (File o : archiveCasePath.listFiles()) {
                                    String o_from_spool = o.getAbsolutePath().replace(archiveCasePath.getAbsolutePath(), "");
                                    if (o_from_spool.equals(f_from_spool)) {
                                        File oo = o.getAbsoluteFile();
                                        while (o.getAbsolutePath().equals(f.getAbsolutePath())) {
                                            oo = new File(oo.getParentFile(), oo.getName() + ".old");
                                            Log.out("Move previous file/dir " + o + " to .old to avoid overwriting.", 2);
                                        }
                                        if (o.isFile()) {
                                            Disk.moveFile(o, oo);
                                        } else {
                                            Disk.moveDir(o, o);
                                        }
                                    }
                                }
                            }
                            //Disk.removeDir(archiveCasePath); No, problem when superimposing many projects in same directory
                        }
                        Disk.moveDir(new File(getSpoolDir(), c.getRelativePath()), archiveCasePath);
                    } else {
                        Log.out("Case spool dir " + new File(getSpoolDir(), c.getRelativePath()) + " does not exists.", 1);
                    }
                    //LogUtils.toc("prj.getSpoolDir(), c.getRelativePath()).exists()");
                } catch (Exception e) {
                    Log.err("Could not move case " + c.getName() + " in dir " + new File(getSpoolDir(), c.getRelativePath()) + " to results dir " + to + ":\n" + e.getLocalizedMessage(), 3);
                }
            }
        }
    }

    public void loadCases() throws Exception {
        Log.logMessage(this, SeverityLevel.INFO, false, "cases.xml loaded.");
        File cfile = new File(_dir, CASE_LIST_FILE);
        if (cfile.exists()) {
            loadCases(cfile);
        }
    }

    public void loadDesignSessions() throws Exception {
        resetDiscreteCases(null);
        File dsfile = new File(_dir, DESIGN_SESSIONS_FILE);
        if (dsfile.exists()) {
            loadDesignSessions(dsfile);
        }
    }

    private void loadCases(File file) throws Exception {
        Log.logMessage(this, SeverityLevel.INFO, false, "loading cases " + file);
        CaseList cases = new CaseList();
        cases.load(file, this);
        if (cases == null) {
            throw new IllegalArgumentException("Failed loading cases.");
        }
        _cases = cases;
        intermediate = _cases.get(0).getIntermediateValues(); //if not reset intermediate values, none will be found when project new loading without input file parsing        
        //_disCases = resetDiscreteCases(null);
    }

    private void loadDesignSessions(File file) {
        Log.logMessage(this, SeverityLevel.INFO, false, "loading sessions " + file);
        try {
            Document d = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
            Element e = d.getDocumentElement();
            if (!e.getTagName().equals(ELEM_SESSIONS)) {
                throw new Exception("wrong XML element " + e.getTagName() + " in file " + file);
            }
            NodeList ss = e.getElementsByTagName(ELEM_SESSION);
            for (int i = 0; i < ss.getLength(); i++) {
                Element el = (Element) ss.item(i);
                int discid = Integer.parseInt(el.getAttribute(ATTR_IDX));
                _designSessions.add(discid, new DesignSession(el, discid));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets the code name.
     */
    public void setCode(String code) {
        _code = code;
        //modified();
    }

    public void setDesigner(Designer d) {
        _designer = d;
        if (d == null) {
            _designerId = DesignConstants.NODESIGNER_ID;
        } else {
            _designerId = d.getName();
            _designer.setProject(this);
        }
    }

    public Designer getDesigner() {
        return _designer;
    }

    public void setDesignerId(String id) {
        _designerId = id;
    }

    public String getDesignerId() {
        return _designerId;
    }

    public LinkedList<OutputFunctionExpression> getOutputFunctionsList() {
        return _outputFunctions;
    }

    public void setDesign(Design d, int discCaseIDx) throws Exception {
        DesignSession ds = (DesignSession) getDesignSession(discCaseIDx);
        if (ds != null) {
            ds.setDesign(d);
        } else {
            throw new Exception("Project.setDesigner: no DesignSession");
        }
    }

    public Design getDesign(int discCaseIDx) {
        DesignSession ds = (DesignSession) getDesignSession(discCaseIDx);
        return ds == null ? null : ds.getDesign();
    }

    /**
     * Sets the variable syntax rules
     */
    public void setVariableSyntax(SyntaxRules syn) {
        _plugin.setVariableSyntax(syn.getStartSymbolIdx(), syn.getLimitsIdx());
    }

    /**
     * Sets the formula syntax rules
     */
    public void setFormulaSyntax(SyntaxRules syn) {
        _plugin.setFormulaSyntax(syn.getStartSymbolIdx(), syn.getLimitsIdx());
    }

    /**
     * Modifies the project name. Be sure to save the project properly.
     */
    public void setName(String name) {
        _name = name;
    }

    /**
     * Sets the output function
     */
    public void setMainOutputFunction(OutputFunctionExpression _ofe) {
        if (_ofe == null) {
            _mainOutputFunctionIndex = -1;
            modified();
            return;
        }

        if (_outputFunctions == null) {
            _outputFunctions = new LinkedList<OutputFunctionExpression>();
        }

        if (_outputFunctions.contains(_ofe)) {
            _mainOutputFunctionIndex = _outputFunctions.indexOf(_ofe);
        } else {
            _outputFunctions.add(_ofe);
            _mainOutputFunctionIndex = _outputFunctions.size() - 1;
        }

        modified();
    }

    /**
     * Sets the output fields names
     */
    /*private void setOutputNames(String[] outputNames) {
     _outputNames = outputNames;
     modified();
     }*/
    /**
     * Registers a parameter list listener. If not null it will be notified each
     * time the parameter list is regenerated.
     */
    public void setParameterListListener(ParameterListListener pll) {
        _parameterListListener = pll;
        if (_parameterListListener != null) {
            _parameterListListener.parameterListModified();
        }
    }

    /**
     * Registers the project type.
     */
    public void setPlugin(IOPluginInterface plugin) {
        //System.out.println("setPlugin " + plugin + " > " + plugin.getSource());
        _plugin = plugin;

        _plugin.setProject(this);

        cleanOutputs();

        modified();
    }

    /**
     * Registers the tagged value list.
     */
    public void setTaggedValues(Map<String, String> tvalues) {
        _tvalues = tvalues;
        if (_tvalues == null) {
            _tvalues = new HashMap<String, String>();
        }
    }

    /**
     * Registers the tagged value list.
     */
    public void setTaggedValue(String key, String val) {
        if (val != null && key != null) {
            _tvalues.put(key, val);
        }
    }

}
