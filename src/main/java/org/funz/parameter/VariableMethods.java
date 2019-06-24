package org.funz.parameter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.funz.log.Log;
import org.funz.log.LogCollector.SeverityLevel;
import org.funz.parameter.Formula.EvalException;
import org.funz.script.MathExpression;
import org.funz.util.ASCII;
import static org.funz.util.Format.toHTML;
import static org.funz.util.ParserUtils.getAllLinesStarting;

/**
 * Variable methods and constants
 */
public class VariableMethods {

    public static class ParseEvalException extends Exception {

        public int line = 0, column = 0;
        public String expression;
        public File file;

        public ParseEvalException(File f, int line, int column, String expression, String error) {
            super("Error while evaluating " + expression + " @ line " + (line + 1) /*+ " column " + column*/ + " in " + f.getName()+": "+error);
            this.line = line;
            this.column = column;
            this.expression = expression;
            this.file = f;
        }

        public final static int readColumn(EvalException ex) {
            // TODO
            return 0;
        }
    }

    public static class BadSyntaxException extends Exception {

        public int line = 0;
        public File file;
        public String txt;

        public BadSyntaxException(File f, int line, String txt, String reason) {
            super("Bad syntax in " + f.getName() + " @ line " + (line + 1) + ": " + reason);
            file = f;
            this.line = line;
            this.txt = txt;
        }
    }

    public static interface LoadProgressObserver {

        public void parsingNewLine(String line);
    }

    /**
     * Variabe single value structure
     */
    public static class Value implements Cloneable {

        public static List<Value> asValueList(String... v) {
            LinkedList<Value> l = new LinkedList<>();
            for (String s : v) {
                l.add(new Value(s));
            }
            return l;
        }

        private boolean selected = true;
        private String value = "", alias = "";

        /**
         * Constructs a Value with its initial value
         */
        public Value(String v) {
            value = v;
        }

        /**
         * Constructs a Value with its initial value and alias
         */
        public Value(String v, String a) {
            value = v;
            alias = a;
        }

        @Override
        public Object clone() {
            Value ret = new Value(value, alias);
            ret.setSelected(selected);
            return ret;
        }

        @Override
        public boolean equals(Object other) {
            Value v = (Value) other;
            return (alias.length() == 0 && v.getAlias().length() == 0 && v.getValue().equals(value)) || ((alias.length() != 0 && v.getAlias().length() != 0) && v.getAlias().equals(alias));
        }

        public String getAlias() {
            return alias;
        }

        public String getValue() {
            return value;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setAlias(String a) {
            alias = a;
        }

        public void setSelected(boolean on) {
            selected = on;
        }

        @Override
        public String toString() {
            return value + ":[" + alias + "]";
        }
    }

    public static Object castValue(String type, String valueStr) {
        if (type.equals(TYPE_INT)) {
            return Integer.parseInt(valueStr);
        }
        if (type.equals(TYPE_REAL) || type.equals(TYPE_CONTINUOUS)) {
            return Double.parseDouble(valueStr);
        }
        if (type.equals(TYPE_STRING)) {
            return valueStr;
        }
        if (type.equals(TYPE_TEXTFILE)) {
            return new File(valueStr);
        }
        return null;
    }
    public static final int DEFAULT_ROUND_OFF = 5;
    public static final int NO_ROUND_OFF = -1;
    public static DecimalFormatSymbols DOT;
    protected static DecimalFormat FORMAT;
    public static final String TYPE_REAL = "real", TYPE_INT = "integer", TYPE_STRING = "string", TYPE_TEXTFILE = "text file", TYPE_CONTINUOUS = "continuous real", TYPE_STRINGS[] = {TYPE_CONTINUOUS, TYPE_REAL, TYPE_INT, TYPE_STRING, TYPE_TEXTFILE}, DISC_TYPE_STRINGS[] = {TYPE_REAL, TYPE_INT, TYPE_STRING, TYPE_TEXTFILE};

    static {
        DOT = new DecimalFormatSymbols();
        DOT.setDecimalSeparator('.');
        FORMAT = new DecimalFormat("#.00000", DOT);
    }

