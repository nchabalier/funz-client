package org.funz.script;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.lang.StringUtils;
import org.funz.Constants;
import org.funz.Proxy;
import org.funz.conf.Configuration;
import org.funz.log.Alert;
import org.funz.log.Log;
import org.funz.log.LogCollector;
import org.funz.log.LogCollector.SeverityLevel;
import org.funz.log.LogFile;
import org.funz.util.ASCII;
import org.funz.util.Data;
import org.funz.util.URLMethods;
import org.math.R.R2jsSession;
import org.math.R.RLog;
import org.math.R.RLog.Level;
import org.math.R.RenjinSession;
import org.math.R.RserveDaemon;
import org.math.R.RserveSession;
import org.math.R.RserverConf;
import org.math.R.Rsession;

/**
 *
 * @author richet
 */
public class RMathExpression extends MathExpression {

    public final static String PlusInfinity = "Inf";
    public final static String MinusInfinity = "-Inf";
    public Rsession R;
    public Properties env = new Properties();
    //public File LIBPATH = new File(Constants.APP_USER_DIR, "R");
    public String toSource, toLoad;
    public RserverConf serverConf;
    public boolean spawnRserve = true;
    public boolean verbose = false;
    File log;

    @Override
    public String getLastMessage() {
        return R.getLastLogEntry();
    }

    public String getLastError() {
        return R.getLastError();
    }

    public String getLastResult() {
        return R.getLastOutput();
    }

    public void toLoad(File f) {
        R.load(f);
        if (toLoad == null) {
            toLoad = f.toURI().toString();
        } else {
            toLoad = toLoad + "," + f.toURI().toString();
        }
    }

    @Override
    public void finalize() throws Throwable {
        finalizeRsession();
        super.finalize();
    }

    String R_engine = null;

    public void initConfiguration() {
        if (Configuration.hasProperty("R.source")) {
            toSource = Configuration.getProperty("R.source", null);
        }

        if (Configuration.hasProperty("R.load")) {
            toLoad = Configuration.getProperty("R.load", null);
        }

        if (Configuration.hasProperty("R.verbose")) {
            verbose = Configuration.getBoolProperty("R.verbose");
        }

        //Log.out("R.server: " + Configuration.getProperty("R.server", "?"), 8);
        String[] Rservers = Configuration.getProperty("R.server", "").split(",");
        if (Rservers != null && Rservers.length > 0) {
            for (String Rserver : Rservers) {
                if (Rserver != null && Rserver.startsWith(RserverConf.RURL_START)) {
                    Log.out("Using Rserve: " + Rserver, 2);
                    try {
                        serverConf = RserverConf.parse(Rserver);
                        R_engine = "Rserve";
                        break;
                    } catch (Exception e) {
                        serverConf = null;
                        Log.err("Impossible to parse " + Rserver + "\n  " + e, 2);
                    }
                } else if (Rserver != null && Rserver.equals("Renjin")) {
                    Log.out("Using Renjin", 2);
                    serverConf = null;
                    R_engine = "Renjin";
                    break;
                } else if (Rserver != null && Rserver.equals("R2js")) {
                    Log.out("Using R2js", 2);
                    serverConf = null;
                    R_engine = "R2js";
                    break;
                } else {
                    Log.out("Ignoring R server: " + Rserver, 2);
                }
            }
        }
        if (R_engine == null) {
            R_engine = "R2js";
            Log.out("Using R2js (by default)", 2);
        }

        if (serverConf != null && serverConf.isLocal()) {
            env.setProperty("http_proxy", Proxy.http_proxy());
        }

        if (serverConf != null && serverConf.isLocal() && spawnRserve) {
            String r_home = null;
            if (Configuration.hasProperty("R.home")) {
                r_home = Configuration.getProperty("R.home", null);
            }
            if (!RserveDaemon.findR_HOME(r_home)) {
                Log.err("Impossible to find R_HOME=" + r_home + ". Asking user...", 2);
                boolean found_R_HOME = false;
                while (!found_R_HOME) {
                    r_home = Alert.askPath("R installation path ?").getAbsolutePath();
                    found_R_HOME = RserveDaemon.findR_HOME(r_home);
                    if (!found_R_HOME) {
                        Alert.showError("Unsuitable R PATH. Try again...");
                    }
                }
                if (found_R_HOME) {
                    Configuration.setProperty("R.home", r_home);
                }
            }
            if (RserveDaemon.R_HOME != null) {
                Log.out("R_HOME: " + RserveDaemon.R_HOME, 2);
            } else {
                Log.err("R installation not available.", 2);
                serverConf = null;
            }
        }
    }

