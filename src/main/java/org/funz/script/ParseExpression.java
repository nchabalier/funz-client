package org.funz.script;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.funz.log.Log;
import org.funz.log.LogCollector.SeverityLevel;
import org.funz.util.Disk;
import org.funz.util.Parser;

public class ParseExpression {

    private static final List<String> STATIC_METHODS = new LinkedList<>();
    private final static String PIPE = ">>";
    public final static String FILES = "files";

    /**
     * Initializes the engine by populating the list of static methods from the org.funz.util.Parser class. (see funz-core)
     */
    static void initEngine() {
        Method[] methods = Parser.class.getMethods();
        for (Method method : methods) {
            if (!STATIC_METHODS.contains(method.getName())) {
                STATIC_METHODS.add(method.getName());
            }
        }
    }

    /**
     * Evaluates a static method on the given object with the specified arguments.
     *
     * @param o The object on which the method is invoked.
     * @param staticMethod The name of the static method to invoke.
     * @param args The arguments to pass to the method.
     * @return The result of the method invocation.
     * @throws Exception If the method cannot be invoked or does not exist.
     */
    public static Object Eval(Object o, String staticMethod, Object... args) throws Exception {
        if (o == null) {
            Log.logMessage("[ParseExpression.Call]", SeverityLevel.ERROR, true, "Calling " + staticMethod + " on args=" + Arrays.asList(args) + " for null object.");
            return null;
        }
        if (staticMethod == null) {
            Log.logMessage("[ParseExpression.Call]", SeverityLevel.ERROR, true, "Calling void method on args=" + Arrays.asList(args) + " for " + o);
            return null;
        }
        staticMethod = staticMethod.trim();
        if (args == null || args.length == 0) {
            Log.logMessage("[ParseExpression.Call]", SeverityLevel.ERROR, true, "Calling " + staticMethod + " on void args for " + o);
            return null;
        }
        Class<?>[] argsclass = new Class[args.length];
        for (int i = 0; i < argsclass.length; i++) {
            Class<?> noprim = args[i].getClass();
            if (noprim == Double.class) {
                argsclass[i] = double.class;
            } else if (noprim == Integer.class) {
                argsclass[i] = int.class;
            } else if (noprim == Character.class) {
                argsclass[i] = char.class;
            } else if (noprim == LinkedList.class) {
                argsclass[i] = List.class;
            } else {
                argsclass[i] = noprim;
            }
        }
        Method m = getMethod(o, staticMethod, argsclass);
        return m.invoke(o, args);
    }

    /**
     * Retrieves a method from the specified object or class based on the method name and argument types.
     *
     * @param o The object or class instance on which the method is to be invoked.
     * @param staticMethod The name of the method to retrieve. If it contains a dot (e.g., "ClassName.methodName"),
     *                     it is treated as a static method of the specified class.
     * @param argsclass An array of argument types that the method accepts.
     * @return The Method object representing the specified method.
     * @throws ClassNotFoundException If the class specified in the static method name cannot be found.
     * @throws NoSuchMethodException If the method with the specified name and argument types cannot be found.
     */
    private static Method getMethod(Object o, String staticMethod, Class<?>[] argsclass) throws ClassNotFoundException, NoSuchMethodException {
        Method m;
        if (staticMethod.contains(".")) { // Integer.parseInt
            String pack = "";
            // If the method starts with an uppercase letter, it is a static method of the java.lang package. like Integer.parseInt
            if (Character.isUpperCase(staticMethod.charAt(0))) {
                pack = "java.lang.";
            }
            Class<?> aClass = Class.forName(pack + staticMethod.substring(0, staticMethod.indexOf(".")));
            m = aClass.getMethod(staticMethod.substring(staticMethod.indexOf(".") + 1), argsclass);
        } else {
            m = o.getClass().getMethod(staticMethod.trim(), argsclass);
        }
        return m;
    }

    /**
     * Checks if the given string is braced or represents a valid algebraic expression.
     *
     * @param expr The string expression to check.
     * @return True if the string is braced or valid, false otherwise.
     */
    static boolean isBraced(String expr) {
        try {
            Double.parseDouble(expr);
            return true;
        } catch (NumberFormatException e) {
            return expr.trim().matches("([!\\-]*[a-zA-Z0-9.]*)\\((.*)\\)");
        }
    }

