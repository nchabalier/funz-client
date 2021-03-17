package org.funz.parameter;

import java.io.File;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.exec.OS;
import org.funz.Project;
import org.funz.XMLConstants;
import org.funz.log.Log;
import org.funz.log.LogCollector.SeverityLevel;

import static org.funz.api.Utils.toObject;
import org.funz.util.ASCII;
import static org.funz.util.Format.fromHTML;
import static org.funz.util.Format.toHTML;
import static org.funz.util.ParserUtils.getASCIIFileContent;
import static org.funz.util.ParserUtils.getAllLinesContaining;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Variable parameter. The varaibles are created by Project during file imports.
 * The same variable may be found within multiple files. A variable may have an
 * array of values or be defined as a cycle (loop). When it is an array, values
 * may have aliases.
 */
public class Variable extends VariableMethods implements XMLConstants, Parameter {

    private List<InputFile> _files = new LinkedList<InputFile>();
    private VarGroup _group;
    Project prj;
    private boolean _isScientific = false, _isSticky = false;
    private String _name, _type = TYPE_STRING;
    //private int _roundOff = DEFAULT_ROUND_OFF;
    private int _paramIdx;
    private List<Variable.Value> _values = new LinkedList<Variable.Value>();
    private String _default_value, _default_model;
    private List<String> instancesInFiles = new LinkedList<String>();

    @Override
    public String toString() {
        return getName();
    }

    public String toInfoString() {
        StringBuilder sb = new StringBuilder();
        sb.append(" Variable: ").append(_name);
        sb.append("\n  Type: ").append(_type);
        sb.append("\n  Default value: ").append(_default_value == null ? "<not set>" : _default_value);
        sb.append("\n  Default model: ").append(_default_model == null ? "<not set>" : _default_model);

        sb.append("\n  ").append(_values.size()).append(" values: ").append(ASCII.cat(" , ", _values.toArray(new VariableMethods.Value[_values.size()])));
        if (_group != null) {
            sb.append("\n  Group: ").append(_group.getName());
        }

        if (isContinuous()) {
            sb.append("\n  Bounds : [").append(getLowerBound()).append(" ; ").append(getUpperBound()).append("]");
        }
        return sb.toString();
    }

    /**
     * Creates an variable from an XML DOM node. Used for loading.
     */
    public Variable(Project p, Element e) throws Exception {
        prj = p;
        _name = e.getAttribute(ATTR_NAME);
        _type = e.getAttribute(ATTR_TYPE);

        String default_value = fromHTML(e.getAttribute(ATTR_DEFVAL));
        if (default_value != null && default_value.length() > 0) {
            _default_value = default_value;
        }

        String default_model = fromHTML(e.getAttribute(ATTR_DEFMOD));
        if (default_model != null && default_model.length() > 0) {
            _default_model = default_model;
        }

        String group = e.getAttribute(ATTR_GROUP);
        if (group != null && group.length() > 0) {
            boolean group_already_exists = false;
            for (VarGroup g : p.getGroups()) {
                if (g.getName().equals(group)) {
                    g.addVariable(this);
                    this.setGroup(g);
                    group_already_exists = true;
                    break;
                }
            }
            if (!group_already_exists) {
                VarGroup g = new VarGroup(group);
                p.addGroup(g);
                g.addVariable(this);
                this.setGroup(g);
            }

        }
        //_isLoop = "true".equals(e.getAttribute(ATTR_IS_LOOP));
        _isSticky = "true".equals(e.getAttribute(ATTR_IS_STICKY));
        _isScientific = "true".equals(e.getAttribute(ATTR_IS_SCI));

        String lower = e.getAttribute(ATTR_LOWER);
        if (lower != null && lower.length() > 0) {
            try {
                double d = Double.parseDouble(lower);
                setLowerBound(d);
            } catch (NumberFormatException nfe) {
            }
        }
        String upper = e.getAttribute(ATTR_UPPER);
        if (upper != null && upper.length() > 0) {
            try {
                double d = Double.parseDouble(upper);
                setUpperBound(d);
            } catch (NumberFormatException nfe) {
            }
        }

        NodeList elems = e.getElementsByTagName(ELEM_ELEMENT);
        for (int i = 0; i < elems.getLength(); i++) {
            Element el = (Element) elems.item(i);
            _values.add(new Value(fromHTML(el.getAttribute(ATTR_VALUE)), fromHTML(el.getAttribute(ATTR_ALIAS))));
        }
    }

