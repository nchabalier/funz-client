package org.funz.parameter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.exec.OS;
import org.apache.commons.io.FileUtils;
import org.funz.Project;
import static org.funz.XMLConstants.*;
import org.funz.conf.Configuration;
import org.funz.doeplugin.DesignSession;
import org.funz.doeplugin.Experiment;
import org.funz.log.Log;
import org.funz.log.LogCollector.SeverityLevel;
import org.funz.util.ASCII;
import static org.funz.util.Data.*;
import org.funz.util.Disk;
import org.funz.util.Format;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Single calculation case coresponding to a unique parameter combination. Case
 * may contain a mixture of descrete and continous parameters.
 */
public class Case extends Experiment {

    private Map<String, Object> _result;

    @Override
    public String toString() {
        return super.toString() + " (" + STATE_STRINGS[_state] + ")";
    }

    public DesignSession getDesignSession() {
        return _discreteId >= 0 ? prj.getDesignSession(_discreteId) : null;
    }

    /**
     * @return the _reserver
     */
    public Reserver getReserver() {
        return _reserver;
    }

    public void reset() {
        _retries = 0;
        setState(Case.STATE_INTACT);
        reserve(null);
        try {
            Disk.removeDir(prj.getCaseTmpDir(this));
        } catch (IOException ee) {
            ee.printStackTrace();
        }
    }

    public String getStatusInformation() {
        return _info;
    }

    /**
     * Describe a parameter position within the tree. Each Case corresponds to a
     * unique combination of Node structures. The number of nodes is exactly the
     * same as the number of parameters.
     */
    public static class Node implements Cloneable {

        public final String name;
        String paramName;
        String paramValue;
        public final static String GROUP_SEPARATOR = ",";
        boolean isSINGLE_PARAM = false;
        boolean isGroup = false;
        String[] grpVarsName;
        String[] grpVarsValue;
        int grpSize = 0;

        @Override
        public String toString() {
            return name + ": " + getParamName() + "=" + getParamValue();
        }

        public int getGrpSize() {
            return grpSize;
        }

        /**
         * Constructs a parameter node.
         *
         * @param index position within a parameter values (if < 0, parameter
         * value is continous and assigned by Designer) @param name value of the
         * parameter
         */
        public Node(/*int index,*/String name, Project prj) {
            //idx = index;
            this.name = name;

            assert name.contains("=") || name.equals(Project.SINGLE_PARAM_NAME) : "Problem creating node " + name;

            if (name.contains("=")) {
                paramName = name.substring(0, name.indexOf("="));
                paramValue = name.substring(name.indexOf("=") + 1);
                if (name.contains(GROUP_SEPARATOR)) {
                    isGroup = true;
                    grpVarsName = paramName.split(GROUP_SEPARATOR);//TODO: erreur d'architecture... à repenser totalement
                    grpVarsValue = paramValue.split(GROUP_SEPARATOR);
                    assert grpVarsName.length == grpVarsValue.length : "size of " + ASCII.cat(",", grpVarsName) + " and " + ASCII.cat(",", grpVarsValue) + " not consistent";
                    grpSize = grpVarsValue.length;
                }

                if (name.startsWith("@")) {
                    VarGroup g = prj.getGroupByName(paramName.substring(1));
                    assert g != null : "Group " + paramName.substring(1) + " not found in project " + prj;
                    isGroup = true;
                    grpVarsName = g.getVariablesName();
                    grpVarsValue = new String[grpVarsName.length];
                    for (int i = 0; i < g.getAlias().size(); i++) {
                        String aliasval = g.getAlias().get(i);
                        if (aliasval.equals(paramValue)) {
                            for (int j = 0; j < g.getVariables().size(); j++) {
                                grpVarsValue[j] = g.getVariables().get(j).getValueAt(i);
                            }
                            break;
                        }
                    }
                    assert grpVarsValue[0].length() > 0 : "Group alias " + paramValue + " not found";
                    grpSize = grpVarsValue.length;
                }

            } else {
                assert name.equals(Project.SINGLE_PARAM_NAME) : "Invalid node name:" + name;
                isSINGLE_PARAM = true;
            }

            //System.out.println("new Node(" /*+ index + ","*/ + name + ")");
        }

        @Override
        public boolean equals(Object other) {
            return other == this || (other != null && ((Node) other).name.equals(name));
        }

