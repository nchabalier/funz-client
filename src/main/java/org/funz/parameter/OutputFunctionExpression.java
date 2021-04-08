package org.funz.parameter;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.funz.log.Log;
import org.funz.log.LogCollector.SeverityLevel;
import org.funz.results.RendererHelper;
import org.funz.script.MathExpression;
import org.funz.script.MathExpression.MathException;
import org.funz.util.ASCII;
import org.funz.util.Data;
import static org.funz.util.Data.asString;

public abstract class OutputFunctionExpression {

    public static List<Class> OutputFunctions;
    public static HashMap<Class, OutputFunctionExpression> OutputFunctionInstances;
    public static HashMap<Class, String> OutputFunctionNames;
    public static String OLD_PARAM_SEPARATOR = ";";
    public static String PARAM_SEPARATOR = ",";
    public static String FUNC_SEPARATOR = "->";
    public static String EOL = "<br/>";
    public static String TYPE_DELIMITER = ":";
    public static String ARRAY_BEGIN = "" + Data.ARRAY_BEG;//"[";
    public static String ARRAY_END = "" + Data.ARRAY_END;//"]";
    public static String COORD_BEGIN = "(";
    public static String COORD_END = ")";
    public static String SEQUENCE_BEGIN = "{";
    public static String SEQUENCE_END = "}";
    public static String TEXT_BEGIN = "'";
    public static String TEXT_END = "'";

    public static String buildPoint(double... x) {
        StringBuilder s = new StringBuilder();
        s.append(COORD_BEGIN);
        for (double d : x) {
            s.append(d);
            s.append(PARAM_SEPARATOR);
        }
        s.deleteCharAt(s.length() - 1);
        s.append(COORD_END);
        return s.toString();
    }

    // <editor-fold defaultstate="collapsed" desc="Types">
    public static class GaussianDensity extends OutputFunctionExpression {

        public GaussianDensity() {
            this("", "");
        }

        public GaussianDensity(String muExpr, String sigmaExpr) {
            setParametersExpression(muExpr, sigmaExpr);
            parametersNames = new String[]{"mean", "standard deviation"};
        }

        public GaussianDensity(String[] musigmaExpr) {
            this(musigmaExpr[0], musigmaExpr[1]);
        }

        public String getSymbolicMean() {
            return parametersExpression[0];
        }

        public String getSymbolicStDev() {
            return parametersExpression[1];
        }

        @Override
        public String toNiceString(Object[] params) {
            if (params == null || params.length < 1 || params[0] == null) {
                return null;
            }
            if (params[0].getClass().isArray()) {
                if (params[0] instanceof double[]) {
                    assert ((double[]) params[0]).length == 2 : "Array size is " + ((double[]) params[0]).length + " instead of 2.";
                    return "N" + COORD_BEGIN + ((double[]) params[0])[0] + PARAM_SEPARATOR + ((double[]) params[0])[1] + COORD_END;
                } else {
                    assert ((Object[]) params[0]).length == 2 : "Array size is " + ((Object[]) params[0]).length + " instead of 2.";
                    return "N" + COORD_BEGIN + ((Object[]) params[0])[0] + PARAM_SEPARATOR + ((Object[]) params[0])[1] + COORD_END;
                }
            } else {
                assert params.length == 2 : "Array size is " + params.length + " instead of 2.";
                return "N" + COORD_BEGIN + params[0] + PARAM_SEPARATOR + params[1] + COORD_END;
            }
        }

        public boolean isNiceString(String s) {
            return s.startsWith("N" + COORD_BEGIN) && s.endsWith(COORD_END);
        }

        @Override
        public String toNiceNumericString(Object parametersValues) {
            return "N" + super.toNiceNumericString(parametersValues).replace(ARRAY_BEGIN, COORD_BEGIN).replace(ARRAY_END, COORD_END);
            /*double[] s = (double[]) parametersValues;
             StringBuffer res = new StringBuffer();
             res.append("[");
             for (int i = 0; i < s.length; i++) {
             res.append(s[i]);
             res.append(PARAM_SEPARATOR);
             }
             res.deleteCharAt(res.length() - 1);
             res.append("]");
             return res.toString();*/
        }

        public String[] fromNiceString(String s) {
            assert s.startsWith("N" + COORD_BEGIN) && s.endsWith(COORD_END) : "Unrecognized as N(x,y) Gaussian density expression: " + s;
            String[] res = s.substring(2, s.length() - 1).split(PARAM_SEPARATOR);
            assert res != null && res.length == 2 : "Unrecognized as N(x,y) Gaussian density number of arguments: " + s;
            return res;
        }

        @Override
        public String getResultRendererData(MathExpression engine, Map<String, Object>... values) {
            String val = null;
            try {
                Object parametersvalues = eval(engine, values);
                val = toNiceNumericString(parametersvalues);
            } catch (Exception ex) {
                Log.logException(false, ex);
                val = "?";
            }
            return RendererHelper.formatXML("Plot1D", toNiceSymbolicString(), val);
        }
    }

    public static class Numeric extends OutputFunctionExpression {

        public Numeric() {
            this("");
        }

        public Numeric(String value) {
            setParametersExpression(value);
            parametersNames = new String[]{"value"};

        }

        public String getSymbolicValue() {
            return parametersExpression[0];
        }

        @Override
        public String toNiceString(Object[] params) {
            assert params.length == 1;
            return params[0].toString();
        }

        public boolean isNiceString(String s) {
            double d = Double.NaN;
            try {
                d = Double.parseDouble(s);
            } catch (Exception e) {
                return false;
            }
            return !Double.isNaN(d);
        }

