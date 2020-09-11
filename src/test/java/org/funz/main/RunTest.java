package org.funz.main;

import java.io.File;
import java.security.Permission;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author richet
 */
public class RunTest extends org.funz.api.TestUtils {

    public static void main(String args[]) throws Exception {
        test(RunTest.class.getName());
    }

    @Before
    public void setUp() throws Exception {
        super.setUp("RunTest");
        System.setSecurityManager(new Exit0SecurityManager());
        
        if (new File("Run.csv").exists()) {
            assert new File("Run.csv").delete() : "could not delete Run.csv";
        }
    }

    @Override
    public void tearDown() {
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
        System.err.println("++++++++++++++++++++++++++++++++++++++++++ testParseError");
        File tmp_in = mult_in();

        try {
            Run.main("Run abcdef".split(" "));
        } catch (ExitCatcher e) {
            assert e.status == Run.PARSE_ERROR : "Bad exit status :" + e.status;
        }
    }

    @Test
    public void testRun1() throws Exception {
        System.err.println("++++++++++++++++++++++++++++++++++++++++++ testRun1");

        File tmp_in = mult_in();

        try {
            Run.main("Run -m R -if tmp/branin.R -iv x1=.5 x2=0.3 -v 0 -ad tmp".split(" "));
        } catch (ExitCatcher e) {
            assert e.status == 0 : "Not 0 exit status :" + e.status;
        }

        assert new File("Run.csv").exists() : "No output file Run.csv created";
    }

    @Test
    public void testOutputExpression() throws Exception {
        System.err.println("++++++++++++++++++++++++++++++++++++++++++ testOutputExpression");

        File tmp_in = mult_in();

        try {
            Run.main("Run -m R -if tmp/branin.R -iv x1=.5 x2=0.3 -v 0 -ad tmp -oe 1+cat".split(" "));
        } catch (ExitCatcher e) {
            assert e.status == 0 : "Not 0 exit status :" + e.status;
        }

        assert new File("Run.csv").exists() : "No output file Run.csv created";
    }

    @Test
    public void testRunNoDefinedVarValues() throws Exception {
        System.err.println("++++++++++++++++++++++++++++++++++++++++++ testRunNoDefinedVarValues");
        File tmp_in = mult_in();

        try {
            Run.main("Run -m R -if tmp/branin.R -iv x1=.5 -v 0 -ad tmp".split(" "));
        } catch (ExitCatcher e) {
            assert e.status == 0 : "Wrong exit status :" + e.status;
        }

        assert new File("Run.csv").exists() : "No output file Run.csv created";
    }

    @Test
    public void testRun9() throws Exception {
        System.err.println("++++++++++++++++++++++++++++++++++++++++++ testRun9");
        File tmp_in = mult_in();

        try {
            Run.main("Run -m R -if tmp/branin.R -iv x1=.5,.6,.7 x2=0.3,.4,.5 -v 0 -ad tmp".split(" "));
        } catch (ExitCatcher e) {
            assert e.status == 0 : "Not 0 exit status :" + e.status;
        }
        assert new File("Run.csv").exists() : "No output file Run.csv created";
    }

    
    @Test
    public void testRun9verb() throws Exception {
        System.err.println("++++++++++++++++++++++++++++++++++++++++++ testRun9");
        File tmp_in = mult_in();

        try {
            Run.main("Run -m R -if tmp/branin.R -iv x1=.5,.6,.7 x2=0.3,.4,.5 -mc sleep=0.1 -v 2 -ad tmp".split(" "));
        } catch (ExitCatcher e) {
            assert e.status == 0 : "Not 0 exit status :" + e.status;
        }
        assert new File("Run.csv").exists() : "No output file Run.csv created";
    }
    
}