        public String getParamValue() {
            if (isSINGLE_PARAM) {
                return "No value";
            }
            return paramValue;
        }

        public String getParamName() {
            if (isSINGLE_PARAM) {
                return "No name";
            }
            return paramName;
        }

        public boolean isGroup() {
            return isGroup;
        }

        public boolean isSingle() {
            return isSINGLE_PARAM;
        }

        public String getGroupVarValue(int i) {
            assert (isGroup) : "Problem getGroupVarValue " + i + " " + name;
            if (isSINGLE_PARAM) {
                return "No value";
            }
            return grpVarsValue[i];
        }

        public String getGroupVarName(int i) {
            assert (isGroup) : "Problem getGroupVarName " + i + " " + name;
            if (isSINGLE_PARAM) {
                return "No name";
            }
            return grpVarsName[i];
        }
    }

    /**
     * Case observer interface.
     */
    public static interface Observer {

        /**
         * Called each time the case is modified.
         *
         * @param index modified case
         * @param what modification type
         */
        public void caseModified(final int index, final int what);
    }

    /**
     * Case reserver callback interface.
     */
    public static interface Reserver {

        /**
         * Supplies the reserver name.
         */
        public String getReserverName();

        public Object getReserver();
    }

    /**
     * Used to manage case tree. Useful for post-processing to access
     * directories.
     */
    public static class TreeNode {

        public LinkedList children;
        public Case leafCase;
        public String name;
        public Parameter parameter;
        public TreeNode parent;

        public TreeNode() {
            name = "root";
        }

        public TreeNode(String n, TreeNode p, Parameter param) {
            p.addChild(this);
            name = n;
            parameter = param;
        }

        public TreeNode(String n, TreeNode p, Parameter param, Case c) {
            p.addChild(this);
            leafCase = c;
            //c.setSelected(false);
            name = n;
            parameter = param;
        }

        void addChild(TreeNode ch) {
            if (children == null) {
                children = new LinkedList();
            }
            children.add(ch);
            ch.parent = this;
        }

        public int getType() {
            return parameter.getParameterType();
        }
    }
    public static final String FILE_INFO = "info.txt";
    /**
     * Modification type.
     */
    //public static final int MODIFIED_CALC = 0, MODIFIED_STATE = 1, MODIFIED_TIME = 2, MODIFIED_INFO = 3;    //MODIFIED_START = 2,
    //MODIFIED_END   = 3,
    //MODIFIED_TIME = 4,
    //MODIFIED_INFO = 5;
    public static final int MODIFIED_CALC = 3, MODIFIED_STATE = 0, MODIFIED_TIME = 2, MODIFIED_INFO = 1;
    public static String[] MODIFIED_STRINGS = {"state", "info", "time", "calc"};
    public static final String PROP_CODE = "code", PROP_START = "start", PROP_END = "end", PROP_DURATION = "duration", PROP_STATE = "state", PROP_CALC = "calc", PROP_VAR = "input", PROP_OUT = "output", PROP_INTER = "intermediate";
    public static final String[] STATE_HTML_STRINGS = {"intact", "<font size=-1 color=red>failed</font>",
        "<font size=-1 color=red>error</font>", "preparing", "<font size=-1 color=green>running</font>",
        "<font size=-1 color=blue>done</font>"
    };
    public static final int STATE_INTACT = 0, STATE_FAILED = 1, STATE_ERROR = 2, STATE_PREPARING = 3, STATE_OVER = 5, STATE_RUNNING = 4;
    public static final String[] STATE_STRINGS = {"intact", "failed", "error", "preparing", "running", "done"};
    public static final int[] STATE_ORDER = {0, 3, 4, 5, 1, 2};
    static NumberFormat f;
    static FieldPosition fp;

    static {
        f = NumberFormat.getIntegerInstance();
        f.setMinimumIntegerDigits(2);
        fp = new java.text.FieldPosition(0);
    }

    public static String longToDurationString(long time) {
        StringBuffer sb = new StringBuffer();
        long sec = time / 1000L;
        if (sec > 1000000) {
            return "?";
        }
        sb = f.format(sec / 3600L, sb, fp);
        sb.append(':');
        sb = f.format((sec % 3600L) / 60L, sb, fp);
        sb.append(':');
        sb = f.format(sec % 60L, sb, fp);
        return sb.toString();
    }