    /**
     * Evaluates an algebraic expression on the given object.
     *
     * @param o The object to evaluate the expression on.
     * @param expr The algebraic expression to evaluate.
     * @return The result of the evaluation.
     * @throws Exception If the expression cannot be evaluated.
     */
    public static Object CallAlgebra(Object o, String expr) throws Exception {
        try {
            return Double.parseDouble(expr);
        } catch (NumberFormatException ignored) {
        }
        if (expr.trim().equals("true")) {
            return true;
        }
        if (expr.trim().equals("false")) {
            return false;
        }

        if (expr.trim().startsWith("-")) {
            return -(Double) CallAlgebra(o, expr.substring(expr.indexOf('-') + 1));
        }
        if (expr.trim().startsWith("!")) {
            return !(Boolean) CallAlgebra(o, expr.substring(expr.indexOf('!') + 1));
        }
        int openBraces = 0;
        boolean simpleQuoted = false;
        boolean doubleQuoted = false;
        StringBuilder current_arg = new StringBuilder();
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            boolean escaped = false;
            if (openBraces == 0 && !simpleQuoted && !doubleQuoted) {
                if (c == '&') {
                    String left = current_arg.toString();
                    String right = expr.substring(i + 1);
                    if (right.startsWith("&")) {
                        right = right.substring(1);
                    }
                    if (isBraced(left) && isBraced(right)) {
                        return (Boolean) CallAlgebra(o, left) && (Boolean) CallAlgebra(o, right);
                    }
                } else if (c == '|') {
                    String left = current_arg.toString();
                    String right = expr.substring(i + 1);
                    if (right.startsWith("|")) {
                        right = right.substring(1);
                    }
                    if (isBraced(left) && isBraced(right)) {
                        return (Boolean) CallAlgebra(o, left) || (Boolean) CallAlgebra(o, right);
                    } else {
                        throw new Exception("Bracketing algebra operations is required: (..)" + c + "(...) in expression: "+expr);
                    }
                } else if (c == '+') {
                    String left = current_arg.toString();
                    String right = expr.substring(i + 1);
                    if (isBraced(left) && isBraced(right)) {
                        return (Double) CallAlgebra(o, left) + (Double) CallAlgebra(o, right);
                    } else {
                        throw new Exception("Bracketing algebra operations is required: (..)" + c + "(...) in expression: "+expr);
                    }
                } else if (c == '-') {
                    String left = current_arg.toString();
                    String right = expr.substring(i + 1);
                    if (isBraced(left) && isBraced(right)) {
                        return (Double) CallAlgebra(o, left) - (Double) CallAlgebra(o, right);
                    } else {
                        throw new Exception("Bracketing algebra operations is required: (..)" + c + "(...) in expression: "+expr);
                    }
                } else if (c == '*') {
                    String left = current_arg.toString();
                    String right = expr.substring(i + 1);
                    if (isBraced(left) && isBraced(right)) {
                        return (Double) CallAlgebra(o, left) * (Double) CallAlgebra(o, right);
                    } else {
                        throw new Exception("Bracketing algebra operations is required: (..)" + c + "(...) in expression: "+expr);
                    }
                } else if (c == '/') {
                    String left = current_arg.toString();
                    String right = expr.substring(i + 1);
                    if (isBraced(left) && isBraced(right)) {
                        return (Double) CallAlgebra(o, left) / (Double) CallAlgebra(o, right);
                    } else {
                        throw new Exception("Bracketing algebra operations is required: (..)" + c + "(...) in expression: "+expr);
                    }
                } else if (c == '>') {
                    String left = current_arg.toString();
                    String right = expr.substring(i + 1);
                    if (isBraced(left) && isBraced(right)) {
                        return (Double) CallAlgebra(o, left) > (Double) CallAlgebra(o, right);
                    } else {
                        //throw new Exception("Bracketing algebra operations is required: (..)" + c + "(...) in expression: "+expr);
                    }
                } else if (c == '<') {
                    String left = current_arg.toString();
                    String right = expr.substring(i + 1);
                    if (isBraced(left) && isBraced(right)) {
                        return (Double) CallAlgebra(o, left) < (Double) CallAlgebra(o, right);
                    } else {
                        //throw new Exception("Bracketing algebra operations is required: (..)" + c + "(...) in expression: "+expr);
                    }
                }
            }
            current_arg.append(c);
            if (c == '\\') {
                escaped = true;
            } else {
                if (escaped) {
                    escaped = false;
                } else {
                    if (c == '\'') {
                        if (!doubleQuoted) {
                            simpleQuoted = !simpleQuoted;
                        }
                    } else if (c == '"') {
                        if (!simpleQuoted) {
                            doubleQuoted = !doubleQuoted;
                        }
                    } else if (!simpleQuoted && !doubleQuoted) {
                        if (c == '(') {
                            openBraces++;
                        } else if (c == ')') {
                            openBraces--;
                        }
                    }
                }
            }
        }

