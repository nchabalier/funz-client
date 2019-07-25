package org.funz.api;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.funz.Project;
import org.funz.ProjectController;
import org.funz.conf.Configuration;
import org.funz.ioplugin.IOPluginInterface;
import org.funz.ioplugin.IOPluginsLoader;
import org.funz.log.Log;
import org.funz.log.LogFile;
import org.funz.script.RMathExpression;
import static org.funz.util.Data.*;

/**
 *
 * @author richet
 */
public class Utils {

    public static void main(String[] args) throws Exception {
        new Funz_v1();
        System.err.println(findVariables(null, new File[]{new File("src/main/resources/samples/branin.R")}));

        Map vv = new HashMap<String, String>();
        vv.put("x1", "0.5");
        vv.put("x2", "0.6");
        compileVariables("R", new File[]{new File("src/main/resources/samples/branin.R")}, vv, new File("."));
    }

    public static void compileVariables(String _model, File[] _input, Map vars_values, File outdir) throws Exception {
        String model;
        if (_model == null || _model.length() == 0) {
            model = Configuration.getModel(IOPluginsLoader.getFirstSuitablePlugin(_input[0]));
            if (model == null) {
                Log.err("Could not automatically select a model for " + _input[0] + " input file.", 0);
                throw new Exception("Failed to identify code for " + _input[0] + " input file.");
            } else {
                Log.out("Automatic selection of model " + model, 0);
            }
        } else {
            model = _model;
        }

        Log.out("Compiling for code " + model + " with input file " + Arrays.asList(_input) + " in " + outdir, 1);

        String name = _input[0].getName() + "_" + Configuration.timeDigest();
        IOPluginInterface plugin = IOPluginsLoader.newInstance(model, _input);
        Log.out("Using plugin " + plugin, 1);

        Project prj = ProjectController.createProject(name, _input[0], model, plugin);
        plugin.setFormulaInterpreter(new RMathExpression(_input[0].getName() + "_" + Configuration.timeDigest(), Configuration.isLog("R") ? new File(prj.getLogDir(), name + ".Rlog") : null));

        Log.out("Using variables " + asString(vars_values), 1);
        for (File _input1 : _input) {
            prj.compileFileIn(_input1, vars_values, outdir);
        }
    }

    public static Map findVariables(String _model, File[] _input) throws Exception {
        String model;
        if (_model == null || _model.length() == 0) {
            model = Configuration.getModel(IOPluginsLoader.getFirstSuitablePlugin(_input[0]));
            if (model == null) {
                Log.err("Could not automatically select a model for " + _input[0] + " input file.", 0);
                throw new Exception("Failed to identify code for " + _input[0] + " input file.");
            } else {
                Log.out("Automatic selection of model " + model, 0);
            }
        } else {
            model = _model;
        }

        Log.out("Parsing for code " + model + " with input files " + Arrays.asList(_input), 1);

        String name = _input[0].getName() + "_" + Configuration.timeDigest();
        String pluginname = model;
        if (pluginname==null) throw new Exception("No plugin for model "+model);
        IOPluginInterface plugin = IOPluginsLoader.newInstance(pluginname, _input);
        Log.out("Using plugin " + plugin, 1);

        Project prj = ProjectController.createProject(name, _input[0], model, plugin);
        plugin.setFormulaInterpreter(new RMathExpression(_input[0].getName() + "_" + Configuration.timeDigest(), Configuration.isLog("R") ? new File(prj.getLogDir(), name + ".Rlog") : null));

        Map<String, String> vv = new HashMap<String, String>();
        for (File _input1 : _input) {
            vv.putAll(prj.findInputVariables(_input1));
        }

        return vv;
    }

    public static Map readOutputs(String _model, File[] _input, File _outputdir) throws Exception {
        String model;
        if (_model == null || _model.length() == 0) {
            model = Configuration.getModel(IOPluginsLoader.getFirstSuitablePlugin(_input[0]));
            if (model == null) {
                Log.err("Could not automatically select a model for " + _input[0] + " input file.", 0);
                throw new Exception("Failed to identify code for " + _input[0] + " input file.");
            } else {
                Log.out("Automatic selection of model " + model, 0);
            }
        } else {
            model = _model;
        }

        Log.out("Parsing for code " + model + " with intput files " + Arrays.asList(_input), 1);

        String name = _input[0].getName() + "_" + Configuration.timeDigest();
        IOPluginInterface plugin = IOPluginsLoader.newInstance(model, _input);
        Log.out("Using plugin " + plugin, 1);

        Project prj = ProjectController.createProject(name, _input[0], model, plugin);
        plugin.setFormulaInterpreter(new RMathExpression(_input[0].getName() + "_" + Configuration.timeDigest(), Configuration.isLog("R") ? new File(prj.getLogDir(), name + ".Rlog") : null));

        Log.out("Setting input files " + Arrays.asList(_input), 1);
        plugin.setInputFiles(_input);

        return plugin.readOutput(_outputdir);
    }


    public static void delete(Object o) {
        o = null;
    }

    /**
     * Convenience method to unserailize data
     *
     * @param s String representation of data.
     * @return Data object
     */
    public static Object toObject(String s) {
        return asObject(s);
    }

    /**
     * Convenience method to serailize data
     *
     * @param o Data object to store
     * @return String
     */
    public static String toString(Object o) {
        return asString(o);
    }

    /*static void startCalculator(final int i) throws Exception {
        System.err.println("Start calculator " + i);
        File conf = new File("calculator.xml");
        assert conf.exists();
        final Calculator calc = new Calculator("file:calculator.xml", new LogFile("Calculator." + i + ".out"), new LogFile("Calculator." + i + ".log"));
        new Thread("Calculator " + i) {
            @Override
            public void run() {
                calc.run();
            }
        }.start();
        Thread.sleep(100);
    }*/

}
