package org.funz.main;

import java.io.File;
import java.security.Permission;
import org.apache.commons.io.FileUtils;
import org.funz.script.RMathExpression;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author richet
 */
public class RunDesignTest extends org.funz.api.TestUtils {

    public static void main(String args[]) throws Exception {
        test(RunDesignTest.class.getName());
    }

    @Before
    public void setUp() throws Exception {
        super.setUp("RunDesignTest");
        System.setSecurityManager(new Exit0SecurityManager());

        if (new File("RunDesign.csv").exists()) {
            assert new File("RunDesign.csv").delete() : "could not delete RunDesign.csv";
        }
    }

    @Override
    public void tearDown() throws InterruptedException {
        System.setSecurityManager(null); // or save and restore original
        try {
            super.tearDown();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    protected static class ExitCatcher extends SecurityException {

        public final int status;

        public ExitCatcher(int status) {
            super("Exit status = " + status);
            this.status = status;
        }
    }

    private static class Exit0SecurityManager extends SecurityManager {

        @Override
        public void checkPermission(Permission perm) {
            // allow anything.
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // allow anything.
        }

        @Override
        public void checkExit(int status) {
            super.checkExit(status);
            //if (status != 0) {
            throw new ExitCatcher(status);
            //}
        }
    }

    @Test
    public void testParseError() throws Exception {
        System.err.println("++++++++++++++++++++++++++++++++++++++++ testParseError");
        File tmp_in = tmp_in();

        try {
            RunDesign.main("RunDesign abcdef".split(" "));
        } catch (ExitCatcher e) {
            assert e.status == RunDesign.PARSE_ERROR : "Bad exit status :" + e.status;
        }
    }

    @Test
    public void testRunDesignFailedResult() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++++++++++ testRunDesignFailedResult");
        File tmp_in = tmp_in();

        try {
            RunDesign.main("RunDesign -m R -d GradientDescent -do nmax=3 -if tmp/branin.R -iv x1=[-1.1,1] x2=[0.3,.4] -v 10 -ad tmp -oe x1+min(cat,10,na.rm=F)".split(" "));
        } catch (ExitCatcher e) {
            assert e.status != 0 : "Bad 0 exit status\n Rundesign.csv:\n" + FileUtils.readFileToString(new File("RunDesign.csv"));
        }
        assert new File("RunDesign.csv").exists() : "No output file RunDesign.csv created";
    }

    @Test 
    public void testRunDesign1() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++++++++++ testRunDesign1");
        File tmp_in = tmp_in();

        try {
            RunDesign.main("RunDesign -m R -d GradientDescent -do nmax=3 -if tmp/branin.R -iv x1=[0,1] x2=[0.3,.4] -v 10 -ad tmp".split(" "));
        } catch (ExitCatcher e) {
            assert e.status == 0 : "Not 0 exit status :" + e.status;
        }
        assert new File("RunDesign.csv").exists() : "No output file RunDesign.csv created";
    }

    @Test
    public void testRun1Design() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++++++++++ testRun1Design");
        File tmp_in = tmp_in();

        try {
            RunDesign.main("RunDesign -m R -d GradientDescent -do nmax=3 -if tmp/branin.R -iv x1=.3 x2=[0.3,.4] -v 10 -ad tmp".split(" "));
        } catch (ExitCatcher e) {
            assert e.status == 0 : "Not 0 exit status :" + e.status;
        }
        assert new File("RunDesign.csv").exists() : "No output file RunDesign.csv created";
    }

    @Test
    public void testRun2Designs() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++++++++++ testRun2Design");
        File tmp_in = tmp_in();

        try {
            RunDesign.main("RunDesign -m R -d GradientDescent -do nmax=3 -if tmp/branin.R -iv x1=.2,.3 x2=[0.3,.4] -v 10 -ad tmp".split(" "));
        } catch (ExitCatcher e) {
            assert e.status == 0 : "Not 0 exit status :" + e.status;
        }
        assert new File("RunDesign.csv").exists() : "No output file RunDesign.csv created";
    }

    @Test
    public void testOutputExpression() throws Exception {
        System.err.println("++++++++++++++++++++++++++++++++++++++++++ testOutputExpression");

        File tmp_in = tmp_in();

        try {
            RunDesign.main("RunDesign -m R -d GradientDescent -do nmax=3 -oe 1+cat[1] -if tmp/branin.R -iv x1=0.5 x2=[0.3,.4] -v 10 -ad tmp".split(" "));
        } catch (ExitCatcher e) {
            assert e.status == 0 : "Not 0 exit status :" + e.status;
        }

        assert new File("RunDesign.csv").exists() : "No output file Run.csv created";
    }

    @Test
    public void testOutputExpressionN() throws Exception {
        System.err.println("++++++++++++++++++++++++++++++++++++++++++ testOutputExpression");

        File tmp_in = tmp_in();

        try {
            RunDesign.main("RunDesign -m R -d GradientDescent -do nmax=3 -oe N(1+cat[1],1) -if tmp/branin.R -iv x1=0.5 x2=[0.3,.4] -v 10 -ad tmp".split(" "));
        } catch (ExitCatcher e) {
            assert e.status == 0 : "Not 0 exit status :" + e.status;
        }

        assert new File("RunDesign.csv").exists() : "No output file Run.csv created";
    }

    @Test
    public void testRunDesign1EGO() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++++++++++ testRunDesign1EGO");
        if (!RMathExpression.GetEngineName().contains("Rserve")) {System.err.println("Not using Rserve, so skipping test");return;} // Do not run if using Renjin or R2js...
        File tmp_in = tmp_in();

        try {
            RunDesign.main("RunDesign -m R -d EGO -if tmp/branin.R -iv x1=[0,1] x2=[0.3,.4] -v 10 -ad tmp".split(" "));
        } catch (ExitCatcher e) {
            assert e.status == 0 : "Not 0 exit status :" + e.status;
        }
        assert new File("RunDesign.csv").exists() : "No output file RunDesign.csv created";
    }

}