    /**
     * Construct a variable with name name
     */
    public Variable(Project p, String name) {
        prj = p;
        _name = name;
        //_values.add(new Value(""));
    }

    public static Object CastValue(String strvalue, String type) {
        Object value = null;
        if (type.equals(Variable.TYPE_REAL) || type.equals(Variable.TYPE_CONTINUOUS)) {
            value = Double.parseDouble(strvalue);
        } else if (type.equals(Variable.TYPE_INT)) {
            value = Integer.parseInt(strvalue);
        } else if (type.equals(Variable.TYPE_STRING)) {
            value = strvalue;
        } else if (type.equals(Variable.TYPE_TEXTFILE)) {
            value = getASCIIFileContent(new File(strvalue));
        } else {
            throw new IllegalArgumentException("Unknown type " + type + ", for value " + strvalue);
        }
        return value;
    }

    public Object castValue(String strvalue) {
        return CastValue(strvalue, getType());
    }

    /**
     * Register a file the variable is found in.
     */
    public void addFile(InputFile fi) {
        if (!_files.contains(fi)) {
            _files.add(fi);
        }

        String[] instances = getAllLinesContaining(fi.getFile(), "" + prj.getVariableSyntax().getStartSymbol() + _name);
        instancesInFiles.addAll(Arrays.asList(instances));

        String[] instances2 = getAllLinesContaining(fi.getFile(), "" + prj.getVariableSyntax().getStartSymbol() + prj.getVariableSyntax().getLeftLimitSymbol() + _name);
        instancesInFiles.addAll(Arrays.asList(instances2));

    }

    public List<String> getInstancesInFiles() {
        return instancesInFiles;
    }

    public boolean equals(Object other) {
        return other == this || ((other instanceof Variable) && other != null && ((Variable) other)._name.equals(_name));
    }

    public String formatNumber(String v) {
        if (v == null || v.length() == 0) {
            return "";
        }

        if (prj.getPlugin().getRoundOff() != VariableMethods.NO_ROUND_OFF) {
            if (v.contains(".") && ((v.substring(v.indexOf(".")).length() - 1) > prj.getPlugin().getRoundOff())) {
                v = format(Double.parseDouble(v), _isScientific, prj.getPlugin().getRoundOff());
            }
        }

        v = v.trim();
        if (v.startsWith(".")) {
            v = "0" + v;
        }
        if (v.contains(".") && v.length() > 3) {
            while (v.endsWith("00")) {
                v = v.substring(0, v.length() - 1);
            }
        }

        return v;
    }

    public String asPath(String v) {
        if (v == null || v.length() == 0) {
            return "" + charReplacer;
        }

        if (_type.equals(TYPE_REAL) || _type.equals(TYPE_CONTINUOUS)) {
            return formatNumber(v);
        }

        //checkPath(v);
        return v;
    }

    public String asNode(String v) {
        if (v == null) {
            return null;
        }
        if (v.length() == 0) {
            return "" + charReplacer;
        }

        if (_type.equals(TYPE_REAL) || _type.equals(TYPE_CONTINUOUS)) {
            return formatNumber(v);
        }

        //checkPath(v);
        return v;
    }
    public static char charReplacer = '#';
    public static char[] win_chartodelete = {'/', '\\', '"', ':', '*', '?', '>', '<', '|', '*'};

    public static boolean checkPath(String v) {
        //PATCH pour remplacer le file.separator par un autre caractère pour éviter que l'arboressence des resultats soit polluée par des sous repertoires
        // Should not occur because alias and var values are red flagged when not authorized.
        boolean win = OS.isFamilyWindows();
        if (win) {
            for (int index = 0; index < Variable.win_chartodelete.length; index++) {
                if (v.indexOf(Variable.win_chartodelete[index]) != -1) {
                    Log.logMessage("Variable", SeverityLevel.ERROR, true, "This path element contains some Windows special characters: " + win_chartodelete[index]);
                    System.err.println("This path element contains some Windows special characters: " + win_chartodelete[index]);
                    //MessageDialog.showError("This path element contains some Windows special characters: " + win_chartodelete[index]);
                    return false;
                }
            }
        }
        return true;
    }