    public static File compileFile(String commentLine, SyntaxRules varSyntax, SyntaxRules formSyntax, File src, File dir, Map vars, MathExpression formulaEngine) throws Exception {
        File ret = new File(dir + File.separator + src.getName());
        if (ret.exists()) {
            Log.logMessage("VariableMethods.compileFile", SeverityLevel.WARNING, false, "File " + ret.getName() + " already exists ! \n  added prefix '_'");
            ret = new File(dir + File.separator + "_" + src.getName());
        }
        parseFile(commentLine, varSyntax, formSyntax, src, ret, vars, null, null, null, null, null, null, formulaEngine);
        return ret;
    }

    public static String format(double value, boolean _isScientific, int roundOff) {
        DecimalFormat df = getDecimalFormat(_isScientific, roundOff);
        return df.format(value);
    }

    public static String format(double value, DecimalFormat df) {
        return df.format(value);
    }

    public static String getFormatPattern(String... x) {
        int ent = 0;
        for (String s : x) {
            ent = Math.max(ent, s.indexOf(DOT.getDecimalSeparator()));
        }

        int dec = 0;
        for (String s : x) {
            dec = Math.max(dec, s.length() - s.indexOf(DOT.getDecimalSeparator()) - 1);
        }

        StringBuffer f = new StringBuffer();
        for (int i = 0; i < ent; i++) {
            f.append("0");
        }
        f.append(DOT.getDecimalSeparator());
        for (int i = 0; i < dec; i++) {
            f.append("0");
        }
        return f.toString();
    }

    public static DecimalFormat getDecimalFormat(boolean isScientific, int roundOff) {
        StringBuffer sb = new StringBuffer();
        sb.append("#.");
        for (int i = 0; i < roundOff - 3; i++) {
            sb.append("0");
        }
        if (isScientific) {
            sb.append("E00");
        } else {
            sb.append("000");
        }
        return new DecimalFormat(sb.toString(), DOT);
    }

    public static boolean isCorrectAlias(String var) {
        if (var == null || var.length() < 1) {
            return false;
        }

        if (!isWordChar(var.charAt(0))) {
            return false;
        }

        for (int i = 1; i < var.length(); i++) {
            char c = var.charAt(i);
            if (!(isWordChar(c) || c == '.' || c == '=')) {
                return false;
            }
        }

        return true;
    }

    public static boolean isCorrectName(String var) {
        if (var == null || var.length() < 1) {
            return false;
        }

        if (!isFirstWordChar(var.charAt(0))) {
            return false;
        }

        for (int i = 1; i < var.length(); i++) {
            char c = var.charAt(i);
            if (!isWordChar(c)) {
                return false;
            }
        }

        if (var.contains("=")) {
            return false;
        }

        return true;
    }

    public static boolean isFirstWordChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    public static boolean isWordChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_' || c == '.';
    }

    public static HashSet parseFile(String commentLine, SyntaxRules varSyntax, SyntaxRules formSyntax, File file, HashMap<String, String> default_value, MathExpression formulaEngine) throws Exception {
        return parseFile(commentLine, varSyntax, formSyntax, file, null, null, null, null, null, null, null, default_value, formulaEngine);
    }
    public final static char DEFAULT_VALUE_CHAR = '~';
    public final static String DEFAULT_MODEL_SEPARATOR = ";";
    public final static String DEFAULT_VALUE_STR = "" + DEFAULT_VALUE_CHAR;

