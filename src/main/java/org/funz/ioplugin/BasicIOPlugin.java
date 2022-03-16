package org.funz.ioplugin;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.funz.Constants;
import org.funz.log.Log;
import org.funz.log.LogCollector.SeverityLevel;
import org.funz.parameter.OutputFunctionExpression;
import org.funz.parameter.SyntaxRules;
import org.funz.script.MathExpression;
import org.funz.script.ParseExpression;
import org.funz.script.RMathExpression;
import org.funz.util.Data;
import org.math.io.parser.ArrayString;

/**
 * Simplified IOPlugin implementation based on properties file. Following
 * content is supported: variableStartSymbol= variableLimit= formulaStartSymbol=
 * formulaLimit= commentLineChar= datasetFilter=contains("*","DEBUT_MOR")
 * keywords.BLUE=toto titi tata keywords.RED= keywords.GREEN=
 * information=blablabla ! links=http://toto.doc http//sdfsf outputlist=z y #
 * output.z.if=true output.z.get=property("*.out","z")
 * output.z.default=Double.NaN
 * output.y.get=grep("*.listing",DEFAULT_FUNCTION_NAME)
 * output.y.if=contains("*","ASK_Y")
 *
 * @author richet
 */
public class BasicIOPlugin extends ExtendedIOPlugin {

    Properties _properties = new Properties();

    public BasicIOPlugin newInstance() {
        try {
            BasicIOPlugin newone = new BasicIOPlugin(source);
            newone.setID(_id);
            return newone;
        } catch (Exception e) {
            return null;
        }
    }

    public BasicIOPlugin(String urlstr) throws IOException {
        Log.out("Instanciating BasicIOPlugin from " + urlstr, 5);
        setSyntax = true;
        if (urlstr.startsWith("file:.")) {
            urlstr = urlstr.replace("file:.", Constants.APP_INSTALL_DIR.toURI().toString());
        }
        source = urlstr;
        setID(source.substring(source.lastIndexOf('/') + 1, source.lastIndexOf(IOPluginsLoader.BASIC_EXTENSION)));

        try {
            URL url = new URL(urlstr);
            _properties.load(url.openStream());
        } catch (Exception e) {
            Log.err(e, 1);
            Log.err("Could not load " + urlstr + " from " + new File(".").getAbsolutePath(), 1);
            throw new IOException("Could not access to plugin file " + urlstr);
        }

        /*System.out.println("BasicIOPlugin "+urlstr);
         for (String s : _properties.stringPropertyNames()) {
         System.out.println(s+" -> "+_properties.getProperty(s));
         }*/
        variableStartSymbol = SyntaxRules.readStartSymbol(_properties.getProperty("variableStartSymbol"));
        variableLimit = SyntaxRules.readLimits(_properties.getProperty("variableLimit"));
        formulaStartSymbol = SyntaxRules.readStartSymbol(_properties.getProperty("formulaStartSymbol"));
        formulaLimit = SyntaxRules.readLimits(_properties.getProperty("formulaLimit"));

        //System.out.println(variableStartSymbol+" "+variableLimit+" "+formulaStartSymbol+" "+formulaLimit);
        if (_properties.containsKey("commentLineChar") && _properties.getProperty("commentLineChar").length() > 0) {
            commentLine = _properties.getProperty("commentLineChar");
        }

        if (_properties.containsKey("information") && _properties.getProperty("information").length() > 0) {
            information = _properties.getProperty("information").trim();
        }

        if (_properties.containsKey("links") && _properties.getProperty("links").length() > 0) {
            doc_links = _properties.getProperty("links").split(" ");
        }

        if (_properties.containsKey("roundOff") && _properties.getProperty("roundOff").length() > 0) {
            _roundoff = Integer.parseInt(_properties.getProperty("roundOff").trim());
        }
    }

    @Override
    public LinkedList<OutputFunctionExpression> suggestOutputFunctions() {
        LinkedList<OutputFunctionExpression> ofl = super.suggestOutputFunctions();
        if (_properties.containsKey("outputfunctions") && _properties.getProperty("outputfunctions").length() > 0) {
            for (String o : _properties.getProperty("outputfunctions").split(" ")) {

                OutputFunctionExpression of = OutputFunctionExpression.read(o);
                boolean ok = true;
                for (String expr : of.getParametersExpression()) {
                    try {
                        Object res = MathExpression.Eval(expr, getVoidOutput());
                        if (res == null) {
                            ok = false;
                        }
                    } catch (Exception e) {
                        Log.logException(false,e);
                        if (Log.level>=10) e.printStackTrace();
                        ok = false;
                    }
                }
                if (ok) {
                    ofl.add(OutputFunctionExpression.read(o));
                }
            }
        }
        return ofl;
    }

    boolean isExpression(String e) {
        return e.startsWith("`") && e.endsWith("`");
    }

