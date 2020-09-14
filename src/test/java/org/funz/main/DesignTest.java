package org.funz.main;

import java.io.File;
import java.security.Permission;
import java.util.regex.Pattern;
import org.apache.ftpserver.util.OS;
import org.funz.util.ParserUtils;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author richet
 */
public class DesignTest extends org.funz.api.TestUtils {

    public static void main(String args[]) throws Exception {
        test(DesignTest.class.getName());
    }

    @Before
    public void setUp() throws Exception {
        super.setUp("DesignTest");
        System.setSecurityManager(new Exit0SecurityManager());

        if (new File("Design.csv").exists()) {
            assert new File("Design.csv").delete() : "could not delete Design.csv";
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
        System.err.println("+++++++++++++++++++++++++++++++++++++++++ testParseError");

        try {
            Design.main("Design abcdef".split(" "));
        } catch (ExitCatcher e) {
            assert e.status == Design.PARSE_ERROR : "Bad exit status :" + e.status;
        }
    }

    @Test
    public void testDesignFailedResult() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++++++++++ testDesignFailedResult");

        if (!hasBash()) {
            System.err.println("No bash interpeter. Skippig test.");
            return;
        }

        try {
            Design.main("Design -d GradientDescent -do nmax=3 epsilon=0.001 delta=1 target=-10 -f ./multtttttttt.sh -fa x1 x2 -iv x1=[-0.5,-0.1] x2=[0.3,.8] -v 10".split(" "));
        } catch (ExitCatcher e) {
            assert e.status != 0 : "Bad 0 exit status";
        }
        assert new File("Design.csv").exists() : "No output file Design.csv created";
    }

    public static boolean matchesIn(String what, String in) {
        return Pattern.compile(in).matcher(what).find();
    }

    @Test
    public void testDesign() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++++++++++ testDesign");

        if (!hasBash()) {
            System.err.println("No bash interpeter. Skippig test.");
            return;
        }

        try {
            Design.main("Design -d GradientDescent -do nmax=3 epsilon=0.001 delta=1 target=-10 -f ./mult.sh -fa x1 x2 -iv x1=[-0.5,-0.1] x2=[0.3,.8] -v 10 -ad tmp".split(" "));
        } catch (ExitCatcher e) {
            assert e.status == 0 : "Bad exit status :" + e.status;
        }
        assert new File("Design.csv").exists() : "No output file Design.csv created";

        assert matchesIn(ParserUtils.getASCIIFileContent(new File("Design.csv")), "found at x1 = -0\\.5(.*)x2 = 0\\.8") : "Did not succeded to find min: \n" + ParserUtils.getASCIIFileContent(new File("Design.csv"));
    }

    @Test
    public void testDesignPar() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++++++++++ testDesignPar");

        if (!hasBash()) {
            System.err.println("No bash interpeter. Skippig test.");
            return;
        }

        try {
            Design.main("Design -d GradientDescent -do nmax=3 epsilon=0.001 delta=1 target=-10 -f ./mult.sh -fa x1 x2 -fp 3 -iv x1=[-0.5,-0.1] x2=[0.3,.8] -v 10".split(" "));
        } catch (ExitCatcher e) {
            assert e.status == 0 : "Bad exit status :" + e.status;
        }
        assert new File("Design.csv").exists() : "No output file Design.csv created";

        assert matchesIn(ParserUtils.getASCIIFileContent(new File("Design.csv")), "found at x1 = -0\\.5(.*)x2 = 0\\.8") : "Did not succeded to find min: \n" + ParserUtils.getASCIIFileContent(new File("Design.csv"));
    }

    @Test
    public void testDesignTooMuchPar() throws Exception {
        System.err.println("+++++++++++++++++++++++++++++++++++++++++ testDesignTooMuchPar");

        if (!hasBash()) {
            System.err.println("No bash interpeter. Skippig test.");
            return;
        }

        try {
            Design.main("Design -d GradientDescent -do nmax=3 epsilon=0.001 delta=1 target=-10 -f ./mult.sh -fa x1 x2 -fp 5 -iv x1=[-0.5,-0.1] x2=[0.3,.8] -v 10".split(" "));
        } catch (ExitCatcher e) {
            assert e.status == 0 : "Bad exit status :" + e.status;
        }
        assert new File("Design.csv").exists() : "No output file Design.csv created";

        Thread.sleep(2000);

        assert matchesIn(ParserUtils.getASCIIFileContent(new File("Design.csv")), "found at x1 = -0\\.5(.*)x2 = 0\\.8") : "Did not succeded to find min: \n" + ParserUtils.getASCIIFileContent(new File("Design.csv"));
    }

    public static boolean hasBash() {
        return System.getProperty("os.name").toLowerCase().contains("inux") || System.getProperty("os.name").toLowerCase().contains("mac");
    }

}
