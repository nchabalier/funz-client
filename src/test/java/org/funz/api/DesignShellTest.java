package org.funz.api;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.Map;
import org.funz.log.Log;
import org.funz.log.LogFile;
import org.funz.script.RMathExpression;
import org.funz.util.ASCII;
import static org.funz.util.Data.newMap;
import org.funz.util.Disk;
import static org.funz.util.Format.ArrayMapToMDString;
import org.funz.util.Parser;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author richet
 */
public class DesignShellTest extends org.funz.api.TestUtils {

    @Before
    public void setUp() throws Exception {
        Funz_v1.init();
    }

    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main(DesignShellTest.class.getName());
    }

    // @Test
    public void testDirect() throws Exception {
        System.err.println("+++++++++++++++++++++++++ testDirect");
        Funz.setVerbosity(3);

        HashMap<String, String> variable_bounds = new HashMap<String, String>();
        variable_bounds.put("x1", "[0,1]");
        variable_bounds.put("x2", "[0,1]");

        DesignShell_v1 shell = new DesignShell_v1(branin, "Test direct", variable_bounds, null);

        shell.startComputationAndWait();
        System.err.println(ArrayMapToMDString(shell.getResultsArrayMap()));

        shell.shutdown();
    }

    // @Test
    public void testIterative() throws Exception {
        System.err.println("+++++++++++++++++++++++++ testIterative");
        Funz.setVerbosity(3);

        HashMap<String, String> variable_bounds = new HashMap<String, String>();
        variable_bounds.put("x1", "[0,1]");
        variable_bounds.put("x2", "[0,1]");
        DesignShell_v1 shell = new DesignShell_v1(branin, "Test iterative", variable_bounds, null);
        shell.setDesignOption("N", "5");
        shell.setDesignOption("repeat", "10");

        shell.startComputationAndWait();
        System.err.println(ArrayMapToMDString(shell.getResultsArrayMap()));

        shell.shutdown();
    }

    @Test
    public void testRGradientDescent() throws Exception {
        System.err.println("+++++++++++++++++++++++++ testRGradientDescent");
        Funz.setVerbosity(3);

        HashMap<String, String> variable_bounds = new HashMap<String, String>();
        variable_bounds.put("x1", "["+mult_x1_min+","+mult_x1_max+"]");
        variable_bounds.put("x2", "["+mult_x2_min+","+mult_x2_max+"]");
        DesignShell_v1 shell = new DesignShell_v1(mult, "GradientDescent", variable_bounds, null);
        shell.setArchiveDirectory(newTmpDir("testRGradientDescent"));
        shell.setDesignOption("nmax", "3");

        shell.startComputationAndWait();

        Map<String, Object[]> res = shell.getResultsArrayMap();

        assert res.get("state") != null : "Status null:" + res;
        assert res.get("state").length == 1 : "Status: '" + ASCII.cat(",", res.get("state")) + "'";
        assert res.get("state")[0].toString().contains("Design over") : "Status: '" + res.get("state")[0] + "'";

        assert ASCII.cat("\n", res.get("analysis")).contains("minimum is ") : "No convergence :" + ASCII.cat("\n", res.get("analysis"));
        double min_found = Double.parseDouble(Parser.after(ASCII.cat("\n", res.get("analysis")),"minimum is ").trim().substring(0,4));
        assert min_found >= mult_min: "Wrong convergence :" + ASCII.cat("\n", res.get("analysis"));
        
        System.err.println(ArrayMapToMDString(shell.getResultsStringArrayMap()));

        shell.shutdown();
    }

    @Test
    public void testManyOutput() throws Exception {
        System.err.println("+++++++++++++++++++++++++ testManyOutput");
        Funz.setVerbosity(3);

        HashMap<String, String> variable_bounds = new HashMap<String, String>();
        variable_bounds.put("x1", "["+mult_x1_min+","+mult_x1_max+"]");
        variable_bounds.put("x2", "["+mult_x2_min+","+mult_x2_max+"]");
        DesignShell_v1 shell = new DesignShell_v1(sumprod, "RandomUnif", variable_bounds, null);
        shell.setArchiveDirectory(newTmpDir("testManyOutput"));
        shell.setDesignOption("sample_size", "10");
        shell.setOutputExpressions("("+sumprod.fname+"[1],"+sumprod.fname+"[2])");

        shell.startComputationAndWait();

        Map<String, Object[]> res = shell.getResultsArrayMap();

        assert res.get("state") != null : "Status null:" + res;
        assert res.get("state").length == 1 : "Status: '" + ASCII.cat(",", res.get("state")) + "'";
        assert res.get("state")[0].toString().contains("Design over") : "Status: '" + res.get("state")[0] + "'";

        assert res.get("cat[1].sample_1") != null : "No cat[1].sample_ in results:" + res.keySet();
        assert res.get("cat[1]+1.sample_2") != null : "No cat[1]+1.sample_2 in results:" + res.keySet();

        shell.shutdown();
    }

    @Test
    public void testOldRGradientDescent() throws Exception {
        System.err.println("+++++++++++++++++++++++++ testOldRGradientDescent");
        Funz.setVerbosity(3);

        HashMap<String, String> variable_bounds = new HashMap<String, String>();
        variable_bounds.put("x1", "["+mult_x1_min+","+mult_x1_max+"]");
        variable_bounds.put("x2", "["+mult_x2_min+","+mult_x2_max+"]");
        DesignShell_v1 shell = new DesignShell_v1(mult, "oldgradientdescent", variable_bounds, null);
        shell.setArchiveDirectory(newTmpDir("testOldRGradientDescent"));
        shell.setDesignOption("nmax", "10");

        shell.startComputationAndWait();

        Map<String, Object[]> res = shell.getResultsArrayMap();

        assert res.get("state") != null : "Status null:" + res;
        assert res.get("state").length == 1 : "Status: '" + ASCII.cat(",", res.get("state")) + "'";
        assert res.get("state")[0].toString().contains("Design over") : "Status: '" + res.get("state")[0] + "'";

        assert ASCII.cat("\n", res.get("analysis")).contains("minimum is ") : "No convergence :" + ASCII.cat("\n", res.get("analysis"));
        double min_found = Double.parseDouble(Parser.after(ASCII.cat("\n", res.get("analysis")),"minimum is ").trim().substring(0,4));
        assert min_found >= mult_min: "Wrong convergence :" + ASCII.cat("\n", res.get("analysis"));
        
        System.err.println(ArrayMapToMDString(shell.getResultsStringArrayMap()));

        shell.shutdown();
    }

    @Test
    public void testOldREGO() throws Exception {
        System.err.println("+++++++++++++++++++++++++ testOldREGO");
        if (!RMathExpression.GetEngineName().contains("Rserve")) {System.err.println("Not using Rserve, so skipping test"); return;} // Do not run if using Renjin or R2js...
        
        Log.setCollector(new LogFile("testOldREGO.log"));
        Funz.setVerbosity(3);

        HashMap<String, String> variable_bounds = new HashMap<String, String>();
        variable_bounds.put("x1", "[0,1]");
        variable_bounds.put("x2", "[0,1]");
        DesignShell_v1 shell = new DesignShell_v1(branin, "oldEGO", variable_bounds, newMap("iterations", "15"));
        shell.setArchiveDirectory(newTmpDir("testOldREGO"));

        assert shell.startComputationAndWait();
        assert shell.stopComputation();

        Map<String, Object[]> res = shell.getResultsArrayMap();

        assert res.get("state") != null : "Status null";
        assert res.get("state").length == 1 : "Status: '" + ASCII.cat(",", res.get("state")) + "'";
        assert res.get("state")[0].toString().contains("Design over") : "Status: '" + res.get("state")[0] + "'";

        assert ASCII.cat("\n", res.get("analysis")).contains("minimum is ") : "No convergence :" + ASCII.cat("\n", res.get("analysis"));
        double min_found = Double.parseDouble(Parser.after(ASCII.cat("\n", res.get("analysis")),"minimum is ").trim().substring(0,3));
        assert min_found <= branin_min: "Wrong convergence :" + org.apache.commons.io.FileUtils.readFileToString(new File("testOldREGO.log"));

        System.err.println(ArrayMapToMDString(shell.getResultsStringArrayMap()));

        shell.shutdown();
    }

    @Test
    public void testREGO() throws Exception {
        System.err.println("+++++++++++++++++++++++++ testREGO");
        if (!RMathExpression.GetEngineName().contains("Rserve")) {System.err.println("Not using Rserve, so skipping test"); return;} // Do not run if using Renjin or R2js...

        Funz.setVerbosity(3);
        Log.setCollector(new LogFile("testREGO.log"));

        HashMap<String, String> variable_bounds = new HashMap<String, String>();
        variable_bounds.put("x1", "[0,1]");
        variable_bounds.put("x2", "[0,1]");
        DesignShell_v1 shell = new DesignShell_v1(branin, "EGO", variable_bounds, newMap("iterations", "15"));
        shell.setArchiveDirectory(newTmpDir("testREGO"));

        assert shell.startComputationAndWait();
        assert shell.stopComputation();

        Map<String, Object[]> res = shell.getResultsArrayMap();

        shell.shutdown();

        assert res.get("state") != null : "Status null: res=" + res;
        assert res.get("state").length == 1 : "Status: '" + ASCII.cat(",", res.get("state")) + "'";
        assert res.get("state")[0].toString().contains("Design over") : "Status: '" + res.get("state")[0] + "'";

        assert ASCII.cat("\n", res.get("analysis")).contains("minimum is ") : "No convergence :" + ASCII.cat("\n", res.get("analysis"));
        double min_found = Double.parseDouble(Parser.after(ASCII.cat("\n", res.get("analysis")),"minimum is ").trim().substring(0,3));
        assert min_found <= branin_min: "Wrong convergence :" + org.apache.commons.io.FileUtils.readFileToString(new File("testREGO.log"));
        
        System.out.println("analysis:\n" + ASCII.cat("\n", res.get("analysis")));

        shell.shutdown();
    }

    @Test
    public void testMoveProject() throws Exception {
        System.err.println("+++++++++++++++++++++++++ testMoveProject");
        Funz.setVerbosity(3);
        Log.setCollector(new LogFile("testMoveProject.log"));

        HashMap<String, String> variable_bounds = new HashMap<String, String>();
        variable_bounds.put("x1", "[0,1]");
        variable_bounds.put("x2", "[0,1]");
        DesignShell_v1 shell = new DesignShell_v1(branin, "GradientDescent", variable_bounds, null);

        File bad_res = newTmpDir("testMoveProject_gradientdescent.res");
        if (bad_res.exists()) {
            Disk.removeDir(bad_res);
            assert !bad_res.exists() : "Could not delete " + bad_res;
        }
        File good_res = newTmpDir("testMoveProject_gradientdescent.res.good");
        if (good_res.exists()) {
            Disk.removeDir(good_res);
            assert !good_res.exists() : "Could not delete " + good_res;
        }
        shell.setArchiveDirectory(good_res);
        shell.redirectOutErr();

        shell.setDesignOption("nmax", "3");

        shell.startComputationAndWait();
        //System.err.println(ArrayMapToMDString(shell.getDataAsArray()));

        shell.setProjectDir(good_res);

        assert !bad_res.exists() : "Used the old archive dir";
        assert good_res.isDirectory() : "Did not created the defined archive dir";

        assert good_res.listFiles(new FileFilter() {

            public boolean accept(File file) {
                return file.getName().endsWith("project.xml");
            }
        }).length == 1 : "Did not used the defined archive dir";

        assert good_res.listFiles(new FileFilter() {

            public boolean accept(File file) {
                return file.getName().endsWith(".out");
            }
        }).length == 1 : "Did not built the error stream in the defined archive dir";

        shell.shutdown();
    }

    // @Test
    /*public void testGradientDescentWithCache() throws Exception {
     Funz.setVerbosity(3);

     HashMap<String, String> variable_bounds = new HashMap<String, String>();
     variable_bounds.put("x1", "[0,1]");
     variable_bounds.put("x2", "[0,1]");

     DesignShell_v1 shell_nocache = new DesignShell_v1(branin,"gradientdescent", variable_bounds);
     shell_nocache.setDesignOption("nmax", "10");

     DesignShell_v1 shell_cache = new DesignShell_v1(branin,"gradientdescent", variable_bounds);
     shell_cache.setDesignOption("nmax", "10");
     shell_cache.setCacheExperiments(true);

     Map<String, String[]> X_nocache = shell_nocache.initDesign();
     Map<String, String[]> X_cache = shell_cache.initDesign();

     assert Utils.ArrayMapToMDString(X_nocache).equals(Utils.ArrayMapToMDString(X_cache));

     boolean end = false;
     while (!end) {
     X_nocache = shell_nocache.nextDesign(branin(X_nocache));
     X_cache = shell_cache.nextDesign(branin(X_cache));
     if (X_nocache == null && X_cache == null) {
     end = true;
     } else if (X_nocache == null) {
     assert false : "X_nocache == null";
     } else if (X_cache == null) {
     assert false : "X_cache == null";
     } else {
     if (!Utils.ArrayMapToMDString(X_nocache).equals(Utils.ArrayMapToMDString(X_cache))) {
     System.err.println("Cache activated :\n" + Utils.ArrayMapToMDString(X_nocache) + " !=\n" + Utils.ArrayMapToMDString(X_cache));
     }
     }
     }

     Map<String, String> analyse_cache = shell_cache.analyseDesign();
     System.err.println(analyse_cache);

     Map<String, String> analyse_nocache = shell_nocache.analyseDesign();
     System.err.println(analyse_nocache);

     for (String k : analyse_cache.keySet()) {
     assert analyse_cache.get(k).equals(analyse_nocache.get(k)) : "Difference in analyse for key " + k + "\n" + analyse_cache.get(k) + "\n != \n" + analyse_nocache.get(k);
     }
     }*/