        public String[] fromNiceString(String s) {
            assert !s.contains(PARAM_SEPARATOR) : "Unrecognized numeric expression: " + s;
            assert !s.contains(ARRAY_BEGIN) : "Unrecognized numeric expression: " + s;
            assert !s.contains(ARRAY_END) : "Unrecognized numeric expression: " + s;
            assert !s.contains(COORD_BEGIN) : "Unrecognized numeric expression: " + s;
            assert !s.contains(COORD_END) : "Unrecognized numeric expression: " + s;
            return new String[]{s};
        }

        @Override
        public String getResultRendererData(MathExpression engine, Map<String, Object>... values) {
            String val = null;
            try {
                Object parametersvalues = eval(engine, values);
                val = toNiceNumericString(parametersvalues);
            } catch (Exception ex) {
                Log.logException(false, ex);
                val = "?";
            }
            return RendererHelper.formatXML("Plot1D", toNiceSymbolicString(), val);
        }
    }

    public static class NumericArray extends OutputFunctionExpression {

        public NumericArray() {
            this("");
        }

        public NumericArray(String value) {
            setParametersExpression(value);
            parametersNames = new String[]{"array"};

        }

        public String getSymbolicValue() {
            return parametersExpression[0];
        }

        @Override
        public String toNiceString(Object[] params) {
            assert params.length == 1;
            return ARRAY_BEGIN + params[0] + ARRAY_END;
        }

        public boolean isNiceString(String s) {
            return s.startsWith(ARRAY_BEGIN) && s.endsWith(ARRAY_END) && !s.startsWith(ARRAY_BEGIN + COORD_BEGIN);
        }

        public String[] fromNiceString(String s) {
            assert s.startsWith(ARRAY_BEGIN) && s.endsWith(ARRAY_END) : "Unrecognized numeric array expression: " + s;
            String[] res = s.substring(1, s.length() - 1).split(PARAM_SEPARATOR);
            assert res != null && res.length >= 1 : "Unrecognized numeric array number of arguments: " + s;

            return res;
        }

        @Override
        public String getResultRendererData(MathExpression engine, Map<String, Object>... values) {
            String val = null;
            try {
                Object parametersvalues = eval(engine, values);
                val = toNiceNumericString(parametersvalues);
            } catch (Exception ex) {
                Log.logException(false, ex);
                val = "?";
            }
            return RendererHelper.formatXML("Plot1D", toNiceSymbolicString(), val);
        }
    }

    public static class Anything extends OutputFunctionExpression {

        public Anything() {
            this("");
        }

        public Anything(String value) {
            setParametersExpression(value);
            parametersNames = new String[]{"object"};
        }

        public Anything(String... values) {
            setParametersExpression(values);
            String[] names = new String[values.length];
            for (int i=0; i < names.length; i++) {
                names[i] = "object"+(i+1);    
            }  
            parametersNames = names;
        }

        public String getSymbolicValue() {
            return parametersExpression[0];
        }

        @Override
        public String toNiceString(Object... params) {
            assert params.length == 1;
            return params[0].toString();
        }

        public boolean isNiceString(String s) {
            return true;
        }

        public String[] fromNiceString(String s) {
            //assert !s.contains(PARAM_SEPARATOR) : "Unrecognized object expression: " + s;
            //assert !s.contains(ARRAY_BEGIN) : "Unrecognized object expression: " + s;
            //assert !s.contains(ARRAY_END) : "Unrecognized object expression: " + s;
            //assert !s.contains(COORD_BEGIN) : "Unrecognized object expression: " + s;
            //assert !s.contains(COORD_END) : "Unrecognized object expression: " + s;
            return new String[]{s};
        }

        @Override
        public String getResultRendererData(MathExpression engine, Map<String, Object>... values) {
            String val = null;
            try {
                Object parametersvalues = eval(engine, values);
                val = toNiceNumericString(parametersvalues);
            } catch (Exception ex) {
                Log.logException(false, ex);
                val = "?";
            }
            return RendererHelper.formatXML("ParPlot", toNiceSymbolicString(), val);
        }
    }

    public static class AnythingND extends OutputFunctionExpression {

        public AnythingND() {
            this(new String[]{""});
        }

        public AnythingND(String... value) {
            setParametersExpression(value);
            String[] names = new String[value.length];
            for (int i = 0; i < names.length; i++) {
                names[i] = "object" + (i + 1);
            }
            parametersNames = names;
        }

        public String getSymbolicValue() {
            return super.toNiceSymbolicString().replace(ARRAY_BEGIN, COORD_BEGIN).replace(ARRAY_END, COORD_END);
        }

        @Override
        public String toNiceString(Object... params) {
            return asString(params).replace(ARRAY_BEGIN, COORD_BEGIN).replace(ARRAY_END, COORD_END);
        }

        public boolean isNiceString(String s) {
            return s.startsWith(COORD_BEGIN) && s.endsWith(COORD_END);
        }

        public String[] fromNiceString(String s) {
            assert s.startsWith(COORD_BEGIN) && s.endsWith(COORD_END) : "Unrecognized coordinates expression: " + s;
            return splitArgs(s);
        }

        @Override
        public String getResultRendererData(MathExpression engine, Map<String, Object>... values) {
            String val = null;
            try {
                Object parametersvalues = eval(engine, values);
                val = toNiceString(parametersvalues);
            } catch (Exception ex) {
                Log.logException(false, ex);
                val = "?";
            }
            return RendererHelper.formatXML("ParPlot", toNiceSymbolicString(), val);
        }
    }

    public static class Sequence extends OutputFunctionExpression {

        public Sequence() {
            this("");
        }