    public static String longToTimeString(long time) {
        StringBuffer sb = new StringBuffer();
        sb = new SimpleDateFormat("HH:mm:ss").format(new Date(time), sb, new java.text.FieldPosition(0));
        return sb.toString();
    }

    public int getDiscreteCaseId() {
        return _discreteId;
    }

    public boolean isCacheChecked() {
        return _cacheIsChecked;
    }

    public void setCacheIsChecked() {
        _cacheIsChecked = true;
    }

    public void incTriesDone() {
        _retries = _retries + 1;
    }

    public int getTriesDone() {
        return _retries;
    }

    public boolean retryIfFailed() {
        return _retryIfFailed;
    }

    public void setRetryIfFailed(boolean retryIfFailed) {
        this._retryIfFailed = retryIfFailed;
    }
    /*public void setDiscreteCaseId(int id) {
     _discreteId = id;
     }*/
    private String _calcName = "", _start = "", _end = "";
    private final int _index;
    String _info;// = "?";
    Map<String, Object> _inter;
    Map<String, Object> _output;
    private long _lstart, _lend;
    private final String _name;
    private Node _nodes[];
    private Observer _observer;
    private volatile Reserver _reserver = null;
    private volatile boolean _selected = true;
    private volatile boolean _cacheIsChecked = false;
    private volatile int _retries = 0;
    private volatile boolean _retryIfFailed = true;
    //private int _selectedIndex = -1;
    private /*volatile*/ int _state = STATE_INTACT;
    // index within the descrete tree of cases
    private int _discreteId = -1;

    /**
     * Build a case from xml dom element
     *
     */
    public Case(Element elem, Project prj) {
        super(prj);
        _index = Integer.parseInt(elem.getAttribute(ATTR_IDX));
        NodeList nodes = elem.getElementsByTagName(ELEM_CASE_NODE);
        _nodes = new Node[nodes.getLength()];
        for (int i = 0; i < nodes.getLength(); i++) {
            Element n = (Element) nodes.item(i);
            _nodes[i] = new Node(/*Integer.parseInt(n.getAttribute(ATTR_IDX)), */n.getAttribute(ATTR_NAME), prj);
        }
        _name = buildName();
        _calcName = elem.getAttribute(ATTR_CALC_NAME);
        setStart(Long.parseLong(elem.getAttribute(ATTR_START)));
        setEnd(Long.parseLong(elem.getAttribute(ATTR_END)));
        _state = Integer.parseInt(elem.getAttribute(ATTR_STATE));
        _discreteId = Integer.parseInt(elem.getAttribute(ATTR_DISC_CASE_ID));

        NodeList nodesi = elem.getElementsByTagName(ELEM_CASE_INTER);
        HashMap<String, Object> inter = new HashMap<String, Object>();
        for (int i = 0; i < nodesi.getLength(); i++) {
            Element n = (Element) nodesi.item(i);
            Object o = asObject(Format.fromHTML(n.getTextContent()));
            //System.out.println(n.getNodeName()+":\n"+XMLConverter.fromHTML(n.getTextContent())+ "\n -> \n"+o);
            inter.put(n.getAttribute(ATTR_NAME), o);
        }
        _inter = inter;

        NodeList nodeso = elem.getElementsByTagName(ELEM_CASE_OUT);
        Map<String, Object> _hinfo = new HashMap<String, Object>();
        for (int i = 0; i < nodeso.getLength(); i++) {
            Element n = (Element) nodeso.item(i);
            Object o = asObject(Format.fromHTML(n.getTextContent()));
            //System.out.println(n.getNodeName()+":\n"+XMLConverter.fromHTML(n.getTextContent())+ "\n -> \n"+o);
            _hinfo.put(n.getAttribute(ATTR_NAME), o);
        }
        _info = _hinfo.toString();
    }

    /**
     * Constructs a case corresponding to a parameters combination (discrete
     * case).
     *
     * @param idx case position
     * @param nodes parameters combination
     */
    public Case(int idx, Node nodes[], Project prj) {
        super(prj);
        _index = idx;
        _nodes = nodes;
        _name = buildName();
        _discreteId = _index;
    }

