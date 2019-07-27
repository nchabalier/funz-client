/*
 * Created on 31 mai 06 by richet
 */
package org.funz.ioplugin;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.funz.Project;
import org.funz.conf.Configuration;
import org.funz.log.Log;
import org.funz.log.LogCollector.SeverityLevel;
import org.funz.parameter.InputFile;
import org.funz.parameter.OutputFunctionExpression;
import org.funz.parameter.SyntaxRules;
import org.funz.parameter.VariableMethods;
import org.funz.script.MathExpression;
import org.funz.script.ParseExpression;
import org.funz.script.RMathExpression;
import org.funz.util.ASCII;
import org.funz.util.Data;
import static org.funz.util.Data.asString;
import org.funz.util.Disk;
import org.math.io.parser.ArrayString;

public class ExtendedIOPlugin implements IOPluginInterface {

    protected String _id = "not set";
    protected String source = null;
    protected File[] _inputfiles;
    protected HashMap<String, Object> _output = new HashMap<String, Object>();
    protected String commentLine;
    public String[] doc_links = {};
    protected int formulaLimit = SyntaxRules.LIMIT_SYMBOL_BRACKETS;
    protected int formulaStartSymbol = SyntaxRules.START_SYMBOL_AT;
    public String information = "Generic default plugin";
    protected int variableLimit = SyntaxRules.LIMIT_SYMBOL_SQ_BRACKETS;
    protected int variableStartSymbol = SyntaxRules.START_SYMBOL_DOLLAR;
    MathExpression mathengine;

    @Override
    public void finalize() throws Throwable {
        if (mathengine != null && mathengine != MathExpression.GetDefaultInstance()) {
            ((RMathExpression) mathengine).finalizeRsession();
        }
        super.finalize();
    }

    public ExtendedIOPlugin() {
        setSyntax = true;
    }

    @Override
    public void setFormulaInterpreter(MathExpression e) {
        mathengine = e;
    }

    @Override
    public MathExpression getFormulaInterpreter() {
        if (mathengine == null) {
            String name = "NullProject";
            if (getProject() != null) {
                name = getProject().getName();
            }
            File logdir = new File(System.getProperty("java.io.tmpdir"));
            if (getProject() != null) {
                logdir = getProject().getLogDir();
            }
            mathengine = new RMathExpression(name, Configuration.isLog("R") ? new File(logdir, name + ".Rlog") : null);
        }
        return mathengine;
    }

    @Override
    public boolean acceptsDataSet(File f) {
        return false;
    }

    @Override
    public boolean acceptsDataDirectory(File f) {
        return false;
    }

    @Override
    public String getCommentLine() {
        return commentLine;
    }

    @Override
    public String getSource() {
        return source;
        //return Constants.APP_INSTALL_DIR.toURI().getPath()+'/'+Constants.PLUGINS_DIR+'/'+Constants.DOE_SUBDIR+'/'+ _id+IOPluginsLoader.BASIC_EXTENSION;
    }

    @Override
    public int getFormulaLimit() {
        return formulaLimit;
    }

    @Override
    public int getFormulaStartSymbol() {
        return formulaStartSymbol;
    }

    @Override
    public File[] getInputFiles() {
        return _inputfiles;
    }

    public String getPluginInformation() {
        return information;
    }

    @Override
    public LinkedList<File> getRelatedFiles(File f) {
        LinkedList<File> list = new LinkedList<File>();
        list.add(f);
        return list;
    }

    @Override
    public int getVariableLimit() {
        return variableLimit;
    }

    @Override
    public int getVariableStartSymbol() {
        return variableStartSymbol;
    }

    @Override
    public HashMap<String, Object> getVoidOutput() {
        return _output;
    }

    @Override
    public String[] getOutputNames() {
        String[] n = new String[_output.size()];
        int i = 0;
        for (String o : _output.keySet()) {
            n[i++] = o;
        }

        return n;
    }

    @Override
    public Map<String, Object> readOutput(File outdir) {
        HashMap<String, Object> lout = new HashMap<String, Object>();
        // this is needed to fill lout with null objects by default. Instead, previous objects should be used by mathengine instead of null ones
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put(ParseExpression.FILES, outdir.listFiles());
        for (String o : _output.keySet()) {
            if (_more_properties.containsKey("output." + o + ".get")) {
                try {
                    lout.put(o, ParseExpression.eval(_more_properties.getProperty("output." + o + ".get"), params));
                } catch (Exception e) {
                    e.printStackTrace();
                    lout.put(o, null);
                    Log.logMessage("Extended IOPlugin " + getID(), SeverityLevel.WARNING, false, "Impossible to eval output: " + o + " with plugin " + _id);
                }
            }
            /*else {
             lout.put(o, null);
             Log.logMessage("Extended IOPlugin " + getID(), SeverityLevel.ERROR, false, "Impossible to get output: " + o + " , no 'output." + o + ".get' key defined in " + _id);
             }*/
        }

        return lout;
    }