    /*public static String cleanPath(String v) {
     //PATCH pour remplacer le file.separator par un autre caractère pour éviter que l'arboressence des resultats soit polluée par des sous repertoires
     // Should not occur because alias and var values are red flagged when not authorized.
     boolean win = OS.isWindows();
     String replacer = v;
     replacer = replacer.replace(File.separatorChar, Variable.charReplacer);
     //if (win) {
     for (int index = 0; index < Variable.win_chartodelete.length; index++) {
     replacer = replacer.replace(Variable.win_chartodelete[index], Variable.charReplacer);
     }
     //}
     return replacer;
     }*/
    public String evaluate(String v) {
        if (v == null || v.length() == 0) {
            return "";
        }

        //assert (!_type.equals(TYPE_CONTINUOUS)) : "Evaluation of value '" + v + "' is only possible for discret types.";
        if (_type.equals(TYPE_REAL) || _type.equals(TYPE_CONTINUOUS)) {
            return formatNumber(v);
        }

        if (_type.equals(TYPE_TEXTFILE)) {
            String c = getASCIIFileContent(new File(v))/*.replaceAll("\n", "#")*/;
            //System.out.println("> " + c);
            return c;
        }
        return v;
    }

    public String getAliasAt(int pos) {
        if (pos < _values.size()) {
            return /*_isLoop ? "" :*/ ((Value) _values.get(pos)).getAlias();
        } else {
            return "";
        }

    }

    public int getIndex() {
        return _paramIdx;
    }

    public void setIndex(int idx) {
        _paramIdx = idx;
    }

    /**
     * Returns the file list of type FileInfo the variable comes from.
     */
    public List<InputFile> getFiles() {
        return _files;
    }

    /**
     * Returns the group the variables belongs to or null.
     */
    public VarGroup getGroup() {
        return _group;
    }

    /**
     * Returns the group name the variables belongs to.
     */
    public String getGroupName() {
        return _group == null ? "" : _group.getName();
    }

    /**
     * Returns the parameter name.
     */
    public String getName() {
        return _name;
    }

    /**
     * Overloaded below method
     */
    public String getNiceValueViewAt(int pos) {
        return getNiceValueViewAt(pos, true, true);
    }

    /**
     * Returns a colored pos-th value view.
     */
    public String getNiceValueViewAt(int pos, boolean html, boolean reduceSize) {
        /*if (_isLoop)
         return getValueAt(pos);*/
        String sz = "";
        if (reduceSize && html) {
            sz = "size=-2 ";
        }

        Value v = (Value) _values.get(pos);
        if (v.getAlias() == null || v.getAlias().length() == 0) {
            if (html) {
                return "<font " + sz + "color=blue>" + v.getValue() + "</font>";
            } else {
                return v.getValue();
            }
        }
        if (html) {
            return "<font " + sz + "color=blue>" + v.getValue() + "</font> [" + v.getAlias() + "]";
        } else {
            return v.getValue() + " [" + v.getAlias() + "]";
        }
    }

    /**
     * Returns the number of values.
     */
    public int getNmOfValues() {
        /*if (_isLoop) {
         if (_type.equals(TYPE_REAL)) {
         double from = Double.parseDouble(_from);
         double to = Double.parseDouble(_to);
         double step = Double.parseDouble(_step);
         return (int) ((to - from) / step) + 1;
         } else {
         int from = Integer.parseInt(_from);
         int to = Integer.parseInt(_to);
         int step = Integer.parseInt(_step);
         return (to - from) / step + ((to - from) % step == 0 ? 1 : 0);
         }
         }*/

        return _values.size();
    }

    public int getParameterType() {
        return PARAM_TYPE_VARIABLE;
    }

    /**
     * Returns the variable type. It may be TYPE_REAL, TYPE_INT or TYPE_STRING
     */
    public String getType() {
        return _type;
    }

