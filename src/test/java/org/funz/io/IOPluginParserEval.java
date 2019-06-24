/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.funz.io;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import org.funz.script.ParseExpression;
import org.funz.util.ASCII;
import org.funz.util.Parser;

/**
 *
 * @author richet
 */
public class IOPluginParserEval {

    public static void main(String[] args) {
        if (args == null || args.length < 2) {
            System.out.println("Usage: java ... IOPluginParserEval <ParserExpressionChain> <path1> <path2> <path3> ...");
            System.out.println("  <ParserExpressionChain>: A >> B >> C >> ... where A,B,C, is one of the following command (for B, C, ... first arg is skipped)");
            for (Method m : Parser.class.getMethods()) {
                System.out.println("  " + m.getName() + "(" + ASCII.cat(",", m.getParameterTypes()) + ") -> " + m.getReturnType());
            }
        }

        String expression = args[0];

        for (int i = 1; i < args.length; i++) {
            File path = new File(args[i]);
            if (!path.exists()) {
                System.err.println("Path " + args[i] + " not found.");
                continue;
            }

            File[] files;
            if (path.isDirectory()) {
                files = path.listFiles();
            } else {
                files = new File[]{path};
            }

            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put(ParseExpression.FILES, files);
            try {
                Object o = ParseExpression.eval(expression, params);
                System.out.println("Path: " + path + " \n Eval: " + expression + "\n Type:" + o.getClass().getCanonicalName() + "\n" + toStrings(o));
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }
    }

    public static String toStrings(Object array) {
        if (array instanceof List) {
            array = ((List) array).toArray(new Object[((List) array).size()]);
        }

        if (array == null) {
            return "";
        }
        if (!array.getClass().isArray()) {
            return array.toString();
        }

        try {
            double[] cast = (double[]) array;
            if (cast == null || cast.length == 0) {
                return "";
            }
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < cast.length; i++) {
                buf.append(cast[i]);
                if (i < cast.length - 1) {
                    buf.append('\n');
                }
            }
            return buf.toString();

        } catch (ClassCastException c) {
        } catch (NullPointerException c) {
        }

        Object[] cast = (Object[]) array;
        if (cast == null || cast.length == 0) {
            return "";
        }
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < cast.length; i++) {
            if (cast[i] != null) {
                if (cast[i].getClass().isArray()) {
                    buf.append(toStrings(cast[i]));
                } else {
                    buf.append(cast[i]);
                }
            }
            if (i < cast.length - 1) {
                buf.append('\n');
            }
        }
        return buf.toString();
    }
}
