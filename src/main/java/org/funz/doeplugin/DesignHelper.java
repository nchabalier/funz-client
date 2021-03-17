package org.funz.doeplugin;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.funz.Project;
import org.funz.log.Log;
import org.funz.log.LogCollector.SeverityLevel;
import org.funz.parameter.OutputFunctionExpression;
import org.funz.parameter.OutputFunctionExpression.Numeric;
import org.funz.parameter.Parameter;
import org.funz.util.ASCII;
import org.funz.util.Data;
import org.math.array.DoubleArray;
import org.math.plot.PlotPanel;

/**
 *
 * @author richet
 */
public class DesignHelper {

    public final static String BASE = "__BASE__";

    /**
     * convenience method to build relative URL for img output, for instance
     */
    public static String getResultsRelativePath(File f, String rootdir) {
        String relpath = f.getPath().replace(File.separatorChar, '/');//needed because it returns an html path
        if (relpath.contains(Project.SPOOL_DIR)) {
            relpath = relpath.substring(relpath.indexOf(Project.SPOOL_DIR) + Project.SPOOL_DIR.length() + 1);
        }

        if (relpath.contains(Project.RESULTS_DIR)) {
            relpath = relpath.substring(relpath.indexOf(Project.RESULTS_DIR) + Project.RESULTS_DIR.length() + 1);
        }

        if (rootdir != null) {
            if (relpath.contains(rootdir)) {
                relpath = relpath.substring(relpath.indexOf(rootdir) + rootdir.length() + 1);
            }
        }

        if (relpath.startsWith(Project.SINGLE_PARAM_NAME)) {
            relpath = relpath.substring(Project.SINGLE_PARAM_NAME.length() + 1);
        }
        return BASE + "/" + relpath;
    }

    /**
     * convenience method to call in analyseDesign impl
     */
    public static String HTMLTable(List<? extends Experiment> experiments, OutputFunctionExpression f) {
        StringBuilder tableresults = new StringBuilder("<table>");

        //values
        for (Experiment experiment : experiments) {
            tableresults.append("<tr>");
            for (int i = 0; i < experiment.getNmOfParameters(); i++) {
                tableresults.append("<td>");
                tableresults.append(experiment.getValueExpression(i));
                tableresults.append("</td>");
            }

            //output values or output function value
            tableresults.append("<td>");
            String o = "";
            if (experiment.getOutputValues() == null || experiment.getOutputValues().isEmpty()) {
                o = "?";
            } else if (f == null) {
                o = ASCII.cat(";", experiment.getOutputValues());
            } else {
                try {
                    o = f.toNiceSymbolicString() + "=" + f.toNiceNumericString(f.eval(experiment.prj.getPlugin().getFormulaInterpreter(), experiment.getOutputValues(), experiment.getInputValues(), experiment.getIntermediateValues()));
                } catch (Exception ex) {
                    Log.logException(false, ex);
                    o = ex.toString();
                }
            }
            tableresults.append(o);
            tableresults.append("</td>");
            tableresults.append("</tr>");
        }
        tableresults.append("</table>");
        return tableresults.toString();
    }

