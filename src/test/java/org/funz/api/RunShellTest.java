package org.funz.api;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.exec.OS;
import org.apache.commons.io.FileUtils;
import org.funz.Project;
import org.funz.util.ASCII;
import org.funz.util.Disk;
import static org.funz.util.Format.ArrayMapToMDString;
import org.funz.util.ParserUtils;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author richet
 */
public class RunShellTest extends org.funz.api.TestUtils {

    public static void main(String args[]) throws Exception {
        test(RunShellTest.class.getName());
    }

    @Before
    public void setUp() throws Exception {
        super.setUp(RunShellTest.class.getName());
    }

    @Test
    public void testExitNot0() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++ testExitNot0");

        if (OS.isFamilyWindows()) {
            File calc = new File("dist", "calculator.xml");
            ASCII.saveFile(calc, FileUtils.readFileToString(calc).replace("exit-1.sh", "exit-1.bat"));
            File calcfail = new File("dist", "calculator.fail.xml");
            ASCII.saveFile(calcfail, FileUtils.readFileToString(calcfail).replace("exit-1.sh", "exit-1.bat"));
        }

        File tmp_in = newTmpFile("exit.dat");
        if (tmp_in.exists()) {
            tmp_in.delete();
        }
        ASCII.saveFile(tmp_in, "exit");

        RunShell_v1 sac = new RunShell_v1("ExitError", tmp_in, (String) null);
        Funz.setVerbosity(verbose);
        sac.setArchiveDirectory(newTmpDir("testExitNot0"));

        assert sac.startComputationAndWait() : "Batch failed !";
        Map<String, Object[]> results = sac.getResultsArrayMap();
        for (String r : results.keySet()) {
            System.out.println(r + " (" + results.get(r).getClass() + ")\n  " + ASCII.cat(",", results.get(r)));
        }

        assert results.containsKey("info") : "No 'info' in results";
        assert results.get("info").length == 1 : "Bad length of 'info'";
        assert results.get("info")[0].toString().contains("failed") : "Did not failed properly !";

        Thread.sleep(1000);

