package org.funz.parameter;

import java.io.PrintStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import org.funz.XMLConstants;
import org.funz.util.ASCII;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Groups variables.
 * Variables must have equal number of values.
 * The very first variable's aliases are used for the entire group.
 */
public class VarGroup implements XMLConstants, Parameter {

    private boolean _isHorizontal;
    private boolean _isNew = true;
    private String _name;
    private int _paramIdx;
    private LinkedList<String> _varnames = new LinkedList<String>();
    private LinkedList<Variable> _vars = new LinkedList<Variable>();
    private boolean _isContinuous = false;
    private boolean _isParametric = false;
    private Variable _parameterVariable;
    private String _parameterVariableName = "";
    private LinkedList<String> _alias = new LinkedList<String>();

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Group: " + _name);
        sb.append(" Variables: ");
        for (Variable v : _vars) {
            sb.append(v.getName() + " "+v.hashCode()+" ");
        }

        return sb.toString();
    }

    public static String catStringList(LinkedList<String> array) {
        if(array==null)return"";
        return ASCII.cat(",,", array.toArray());
    }

    public static String[] uncatStringList(String str) {
        if (str!=null && str.length()>0)
        return str.split(",,");
        else return new String[0];
    }

    /** Constructs a group from an XML DOM node.
     * Used during project loading.
     */
    public VarGroup(Element e) throws Exception {
        _name = e.getAttribute(ATTR_NAME);
        _isHorizontal = "true".equals(e.getAttribute(ATTR_IS_HOR));
        _isContinuous = "true".equals(e.getAttribute(ATTR_IS_CONTINUOUS));
        if (e.getAttribute(ATTR_ALIAS) != null) {
            for (String string : uncatStringList(e.getAttribute(ATTR_ALIAS))) {
                _alias.add(string);
            }
        }

        _parameterVariableName = e.getAttribute(ATTR_PARAM_VARIABLE);

        NodeList vars = e.getElementsByTagName(ELEM_VAR);
        for (int i = 0; i < vars.getLength(); i++) {
            _varnames.add(((Element) vars.item(i)).getAttribute(ATTR_NAME));
        }
        _isNew = false;
    }

    /** Constructs an empty group with name name */
    public VarGroup(String name) {
        _name = name;
    }

    public String getParameterVariableName() {
        return _parameterVariableName;
    }

    public Variable getParameterVariable() {
        return _parameterVariable;
    }

    public void setParameterVariable(Variable parameterVariable) {
        _parameterVariable = parameterVariable;
    }

    /** Adds a variable. */
    public void addVariable(Variable var) {
        if (!_vars.contains(var)) {
            _vars.add(var);
            sortVariablesByName();
        }
    }

    /*public boolean equals(Object other) {
        return other == this || ((other instanceof VarGroup) && other != null && ((VarGroup) other)._name.equals(_name));
    }*/

    public String getCatValuesAt(int pos) {
        StringBuffer sb = new StringBuffer();
        int i = 1;
        for (Iterator<Variable> it = _vars.iterator(); it.hasNext(); i++) {
            Variable v = (Variable) it.next();
            sb.append(v.getName());
            sb.append("=");
            sb.append(v.getValueAt(pos));
            if (i < _vars.size()) {
                sb.append(Case.Node.GROUP_SEPARATOR);
            }
        }
        return sb.toString();
    }

    public int getIndex() {
        return _paramIdx;
    }

    public void setIndex(int idx) {
        _paramIdx = idx;
    }

    /** Gets the group name. */
    public String getName() {
        return _name;
    }

    public int getNmOfValues() {
        if (_vars.size() > 0) {
            return ((Variable) _vars.getFirst()).getNmOfValues();
        }
        return 0;
    }

    public int getParameterType() {
        return PARAM_TYPE_VARGROUP;
    }

    public Variable.Value[] getValueArray() {
        throw new UnsupportedOperationException("Value array not accessible for a group");
        /*int nv = getNmOfValues();
        Variable.Value vals[] = new Variable.Value[nv];
        for (int i = 0; i < nv; i++) {
            vals[i] = new Variable.Value(getValueViewAt(i));
        }
        return vals;*/
    }

    @Override
    public String getValueNode(int pos) {
        return getValueAsPathAt( pos);
    }
    
    public String getValueAsPathAt(int pos) {
        //return _name + "=" + (pos + 1);


        StringBuffer vnames = new StringBuffer();
        StringBuffer vvals = new StringBuffer();
        if (_vars.size() > 0) {
            if (_alias != null && _alias.size() > pos && _alias.get(pos) != null && _alias.get(pos).length() > 0) {
                return "@" + _name + "=" + _alias.get(pos);
            }
            for (Variable v : _vars) {
                vnames.append(v.getName());
                vnames.append(Case.Node.GROUP_SEPARATOR);
                vvals.append(v./*getValueAsPathAt(pos)*/evaluate(v.getValueAt(pos)));
                vvals.append(Case.Node.GROUP_SEPARATOR);
            }
            vnames = vnames.deleteCharAt(vnames.length() - 1);
            vvals = vvals.deleteCharAt(vvals.length() - 1);
            return vnames.toString() + "=" + vvals.toString();
        } else {
            return "No var in group " + getName();
        }

    /*if (_vars.size() > 0) {
    String a = ((Variable) _vars.getFirst()).getAliasAt(pos);
    if (a != null && a.length() > 0) {
    return a;
    }
    }
    StringBuffer sb = new StringBuffer();
    int i = 1;
    for (Iterator it = _vars.iterator(); it.hasNext(); i++) {
    Variable v = (Variable) it.next();
    sb.append(v.getName());
    sb.append("=");
    sb.append(v.getValueAt(pos));
    if (i < _vars.size()) {
    sb.append(",");
    }
    }
    return sb.toString();*/
    }

    /** Returns the vaiable list of type Variable */
    public LinkedList<Variable> getVariables() {
        return _vars;
    }

    public String[] getVariablesName() {
        String[] vn = new String[_vars.size()];
        for (int i = 0; i < vn.length; i++) {
            vn[i] = _vars.get(i).getName();
        }
        return vn;
    }

    /** Construct a space separated variable list. */
    public String getVariablesString() {
        return ASCII.cat(",", getVariablesName());
    }

    /** Returns  variable index.*/
    public int getVarIndex(Variable var) {
        for (int i = 0; i < _vars.size(); i++) {
            if (_vars.get(i) == var) {
                return i;
            }
        }
        return -1;
    }

    /** Returns all variabale names. Used only during project loading.  */
    public LinkedList<String> getVarNames() {
        return _varnames;
    }

    /** Says whether the group is read from file or created later on. */
    public boolean isNew() {
        return _isNew;
    }

    /** Mark group as old */
    public void markAsOld() {
        _isNew = false;
    }

    /** Reoves variable from the group. */
    public void removeVariable(Variable var) {
        _vars.remove(var);
        sortVariablesByName();
    }

    /** Serializes the group for saving. */
    public void save(PrintStream ps) {
        ps.println("\t<" + ELEM_GROUP + "\n\t" +
                ATTR_NAME + "=\"" + _name + "\"\n\t" +
                ATTR_IS_CONTINUOUS + "=\"" + _isContinuous + "\"\n\t" +
                ATTR_ALIAS + "=\"" + catStringList(_alias) + "\"\n\t" +
                ATTR_PARAM_VARIABLE + "=\"" + (_parameterVariable == null ? "" : _parameterVariable.getName()) + "\"\n\t" +
                ATTR_IS_HOR + "=\"" + _isHorizontal + "\"\n\t\t" + ">");

        for (Iterator<Variable> it = _vars.iterator(); it.hasNext();) {
            ps.println("\t\t<" + ELEM_VAR + " " + ATTR_NAME + "=\"" + ((Variable) it.next()).getName() + "\"/>");
        }



        ps.println("\t</" + ELEM_GROUP + ">");

    }

    /** Sets the group name. */
    public void setName(String name) {
        _name = name;
    }

    /** Registers a variale list. */
    public void setVariables(LinkedList<Variable> vars) {
        _vars = vars;
    }

    public LinkedList<org.funz.parameter.Parameter> getGroupComponents() {
        LinkedList<org.funz.parameter.Parameter> ret = new LinkedList<org.funz.parameter.Parameter>();
        ret.addAll(_vars);
        return ret;
    }

    public double getMinValue() {
        return _parameterVariable == null ? 0.0 : _parameterVariable.getMinValue();
    }

    public double getMaxValue() {
        return _parameterVariable == null ? 1.0 : _parameterVariable.getMaxValue();
    }

    public double getLowerBound() {
        throw new IllegalArgumentException("Not yet implemented.");
    }

    public double getUpperBound() {
        throw new IllegalArgumentException("Not yet implemented.");
    }

    public boolean isGroup() {
        return true;
    }

    public boolean isContinuous() {
        // todo
        return _isContinuous;
    }

    public void setIsContinuous(boolean isContinuous) {
        _isContinuous = isContinuous;
    }

    public boolean isReal() {
        // todo
        return false;
    }

    public boolean isInteger() {
        return false;
    }

    /**
     * @return the _alias
     */
    public LinkedList<String> getAlias() {
        return _alias;
    }

    /**
     * @param alias the _alias to set
     */
    public void setAlias(LinkedList<String> alias) {
        this._alias = alias;
    }

    private void sortVariablesByName() {
        if (_vars.size() > 0) {
            Collections.sort(_vars, new Comparator<Variable>() {
                @Override
                public int compare(final Variable v1, final Variable v2) {
                    return v1.getName().compareTo(v2.getName());
                }
            });
        }
    }
}
