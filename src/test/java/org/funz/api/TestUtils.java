package org.funz.api;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.funz.Project;
import static org.funz.api.DesignShell_v1.DEFAULT_FUNCTION_NAME;
import org.funz.calculator.Calculator;
import org.funz.conf.Configuration;
import org.funz.log.Alert;
import org.funz.log.AlertCollector;
import org.funz.log.Log;
import org.funz.log.LogFile;
import org.funz.log.LogTicToc;
import static org.funz.log.LogTicToc.HMS;
import org.funz.main.MainUtils;
import org.funz.script.MathExpression;
import org.funz.util.ASCII;
import static org.funz.util.Data.asString;
import static org.funz.util.Data.newMap;
import org.funz.util.Disk;
import static org.funz.util.ParserUtils.getASCIIFileContent;
import org.junit.After;
import org.junit.Rule;
import org.junit.rules.MethodRule;
import org.junit.rules.TestWatchman;
import org.junit.runners.model.FrameworkMethod;
import org.math.array.DoubleArray;

/**
 *
 * @author richet
 */
public class TestUtils {

    static {
        MainUtils.CLEAR_LINE = "\n> ";
        System.setProperty("java.awt.headless", "true");
        System.setProperty("app.home", "./dist");
        Configuration.writeUserProperty = false;
        Alert.setCollector(new AlertCollector() {

            @Override
            public void showInformation(String string) {
                System.err.println("\nIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII\n" + string + "\nIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII\n");
            }

            @Override
            public void showError(String string) {
                System.err.println("\nEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE\n" + string + "\nEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE\n");
            }

            @Override
            public void showException(Exception i) {
                System.err.println("\nXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX\n" + i.getMessage() + "\nXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX\n");
            }

            @Override
            public String askInformation(String q) {
                System.err.println("\n????????????????????????????????????????????????\n" + q + "\n????????????????????????????????????????????????\n");
                return "???";
            }

            @Override
            public File askPath(String q) {
                System.err.println("\n????????????????????????????????????????????????\n" + q + "\n????????????????????????????????????????????????\n");
                return new File(".");
            }

            @Override
            public boolean askYesNo(String q) {
                System.err.println("\n????????????????????????????????????????????????\n" + q + "\n????????????????????????????????????????????????\n");
                return false;
            }
        });
    }

    public static void out(String string, int i) {
        Log.out(string, i);
    }

    public static void err(String msg, int i) {
        Log.err(msg, i);
    }

    public static void err(Exception ex, int i) {
        Log.err(ex, i);
    }

    public static final String R = "R";

    public static int verbose = 2;
    public static Calculator[] calculators;
    static MathExpression M = null; //new RMathExpression("R://localhost", new File("R.log"));

    public static void test(String arg) throws Exception {
        calculators = new Calculator[8];
        for (int i = 0; i < calculators.length - 1; i++) {
            calculators[i] = startCalculator(i, CONF_XML);
        }
        calculators[calculators.length - 1] = startCalculator(calculators.length - 1, CONF_XML_FAILING);

        Funz_v1.init(null, null, M);
        Configuration.setVerboseLevel(verbose);

        org.junit.runner.JUnitCore.main(arg);

        for (int i = 0; i < calculators.length; i++) {
            calculators[i].askToStop("end test " + arg);
        }
    }

    @Rule
    public MethodRule watchman = new TestWatchman() {
        @Override
        public void starting(FrameworkMethod method) {
            super.starting(method);
            LogTicToc.tic(method.getName());
        }

        @Override
        public void finished(FrameworkMethod method) {
            LogTicToc.toc(method.getName());
            super.finished(method);
        }
    };

    String[] CLEANUP_FILES = {"(.*)\\.Rdata", "(.*)\\.png", "x(.*)"};

