package org.funz.script;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import org.math.array.DoubleArray;

/**
 *
 * @author richet
 */
public class ExpressionHelper {

    final static String FORCE_PAR = "\\(";
    final static String PAR = "(";
    final static String DOT = ".";
    final static String W_REGEXP = ".*\\w";
    final static String DOT_REGEXP = ".*\\w\\.";
    final static String SPACE = " ";

    public static String prefixMethod(String method, String prefix, String unstat) {
        if (!unstat.contains(method)) {
            return unstat;
        }

        StringBuilder sb = new StringBuilder();

        String[] parts = (unstat + SPACE).split(method + FORCE_PAR);

        String previous = null;
        for (String string : parts) {
            //System.out.println("> " + string);

            if (previous != null) {
                if (previous.matches(W_REGEXP) || previous.matches(DOT_REGEXP)) {
                    sb.append(method);
                    sb.append(PAR);
                    sb.append(string);
                } else {
                    sb.append(prefix);
                    sb.append(DOT);
                    sb.append(method);
                    sb.append(PAR);
                    sb.append(string);
                }
            } else {
                sb.append(string);
            }
            previous = string;
        }

        return sb.toString();
    }

    public static void main(String[] args) {
        String str = prefixMethod("atan", "Math", "atan(((0.6666666666666666)+(0.3333333333333333)^(1.0)*atan((0.6666666666666666))+tan(10)))");
        System.out.println("[atan]  > " + str);
        str = prefixMethod("tan", "Math", str);
        System.out.println("[tan]  > " + str);

        String str2 = prefixConstant("E", "Math", "1E5+5*E");
        System.out.println("[E]  > " + str2);

        String str3 = prefixConstant("PI", "Math", "PI+1.5");
        System.out.println("[PI]  > " + str3);

        String str4 = prefixConstant("PI", "Math", "Math.PI+1.5");
        System.out.println("[Math.PI]  > " + str4);
    }

    public static String prefixConstant(String constant, String prefix, String unstat) {
        StringBuilder sb = new StringBuilder();

        String[] parts = (unstat + SPACE).split(constant);

        String previous = null;
        for (String string : parts) {
            //System.out.println("> " + string);

            if (previous != null) {
                if (previous.matches(W_REGEXP) || previous.matches(DOT_REGEXP)) {
                    sb.append(constant);
                    sb.append(string);
                } else {
                    sb.append(prefix);
                    sb.append(DOT);
                    sb.append(constant);
                    sb.append(string);
                }
            } else {
                sb.append(string);
            }
            previous = string;
        }

        return sb.toString();
    }

    /*public static String prefixConstant(String constant, String prefix, String unstat) {
    String stat = unstat.replaceAll(constant, prefix + "." + constant);
    return stat;
    }*/
    /** Method to cast a double[][] to double[] or double.
     * @param array double[][]
     * @return
     */
    public static Object castDoubleArray(double[][] array) {
        if (array.length == 1) {
            if (array[0].length == 1) {
                //System.err.println("Cast to double");
                return array[0][0];
            } else {
                //System.err.println("Cast to double[]");
                return array[0];
            }
        } else if (array[0].length == 1) {
            //System.err.println("Cast to double[]");
            return DoubleArray.getColumnsCopy(array, 0);
        } else {
            //System.err.println("Cast to double[][]");
            return array;
        }
    }

    public static LinkedList<String> parseNonStaticMethods(Class c) {
        LinkedList<String> Methods = new LinkedList<String>();

        Method[] methods = c.getMethods();
        for (Method method : methods) {
            if (!Modifier.isStatic(method.getModifiers())) {
                //System.out.println("Adding non-static method " + method.getName());
                boolean isAlreadyIn = false;
                boolean isAlreadyInclude = false;
                for (String m : Methods) {
                    if (m.equals(method.getName())) {
                        isAlreadyIn = true;
                        break;
                    }
                    if (method.getName().contains(m)) {
                        isAlreadyInclude = true;
                        break;
                    }
                }
                if (!isAlreadyIn) {
                    if (!isAlreadyInclude) {
                        Methods.add(method.getName());
                    } else {
                        Methods.addFirst(method.getName());
                    }
                }
            }
        }
        //System.out.println("Methods:\n" + ASCII.cat("\n  ", Methods));
        return Methods;
    }

    public static LinkedList<String> parseStaticMethods(Class c) {
        LinkedList<String> StaticMethods = new LinkedList<String>();

        //if (c==null) return StaticMethods;
        Method[] methods = c.getMethods();
        for (Method method : methods) {
            if (Modifier.isStatic(method.getModifiers())) {
                //System.out.println("Adding static method " + method.getName());
                boolean isAlreadyIn = false;
                boolean isAlreadyInclude = false;
                for (String m : StaticMethods) {
                    if (m.equals(method.getName())) {
                        isAlreadyIn = true;
                        break;
                    }
                    if (method.getName().contains(m)) {
                        isAlreadyInclude = true;
                        break;
                    }
                }
                if (!isAlreadyIn) {
                    if (!isAlreadyInclude) {
                        StaticMethods.add(method.getName());
                    } else {
                        StaticMethods.addFirst(method.getName());
                    }
                }
            }
        }

        //System.out.println("Static Methods:\n" + ASCII.cat("\n  ", StaticMethods));

        return StaticMethods;
    }

    public static LinkedList<String> parseStaticFields(Class c) {
        LinkedList<String> Fields = new LinkedList<String>();

        Field[] fields = c.getFields();
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())) {
                // System.out.println("Adding static method " + field.getName());
                boolean isAlreadyIn = false;
                boolean isAlreadyInclude = false;
                for (String m : Fields) {
                    if (m.equals(field.getName())) {
                        isAlreadyIn = true;
                        break;
                    }
                    if (field.getName().contains(m)) {
                        isAlreadyInclude = true;
                        break;
                    }
                }
                if (!isAlreadyIn) {
                    if (!isAlreadyInclude) {
                        Fields.add(field.getName());
                    } else {
                        Fields.addFirst(field.getName());
                    }
                }
            }
        }

        //System.out.println("Static Methods:\n" + ASCII.cat("\n  ", Fields));

        return Fields;
    }
}
