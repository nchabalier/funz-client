package org.funz.api;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.funz.Constants;
import static org.funz.StarterMonitor.POOL;
import org.funz.conf.Configuration;
import org.funz.log.Log;
import static org.funz.log.LogTicToc.HMS;
import org.funz.run.CalculatorsPool;
import org.funz.run.Computer;
import org.funz.script.MathExpression;
import org.funz.script.RMathExpression;
import org.funz.util.ASCII;
import org.math.R.Rsession;

/**
 *
 * @author richet
 */
public class Funz_v1 extends Funz {

    public static Configuration CONF;
    public static CalculatorsPool POOL;
    public static MathExpression MATH;

    public static void init() {
        Funz.init();
        init(null, null, null);
    }

    public static void init(Configuration conf, CalculatorsPool pool, MathExpression math) {
        Log.out("Funz init: "+HMS(),0);
        try {
            //tic("Verbose");
            //First, try to read form System.getProperty (which overloads everything)
            String verboselevel_property = System.getProperty("verbosity");
            if (verboselevel_property != null && verboselevel_property.length() > 0) {
                try {
                    setVerbosity(Integer.parseInt(verboselevel_property));
                } catch (NumberFormatException nfe) {
                    nfe.printStackTrace();
                    setVerbosity(10);
                }
            } else { // else use Configuration by default
                verboselevel_property = Configuration.getProperty("verbosity", "1");
                try {
                    setVerbosity(Integer.parseInt(verboselevel_property));
                } catch (NumberFormatException nfe) {
                    nfe.printStackTrace();
                    setVerbosity(10);
                }
            }

            Log.out("Verbose level set to " + Log.level, 1);
            //toc("Verbose");

            //tic("APP_USER_DIR");
            if (Constants.APP_USER_DIR.exists()) {
                if (!Constants.APP_USER_DIR.canWrite()) {
                    Log.err("Impossible to write in " + Constants.APP_USER_DIR.getAbsolutePath(), 0);
                    System.exit(-1);
                }
            } else {
                boolean success = Constants.APP_USER_DIR.mkdir();
                if (!success) {
                    Log.err("Impossible to create " + Constants.APP_USER_DIR.getAbsolutePath(), 0);
                    System.exit(-1);
                } else if (!Constants.APP_USER_DIR.exists() || !Constants.APP_USER_DIR.canWrite()) {
                    Log.err("Impossible to write in " + Constants.APP_USER_DIR.getAbsolutePath(), 0);
                    System.exit(-1);
                }
            }
            //toc("APP_USER_DIR");

            //tic("USER_TMP_DIR");
            if (!Constants.USER_TMP_DIR.exists()) {
                boolean success = Constants.USER_TMP_DIR.mkdirs();
                if (!success) {
                    Log.err("Impossible to create " + Constants.USER_TMP_DIR.getAbsolutePath(), 0);
                    System.exit(-1);
                }
            }
            //toc("USER_TMP_DIR");

            //tic("installation");
            Log.out("Funz installation directory: " + Constants.APP_INSTALL_DIR, 1);
            //toc("installation");

            //tic("quota");
            try {
                if (conf == null) {
                    CONF = new Configuration(new File(Constants.APP_INSTALL_DIR, "quotas.hex"), Log.Collector);
                } else {
                    CONF = conf;
                }
                Log.out("Funz configuration: "+CONF.toString(), 1);
            } catch (Exception e) {
                Log.err("Funz configuration exception: " + e.getMessage(), 0);
            }
            //toc("quota");

            //tic("models");
            //Log.out("Supported models: " + Configuration.getModels(), 0);
            MODELS = conf.getModels().toArray(new String[conf.getModels().size()]);
            //toc("models");

            //tic("designs");
            //Log.out("Supported designs: " + conf.getDesigners(), 0);
            DESIGNS = conf.getDesigners().toArray(new String[conf.getDesigners().size()]);
            //toc("designs");

            //tic("properties");
            try {
                Log.out("Reading properties", 1);
                Configuration.readProperties("file:" + Constants.APP_INSTALL_DIR +"/"+ Configuration.properties);
                Log.out("  file:" + Constants.APP_INSTALL_DIR +"/"+ Configuration.properties, 2);
            } catch (Exception e) {
                Log.err("Exception: " + e.getMessage(), 0);
            }
            //toc("properties");
            
            //tic("math");
            if (math == null) {
                Log.out("Initializing math. engine...", 1);
                String Rname = "Funz_" + Configuration.timeDigest();
                MATH = new RMathExpression(Rname, File.createTempFile(Rname, ".log"));
            } else {
                MATH = math;
            }
            Log.out("Using math. engine " + MATH.getEngineName(), 1);

            MathExpression.SetDefaultInstance(MATH);
            Log.out("Default Math engine is " + MathExpression.GetDefaultInstance().toString(), 1);
            //toc("math");

            //tic("pool");
            if (POOL == null) {
                if (pool == null) {
                    Log.out("Initializing calc. engine...", 1);
                    POOL = new CalculatorsPool(CalculatorsPool.getFreePort(true));
                    Log.out("                            Done. ", 1);
                } else {
                    POOL = pool;
                    Log.out("Using calc. engine " + POOL.toString(), 1);
                }
            }
            //toc("pool");

            //tic("Headless");
            Log.out("Headless mode: " + GraphicsEnvironment.isHeadless(), 1);
            //toc("Headless");
            
            Log.out("  Funz models (port "+POOL.getPort()+"): "+ASCII.cat(" ", MODELS), 0);
            Log.out("  Funz designs (engine "+MATH.getEngineName()+"): "+ASCII.cat(" ", DESIGNS), 0);
        } catch (Exception e) {
            e.printStackTrace();
            Log.err("Error while initializing Funz", -1);
            Log.err(e, -1);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        if (POOL != null) {
            POOL.setRefreshing(false, this, "finalize Funz_v1");
        }
        super.finalize(); //To change body of generated methods, choose Tools | Templates.
    }

    public static void setVerbosity(int l) {
        Funz.setVerbosity(l);
    }

    /**
     * Get the code corresponding to this "model" key
     *
     * @param model
     * @return the code name
     */
    public static String ModelToCode(String model) {
        return Configuration.getCode(model);
    }

    public static Map<String, String[]> getCalculatorsInformation() {
        Object dummy = new Object();
        if (!POOL.isRefreshing()) {
            POOL.setRefreshing(true, dummy, "Funz.getCalculatorsInformation");
            try {
                Thread.sleep(5000); // to let enough time to actualize grid
            } catch (InterruptedException ex) {
            }
        }
        Map<String, String[]> info = new HashMap<String, String[]>();
        synchronized (POOL) {
            for (Iterator<Computer> i = POOL.getComputers().iterator(); i.hasNext();) {
                Computer c = i.next();
                //for (Computer c : POOL.getComputers()) {
                String[] infos = new String[]{c.os, "" + c.use, c.activity, c.codeList};
                info.put(c.name + "(" + c.host + ":" + c.port + ")", infos);
            }
        }
        POOL.setRefreshing(false, dummy, "end Funz.getCalculatorsInformation");
        return info;
    }
}