    /**
     * Returns the value array
     */
    public Value[] getValueArray() {
        List vlist = getValues();
        Value vals[] = new Value[vlist.size()];
        vlist.toArray(vals);
        try {
            for (int i = 0; i < vals.length; i++) {
                vals[i] = (Value) vals[i].clone();
            }

        } catch (Exception e) {
        }
        return vals;
    }

    public String getValueAt(int pos) {
        return ((Value) _values.get(pos)).getValue();
    }

    /**
     * Returns the value list of type Variable.Value
     */
    public List<Variable.Value> getValues() {
        return _values;
    }

    public LinkedList<Variable.Value> getValuesCopy() {
        LinkedList<Value> copy = new LinkedList<Value>();
        for (Value value : _values) {
            copy.add((Value) value.clone());
        }
        return copy;
    }

    public Object getValueObjectAt(int pos) {
        return VariableMethods.castValue(getType(), getValueAt(pos));
    }

    public String getValuesString() {
        return getValuesString(false);
    }

    /**
     * Reurns a string representation of the value array.
     *
     * @param htmlPrefix if true wraps the return string by html tags
     */
    public String getValuesString(boolean htmlPrefix) {
        StringBuilder sb = new StringBuilder();
        String sz = "";
        if (htmlPrefix) {
            sb.append("<html>");
            sz = "size=-2 ";
        }

        /*if (_isLoop) {
         sb.append("from <font " + sz + "color=blue>" + _from + "</font> to <font " + sz + "color=blue>" + _to + "</font> step <font " + sz + "color=blue>"
         + _step + "</font>");
         } else {*/
        if (checkValid() != null) {
            sb.append("<font ").append(sz).append("color=red>change me</font>");
        } else {
            if (!isContinuous()) {
                int nmValues = getNmOfValues();
                for (int i = 0; i < nmValues; i++) {
                    sb.append(getNiceValueViewAt(i, htmlPrefix, htmlPrefix));
                    if (i != nmValues - 1) {
                        sb.append(" ");
                    }
                }
            } else {
                sb.append("[ ").append(getLowerBound()).append(" ; ").append(getUpperBound()).append(" ]");
            }
        }
        //}
        if (htmlPrefix) {
            sb.append("</html>");
        }

        return sb.toString();
    }

    /**
     * Returns the variable representation for a pos-th value.
     */
    public String getValueNode(int pos) {
        assert (!_type.equals(TYPE_CONTINUOUS)) : "Cannot call getValueNode with continuous variable.";
        /*if (_isLoop)
         return _name + "=" + getValueAt(pos);*/

        Value v = getValues().get(pos);
        if (v.getAlias() == null || v.getAlias().length() == 0) {
            return _name + "=" + asNode(v.getValue());
        } else {
            return "@" + _name + "=" + v.getAlias();
        }
    }

    /**
     * Says whether the varaiable is a cycle or not.
     */
    /*public boolean isLoop() {
     return _isLoop;
     }*/
    /**
     * Sets the loop flag.
     */
    /*public void setLoop(boolean on) {
     _isLoop = on;
     }*/
    /**
     * Returns the variable representation for a pos-th value.
     */
    public String getValueAsPathAt(int pos) {
        assert (!_type.equals(TYPE_CONTINUOUS)) : "Cannot call getValueViewAt with continuous variable.";
        /*if (_isLoop)
         return _name + "=" + getValueAt(pos);*/

        Value v = getValues().get(pos);
        if (v.getAlias() == null || v.getAlias().length() == 0) {
            return _name + "=" + asPath(v.getValue());
        } else {
            return "@" + _name + "=" + v.getAlias();
        }
    }

    /**
     * Says whether the variable values will be scientifically formatted or not.
     */
    public boolean isScientific() {
        return _isScientific;
    }

    /**
     * Sets the sticky flag. If true, the variable will participate in final
     * parameters even if it has only one value.
     */
    public boolean isSticky() {
        return _isSticky;
    }