        public Sequence(String value) {
            setParametersExpression(value);
            parametersNames = new String[]{"sequence"};

        }

        public String getSymbolicValue() {
            return parametersExpression[0];
        }

        @Override
        public String toNiceString(Object[] params) {
            assert params.length == 1;
            return SEQUENCE_BEGIN + params[0] + SEQUENCE_END;
        }

        public boolean isNiceString(String s) {
            return s.startsWith(SEQUENCE_BEGIN) && s.endsWith(SEQUENCE_END);
        }

        public String[] fromNiceString(String s) {
            assert s.startsWith(SEQUENCE_BEGIN) && s.endsWith(SEQUENCE_END) : "Unrecognized sequence expression: " + s;
            String[] res = s.substring(1, s.length() - 1).split(PARAM_SEPARATOR);
            assert res != null && res.length >= 1 : "Unrecognized numeric array number of arguments: " + s;
            return res;
        }

        @Override
        public String toNiceNumericString(Object parametersValues) {
            return super.toNiceNumericString(parametersValues).replace(ARRAY_BEGIN, SEQUENCE_BEGIN).replace(ARRAY_END, SEQUENCE_END);
            /*double[] s = (double[]) parametersValues;
             StringBuffer res = new StringBuffer();
             res.append("[");
             for (int i = 0; i < s.length; i++) {
             res.append(s[i]);
             res.append(PARAM_SEPARATOR);
             }
             res.deleteCharAt(res.length() - 1);
             res.append("]");
             return res.toString();*/
        }

        @Override
        public String getResultRendererData(MathExpression engine, Map<String, Object>... values) {
            String val = null;
            try {
                Object parametersvalues = eval(engine, values);
                val = toNiceNumericString(parametersvalues);
            } catch (Exception ex) {
                Log.logException(false, ex);
                val = "?";
            }
            return RendererHelper.formatXML("Plot2D", toNiceSymbolicString(), val);
        }
    }

    public static class Numeric2D extends OutputFunctionExpression {

        public Numeric2D() {
            this("", "");
        }

        public Numeric2D(String x, String y) {
            setParametersExpression(x, y);
            parametersNames = new String[]{"x", "y"};

        }

        public Numeric2D(String[] xy) {
            this(xy[0], xy[1]);
        }

        public String getSymbolicX() {
            return parametersExpression[0];
        }

        public String getSymbolicY() {
            return parametersExpression[1];
        }

        @Override
        public String toNiceNumericString(Object parametersValues) {
            return super.toNiceNumericString(parametersValues).replace(ARRAY_BEGIN, COORD_BEGIN).replace(ARRAY_END, COORD_END);
        }

        @Override
        public String toNiceString(Object[] params) {
            if (params[0].getClass().isArray()) {
                if (params[0] instanceof double[]) {
                    assert ((double[]) params[0]).length == 2 : "Array size is " + ((double[]) params[0]).length + " instead of 2.";
                    return COORD_BEGIN + ((double[]) params[0])[0] + PARAM_SEPARATOR + ((double[]) params[0])[1] + COORD_END;
                } else {
                    assert ((Object[]) params[0]).length == 2 : "Array size is " + ((Object[]) params[0]).length + " instead of 2.";
                    return COORD_BEGIN + ((Object[]) params[0])[0] + PARAM_SEPARATOR + ((Object[]) params[0])[1] + COORD_END;
                }
            } else {
                assert params.length == 2 : "Array size is " + params.length + " instead of 2.";
                return COORD_BEGIN + params[0] + PARAM_SEPARATOR + params[1] + COORD_END;
            }
        }

        public boolean isNiceString(String s) {
            return s.startsWith(COORD_BEGIN) && s.endsWith(COORD_END) && s.split(PARAM_SEPARATOR).length == 2;
        }

        public String[] fromNiceString(String s) {
            assert s.startsWith(COORD_BEGIN) && s.endsWith(COORD_END) : "Unrecognized 2D coordinates expression: " + s;
            String[] res = s.substring(1, s.length() - 1).split(PARAM_SEPARATOR);
            assert res != null && res.length == 2 : "Unrecognized 2D coordinates number of arguments: " + s;
            return res;
        }

        @Override
        public String getResultRendererData(MathExpression engine, Map<String, Object>... values) {
            String val = null;
            try {
                Object parametersvalues = eval(engine, values);
                val = toNiceNumericString(parametersvalues);
            } catch (Exception ex) {
                Log.logException(false, ex);
                val = "?";
            }
            return RendererHelper.formatXML("Plot2D", toNiceSymbolicString(), val);
        }
    }

    public static class Numeric2DArray extends OutputFunctionExpression {

        public Numeric2DArray() {
            this("", "");
        }

        public Numeric2DArray(String x, String y) {
            setParametersExpression(x, y);
            parametersNames = new String[]{"X", "Y"};
        }

        public Numeric2DArray(String[] xy) {
            this(xy[0], xy[1]);
        }

        public String getSymbolicValue() {
            return parametersExpression[0];
        }

        @Override
        public String toNiceString(Object[] params) {
            assert params.length == 2;
            return ARRAY_BEGIN + params[0] + PARAM_SEPARATOR + params[1] + ARRAY_END;
        }

        @Override
        public String toNiceNumericString(Object parametersValues) {
            Object[] parametersvalues = (Object[]) parametersValues;
            assert parametersvalues.length == 2 : "Need 2 arrays instead of " + parametersvalues.length + " : " + asString(parametersValues);
            double[] x = (double[]) parametersvalues[0];
            double[] y = (double[]) parametersvalues[1];
            StringBuffer res = new StringBuffer();
            res.append(ARRAY_BEGIN);
            for (int i = 0; i < x.length; i++) {
                res.append(COORD_BEGIN);
                res.append(x[i]);
                res.append(PARAM_SEPARATOR);
                res.append(y[i]);
                res.append(COORD_END);
                res.append(PARAM_SEPARATOR + EOL);
            }
            res.delete(res.length() - 6, res.length());
            res.append(ARRAY_END);
            return res.toString();
        }