    void testWWWConnection() {
        String url = "http://www.r-project.org";
        try {
            String rproject = URLMethods.readURL(url, Integer.parseInt(Configuration.getProperty("url.timeout", "5000"))).trim();
            if (rproject == null || rproject.length() == 0 || !rproject.contains("The R Project for Statistical Computing")) {
                Configuration.setWWWConnected(false);
            } else {
                Configuration.setWWWConnected(true);
            }
        } catch (Exception ne) {
            Configuration.setWWWConnected(false);
            Log.err("URL " + url + " not accessible.", 2);
        }
    }

    public RMathExpression(Rsession R) {
        super("Rsession " + (R instanceof RenjinSession ? "(Renjin) " : R instanceof R2jsSession ? "(R2js) " : "(Rserve) ") + R.hashCode());
        this.R = R;
    }

    public RMathExpression(String name) {
        this(name, new File(Constants.APP_USER_DIR, name + ".Rlog"));
    }

    public RMathExpression(String name, File log) {
        super(name);

        this.log = log;

        initConfiguration();

        if (R == null) {
            createR();
        }

        initR();
    }

    void initR() {
        try { // Fix for windows when /cygdrive/c/.. remains in HOME
            R.voidEval("Sys.setenv(HOME='"
                    + new File(System.getProperty("user.home")).getAbsolutePath().
                            replace('\\', '/') // Fix path sep
                    + "')");
        } catch (Rsession.RException ex) {
            Log.err("Failed to setup user homedir: " + ex.getMessage(), 3);
        }
        R.log("######################### INFORMATION ###########################", Level.WARNING);
        printInformation(R);
        initLibPath(R);
        R.log("#################################################################", Level.WARNING);
        initSession();
    }
    RLog streamlogger;

    public static class LoggerCollector implements RLog {

        LogCollector log;

        public LoggerCollector(LogCollector log) {
            this.log = log;
        }

        public synchronized void log(String m, Level l) {
            SeverityLevel sl = SeverityLevel.INFO;
            switch (l) {
                case ERROR:
                    sl = SeverityLevel.ERROR;
                case WARNING:
                    sl = SeverityLevel.WARNING;
                default:
                    sl = SeverityLevel.INFO;
            }

            log.logMessage(sl, false, m);
        }

        public void closeLog() {
            if (log != null) {
                log.close();
            }
        }
    }

