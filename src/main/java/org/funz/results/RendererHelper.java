/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.funz.results;

import java.util.HashSet;
import java.util.Set;
import org.funz.Project;
import org.funz.parameter.OutputFunctionExpression;
import static org.funz.util.Format.toHTML;

/**
 *
 * @author richet
 */
public class RendererHelper {

    public static OutputFunctionExpression buildAnythingOutputFunctionExpression(Project prj) {
        // init all in/inter/out outputexpression object for parplot renderer
        Set<String> all = new HashSet<String>();
        for (String vi : prj.getVoidInput().keySet()) {
            if (prj.getVariableByName(vi).isContinuous()) {
                // do not put discrete vars, as they will be aded by rendering init process
                if (prj.getVoidInput().get(vi) instanceof Double || prj.getVoidInput().get(vi) instanceof String || prj.getVoidInput().get(vi) instanceof Integer) {
                    all.add(vi);
                }
            }
        }
        for (String vi : prj.getVoidIntermediate().keySet()) {
            if (prj.getVoidIntermediate().get(vi) instanceof Double || prj.getVoidIntermediate().get(vi) instanceof String || prj.getVoidIntermediate().get(vi) instanceof Integer) {
                all.add(vi);
            }
        }
        for (String vo : prj.getPlugin().getVoidOutput().keySet()) {
            if (prj.getPlugin().getVoidOutput().get(vo) instanceof Double || prj.getPlugin().getVoidOutput().get(vo) instanceof String || prj.getPlugin().getVoidOutput().get(vo) instanceof Integer) {
                all.add(vo);
            }
        }
        for (OutputFunctionExpression of : prj.getOutputFunctionsList()) {
            if (of instanceof OutputFunctionExpression.Numeric) {
                all.add(of.toNiceSymbolicString());
            }
        }
        return new OutputFunctionExpression.AnythingND(all.toArray(new String[all.size()]));
    }
    public static String ARRAY_SEP = ";";

    public static String tryHTMLize(String res, String title) {
        return formatXML("HTML", title, res);
    }

    public static String formatXML(String type, String title, String res) {
        if (res == null) {
            return "<" + type + " name='" + toHTML(title) + "'></" + type + ">";
        }
        if (res.startsWith("<" + type + ">")) {
            res = res.replace("<" + type + ">", "<" + type + " name='" + toHTML(title) + "'>");
        }
        if (!res.trim().startsWith("<") || !res.contains("<" + type + " name=")) {
            res = "<" + type + " name='" + toHTML(title) + "'>" + toHTML(res) + "</" + type + ">";
        }
        return res;
    }
}