        sac.shutdown();
    }

    @Test
    public void testCrash() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++ testCrash");

        if (OS.isFamilyWindows()) {
            File calc = new File("dist", "calculator.xml");
            ASCII.saveFile(calc, FileUtils.readFileToString(calc).replace("crash.sh", "crash.bat"));
            File calcfail = new File("dist", "calculator.fail.xml");
            ASCII.saveFile(calcfail, FileUtils.readFileToString(calcfail).replace("crash.sh", "crash.bat"));
        }

        File tmp_in = newTmpFile("crash.dat");
        if (tmp_in.exists()) {
            tmp_in.delete();
        }
        ASCII.saveFile(tmp_in, "crash");

        RunShell_v1 sac = new RunShell_v1("Crash", tmp_in, (String) null);
        Funz.setVerbosity(verbose);
        sac.setArchiveDirectory(newTmpDir("testCrash"));

        assert sac.startComputationAndWait() : "Batch failed !";
        Map<String, Object[]> results = sac.getResultsArrayMap();
        for (String r : results.keySet()) {
            System.out.println(r + " (" + results.get(r).getClass() + ")\n  " + ASCII.cat(",", results.get(r)));
        }

        assert results.containsKey("info") : "No 'info' in results";
        assert results.get("info").length == 1 : "Bad length of 'info'";
        assert results.get("info")[0].toString().contains("failed") : "Did not failed properly !";

        assert Arrays.asList(sac.getArchiveDirectory().list()).contains("output") : "No output dir in " + Arrays.asList(sac.getArchiveDirectory().list()) + "\n info:\n" + results.get("info")[0];

        Thread.sleep(1000);

        sac.shutdown();
    }

    @Test
    public void testBadCode() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++ testBadCode");

        File tmp_in = newTmpFile("branin.nop.R");
        if (tmp_in.exists()) {
            tmp_in.delete();
        }
        Disk.copyFile(new File("src/test/samples/branin.nop.R"), tmp_in);

        RunShell_v1 sac = new RunShell_v1("R Crash", tmp_in, (String) null); // R should be dedected by plugin automatically.
        Funz.setVerbosity(verbose);
        sac.setArchiveDirectory(newTmpDir("testBadCode"));

        sac.setOutputExpressions("cat");
        assert sac.startComputationAndWait() : "Batch failed !";
        Map<String, Object[]> results = sac.getResultsArrayMap();
        for (String r : results.keySet()) {
            System.out.println(r + " (" + results.get(r).getClass() + ")\n  " + ASCII.cat(",", results.get(r)));
        }

        assert results.containsKey("info") : "No 'info' in results";
        assert results.get("info").length == 1 : "Bad length of 'info'";
        assert results.get("info")[0].toString().contains("failed") : "Did not failed properly !";

        Thread.sleep(1000);

        sac.shutdown();
    }

    @Test
    public void testImplicitCache() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++ testImplicitCache");

        File tmp = newTmpDir("testImplicitCache");
        assert tmp.list().length == 0 : "Cannot empty tmp !";

        File tmp_in = branin_in();
        ASCII.saveFile(tmp_in, ParserUtils.getASCIIFileContent(tmp_in).replace("t=0", "t=2"));

        Funz.setVerbosity(verbose);

        Project prj = new Project("branin", tmp);
        RunShell_v1 sac = new RunShell_v1(prj);//R, branin_in, (String) null); // R should be dedected by plugin automatically.
        sac.setInputModel(R, tmp_in);
        sac.setArchiveDirectory(sac.prj.getResultsDir());

        assert Arrays.asList(sac.getInputVariables()).contains("x1") : "Variable x1 not detected";
        assert Arrays.asList(sac.getInputVariables()).contains("x2") : "Variable x2 not detected";
        HashMap<String, String[]> X = new HashMap<String, String[]>();
        String[] x1 = new String[]{"0.1"};
        X.put("x1", x1);
        sac.setInputVariablesGroup("X", X);
        String[] x2 = new String[]{"0.1"};
        X.put("x2", x2);
        sac.setInputVariablesGroup("X", X);
        sac.setOutputExpressions("cat");
        sac.startComputationAndWait();
        Map<String, Object[]> results = sac.getResultsArrayMap();

        assert Arrays.deepEquals(results.get("x1"), x1) : Arrays.toString(results.get("x1")) + " != " + Arrays.toString(x1);
        assert Arrays.deepEquals(results.get("x2"), x2) : Arrays.toString(results.get("x2")) + " != " + Arrays.toString(x2);
        String[] z = branin(x1, x2);//new String[]{"136.0767"};
        assert Arrays.deepEquals(round2(results.get("cat")), z) : Arrays.toString(round2(results.get("cat"))) + " != " + Arrays.toString(z);

        assert (double) (results.get("duration")[0]) > 5 : "Too short case run ! (duration was " + results.get("duration")[0] + ")";

        Thread.sleep(1000);
        sac.shutdown();

        Project prj2 = new Project("branin", tmp);
        RunShell_v1 sac2 = new RunShell_v1(prj2);//R, branin_in, (String) null); // R should be dedected by plugin automatically.
        sac2.setInputModel(R, tmp_in);
        sac2.setArchiveDirectory(sac2.prj.getResultsDir());

        assert Arrays.asList(sac2.getInputVariables()).contains("x1") : "Variable x1 not detected";
        assert Arrays.asList(sac2.getInputVariables()).contains("x2") : "Variable x2 not detected";
        sac2.setInputVariablesGroup("X", X);
        sac2.setOutputExpressions("cat");
        sac2.startComputationAndWait();
        Map<String, Object[]> results2 = sac2.getResultsArrayMap();

        assert Arrays.deepEquals(results2.get("x1"), x1) : Arrays.toString(results2.get("x1")) + " != " + Arrays.toString(x1);
        assert Arrays.deepEquals(results2.get("x2"), x2) : Arrays.toString(results2.get("x2")) + " != " + Arrays.toString(x2);
        assert Arrays.deepEquals(round2(results2.get("cat")), z) : Arrays.toString(round2(results2.get("cat"))) + " != " + Arrays.toString(z);

        assert (Double) (results2.get("duration"))[0] < 5 : "Did not used cache ! (duration was " + ((Object[]) (results2.get("duration")))[0] + ")";

        Thread.sleep(1000);
        sac2.shutdown();
    }

    @Test
    public void testMoreCasesCache() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++ testMoreCasesCache");

        File tmp = newTmpDir("testMoreCasesCache");
        assert tmp.list().length == 0 : "Cannot empty tmp !";

        File tmp_in = branin_in();
        ASCII.saveFile(tmp_in, ParserUtils.getASCIIFileContent(new File("src/test/samples/branin.R")).replace("t=0", "t=2"));

        Funz.setVerbosity(verbose);

        Project prj = new Project("branin", tmp);
        RunShell_v1 sac = new RunShell_v1(prj);//R, branin_in, (String) null); // R should be dedected by plugin automatically.
        sac.setInputModel(R, tmp_in);
        sac.setArchiveDirectory(sac.prj.getResultsDir());

        assert Arrays.asList(sac.getInputVariables()).contains("x1") : "Variable x1 not detected";
        assert Arrays.asList(sac.getInputVariables()).contains("x2") : "Variable x2 not detected";
        HashMap<String, String[]> X = new HashMap<String, String[]>();
        String[] x1 = new String[]{"0.1"};
        X.put("x1", x1);
        sac.setInputVariablesGroup("X", X);
        String[] x2 = new String[]{"0.1"};
        X.put("x2", x2);
        sac.setInputVariablesGroup("X", X);
        sac.setOutputExpressions("cat");
        sac.startComputationAndWait();
        Map<String, Object[]> results = sac.getResultsArrayMap();

        assert Arrays.deepEquals(results.get("x1"), x1) : Arrays.toString(results.get("x1")) + " != " + Arrays.toString(x1);
        assert Arrays.deepEquals(results.get("x2"), x2) : Arrays.toString(results.get("x2")) + " != " + Arrays.toString(x2);
        String[] z = branin(x1, x2);//new String[]{"136.0767"};
        assert Arrays.deepEquals(round2(results.get("cat")), z) : Arrays.toString(round2(results.get("cat"))) + " != " + Arrays.toString(z);

        assert (double) (results.get("duration")[0]) > 5 : "Too short case run ! (duration was " + results.get("duration")[0] + ")";

        Thread.sleep(1000);
        sac.shutdown();

        Project prj2 = new Project("branin", tmp);
        RunShell_v1 sac2 = new RunShell_v1(prj2);//R, branin_in, (String) null); // R should be dedected by plugin automatically.
        sac2.setInputModel(R, tmp_in);
        sac2.setArchiveDirectory(sac2.prj.getResultsDir());

        assert Arrays.asList(sac2.getInputVariables()).contains("x1") : "Variable x1 not detected";
        assert Arrays.asList(sac2.getInputVariables()).contains("x2") : "Variable x2 not detected";
        X.put("x1", new String[]{x1[0], "0.2"});
        X.put("x2", new String[]{x2[0], "0.2"});
        sac2.setInputVariablesGroup("X", X);
        sac2.setOutputExpressions("cat");
        sac2.startComputationAndWait();
        Map<String, Object[]> results2 = sac2.getResultsArrayMap();

        System.out.println(Arrays.toString(round2(results2.get("cat"))));
        System.out.println(Arrays.toString(round2(results2.get("duration"))));
        //assert Arrays.deepEquals(results2.get("x1"), x1) : Arrays.toString(results2.get("x1")) + " != " + Arrays.toString(x1);
        //assert Arrays.deepEquals(results2.get("x2"), x2) : Arrays.toString(results2.get("x2")) + " != " + Arrays.toString(x2);
        //assert Arrays.deepEquals(round2(results2.get("cat")), z) : Arrays.toString(round2(results2.get("cat"))) + " != " + Arrays.toString(z);

        assert (Double) (results2.get("duration"))[0] < 5 : "Did not used cache ! (duration was " + ((Double[]) (results2.get("duration")))[0] + ")";
        assert (Double) (results2.get("duration"))[1] > 5 : "Too short case run ! (duration was " + ((Double[]) (results2.get("duration")))[1] + ")";

        Thread.sleep(1000);
        sac2.shutdown();
    }

    @Test
    public void testCacheCase() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++ testCacheCase");

        test1Case();
        File cache = newTmpDir("test1Case.oldtmp");
        Disk.moveDir(new File("tmp", "test1Case.tmp"), cache);
        assert (cache.exists()) : "No cache available !";

        File tmp_in = branin_in();

        RunShell_v1 sac = new RunShell_v1(R, tmp_in, (String) null); // R should be dedected by plugin automatically.
        Funz.setVerbosity(verbose);
        sac.setArchiveDirectory(newTmpDir("testCacheCase"));
        sac.addCacheDirectory(cache);

        assert Arrays.asList(sac.getInputVariables()).contains("x1") : "Variable x1 not detected";
        assert Arrays.asList(sac.getInputVariables()).contains("x2") : "Variable x2 not detected";

        HashMap<String, String[]> X = new HashMap<String, String[]>();
        String[] x1 = new String[]{"0.1"};
        X.put("x1", x1);
        String[] x2 = new String[]{"0.1"};
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
        String[] z = branin(x1, x2);//new String[]{"136.0767"};
        assert Arrays.deepEquals(round2(results.get("cat")), z) : Arrays.toString(round2(results.get("cat"))) + " != " + Arrays.toString(z);

        Thread.sleep(1000);

        sac.shutdown();

        Disk.removeDir(cache);
    }

    @Test
    public void testNoCase() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++ testNoCase");

        File tmp_in = newTmpFile("branin.nop.R");
        if (tmp_in.exists()) {
            tmp_in.delete();
        }
        Disk.copyFile(new File("src/test/samples/branin.nop.R"), tmp_in);

        RunShell_v1 sac = new RunShell_v1(R, tmp_in, (String) null); // R should be dedected by plugin automatically.
        Funz.setVerbosity(verbose);
        sac.setArchiveDirectory(newTmpDir("testNoCase"));

        sac.setOutputExpressions("cat");
        sac.startComputationAndWait();
        Map<String, Object[]> results = sac.getResultsArrayMap();
        for (String r : results.keySet()) {
            System.out.println(r + " (" + results.get(r).getClass() + ")\n  " + ASCII.cat(",", results.get(r)));
        }

        String[] z = new String[]{"136.0"};
        assert Arrays.deepEquals(round2(results.get("cat")), z) : Arrays.toString(round2(results.get("cat"))) + " != " + Arrays.toString(z);

        Thread.sleep(1000);

        sac.shutdown();
    }

    @Test
    public void test1Case() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++ test1Case");

        File tmp_in = branin_in();

        RunShell_v1 sac = new RunShell_v1(R, tmp_in, (String) null); // R should be dedected by plugin automatically.
        Funz.setVerbosity(verbose);
        File archivedir = new File("tmp", "test1Case.tmp");
        Disk.removeDir(archivedir);
        assert !archivedir.exists() : "Cannot cleanup archive dir " + archivedir;
        sac.setArchiveDirectory(archivedir);

        assert Arrays.asList(sac.getInputVariables()).contains("x1") : "Variable x1 not detected";
        assert Arrays.asList(sac.getInputVariables()).contains("x2") : "Variable x2 not detected";
        HashMap<String, String[]> X = new HashMap<String, String[]>();
        String[] x1 = new String[]{"0.1"};
        X.put("x1", x1);
        sac.setInputVariablesGroup("X", X);
        String[] x2 = new String[]{"0.1"};
        X.put("x2", x2);
        sac.setInputVariablesGroup("X", X);
        sac.setOutputExpressions("cat");
        sac.startComputationAndWait();
        Map<String, Object[]> results = sac.getResultsArrayMap();
        for (String r : results.keySet()) {
            System.out.println(r + " (" + results.get(r).getClass() + ")((" + results.get(r)[0].getClass() + "))\n  " + ASCII.cat(",", results.get(r)));
        }

        assert Arrays.deepEquals(results.get("x1"), x1) : Arrays.toString(results.get("x1")) + " != " + Arrays.toString(x1);
        assert Arrays.deepEquals(results.get("x2"), x2) : Arrays.toString(results.get("x2")) + " != " + Arrays.toString(x2);
        String[] z = branin(x1, x2);//new String[]{"136.0767"};
        assert Arrays.deepEquals(round2(results.get("cat")), z) : Arrays.toString(round2(results.get("cat"))) + " != " + Arrays.toString(z);

        assert sac.getArchiveDirectory().listFiles().length > 0 : "Empty archive directory: " + sac.getArchiveDirectory();

        Thread.sleep(1000);

        sac.shutdown();
    }

    @Test
    public void testIOPluginMore() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++ testIOPluginMore");

        File tmp_in = branin_in();
        File tmp_plugin = newTmpFile("getZ.ioplugin");
        Disk.copyFile(new File("src/test/samples/getZ.ioplugin"), tmp_plugin);

        RunShell_v1 sac = new RunShell_v1(R, new File[]{tmp_in, tmp_plugin}, (String) null); // R should be dedected by plugin automatically.
        Funz.setVerbosity(verbose);
        File archivedir = new File("tmp");
        Disk.removeDir(archivedir);
        assert !archivedir.exists() : "Cannot cleanup archive dir " + archivedir;
        sac.setArchiveDirectory(archivedir);

        assert Arrays.asList(sac.getInputVariables()).contains("x1") : "Variable x1 not detected";
        assert Arrays.asList(sac.getInputVariables()).contains("x2") : "Variable x2 not detected";
        HashMap<String, String[]> X = new HashMap<String, String[]>();
        String[] x1 = new String[]{"0.1"};
        X.put("x1", x1);
        sac.setInputVariablesGroup("X", X);
        String[] x2 = new String[]{"0.1"};
        X.put("x2", x2);
        sac.setInputVariablesGroup("X", X);
        sac.setOutputExpressions("cat");
        sac.startComputationAndWait();
        Map<String, Object[]> results = sac.getResultsArrayMap();
        for (String r : results.keySet()) {
            if (results.get(r)!=null && results.get(r).length>0 && results.get(r)[0]!=null)
                System.out.println(r + " (" + results.get(r).getClass() + ")((" + results.get(r)[0].getClass() + "))\n  " + ASCII.cat(",", results.get(r)));
        }

        assert Arrays.deepEquals(results.get("x1"), x1) : Arrays.toString(results.get("x1")) + " != " + Arrays.toString(x1);
        assert Arrays.deepEquals(results.get("x2"), x2) : Arrays.toString(results.get("x2")) + " != " + Arrays.toString(x2);
        String[] z = branin(x1, x2);//new String[]{"136.0767"};
        assert Arrays.deepEquals(round2(results.get("cat")), z) : Arrays.toString(round2(results.get("cat"))) + " != " + Arrays.toString(z);
        assert results.get("Z") != null : "No ioplugin more output parsed !";
        assert ("" + round2(results.get("Z")[0]) + "").equals(z[0]) : "" + round2(results.get("Z")[0]) + "" + " != " + z[0];

        Thread.sleep(1000);

        sac.shutdown();
    }

    @Test
    public void testFailedCase() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++ testFailedCase");

        File tmp_in = branin_in();

        RunShell_v1 sac = new RunShell_v1(R, tmp_in, (String) null); // R should be dedected by plugin automatically.
        Funz.setVerbosity(verbose);
        sac.setArchiveDirectory(newTmpDir("testFailedCase"));

        assert Arrays.asList(sac.getInputVariables()).contains("x1") : "Variable x1 not detected";
        assert Arrays.asList(sac.getInputVariables()).contains("x2") : "Variable x2 not detected";
        HashMap<String, String[]> X = new HashMap<String, String[]>();
        String[] x1 = new String[]{"0.1"};
        X.put("x1", x1);
        sac.setInputVariablesGroup("X", X);
        String[] x2 = new String[]{"0.1a"};
        X.put("x2", x2);
        sac.setInputVariablesGroup("X", X);
        sac.setOutputExpressions("cat");
        sac.startComputationAndWait();

        Map<String, Object[]> results = sac.getResultsArrayMap();

        for (String r : results.keySet()) {
            System.out.println(r + " (" + results.get(r).getClass() + ")\n  " + ASCII.cat(",", results.get(r)));
        }

        assert Arrays.deepEquals(results.get("x1"), x1) : Arrays.toString(results.get("x1")) + " != " + Arrays.toString(x1);
        assert Arrays.deepEquals(results.get("x2"), x2) : Arrays.toString(results.get("x2")) + " != " + Arrays.toString(x2);

        sac.shutdown();
    }

    @Test
    public void testArchive() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++ testArchive");

        File tmp_in = branin_in();

        RunShell_v1 sac = new RunShell_v1(R, tmp_in, (String) null); // R should be detected by plugin automatically.
        Funz.setVerbosity(verbose);

        File bad_res = newTmpDir("testArchive_branin.R.res");
        if (bad_res.exists()) {
            Disk.removeDir(bad_res);
            assert !bad_res.exists() : "Could not delete " + bad_res;
        }
        File good_res = newTmpDir("testArchive_branin.R.res.good");
        if (good_res.exists()) {
            Disk.removeDir(good_res);
            assert !good_res.exists() : "Could not delete " + good_res;
        }
        sac.setArchiveDirectory(good_res);
        sac.redirectOutErr();

        assert Arrays.asList(sac.getInputVariables()).contains("x1") : "Variable x1 not detected";
        assert Arrays.asList(sac.getInputVariables()).contains("x2") : "Variable x2 not detected";

        HashMap<String, String[]> X = new HashMap<String, String[]>();
        String[] x1 = new String[]{"0.1"};
        X.put("x1", x1);
        String[] x2 = new String[]{"0.1"};
        X.put("x2", x2);
        sac.setInputVariablesGroup("X", X);
        sac.setOutputExpressions("cat");
        sac.startComputationAndWait();
        Map<String, Object[]> results = sac.getResultsArrayMap();
        for (String r : results.keySet()) {
            System.out.println(r + " (" + results.get(r).getClass() + ")\n  " + ASCII.cat(",", results.get(r)));
        }

        assert Arrays.deepEquals(results.get("x1"), x1) : Arrays.toString(results.get("x1")) + " != " + Arrays.toString(x1);
        assert Arrays.deepEquals(results.get("x2"), x2) : Arrays.toString(results.get("x2")) + " != " + Arrays.toString(x2);
        String[] z = branin(x1, x2);//new String[]{"136.0767"};
        assert Arrays.deepEquals(round2(results.get("cat")), z) : Arrays.toString(round2(results.get("cat"))) + " != " + Arrays.toString(z);

        //sac.archive();
        assert !bad_res.exists() : "Used the old archive dir";
        assert good_res.isDirectory() : "Did not created the defined archive dir";

        System.err.println(good_res.listFiles());

        assert good_res.listFiles(new FileFilter() {

            public boolean accept(File file) {
                return file.getName().endsWith("0.1,0.1");
            }
        }).length == 1 : "Did not used the defined archive dir";

        assert good_res.listFiles(new FileFilter() {

            public boolean accept(File file) {
                return file.getName().equals(tmp_in.getName() + ".out");
            }
        }).length == 1 : "Did not built the stream in the defined archive dir";

        Thread.sleep(1000);
        sac.shutdown();
    }

    @Test
    public void testKill() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++ testKill");

        File tmp_in = branin_in();

        final RunShell_v1 sac = new RunShell_v1(R, tmp_in, (String) null);
        //RunShell_v1 sac = new RunShell_v1(R, branin_in); // R should be dedected by plugin automatically.
        Funz.setVerbosity(verbose);
        sac.setArchiveDirectory(newTmpDir("testKill"));

        assert Arrays.asList(sac.getInputVariables()).contains("x1") : "Variable x1 not detected";
        assert Arrays.asList(sac.getInputVariables()).contains("x2") : "Variable x2 not detected";

        HashMap<String, String[]> X = new HashMap<String, String[]>();
        final String[] x1 = new String[]{"0.1", "0.2", "0.3", "0.4", "0.5"};
        X.put("x1", x1);
        final String[] x2 = new String[]{"0.3", "0.4", "0.5", "0.2", "0.3"};
        X.put("x2", x2);
        sac.setInputVariablesGroup("X", X);
        sac.setOutputExpressions("cat");

        sac.prj.setMaxCalcs(2);

        Thread t = new Thread(new Runnable() {

            public void run() {
                try {
                    sac.startComputationAndWait();
                    Map<String, Object[]> results = sac.getResultsArrayMap();
                    for (String r : results.keySet()) {
                        System.out.println(r + " (" + results.get(r).getClass() + ")\n  " + ASCII.cat(",", results.get(r)));
                    }

                    //assert Arrays.deepEquals(results.get("x1"), x1) : Arrays.toString(results.get("x1")) + " != " + Arrays.toString(x1);
                    //assert Arrays.deepEquals(results.get("x2"), x2) : Arrays.toString(results.get("x2")) + " != " + Arrays.toString(x2);
                    String[] z = branin(x1, x2);//new String[]{"311.5395", "252.1709", "223.7021", "71.96581", "55.78288"};
                    assert !Arrays.deepToString(round2(results.get("cat"))).equals(Arrays.deepToString(z)) : Arrays.toString(round2(results.get("cat"))) + " == " + Arrays.toString(z);

                    //System.err.println("Computers pool : " + Print.gridStatusInformation());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        t.start();

        Thread.sleep(3000);
        sac.shutdown();

        t.join();
    }

    @Test
    public void testConcurrency() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++ testConcurrency");

        final boolean[] tests = new boolean[]{false, false, false, false};
        final boolean[] done = new boolean[tests.length];
        for (int i = 0; i < tests.length; i++) {
            done[i] = false;
        }
        Funz.setVerbosity(verbose);
        for (int i = 0; i < tests.length; i++) {
            final int I = i;
            new Thread(new Runnable() {

                public void run() {
                    File tmp_in = null;
                    try {
                        tmp_in = newTmpFile("branin." + I + ".R");
                    } catch (IOException ex) {
                        assert false : ex.getMessage();
                    }
                    if (tmp_in.exists()) {
                        tmp_in.delete();
                    }
                    while (true) {
                        try {
                            Disk.copyFile(new File("src/test/samples/branin.R"), tmp_in);
                            break;
                        } catch (Exception e) {
                            System.err.println("Retrying initialization of test ...");
                        }
                    }
                    try {
                        RunShell_v1 sac = new RunShell_v1(R, tmp_in, (String) null); // R should be dedected by plugin automatically.
                        sac.setArchiveDirectory(newTmpDir("testConcurrency_" + I));

                        assert Arrays.asList(sac.getInputVariables()).contains("x1") : "Variable x1 not detected";
                        assert Arrays.asList(sac.getInputVariables()).contains("x2") : "Variable x2 not detected";

                        HashMap<String, String[]> X = new HashMap<String, String[]>();
                        String[] x1 = new String[]{"0.1", "0.2", "0.3", "0.4", "0.5"};
                        X.put("x1", x1);
                        String[] x2 = new String[]{"0.3", "0.4", "0.5", "0.2", "0.3"};
                        X.put("x2", x2);
                        sac.setInputVariablesGroup("X", X);
                        sac.setOutputExpressions("cat");
                        sac.startComputationAndWait();
                        Map<String, Object[]> results = sac.getResultsArrayMap();
                        /*for (String r : results.keySet()) {
                         System.out.println(r + "\n  " + ASCII.cat(",", results.get(r)));
                         }*/

                        assert Arrays.deepEquals(results.get("x1"), x1) : Arrays.toString(results.get("x1")) + " != " + Arrays.toString(x1);
                        assert Arrays.deepEquals(results.get("x2"), x2) : Arrays.toString(results.get("x2")) + " != " + Arrays.toString(x2);
                        String[] z = branin(x1, x2);//new String[]{"75.34526", "19.61803", "18.87899", "17.54458", "5.154316"};
                        assert Arrays.deepEquals(round2(results.get("cat")), z) : Arrays.toString(round2(results.get("cat"))) + " != " + Arrays.toString(z);

                        Thread.sleep(1000);
                        sac.shutdown();
                        synchronized (tests) {
                            tests[I] = true;
                            done[I] = true;
                        }
                    } catch (Exception e) {
                        System.err.println("ERROR shell " + I + " \n" + e.getLocalizedMessage());
                        e.printStackTrace();
                        synchronized (tests) {
                            tests[I] = false;
                            done[I] = true;
                        }
                    } catch (AssertionError e) {
                        System.err.println("FAILED shell " + I + " \n" + e.getLocalizedMessage());
                        synchronized (tests) {
                            tests[I] = false;
                            done[I] = true;
                        }
                    }
                }
            }).start();
        }

        boolean alltrue = false;
        while (!alltrue) {
            Thread.sleep(2000);
            //System.err.println(".");
            System.err.println("============================\n" + Print.gridStatusInformation() + "============================\n");
            synchronized (tests) {
                alltrue = alltrue(done);
            }
        }

        assert alltrue(tests) : "One concurency run failed !";
    }

    @Test
    public void testMultipleCases() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++ testMultipleCases");

        File tmp_in = branin_in();

        RunShell_v1 sac = new RunShell_v1(R, tmp_in, (String) null); // R should be dedected by plugin automatically.
        Funz.setVerbosity(verbose);
        sac.setArchiveDirectory(newTmpDir("testMultipleCases"));

        assert Arrays.asList(sac.getInputVariables()).contains("x1") : "Variable x1 not detected";
        assert Arrays.asList(sac.getInputVariables()).contains("x2") : "Variable x2 not detected";

        String[] x1 = new String[]{"0.1", "0.2"};
        String[] x2 = new String[]{"0.3", "0.4", "0.5"};
        sac.setInputVariable("x1", x1);
        sac.setInputVariable("x2", x2);
        sac.setOutputExpressions("cat");
        sac.startComputationAndWait();
        Map<String, Object[]> results = sac.getResultsArrayMap();
        System.out.println(ArrayMapToMDString(results));

        //assert Arrays.deepEquals(results.get("x1"), x1) : Arrays.toString(results.get("x1")) + " != " + Arrays.toString(x1);
        //assert Arrays.deepEquals(results.get("x2"), x2) : Arrays.toString(results.get("x2")) + " != " + Arrays.toString(x2);
        //String[] z = branin(x1, x2);//new String[]{"75.34526", "19.61803", "18.87899", "17.54458", "5.154316"};
        //assert Arrays.deepToString(round2(results.get("cat"))).equals(Arrays.deepToString(z)) : Arrays.toString(round2(results.get("cat"))) + " != " + Arrays.toString(z);
        System.err.println("Computers pool : " + Print.gridStatusInformation());

        sac.shutdown();

        Thread.sleep(1000);
    }

    @Test
    public void testMultipleGroupCases() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++ testMultipleGroupCases");

        File tmp_in = branin_in();

        RunShell_v1 sac = new RunShell_v1(R, tmp_in, (String) null); // R should be dedected by plugin automatically.
        Funz.setVerbosity(verbose);
        sac.setArchiveDirectory(newTmpDir("testMultipleGroupCases"));

        assert Arrays.asList(sac.getInputVariables()).contains("x1") : "Variable x1 not detected";
        assert Arrays.asList(sac.getInputVariables()).contains("x2") : "Variable x2 not detected";

        HashMap<String, String[]> X = new HashMap<String, String[]>();
        String[] x1 = new String[]{"0.1", "0.2", "0.3", "0.4", "0.5"};
        X.put("x1", x1);
        String[] x2 = new String[]{"0.3", "0.4", "0.5", "0.2", "0.3"};
        X.put("x2", x2);
        sac.setInputVariablesGroup("X", X);
        sac.setOutputExpressions("cat");
        sac.startComputationAndWait();
        Map<String, Object[]> results = sac.getResultsArrayMap();
        System.out.println(ArrayMapToMDString(results));

        assert Arrays.deepEquals(results.get("x1"), x1) : Arrays.toString(results.get("x1")) + " != " + Arrays.toString(x1);
        assert Arrays.deepEquals(results.get("x2"), x2) : Arrays.toString(results.get("x2")) + " != " + Arrays.toString(x2);
        String[] z = branin(x1, x2);//new String[]{"75.34526", "19.61803", "18.87899", "17.54458", "5.154316"};
        assert Arrays.deepToString(round2(results.get("cat"))).equals(Arrays.deepToString(z)) : Arrays.toString(round2(results.get("cat"))) + " != " + Arrays.toString(z);

        System.err.println("Computers pool : " + Print.gridStatusInformation());

        sac.shutdown();

        Thread.sleep(1000);
    }

    @Test
    public void testDuplicateCases() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++ testDuplicateCases");

        File tmp_in = branin_in();

        RunShell_v1 sac = new RunShell_v1(R, tmp_in, (String) null);
        Funz.setVerbosity(verbose);
        sac.setArchiveDirectory(newTmpDir("testDuplicateCases"));

        assert Arrays.asList(sac.getInputVariables()).contains("x1") : "Variable x1 not detected";
        assert Arrays.asList(sac.getInputVariables()).contains("x2") : "Variable x2 not detected";

        HashMap<String, String[]> X = new HashMap<String, String[]>();
        String[] x1 = new String[]{"0.1", "0.2", "0.1", "0.1"};
        X.put("x1", x1);
        String[] x2 = new String[]{"0.1", "0.2", "0.1", "0.1"};
        X.put("x2", x2);
        sac.setInputVariablesGroup("X", X);
        //"x1",X.get("x1"));
        //sac.setInputVariable("x2",X.get("x2"));
        sac.setOutputExpressions("cat");

        sac.startComputationAndWait();
        Map<String, Object[]> results = sac.getResultsArrayMap();
        //System.out.println(Utils.ArrayMapToMDString(results));

        assert Arrays.deepEquals(results.get("x1"), new String[]{"0.1", "0.2", "0.1", "0.1"}) : Arrays.toString(results.get("x1")) + " != " + Arrays.toString(new String[]{"0.1", "0.2", "0.1", "0.1"});
        assert Arrays.deepEquals(results.get("x2"), new String[]{"0.1", "0.2", "0.1", "0.1"}) : Arrays.toString(results.get("x2")) + " != " + Arrays.toString(new String[]{"0.1", "0.2", "0.1", "0.1"});
        String[] z = new String[]{"136.0", "50.7", "136.0", "136.0"};//f(new String[]{"0.1", "0.2"},new String[]{"0.1", "0.2"});
        assert Arrays.deepEquals(round2(results.get("cat")), z) : Arrays.toString(round2(results.get("cat"))) + " != " + Arrays.toString(z);

        Thread.sleep(1000);
        sac.shutdown();
    }

    @Test
    public void testVectorize() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++ testVectorize");

        File tmp_in = branin_in();

        RunShell_v1 sac = new RunShell_v1(R, tmp_in, (String) null);
        Funz.setVerbosity(verbose);
        sac.setArchiveDirectory(newTmpDir("testVectorize"));

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
            System.out.println(r + " (" + results.get(r).getClass() + ")\n  " + ASCII.cat(",", results.get(r)));
        }

        assert Arrays.deepEquals(results.get("x1"), x1) : Arrays.toString(results.get("x1")) + " != " + Arrays.toString(x1);
        assert Arrays.deepEquals(results.get("x2"), x2) : Arrays.toString(results.get("x2")) + " != " + Arrays.toString(x2);
        String[] z = branin(x1, x2);//new String[]{"214.8081", "167.0327", "146.7375"};
        assert Arrays.deepEquals(round2(results.get("cat")), z) : Arrays.toString(round2(results.get("cat"))) + " != " + Arrays.toString(z);

        Thread.sleep(1000);
        sac.shutdown();
    }

    @Test
    public void testImplicitCode() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++ testImplicitCode");

        File tmp_in = branin_in();

        RunShell_v1 sac = new RunShell_v1(null/*"R"*/, tmp_in, (String) null); // R should be dedected by plugin automatically.
        Funz.setVerbosity(verbose);
        sac.setArchiveDirectory(newTmpDir("testImplicitCode"));

        assert Arrays.asList(sac.getInputVariables()).contains("x1") : "Variable x1 not detected";
        assert Arrays.asList(sac.getInputVariables()).contains("x2") : "Variable x2 not detected";

        HashMap<String, String[]> X = new HashMap<String, String[]>();
        String[] x1 = new String[]{"0.1"};
        X.put("x1", x1);
        String[] x2 = new String[]{"0.1"};
        X.put("x2", x2);
        sac.setInputVariablesGroup("X", X);
        sac.setOutputExpressions("cat");
        sac.startComputationAndWait();
        Map<String, Object[]> results = sac.getResultsArrayMap();
        for (String r : results.keySet()) {
            System.out.println(r + " (" + results.get(r).getClass() + ")\n  " + ASCII.cat(",", results.get(r)));
        }

        assert Arrays.deepEquals(results.get("x1"), x1) : Arrays.toString(results.get("x1")) + " != " + Arrays.toString(x1);
        assert Arrays.deepEquals(results.get("x2"), x2) : Arrays.toString(results.get("x2")) + " != " + Arrays.toString(x2);
        String[] z = branin(x1, x2);//new String[]{"214.8081"};
        assert Arrays.deepEquals(round2(results.get("cat")), z) : Arrays.toString(round2(results.get("cat"))) + " != " + Arrays.toString(z);

        Thread.sleep(1000);
        sac.shutdown();
    }
}
