package org.funz.doe;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Properties;
import org.funz.Project;
import org.funz.conf.Configuration;
import org.funz.doeplugin.Design;
import org.funz.doeplugin.DesignConstants.Status;
import org.funz.doeplugin.DesignHelper;
import org.funz.doeplugin.DesignSession;
import org.funz.doeplugin.Designer;
import org.funz.doeplugin.Experiment;
import org.funz.ioplugin.ExtendedIOPlugin;
import org.funz.parameter.Case;
import org.funz.parameter.Case.Node;
import org.funz.parameter.OutputFunctionExpression;
import org.funz.parameter.Parameter;
import org.funz.script.MathExpression;
import org.funz.script.RMathExpression;
import org.funz.util.ASCII;
import org.math.R.*;
import org.math.array.DoubleArray;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RserveException;

/**
 *
 * @author richet
 */
public abstract class TestUtils {

    public static File TEST_DIR = new File("./tmp/");

    static {
        boolean ok = true;
        if (!TEST_DIR.exists()) {
            ok = TEST_DIR.mkdir();
        }
        if (!ok) {
            System.err.println("Impossible to create log directory in " + TEST_DIR.getAbsolutePath());
            System.exit(-1);
        }
    }
    public static String OUTPUT_NAME = "Y";
    public String analyse = "";
    public Design design;
    public Designer designer;
    public ArrayList<Experiment> experiments = new ArrayList<Experiment>();
    //public JFrame frame;
    public int nparam = -1;
    public HashMap<String, String> options;
    public Case parent;
    public Project prj;
    public Status s;
    public DesignSession ses;
    public static Rsession R;

    /*static {
     System.out.println("Proxy.http_proxy()="+Proxy.http_proxy());
     R.init(System.out, new File("tmp/Funz/R"), Proxy.http_proxy());
     OutputFunctionExpression.initOutputFunctionTypes();
     }*/
    public static void initEngine(Properties p) {
        Configuration.readProperties(null);
        if (p != null) {
            for (String k : p.stringPropertyNames()) {
                Configuration.setProperty(k, p.getProperty(k));
            }
        }

        MathExpression.SetDefaultInstance(RMathExpression.class);

        R = ((RMathExpression) (MathExpression.GetDefaultInstance())).R;
        System.err.println("R " + R);

        File test_functions = new File(System.getProperty("test.functions", "src" + File.separator + "test" + File.separator + "R" + File.separator + "TestFunctions.R"));
        R.source(test_functions);

        /*try {
         System.out.println("libPaths=" + R.cat(R.eval(".libPaths()").asStrings()));
         } catch (REXPMismatchException r) {
         r.printStackTrace(System.err);
         }*/
    }

    public TestUtils(Properties p) {
        //if (R == null) {
        System.setProperty("app.user", "tmp/Funz");
        initEngine(p);
        //}
    }

    public TestUtils(HashMap<String, String> o) {
        this(o, null);
    }