    public static String buildPNGPlot(File target, PlotPanel plot, int width, int height) {
        /*Plot2DPanel plot = new Plot2DPanel();
         if (y.length > 1) {
         plot.addScatterPlot("", getColumnCopy(x, 0), y);
         }
        
         plot.setAxisLabel(1, _f.toNiceSymbolicString());
         plot.getAxis(1).setLabelPosition(-.15, .5);
        
         plot.setAxisLabel(0, p.getName());
         plot.getAxis(0).setLabelPosition(.5, -.1);
         plot.getAxis(1).setLabelAngle(-Math.PI / 2);*/

        try {
            plot.plotCanvas.setSize(width, height);
            BufferedImage bufferedImage = new BufferedImage(plot.plotCanvas.getWidth(), plot.plotCanvas.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = bufferedImage.createGraphics();
            Thread.sleep(500);
            plot.plotCanvas.paint(g);
            g.dispose();
            ImageIO.write((RenderedImage) bufferedImage, "PNG", target);
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }

        return "<img src='" + DesignHelper.getResultsRelativePath(target, null) + "' width='" + width + "' height='" + height + "'/>";
    }

    public static boolean IsOutputFull(List<? extends Experiment> exps) {
        for (Experiment experiment : exps) {
            if (experiment.getOutputValues() == null || experiment.getOutputValues().isEmpty()) {
                //System.err.println("IsOutputFull > " + false + "\n   for experiment " + experiment.toString());
                return false;
            }
            /*DO NOT IMPLEMENT THIS (because full output is true even when nan are inside): else {
             for (String ok : experiment.getOutputValues().keySet()) {
             Object o = experiment.getOutputValues().get(ok);
             if (o instanceof Double && ((Double) o).isNaN()) {
             return false;
             }
             if (o == null || o.toString().length() == 0) {
             return false;
             }
             }
             return true;
             }*/
        }
        //System.err.println("IsOutputFull > " + true);
        return true;
    }

    public static List<Experiment> getFinishedExperiments(List<? extends Experiment> experiments) {
        List<Experiment> finishedExperiments = new ArrayList<Experiment>();
        for (Experiment experiment : experiments) {
            if (experiment.getOutputValues() != null && !experiment.getOutputValues().isEmpty()) {
                finishedExperiments.add(experiment);
            }
        }
        return finishedExperiments;
    }

    public static double[] getOutputArray(List<? extends Experiment> finishedExperiments, OutputFunctionExpression f) {
        if (!(f instanceof Numeric)) {
            throw new IllegalArgumentException("Impossible to cast as numeric array: " + f.toString());
        }
        //assert (f instanceof Numeric) : "Impossible to cast as numeric array: " + f.toString();
        double[] z = new double[finishedExperiments.size()];
        for (int i = 0; i < z.length; i++) {
            Experiment e = finishedExperiments.get(i);
            try {
                z[i] = (Double) e.doEval(f);
            } catch (Exception ex) {
                Log.logException(false, ex);
                z[i] = Double.NaN;
            }
        }
        return z;
    }

    static double[] rep(int times, double val) {
        double[] ret = new double[times];
        for (int i = 0; i < times; i++) {
            ret[i] = val;
        }
        return ret;
    }

    public static double[][] getOutputParams(List<? extends Experiment> finishedExperiments, OutputFunctionExpression f) {
        double[][] z = new double[finishedExperiments.size()][];
        int l = 0;
        for (int i = 0; i < z.length; i++) {
            Experiment e = finishedExperiments.get(i);
            try {
                try {
                    z[i] = DesignHelper.castDoubleArray(e.doEval(f));
                    if (z[i] != null && l == 0 && z[i].length > 0) {
                        l = z[i].length;
                    }
                } catch (Exception ex) {
                    Log.logMessage("DesignHelper", SeverityLevel.ERROR, true, e.toString());
                    ex.printStackTrace(System.err);
                    z[i] = rep(l, Double.NaN);
                }
            } catch (ClassCastException cce) {
                String c;
                try {
                    c = e.doEval(f).getClass().getSimpleName();
                } catch (Exception ex) {
                    Log.logException(false, ex);
                    ex.printStackTrace(System.err);
                    c = "?";
                }
                Log.logMessage(f, SeverityLevel.ERROR, false, "Problem while casting " + f.toNiceSymbolicString() + " to (double[]): \n  in fact class is " + c + " : " + e.getOutputValues());
                cce.printStackTrace(System.err);
            }
        }
        for (int i = 0; i < z.length; i++) {
            if (z[i] == null || z[i].length != l) {
                z[i] = rep(l, Double.NaN);
            }
        }
        return z;
    }

    public static double[] castDoubleArray(Object in) {
        if (in == null) {
            return null;
        }

        if (in instanceof Double) {
            return new double[]{(Double) in};
        }

        if (in instanceof String) {
            return castDoubleArray(Data.asObject((String) in));
        }

        int i = 0;
        if (in instanceof Double[]) {
            Double[] inarray = (Double[]) in;
            double[] outarray = new double[inarray.length];
            for (Double object : inarray) {
                if (object != null) {
                    outarray[i++] = object;
                } else {
                    outarray[i++] = Double.NaN;
                }
            }
            return outarray;
        } else if (in instanceof Object[]) {
            Object[] inarray = (Object[]) in;
            if (inarray.length==1) // to squeeze dimensions...
                return castDoubleArray(inarray[0]);
            double[] outarray = new double[inarray.length];
            for (Object object : inarray) {
                if (object != null) {
                    outarray[i++] = (Double) object;
                } else {
                    outarray[i++] = Double.NaN;
                }
            }
            return outarray;
        } else if (in instanceof double[]) {
            return (double[]) in;
        } else {
            Log.logMessage("castDoubleArray",SeverityLevel.WARNING, false,"Could not cast to double[]: " + Data.asString(in) + " (class " + in.getClass() + ")");
            return null;
        }
    }

    public static double[][] getBounds(Parameter[] parameters) {
        double[][] bounds = new double[parameters.length][2];
        int i = 0;
        for (Parameter p : parameters) {
            bounds[i][0] = p.getLowerBound();
            bounds[i][1] = p.getUpperBound();
            i++;
        }
        return bounds;
    }

    public static Experiment createExperiment(double[] doe, Parameter[] parameters, Project prj) {
        DesignedExperiment e = new DesignedExperiment(parameters.length, prj);
        int i = 0;
        for (Parameter p : parameters) {
            e.setValueExpression(i, p.getName() + "=" + doe[i]);
            i++;
        }
        return e;
    }

    public static List<Experiment> createExperiments(double[][] doe, Parameter[] parameters, Project prj) {
        List<Experiment> experimentsToAppendInQueue = new ArrayList<Experiment>(doe.length);
        for (int j = 0; j < doe.length; j++) {
            experimentsToAppendInQueue.add(createExperiment(doe[j], parameters, prj));
        }
        return experimentsToAppendInQueue;
    }

    public static double[][] getInputArray(List<? extends Experiment> exps) {
        double[][] doe = new double[exps.size()][];
        for (int j = 0; j < doe.length; j++) {
            doe[j] = new double[exps.get(j).getNmOfParameters()];
            for (int i = 0; i < exps.get(j).getNmOfParameters(); i++) {
                doe[j][i] = exps.get(j).getDoubleValue(i);
            }
        }
        return doe;
    }

    public static double[] getInputArray(Experiment e) {
        double[] x = new double[e.getNmOfParameters()];
        for (int i = 0; i < x.length; i++) {
            x[i] = e.getDoubleValue(i);
        }
        return x;
    }

    public static boolean equals(double[][] x, double[][] y) {
        if (x.length != y.length) {
            return false;
        }
        for (int i = 0; i < y.length; i++) {
            if (!equals(x[i], y[i])) {
                //System.err.println(DoubleArray.toString(x[i]) + " != " + DoubleArray.toString(y[i]));
                return false;
            }
        }
        return true;
    }

    public static boolean equals(double[] x, double[] y) {
        if (x.length != y.length) {
            return false;
        }
        for (int i = 0; i < y.length; i++) {
            if (x[i] != y[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean checkInputEquals(List<? extends Experiment> exps, double[][] doe) {
        return equals(getInputArray(exps), doe);

    }

    public static boolean checkContains(List<? extends Experiment> exps, double... x) {
        for (Experiment e : exps) {
            if (equals(getInputArray(e), x)) {
                return true;
            }
        }
        return false;
    }
}