    public LinkedList<OutputFunctionExpression> suggestOutputFunctions() {
        LinkedList<OutputFunctionExpression> ofl = new LinkedList<OutputFunctionExpression>();
        return ofl;
    }

    @Override
    public void setID(String fn) {
        _id = fn;
    }

    @Override
    public String getID() {
        return _id;
    }

    Properties _more_properties = new Properties();

    @Override
    public void setInputFiles(File... inputfiles) {
        _inputfiles = inputfiles;

        _output.clear();
        //_output.put("output_lines", new String[0]);
        for (File io : inputfiles) {
            if (io.getName().endsWith("ioplugin")) {
                try {
                    _more_properties.load(new FileReader(io));//new FileReader(io));
                    Log.out("more_properties: " + _more_properties, 4);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        //_output.put("out", "");
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put(ParseExpression.FILES, _inputfiles);
        // _output.put("err", "");
        if (!_more_properties.isEmpty()) {
            Log.out("Found more ioplugin:" + _more_properties, 3);
            if (_more_properties.containsKey("outputlist") && _more_properties.getProperty("outputlist").length() > 0) {
                Log.logMessage("Extended IOPlugin " + getID(), SeverityLevel.INFO, false, "  outputlist=" + _more_properties.getProperty("outputlist"));
                for (String o : _more_properties.getProperty("outputlist").split(" ")) {
                    Object test = true;
                    if (_more_properties.containsKey("output." + o + ".if")) {

                        String ifprop = _more_properties.getProperty("output." + o + ".if");
                        if (ifprop.equals("true")) {
                            test = true;
                        } else if (ifprop.equals("false")) {
                            test = false;
                        } else {
                            test = ParseExpression.eval(ifprop, params);
                            if (test == null) {
                                test = false;
                            }
                        }
                    }
                    if (test instanceof Boolean && (Boolean) test) {
                        if (_more_properties.containsKey("output." + o + ".default")) {
                            try {
                                _output.put(o, MathExpression.Eval(_more_properties.getProperty("output." + o + ".default"), null));
                            } catch (Exception ex) {
                                _output.put(o, "?");
                                Log.logException(true, ex);
                            }
                        } else {
                            _output.put(o, "?");
                        }
                        Log.logMessage("Extended IOPlugin " + getID(), SeverityLevel.INFO, false, "  " + o + "=" + _output.get(o));
                    } else {
                        Log.logMessage("Extended IOPlugin " + getID(), SeverityLevel.INFO, false, "  " + o + " not available");
                    }
                }
            }
        }
    }

    /*public void setOutputFunctionSuggestion(String FunctionOutputValueType, String[] functionOutputParameters) {
     if (outputFunctionSuggestion.containsKey(FunctionOutputValueType))
     outputFunctionSuggestion.remove(FunctionOutputValueType);
     outputFunctionSuggestion.put(FunctionOutputValueType, functionOutputParameters);
     //System.out.println("plugin "+getSource()+" : outputFunctionSuggestion<"+FunctionOutputValueType+"> = "+ASCII.cat(" ", functionOutputParameters));
     }*/
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getName()).append(" plugin from <").append(_id).append(">\n");
        sb.append("  input files: ").append(ASCII.cat(",", _inputfiles)).append("\n");
        sb.append("  default output: ").append(ASCII.cat("\n                  ", _output));
        sb.append("\n  syntax:").append("  comment = ").append(commentLine).append("  variable = ").append(SyntaxRules.START_SYMBOL_STRINGS[variableStartSymbol]).append(SyntaxRules.LIMIT_STRINGS[variableLimit]).append("  formula  = ").append(SyntaxRules.START_SYMBOL_STRINGS[formulaStartSymbol]).append(SyntaxRules.LIMIT_STRINGS[formulaLimit]).append("\n");
        /*if (_modeler != null) {
         sb.append("  modeler  = ").append(_modeler).append("\n");
         }*/
        return sb.toString();
    }
    int _roundoff = VariableMethods.NO_ROUND_OFF;

    @Override
    public int getRoundOff() {
        return _roundoff;
    }

    static List<File> recursiveListFiles(File root, FileFilter ff) {
        List<File> ins = new LinkedList<File>();
        File[] in = root.listFiles(ff);
        for (File file : in) {
            if (file.isDirectory()) {
                ins.addAll(recursiveListFiles(file, ff));
            } else {
                ins.add(file);
            }
        }
        return ins;
    }
    static double tic;

    public static double tic() {
        double oldtic = tic;
        tic = Calendar.getInstance().getTimeInMillis();
        return oldtic;
    }

    public static double toc() {
        return Calendar.getInstance().getTimeInMillis();
    }

    // This is the test entry point. 1st arg is ioplugin path, next are test case directories.
    public static void main(String[] args) throws Exception {
        String errors = "";
        System.out.println("{\n\"class\": \"ExtendedIOPlugin\",");
        System.out.println("\"arg\": \"" + args[0] + "\",");
        for (int i = 1; i < args.length; i++) {
            String a = args[i];

            System.out.println("\"test\": {");
            System.out.println("  \"name\": \"" + a + "\",");

            File dir = new File(a);
            if (!dir.exists() ||!dir.isDirectory() ) {
                System.err.println("FAILED: " + a + " does no exists.");
                System.out.println("  \"status\": \"test directory NOT found\",");
            } else {
                System.out.println("  \"status\": \"test directory found\",");
            }

            File[] infotxt = dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.equals("info.txt");
                }
            });
            if (infotxt.length == 1) {
                System.out.println("  \"reference\": \"info.txt is available\",");
            } else {
                System.out.println("  \"reference\": \"NO info.txt available!!!\",");
            }