        public boolean isNiceString(String s) {
            return s.startsWith(ARRAY_BEGIN + COORD_BEGIN) && s.endsWith(COORD_END + ARRAY_END) && s.split("\\" + COORD_END)[0].split(PARAM_SEPARATOR).length == 2;
        }

        public String[] fromNiceString(String s) {
            assert s.startsWith(ARRAY_BEGIN + COORD_BEGIN) && s.endsWith(COORD_END + ARRAY_END) : "Unrecognized 2D coordinates array expression: " + s;
            String[] res = s.substring(1, s.length() - 1).replace("(", "").replace(")", "").replace(EOL, "").split(PARAM_SEPARATOR);
            assert res != null && res.length >= 2 && res.length % 2 == 0 : "Unrecognized 2D coordinates array number of arguments: " + s;
            return res;
            /*double[][] dres = new double[res.length / 2][2];
             for (int i = 0; i < dres.length; i++) {
             dres[i][0] = Double.parseDouble(res[2 * i]);
             dres[i][1] = Double.parseDouble(res[2 * i + 1]);
             }
             return DoubleArray.transpose(dres);*/
        }

        @Override
        public String getResultRendererData(MathExpression engine, Map<String, Object>... values) {
            String val = null;
            try {
                Object parametersvalues = eval(engine, values);
                val = toNiceNumericString(parametersvalues);
            } catch (Exception ex) {
                Log.logException(false, ex);
                val = "?";
            }
            return RendererHelper.formatXML("Plot2D", toNiceSymbolicString(), val);
        }
        /*@Override
         public String getResultRendererData(HashMap<String, Object> outputValues) {
         Object[] parametersvalues = (Object[]) eval(outputValues);
         System.out.println( parametersvalues[0]);
         double[] x = (double[]) parametersvalues[0];
         System.out.println( parametersvalues[1]);
        
         double[] y = (double[]) parametersvalues[1];
         StringBuffer res = new StringBuffer();
         res.append("{");
         for (int i = 0; i < x.length; i++) {
         res.append("(");
         res.append(x[i]);
         res.append(PARAM_SEPARATOR);
         res.append(y[i]);
         res.append(")");
         res.append(PARAM_SEPARATOR);
         }
         res.deleteCharAt(res.length() - 1);
         res.append("}");
         return "<Plot2D name='" + toNiceSymbolicString() + "'>" + res + "</Plot2D>";
         }*/
    }

    public static class Numeric3D extends OutputFunctionExpression {

        public Numeric3D() {
            this("", "", "");
        }

        public Numeric3D(String x, String y, String z) {
            setParametersExpression(x, y, z);
            parametersNames = new String[]{"x", "y", "z"};

        }

        public Numeric3D(String[] xyz) {
            this(xyz[0], xyz[1], xyz[2]);
        }

        public String getSymbolicX() {
            return parametersExpression[0];
        }

        public String getSymbolicY() {
            return parametersExpression[1];
        }

        public String getSymbolicZ() {
            return parametersExpression[2];
        }

        @Override
        public String toNiceString(Object[] params) {
            if (params[0].getClass().isArray()) {
                if (params[0] instanceof double[]) {
                    assert ((double[]) params[0]).length == 3 : "Array size is " + ((double[]) params[0]).length + " instead of 3.";
                    return COORD_BEGIN + ((double[]) params[0])[0] + PARAM_SEPARATOR + ((double[]) params[0])[1] + PARAM_SEPARATOR + ((double[]) params[0])[2] + COORD_END;
                } else {
                    assert ((Object[]) params[0]).length == 3 : "Array size is " + ((Object[]) params[0]).length + " instead of 3.";
                    return COORD_BEGIN + ((Object[]) params[0])[0] + PARAM_SEPARATOR + ((Object[]) params[0])[1] + PARAM_SEPARATOR + ((Object[]) params[0])[2] + COORD_END;
                }
            } else {
                assert params.length == 3 : "Array size is " + params.length + " instead of 3.";
                return COORD_BEGIN + params[0] + PARAM_SEPARATOR + params[1] + PARAM_SEPARATOR + params[2] + COORD_END;
            }
        }

        public boolean isNiceString(String s) {
            return s.startsWith(COORD_BEGIN) && s.endsWith(COORD_END) && s.split(PARAM_SEPARATOR).length == 3;
        }

        @Override
        public String toNiceNumericString(Object parametersValues) {
            return super.toNiceNumericString(parametersValues).replace(ARRAY_BEGIN, COORD_BEGIN).replace(ARRAY_END, COORD_END);
        }

        public String[] fromNiceString(String s) {
            assert s.startsWith(COORD_BEGIN) && s.endsWith(COORD_END) : "Unrecognized 3D coordinates expression: " + s;
            String[] res = s.substring(1, s.length() - 1).replace(COORD_BEGIN, "").replace(COORD_END, "").replace(EOL, "").split(PARAM_SEPARATOR);
            assert res != null && res.length == 3 : "Unrecognized 3D coordinates number of arguments: " + s;
            return res;
        }

        @Override
        public String getResultRendererData(MathExpression engine, Map<String, Object>... values) {
            String val = null;
            try {
                Object parametersvalues = eval(engine, values);
                val = toNiceNumericString(parametersvalues);
            } catch (Exception ex) {
                Log.logException(false, ex);
                val = "?";
            }
            return RendererHelper.formatXML("Plot3D", toNiceSymbolicString(), val);
        }
    }