    void createR() {
        try {
            streamlogger = (log == null ? new RLog() {

                public void log(String string, Level level) {
                    Log.logMessage(this, SeverityLevel.valueOf(level.toString()), true, string);
                }

                public void closeLog() {
                    Log.logMessage(this, SeverityLevel.INFO, true, "close log.");
                }
            } : new LoggerCollector(new LogFile(log)));

            org.math.R.Log.Out = new org.math.R.Log() {

                @Override
                public void print(String string) {
                    Log.out(string, 2);
                }

                @Override
                public void println(String string) {
                    Log.out(string, 2);
                }
            };

            org.math.R.Log.Err = new org.math.R.Log() {

                @Override
                public void print(String string) {
                    Log.err(string, 2);
                }

                @Override
                public void println(String string) {
                    Log.err(string, 2);
                }
            };

            try {
                if (serverConf != null && R_engine.equals("Rserve")) {
                    if (serverConf.isLocal() && serverConf.port < 0) {
                        serverConf = null; // We reset this conf to let RserveSession startup deamon by itself.
                    }
                    R = new RserveSession(streamlogger, env, serverConf);
                    R.setCRANRepository(Configuration.getProperty("R.repos","http://cloud.r-project.org"));
                } else if (R_engine.equals("Renjin")) {
                    R = new RenjinSession(streamlogger, env);
                } else {
                    R = null;
                }
            } catch (Exception e) {
                Log.out("Could not instanciate Rsession: " + e.getMessage(), 0);
                e.printStackTrace();
                R = null;
            }

            if (R == null) { //default if (R_engine.equals("R2js")){
                R = new R2jsSession(streamlogger, env);
                if (R == null) {
                    throw new Exception("Cannot instanciate R2jsSession");
                }
            }
            if (R instanceof R2jsSession) {
                ((R2jsSession) R).debug_js = Boolean.parseBoolean(Configuration.getProperty("R2js.debug", "false"));
                R.voidEval("Sys__info = function() {return(" + asRList(Data.newMap(
                        "nodename", InetAddress.getLocalHost().getHostName(),
                        "sysname", System.getProperty("os.name"),
                        "release", "?",
                        "version", System.getProperty("os.version"),
                        "user", System.getProperty("user.name")
                )) + ")}");
                R.voidEval("Sys.setenv(R_HOME='')");//+toRcode(System.getenv())+")\nreturn(env[v])}");
                R.voidEval("options = function() {return(" + asRList(Data.newMap(
                        "OutDec", DecimalFormatSymbols.getInstance().getDecimalSeparator()
                )) + ")}");
            }

            R.TRY_MODE = true;
        } catch (Exception e) {
            Log.err("R not available.\nPlease check your R/Rserve/Renjin installation manually.", 2);
            Alert.showError("R not available.\nPlease check your R/Rserve/Renjin installation manually.");
            e.printStackTrace(System.err);
            //System.exit(-1);
        }
    }

    String asRList(Map m) {
        if (m.isEmpty()) {
            return "list()";
        }
        String l = "list(";
        for (Object k : m.keySet()) {
            l = l + k + "='" + m.get(k) + "',";
        }
        return l.substring(0, l.length() - 1) + ")";
    }

    public static void printInformation(Rsession R) {
        try {
            String nodename = (String) R.eval("Sys.info()[['nodename']]");
            String rhome = (String) R.eval("Sys.getenv('R_HOME')");
            String host = (nodename == null ? "?" : nodename) + ":" + (rhome == null ? "?" : rhome);
            R.log("Host " + host, Level.WARNING);
            String dir = R.eval("getwd()").toString();
            R.log("Directory " + dir, Level.WARNING);
            //System.err.println("Host " + R.silentlyEval("Sys.info()[['nodename']]").asString() + ":" + R.silentlyEval("Sys.getenv('R_HOME')").asString());
            String sys = R.eval("Sys.info()[['sysname']]") + " " + R.eval("Sys.info()[['release']]") + " " + R.eval("Sys.info()[['version']]");
            R.log("System " + sys, Level.WARNING);

            String user = R.eval("Sys.info()[['user']]").toString();
            R.log("User " + user, Level.WARNING);
            String dec = R.eval("options()[['OutDec']]").toString();
            R.log("DecimalSeparator " + dec, Level.WARNING);
            String vers = (String) R.eval("R.version.string");
            R.log(vers, Level.WARNING);
            //R.log("R http_proxy " + R.silentlyEval("Sys.getenv('http_proxy')").asString(), Level.WARNING);
            R.note_text("Returns:\n\n"
                    + "  * Host " + host + "\n"
                    + "  * Directory " + dir + "\n"
                    + "  * System " + sys + "\n"
                    + "  * User " + user + "\n"
                    + "  * DecimalSeparator " + dec + "\n"
                    + "  * R.version.string " + vers);
        } catch (Exception r) {
            Log.err(r, 1);
            try {
                System.err.println("nodename: " + R.eval("Sys.info()[['nodename']]"));
            } catch (Exception ex) {
                Log.err(ex, 1);
            }
            try {
                System.err.println("sysname: " + R.eval("Sys.info()[['sysname']]"));
            } catch (Exception ex) {
                Log.err(ex, 1);
            }
            try {
                System.err.println("release: " + R.eval("Sys.info()[['release']]"));
            } catch (Exception ex) {
                Log.err(ex, 1);
            }
            try {
                System.err.println("version: " + R.eval("Sys.info()[['version']]"));
            } catch (Exception ex) {
                Log.err(ex, 1);
            }
            try {
                System.err.println("user: " + R.eval("Sys.info()[['user']]"));
            } catch (Exception ex) {
                Log.err(ex, 1);
            }
        }
    }

