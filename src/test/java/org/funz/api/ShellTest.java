package org.funz.api;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.funz.log.Log;
import org.funz.log.LogFile;
import org.funz.script.RMathExpression;
import org.funz.util.ASCII;
import static org.funz.util.Data.newMap;
import org.funz.util.Disk;
import static org.funz.util.Format.ArrayMapToMDString;
import static org.funz.util.ParserUtils.getASCIIFileContent;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author richet
 */
public class ShellTest extends org.funz.api.TestUtils {

    public static void main(String args[]) throws Exception {
        test(ShellTest.class.getName());
    }

    @Before
    public void setUp() throws Exception {
        super.setUp(ShellTest.class.getName());
    }

    //@Test
    public void testCacheCase() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++++++++++++ testCacheCase");

        test1Design();
        System.err.println("==================================================");

        File tmp_in = mult_in();

        Shell_v1 sac = new Shell_v1(R, tmp_in, "cat", ALGORITHM, newMap("x1", "[-0.5,-0.1]", "x2", "[0.3,0.8]"), newMap("nmax", "3")); // R should be dedected by plugin automatically.
        Funz.setVerbosity(verbose);

        sac.addCacheDirectory(new File(tmp_in.getParentFile(), "test1Design"));

        assert Arrays.asList(sac.getInputVariables()).contains("x1") : "Variable x1 not detected";
        assert Arrays.asList(sac.getInputVariables()).contains("x2") : "Variable x2 not detected";

        sac.setInputVariable("x2", new String[]{"0.1"});
        sac.setOutputExpressions("cat");
        sac.startComputationAndWait();
        Map<String, String[]> results = sac.getResultsStringArrayMap();
        System.err.println(ArrayMapToMDString(results));