    public static class Numeric3DArray extends OutputFunctionExpression {

        public Numeric3DArray() {
            this("", "", "");
        }

        public Numeric3DArray(String x, String y, String z) {
            setParametersExpression(x, y, z);
            parametersNames = new String[]{"X", "Y", "Z"};
        }

        public Numeric3DArray(String[] xyz) {
            this(xyz[0], xyz[1], xyz[2]);
        }

        public String getSymbolicValue() {
            return parametersExpression[0];
        }

        @Override
        public String toNiceString(Object[] params) {
            assert params.length == 3;
            return ARRAY_BEGIN + params[0] + PARAM_SEPARATOR + params[1] + PARAM_SEPARATOR + params[2] + ARRAY_END;
        }

        @Override
        public String toNiceNumericString(Object parametersValues) {
            Object[] parametersvalues = (Object[]) parametersValues;
            assert parametersvalues.length == 3 : "Need 3 arrays instead of " + parametersvalues.length + " : " + asString(parametersValues);
            double[] x = (double[]) parametersvalues[0];
            double[] y = (double[]) parametersvalues[1];
            double[] z = (double[]) parametersvalues[2];

            StringBuffer res = new StringBuffer();
            res.append(ARRAY_BEGIN);
            for (int i = 0; i < x.length; i++) {
                res.append(COORD_BEGIN);
                res.append(x[i]);
                res.append(PARAM_SEPARATOR);
                res.append(y[i]);
                res.append(PARAM_SEPARATOR);
                res.append(z[i]);
                res.append(COORD_END);
                res.append(PARAM_SEPARATOR + EOL);
            }
            res.delete(res.length() - 6, res.length());
            res.append(ARRAY_END);
            return res.toString();
        }

        public boolean isNiceString(String s) {
            return s.startsWith(ARRAY_BEGIN + COORD_BEGIN) && s.endsWith(COORD_END + ARRAY_END) && s.split("\\" + COORD_END)[0].split(PARAM_SEPARATOR).length == 3;
        }

        public String[] fromNiceString(String s) {
            assert s.startsWith(ARRAY_BEGIN + COORD_BEGIN) && s.endsWith(COORD_END + ARRAY_END) : "Unrecognized 3D coordinates array expression: " + s;
            String[] res = s.replace(ARRAY_BEGIN, "").replace(ARRAY_END, "").replace(COORD_BEGIN, "").replace(COORD_END, "").replace(EOL, "").split(PARAM_SEPARATOR);
            assert res != null && res.length >= 3 && res.length % 3 == 0 : "Unrecognized 3D coordinates array number of arguments: " + s;
            return res;
            /*double[][] dres = new double[res.length / 3][3];
             for (int i = 0; i < dres.length; i++) {
             dres[i][0] = Double.parseDouble(res[3 * i]);
             dres[i][1] = Double.parseDouble(res[3 * i + 1]);
             dres[i][2] = Double.parseDouble(res[3 * i + 2]);
             }
             return DoubleArray.transpose(dres);*/
        }

        @Override
        public String getResultRendererData(MathExpression engine, Map<String, Object>... values) {
            String val = null;
            try {
                Object parametersvalues = eval(engine, values);
                val = toNiceNumericString(parametersvalues);
            } catch (Exception ex) {
                Log.logException(false, ex);
                val = "?";
            }
            return RendererHelper.formatXML("Plot3D", toNiceSymbolicString(), val);
        }
    }

    /*public static class NumericMatrix extends OutputFunctionExpression {
    
     public NumericMatrix() {
     this("", "", "");
     }
    
     public NumericMatrix(String x, String y, String z) {
     setParametersExpression(x, y, z);
     parametersNames = new String[]{"X", "Y", "Z(x,y)"};
     }
    
     public String getSymbolicValue() {
     return parametersExpression[0];
     }
    
     @Override
     public String toNiceString(Object[] params) {
     assert params.length == 3;
     return "{(" + params[0] + PARAM_SEPARATOR + params[1] + ")" + FUNC_SEPARATOR + params[2] + "}";
     }
    
     @Override
     public String toNiceNumericString(Object parametersValues) {
     Object[] parametersvalues = (Object[]) parametersValues;
     double[] x = (double[]) parametersvalues[0];
     double[] y = (double[]) parametersvalues[1];
     double[][] z = (double[][]) parametersvalues[2];
    
     StringBuffer res = new StringBuffer();
     res.append("({");
     for (int i = 0; i < x.length; i++) {
     res.append(x[i]);
     res.append(PARAM_SEPARATOR);
     }
     res.delete(res.length() - 1, res.length());
     res.append("}" + EOL);
     res.append("{");
     for (int i = 0; i < y.length; i++) {
     res.append(y[i]);
     res.append(PARAM_SEPARATOR);
     }
     res.delete(res.length() - 1, res.length());
     res.append("})" + FUNC_SEPARATOR + EOL);
    
     res.append("[");
     for (int i = 0; i < z.length; i++) {
     res.append("[");
     for (int j = 0; j < z[i].length; j++) {
     res.append(z[i][j]);
     res.append(PARAM_SEPARATOR);
     }
     res.append("]" + EOL);
     }
     res.delete(res.length() - 5, res.length());
     res.append("]");
    
    
     return res.toString();
     }
    
     @Override
     public String getResultRendererData(HashMap<String, Object> outputValues, HashMap<String, Object> inputValues) {
     Object parametersvalues = eval(outputValues, inputValues);
     return "<Plot3D name='" + toNiceSymbolicString() + "'>" + toNiceNumericString(parametersvalues) + "</Plot3D>";
     }
     }*/