    /* Construct a case comming from Designer
     */
    public Case(int idx, Experiment exp, Case parentDiscreteCase, Project prj) {
        super(prj);
        _index = idx;
        //_selectedIndex = _index;
        _discreteId = parentDiscreteCase.getDiscreteCaseId();

        // add parent nodes only if not all-in-one
        int parentnodes = parentDiscreteCase.getNmOfNodes();
        if (parentDiscreteCase._nodes.length == 1 && parentDiscreteCase._nodes[0].isSingle()) {
            _nodes = new Node[exp.getNmOfParameters()];
            parentnodes = 0;
        } else {
            _nodes = new Node[exp.getNmOfParameters() + parentDiscreteCase.getNmOfNodes()];
            for (int i = 0; i < parentDiscreteCase.getNmOfNodes(); i++) {
                _nodes[i] = parentDiscreteCase.getNode(i);
            }
        }

        for (int i = 0; i < _nodes.length - parentnodes; i++) {
            assert exp.getValueExpression(i) != null && exp.getValueExpression(i).length() > 0 : "Problem with experiment value expression " + i + "\n" + exp.toString();
            Node n = new Node(/*-1,*/exp.getValueExpression(i), prj);
            _nodes[i + parentnodes] = n;
        }
        _name = buildName();
    }

    private String buildName() {
        // contrcut this case name as "param0value/param1value/param2value/..."
        StringBuilder sb = new StringBuilder();
        sb.append(_nodes[0].name);
        for (int i = 1; i < _nodes.length; i++) {
            sb.append("/").append(_nodes[i].name);
        }
        return sb.toString();
    }

    void save(PrintStream ps) {
        ps.println("<" + ELEM_CASE + " "
                + ATTR_IDX + "=\"" + _index + "\" "
                + ATTR_CALC_NAME + "=\"" + Format.toHTML(_calcName) + "\" "
                + ATTR_START + "=\"" + _lstart + "\" "
                + ATTR_END + "=\"" + _lend + "\" "
                + ATTR_STATE + "=\"" + _state + "\" "
                + ATTR_DISC_CASE_ID + "=\"" + _discreteId + "\" "
                + ">");
        StringBuilder vars = new StringBuilder();
        for (Node n : _nodes) {
            ps.println("\t<" + ELEM_CASE_NODE + " "
                    + ATTR_IDX + "=\"" + /*n.idx +*/ "\" "
                    + ATTR_NAME + "=\"" + Format.toHTML(n.name) + "\" "
                    + "/>");
            if (n.isGroup) {
                VarGroup g = prj.getGroupByName(n.paramName);
                for (int i = 0; i < n.grpVarsName.length; i++) {
                    vars.append("\t<").append(ELEM_CASE_IN).append(" " + ATTR_NAME + "=\"").append(Format.toHTML(n.grpVarsName[i])).append("\" " + ">");
                    vars.append(Format.toHTML(asString(n.grpVarsValue[i])));
                    vars.append("</").append(ELEM_CASE_IN).append(">\n");
                }
            } else {
                vars.append("\t<").append(ELEM_CASE_IN).append(" " + ATTR_NAME + "=\"").append(Format.toHTML(n.paramName)).append("\" " + ">");
                vars.append(Format.toHTML(asString(n.paramValue)));
                vars.append("</").append(ELEM_CASE_IN).append(">\n");
            }
        }
        ps.println(vars.toString());

        if (_inter != null) {
            for (String i : _inter.keySet()) {
                ps.print("\t<" + ELEM_CASE_INTER + " "
                        + ATTR_NAME + "=\"" + Format.toHTML(i) + "\" "
                        + ">");
                if (_inter.get(i) != null) {
                    ps.print(Format.toHTML(asString(_inter.get(i))));
                }
                ps.println("</" + ELEM_CASE_INTER + ">");
            }
        }

        if (_output != null) {
            for (String i : _output.keySet()) {
                ps.print("\t<" + ELEM_CASE_OUT + " "
                        + ATTR_NAME + "=\"" + Format.toHTML(i) + "\" "
                        + ">");
                if (_output.get(i) != null) {
                    ps.print(Format.toHTML(asString(_output.get(i))));
                }
                ps.println("</" + ELEM_CASE_OUT + ">");
            }
        }

        ps.println("</" + ELEM_CASE + ">");
    }

    // Says whether it comes from Designer
    /*public boolean isExperiment() {
     return _isExperiment;//_nodes[0].idx < 0;
     }*/
    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (other == this) {
            return true;
        }