        sac.shutdown();
    }

    //@Test
    public void testDefaultCase() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++++++++++++ testDefaultCase");

        File tmp_in = mult_in();

        Shell_v1 sac0 = new Shell_v1(R, tmp_in, "cat", null, null, null); // R should be dedected by plugin automatically.
        Funz.setVerbosity(verbose);

        assert Arrays.asList(sac0.getInputVariables()).contains("x1") : "Variable x1 not detected";
        assert Arrays.asList(sac0.getInputVariables()).contains("x2") : "Variable x2 not detected";

        assert sac0.getProject().getVariableByName("x1").getDefaultValue() == null : "Found x1 default value: " + sac0.getProject().getVariableByName("x1").getDefaultValue();
        assert sac0.getProject().getVariableByName("x2").getDefaultValue().equals(".5") : "Did not found x2 default value.";

        assert !sac0.startComputationAndWait() : "Started without cases !";

        Map<String, String[]> results0 = sac0.getResultsStringArrayMap();
        assert results0 == null || !ArrayMapToMDString(results0).contains("cat") : "Not empty results: " + ArrayMapToMDString(results0);

        String s = getASCIIFileContent(tmp_in);
        s = s.replace("?x1", "?[x1~-.3]");
        System.err.println("s " + s);
        ASCII.saveFile(tmp_in, s);

        Shell_v1 sac = new Shell_v1(R, tmp_in, "cat", null, null, null); // R should be dedected by plugin automatically.
        Funz.setVerbosity(verbose);

        assert Arrays.asList(sac.getInputVariables()).contains("x1") : "Variable x1 not detected";
        assert Arrays.asList(sac.getInputVariables()).contains("x2") : "Variable x2 not detected";

        assert sac.getProject().getVariableByName("x1").getDefaultValue().equals("-.3") : "Did not found x1 default value.";
        assert sac.getProject().getVariableByName("x2").getDefaultValue().equals(".5") : "Did not found x2 default value.";

        assert sac.startComputationAndWait() : "Failed to run with default cases: " + sac.getState();

        Map<String, String[]> results = sac.getResultsStringArrayMap();
        System.err.println(ArrayMapToMDString(results));
        assert ArrayMapToMDString(results).contains("cat") : "Empty results: " + ArrayMapToMDString(results);
        System.err.println(sac.getProject().getCaseResultDir(0));

        sac.shutdown();
    }

    private static final String ALGORITHM = "GradientDescent";

    //@Test
    public void test1Case1Design() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++++++++++++ test1Case1Design");

        File tmp_in = mult_in();

        Shell_v1 sac = new Shell_v1(R, tmp_in, "cat", ALGORITHM, newMap("x1", "[-0.5,-0.1]", "x2", new String[]{"0.1"}), newMap("nmax", "15")); // R should be dedected by plugin automatically.
        sac.setArchiveDirectory(newTmpDir("test1Case1Design"));
        Funz.setVerbosity(verbose);

        //sac.addCacheDirectory(new File(mult_in.getParentFile(), "mult.R.res"));
        assert Arrays.asList(sac.getInputVariables()).contains("x1") : "Variable x1 not detected";
        assert Arrays.asList(sac.getInputVariables()).contains("x2") : "Variable x2 not detected";

        //sac.setInputVariable("x2", new String[]{"0.1"});
        sac.startComputationAndWait();
        Map<String, String[]> results = sac.getResultsStringArrayMap();

        assert Double.parseDouble(results.get("min")[0].trim()) == -0.05 : "Bad convergence to " + results.get("min")[0];

        sac.shutdown();
    }

    //@Test
    public void test1Design() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++++++++++++ test1Design");

        File tmp_in = mult_in();

        System.err.println( newMap("x1", "[-0.5,-0.1]", "x2", "[0.3,0.8]"));
        Shell_v1 sac = new Shell_v1(R, tmp_in, "cat", ALGORITHM, newMap("x1", "[-0.5,-0.1]", "x2", "[0.3,0.8]"), newMap("nmax", "3")); // R should be dedected by plugin automatically.
        sac.setArchiveDirectory(newTmpDir("test1Design"));
        Funz.setVerbosity(10);

        //sac.addCacheDirectory(new File(mult_in.getParentFile(), "mult.R.res"));
        assert Arrays.asList(sac.getInputVariables()).contains("x1") : "Variable x1 not detected";
        assert Arrays.asList(sac.getInputVariables()).contains("x2") : "Variable x2 not detected";

        sac.startComputationAndWait();
        Map<String, String[]> results = sac.getResultsStringArrayMap();
        sac.shutdown();

        System.err.println(sac.getState());
        System.err.println(ArrayMapToMDString(results));

        assert results != null : "No results";
        assert results.get("min") != null : "No min in results:" + results.keySet();
        assert results.get("min").length > 0 : "No content min in results";
        assert results.get("min")[0].trim().equals("-0.4") : "Bad convergence to " + results.get("min")[0];
    }

    //@Test
    public void test1DesignOutExpr() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++++++++++++ test1DesignOutExpr");
        if (!RMathExpression.GetEngineName().contains("Rserve")) {
            System.err.println("Not using Rserve, so skipping test");
            return;
        } // Do not run if using Renjin or R2js...

        File tmp_in = branin_in();

        Shell_v1 sac = new Shell_v1(R, tmp_in, "cat", "EGO", newMap("x1", "[0,1]", "x2", "[0,1]"), newMap("iterations", "10"));
        sac.setArchiveDirectory(newTmpDir("test1DesignOutExpr"));
        Funz.setVerbosity(verbose);

        sac.setOutputExpression("GaussianDensity:cat[1],1");

        //sac.addCacheDirectory(new File(mult_in.getParentFile(), "mult.R.res"));
        assert Arrays.asList(sac.getInputVariables()).contains("x1") : "Variable x1 not detected";
        assert Arrays.asList(sac.getInputVariables()).contains("x2") : "Variable x2 not detected";

        sac.startComputationAndWait();
        Map<String, String[]> results = sac.getResultsStringArrayMap();
        sac.shutdown();

        System.err.println(sac.getState());
        System.err.println(ArrayMapToMDString(results));

        assert results != null : "No results";
        assert results.get("min") != null : "No min in results:" + results.keySet();
        assert results.get("min").length > 0 : "No content min in results";
        assert results.get("min")[0].trim().startsWith("0.") : "Bad convergence to " + results.get("min")[0];
    }

    //@Test
    public void testNoDesignOutExpr() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++++++++++++ testNoDesignOutExpr");

        File tmp_in = branin_in();

        Shell_v1 sac = new Shell_v1(R, tmp_in, "cat", null, newMap("x1", new String[]{"0", "1"}, "x2", new String[]{"0", "1"}), null);
        Funz.setVerbosity(verbose);
        sac.setArchiveDirectory(newTmpDir("testNoDesignOutExpr"));
        sac.setOutputExpressions("GaussianDensity:cat[1],1");
        //sac.setOutputExpressions("cat[1]");

        //sac.addCacheDirectory(new File(mult_in.getParentFile(), "mult.R.res"));
        assert Arrays.asList(sac.getInputVariables()).contains("x1") : "Variable x1 not detected";
        assert Arrays.asList(sac.getInputVariables()).contains("x2") : "Variable x2 not detected";

        sac.startComputationAndWait();
        Map<String, String[]> results = sac.getResultsStringArrayMap();
        sac.shutdown();
        System.err.println(ArrayMapToMDString(results));
        assert results.get("N(cat[1],1)")[0].contains("[305.9563,1") : "Bad eval for N(cat[1],1) : \n" + ArrayMapToMDString(results);
    }

    //@Test
    public void testNDesign() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++++++++++++ testNDesign");

        File tmp_in = mult_in();

        Shell_v1 sac = new Shell_v1(R, tmp_in, "cat", ALGORITHM, newMap("x1", "[-0.5,-0.1]"), newMap("nmax", "3", "delta", "1")); // R should be dedected by plugin automatically.
        sac.setArchiveDirectory(newTmpDir("testNDesign"));
        Funz.setVerbosity(verbose);

        //sac.addCacheDirectory(new File(mult_in.getParentFile(), "mult.R.res"));
        assert Arrays.asList(sac.getInputVariables()).contains("x1") : "Variable x1 not detected";
        assert Arrays.asList(sac.getInputVariables()).contains("x2") : "Variable x2 not detected";

        sac.setInputVariable("x2", new String[]{"0.3", "0.8"});

        sac.startComputationAndWait();
        Map<String, String[]> results = sac.getResultsStringArrayMap();
        //System.err.println(ArrayMapToMDString(results));
        sac.shutdown();

        //System.err.println(ASCII.cat(",",results.get("min")));
        assert Double.parseDouble(results.get("min")[0].trim()) < 0 : "0: Bad convergence to " + results.get("min")[0];
        assert Double.parseDouble(results.get("min")[1].trim()) < 0 : "1: Bad convergence to " + results.get("min")[1];
    }

    //@Test
    public void test1Case0Design() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++++++++++++ test1Case0Design");

        File tmp_in = mult_in();

        Shell_v1 sac = new Shell_v1(R, tmp_in, "cat", null, null, null); // R should be dedected by plugin automatically.
        sac.setArchiveDirectory(newTmpDir("test1Case0Design"));
        Funz.setVerbosity(verbose);

        //sac.addCacheDirectory(new File(mult_in.getParentFile(), "mult.R.res"));
        assert Arrays.asList(sac.getInputVariables()).contains("x1") : "Variable x1 not detected";
        assert Arrays.asList(sac.getInputVariables()).contains("x2") : "Variable x2 not detected";

        sac.setInputVariable("x1", new String[]{"-0.1"});
        sac.setInputVariable("x2", new String[]{"0.1"});

        sac.startComputationAndWait();
        Map<String, String[]> results = sac.getResultsStringArrayMap();

        sac.shutdown();

        assert results.get("cat")[0].equals("[-0.01]") : "Bad output:" + results.get("cat") + "\n" + ArrayMapToMDString(results);
    }

    //@Test
    public void testNCasesNoDesign() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++++++++++++ testNCasesNoDesign");

        File tmp_in = mult_in();

        Shell_v1 sac = new Shell_v1(R, tmp_in, "cat", null, null, null); // R should be dedected by plugin automatically.
        sac.setArchiveDirectory(newTmpDir("testNCasesNoDesign"));
        Funz.setVerbosity(verbose);

        //sac.addCacheDirectory(new File(mult_in.getParentFile(), "mult.R.res"));
        assert Arrays.asList(sac.getInputVariables()).contains("x1") : "Variable x1 not detected";
        assert Arrays.asList(sac.getInputVariables()).contains("x2") : "Variable x2 not detected";

        sac.setInputVariable("x1", new double[]{-0.1, -.2, -.3});
        sac.setInputVariable("x2", new String[]{"0.1", ".2", ".3"});

        sac.startComputationAndWait();
        Map<String, String[]> results = sac.getResultsStringArrayMap();
        System.err.println(ArrayMapToMDString(results));

        sac.shutdown();
    }

    //@Test
    public void testMoveProject() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++++++++++++ testMoveProject");
        Log.setCollector(new LogFile("testMoveProject.log"));

        File tmp_in = mult_in();

        Shell_v1 sac = new Shell_v1(R, tmp_in, "cat", ALGORITHM, newMap("x1", "[-0.5,-0.3]"), newMap("nmax", "3")); // R should be dedected by plugin automatically.
        Funz.setVerbosity(verbose);

        File bad_res = new File("tmp"+File.separator+"mult.R.res");
        if (bad_res.exists()) {
            Disk.removeDir(bad_res);
            assert !bad_res.exists() : "Could not delete " + bad_res;
        }
        sac.setArchiveDirectory(bad_res);

        File good_res = new File("tmp"+File.separator+"good/mult.R.res.good");
        if (good_res.exists()) {
            Disk.removeDir(good_res);
            assert !good_res.exists() : "Could not delete " + good_res;
        }
        sac.setArchiveDirectory(good_res);
        sac.redirectOutErr();

        assert Arrays.asList(sac.getInputVariables()).contains("x1") : "Variable x1 not detected";
        assert Arrays.asList(sac.getInputVariables()).contains("x2") : "Variable x2 not detected";

        sac.startComputationAndWait();

        Map<String, String[]> results = sac.getResultsStringArrayMap();
        System.err.println(ArrayMapToMDString(results));

        sac.setProjectDir(good_res);
        sac.shutdown();

        assert !bad_res.exists() : "Used the old archive dir";
        assert good_res.isDirectory() : "Did not created the defined archive dir";
        System.err.println("good_res.listFiles(): "+good_res.listFiles());
        assert good_res.listFiles(new FileFilter() {

            public boolean accept(File file) {
                return file.getName().endsWith("project.xml");
            }
        }).length == 1 : "Did not used the defined archive dir";
    }

    public void testKill() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++++++++++++ testKill");

        File tmp_in = mult_in();

        final Shell_v1 sac = new Shell_v1(R, tmp_in, "cat", ALGORITHM, newMap("x1", "[-0.5,-0.1]", "x2", "[0.3,0.8]"), newMap("nmax", "5"));
        //RunShell_v1 sac = new RunShell_v1(R, mult_in); // R should be dedected by plugin automatically.
        Funz.setVerbosity(verbose);

        assert Arrays.asList(sac.getInputVariables()).contains("x1") : "Variable x1 not detected";
        assert Arrays.asList(sac.getInputVariables()).contains("x2") : "Variable x2 not detected";

        Thread t = new Thread(new Runnable() {

            public void run() {
                try {
                    /*assert*/ sac.startComputationAndWait() /*: "!!!"*/;

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        t.start();

        Thread.sleep(10000);
        System.err.println("KILL !!!!!!!!!!!!!!!!!!!!!!!!!!");
        assert sac.stopComputation() : "Could not break shell running.";

        t.join();

        System.err.println(ArrayMapToMDString(sac.getResultsStringArrayMap()));

        sac.shutdown();
    }

    //@Test
    public void testConcurrency() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++++++++++++ testConcurrency");
        boolean[] tests = new boolean[]{false, false, false, false};
        boolean[] done = new boolean[tests.length];
        Thread[] ts = new Thread[tests.length];
        for (int i = 0; i < tests.length; i++) {
            done[i] = false;
        }
        Funz.setVerbosity(verbose);
        for (int i = 0; i < tests.length; i++) {
            final int I = i;
            ts[i] = new Thread(new Runnable() {

                public void run() {
                    final File tmp_in = new File("tmp"+File.separator+"mult." + I + ".R");
                    if (tmp_in.exists()) {
                        tmp_in.delete();
                    }
                    while (true) {
                        try {
                            Disk.copyFile(new File("src"+File.separator+"test"+File.separator+"samples","mult.R"), tmp_in);
                            break;
                        } catch (Exception e) {
                            System.err.println("Retrying initialization of test ...");
                        }
                    }
                    try {
                        final Shell_v1 sac = new Shell_v1(R, tmp_in, "cat", ALGORITHM, newMap("x1", "[-0.5,-0.1]", "x2", "[0.3,0.8]"), newMap("nmax", "3"));

                        assert Arrays.asList(sac.getInputVariables()).contains("x1") : "Variable x1 not detected";
                        assert Arrays.asList(sac.getInputVariables()).contains("x2") : "Variable x2 not detected";

                        sac.setArchiveDirectory(newTmpDir("testConcurrency."+I));

                        sac.startComputationAndWait();

                        Map<String, String[]> results = sac.getResultsStringArrayMap();
                        assert results != null && !results.isEmpty() : "Empty results!";

                        synchronized (tests) {
                            tests[I] = true;
                            done[I] = true;
                            tests.notifyAll();
                        }
                        sac.shutdown();
                    } catch (Exception e) {
                        System.err.println("ERROR shell " + I + " \n" + e.getLocalizedMessage());
                        e.printStackTrace();
                        synchronized (tests) {
                            tests[I] = false;
                            done[I] = true;
                            tests.notifyAll();
                        }
                    } catch (AssertionError e) {
                        System.err.println("FAILED shell " + I + " \n" + e.getLocalizedMessage());
                        synchronized (tests) {
                            tests[I] = false;
                            done[I] = true;
                            tests.notifyAll();
                        }
                    }
                }
            });
            ts[i].start();
        }

        boolean  alldone = false;
        while (!alldone) {
            synchronized (tests) {
                alldone = alltrue(done); 
                tests.notifyAll();  
            }   
            Thread.sleep(1000);
            System.err.println("done: "+Arrays.toString(done)+" tests: "+Arrays.toString(tests));
        }

        assert alltrue(tests) : "One concurency run failed !";

        for (int i = 0; i < tests.length; i++) {
            ts[i].interrupt();
            synchronized (tests) {
                tests.notifyAll();
            }
            ts[i].join();
        }
    }

    //@Test
    public void testDuplicateCases() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++++++++++++ testDuplicateCases");

        File tmp_in = branin_in();

        Shell_v1 sac = new Shell_v1(R, tmp_in, "cat", null, null, null);
        Funz.setVerbosity(verbose);

        assert Arrays.asList(sac.getInputVariables()).contains("x1") : "Variable x1 not detected";
        assert Arrays.asList(sac.getInputVariables()).contains("x2") : "Variable x2 not detected";

        HashMap<String, String[]> X = new HashMap<String, String[]>();
        String[] x1 = new String[]{"0.1", "0.2", "0.1", "0.1"};
        X.put("x1", x1);
        String[] x2 = new String[]{"0.1", "0.2", "0.1", "0.1"};
        X.put("x2", x2);
        sac.setInputVariablesGroup("X", X);
        sac.setOutputExpressions("cat");
        sac.startComputationAndWait();
        Map<String, Object[]> results = sac.getResultsArrayMap();
        for (String r : results.keySet()) {
            System.out.println(r + "\n  " + ASCII.cat(",", results.get(r)));
        }

        assert Arrays.deepEquals(results.get("x1"), x1) : Arrays.toString(results.get("x1")) + " != " + Arrays.toString(x1);
        assert Arrays.deepEquals(results.get("x2"), x2) : Arrays.toString(results.get("x2")) + " != " + Arrays.toString(x2);
        String[] z = branin(x1, x2);//new String[]{"136.0767", "50.75626", "136.0767", "136.0767"};
        assert Arrays.deepEquals(round2(results.get("cat")), z) : Arrays.toString(round2(results.get("cat"))) + " != " + Arrays.toString(z);

        sac.shutdown();
    }

    //@Test
    public void testVectorize() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++++++++++++ testVectorize");

        File tmp_in = branin_in();

        Shell_v1 sac = new Shell_v1(R, tmp_in, "cat", null, null, null);
        Funz.setVerbosity(verbose);

        assert Arrays.asList(sac.getInputVariables()).contains("x1") : "Variable x1 not detected";
        assert Arrays.asList(sac.getInputVariables()).contains("x2") : "Variable x2 not detected";

        HashMap<String, String[]> X = new HashMap<String, String[]>();
        String[] x1 = new String[]{"0.1", "0.2", "0.3"};
        X.put("x1", x1);
        String[] x2 = new String[]{"0.1", "0.2", "0.3"};
        X.put("x2", x2);
        sac.setInputVariablesGroup("X", X);
        sac.setOutputExpressions("cat");
        sac.startComputationAndWait();
        Map<String, Object[]> results = sac.getResultsArrayMap();
        for (String r : results.keySet()) {
            System.out.println(r + "\n  " + ASCII.cat(",", results.get(r)));
        }

        assert Arrays.deepEquals(results.get("x1"), x1) : Arrays.toString(results.get("x1")) + " != " + Arrays.toString(x1);
        assert Arrays.deepEquals(results.get("x2"), x2) : Arrays.toString(results.get("x2")) + " != " + Arrays.toString(x2);
        String[] z = branin(x1, x2);//new String[]{"214.8081", "167.0327", "146.7375"};
        assert Arrays.deepEquals(round2(results.get("cat")), z) : Arrays.toString(round2(results.get("cat"))) + " != " + Arrays.toString(z);

        sac.shutdown();
    }

    @Test
    public void testManyOutput() throws Exception {
        System.err.println("+++++++++++++++++++++++++ testManyOutput");

        File tmp_in = mult_in();

        System.err.println( newMap("x1", "[-0.5,-0.1]", "x2", "[0.3,0.8]"));
        Shell_v1 sac = new Shell_v1(R, tmp_in, "(cat[1],cat[1]+1)", "RandomUnif", newMap("x1", "[-0.5,-0.1]", "x2", "[0.3,0.8]"), newMap("sample_size", "10")); // R should be dedected by plugin automatically.
        sac.setArchiveDirectory(newTmpDir("testManyOutput"));
        Funz.setVerbosity(10);

        //sac.addCacheDirectory(new File(mult_in.getParentFile(), "mult.R.res"));
        assert Arrays.asList(sac.getInputVariables()).contains("x1") : "Variable x1 not detected";
        assert Arrays.asList(sac.getInputVariables()).contains("x2") : "Variable x2 not detected";

        sac.startComputationAndWait();
        Map<String, String[]> results = sac.getResultsStringArrayMap();
        sac.shutdown();

        System.err.println(sac.getState());
        System.err.println(ArrayMapToMDString(results));

        assert results != null : "No results";

<<<<<<< HEAD
        assert results.get("sample_cat") != null : "No sample_cat in results:" + results.keySet();
        assert results.get("sample_cat+1") != null : "No sample_cat in results:" + results.keySet();
=======
        assert results.get("cat[1].sample_1") != null : "No cat[1].sample_1 in results:" + results.keySet();
        assert results.get("cat[1]+1.sample_2") != null : "No cat[1]+1.sample_2 in results:" + results.keySet();
    }

    @Test
    public void testStringOutput() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++++++++++++ testStringOutput");

        File tmp_in = mult_in();

        Shell_v1 sac = new Shell_v1(R, tmp_in, "catstr", null, null, null); // R should be dedected by plugin automatically.
        sac.setArchiveDirectory(newTmpDir("testStringOutput"));
        Funz.setVerbosity(verbose);

        //sac.addCacheDirectory(new File(mult_in.getParentFile(), "mult.R.res"));
        assert Arrays.asList(sac.getInputVariables()).contains("x1") : "Variable x1 not detected";
        assert Arrays.asList(sac.getInputVariables()).contains("x2") : "Variable x2 not detected";

        sac.setInputVariable("x1", new String[]{"-0.1"});
        sac.setInputVariable("x2", new String[]{"0.1"});

        sac.startComputationAndWait();
        Map<String, String[]> results = sac.getResultsStringArrayMap();

        sac.shutdown();
        assert results.containsKey("catstr") : "No output catstr in "+ "\n" + ArrayMapToMDString(results);
        assert results.get("catstr")!=null : "Null output catstr in "+ "\n" + ArrayMapToMDString(results);
        assert results.get("catstr").length>0 : "Empty output catstr in "+ "\n" + ArrayMapToMDString(results);
        assert results.get("catstr")[0]!=null && results.get("catstr")[0].equals("[-0.01]") : "Bad output:" + Arrays.toString(results.get("catstr")) + "\n" + ArrayMapToMDString(results);
   
        System.err.println(ArrayMapToMDString(results));
>>>>>>> 40c1d883ca2dbdd9cabb287b207e7ecd44a88aee
    }
}