    @Override
    public void setInputFiles(File[] inputfiles) {
        super.setInputFiles(inputfiles);

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put(ParseExpression.FILES, _inputfiles);

        if (_properties.containsKey("outputlist") && _properties.getProperty("outputlist").length() > 0) {
            Log.logMessage("BasicIOPlugin " + getID(), SeverityLevel.INFO, false, "  outputlist=" + _properties.getProperty("outputlist"));
            
            List<String> outputlist = new LinkedList<String>();
            Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(_properties.getProperty("outputlist"));
            while (m.find()) outputlist.add(m.group(1)); 
            for (String o : outputlist) {
                if (isExpression(o)) {
                    Object eval = ParseExpression.eval(o, params);
                    if (eval != null) {
                        if (eval instanceof String) {
                            _output.put(eval.toString(), "?");
                        } else if (eval instanceof String[]) {
                            String[] evals = (String[]) eval;
                            for (String e : evals) {
                                _output.put(e, "?");
                            }
                        } else if (eval instanceof List) {
                            List evals = (List) eval;
                            for (Object e : evals) {
                                _output.put(e.toString(), "?");
                            }
                        } else {
                            Log.logMessage("BasicIOPlugin " + getID(), SeverityLevel.INFO, false, "  output name expression " + o + " not evaluable:" + eval);
                        }
                    }
                } else {
                    Object test = true;
                    if (_properties.containsKey("output." + o + ".if")) {
                        String ifprop = _properties.getProperty("output." + o + ".if");
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
                        if (_properties.containsKey("output." + o + ".default")) {
                            try {
                                _output.put(o, MathExpression.Eval(_properties.getProperty("output." + o + ".default"), null));
                            } catch (Exception ex) {
                                _output.put(o, "?");
                                Log.logException(true, ex);
                            }
                        } else {
                            _output.put(o, "?");
                        }
                        Log.logMessage("BasicIOPlugin " + getID(), SeverityLevel.INFO, false, "  " + o + "=" + _output.get(o));
                    } else {
                        Log.logMessage("BasicIOPlugin " + getID(), SeverityLevel.INFO, false, "  " + o + " not available");
                    }
                }
            }
        }

        //_output.put("output_lines", new String[0]);
        //_output.put("out", "");
        //_output.put("err", "");
    }

    @Override
    public Map<String, Object> readOutput(File outdir) {
        Map<String, Object> lout = super.readOutput(outdir);
        //lout.put(ParseExpression.FILES, outdir.listFiles());

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put(ParseExpression.FILES, outdir.listFiles());

        for (String o : _output.keySet()) {
            if (!lout.containsKey(o)) {
                if (!_properties.containsKey("output." + o + ".get")) {
                    if (_properties.containsKey("output.???.get")) {
                        Object oe = ParseExpression.eval(o, params);
                        if (oe != null) {
                            String oes = oe.toString();
                            try {
                                lout.put(oes, ParseExpression.eval(_properties.getProperty("output.???.get").replace("???", oes), params));
                            } catch (Exception e) {
                                lout.put(oes, null);
                                Log.logMessage("BasicIOPlugin " + getID(), SeverityLevel.ERROR, false, "Impossible to get output: " + o + " , failed to use 'output.???.get' key defined in " + _id);
                                if (Log.level>=10) e.printStackTrace();
                            }
                        }
                    } else {
                        lout.put(o, null);
                        Log.logMessage("BasicIOPlugin " + getID(), SeverityLevel.ERROR, false, "Impossible to get output: " + o + " , no 'output." + o + ".get' key defined in " + _id);
                    }
                } else {
                    try {
                        lout.put(o, ParseExpression.eval(_properties.getProperty("output." + o + ".get"), params));
                    } catch (Exception e) {
                        lout.put(o, null);
                        Log.logMessage("BasicIOPlugin " + getID(), SeverityLevel.WARNING, false, "Impossible to eval output: " + o + " with plugin " + _id);
                        if (Log.level>=10) e.printStackTrace();
                    }
                }
            }
        }

        return lout;
    }

    @Override
    public boolean acceptsDataSet(File f) {
        if (_properties.containsKey("datasetFilter") && _properties.getProperty("datasetFilter").length() > 0) {
            String expr = _properties.getProperty("datasetFilter");

            HashMap<String, Object> params = new HashMap<String, Object>();
            params.put(ParseExpression.FILES, new File[]{f});
            Object test = false;
            test = ParseExpression.eval(expr, params);
            if (test == null) {
                test = false;
            }

            if (test instanceof Boolean && (Boolean) test) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    // This is the test entry point. 1st arg is ioplugin path, next are test case directories.
    public static void main(String[] args) throws Exception {
        Log.level = 10;
        //MathExpression.SetDefaultInstance(RMathExpression.NewInstance("BasicIOPlugin")); not required: will just output "No instance ..." messages
        String errors = "";
        System.out.println("{\n\"class\": \"BasicIOPlugin\",");
        System.out.println("\"arg\": \"" + args[0] + "\",");
        for (int i = 1; i < args.length; i++) {
            String a = args[i];

            System.out.println("\"test\": {");
            System.out.println("  \"name\": \"" + a + "\",");

            File dir = new File(a);
            if (!dir.exists() || !dir.isDirectory()) {
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
                    BasicIOPlugin plugin = new BasicIOPlugin(args[0]);
                    File inputfile = new File(input[0], dir.getName());
                    System.out.println("  \"methods\": {");
                    System.out.println("    \"acceptsDataSet\": \"" + plugin.acceptsDataSet(inputfile) + "\",");
                    plugin.setInputFiles(input[0].listFiles());
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
                            System.out.print("        \"" + ok );
                            Object o = out.get(ok);
                            String ostr = Data.asString(o);
                            String res = "FAILED:";
                            String ref = infos.getProperty("output." + ok);
                            if (ref!=null && ostr.equals(ref)) {
                                res = "OK";
                            } else {
                                res = res + " " + ostr + " != " + ref;
                            }
                            if (!res.equals("OK")) {
                                errors = errors + "\n" + a + ": " + res;
                            }
                            System.out.println("\": \"" + res + "\",");
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

    @Override
    public void setCommentLine(String c) {
        commentLine = c;
    }
}