        if (expr.contains("(")) {
            Object ret = CallMethod(o, expr);
            if (ret != null && ret.toString().equals(expr)) {
                throw new Exception("Cannot evaluate expression " + expr); //to avoid infinite loop between CallAlgebra & CallMethod
            } else {
                return ret;
            }
        } else {
            Log.err(new Exception("Cannot evaluate algebra expression " + expr), 1);
            return expr;
        }
    }

    /**
     * Evaluates a method call expression on the given object.
     *
     * @param o The object to evaluate the method call on.
     * @param expr The method call expression to evaluate.
     * @return The result of the method call.
     * @throws Exception If the method call cannot be evaluated.
     */
    public static Object CallMethod(Object o, String expr) throws Exception {
        try {
            return Double.parseDouble(expr);
        } catch (NumberFormatException ignored) {
        }
        if (expr.trim().equals("true")) {
            return true;
        }
        if (expr.trim().equals("false")) {
            return false;
        }

        if (!expr.contains("(")) {
            Object a = CallAlgebra(o, expr);
            if (a != null && a.toString().equals(expr)) {
                Log.err(new Exception("Cannot evaluate method " + expr), 1); //to avoid infinite loop between CallAlgebra & CallMethod
            }
            return a;
        }

        String method = expr.substring(0, expr.indexOf('('));
        String head = "";
        if (!method.isEmpty()) {
            int h = method.length() - 1;
            while (h > 0 && (Character.isLetterOrDigit(method.charAt(h)) || method.charAt(h) == '.' || method.charAt(h) == '_')) {
                h--;
            }
            head = h > 0 ? method.substring(0, h + 1) : "";
            method = h > 0 ? method.substring(h + 1) : method;
        }
        String args_string = expr.substring(expr.indexOf('(') + 1, expr.lastIndexOf(')'));
        String tail = expr.lastIndexOf(')') == expr.length() - 1 ? "" : expr.substring(expr.lastIndexOf(')') + 1);

        List<String> args_string_list = getArgsStringList(args_string);
        String[] args_string_array = args_string_list.toArray(new String[0]);
        Object[] args = new Object[args_string_array.length];
        for (int i = 0; i < args.length; i++) {
            String arg_string = args_string_array[i].trim();
            if (arg_string.startsWith("\"") && arg_string.endsWith("\"")) {
                args[i] = arg_string.substring(1, arg_string.length() - 1).replace("\\\"","\"");
                continue;
            }
            if (arg_string.startsWith("'") && arg_string.endsWith("'") && arg_string.length() == 3) {
                args[i] = ""+arg_string.charAt(1);
                continue;
            }
            try {
                int in = Integer.parseInt(arg_string);
                args[i] = in;
                continue;
            } catch (NumberFormatException ignored) {
            }
            try {
                double d = Double.parseDouble(arg_string);
                args[i] = d;
                continue;
            } catch (NumberFormatException ignored) {
            }
            args[i] = CallAlgebra(o, arg_string.trim());
        }
        if (method.isEmpty() && args.length == 1) {//not a function call, just a bracket eval eg. (1+1)*3
            return CallAlgebra(o, head + args[0].toString() + tail);
        } else {
            if (head.isEmpty() && tail.isEmpty()) {
                return Eval(o, method, args);
            } else {
                return CallAlgebra(o, head + Eval(o, method, args) + tail);
            }
        }
    }

    /**
     * Parses a string of arguments into a list of individual argument strings.
     *
     * @param args_string The string containing the arguments.
     * @return A list of argument strings.
     */
    private static List<String> getArgsStringList(String args_string) {
        List<String> args_string_list = new LinkedList<>();
        int openBraces = 0;
        boolean escaped = false;
        boolean simpleQuoted = false;
        boolean doubleQuoted = false;
        StringBuilder current_arg = new StringBuilder();
        for (int i = 0; i < args_string.length(); i++) {
            char c = args_string.charAt(i);
            if (c == ',' && openBraces == 0 && !simpleQuoted && !doubleQuoted) {
                args_string_list.add(current_arg.toString());
                current_arg = new StringBuilder();
                continue;
            }
            current_arg.append(c);
            if (c == '\\') {
                escaped = true;
            } else {
                if (escaped) {
                    escaped = false;
                } else {
                    if (c == '\'') {
                        if (!doubleQuoted) {
                            simpleQuoted = !simpleQuoted;
                        }
                    } else if (c == '\"') {
                        if (!simpleQuoted) {
                            doubleQuoted = !doubleQuoted;
                        }
                    } else if (!simpleQuoted && !doubleQuoted) {
                        if (c == '(') {
                            openBraces++;
                        } else if (c == ')') {
                            openBraces--;
                        }
                    }
                }
            }
        }
        args_string_list.add(current_arg.toString());
        return args_string_list;
    }

    /**
     * Retrieves the list of syntax elements (static methods) available for parsing.
     *
     * @return A list of syntax elements.
     */
    public static List<String> getSyntax() {
        return new LinkedList<>(STATIC_METHODS);
    }

    /**
     * Converts a user-defined expression into a generalized expression format.
     *
     * @param userExpression The user-defined expression.
     * @return The generalized expression.
     */
    static String importExpression(String userExpression) {
        String gexpr = userExpression;

        while (gexpr.contains(PIPE)) {
            int i = gexpr.indexOf(PIPE);
            int j = gexpr.indexOf("(", i + PIPE.length());
            if (gexpr.substring(j + 1).trim().startsWith(")")) {//no more arg
                gexpr = gexpr.substring(i + PIPE.length(), j + 1) + gexpr.substring(0, i) + gexpr.substring(j + 1);
            } else {
                gexpr = gexpr.substring(i + PIPE.length(), j + 1) + gexpr.substring(0, i) + "," + gexpr.substring(j + 1);
            }
        }
        return gexpr;
    }

    /**
     * Exports an object, simplifying it if it is a list with a single element.
     *
     * @param go The object to export.
     * @return The simplified or original object.
     */
    static Object exportObject(Object go) {
        if (go instanceof List<?>) {
            List<?> golist = (List<?>) go;
            if (golist.size() == 1) {
                return exportObject(golist.get(0));
            } else {
                return golist;
            }
        }

        return go;
    }

    /**
     * Evaluates an expression using the provided parameters.
     *
     * @param expression The expression to evaluate.
     * @param outputNamesValues A map expression parameter names and their values.
     * @return The result expression the evaluation.
     */
    public static Object eval(Object expression, Map<String, Object> outputNamesValues) {
        assert expression instanceof String : "Problem trying to eval " + expression.toString() + " : not a String";
        return eval((String) expression, outputNamesValues);
    }

    /**
     * Evaluates a function or expression using the provided parameters.
     *
     * @param expression The function or expression to evaluate.
     * @param params A map of parameter names and their values.
     * @return The result of the evaluation.
     */
    public synchronized static Object eval(String expression, Map<String, Object> params) {
        Object out = null;
        List<File> rFiles = new LinkedList<>();
        try {
            Log.logMessage("ParseExpression", SeverityLevel.INFO, false, "eval(" + expression + "," + (params != null ? params.toString() : "null") + ")");

            if(params == null) {
                Log.logMessage("ParseExpression", SeverityLevel.ERROR, false, "params is null");
                return null;
            }
            if (params.containsKey(expression)) {
                return params.get(expression);
            }
            File[] files = null;
            if (!(params.get(FILES) instanceof File[])) {
                try {
                    Object[] files_o = (Object[]) params.get(FILES);
                    if (files_o != null) {
                        files = new File[files_o.length];
                        for (int i = 0; i < files_o.length; i++) {
                            files[i] = (File) files_o[i];
                        }
                    }
                } catch (Exception e) {
                    Log.logException(true, e);
                }
            } else {
                files = (File[]) params.get(FILES);
            }

            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        rFiles.add(file);
                    } else {
                        try {
                            Disk.listRecursiveFiles(file, rFiles);
                        } catch (Exception e) {
                            Log.logException(true, e);
                        }
                    }
                }
            }

            Parser o = new Parser(rFiles.toArray(new File[0]));
            if (expression.startsWith("`") && expression.endsWith("`")) {
                expression = expression.substring(1, expression.length() - 1);
            }
            String ie = importExpression(expression);
            out = exportObject(CallAlgebra(o, ie));
            Log.logMessage("ParseExpression", SeverityLevel.INFO, false, "  >> " + out);
        } catch (Exception e) {
            Log.logException(false, new Exception(e.getClass()+": Failed to evaluate expression " + expression + " on files " + rFiles + "\n" + e.getMessage()));
        }
        return out;
    }
}