 /*public static class RandomSample extends OutputFunctionExpression {
    
     public RandomSample() {
     this("");
     }
    
     public RandomSample(String expr) {
     setParametersExpression(expr);
     parametersNames = new String[]{"sample"};
    
     }
    
     public String getSymbolicValue() {
     return parametersExpression[0];
     }
    
     @Override
     public String toNiceString(Object[] params) {
     assert params.length == 1;
     return "{" + params[0] + "}";
     }
    
     @Override
     public String getResultRendererData(HashMap<String, Object> outputValues) {
     Object parametersvalues = eval(outputValues);
     return "<Plot1D name='" + toNiceSymbolicString() + "'>" + toNiceNumericString(parametersvalues) + "</Plot1D>";
     }
     }*/
    public static class Text extends OutputFunctionExpression {

        public Text() {
            this("");
        }

        public Text(String value) {
            setParametersExpression(value);
            parametersNames = new String[]{"text"};
        }

        public String getSymbolicValue() {
            return parametersExpression[0];
        }

        @Override
        public String toNiceString(Object[] params) {
            assert params.length == 1;
            return TEXT_BEGIN + params[0] + TEXT_END;
        }

        public String[] fromNiceString(String s) {
            assert s.startsWith(TEXT_BEGIN) : "Unrecognized text expression: " + s;
            assert s.endsWith(TEXT_END) : "Unrecognized text expression: " + s;
            return new String[]{s.substring(1, s.length() - 1)};
        }

        public boolean isNiceString(String s) {
            return s.startsWith(TEXT_BEGIN) && s.endsWith(TEXT_END);
        }

        @Override
        public String getResultRendererData(MathExpression engine, Map<String, Object>... values) {
            String val = null;
            try {
                Object parametersvalues = eval(engine, values);
                val = toNiceNumericString(parametersvalues);
            } catch (Exception ex) {
                Log.logException(false, ex);
                val = "?";
            }
            return RendererHelper.formatXML("HTML", toNiceSymbolicString(), val);
        }
    }

    /*public static class StringArray extends OutputFunctionExpression {
    
     public StringArray() {
     this("");
     }
    
     public StringArray(String value) {
     setParametersExpression(value);
     parametersNames = new String[]{"lines"};
     }
    
     public String getSymbolicValue() {
     return parametersExpression[0];
     }
    
     @Override
     public String toNiceString(Object[] params) {
     assert params.length == 1;
     return "('" + params[0] + "')";
     }
    
     @Override
     public String getResultRendererData(HashMap<String, Object> outputValues) {
     Object parametersvalues = eval(outputValues);
     return "<Plot2D name='" + toNiceSymbolicString() + "'>" + toNiceNumericString(parametersvalues) + "</Plot1D>";
     }
     }*/
    // </editor-fold>
    public static Object[] dup(Object a, int n) {
        Object[] d = new Object[n];
        for (int i = 0; i < d.length; i++) {
            d[i] = a;

        }
        return d;
    }

    public static Class[] dup(Class a, int n) {
        Class[] d = new Class[n];
        for (int i = 0; i < d.length; i++) {
            d[i] = a;

        }
        return d;
    }

    public static String[] dup(String a, int n) {
        String[] d = new String[n];
        for (int i = 0; i < d.length; i++) {
            d[i] = a;

        }
        return d;
    }

    static {
        initOutputFunctionTypes();
    }

    public synchronized static void initOutputFunctionTypes() {
        if (OutputFunctions == null) {
            OutputFunctions = new LinkedList<Class>();
            OutputFunctionInstances = new HashMap<>();
            OutputFunctionNames = new HashMap<>();

            // !!! be carefull that order is important for recognition of type: more specified to less one (Anything)

            OutputFunctions.add(GaussianDensity.class);
            OutputFunctionInstances.put(GaussianDensity.class, new GaussianDensity());
            OutputFunctionNames.put(GaussianDensity.class, "Gaussian density");
                        
            OutputFunctions.add(Numeric3DArray.class);
            OutputFunctionInstances.put(Numeric3DArray.class, new Numeric3DArray());
            OutputFunctionNames.put(Numeric3DArray.class, "Numeric3DArray");

            OutputFunctions.add(Numeric2DArray.class);
            OutputFunctionInstances.put(Numeric2DArray.class, new Numeric2DArray());
            OutputFunctionNames.put(Numeric2DArray.class, "Numeric2DArray");

            OutputFunctions.add(NumericArray.class);
            OutputFunctionInstances.put(NumericArray.class, new NumericArray());
            OutputFunctionNames.put(NumericArray.class, "NumericArray");

            OutputFunctions.add(Numeric3D.class);
            OutputFunctionInstances.put(Numeric3D.class, new Numeric3D());
            OutputFunctionNames.put(Numeric3D.class, "Numeric3D");

            OutputFunctions.add(Numeric2D.class);
            OutputFunctionInstances.put(Numeric2D.class, new Numeric2D());
            OutputFunctionNames.put(Numeric2D.class, "Numeric2D");

            OutputFunctions.add(Numeric.class);
            OutputFunctionInstances.put(Numeric.class, new Numeric());
            OutputFunctionNames.put(Numeric.class, "Numeric");

            OutputFunctions.add(Sequence.class);
            OutputFunctionInstances.put(Sequence.class, new Sequence());
            OutputFunctionNames.put(Sequence.class, "Sequence");

            OutputFunctions.add(Text.class);
            OutputFunctionInstances.put(Text.class, new Text());
            OutputFunctionNames.put(Text.class, "Text");

            OutputFunctions.add(AnythingND.class);
            OutputFunctionInstances.put(AnythingND.class, new AnythingND());
            OutputFunctionNames.put(AnythingND.class, "AnythingND");

            OutputFunctions.add(Anything.class);
            OutputFunctionInstances.put(Anything.class, new Anything());
            OutputFunctionNames.put(Anything.class, "Anything");
        }
    }