        Node nodes[] = ((Case) other)._nodes;
        if (nodes.length == _nodes.length) {
            for (int i = 0; i < _nodes.length; i++) {
                if (!nodes[i].equals(_nodes[i])) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }

    /**
     * returns the calculator name
     */
    public String getCalculatorName() {
        return _calcName;
    }

    /**
     * Returns the calculation duration in milliseconds.
     */
    public long getDuration() {
        if (_lstart == 0) {
            return -1;
        }
        if (_lend < _lstart) {
            return System.currentTimeMillis() - _lstart;
        }
        return _lend - _lstart;
    }

    /**
     * Returns a string representation of the calculation duration.
     */
    public String getDurationString() {
        long d = getDuration();
        if (d < 0) {
            return "-";
        }
        return longToDurationString(d);
    }

    /**
     * Returns the calculation end time moment in milliseconds.
     */
    public long getEnd() {
        return _lend;
    }

    /**
     * Returns a string representation of the calculation end time moment.
     */
    public String getEndString() {
        return _end;
    }

    /**
     * Returns the case index within the project's case array
     */
    public int getIndex() {
        return _index;
    }

    public Map<String, Object> getIntermediateValues() {
        return _inter;
    }

    public Map<String, Object> getOutputValues() {
        return _output;
    }

    public Map<String, Object> getResult() {
        return _result;
    }

    public Map<String, Object> getInputValues() {
        HashMap<String, Object> in = new HashMap<String, Object>();
        for (int i = 0; i < _nodes.length; i++) {
            Node n = _nodes[i];
            if (n.isSingle()) {
                //in.put(n.getParamName(), n.getParamValue());
            } else if (!n.isGroup()) {
                Variable v = prj.getVariableByName(n.getParamName());
                if (v != null) {
                    in.put(v.getName(), v.castValue(n.getParamValue()));
                } else {
                    in.put(n.getParamName(), "?");
                    Log.logException(true, new Exception("Cannot find variable " + n.getParamName() + " in " + prj.getVariables()));
                }
            } else {
                VarGroup g = prj.getGroupByName(n.getParamName());
                if (g != null) {
                    in.put(g.getName(), n.getParamValue());
                } else {
                    in.put(n.getParamName(), n.getParamValue());
                    //Configuration.logException(true, new Exception("Cannot find group " + n.getParamName()));
                }
                for (int j = 0; j < n.grpSize; j++) {
                    Variable v = prj.getVariableByName(n.getGroupVarName(j));
                    if (v != null) {
                        in.put(v.getName(), v.castValue(n.getGroupVarValue(j)));
                    }
                }
            }
        }
        return in;
    }

    class ProxyExperiment extends Experiment {

        Case src;

        public DesignSession getDesignSession() {
            return src.getDesignSession();
        }

        public ProxyExperiment(Case source) {
            super(source.prj);
            src = source;
        }

        @Override
        public String getValueExpression(int paramIdx) {
            return src.getValueExpression(src.prj.getContinuousParameters().get(paramIdx).getIndex());
        }

        @Override
        public int getNmOfParameters() {
            return src.prj.getContinuousParameters().size();
        }

        @Override
        public Map<String, Object> getOutputValues() {
            return src.getOutputValues();
        }

        @Override
        public Map<String, Object> getInputValues() {
            return src.getInputValues();
        }

        @Override
        public Map<String, Object> getIntermediateValues() {
            return src.getIntermediateValues();
        }
    }

    public Experiment getContinuousExperiment() {
        if (continousProxy == null) {
            continousProxy = new ProxyExperiment(this);
        }
        return continousProxy;
    }
    Experiment continousProxy;

    /**
     * Returns the case name. Somewhat like
     * "/param1value/param2value/param3value" etc
     */
    public String getName() {
        return _name;
    }

    /*private LinkedList<String> getAllCaseNodes() {
     LinkedList<String> sb = new LinkedList<String>();
    
     for (int i = 0; i < _nodes.length; i++) {
     if (!_nodes[i].isSingle()) {
     sb.add(_nodes[i].name);
     }
     }
     return sb;
     }*/
    String _relativePath = null;
    volatile static MessageDigest digest = null;

    static synchronized String digest(String in) {
        if (digest == null) {
            try {
                digest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        return Configuration.toHexString(digest.digest(in.getBytes()));
    }
    public static int MAX_PATH_LENGTH = Integer.parseInt(Configuration.getProperty("max_path_length", OS.isFamilyWindows() ? "64" : "256"));

    String cont_path, disc_path, discgrp_path, cont_hash, disc_hash, discgrp_hash;
    public String getRelativePath() {
        if (_relativePath == null) {
            StringBuilder discgrp = new StringBuilder();

            StringBuilder disc = new StringBuilder();

            StringBuilder cont = new StringBuilder();

            LinkedList<String> ns = new LinkedList<String>();
            for (int i = 0; i < _nodes.length; i++) {
                if (!_nodes[i].isSingle()) {
                    ns.add(_nodes[i].name);
                }
            }

            for (String n : ns) {
                String nodename = n.substring(0, n.indexOf("="));
                Variable v = prj.getVariableByName(nodename);
                if (v != null) {
                    if (v.isContinuous()) {
                        //System.out.println(" Variable " + n + " is cont");
                        cont.append(File.separator);
                        cont.append(n);
                    } else {
                        //System.out.println(" Variable " + n + " is disc");
                        disc.append(File.separator);
                        disc.append(n);
                    }
                } else {
                    //System.out.println(" Variable " + n + " is disc");
                    discgrp.append(File.separator);
                    discgrp.append(n);
                }
            }

            cont_path = cont.toString();
            cont_hash = cont_path;
            
            disc_path = disc.toString();
            disc_hash = disc_path;

            discgrp_path = discgrp.toString();
            discgrp_hash = discgrp_path;

            _relativePath = discgrp_hash + disc_hash + cont_hash;

            if (_relativePath.length() > MAX_PATH_LENGTH) {
                if (cont_hash.length() > 32) {//to support MAX_PATH OS variabel : 260 in Windows, 4096 in unix (by default)
                    cont_hash = File.separator + digest(cont_hash);
                    Log.logMessage(this, SeverityLevel.INFO, false, "Using digest path:\n  " + cont.toString() + " becomes\n  " + cont_hash);
                    _relativePath = discgrp_hash + disc_hash + cont_hash;
                }
            }

            if (_relativePath.length() > MAX_PATH_LENGTH) {
                if (discgrp_hash.length() > 32) {
                    discgrp_hash = File.separator + digest(discgrp_hash);
                    Log.logMessage(this, SeverityLevel.INFO, false, "Using digest path:\n  " + discgrp.toString() + " becomes\n  " + discgrp_hash);
                    _relativePath = discgrp_hash + disc_hash + cont_hash;
                }
            }

            if (_relativePath.length() > MAX_PATH_LENGTH) {
                if (disc_hash.length() > 32) {
                    disc_hash = File.separator + digest(disc_hash);
                    Log.logMessage(this, SeverityLevel.INFO, false, "Using digest path:\n  " + disc.toString() + " becomes\n  " + disc_hash);
                    _relativePath = discgrp_hash + disc_hash + cont_hash;
                }
            }

            //if (_relativePath.length() > MAX_PATH_LENGTH) {
            //    MessageDialog.showInformation("Warning, length of case " + _relativePath + "might bee too long. Try to reduce path length.");
            //}
        }
        return _relativePath;
    }

    public static void main(String[] args) {
        int size = 2001;
        StringBuilder pathtest = new StringBuilder();
        for (int i = 0; i < size; i++) {
            pathtest.append((int) (Math.random() * 10));
        }
        String _relativePath = pathtest.toString();

        System.out.println("Path is " + _relativePath);
        System.out.println("Length is " + _relativePath.length());

        if (_relativePath.length() > 32) {//to support MAX_PATH OS variabel : 260 in Windows, 4096 in unix (by default)
            _relativePath = digest(_relativePath);
        }

        System.out.println("Path is " + _relativePath);
    }

    /**
     * Returns the number of nodes.
     */
    public int getNmOfNodes() {
        return _nodes.length;
    }

    /**
     * Retuns the node in position pos.
     */
    public Node getNode(int pos) {
        return _nodes[pos];
    }

    public int getNmOfParameters() {
        return _nodes.length;
    }

    public String getValueExpression(int paramIdx) {
        /*System.out.print("Case.getValueExpression(" + paramIdx + ") Nodes:");
         for (Node n : _nodes) {
         System.out.print(n.name + " - ");
         }
         System.out.print(" > " + _nodes[paramIdx].name + "\n");*/

        return _nodes[paramIdx].name;
    }

    public String getParamValue(int paramIdx) {
        return _nodes[paramIdx].getParamValue();
    }

    public String getParamName(int paramIdx) {
        return _nodes[paramIdx].getParamName();
    }

    /**
     * Returns the node list.
     */
    public Node[] getNodes() {
        return _nodes;
    }

    /**
     * Returns the calculation start time moment in milliseconds.
     */
    public long getStart() {
        return _lstart;
    }

    /**
     * Returns a string representation of the calculation start time moment.
     */
    public String getStartString() {
        return _start;
    }

    /**
     * Returns the case state type.
     */
    public int getState() {
        return _state;
    }

    /**
     * Returns an html representation of the current state
     */
    public String getStateHTMLString() {
        return STATE_HTML_STRINGS[_state];
    }

    /**
     * Returns the sate string.
     */
    public String getStateString() {
        return STATE_STRINGS[_state];
    }

    /**
     * Says whether the calculation is over or not.
     */
    public boolean hasRun() {
        return _state == STATE_OVER || _state == STATE_FAILED;
    }

    public boolean isOver() {
        return _state == STATE_OVER;
    }

    public boolean isFailed() {
        return _state == STATE_FAILED;
    }

    public boolean isError() {
        return _state == STATE_ERROR;
    }

    /**
     * Says whether the case is reserved or not.
     */
    public synchronized boolean isReserved() {
        return _reserver != null;
    }

    /**
     * Says whether the case is selected for calculation or not
     */
    public boolean isSelected() {
        return _selected;
    }

    /**
     * Notifies the observer about a modification.
     */
    public void modified(int what, String from) {
        //System.err.println(Calendar.getInstance().getTimeInMillis() + " from " + from);
        //System.err.println("Case.modified> " + what + " <MODIFIED_CALC = 0, MODIFIED_STATE = 1, MODIFIED_TIME = 2, MODIFIED_INFO = 3;>");
        if (_observer != null) {
            _observer.caseModified(_index, what);
        }
    }

    /**
     * Reads the execution information after a crash. This is called only if the
     * application tries to rerun a crashed project.
     */
    public boolean fromInfoFile(File f) {
        try {
            Properties prop = new Properties();
            FileInputStream fis = new FileInputStream(f);
            prop.load(fis);
            fis.close();
            long start = Long.parseLong(prop.getProperty(PROP_START, "0"));
            long end = Long.parseLong(prop.getProperty(PROP_END, "0"));
            int state = Integer.parseInt(prop.getProperty(PROP_STATE, "0"));
            _calcName = prop.getProperty(PROP_CALC, "");
            Observer o = _observer;
            _observer = null;
            setStart(start);
            setEnd(end);
            setState(state);
            _observer = o;
        } catch (Exception e) {
            // failed
            return false;
        }
        return true;
    }

    /**
     * Reserves the case for a calculator.
     */
    public synchronized boolean reserve(Reserver reserver) {
        //System.err.print(getName()+" reserve ...\n  reserver="+reserver+"\n  _reserver="+_reserver+"\n");
        if (reserver != null && _reserver != null) {
            return false;
        }
        _reserver = reserver;
        if (_reserver != null) {
            setState(STATE_PREPARING);
            _calcName = _reserver.getReserverName();
            modified(MODIFIED_CALC, "reserve");
            return true;
        }
        return false;
        //System.out.println("  ok");
    }

    /**
     * Modifies the calculation end time.
     */
    public void setEnd(long end) {
        _lend = end;
        _end = _lend == 0L ? "" : longToTimeString(end);
        modified(MODIFIED_TIME, "setEnd");
    }

    /**
     * Sets the information string.
     */
    public void setInformation(String info) {
        setSilentlyInfo(info);
        modified(MODIFIED_INFO, "setInformation " + info);
    }

    public void setOutputValues(Map<String, Object> output) {
        _output = output;
        modified(MODIFIED_INFO, "setOutputValues");
    }

    public void setResult(Map<String, Object> result) {
        _result = result;
        modified(MODIFIED_INFO, "setResult");
    }

    public void setIntermediateValues(Map<String, Object> inter) {
        _inter = inter;
        modified(MODIFIED_INFO, "setIntermediateValues");
    }

    public void addIntermediateValues(Map<String, Object> inter) {
        //System.err.println("Case.setInfo> " + object);
        if (_inter == null) {
            _inter = new HashMap<String, Object>();
        }
        _inter.putAll(inter);
        modified(MODIFIED_INFO, "addIntermediateValues");
    }

    /**
     * Registers the case observer.
     */
    public void setObserver(Observer o) {
        _observer = o;
    }

    public Observer getObserver() {
        return _observer;
    }

    /**
     * Marks the case as selected or not.
     */
    public void setSelected(boolean selected) {
        _selected = selected;
    }

    /**
     * Sets the index of the case if it is selected and not all cases are
     * displayed
     */
    /*public void setSelectedIndex(int selectedIndex) {
     _selectedIndex = selectedIndex;
     }*/
    /**
     * Sets the information string without the observer notification.
     */
    public void setSilentlyInfo(String info) {
        //System.err.println(getName() + " > " + info);
        _info = info;
    }

    /**
     * Modifies the state without notification of the observer
     */
    synchronized public void setSilentlyState(int state) {
        _state = state;
    }

    /**
     * Modifies the calculation start time.
     */
    public void setStart(long start) {
        _lstart = start;
        _start = _lstart == 0L ? "?" : longToTimeString(start);
        modified(MODIFIED_TIME, "setStart");
    }

    /**
     * Modifies the state.
     */
    synchronized public void setState(int state) {
        _state = state;
        modified(MODIFIED_STATE, "setState");
    }

    public static Properties readInfoFile(File f) {
        Properties info = new Properties();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(f);
            info.load(fis);
        } catch (Exception e) {
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ex) {
                }
            }
        }
        return info;
    }