    @After
    public void tearDown() throws InterruptedException {
        Log.setCollector(Log.SystemCollector);

        if (Funz_v1.POOL != null) {
            Funz_v1.POOL.shutdown();
        }
        Thread.sleep(2000);

        for (final String m : CLEANUP_FILES) {
            File[] todelete = new File(".").listFiles(new FilenameFilter() {

                @Override
                public boolean accept(File dir, String name) {
                    if (name.matches(m)) {
                        return true;
                    }
                    return false;
                }
            });
            for (File file : todelete) {
                if (file.isFile()) {
                    System.err.println("Delete file " + file + " : " + file.delete());
                }
                if (file.isDirectory()) {
                    System.err.println("Delete directory " + file);
                    try {
                        org.apache.commons.io.FileUtils.deleteDirectory(file);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }

    static File tmp = new File("tmp");

    public static File newTmpDir(String id) throws IOException {
        if (!tmp.isDirectory()) {
            FileUtils.forceMkdir(tmp);
        }

        File tmpdir = new File(tmp, id);
        while (tmpdir.exists()) {
            tmpdir = new File(tmp, tmpdir.getName() + "_"); // that should not occur !
        }
        int n = 5;
        while (!tmpdir.isDirectory() && (n--) > 0) {
            try {
                org.apache.commons.io.FileUtils.forceMkdir(tmpdir);
            } catch (Exception e) {
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
            }
        }
        return tmpdir;
    }

    public static File newTmpFile(String id) throws IOException {
        if (!tmp.isDirectory()) {
            FileUtils.forceMkdir(tmp);
        }

        File tmpfile = new File(tmp, id);
        while (tmpfile.exists()) {
            tmpfile = new File(tmp, tmpfile.getName() + "_"); // that should not occur !
        }
        return tmpfile;
    }

    public void setUp(String name) throws Exception {
        MainUtils.init(name, verbose);
        System.err.println("Test init at: " + HMS());

        File fdir = new File(".f");
        org.apache.commons.io.FileUtils.deleteDirectory(fdir);

        if (Funz_v1.POOL == null) {
            throw new Exception("POOL is null !!!");
        } else 
            Funz_v1.POOL.wakeup();

        System.err.println(Funz_v1.POOL.toString());

        Project.DEFAULT_waitingTimeout = 10;//10 s. max before hard stopping batch if no calc found.
        Project.DEFAULT_blacklistTimeout = 600;//10 s. max before hard stopping batch if no calc found.
    }

    public static final String CONF_XML = "./dist/calculator.xml";
    public static final String CONF_XML_FAILING = "./dist/calculator.fail.xml";

    static {
        ASCII.saveFile(new File(CONF_XML_FAILING), getASCIIFileContent(new File(CONF_XML)).replace("R CMD", "RR CMD"));
    }

    public static Calculator startCalculator(final int i, String conf_xml) throws Exception {
        System.err.println("------------- startCalculator " + i + " " + conf_xml);
        File conf = new File(conf_xml);
        assert conf.exists();
        Calculator.PING_PERIOD = 1000;
        final Calculator calc = new Calculator("file:" + conf.getAbsolutePath(), new LogFile("Calculator." + i + ".out"), new LogFile("Calculator." + i + ".log"));
        new Thread("Calculator " + i) {

            @Override
            public void run() {
                calc.runloop();
            }
        }.start();
        Thread.sleep(100);
        return calc;
    }

    static boolean alltrue(boolean[] a) {
        for (int i = 0; i < a.length; i++) {
            if (!a[i]) {
                return false;
            }
        }
        return true;
    }

    public static String round2(String z) {
        return round2(new String[]{z})[0];
    }

    public static String round2(Object z) {
        return round2(new Object[]{z})[0];
    }

    public static String[] round2(String[] z) {
        if (z == null) {
            return null;
        }
        String[] newz = new String[z.length];
        for (int i = 0; i < newz.length; i++) {
            boolean endbracket = false;
            if (z[i] != null && z[i].contains("]")) {
                endbracket = true;
            }
            if (z[i] != null && z[i].contains(".")) {
                if (endbracket) {
                    newz[i] = z[i].substring(0, Math.min(z[i].indexOf('.') + 2, z[i].indexOf(']')));
                    newz[i] = newz[i] + "]";
                } else {
                    newz[i] = z[i].substring(0, z[i].indexOf('.') + 2);
                }
            } else {
                newz[i] = z[i];
            }
            if (newz[i] != null) {
                newz[i] = newz[i].replace("[", "");
                newz[i] = newz[i].replace("]", "");
            }
        }
        return newz;
    }

    public static String[] round2(Object[] z) {
        if (z == null) {
            return null;
        }
        String[] newz = new String[z.length];
        for (int i = 0; i < newz.length; i++) {
            boolean endbracket = false;
            if (asString(z[i]).contains("]")) {
                endbracket = true;
            }
            if (z[i] == null) {
                newz[i] = null;
            } else if (asString(z[i]).contains(".")) {
                if (endbracket) {
                    newz[i] = asString(z[i]).substring(0, Math.min(asString(z[i]).indexOf('.') + 2, asString(z[i]).indexOf(']')));
                    newz[i] = newz[i] + "]";
                } else {
                    newz[i] = asString(z[i]).substring(0, asString(z[i]).indexOf('.') + 2);
                }
            } else {
                newz[i] = asString(z[i].toString());
            }
            newz[i] = newz[i].replace("[", "");
            newz[i] = newz[i].replace("]", "");
        }
        return newz;
    }

    /* @see R::DiceKriging::branin
    branin <- function(x) {
	x1 <- x[1]*15-5   
	x2 <- x[2]*15     
	(x2 - 5/(4*pi^2)*(x1^2) + 5/pi*x1 - 6)^2 + 10*(1 - 1/(8*pi))*cos(x1) + 10
    }
     */
    public static double branin(double x1, double x2) {
        x1 = x1 * 15 - 5;
        x2 = x2 * 15;
        return Math.pow(x2 - 5 / (4 * Math.PI * Math.PI) * (x1 * x1) + 5 / Math.PI * x1 - 6, 2) + 10 * (1 - 1 / (8 * Math.PI)) * Math.cos(x1) + 10;
    }

    double branin_min = 0.4;
    double[] branin_xmin1 = {0.9616520, 0.15};
    double[] branin_xmin2 = {0.1238946, 0.8166644};
    double[] branin_xmin3 = {0.5427730, 0.15};

    public static String[] branin(String[] x1, String[] x2) {
        assert x1.length == x2.length;
        String[] z = new String[x1.length];
        for (int i = 0; i < z.length; i++) {
            z[i] = "[" + branin(Double.parseDouble(x1[i]), Double.parseDouble(x2[i])) + "]";
        }
        return round2(z);
    }

    DesignShell_v1.Function branin = new DesignShell_v1.Function(DEFAULT_FUNCTION_NAME, "x1", "x2") {
        @Override
        public Map f(Object... strings) {
            double[] x = new double[strings.length];
            for (int i = 0; i < x.length; i++) {
                x[i] = Double.parseDouble(strings[i].toString());
            }
            double x1 = x[0];
            double x2 = x[1];
            return newMap(DEFAULT_FUNCTION_NAME, branin(x1, x2));
        }
    };

    public static File branin_in() throws IOException {
        File tmp_in = newTmpFile("branin.R");
        if (tmp_in.exists()) {
            tmp_in.delete();
        }
        Disk.copyFile(new File("src/test/samples/branin.R"), tmp_in);
        assert tmp_in.exists() : "File " + tmp_in + " does not exist.";

        return tmp_in;
    }

    static DesignShell_v1.Function mult = new DesignShell_v1.Function(DEFAULT_FUNCTION_NAME, "x1", "x2") {
        @Override
        public Map f(Object... strings) {
            double[] vals = new double[strings.length];
            for (int i = 0; i < vals.length; i++) {
                vals[i] = Double.parseDouble(strings[i].toString());
            }
            return newMap(mult.fname, DoubleArray.product(vals));
        }
    };

    public static File mult_in() throws IOException {
        File tmp_in = newTmpFile("mult.R");
        if (tmp_in.exists()) {
            tmp_in.delete();
        }
        Disk.copyFile(new File("src/test/samples/mult.R"), tmp_in);
        assert tmp_in.exists() : "File " + tmp_in + " does not exist.";

        return tmp_in;
    }

    public final static double mult_min = -0.4;
    public final static double mult_x1_min = -0.5;
    public final static double mult_x1_max = -0.1;
    public final static double mult_x2_min = 0.3;
    public final static double mult_x2_max = 0.8;
}
