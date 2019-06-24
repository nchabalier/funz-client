package org.funz.script;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.funz.conf.Configuration;
import org.funz.log.Log;

/**
 *
 * @author richet
 */
public abstract class MathExpression {

    public String getLastMessage() {
        return "?";
    }

    public static class MathException extends Exception {

        public MathException(String what) {
            super(what);
        }
    }
    private static MathExpression defaultInstance;
    protected String name;
    List<String> globalVariables = new LinkedList<String>();

    static {
        //Automated instanciation of class MathExpression using -DMathExpression.class=org.comp.math.myengine setting
        if (System.getProperty("MathExpression.class") != null) {
            try {
                SetDefaultInstance((Class) Class.forName(System.getProperty("MathExpression.class"), true, MathExpression.class.getClassLoader()));
            } catch (ClassNotFoundException c) {
                System.out.println("Class " + System.getProperty("MathExpression.class") + " not found. No MathExpression class set.");
            }
        }
    }

    public MathExpression(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
    
    public static void SetDefaultInstance(Class MathExpressionClass) {
        SetDefaultInstance(NewInstance(MathExpressionClass, "Default_" + Configuration.timeDigest()));
    }

    public static void SetDefaultInstance(MathExpression instance) {
        defaultInstance = instance;
    }

    /**
     * @return the main instance
     */
    public static MathExpression GetDefaultInstance() {
        return defaultInstance;
    }

    /**
     * @return the instance
     */
    public static MathExpression NewInstance(Class MathExpressionClass, String name) {
        MathExpression instance = null;
        if (MathExpressionClass == null) {
            System.err.println("No MathExpression Class given.");
            return null;
        }
        try {
            instance = (MathExpression) MathExpressionClass.getConstructor(String.class).newInstance(name);
            //System.err.println("instance="+instance);
        } catch (Exception ie) {
            ie.printStackTrace(System.err);
        }
        return instance;
    }

    /**
     * Eval expression with given vars (generally numeric).
     */
    public abstract Object eval(String expression, Map<String, Object> vars) throws MathException;

    public static synchronized Object Eval(String expression, Map<String, Object> vars) throws Exception {
        if (GetDefaultInstance()==null) throw new Exception("No default instance available.");
        return GetDefaultInstance().eval(expression, vars);
    }

    /**
     * Set objects.
     */
    public synchronized boolean set(String... expression) throws MathException {
        boolean done = true;
        for (String e : expression) {
            done = done & set(e);
        }
        return done;
    }

    public abstract boolean set(String expression) throws MathException;

    public static synchronized boolean Set(String... expression) throws MathException {
        return GetDefaultInstance().set(expression);
    }

    public static boolean Set(String expression) throws MathException {
        return GetDefaultInstance().set(expression);
    }

    /**
     * unSet objects.
     */
    public abstract void reset() throws MathException;

    public static synchronized void Reset() throws MathException {
        GetDefaultInstance().reset();
    }

    /**
     * Used to get name of engine impl.
     */
    public abstract String getEngineName();

    public static String GetEngineName() {
        return GetDefaultInstance().getEngineName();
    }

    public abstract List<String> listVariables(boolean includeGlobalEnvironment, boolean includeShadowVariables);

    public final static String ALL_OPERANDS = ",;:.!?{}()[]<>+-*/=\\%&|^~$@#";

    public String getOperands() {
        return ALL_OPERANDS;
    }

    public static String GetOperands() {
        return GetDefaultInstance().getOperands();
    }

    public abstract String getMinusInfinityExpression();

    public static String GetMinusInfinityExpression() {
        return GetDefaultInstance().getMinusInfinityExpression();
    }

    public abstract String getPlusInfinityExpression();

    public static String GetPlusInfinityExpression() {
        return GetDefaultInstance().getPlusInfinityExpression();
    }
    final static String AW = "((\\A)|(\\W))(";
    final static String Az = ")((\\W)|(\\z))";

    public static String replaceVariable(final String expr, final String var, final String val) {
        String regexp = AW + var + Az;
        Matcher m = Pattern.compile(regexp).matcher(expr);
        if (m.find()) {
            return expr.replace(m.group(), m.group().replace(var, val));
        } else {
            return expr;
        }
    }
}