    public static void initLibPath(Rsession R) {
        if (R instanceof R2jsSession) {
            R.log("R2js ignore libPath setting $HOME/.Funz/R", Level.WARNING);
            return;
        }
        try {
            //if (RLibPath == null) {
            String RLibPath = "file.path('"+Constants.APP_USER_DIR.getAbsolutePath().replace('\\', '/')+"','R')";
            //}
            //if (RLibPath != null) {
            R.voidEval("if (!file.exists(" + RLibPath + ")) dir.create(" + RLibPath + ",recursive=TRUE)");
            R.voidEval(".libPaths(new=" + RLibPath + ")");
            R.log("libPath " + ASCII.cat("\n            ", R.asStrings(R.eval(".libPaths()"))), Level.WARNING);
            //}
        } catch (Exception r) {
            Log.err(r, 1);
            r.printStackTrace();
        }
    }

    void initSession() {
        if (toSource != null) {
            String[] uri = toSource.split(",");
            for (String u : uri) {
                try {
                    File f = File.createTempFile("toSource", ".R");
                    File ret = URLMethods.downloadFile(u, f, Integer.parseInt(Configuration.getProperty("url.timeout", "5000")));
                    if (ret != null && ret.exists()) {
                        R.source(ret);
                    }
                } catch (IOException ex) {
                    Log.logException(false, ex);
                }
            }
        }

        if (toLoad != null) {
            String[] uri = toLoad.split(",");
            for (String u : uri) {
                try {
                    File f = File.createTempFile("toLoad", ".Rdata");
                    File ret = URLMethods.downloadFile(u, f, Integer.parseInt(Configuration.getProperty("url.timeout", "5000")));
                    if (ret != null) {
                        R.load(ret);
                    }
                } catch (IOException ex) {
                    Log.logException(false, ex);
                }
            }
        }
        globalVariables.clear();
        globalVariables.addAll(Arrays.asList(R.ls()));
    }

    public synchronized void finalizeRsession() {
        try {
            if (R != null) {
                R.end();
            }
            Log.logMessage(name, SeverityLevel.INFO, false, "Rsession " + name + " ended.");
        } catch (Exception e) {
            Log.logMessage(name, SeverityLevel.ERROR, false, "Rsession " + name + " end failed.");
            if (Log.level >= 10) e.printStackTrace();
        }
        if (streamlogger != null) {
            streamlogger.closeLog();
        }
    }

