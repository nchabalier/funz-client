package org.funz.doeplugin;

import java.io.*;
import java.util.Map;
import org.funz.Project;
import static org.funz.XMLConstants.*;
import org.funz.parameter.Case;
import static org.funz.util.Format.XML.getText;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class DesignSession {

    int caseIdx;

    public DesignSession(int caseIdx) {
        this.caseIdx = caseIdx;
    }
    static String ELEM_ANALYSIS_TAG = "<" + ELEM_ANALYSIS + ">";
    static String ELEM_ANALYSIS_ENDTAG = "</" + ELEM_ANALYSIS + ">";

    public DesignSession(Element elem, int caseIdx) throws Exception {
        Node node = elem.getElementsByTagName(ELEM_ANALYSIS).item(0);
        _res = getText(node); // return the content AND TAGS <analysis>...</analysis>
        assert _res.startsWith(ELEM_ANALYSIS_TAG) && _res.trim().endsWith(ELEM_ANALYSIS_ENDTAG) : "bad design session storage:" + _res;
        _res = _res.substring((ELEM_ANALYSIS_TAG).length(), _res.indexOf(ELEM_ANALYSIS_ENDTAG));
        //_res = XMLConstants.XMLConverter.toString(elem);
    }

    public void setAnalysis(String res) {
        _res = res;
    }

    public String getAnalysis() {
        return _res;
    }

    public Design getDesign() {
        return _design;
    }

    public void setDesign(Design d) {
        _design = d;
    }

    public void save(PrintStream ps) {
        ps.println("<" + ELEM_SESSION + " " + ATTR_IDX + "='" + caseIdx + "' >\n  " + ELEM_ANALYSIS_TAG + "\n"
                + (_res != null ? /*XMLConverter.toXML*/ (_res) : "")
                + "\n  " + ELEM_ANALYSIS_ENDTAG + "\n</" + ELEM_SESSION + ">");

    }
    //private HashMap<String, Object> _props = new HashMap<String, Object>();
    private Design _design;
    private String _res;
    //private static final String TYPE_STRING = "str",  TYPE_DOUBLE = "dbl",  TYPE_INT = "int";

    public Case getDiscreteCase() {
        Project prj = _design._designer.getProject();
        return prj.getDiscreteCases().get(caseIdx);
    }

    public Map<String, Object> getFixedParameters() {
        //List<Parameter> fixedparams=_design._designer.getProject().getDiscreteParameters();
        if (_design == null || _design._designer == null || _design._designer.getProject() == null) {
            return null;
        }
        Project prj = _design._designer.getProject();
        return prj.getDiscreteCases().get(caseIdx).getInputValues();
    }
}
