package org.funz.api;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import org.funz.calculator.Calculator;
import org.funz.conf.Configuration;
import org.funz.log.Alert;
import org.funz.log.AlertCollector;
import org.funz.log.Log;
import org.funz.log.LogFile;
import org.funz.main.MainUtils;
import org.funz.script.MathExpression;
import org.funz.util.ASCII;
import static org.funz.util.Data.asString;
import org.funz.util.Disk;
import static org.funz.util.ParserUtils.getASCIIFileContent;
import org.junit.After;

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

    public static File tmp_in() throws IOException {
        File tmp_in = new File("tmp/branin.R");
        if (tmp_in.exists()) {
            tmp_in.delete();
        }
        Disk.copyFile(new File("src/main/resources/samples/branin.R"), tmp_in);
        assert tmp_in.exists() : "File " + tmp_in + " does not exist.";

        return tmp_in;
    }

    public static int verbose = 2;
    public static Calculator[] calculators;
    static MathExpression M = null;// = new RenjinMathExpression("Renjin");

    public static void test(String arg) throws Exception {
        Configuration.setVerboseLevel(verbose);

        calculators = new Calculator[4];
        for (int i = 0; i < calculators.length - 1; i++) {
            calculators[i] = startCalculator(i, CONF_XML);
        }
        calculators[calculators.length - 1] = startCalculator(calculators.length - 1, CONF_XML_FAILING);

        Funz_v1.init(null, null, M);

        org.junit.runner.JUnitCore.main(arg);

        for (int i = 0; i < calculators.length; i++) {
            calculators[i].askToStop("end test " + arg);
        }
    }

    String[] CLEANUP_FILES = {"(.*)\\.Rdata", "(.*)\\.png", /*"(.*)\\.csv",*/ "x(.*)"};

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

    public void setUp(String name) throws Exception {
        MainUtils.init(name, verbose);

        File dir = new File("tmp");
        org.apache.commons.io.FileUtils.deleteDirectory(dir);
        org.apache.commons.io.FileUtils.forceMkdir(dir);
        org.apache.commons.io.FileUtils.cleanDirectory(dir);

        File fdir = new File(".f");
        org.apache.commons.io.FileUtils.deleteDirectory(fdir);
        
        if (Funz_v1.POOL == null) {
            throw new Exception("POOL is null !!!");
        }
    }

    public static final String CONF_XML = "calculator.xml";
    public static final String CONF_XML_FAILING = "calculator.fail.xml";

    static {
        ASCII.saveFile(new File(CONF_XML_FAILING), getASCIIFileContent(new File(CONF_XML)).replace("R CMD", "RR CMD"));
    }

    public static Calculator startCalculator(final int i, String conf_xml) throws Exception {
        System.err.println("------------- startCalculator " + i + " " + conf_xml);
        File conf = new File(conf_xml);
        assert conf.exists();
        final Calculator calc = new Calculator("file:" + conf.getName(), new LogFile("Calculator." + i + ".out"), new LogFile("Calculator." + i + ".log"));
        new Thread("Calculator " + i) {

            @Override
            public void run() {
                calc.run();
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

    public static String[] f(String[] x1, String[] x2) {
        assert x1.length == x2.length;
        String[] z = new String[x1.length];
        for (int i = 0; i < z.length; i++) {
            z[i] = "[" + f(Double.parseDouble(x1[i]), Double.parseDouble(x2[i])) + "]";
        }
        return round2(z);
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

    public static double f(double x1, double x2) {
        x1 = x1 * 15 - 5;
        x2 = x2 * 15;
        return Math.pow(x2 - 5 / (4 * Math.PI * Math.PI) * (x1 * x1) + 5 / Math.PI * x1 - 6, 2) + 10 * (1 - 1 / (8 * Math.PI)) * Math.cos(x1) + 10;
    }
}