//    static String[] branin(Map<String, String[]> X) {
//        String[] Y = new String[X.get("x1").length];
//        for (int i = 0; i < Y.length; i++) {
//            double y = 0;
//            for (String x : X.keySet()) {
//                y = y + Double.parseDouble(X.get(x)[i]);
//            }
//            Y[i] = "" + y;
//        }
//        System.err.println(Utils.ArrayMapToMDString(X) + "------>\n" + ASCII.cat("\n", Y));
//        return Y;
//    }
    @Test
    public void testError() throws Exception {
        System.err.println("+++++++++++++++++++++++++ testError");
        Funz.setVerbosity(3);
        HashMap<String, String> variable_bounds = new HashMap<String, String>();
        variable_bounds.put("x1", "["+mult_x1_min+","+mult_x1_max+"]");
        variable_bounds.put("x2", "["+mult_x2_min+","+mult_x2_max+"]");
        DesignShell_v1 shell = new DesignShell_v1(mult, "GradientDescent", variable_bounds, null);
        shell.setArchiveDirectory(newTmpDir("testError"));
        shell.setDesignOption("nmax", "NaN");
        boolean failed = false;
        try {
            failed = !shell.startComputationAndWait();
        } catch (Exception e) {
            failed = true;
        }
        assert failed : "Did not correclty failed";

        shell.shutdown();
    }
}