            if (dir.exists() && dir.isDirectory()) {
                File[] input = dir.listFiles(new FileFilter() {

                    public boolean accept(File f) {
                        if (f.exists() && f.isDirectory() && f.getName().equals("input")) {
                            return true;
                        }
                        return false;
                    }
                });
                if (input == null || input.length == 0) {
                    System.err.println("FAILED: no input directory");
                    System.out.println("  \"input\": \"no input directory\",");
                } else {
                    System.out.println("  \"input\": \"" + input[0].getAbsolutePath() + "\",");
                }

                File[] output = dir.listFiles(new FileFilter() {

                    public boolean accept(File f) {
                        if (f.exists() && f.isDirectory() && f.getName().equals("output")) {
                            return true;
                        }
                        return false;
                    }
                });
                if (output == null || output.length == 0) {
                    System.err.println("FAILED: no output directory");
                    System.out.println("  \"output\": \"no output directory\",");
                } else {
                    System.out.println("  \"output\": \"" + output[0].getAbsolutePath() + "\",");
                }

                if (input != null && input.length == 1 && output != null && output.length == 1) {
                    ExtendedIOPlugin plugin = null;
                    try {
                        Class pluginclass = Class.forName(args[0]);
                        plugin = (ExtendedIOPlugin) pluginclass.newInstance();
                    } catch (Exception ex) {
                        assert false:"Class " + args[0] + " not found:\n" + ex.getMessage();
                    }
                    
                    plugin.setProject(new Project(a));
             
                    File[] inputdirs = input[0].listFiles(new FileFilter() {
                        public boolean accept(File pathname) {
                            return pathname.isDirectory();
                        }
                    });
                    File[] inputfiles = input[0].listFiles(new FileFilter() {
                        public boolean accept(File pathname) {
                            return pathname.isFile();
                        }
                    });
                    InputFile[] prj_inputfiles = plugin.importFileOrDir(input[0]);
                    File[] prj_files = new File[prj_inputfiles.length];
                    for (int j = 0; j < prj_files.length; j++) {
                        prj_files[j] = prj_inputfiles[j].getFile();
                    }
                    
                    System.out.println("  \"methods\": {");
                    for (File inputfile : inputfiles) 
                         System.out.println("    \"acceptsDataSet\": \"" + plugin.acceptsDataSet(inputfile) + "\",");
                    for (File inputdir : inputdirs) 
                        System.out.println("    \"acceptsDataDirectory\": \"" + plugin.acceptsDataDirectory(inputdir) + "\",");
                    plugin.setInputFiles(prj_files);
                    System.out.println("    \"setInputFiles.output\": {");
                    for (String ok : plugin._output.keySet()) {
                        Object o = plugin._output.get(ok);
                        String ostr = Data.asString(o);
                        System.out.println("      \"" + ok + "\": \"" + ostr + "\",");
                    }
                    System.out.println("    },");

                    System.out.println("    \"suggestOutputFunctions\": [");
                    for (OutputFunctionExpression of : plugin.suggestOutputFunctions()) {
                        System.out.println("  \"" + of.toNiceSymbolicString() + "\",");
                    }
                    System.out.println("    ],");

                    System.out.println("    \"readOutput\": {");
                    Map<String, Object> out = plugin.readOutput(output[0]);
                    for (String ok : out.keySet()) {
                        Object o = out.get(ok);
                        String ostr = Data.asString(o);
                        System.out.println("        \"" + ok + "\": \"" + ostr + "\",");
                    }
                    System.out.println("    },");

                    if (infotxt.length == 1) {
                        Properties infos = new Properties();
                        try {
                            URL url = new URL("file:" + infotxt[0].getAbsolutePath());
                            infos.load(url.openStream());
                        } catch (IOException e) {
                            System.err.println("info.txt unreadable:\n" + e.getMessage());
                        }
                        System.out.println("    \"results\": {");
                        for (String ok : out.keySet()) {
                            Object o = out.get(ok);
                            String ostr = Data.asString(o);
                            String res = "FAILED:";
                            String ref = infos.getProperty("output." + ok);
                            if (ostr.equals(ref)) {
                                res = "OK";
                            } else {
                                res = res + " " + ostr + " != " + ref;
                            }
                            if (!res.equals("OK")) {
                                errors = errors + "\n" + a + ": " + res;
                            }
                            System.out.println("        \"" + ok + "\": \"" + res + "\",");
                        }
                        System.out.println("    },");
                    } else {
                        System.out.println("    \"results\": \"ERROR no reference found.\",");
                    }
                }
                System.out.println("  },");
            }
            System.out.println("},");
        }
        System.out.println("}");
        assert errors.length() == 0 : errors;
    }
    // ant compile dist; cd dist; zip -r ../../plugin-R/funz-client.zip *; cd ..
    
    public boolean setSyntax = false;

    @Override
    public void setCommentLine(String c) {
        if (!setSyntax && !c.equals(commentLine)) {
            Log.err("Operation not permitted: setCommentLine " + c, 1);
        } else {
            commentLine = c;
        }
    }

    @Override
    public void setFormulaSyntax(int s, int l) {
        if (!setSyntax && (s != formulaStartSymbol || l != formulaLimit)) {
            Log.err("Operation not permitted: setFormulaSyntax " + s + " " + l, 1);
        } else {
            formulaStartSymbol = s;
            formulaLimit = l;
        }
    }

    @Override
    public void setVariableSyntax(int s, int l) {
        if (!setSyntax && (s != formulaStartSymbol || l != formulaLimit)) {
            Log.err("Operation not permitted: setVariableSyntax " + s + " " + l, 1);
        } else {
            variableStartSymbol = s;
            variableLimit = l;
        }
    }
    Project _prj;

    public void setProject(Project p) {
        _prj = p;
    }

    public Project getProject() {
        return _prj;
    }

    /**
     * This method is called when a first launch failed previously. It is
     * supposed to process input files and change something that will help the
     * code to successfully run this tiume.
     *
     * @param inputDirectory "input" directory
     * @return try again if this case failed.
     */
    public boolean inputDirRetryPatch(File inputDirectory) {
        //As default behaviour, nothing is done.
        /*if (_prj.patchInputWhenFailed) {
         AskForEditInput ask = new AskForEditInput(inputDirectory);
         ask.setVisible(true);
         if (ask.doNotAskAgain()) {
         _prj.patchInputWhenFailed = false;
         }
         return ask.tryAgain();
         }*/
        return true;
    }

    @Override
    public InputFile[] importFileOrDir(File src) throws Exception {
        File trg = new File(_prj.getDirectory() + File.separator + Project.FILES_DIR + File.separator + src.getName());
        if (src.isDirectory()) {
            trg.mkdirs();
            Disk.copyDir(src, trg);
            List<InputFile> inputfiles = buildRecursiveInput(trg);
            if (inputfiles == null || inputfiles.size() == 0) {
                throw new IllegalArgumentException("Empty directory to import.");
            }
            return inputfiles.toArray(new InputFile[inputfiles.size()]);
        } else {
            Disk.copyFile(src, trg);
            InputFile fi = new InputFile(trg);
            return new InputFile[]{fi};
        }
    }

    public static List<InputFile> buildRecursiveInput(File root) {
        return buildRecursiveInput(root, new ArrayList<String>(0));
    }

    public static List<InputFile> buildRecursiveInput(File root, List<String> roots) {
        List<InputFile> list = new LinkedList<InputFile>();
        if (root.isDirectory()) {
            for (File children : root.listFiles()) {
                List<String> childrenroot = new ArrayList<String>(roots.size() + 1);
                for (String r : roots) {
                    childrenroot.add(r);
                }
                childrenroot.add(root.getName());
                list.addAll(buildRecursiveInput(children, childrenroot));
            }
        } else if (root.isFile()) {
            list.add(new InputFile(root, roots.toArray(new String[roots.size()])));
        }
        return list;
    }

}