    public static OutputFunctionExpression newInstanceOfType(String type, String... args) {
        initOutputFunctionTypes();
        try {
            if (OutputFunctionNames.values().contains(type)) {
                Class klass = null;
                for (Class c : OutputFunctionNames.keySet()) {
                    if (OutputFunctionNames.get(c).equals(type)) {
                        klass = c;
                        break;
                    }
                }
                if (args == null) {
                    return (OutputFunctionExpression) (klass.newInstance());
                } else {
                    return (OutputFunctionExpression) klass.getConstructor(args.length == 1 ? String.class : new String[args.length].getClass()).newInstance(args.length == 1 ? args[0] : (Object) args);
                }
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return null;
        }
    }

    /*public static OutputFunctionExpression getDefaultOutputFunction() {
     return new Numeric("?");
     }*/
    public static OutputFunctionExpression read(String str) {
        initOutputFunctionTypes();
        for (Class type : OutputFunctions) {
            OutputFunctionExpression instance = OutputFunctionInstances.get(type);
            if (str.startsWith(type.getSimpleName() + TYPE_DELIMITER)) {
                //System.err.println("seems to be "+type.getName());
                try {
                    Object[] args = splitArgs(str.substring(type.getSimpleName().length() + TYPE_DELIMITER.length()));//.substring(type.getSimpleName().length() + 1).replace(OLD_PARAM_SEPARATOR, PARAM_SEPARATOR).split(PARAM_SEPARATOR);
                    Class[] argsclasses = new Class[args.length];
                    for (int i = 0; i < argsclasses.length; i++) {
                        argsclasses[i] = String.class;
                    }
                    OutputFunctionExpression expr = null;
                    if (type == AnythingND.class) {
                        String[] dummy = new String[]{""};
                        expr = (OutputFunctionExpression) type.getConstructor(dummy.getClass()).newInstance((Object) args);
                    } else {
                        expr = (OutputFunctionExpression) type.getConstructor(argsclasses).newInstance(args);
                    }
                    return expr;
                } catch (Exception e) {
                    Log.logException(false, e);
                    throw new IllegalArgumentException("Parameters of " + str + " not suitable for " + type.getSimpleName() + " constructor");
                }
            } else if (instance.isNiceString(str)) {
                String[] args = instance.fromNiceString(str);
                Class[] argsclasses = new Class[args.length];
                for (int i = 0; i < argsclasses.length; i++) {
                    argsclasses[i] = String.class;
                }
                try {
                    return (OutputFunctionExpression) type.getConstructor(argsclasses).newInstance(args);
                } catch (Exception e) {
                    Log.logException(false, e);
                    throw new IllegalArgumentException("Parameters of " + str + " not suitable for " + type.getSimpleName() + " constructor");
                }
            }
        }
        throw new IllegalArgumentException("Expression " + str + " is not suitable as an OutputFunctionExpression");
    }

    static int matchingCloseBrace(String expr, int openBracePos) {
        char openBrace = expr.charAt(openBracePos);
        char closeBrace = 0;
        if (openBrace == '(') {
            closeBrace = ')';
        } else if (openBrace == '[') {
            closeBrace = ']';
        } else if (openBrace == '{') {
            closeBrace = '}';
        } else {
            return -1;
            //throw new UnsupportedOperationException("Could not define closing brace for " + openBrace);
        }

        int braces = 1;
        for (int i = openBracePos + 1; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == openBrace) {
                braces++;
            } else if (c == closeBrace) {
                braces--;
            }
            if (braces == 0) {
                return i;
            }
        }

        return -1;
    }

    static int matchingOpenBrace(String expr, int closeBracePos) {
        char closeBrace = expr.charAt(closeBracePos);
        char openBrace = 0;
        if (closeBrace == ')') {
            openBrace = '(';
        } else if (closeBrace == ']') {
            openBrace = '[';
        } else if (closeBrace == '}') {
            openBrace = '{';
        } else {
            return -1;
            //throw new UnsupportedOperationException("Could not define opening brace for " + closeBrace);
        }

        int braces = 1;
        for (int i = closeBracePos - 1; i >= 0; i--) {
            char c = expr.charAt(i);
            if (c == openBrace) {
                braces++;
            } else if (c == closeBrace) {
                braces--;
            }
            if (braces == 0) {
                return i;
            }
        }

        return -1;
    }