    /**
     * Says whether tha varaiable is not virgin.
     */
    public String checkValid() {
        StringBuffer log = new StringBuffer();
        if (isContinuous()) {
            if (Double.isNaN(getLowerBound())) {
                log.append("Lower bound is NaN.");
            }
            if (Double.isNaN(getUpperBound())) {
                log.append("Upper bound is NaN.");
            }

        } else {
            if (_values == null) {
                log.append("Values list is null.");
            }

            if (_values.size() <= 0) {
                log.append("Values list is empty.");
            }

            for (Variable.Value v : getValues()) {
                if (v.getValue() == null) {
                    log.append("One value is null.");
                }
                if (v.getValue().length() == 0) {
                    log.append("One value is empty.");
                }

                if (!checkPath(v.getValue())) {
                    log.append("Invalid path for value " + v + ".");
                }

                if (_type.equals(TYPE_REAL)) {
                    try {
                        Double.parseDouble(v.getValue());
                    } catch (Exception e) {
                        log.append("Type real but cannot parse double from " + v.getValue() + ".");
                    }
                }

                if (_type.equals(TYPE_INT)) {
                    try {
                        Integer.parseInt(v.getValue());
                    } catch (Exception e) {
                        log.append("Type int but cannot parse integer from " + v.getValue() + ".");
                    }
                }

                if (_type.equals(TYPE_TEXTFILE)) {
                    if (!new File(v.getValue()).exists()) {
                        log.append("Type file but cannot find file from " + v.getValue() + ".");
                    }
                }
            }

            //return /*_isLoop ||*/ !(_values.size() == 1 && _type.equals(TYPE_REAL) && ((Value) _values.getFirst()).getValue().equals(""));
        }
        if (log.length() == 0) {
            return null;
        } else {
            return log.toString();
        }
    }

    /**
     * Deregister a file.
     */
    public void removeFile(InputFile fi) {
        _files.remove(fi);
    }

    /**
     * Serializes the variable. Used for saving.
     */
    public void save(PrintStream ps) {
        ps.print("\t<" + ELEM_VARIABLE + "\n\t\t" + ATTR_NAME + "=\"" + _name + "\"\n\t\t" + ATTR_TYPE + "=\"" + _type + "\"\n\t\t");
        /*if (_isLoop)
         ps.print(ATTR_IS_LOOP + "=\"true\"\n\t\t");*/
        ps.print(ATTR_GROUP + "=\"" + getGroupName() + "\"\n\t\t");
        ps.print(ATTR_DEFVAL + "=\"" + toHTML(getDefaultValue() == null ? "" : getDefaultValue()) + "\"\n\t\t");
        ps.print(ATTR_DEFMOD + "=\"" + toHTML(getDefaultModel() == null ? "" : getDefaultModel()) + "\"\n\t\t");
        ps.print(ATTR_IS_STICKY + "=\"" + _isSticky + "\"\n\t\t");

        ps.print(ATTR_LOWER + "=\"" + _lower + "\"\n\t\t");
        ps.print(ATTR_UPPER + "=\"" + _upper + "\"\n\t\t");

        ps.println(">");

        for (Iterator it = _files.iterator(); it.hasNext();) {
            ps.println("\t\t<" + ELEM_SOURCE + " " + ATTR_NAME + "=\"" + ((InputFile) it.next()).getFile().getName() + "\" />");
        }

//if (!_isLoop)
        for (Iterator it = _values.iterator(); it.hasNext();) {
            Value v = (Value) it.next();
            ps.println("\t\t<" + ELEM_ELEMENT + " " + ATTR_VALUE + "=\"" + toHTML(v.getValue()) + "\" " + ATTR_ALIAS + "=\"" + toHTML(v.getAlias()) + "\" />");
        }

        ps.println("\t</" + ELEM_VARIABLE + ">");
    }

    public void setAliasAt(int pos, String s) {
        ((Value) (_values.get(pos))).setAlias(s);
    }

    /**
     * Associates the variable with a group. Only one group may detain the
     * variable at a time.
     */
    public void setGroup(VarGroup group) {
        _group = group;
    }

    /**
     * Sets the round-off precision.
     */
    /*public void setRoundOff(int roundOff) {
     _roundOff = roundOff;
     }*///private boolean _isLoop = false;
    /**
     * Changes the scientific format flag. Ignored if the variable is not of
     * type TYPE_REAL.
     */
    public void setScientific(boolean on) {
        _isScientific = on;
    }