    public static void main(String[] args) throws MathException {
        Configuration.readProperties(null);
        Configuration.setWWWConnected(true);

        RMathExpression engine = new RMathExpression("MathExpressionTest");

        System.err.println("c(1+2,a-1)");
        try {
            Map<String, Object> a = new HashMap<String, Object>();
            a.put("a", 10);
            engine.eval("c(1+2,a-1)", a);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        System.err.println("1+2");
        try {
            engine.eval("1+2", null);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        System.err.println("1+fghdf");
        try {
            engine.eval("1+dghs", null);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        System.err.println("1+2");
        try {
            engine.eval("1+2", null);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

        System.err.println("a+b");
        Map<String, Object> ab = new HashMap<String, Object>();
        ab.put("a", 1);
        ab.put("b", 2);
        try {
            engine.eval("a+b", ab);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }

        System.err.println("a+b");
        Map<String, Object> ab_nob = new HashMap<String, Object>();
        ab_nob.put("a", 1);
        try {
            engine.eval("a+b", ab_nob);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    @Override
    public synchronized Object eval(String expression, Map<String, Object> vars) throws MathException {
        if (expression == null) {
            return null;
        }
        HashMap<String, Object> cleanvars = null;
        if (vars != null) {
            cleanvars = new HashMap<String, Object>();
            for (String v : vars.keySet()) {
                if (v == null) {
                    continue;
                }
                if (expression.equals(v)) {
                    return vars.get(v);
                }
                if (expression.contains(v)) {
                    cleanvars.put(v, vars.get(v));
                }
            }
        }
        Object ret = null;
        boolean restartR = false;
        try {
            if (cleanvars != null && !cleanvars.isEmpty()) {
                R.set(cleanvars);
            }

            ret = R.eval(expression);

            if (cleanvars != null && !cleanvars.isEmpty()) {
                R.rm(cleanvars.keySet().toArray(new String[cleanvars.size()]));
            }

            /*if (ret == null) {
             throw new MathException("Failed to cast " + expression + " : " + R.eval(expression));
             }*/
        } catch (Exception ex) {
            Log.err(ex, 2);
            if (Log.level>=10) ex.printStackTrace();
            try {// add a test to check session is available. otherwise restart.
                if (!((Double) (R.eval("1+41")) == 42)) {
                    restartR = true;
                }
            } catch (Exception e) {
                Log.err(e, 2);
                Log.err("...So will restart R...", 2);
                restartR = true;
            }
            throw new MathException("Bad syntax in " + expression + " with " + vars + " (" + ex.getMessage() + ")");
        } finally {
            if (restartR) {
                finalizeRsession();
                name = name + ".";
                createR();
                R.log("Restarting R session...", Level.ERROR);
                initR();
            }
        }
        if ((ret == null && vars != null && !vars.isEmpty())) {
            throw new MathException("Error in eval " + expression + " with " + vars);
        }
        if (ret != null && ret.toString().contains(Rsession.CAST_ERROR)) {
            throw new MathException("Unsupported return type in eval " + expression + " with " + vars);
        }
        return ret;
    }

    @Override
    public synchronized boolean set(String expression) throws MathException {
        if (expression != null && expression.length() > 0) {
            if (expression.contains(";")) {
                int i = 0;
                while ((i = expression.indexOf(";", i + 1)) > 0) {
                    int openbraces = StringUtils.countMatches(expression.substring(0, i), "{") - StringUtils.countMatches(expression.substring(0, i), "}");
                    if (openbraces == 0) {
                        boolean part1 = set(expression.substring(0, i));
                        boolean part2 = set(expression.substring(i + 1));
                        return part1 && part2;
                    }
                }
            }
            boolean ok = false;
            try {
                ok = R.voidEval(expression);
            } catch (Rsession.RException ex) {
                throw new MathException(ex.toString());
            }
            return ok;

        } else {
            return true;
        }
    }

    @Override
    public synchronized void reset() throws MathException {
        if (!R.rmAll()) {
            throw new MathException("Could not clean R env. !");
        }
        initSession();
    }

    public void updateKeywords() {
        /*for (String k : R.listCommands()) {
         if (!k.startsWith(".") && k.matches("([a-z_0-9\\.]+)")) {
         Rhelp.source(k);
         }
         }*/
    }

    @Override
    public String getOperands() {
        return super.getOperands().replace('.', (char) 0);
    }

    @Override
    public String getMinusInfinityExpression() {
        return MinusInfinity;
    }

    @Override
    public String getPlusInfinityExpression() {
        return PlusInfinity;
    }

    @Override
    public String getEngineName() {
        return "R (" + R.getClass() + ")";
    }

    public static RMathExpression NewInstance(String name) {
        return (RMathExpression) MathExpression.NewInstance(RMathExpression.class, name);
    }

    @Override
    public synchronized List<String> listVariables(boolean includeGlobal, boolean includeShadow) {
        List<String> lsv = new LinkedList<String>();
        String[] ls = R.ls();
        for (String s : ls) {
            try {
                boolean rexp = (boolean) R.eval("is.function(" + s + ")");
                if (!rexp) {
                    if (includeGlobal || !globalVariables.contains(s)) {
                        if (includeShadow || !s.startsWith(".")) {
                            lsv.add(s);
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return lsv;
    }

    @Override
    public String toString() {
        return getEngineName() + (serverConf == null ? "?" : serverConf.toString());
    }

}
