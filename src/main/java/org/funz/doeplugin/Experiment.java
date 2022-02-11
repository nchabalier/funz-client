package org.funz.doeplugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.funz.Project;
import org.funz.log.Log;
import org.funz.parameter.OutputFunctionExpression;
import org.funz.util.ASCII;
import org.funz.util.Data;
import static org.funz.util.Data.*;
import static org.funz.util.Format.ArrayMapToMDString;

public abstract class Experiment {

    public Project prj;

    public Experiment(Project prj) {
        this.prj = prj;
    }

    /**
     * Returns the design session to save/get persistent properties
     */
    public abstract DesignSession getDesignSession();

    /**
     * Retruns a string presentation of the parameter at position
     * <b>paramIdx</id>
     *
     * @param paramIdx parameter of interest position, must be <
     * getNmOfParameters()
     */
    public abstract String getValueExpression(int paramIdx);

    /**
     * Returns the number of parameters. All Experiences within the same project
     * must return the same number of parameters.
     */
    public abstract int getNmOfParameters();

    /**
     * Returns the simulation result corresponding to the combination of
     * parameters of this Experience. If null the result is not yet available.
     */
    public abstract Map<String, Object> getOutputValues();

    public abstract Map<String, Object> getInputValues();

    public abstract Map<String, Object> getIntermediateValues();
    Map<String, Object> evaluatedFunctions = new HashMap<String, Object>()/*{

     @Override
     public Object put(String key, Object value) {
     System.err.println(toTabString(false)+" -------> "+key +" : "+value.getClass()+" "+value);
     return super.put(key, value); //To change body of generated methods, choose Tools | Templates.
     }

     @Override
     public void putAll(Map<? extends String, ? extends Object> m) {
     for (String k : m.keySet()) {
     System.err.println(toTabString(false)+" -------> "+k +" : "+m.get(k).getClass()+" "+m.get(k));

     }
     super.putAll(m);
     } 
     }*/;

    public synchronized Object doEval(OutputFunctionExpression f) throws Exception {
        if (evaluatedFunctions == null) {
            return null;
        }
        evaluatedFunctions.putAll(getInputValues());
        Map inter = getIntermediateValues();
        if (inter != null) {
            evaluatedFunctions.putAll(getIntermediateValues());
        }
        evaluatedFunctions.putAll(getOutputValues());

        if (!evaluatedFunctions.containsKey(f.toNiceSymbolicString())) {
            Object result = f.eval(prj.getPlugin().getFormulaInterpreter(), DesignHelper.merge(getOutputValues(), getInputValues(), getIntermediateValues()));
            if (result instanceof String) {
                result = Data.asObject((String) result);
            }

            if (result != null && !isNaN(result)) {
                evaluatedFunctions.put(f.toNiceSymbolicString(), result);
                return result;
            } else {
                return null;
            }
        }
        return evaluatedFunctions.get(f.toNiceSymbolicString());
    }

    static boolean isNaN(Object result) {
        if (result instanceof Double) {
            return Double.isNaN((Double) result);
        } else if (result instanceof Integer) {
            return Double.valueOf((Integer) result).isNaN();
        } else if (result instanceof double[]) {
            for (int i = 0; i < ((double[]) result).length; i++) {
                if (isNaN(((double[]) result)[i])) {
                    return true;
                }
            }
            return false;
        } else if (result instanceof Object[]) {
            for (int i = 0; i < ((Object[]) result).length; i++) {
                if (isNaN(((Object[]) result)[i])) {
                    return true;
                }
            }
            return false;
        } else if (result instanceof String) {
            Object o = Data.asObject((String) result);
            if (o instanceof String) {
                return o.equals("NaN");
            } else {
                return isNaN(o);
            }
        }
        Log.err("[WARNING] " + result + "(class=" + (result == null ? "null" : result.getClass()) + ") is taken as NaN", 2);
        return true;
    }

    /**
     * Convenience method for converting the parameters value from String to
     * double. If the value is not convertible it returns 0.
     *
     * @param paramIdx parameter of interest position, must be <
     * getNmOfParameters()
     */
    public double getDoubleValue(int paramIdx) {
        try {
            return Double.parseDouble(getValueExpression(paramIdx).substring(getValueExpression(paramIdx).indexOf("=") + 1));
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    public String getValue(int paramIdx) {
        return getValueExpression(paramIdx).substring(getValueExpression(paramIdx).indexOf("=") + 1);
    }

    /**
     * Convenience method for converting the parameters value from String to
     * integer. If the value is not convertible it returns 0.
     *
     * @param paramIdx parameter of interest position, must be <
     * getNmOfParameters()
     */
    public int getIntValue(int paramIdx) {
        try {
            return Integer.parseInt(getValueExpression(paramIdx).substring(getValueExpression(paramIdx).indexOf("=") + 1));
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Compares two Experience parameters combination. The parameters order is
     * important. null parameter values are allowed.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Experiment)) {
            return false;
        }

        Experiment other = (Experiment) obj;
        if (other.getNmOfParameters() != getNmOfParameters()) {
            return false;
        }

        for (int i = 0; i < getNmOfParameters(); i++) {
            String his = other.getValueExpression(i);
            String mine = getValueExpression(i);
            if (his == null && mine == null) {
                continue;
            }
            if (his == null || !his.equals(mine)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return toTabString(true);
    }

    public String toTabString(boolean all) {
        StringBuilder out = new StringBuilder(/*"Experiment :"*/);
        //out.append(" " + getNmOfParameters() + "  parameters values: ");
        for (int i = 0; i < getNmOfParameters(); i++) {
            out.append("| ").append(getValue(i)).append('\u0009');
        }
        if (all & getOutputValues() != null) {
            HashMap<String, Object> catMap = new HashMap<String, Object>() {

                @Override
                public Object get(Object o) {
                    return asString(super.get(o));
                }
            };
            catMap.putAll(getOutputValues());
            out.append("|| ").append(ASCII.cat('\u0009' + "| ", catMap));
        }
        return out.toString();
    }

    public static String toString_ExperimentArray(String title, final List<? extends Experiment> exps) {
        Map[] exps_map = new Map[exps.size()];
        for (int i = 0; i < exps_map.length; i++) {
            Experiment e = exps.get(i);
            Map all = e.getInputValues();
            all.putAll(e.getOutputValues());
            exps_map[i] = all;
        }
        return "#" + title + "\n\n" + ArrayMapToMDString(mergeMapArray(exps_map));
    }
}