    /*public static void main(String[] args) {
     Configuration.readProperties(null);
     MathExpression.SetDefaultInstance(RMathExpression.class);
     MathExpression.LogFrame.setVisible(true);

     SyntaxRules varSyntax = new SyntaxRules(6, 1);

     System.err.println(varSyntax.toString());
     try {
     System.out.println(parseFileVars(varSyntax, new File("test.in"), new File("test.out"), null, null, null));
     } catch (Exception ex) {
     ex.printStackTrace();
     }
     }*/
    public synchronized static HashSet parseFileVars(SyntaxRules varSyntax, File file, File trg, Map values, LinkedList replaceables, Map<String, String> defaultmodels)
            throws UnsupportedEncodingException, FileNotFoundException, IOException, BadSyntaxException {
        long tic = Calendar.getInstance().getTimeInMillis();
        char varStart = varSyntax.getStartSymbol(),
                varLLimit = varSyntax.getLeftLimitSymbol(),
                varRLimit = varSyntax.getRightLimitSymbol();

        HashSet vars = new HashSet();

        NumberFormat nf = NumberFormat.getIntegerInstance();
        nf.setGroupingUsed(false);
        nf.setMinimumIntegerDigits(6);

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), ASCII.CHARSET));
        PrintWriter writer = trg == null ? null : new PrintWriter(trg, ASCII.CHARSET);

        List<String> alreadyadded = new LinkedList<String>();

        String line;
        boolean inVariable = false, inOpenVariable = false;
        int varStartCol = 0;
        int openVarLimit = 0;
        boolean inModel = false;

        int lineNumber = 0, lastTag = -1;
        StringBuffer out = null;
        while ((line = reader.readLine()) != null) {

            StringBuffer var = null;
            if (trg != null) {
                out = new StringBuffer();
            }

            for (int pos = 0; pos < line.length(); pos++) {
                char c = line.charAt(pos);
                // last line symbol
                if (pos == line.length() - 1) {
                    if (inVariable) {
                        if (c != varRLimit || (inModel && openVarLimit > 0)) { // $(VARNAM\n
                            var.append(c);
                            reader.close();
                            if (writer != null) {
                                writer.close();
                            }
                            throw new BadSyntaxException(file, lineNumber, var.toString(), "variable " + var.toString() + " not closed by " + varRLimit + " before EOL :\n" + line);
                        } else { // $(VARNAME)\n
                            String name = var.toString();
                            if (name.contains(DEFAULT_VALUE_STR)) {
                                name = name.substring(0, name.indexOf(DEFAULT_VALUE_STR));

                                if (defaultmodels != null) {
                                    String prev = defaultmodels.get(name);
                                    if (prev != null) {
                                        prev = prev + DEFAULT_MODEL_SEPARATOR;
                                    } else {
                                        prev = "";
                                    }
                                    defaultmodels.put(name, prev + var.substring(name.length() + 1));
                                }
                            }
                            if (replaceables != null && !alreadyadded.contains(name)) {
                                replaceables.add(new Replaceable(name, lineNumber, varStartCol, pos));
                                alreadyadded.add(name);
                            }
                            vars.add(name);
                            inVariable = false;

                            if (out != null) {
                                if (values != null && values.containsKey(name)) {
                                    out.append(values.get(name));
                                } else {
                                    out.append(varStart).append(varLLimit).append(var).append(varRLimit);
                                }
                            }
                            break;
                        }
                    }

                    if (inOpenVariable) { // $VARNAME\n

                        if (isWordChar(c)) {
                            var.append(c);
                            String name = var.toString();
                            if (replaceables != null && !alreadyadded.contains(name)) {
                                replaceables.add(new Replaceable(name, lineNumber, varStartCol, pos));
                                alreadyadded.add(name);
                            }
                            vars.add(name);
                            inOpenVariable = false;

                            if (out != null) {
                                if (values != null && values.containsKey(name)) {
                                    out.append(values.get(name));
                                } else {
                                    out.append(varStart).append(var);
                                }
                            }
                        } else {
                            String name = var.toString();
                            if (replaceables != null && !alreadyadded.contains(name)) {
                                replaceables.add(new Replaceable(name, lineNumber, varStartCol, pos - 1));
                                alreadyadded.add(name);
                            }
                            vars.add(name);
                            inOpenVariable = false;

                            if (out != null) {
                                if (values != null && values.containsKey(name)) {
                                    out.append(values.get(name));
                                } else {
                                    out.append(varStart).append(var);
                                }
                                out.append(c);
                            }
                        }
                        break;
                    }

                    if (out != null) {
                        out.append(c);
                    }
                    break;
                }

                if (inVariable) { // $(VARNAM
                    if (!inModel) {
                        if (isWordChar(c)) {//(c == '-' && var.charAt(var.length() - 1) == 'E') || (c == '-' && var.charAt(var.length() - 1) == DEFAULT_VALUE_CHAR)) {
                            var.append(c);
                        } else if (c == DEFAULT_VALUE_CHAR) { //$(VAR~
                            var.append(c);
                            inModel = true;
                        } else if (c == varRLimit) { // $(VARNAME)
                            String name = var.toString();
                            if (name.contains(DEFAULT_VALUE_STR)) {
                                name = name.substring(0, name.indexOf(DEFAULT_VALUE_STR));

                                if (defaultmodels != null) {
                                    String prev = defaultmodels.get(name);
                                    if (prev != null) {
                                        prev = prev + DEFAULT_MODEL_SEPARATOR;
                                    } else {
                                        prev = "";
                                    }
                                    defaultmodels.put(name, prev + var.substring(name.length() + 1));
                                }
                            }
                            if (replaceables != null && !alreadyadded.contains(name)) {
                                replaceables.add(new Replaceable(name, lineNumber, varStartCol, pos));
                                alreadyadded.add(name);
                            }
                            vars.add(name);
                            inVariable = false;

                            if (out != null) {
                                if (values != null && values.containsKey(name)) {
                                    out.append(values.get(name));
                                } else {
                                    out.append(varStart).append(varLLimit).append(var).append(varRLimit);
                                }
                            }
                        } else { // $(VARNAME + 10
                            reader.close();
                            if (writer != null) {
                                writer.close();
                            }
                            throw new BadSyntaxException(file, lineNumber, var.toString(), "variable " + var.toString() + " not closed by " + varRLimit + " :\n" + line);
                        }
                    } else {//$(VAR~
                        if (c == varRLimit) {
                            if (openVarLimit == 0) {
                                String name = var.toString();
                                if (name.contains(DEFAULT_VALUE_STR)) {
                                    name = name.substring(0, name.indexOf(DEFAULT_VALUE_STR));

                                    if (defaultmodels != null) {
                                        String prev = defaultmodels.get(name);
                                        if (prev != null) {
                                            prev = prev + DEFAULT_MODEL_SEPARATOR;
                                        } else {
                                            prev = "";
                                        }
                                        defaultmodels.put(name, prev + var.substring(name.length() + 1));
                                    }
                                }
                                if (replaceables != null && !alreadyadded.contains(name)) {
                                    replaceables.add(new Replaceable(name, lineNumber, varStartCol, pos));
                                    alreadyadded.add(name);
                                }
                                vars.add(name);
                                inModel = false;
                                inVariable = false;

                                if (out != null) {
                                    if (values != null && values.containsKey(name)) {
                                        out.append(values.get(name));
                                    } else {
                                        out.append(varStart).append(varLLimit).append(var).append(varRLimit);
                                    }
                                }
                            } else {
                                openVarLimit--;
                                var.append(c);
                            }
                        } else if (c == varLLimit) {
                            openVarLimit++;
                            var.append(c);
                        } else {
                            var.append(c);
                        }
                    }

                } else if (inOpenVariable) {
                    if (isWordChar(c)) { // $VARN
                        var.append(c);
                    } else { // $VAR ...
                        String name = var.toString();
                        if (replaceables != null && !alreadyadded.contains(name)) {
                            replaceables.add(new Replaceable(name, lineNumber, varStartCol, pos - 1));
                            alreadyadded.add(name);
                        }
                        vars.add(name);
                        inOpenVariable = false;
                        if (out != null) {
                            if (values != null && values.containsKey(name)) {
                                out.append(values.get(name));
                            } else {
                                out.append(varStart).append(var);
                            }
                            if (c != varStart) {
                                out.append(c);
                            }
                        }

                        // $VAR$FOOBAR
                        if (c == varStart) {
                            pos--;
                        }
                    }
                } else if (c == varStart) { // $
                    char next = line.charAt(pos + 1);

                    if (isFirstWordChar(next)) { // $V
                        varStartCol = pos;
                        var = new StringBuffer();
                        var.append(next);

                        pos++;
                        if (pos == line.length() - 1) {
                            String name = var.toString();
                            /*if (name.contains(DEFAULT_VALUE_STR)) {
                             name = name.substring(0, name.indexOf(DEFAULT_VALUE_STR));
                             if (defaultvalues != null) {
                             String prev = defaultvalues.get(name);
                             if (prev != null) {
                             prev = prev + DEFAULT_MODEL_SEPARATOR;
                             } else {
                             prev = "";
                             }
                             defaultvalues.put(name, prev + var.substring(name.length() + 1));
                             }
                             }*/
                            if (replaceables != null && !alreadyadded.contains(name)) {
                                replaceables.add(new Replaceable(name, lineNumber, varStartCol, varStartCol + 1));
                                alreadyadded.add(name);
                            }
                            vars.add(name);
                            lastTag = lineNumber;

                            if (out != null) {
                                if (values != null && values.containsKey(name)) {
                                    out.append(values.get(name));
                                } else {
                                    out.append(varStart).append(var);
                                }
                            }
                        } else {
                            inOpenVariable = true;
                            lastTag = lineNumber;
                        }
                    } else if (next == varLLimit) {// $(
                        if (pos < line.length() - 2) {
                            next = line.charAt(pos + 2);
                            if (isFirstWordChar(next)) { // $(V
                                varStartCol = pos;
                                var = new StringBuffer();
                                var.append(next);
                                pos += 2;
                                if (pos == line.length() - 1) {
                                    reader.close();
                                    if (writer != null) {
                                        writer.close();
                                    }
                                    throw new BadSyntaxException(file, lineNumber, var.toString(), "variable " + var.toString() + " not closed by " + varRLimit + " before eol :\n" + line);
                                }
                                inVariable = true;
                                lastTag = lineNumber;
                            } else {
                                reader.close();
                                if (writer != null) {
                                    writer.close();
                                }
                                throw new BadSyntaxException(file, lineNumber, var.toString(), "bad syntax " + varStart + varRLimit + "..." + " :\n" + line);
                            }
                        } else {
                            reader.close();
                            if (writer != null) {
                                writer.close();
                            }
                            throw new BadSyntaxException(file, lineNumber, var.toString(), "bad syntax " + varStart + varRLimit + "..." + " :\n" + line);
                        }
                    }
                } else {
                    if (out != null) {
                        out.append(c);
                    }
                }
            }

            lineNumber++;
            if (writer != null) {
                writer.println(out);
            }
        }
        reader.close();
        if (writer != null) {
            writer.close();
        }
        long toc = Calendar.getInstance().getTimeInMillis();
        Log.logMessage("VariableMethods.parseFileVars", SeverityLevel.INFO, false, "parse " + file.getName() + " vars in " + ((toc - tic) / 1000.0) + " s.");
        return vars;
    }

    private static String[] getFunctionalComments(BufferedReader in, String execCommentStart) {
        List<String> lines = new LinkedList<String>();
        String tmp;

        try {
            int openbraces = 0;
            StringBuilder toexec = new StringBuilder();
            while ((tmp = in.readLine()) != null) {
                if (tmp.startsWith(execCommentStart)) {
                    //System.err.println(tmp);
                    String todo = tmp.substring(execCommentStart.length());
                    if (todo.endsWith("\\")) {
                        todo = todo.substring(0, todo.length() - 1);
                    }
                    //System.err.println("=> " + todo);
                    openbraces += StringUtils.countMatches(todo, "{") - StringUtils.countMatches(todo, "}");
                    //System.err.println("   openbraces="+openbraces);
                    if (toexec.length() > 0) {
                        toexec.append('\n');
                    }
                    toexec.append(todo);
                    if (openbraces <= 0) {
                        lines.add(toexec.toString());
                        toexec.delete(0, toexec.length());
                        openbraces = 0;
                    }
                } else {
                    if (toexec.length() > 0) {
                        lines.add(toexec.toString());
                        toexec.delete(0, toexec.length());
                        openbraces = 0;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
        String[] lines_string = new String[lines.size()];
        for (int i = 0; i < lines_string.length; i++) {
            lines_string[i] = lines.get(i);
        }

        return lines_string;
    }

    public static String[] getFunctionalComments(File file, String start) {
        BufferedReader in = null;
        InputStreamReader isr = null;
        FileInputStream fis = null;
        String[] ret = null;
        try {
            fis = new FileInputStream(file);
            isr = new InputStreamReader(fis, ASCII.CHARSET);
            in = new BufferedReader(isr);
            //in = new BufferedReader(new InputStreamReader(new FileInputStream(file), CHARSET));
            ret = getFunctionalComments(in, start);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        } finally {
            try {
                fis.close();
                isr.close();
                in.close();
            } catch (Exception ee) {
                ee.printStackTrace(System.err);
            }

        }
        return ret;
    }

    public static final String MATHENGINE_SET_MARKER = ":";
    public static final String MATHENGINE_TEST_MARKER = "?";    
    
    public synchronized static void parseFileForms(String commentLine, SyntaxRules formSyntax, File file, File trg, LinkedList replaceables, MathExpression formulaEngine) //
            throws UnsupportedEncodingException, FileNotFoundException, IOException, BadSyntaxException, ParseEvalException, MathExpression.MathException { //

        long tic = Calendar.getInstance().getTimeInMillis();
        char frmStart = formSyntax.getStartSymbol(),
                frmLLimit = formSyntax.getLeftLimitSymbol(),
                frmRLimit = formSyntax.getRightLimitSymbol();

        Object lock;
        if (formulaEngine != null) {
            //formulaEngine.reset();
            String execCommentStart = commentLine + frmStart + MATHENGINE_SET_MARKER;
            String[] fcomments = getFunctionalComments(file, execCommentStart);
            for (String fc : fcomments) {
                if (!formulaEngine.set(fc/*.split("\n")*/)) {
                    throw new MathExpression.MathException("Bad instruction: " + fc);
                }
            }
            String testCommentStart = commentLine + frmStart + MATHENGINE_TEST_MARKER;
            String[] testLines = getAllLinesStarting(file, testCommentStart);
            for (String test : testLines) {
                Object t = formulaEngine.eval(test.substring(testCommentStart.length()), null);
                if (t == null || !((Boolean) t)) {
                    throw new MathExpression.MathException("Failed test: " + test);
                }
            }
            lock = formulaEngine;
        } else {
            lock = new Object();
        }

        synchronized (lock) {

            NumberFormat nf = NumberFormat.getIntegerInstance();
            nf.setGroupingUsed(false);
            nf.setMinimumIntegerDigits(6);

            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), ASCII.CHARSET));
            PrintWriter writer = trg == null ? null : new PrintWriter(trg, ASCII.CHARSET);

            String line;
            boolean inFormula = false;
            int frmStartCol = 0, frmLine = 0, frmLastLine = -1;

            int lineNumber = 0, lastTag = -1;
            StringBuffer formula = null;
            StringBuffer eval = null;
            StringBuffer out = null;
            while ((line = reader.readLine()) != null) {

                if (trg != null && !inFormula) {
                    out = new StringBuffer();
                }

                for (int pos = 0; pos < line.length(); pos++) {
                    char c = line.charAt(pos);

                    if (inFormula) {
                        formula.append(c);
                    }

                    // last line symbol
                    if (pos == line.length() - 1) {

                        if (inFormula && c == frmRLimit) // ${ 10 + sin(4) }\n
                        {
                            String name = formula.toString();
                            if (replaceables != null) {
                                replaceables.add(new Replaceable(name, frmLine, lineNumber, frmStartCol, pos));
                            }
                            inFormula = false;
                            frmLastLine = lineNumber;

                            if (out != null) {
                                //eval.append(")");
                                try {
                                    out.append(Formula.Eval(eval.toString(), formulaEngine));
                                } catch (EvalException ex) {
                                    reader.close();
                                    if (writer != null) {
                                        writer.close();
                                    }
                                    throw new ParseEvalException(file, lineNumber, ParseEvalException.readColumn(ex), eval.toString(),formulaEngine.getLastMessage()+"\n"+ex.getMessage());
                                }
                            }
                            break;
                        }

                        if (out != null) {
                            if (inFormula) {
                                eval.append(c);
                            } else {
                                out.append(c);
                            }
                        }
                        break;
                    }

                    if (inFormula && c == frmRLimit) {
                        String name = formula.toString();
                        if (replaceables != null) {
                            replaceables.add(new Replaceable(name, frmLine, lineNumber, frmStartCol, pos));
                        }
                        inFormula = false;
                        frmLastLine = lineNumber;

                        if (out != null) {
                            //eval.append(")");
                            try {
                                out.append(Formula.Eval(eval.toString(), formulaEngine));
                            } catch (EvalException ex) {
                                reader.close();
                                if (writer != null) {
                                    writer.close();
                                }
                                throw new ParseEvalException(file, lineNumber, ParseEvalException.readColumn(ex), eval.toString(),formulaEngine.getLastMessage());
                            }
                        }
                    } else if (c == frmStart) {
                        char next = line.charAt(pos + 1);

                        //YR
                        if (inFormula) {
                            formula.append(next);
                        }

                        if (inFormula && next == frmLLimit) {
                            reader.close();
                            if (writer != null) {
                                writer.close();
                            }
                            throw new BadSyntaxException(file, lineNumber, formula.toString(), c + " formula inside formula are forbiden" + " :\n" + line);
                        }

                        if (next == frmLLimit) {
                            frmLine = lineNumber;
                            frmStartCol = pos;
                            inFormula = true;
                            lastTag = lineNumber;

                            formula = new StringBuffer();
                            formula.append(c);
                            formula.append(next);
                            pos++;
                            eval = new StringBuffer();
                            //eval.append("(");
                        } else {
                            if (out != null) {
                                if (inFormula) {
                                    eval.append(c);
                                } else {
                                    out.append(c);
                                }
                            }
                        }
                    } else {
                        if (out != null) {
                            if (inFormula) {
                                eval.append(c);
                            } else {
                                out.append(c);
                            }
                        }
                    }
                }

                lineNumber++;
                if (writer != null && !inFormula) {
                    writer.println(out);
                }
            }
            reader.close();
            if (writer != null) {
                writer.close();
            }

        }
        long toc = Calendar.getInstance().getTimeInMillis();
        Log.logMessage("VariableMethods.parseFileForms", SeverityLevel.INFO, false, "parse " + file.getName() + " forms in " + ((toc - tic) / 1000.0) + " s.");
    }

    public synchronized static HashSet parseFile(String commentLine,
            SyntaxRules varSyntax, //
            SyntaxRules formSyntax, //
            File file, File trg, //
            Map values, //
            StringBuffer content, //
            StringBuffer html, //
            LinkedList replaceables, //
            Map lines, LoadProgressObserver observer, //
            Map<String, String> defaultmodels, //
            MathExpression formulaEngine) throws UnsupportedEncodingException, FileNotFoundException, IOException, BadSyntaxException, ParseEvalException, MathExpression.MathException {
        File trg_vars = File.createTempFile("vars_", (trg == null ? "" + file.hashCode() : trg.getName()), (trg == null ? new File(".") : trg.getParentFile()));
        //new File(System.getProperty("java.io.tmpdir"), "vars_" + (trg == null ? file.hashCode() : trg.getName()));
        HashSet vars = parseFileVars(varSyntax, file, trg_vars, values, replaceables, defaultmodels);
        parseFileForms(commentLine, formSyntax, trg_vars, trg, replaceables, formulaEngine);
        trg_vars.delete();
        return vars;
    }

    public static String toHtmlChar(char c) {
        return toHTML(c + "");
        /*if (c == '<') {
         return "&lt;";
         }
         if (c == '&') {
         return "&amp;";
         }
         return "" + c;*/
    }
}