    /**
     * Says whether the variable is sticky or not.
     */
    public void setSticky(boolean on) {
        _isSticky = on;
    }

    /**
     * Modifies the variable type.
     */
    public void setType(String type) {
        _type = type;
    }

    /**
     * Modifies the variable type.
     */
    public void setDefaultModel(String mod, boolean parse) {
        if (mod == null) {
            mod = "";
        }
        _default_model = mod;
        if (parse) {
            parseDefaultModel();
        }
    }

    private void parseDefaultModel() {
        for (String m : _default_model.split(VariableMethods.DEFAULT_MODEL_SEPARATOR)) {
            if (m.startsWith("{") && m.endsWith("}")) {
                String[] sample = m.substring(1, m.length() - 1).split(",");
                setDiscTypeFromSampleValues(sample);
                LinkedList<Value> values = new LinkedList<Value>();
                for (String s : sample) {
                    values.add(new Value(s.replace("\"", "")));
                }
                setValues(values);
            } else if (m.startsWith("[") && m.endsWith("]")) {
                setType(TYPE_CONTINUOUS);
                String[] bounds = m.substring(1, m.length() - 1).split(",");
                setLowerBound(Double.parseDouble(bounds[0]));
                setUpperBound(Double.parseDouble(bounds[bounds.length - 1]));
            } else if (m.startsWith("\"") && m.endsWith("\"")) {
                comment = m.substring(1, m.length() - 1);
            } else if (m.startsWith(":")) {
                String grp = m.substring(1);
                if (grp.length() > 0) {
                    VarGroup g = prj.findGroup(grp);
                    if (g == null) {
                        g = new VarGroup(grp);
                        prj.addGroup(g);
                    }
                    setGroup(g);
                    g.addVariable(this);
                }
            } else if (m.startsWith("'") && m.endsWith("'")) {
                m = m.substring(1, m.length() - 1);
                if (m.contains("/")) {
                    setType(TYPE_TEXTFILE);
                } else {
                    setType(TYPE_STRING);
                }
                _default_value = m;
            } else {
                setDiscTypeFromSampleValues(m);
                _default_value = m;
                /*LinkedList<Value> values = new LinkedList<Value>();
                values.add(new Value(_default_value));
                setValues(values);*/
            }
        }
    }
    private String comment = null;

    /**
     * Modifies the variable type.
     */
    public void setDiscTypeFromSampleValues(String... values) {
        int level = 0; // O = int, 1 = double, 2 = file, 3 = string
        for (String string : values) {
            if (level == 0) {
                try {
                    Integer.parseInt(string);
                } catch (NumberFormatException e) {
                    level++;
                }
            } else if (level == 1) {
                try {
                    Double.parseDouble(string);
                } catch (NumberFormatException e) {
                    level++;
                }
            } else if (level == 2) {
                if (!new File(string).canRead()) {
                    level++;
                }
            } else if (level == 3) {
                break;
            }
        }

        if (level == 0) {
            setType(TYPE_INT);
        } else if (level == 1) {
            setType(TYPE_REAL);
        } else if (level == 2) {
            setType(TYPE_TEXTFILE);
        } else if (level == 3) {
            setType(TYPE_STRING);
        }
    }

    /**
     * Modifies the variable type.
     */
    public String getDefaultValue() {
        return _default_value;
    }

    public String getDefaultModel() {
        return _default_model;
    }

    public String getComment() {
        return comment;
    }

    /**
     * Sets value list.
     */
    public void setValues(List<Variable.Value> values) {
        _values = values;
    }

    public LinkedList<org.funz.parameter.Parameter> getGroupComponents() {
        return null;
    }
    private double _lower = Double.NaN;
    private double _upper = Double.NaN;

    public double getLowerBound() {
        //assert isContinuous() : "Variable " + getName() + " is not continuous !";

        return _lower;
    }

    public double getUpperBound() {
        //assert isContinuous() : "Variable " + getName() + " is not continuous !";

        return _upper;
    }

    public double getMinValue() {
        //assert !isContinuous() : "Variable " + getName() + " is continuous !";

        if (isReal() || isInteger()) {
            double m = Double.MAX_VALUE;
            for (Variable.Value v : _values) {
                m = Math.min(m, Double.parseDouble(v.getValue()));
            }

            return m;
        }

        throw new IllegalArgumentException("Only discrete variables as integers or doubles have min values.");
    }

