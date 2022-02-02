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

    public static List<String> StaticMethods = new LinkedList<String>();
    public final static String PIPE = ">>";
    public final static String FILES = "files";

    static void initEngine() {
        Method[] methods = Parser.class.getMethods();
        for (Method method : methods) {
            if (!StaticMethods.contains(method.getName())) {
                StaticMethods.add(method.getName());
            }
        }
    }

    public static Object Eval(Object o, String staticMethod, Object... args) throws Exception {
//        System.err.println("---------------------> Eval " + staticMethod + " ... " + Arrays.asList(args));
        if (o == null) {
            Log.logMessage("[ParseExpression.Call]", SeverityLevel.WARNING, true, "Calling " + staticMethod + " on args=" + Arrays.asList(args) + " for null object.");
        }
        if (staticMethod == null) {
            Log.logMessage("[ParseExpression.Call]", SeverityLevel.WARNING, true, "Calling void method on args=" + Arrays.asList(args) + " for " + o);
        }
        staticMethod = staticMethod.trim();
        if (args == null || args.length == 0) {
            Log.logMessage("[ParseExpression.Call]", SeverityLevel.WARNING, true, "Calling " + staticMethod + " on void args for " + o);
        }
        Class[] argsclass = new Class[args.length];
        for (int i = 0; i < argsclass.length; i++) {
            Class noprim = args[i].getClass();
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
        Method m = null;
        if (staticMethod.contains(".")) { // Integer.parseInt
            String pack = "";
            if (Character.isUpperCase(staticMethod.charAt(0))) {
                pack = "java.lang.";
            }
            Class classs = Class.forName(pack + staticMethod.substring(0, staticMethod.indexOf(".")));
            m = classs.getMethod(staticMethod.substring(staticMethod.indexOf(".") + 1), argsclass);
        } else {
            m = o.getClass().getMethod(staticMethod.trim(), argsclass);
        }
        Object out = m.invoke(o, args);
//        System.err.println("-----------------------------------------------------> " + out);
        return out;
    }

    static boolean isBraced(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch (NumberFormatException e) {
            return s.trim().matches("([\\!\\-]*[a-zA-Z0-9\\.]*)\\((.*)\\)");// startsWith("(") && s.endsWith(")");
        }
    }

    public static Object CallAlgebra(Object o, String expr) throws Exception {
//        System.err.println("> CallAlgebra " + expr);
        try {
            Double d = Double.parseDouble(expr.toString());
            return d;
        } catch (NumberFormatException e) {
        }
        if (expr.toString().trim().equals("true")) {
            return true;
        }
        if (expr.toString().trim().equals("false")) {// pffff...
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
                    } else if (!simpleQuoted && !doubleQuoted && !escaped) {
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
            if (ret != null && ret.toString().equals(expr.toString())) {
                throw new Exception("Cannot evaluate expression " + expr); //to avoid infinite loop between CallAlgebra & CallMethod
            } else {
                return ret;
            }
        } else {
            Log.err(new Exception("Cannot evaluate algebra expression " + expr), 1);
            return expr;
        }
    }

    public static Object CallMethod(Object o, String expr) throws Exception {
//        System.err.println("> CallMethod " + expr);
        try {
            Double d = Double.parseDouble(expr);
            return d;
        } catch (NumberFormatException e) {
        }
        if (expr.trim().equals("true")) {
            return true;
        }
        if (expr.trim().equals("false")) {// pffff...
            return false;
        }

        if (!expr.contains("(")) {
            Object a = CallAlgebra(o, expr);
            if (a != null && a.toString().equals(expr.toString())) {
                Log.err(new Exception("Cannot evaluate method " + expr), 1); //to avoid infinite loop between CallAlgebra & CallMethod
            }
            return a;
        }

        String method = expr.substring(0, expr.indexOf('('));
        String head = "";
        if (method.length() > 0) {
            int h = method.length() - 1;
            while (h > 0 && (Character.isLetterOrDigit(method.charAt(h)) || method.charAt(h) == '.')) {
                h--;
            }
            head = h > 0 ? method.substring(0, h + 1) : "";
            method = h > 0 ? method.substring(h + 1) : method;
        }
        String args_string = expr.substring(expr.indexOf('(') + 1, expr.lastIndexOf(')'));
        String tail = expr.lastIndexOf(')') == expr.length() - 1 ? "" : expr.substring(expr.lastIndexOf(')') + 1);

//        System.err.println("method " + method);
//        System.err.println("head " + head);
//        System.err.println("tail " + tail);
        List<String> args_string_list = new LinkedList<String>();
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
//                System.err.println(">>> " + args_string_list);
                continue;
            }
//            System.err.println(current_arg);
            //System.err.println(StringUtils.repeat(" ",i)+c);
            if (c == '\\') {
                escaped = true;
//                System.err.println(StringUtils.repeat(" ", i + 1) + "escaped = T");
            } else {
                current_arg.append(c);
                if (escaped) {
                    escaped = false;
//                    System.err.println(">" + StringUtils.repeat(" ", i) + "escaped = F");
                } else {
                    if (c == '\'') {
//                        System.err.print(">" + StringUtils.repeat(" ", i) + "simpleQuoted = " + simpleQuoted);
                        if (!doubleQuoted) {
                            simpleQuoted = !simpleQuoted;
                        }
//                        System.err.println(" -> " + simpleQuoted);
                    } else if (c == '\"') {
//                        System.err.print(">" + StringUtils.repeat(" ", i) + "doubleQuoted = " + doubleQuoted);
                        if (!simpleQuoted) {
                            doubleQuoted = !doubleQuoted;
                        }
//                        System.err.println(" -> " + doubleQuoted);
                    } else if (!simpleQuoted && !doubleQuoted && !escaped) {
                        if (c == '(') {
                            openBraces++;
//                            System.err.println(">" + StringUtils.repeat(" ", i) + "openBraces = " + openBraces);
                        } else if (c == ')') {
                            openBraces--;
//                            System.err.println(">" + StringUtils.repeat(" ", i) + "openBraces = " + openBraces);
                        }
                    }
                }
            }
        }
        args_string_list.add(current_arg.toString());
        String[] args_string_array = args_string_list.toArray(new String[args_string_list.size()]);
        Object[] args = new Object[args_string_array.length];
        for (int i = 0; i < args.length; i++) {
            String arg_string = args_string_array[i].trim();
            if (arg_string.startsWith("\"") && arg_string.endsWith("\"")) {
                args[i] = arg_string.substring(1, arg_string.length() - 1);
                continue;
            }
            if (arg_string.startsWith("'") && arg_string.endsWith("'") && arg_string.length() == 3) {
                args[i] = arg_string.charAt(1);
                continue;
            }
            try {
                int in = Integer.parseInt(arg_string);
                args[i] = in;
                continue;
            } catch (NumberFormatException e) {
            }
            try {
                double d = Double.parseDouble(arg_string);
                args[i] = d;
                continue;
            } catch (NumberFormatException e) {
            }
            args[i] = CallAlgebra(o, arg_string.trim());
        }
        if (method.length() == 0 && args.length == 1) {//not a function call, just a bracket eval eg. (1+1)*3
//            System.err.println("-------------------------> CallAlgebra " + head + args[0].toString() + tail);
            return CallAlgebra(o, head + args[0].toString() + tail);
        } else {
//            System.err.println("-------------------------> CallAlgebra/Eval h=" + head + " Call(o, method=" + method + ",args=<<" + Arrays.asList(args) + ">>) t=" + tail);
            if (head.length() == 0 && tail.length() == 0) {
                return Eval(o, method, args);
            } else {
                return CallAlgebra(o, head + Eval(o, method, args) + tail);
            }
        }
    }

    public static List getSyntax() {
        LinkedList<String> list = new LinkedList<String>();
        list.addAll(StaticMethods);
        return list;
    }

    static String importExpression(String userExpression) {
        String gexpr = userExpression;

        while (gexpr.contains(PIPE)) {
            //System.out.println("    " + gexpr);
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

    static Object exportObject(Object go) {
        if (go instanceof List) {
            List golist = (List) go;
            if (golist.size() == 1) {
                return exportObject(golist.get(0));
            } else {
                return golist;//ASCII.cat("\n", golist);
                //return ASCII.cat("\n", golist);
            }
        }

        return go;//.toString();
    }

    public static Object eval(Object of, Map<String, Object> outputNamesValues) {
        assert of instanceof String : "Problem trying to eval " + of.toString() + " : not a String";
        return eval((String) of, outputNamesValues);
    }

    /**
     * apply the function to get the result
     *
     * @param outputValues values of initial outputs
     */
    public synchronized static Object eval(String f, Map<String, Object> params) {
        Object out = null;
        List<File> rfiles = new LinkedList<>();
        try {
            Log.logMessage("ParseExpression", SeverityLevel.INFO, false, "eval(" + f + "," + (params != null ? params.toString() : "null") + ")");

            if (params != null && params.containsKey(f)) {
                return params.get(f);
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
                        rfiles.add(file);
                    } else {
                        try {
                            Disk.listRecursiveFiles(file, rfiles);
                        } catch (Exception e) {
                            Log.logException(true, e);
                        }
                    }
                }
            }

            Parser o = new Parser(rfiles.toArray(new File[rfiles.size()]));
            if (f.startsWith("`") && f.endsWith("`")) {
                f = f.substring(1, f.length() - 1);
            }
            String ie = importExpression(f);
            out = exportObject(CallAlgebra(o, ie));
            Log.logMessage("ParseExpression", SeverityLevel.INFO, false, "  >> " + out);
        } catch (Exception e) {
            Log.logException(false, new Exception("Failed to evaluate expression " + f + " on files " + rfiles + "\n" + e.getMessage()));
            if (Log.level>=10) e.printStackTrace();
        }
        return out;
    }
}
