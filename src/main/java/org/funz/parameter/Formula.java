package org.funz.parameter;

import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.funz.Constants;
import org.funz.log.Log;
import org.funz.log.LogCollector.SeverityLevel;
import org.funz.script.MathExpression;
import org.funz.util.ASCII;
import static org.funz.util.ParserUtils.getASCIIFileContent;

/**
 * Parses and treats formulas. This is not fully featured formula parser. It
 * undersands only basic functions and operations. Default formatted example: ${
 * 10. + tan( 2.6 ) / cos( $VAR )} Custom formatted example: ${ ( $KEFF - 1 ) /
 * $KEFF |0.000}
 */
public class Formula {

    /**
     * Thrown whenever a wrong math operation occurs
     */
    public static class EvalException extends Exception {

        protected EvalException(String s) {
            super(s);
        }

        public String toString() {
            return getMessage();
        }
    }

    public static Map<String, String> AUTO_REPLACE_IN_EVAL = new HashMap();
    static{
        AUTO_REPLACE_IN_EVAL.put("\r\n","\n"); // for windows
    }
    /**
     * Evaluates an expression
     */
    public static String Eval(String expr, MathExpression engine) throws EvalException {
        Log.logMessage("Formula", SeverityLevel.INFO, false, "Parsing formula: " + expr);

        if (AUTO_REPLACE_IN_EVAL != null) {
            for (String s : AUTO_REPLACE_IN_EVAL.keySet()) {
                if (expr.contains(s)) {
                    Log.logMessage("Formula", SeverityLevel.INFO, false, "Replacing '" + s + "' by '" + AUTO_REPLACE_IN_EVAL.get(s) + "' inside formula '" + expr + "'");
                    expr = expr.replace(s, AUTO_REPLACE_IN_EVAL.get(s));
                    Log.logMessage("Formula", SeverityLevel.INFO, false, "                                                             -> '" + expr + "'");

                }
            }
        }

        if (engine == null) {
            engine = MathExpression.GetDefaultInstance();
        }

        //String original = expr;
        if (expr == null) {
            throw new EvalException("Math expression no set.");
        }
        if (expr.length() == 0) {
            throw new EvalException("Empty math expression.");
        }

        String format = null;
        DecimalFormat df = null;
        int f1;
        if ((f1 = expr.indexOf('|')) >= 0) {
            try {
                format = expr.substring(f1 + 1, expr.length()).trim().replaceAll("[1-9]", "0").replaceAll("E-", "E");
                DecimalFormatSymbols dot = new DecimalFormatSymbols();
                dot.setDecimalSeparator('.');
                df = new DecimalFormat(format, dot);
                expr = expr.substring(0, f1);// + expr.substring(expr.length() - 1); that was to remove the lasting ")" previously added...
            } catch (Exception e) {
                throw new EvalException(" Wrong format \"" + format + "\": " + e.getMessage());
            }
        }

        //System.err.print("Evaluating formula: '" + expr+"'");
        Object eval = null;
        try {
            eval = engine.eval(expr, null);
        } catch (Exception ex) {
            throw new EvalException("Incorrect expression: \"" + expr + "\"\n" + ex.getMessage());
        }
        //System.err.println("Evaluating formula: " + expr+" = "+eval.toString()+" ("+eval.getClass()+")");

        if (eval == null) {
            throw new EvalException("Void expression");
        }

        try {
            Double value;
            if (eval instanceof Integer) {
                value = ((Integer) eval).doubleValue();
            } else /*if (eval instanceof Double)*/ {
                value = (Double) eval;
            }
            if (df != null) {
                //System.err.println(" = " + df.format(eval));
                return df.format(value);
            } else {
                //System.err.println(" = " + eval.toString());
                return value.toString();
            }
        } catch (ClassCastException ex) {
            if (df != null) {
                throw new EvalException("Incorrect numeric value: \"" + eval + "\"\n" + ex.getMessage());
            } else {
                Log.logMessage("[Formula]", SeverityLevel.WARNING, false, "Expression " + expr + " was not interpreteed as numeric.");
                return eval.toString();
            }
        }
    }

    public static boolean isValid(String expr, SyntaxRules varSyntax, MathExpression engine) {
        if (expr.contains("|")) {
            expr = expr.substring(0, expr.indexOf("|"));
        }

        try {
            File tmpdir = new File(Constants.USER_TMP_DIR, "form" + expr.hashCode());
            tmpdir.mkdirs();
            if (!tmpdir.exists() || !tmpdir.isDirectory()) {
                Log.logMessage("[Formula]", SeverityLevel.ERROR, false, "Unable to create temporary directory " + tmpdir.getAbsolutePath());
                System.err.println("Unable to create temporary directory " + tmpdir.getAbsolutePath());
            }

            File raw = new File(tmpdir, "raw");
            ASCII.saveFile(raw, expr);
            HashMap<String, String> default_value = new HashMap<String, String>()/* {
                    
                     @Override
                     public boolean containsKey(Object key) {
                     return true;
                     }
                    
                     @Override
                     public String get(Object key) {
                     if (super.containsKey(key)) {
                     return super.get(key);
                     } else {
                     return Math.random() + "";
                     }
                     }
                     }*/;
            SyntaxRules anyotherSyntax = new SyntaxRules(varSyntax.getStartSymbolIdx() < SyntaxRules.START_SYMBOL_STRINGS.length - 1 ? varSyntax.getStartSymbolIdx() + 1 : varSyntax.getStartSymbolIdx() - 1, varSyntax.getLimitsIdx());
            HashSet vars = VariableMethods.parseFile("XXX", varSyntax, anyotherSyntax, raw, default_value, engine);

            for (Object object : vars) {
                if (!default_value.containsKey(object.toString())) {
                    default_value.put(object.toString(), Math.random() + "");
                }
            }

            //File tmp = File.createTempFile(Constants.APP_NAME, "form");
            File varreplaced = VariableMethods.compileFile("XXX", varSyntax, anyotherSyntax, raw, tmpdir, default_value, engine);
            String varreplaced_expr = getASCIIFileContent(varreplaced);
            String res = null;
            try {
                res = Formula.Eval(varreplaced_expr, engine);
                //Configuration.logMessage(r, LogCollector.SeverityLevel.INFO, false, "  and returns " + res);
            } catch (Formula.EvalException pse) {
                //pse.printStackTrace(System.err);
                return false;
                //Configuration.logMessage(r, LogCollector.SeverityLevel.INFO, false, "  and returns ERROR " + pse.getMessage());
            }
            return res != null && res.length() > 0;
        } catch (Exception e) {
            //e.printStackTrace(System.err);
            return false;
        }
    }
}