    public TestUtils(HashMap<String, String> o, Properties p) {
        this(p);
        options = o;
        try {
            init();
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }

    public void init() throws Exception {
        prj = new Project(getClass().getSimpleName());
        prj.setPlugin(new ExtendedIOPlugin());
        parent = new Case(0, new Node[]{new Node(Project.SINGLE_PARAM_NAME, prj)}, prj);

        initIO();
        designer.setOptions(options);

        prj.resetDiscreteCases(null);

        OutputFunctionExpression.initOutputFunctionTypes();
        LinkedList<Parameter> paramsl = new LinkedList<Parameter>();
        for (Parameter p : designer.getParameters()) {
            paramsl.add(p);
        }
        String valid = designer.isValid(paramsl, designer.getOutputFunctionExpression());
        assert valid.equals(Designer.VALID) : valid;

        ses = new DesignSession(0);
        design = designer.createDesign(ses);

        design.init(TEST_DIR);
        designLoop();
    }

    @Override
    protected void finalize() throws Throwable {
        R.end();
    }

    public abstract void initIO();

    void designLoop() {
        //System.out.println(designer.getOptions());

        int i = 0;
        ArrayList<Experiment> torun = new ArrayList<Experiment>();
        s = design.getInitialDesign(torun);
        if (s.getDecision() != Design.Decision.READY_FOR_NEXT_ITERATION) {
            System.err.println("Failed initDesign : " + s.getMessage());
        }
        run(i, torun);
        analyse = design.displayResultsTmp(experiments).replace(DesignHelper.BASE, ".");
        printAnalysis("Run design " + i + " : " + s.getDecision(), analyse);

        while (s.getDecision() == Design.Decision.READY_FOR_NEXT_ITERATION) {
            i++;
            torun.clear();

            s = design.getNextDesign(experiments, torun);
            if (s.getDecision() == Design.Decision.READY_FOR_NEXT_ITERATION) {
                run(i, torun);
                analyse = design.displayResultsTmp(experiments).replace(DesignHelper.BASE, ".");
                printAnalysis("Run design " + i + " : " + s.getDecision(), analyse);
            }
        }

        analyse = design.displayResults(experiments).replace(DesignHelper.BASE, ".");
        printAnalysis("End of design", analyse);
    }

    public ArrayList<Experiment> f(ArrayList<Experiment> X) {
        ArrayList<Experiment> calculated = new ArrayList<Experiment>();
        for (Experiment experiment : X) {
            calculated.add(f(experiment));
        }
        return calculated;
    }

    public Experiment f(Experiment x) {
        HashMap<String, Object> res = new HashMap<String, Object>();
        res.put(OUTPUT_NAME, f(DesignHelper.getInputArray(x)));
        Case xy = new Case(0, x, parent, prj);
        xy.setOutputValues(res);
        return xy;
    }

    public abstract Object f(double[] x);

    public static double branin(double[] x) {
        assert x.length == 2 : "branin function requires 2D argument";
        try {
            return R.asDouble(R.eval("branin(cbind(" + ASCII.cat(",", x) + "))"));
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
        return Double.NaN;
    }

    public static double peaks(double[] x) {
        assert x.length == 2 : "peaks function requires 2D argument";
        try {
            return R.asDouble(R.eval("peaks(cbind(" + ASCII.cat(",", x) + "))"));
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
        return Double.NaN;
    }

    public static double ishigami(double[] x) {
        assert x.length == 3 : "ishigami function requires 3D argument";
        try {
            return R.asDouble(R.eval("ishigami(cbind(" + ASCII.cat(",", x) + "))"));
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
        return Double.NaN;
    }

    public static double morris(double[] x) {
        assert x.length == 20 : "morris function requires 20D argument";
        try {
            return R.asDouble(R.eval("fmorris(cbind(" + ASCII.cat(",", x) + "))"));
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
        return Double.NaN;
    }

    public static double sobol(double[] x) {
        assert x.length == 8 : "sobol function requires 8D argument";
        try {
            return R.asDouble(R.eval("fsobol(cbind(" + ASCII.cat(",", x) + "))"));
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
        return Double.NaN;
    }

    public static double camelback(double[] x) {
        assert x.length == 2 : "camelback function requires 2D argument";
        try {
            return R.asDouble(R.eval("camelback(cbind(" + ASCII.cat(",", x) + "))"));
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
        return Double.NaN;
    }

    public static double hartman3(double[] x) {
        assert x.length == 3 : "hartman3 function requires 3D argument";
        try {
            return R.asDouble(R.eval("hartman3(cbind(" + ASCII.cat(",", x) + "))"));
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
        return Double.NaN;
    }

    public static double hartman6(double[] x) {
        assert x.length == 6 : "hartman6 function requires 6D argument";
        try {
            return R.asDouble(R.eval("hartman6(cbind(" + ASCII.cat(",", x) + "))"));
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
        return Double.NaN;
    }

    public static double goldsteinPrice(double[] x) throws REXPMismatchException {
        assert x.length == 2 : "goldsteinPrice function requires 2D argument";
        try {
            return R.asDouble(R.eval("goldsteinPrice(cbind(" + ASCII.cat(",", x) + "))"));
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
        return Double.NaN;
    }

    public static void main(String[] args) throws REXPMismatchException, RserveException {
        System.setProperty("app.user", "tmp/Funz");
        TestUtils.initEngine(null);

        double[] x = DoubleArray.random(2);
        System.out.println("branin(" + ASCII.cat(",", x) + ") = " + branin(x));

        x = DoubleArray.random(2);
        System.out.println("peaks(" + ASCII.cat(",", x) + ") = " + peaks(x));

        x = DoubleArray.random(3);
        System.out.println("ishigami(" + ASCII.cat(",", x) + ") = " + ishigami(x));

        x = DoubleArray.random(20);
        System.out.println("morris(" + ASCII.cat(",", x) + ") = " + morris(x));

        x = DoubleArray.random(8);
        System.out.println("sobol(" + ASCII.cat(",", x) + ") = " + sobol(x));

        x = DoubleArray.random(2);
        System.out.println("camelback(" + ASCII.cat(",", x) + ") = " + camelback(x));

        x = DoubleArray.random(3);
        System.out.println("hartman3(" + ASCII.cat(",", x) + ") = " + hartman3(x));

        x = DoubleArray.random(6);
        System.out.println("hartman6(" + ASCII.cat(",", x) + ") = " + hartman6(x));

        x = DoubleArray.random(2);
        System.out.println("goldsteinPrice(" + ASCII.cat(",", x) + ") = " + goldsteinPrice(x));

        R.end();
    }

    void printAnalysis(String name, String a) {
        System.out.println("\n## Analysis: " + name + "\n\n  " + a.replace("<br/>", "\n  "));
    }

    void run(int i, ArrayList<Experiment> torun) {
        System.out.println("\nDecision " + i + " : " + s.getDecision() + " : " + s.getMessage());
        //System.out.println(Experiment.toString_ExperimentArray("torun", torun));
        System.out.println("\n# Run " + i+"\n");
        experiments.addAll(f(torun));
        System.out.println(Experiment.toString_ExperimentArray("# Experiments", experiments));
    }
}