    /**
     * Writes an information file after the calcilation. This is used for the
     * fault tolerance technique.
     */
    public void writeInfoFile(File f) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileOutputStream(f));
            getInfo().store(writer, null/*"Written at " + System.currentTimeMillis()*/);
        } catch (Exception e) {
            // not important
        } finally {
            try {
                writer.close();
            } catch (Exception ee) {
            }
        }

        String path = f.getAbsolutePath();
        if (disc_hash!=null && !disc_hash.equals(disc_path)) {
            File hash_path = new File(path.substring(0,path.indexOf(disc_hash)),disc_hash);
            try{
                FileUtils.writeStringToFile(new File(hash_path,".path"), disc_path);
            } catch(IOException e) {
                Log.err(e, 1);
            }
        }
        if (discgrp_hash!=null && !discgrp_hash.equals(discgrp_path)) {
            File hash_path = new File(path.substring(0,path.indexOf(discgrp_hash)));
            try{
                FileUtils.writeStringToFile(new File(hash_path,".path"), discgrp_path);
            } catch(IOException e) {
                Log.err(e, 1);
            }
        }
        if (cont_hash!=null && !cont_hash.equals(cont_path)) {
            File hash_path = new File(path.substring(0,path.indexOf(cont_hash)));
            try{
                FileUtils.writeStringToFile(new File(hash_path,".path"), cont_path);
            } catch(IOException e) {
                Log.err(e, 1);
            }
        }
    }

    public Properties getInfo() {
        Properties props = new Properties();
        try {
            props.setProperty(PROP_START, "" + _start);
            props.setProperty(PROP_END, "" + _end);
            props.setProperty(PROP_DURATION, "" + Math.max(0, (_lend - _lstart) / 1000));
            props.setProperty(PROP_CODE, prj.getCode());
            props.setProperty(PROP_STATE, STATE_STRINGS[_state]);
            props.setProperty(PROP_CALC, _calcName);
            props.setProperty(PROP_OUT, "" + asString(_output));
            props.setProperty("info", _info == null ? "" : _info);
        } catch (Exception e) {
            e.printStackTrace();
            props.setProperty("exception", e.toString());
            return props;
        }
        try {
            if (_nodes != null) {
                for (Node n : _nodes) {
                    if (n != null) {
                        props.setProperty(PROP_VAR + "." + n.getParamName(), n.getParamValue());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            props.setProperty("exception:nodes", e.getMessage());
            return props;
        }
        try {
            Map<String, Object> outs = getOutputValues();
            if (outs != null) {
                for (String o : outs.keySet()) {
                    Object out = outs.get(o);
                    props.setProperty(PROP_OUT + "." + o, "" + asString(out));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            props.setProperty("exception:output", e.getMessage());
            return props;
        }
        try {
            Map<String, Object> inter = getIntermediateValues();
            if (inter != null) {
                for (String i : inter.keySet()) {
                    Object iv = inter.get(i);
                    props.setProperty(PROP_INTER + "." + i, "" + asString(iv));
                }
            }

            return props;
        } catch (Exception e) {
            e.printStackTrace();

            props.setProperty("exception:intermediate", e.getMessage());
            return props;
        }
    }
}