    public static String[] splitArgs(String arglist) {
        if (arglist == null) {
            return null;
        }
        if (arglist.length() == 0) {
            return new String[]{""};
        }
        while (matchingCloseBrace(arglist, 0) == arglist.length() - 1) {
            arglist = arglist.substring(1, arglist.length() - 1);
            if (arglist.length() == 0) {
                return new String[]{""};
            }
        }
        LinkedList<String> args = new LinkedList<String>();
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < arglist.length(); i++) {
            char c = arglist.charAt(i);
            if (c == '(' || c == '[' || c == '{') {
                int i2 = matchingCloseBrace(arglist, i);
                if (i2==-1) {
                    Log.logMessage("splitArgs", SeverityLevel.ERROR, true, "Could not find closing brace in " + arglist+" after "+i);
                    break;
                }
                b = b.append(arglist.substring(i, i2 + 1));
                i = i2;
                continue;
            }
            if (c == PARAM_SEPARATOR.charAt(0)) {
                args.add(b.toString());
                b = new StringBuilder();
            } else {
                b = b.append(c);
            }
        }
        args.add(b.toString());
        return args.toArray(new String[args.size()]);
    }
    //TODO : HashMap<String, String> parameters ??
    public String[] parametersExpression;
    public String[] parametersNames;
    boolean retryNextTime = true; // due to instability of groovy parser... To remove when groovy more stable
    //or maybe control concurrency with synchronized in Groove*Expression classes ?

    public Object eval(MathExpression engine, Map<String, Object>... vars) throws Exception {
        Object[] parametersValues = new Object[parametersExpression.length];
        try {
            retryNextTime = true;
            HashMap<String, Object> values = new HashMap<String, Object>();
            for (Map<String, Object> v : vars) {
                if (v != null) {
                    values.putAll(v);
                }
            }

            for (int i = 0; i < parametersExpression.length; i++) {
                try {
                    parametersValues[i] = engine.eval(parametersExpression[i], values);
                } catch (MathException ex) {
                    Log.logException(true, ex);
                    parametersValues[i] = null;
                }
            }
            //Configuration.logMessage(this, SeverityLevel.INFO, false, "Instanciating values of " + toNiceSymbolicString() + " with " + outputValues);
            if (parametersValues.length == 1) {
                return parametersValues[0];
            } else {
                return parametersValues;
            }
        } catch (IllegalArgumentException e) {
            if (retryNextTime) {
                Log.logMessage(this, SeverityLevel.INFO, false, "Problem instanciating " + toNiceSymbolicString() + " with " + vars + ": retrying...");
                retryNextTime = false;
                return eval(engine, vars);
            } else {
                Log.logMessage(this, SeverityLevel.PANIC, false, "Problem instanciating " + toNiceSymbolicString() + " with " + vars + ": no more retry.");
            }
        }
        /*catch (ScriptException e) {
         Configuration.logMessage(this, SeverityLevel.PANIC, false, "Problem instanciating " + toNiceSymbolicString() + " with " + outputValues + ":\n  " + e.getMessage());
         }*/

        retryNextTime = true;
        return null;
    }

    public String[] getParametersExpression() {
        return parametersExpression;
    }

    public String[] getParametersNames() {
        return parametersNames;
    }

    public void displayNotValidInformation(String info) {
        Log.logMessage(this, SeverityLevel.WARNING, false, "Output expression " + toString() + " is not valid:" + info);
        //System.err.println("Output expression " + toString() + " is not valid:" + info);
    }

    public String checkValidExpression() {
        if (parametersExpression == null) {
            return "Parameters not defined.";
        }

        for (Object p : parametersExpression) {
            if (((String) p).length() == 0) {
                return "Parameter name is empty.";
            }

        }
        return null;
    }

    public void setParameterExpression(int i, String _parameterExpression) {
        parametersExpression[i] = _parameterExpression;
    }

    public void setParametersExpression(String... _parametersExpression) {
        parametersExpression = _parametersExpression;
    }

    public String toNiceNumericString(Object parametersValues) {
        return asString(parametersValues);

        /*if (parametersValues == null) {
         return "";
         }
         String str = null;
         if (parametersExpression.length > 1) {
         Object[] parametersValuesArray = (Object[]) parametersValues;
         assert parametersExpression.length == parametersValuesArray.length : "Array size not matching: parametersExpression=" + parametersExpression.length + " parametersValuesArray=" + parametersValuesArray.length;
         str = toNiceString(parametersValuesArray);
         } else {
        
         if (parametersValues.getClass().isArray()) {
         if (parametersValues instanceof double[]) {
         double[] parametersValuesArray = (double[]) parametersValues;
         str = toNiceString(ASCII.cat(PARAM_SEPARATOR, parametersValuesArray));
         } else {
         Object[] parametersValuesArray = (Object[]) parametersValues;
         str = toNiceString(ASCII.cat(PARAM_SEPARATOR, parametersValuesArray));
         }
         } else {
         str = toNiceString(parametersValues);
         }
         }
        
         return str;*/

 /*if (parametersValues.getClass().isArray()) {
         Object[] parametersValuesArray = (Object[]) parametersValues;
        
         for (int i = 0; i < parametersExpression.length; i++) {
         str = str.replaceAll((String) parametersExpression[i], parametersValuesArray[i].toString());
         }
        
         return str;
         } else {
         return toNiceSymbolicString().replaceAll((String) parametersExpression[0], parametersValues.toString());
         }*/
    }

    public String toNiceSymbolicString() {
        return toNiceString(parametersExpression);
    }

    public abstract String toNiceString(Object[] params);

    public abstract String[] fromNiceString(String s);

    public double[] fromNiceNumericString(String s) {
        String[] res = /*OutputFunctionInstances.get(this.getClass().getSimpleName()).*/ fromNiceString(s);
        double[] dres = new double[res.length];
        for (int i = 0; i < dres.length; i++) {
            dres[i] = Double.parseDouble(res[i]);
        }
        return dres;
    }

    public abstract boolean isNiceString(String s);

    public String toNiceString(Object aloneparam) {
        return toNiceString(new Object[]{aloneparam});
    }

    /**
     * Return xml compatible string for renderers
     */
    public abstract String getResultRendererData(MathExpression engine, Map<String, Object>... values);

    @Override
    public String toString() {
        return toNiceSymbolicString();
    }

    public String write() {
        return getClass().getSimpleName() + TYPE_DELIMITER + ASCII.cat(PARAM_SEPARATOR, parametersExpression);
    }
}