    public double getMaxValue() {
        //assert !isContinuous() : "Variable " + getName() + " is continuous !";

        if (isReal() || isInteger()) {
            double m = Double.MIN_VALUE;
            for (Variable.Value v : _values) {
                m = Math.max(m, Double.parseDouble(v.getValue()));
            }

            return m;
        }

        throw new IllegalArgumentException("Only discrete variables as integers or doubles have max values.");
    }

    public boolean isGroup() {
        return false;
    }

    public boolean isContinuous() {
        return _type != null && _type.equals(TYPE_CONTINUOUS);
    }

    public boolean isReal() {
        return _type.equals(TYPE_REAL);
    }

    public boolean isInteger() {
        return _type.equals(TYPE_INT);
    }

    public void setLowerBound(double min) {
        this._lower = min;
    }

    public void setUpperBound(double max) {
        this._upper = max;
    }

    public static Variable newVariable(String name, Object model, Project prj) {
        Variable v = prj.getVariableByName(name);
        if (v == null) {
            v = new Variable(prj, name);
            prj.getVariables().add(v);
        }
        return setVariable(name, model, prj);
    }

    public static Variable setVariable(String name, Object model, Project prj) {
        Variable v = prj.getVariableByName(name);
        if (v == null) {
            throw new IllegalArgumentException("Variable " + name + " not found in project.");
        }

        LinkedList<VariableMethods.Value> vals = new LinkedList<>();
        if (model instanceof String && model.toString().startsWith("[") && model.toString().endsWith("]")) {
            v.setType(Variable.TYPE_CONTINUOUS);

            double[] bounds = (double[]) toObject(model.toString());
            v.setLowerBound(bounds[0]);
            v.setUpperBound(bounds[1]);
        } else {
            if (model instanceof Double) {
                model = new Double[]{(Double) model};
            }
            if (model instanceof Integer) {
                model = new Integer[]{(Integer) model};
            }
            if (model instanceof String) {
                model = new String[]{(String) model};
            }
            if (model instanceof File) {
                model = new File[]{(File) model};
            }

            if (model instanceof Double[]) {
                v.setType(Variable.TYPE_REAL);
                Double[] ovv = (Double[]) model;
                for (Double ovv1 : ovv) {
                    vals.add(new VariableMethods.Value(ovv1.toString()));
                }
            } else if (model instanceof double[]) {
                v.setType(Variable.TYPE_REAL);
                double[] ovv = (double[]) model;
                for (Double ovv1 : ovv) {
                    vals.add(new VariableMethods.Value(ovv1.toString()));
                }
            } else if (model instanceof Integer[]) {
                v.setType(Variable.TYPE_INT);
                Integer[] ovv = (Integer[]) model;
                for (Integer ovv1 : ovv) {
                    vals.add(new VariableMethods.Value(ovv1.toString()));
                }
            } else if (model instanceof int[]) {
                v.setType(Variable.TYPE_INT);
                int[] ovv = (int[]) model;
                for (Integer ovv1 : ovv) {
                    vals.add(new VariableMethods.Value(ovv1.toString()));
                }
            } else if (model instanceof String[]) {
                v.setType(Variable.TYPE_STRING);
                String[] ovv = (String[]) model;
                for (String ovv1 : ovv) {
                    vals.add(new VariableMethods.Value(ovv1));
                }
            } else if (model instanceof File[]) {
                v.setType(Variable.TYPE_TEXTFILE);
                File[] ovv = (File[]) model;
                for (File ovv1 : ovv) {
                    vals.add(new VariableMethods.Value(ovv1.getAbsolutePath()));
                }
            } else if (model instanceof Object[]) {
                v.setType(Variable.TYPE_STRING);
                Object[] ovv = (Object[]) model;
                for (Object ovv1 : ovv) {
                    vals.add(new VariableMethods.Value(ovv1.toString()));
                }
            } else {
                throw new IllegalArgumentException("Cannot cast values of variable " + v + ": " + model.getClass());
            }
            v.setValues(vals);
        }
        return v;
    }
}
