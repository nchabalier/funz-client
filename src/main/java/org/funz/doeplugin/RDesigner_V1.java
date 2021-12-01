package org.funz.doeplugin;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.funz.conf.Configuration;
import org.funz.doeplugin.DesignConstants.Status;
import static org.funz.doeplugin.DesignHelper.BASE;
import static org.funz.doeplugin.Experiment.toString_ExperimentArray;
import org.funz.log.Alert;
import org.funz.log.Log;
import org.funz.log.LogCollector.SeverityLevel;
import org.funz.log.LogFile;
import org.funz.parameter.OutputFunctionExpression;
import org.funz.parameter.Parameter;
import org.funz.results.RendererHelper;
import org.funz.script.RMathExpression;
import org.funz.util.ASCII;
import org.funz.util.Data;
import org.funz.util.Disk;
import org.funz.util.URLMethods;
import org.math.R.RLog;
import org.math.R.RLog.Level;
import org.math.R.RserveSession;
import org.math.R.Rsession;
import org.math.array.DoubleArray;
import static org.math.array.DoubleArray.getColumnCopy;

/**
 *
 * @author richet
 */
public class RDesigner_V1 extends Designer {

    public static final String RCOMMENT = "#";
    private static final String KEY_DELIMITER = ":";
    public final String OPT_SEP = ";";
    public String urlstr;
    String name;
    String content;
    String quickhelp;
    String input;
    String output;
    String tags;
    String[] libraries;
    File Rsrc;
    File[] dependencies;

    public RDesigner_V1 newInstance() {
        return new RDesigner_V1(urlstr);
    }

    public RDesigner_V1(String urlstr) {
        this.urlstr = urlstr;
        name = urlstr.substring(urlstr.lastIndexOf("/") + 1, urlstr.lastIndexOf(".R")).replace("_", " ");
        content = URLMethods.readURL(urlstr);
        try {
            Rsrc = File.createTempFile(name, ".R");
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
        }
        ASCII.saveFile(Rsrc, content);
        //System.out.println("content=" + content);
        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line.startsWith(RCOMMENT)) {
                if (line.startsWith(RCOMMENT + "help" + KEY_DELIMITER)) {
                    //System.out.println("help line:" + line);
                    quickhelp = line.substring(line.indexOf(KEY_DELIMITER) + 1).trim();
                }
                if (line.startsWith(RCOMMENT + "input" + KEY_DELIMITER)) {
                    //System.out.println("input line:" + line);
                    output = line.substring(line.indexOf(KEY_DELIMITER) + 1).trim();
                }
                if (line.startsWith(RCOMMENT + "output" + KEY_DELIMITER)) {
                    //System.out.println("output line:" + line);
                    output = line.substring(line.indexOf(KEY_DELIMITER) + 1).trim();
                }
                if (line.startsWith(RCOMMENT + "tags" + KEY_DELIMITER)) {
                    //System.out.println("tags line:" + line);
                    tags = line.substring(line.indexOf(KEY_DELIMITER) + 1).trim();
                }
                if (line.startsWith(RCOMMENT + "options" + KEY_DELIMITER) || line.startsWith(RCOMMENT + "parameters" + KEY_DELIMITER)) {
                    //System.out.println("parameters line:" + line);
                    String[] options = line.substring(line.indexOf(KEY_DELIMITER) + 1).trim().split(OPT_SEP);
                    String[] parameters_defval = new String[options.length];
                    for (int i = 0; i < options.length; i++) {
                        String s = options[i];
                        if (s.contains("=")) {
                            String[] pv = s.split("=");
                            if (pv.length == 2) {
                                options[i] = pv[0].trim();
                                parameters_defval[i] = pv[1];
                                if (parameters_defval[i].contains("|")) {
                                    _optsModel.put(options[i], parameters_defval[i]);
                                    parameters_defval[i] = parameters_defval[i].substring(0, parameters_defval[i].indexOf('|')) + "'";
                                }
                            }
                        } else {
                            parameters_defval[i] = "";
                        }
                        _opts.put(options[i], parameters_defval[i]);
                    }
                }
                if (line.startsWith(RCOMMENT + "require" + KEY_DELIMITER) || line.startsWith(RCOMMENT + "library" + KEY_DELIMITER)) {
                    //System.out.println("require line:" + line);
                    libraries = line.substring(line.indexOf(KEY_DELIMITER) + 1).trim().split(OPT_SEP);
                }
                if (line.startsWith(RCOMMENT + "dependencies" + KEY_DELIMITER)) {
                    //System.out.println("require line:" + line);
                    String[] deps = line.substring(line.indexOf(KEY_DELIMITER) + 1).trim().split(OPT_SEP);
                    dependencies = new File[deps.length];
                    for (int i = 0; i < deps.length; i++) {
                        try {
                            dependencies[i] = File.createTempFile(deps[i], "");
                            URLMethods.downloadFile(urlstr.substring(0, urlstr.lastIndexOf("/") + 1) + deps[i], dependencies[i]);
                        } catch (Exception e) {
                            dependencies[i] = null;
                        }
                    }
                }
            }
        }
    }

    @Override
    public String getDesignOutputTitle() {
        return getName();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getQuickHelp() {
        return quickhelp;
    }

    @Override
    public String getType() {
        return tags;
    }

    @Override
    public String isValid(List<Parameter> params, OutputFunctionExpression f) {
        return VALID;
    }

    @Override
    public boolean viewManagedParams() {
        return true;
    }

    @Override
    public Design createDesign(DesignSession session) {
        return new RDesign(this, session);
    }

    public class RDesign extends Design {

        public Rsession R;
        double[] min, max;
        String[] Xnames;
        String[] Xynames;
        int currentiteration = -1;
        RMathExpression RME;

        public String getStatus() {
            return "["+currentiteration+"]";
        }

        public RDesign(Designer d, DesignSession ds) {
            super(d, ds);
            if (libraries != null && libraries.length > 0 && (RMathExpression.GetEngineName().contains("R2js") || RMathExpression.GetEngineName().contains("Renjin"))) {
                Alert.showInformation("Default math engine is R2js/Renjin but Designer "+getName()+" requires some libraries. You should use Rserve to avoid incompatibility.");
            }

            RME = RMathExpression.NewInstance(name); // need to do that, because if RME is not explcitely referenced in this RDesign, it will be garbaged soon !!!
            R = RME.R;
            
            R.note_header("Design " + d.getName() + " for " + ds.getFixedParameters(), System.getProperty("user.name"));

            if (Configuration.isLog("RDesigner")) {
                final LogFile log = new LogFile(new File(prj.getLogDir()/*_repository*/, name + "_" + Data.asString(ds.getFixedParameters()) + ".Rlog"));
                R.addLogger(new RLog() {

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
                });
            }
        }

        @Override
        public void endDesign() {
            if (R!=null) R.end();
            super.endDesign();
        }

        @Override
        public void init(File r) throws Exception {
            super.init(r);

            R.note_text("# Initialization of session in directory " + _repository);

            if (_parameters.length == 0) {
                Log.logException(true, new Exception("No parameter to design..."));
                R.log("No parameter to design...", Level.ERROR);
                return;
            }

            Xnames = new String[_parameters.length/*+1*/];
            int i = 0;
            for (Parameter p : _parameters) {
                assert p.isContinuous();
                Xnames[i++] = p.getName();
            }

            if (libraries != null && libraries.length > 0) {
                R.note_text("Load libraries");
                for (String lib : libraries) {
                    lib = lib.trim();
                    if (lib.length() > 0) {
                        try {
                            String loadmsg = "?";
                            if (lib.contains(":")) {
                                String src = lib.substring(0, lib.indexOf(":"));
                                String n = lib.substring(lib.lastIndexOf("/") + 1);
                                String path = lib.substring(lib.indexOf(":") + 1, lib.lastIndexOf("/"));
                                if (!R.isPackageInstalled(n, null)) {
                                    R.eval("devtools::install_" + src + "('" + path + "/" + n + "')", true);
                                }
                                loadmsg = R.loadPackage(n);
                                if (loadmsg.equals(Rsession.PACKAGELOADED)) {
                                    Log.logMessage(Rsrc.getName(), SeverityLevel.INFO, true, "Loading package " + n + ": " + loadmsg + " (version " + R.eval("packageDescription('" + n + "')$Version") + ")");
                                } else {
                                    Log.logMessage(Rsrc.getName(), SeverityLevel.WARNING, true, "Loading package " + n + ": " + loadmsg);
                                    throw new Exception(loadmsg);
                                }
                            } else {
                                loadmsg = R.installPackage(lib, true);
                                if (loadmsg.equals(Rsession.PACKAGELOADED) || loadmsg.equals(Rsession.PACKAGEINSTALLED)) {
                                    Log.logMessage(Rsrc.getName(), SeverityLevel.INFO, true, "Installing & loading package " + lib + ": " + loadmsg + " (version " + R.eval("packageDescription('" + lib + "')$Version") + ")");
                                } else {
                                    Log.logMessage(Rsrc.getName(), SeverityLevel.WARNING, true, "Installing & loading package " + lib + ": " + loadmsg);
                                    throw new Exception(loadmsg);
                                }
                            } 
                        } catch (Exception e) {
                            if (initFailedMsg != null) {
                                initFailedMsg = initFailedMsg + "\n" + e.getMessage();
                            } else {
                                initFailedMsg = e.getMessage();
                            }
                            Alert.showError(getName() + ".init: Could not load required libraries\n" + e);
                            R.note_text("Error: " + getName() + ".init: Could not load required libraries\n" + e);
                            throw e;
                        }
                    }
                }
            }

            if (dependencies != null) {
                R.note_text("Setup dependencies");
                for (int j = 0; j < dependencies.length; j++) {
                    try {
                        R.note_code("# import " + dependencies[j]);
                        if (R instanceof RserveSession) {
                            ((RserveSession) R).putFile(dependencies[j]);
                        }
                        Log.logMessage(Rsrc.getName(), SeverityLevel.INFO, true, "upload " + "R:>" + R.getLastLogEntry() + "; R!>" + R.getLastError());
                    } catch (Exception e) {
                        Log.err(e, 1);
                        Alert.showError(getName() + ".init: Could not load required dependencies\n" + e);
                        R.note_text("Error: " + getName() + ".init: Could not load required dependencies\n" + e);
                        throw e;
                    }
                }
            }

            R.note_text("Setup R designer file");
            R.source(Rsrc);
            Log.logMessage(Rsrc.getName(), SeverityLevel.INFO, true, "source " + "R:>" + R.getLastLogEntry() + "; R!>" + R.getLastError());
            //System.err.println("source " + "R:>" + R.getLastLogEntry() + "\nR!>" + R.getLastError());

            double[][] Xbounds = DesignHelper.getBounds(_parameters);
            min = getColumnCopy(Xbounds, 0);
            max = getColumnCopy(Xbounds, 1);
            try {
                R.note_text("Setup parameters bounds");
                R.set("Xbounds", DoubleArray.transpose(Xbounds), Xnames);
                R.voidEval("unorm = function(X,b) {Xun = X\nfor (i in 1:ncol(X)) {\nXun[,i]=X[,i]*(b[2,i]-b[1,i]) + b[1,i]\n}\nreturn(Xun)\n}");

                if (_session != null && _session.getFixedParameters() != null) {
                    for (String k : _session.getFixedParameters().keySet()) {
                        R.set(k, _session.getFixedParameters().get(k));
                    }
                }

                if (getOptions() != null) {
                    R.note_text("Setup design options");
                    for (String parameter : getOptions().keySet()) {
                        //R.log("[i] setting option: "+ parameter + " <- "+getOption(parameter),Level.INFO);
                        if (getOption(parameter) != null) {
                            if (getOption(parameter).startsWith("file:")) {
                                try {
                                    String fname = getOption(parameter).substring(getOption(parameter).lastIndexOf(File.separator) + 1);
                                    if (R instanceof RserveSession) {
                                        ((RserveSession) R).putFile(new File(new URI(getOption(parameter).trim())), fname);
                                    }
                                    R.set(parameter, fname);
                                } catch (Exception ex) {
                                    Log.err(ex, 1);
                                    R.set(parameter, getOption(parameter));
                                }
                            }
                        }

                        R.set(parameter, getOption(parameter));
                    }
                }
            
                R.note_text("Create R designer environment");
                Log.logMessage(Rsrc.getName(), SeverityLevel.INFO, true, "init: " + R.voidEval("DesignEnv <- " + _designer.getName() + "(.GlobalEnv)"));
                Log.logMessage(Rsrc.getName(), SeverityLevel.INFO, true, "R:>" + R.getLastLogEntry() + "; R!>" + R.getLastError());
                
                R.savels(new File(_repository.getAbsolutePath(), getName().replace(' ', '_') + getStatus() + ".Rdata"), "");//(currentiteration) );
            } catch (Rsession.RException e) {
                Log.err(e, 1);
                Alert.showError(getName() + ".init: Could not initialize design\n" + e);
                R.note_text(getName() + ".init: Could not initialize design\n" + e);
                throw e;
            }
        }
        String initFailedMsg;

        @Override
        public synchronized Status getInitialDesign(List<Experiment> experimentsToAppendInQueue) {
            R.note_text("# Build initial design");

            if (initFailedMsg != null) {
                R.note_text("Error: " + initFailedMsg);
                return new Status(Decision.ERROR, initFailedMsg);
            }

            if (_parameters==null || _parameters.length<=0) {
                return new Status(Decision.ERROR, "No input parameters setup for Design !");
            }

            try {
                currentiteration = 0;

                double[][] X0 = null;
                try {
                    String Rinput = "list(";
                    for (Parameter p : _parameters) {
                        Rinput = Rinput + "\"" + p.getName() + "\"=list('min'=" + p.getLowerBound() + ",'max'=" + p.getUpperBound() + "),";
                    }
                    Rinput = Rinput.substring(0, Rinput.length() - 1) + ")";
                    String Routput = "\"" + _designer.getOutputFunctionExpression().toNiceSymbolicString().replace("\"","'") + "\"";
                    X0 = R.asMatrix(R.eval("getInitialDesign(DesignEnv," + Rinput + "," + Routput + ")"));
                } catch (Exception r) {
                    Log.err(r, 1);
                    return new Status(Decision.ERROR, r.getMessage() + "\nR> " + R.getLastLogEntry() + "\nR!> " + R.getLastError());
                }

                //V1 plugin are expected to scale themselve: List<Experiment> exps = DesignHelper.createExperiments(scaleParametersWithBounds(X0), _parameters, prj);
                List<Experiment> exps = DesignHelper.createExperiments(X0, _parameters, prj);
                experimentsToAppendInQueue.addAll(exps);

                R.set("X0", X0, Xnames);

                if (!_repository.isDirectory()) {
                    _repository.mkdirs();
                }
                R.savels(new File(_repository.getAbsolutePath(), getName().replace(' ', '_') + getStatus() + ".Rdata"), "");//(currentiteration) );

                Status s = new Status(Decision.READY_FOR_NEXT_ITERATION);
                R.note_text(s.getMessage());
                return s;
            } catch (Exception e) {
                Log.err(e, 1);
                String m = e.getMessage();
                try {
                    m = m + "\nR: " + R.getLastLogEntry() + " !" + R.getLastError();
                } catch (Exception ex) {
                    Log.err(ex, 1);
                    m = m + "\nR: ?";
                }
                Status s = new Status(Decision.ERROR, m);
                R.note_text(s.getMessage());
                return s;
            }
        }

        private String[] Ynames(double[][] ysdy) {
            int n = DoubleArray.getColumnDimension(ysdy, 0);
            if (n == 0 || _f.getParametersExpression().length == n) {
                return _f.getParametersExpression();
            } else if (_f instanceof OutputFunctionExpression.NumericArray) {
                String e = _f.getParametersExpression()[0];
                if (e.startsWith("c(") && e.endsWith(")")) {
                    e = e.substring(2, e.length() - 1);
                }

                String[] ee = e.split(",");
                if (ee.length == n) {
                    return ee;
                }
            }
            // else
            String[] yn = new String[n];
            for (int i = 0; i < yn.length; i++) {
                yn[i] = StringUtils.join(_f.getParametersExpression(),"")+(i+1);
            }
            return yn;
        }

        @Override
        public synchronized Status getNextDesign(List<Experiment> finishedExperiments, List<Experiment> returnedExperiments) {
            R.note_text("# Build next design");

            try {
                double[][] ysdy = getOutputParams(finishedExperiments);

                R.set("Y" + currentiteration, ysdy, Ynames(ysdy));

                R.savels(new File(_repository.getAbsolutePath(), getName().replace(' ', '_') + getStatus() + ".Rdata"), "");//(currentiteration) );

                Object rexp = null;
                try {
                    rexp = R.eval("getNextDesign(DesignEnv,X" + currentiteration + ",Y" + currentiteration + ")");
                    if (rexp == null) {
                        Status s = new Status(Decision.DESIGN_OVER);
                        R.note_text(s.toString());
                        return s;
                    }
                } catch (Exception e) {
                    Log.err(e, 1);
                    Status s = new Status(Decision.ERROR, "Cannot perform R:getNextDesign call on " + toString_ExperimentArray("finishedExperiments", finishedExperiments) + "\n " + e.getMessage() + "\nR> " + R.getLastLogEntry() + "\nR!> " + R.getLastError());
                    R.note_text(s.toString());
                    return s;
                }
                double[][] Xn;
                if (rexp instanceof double[]) {
                    if (Xnames.length == 1) {
                        Xn = DoubleArray.columnVector((double[]) rexp);
                    } else {
                        Xn = new double[][]{(double[]) rexp};
                    }
                } else if (rexp instanceof double[][]) {
                    Xn = (double[][]) rexp;
                } else {
                    Xn = R.asMatrix(rexp);
                }
                if (Xn == null || Xn.length == 0 || Xn[0].length == 0) {
                    return new Status(DesignConstants.Decision.DESIGN_OVER);
                }
                if (allNaN(Xn)) {
                    Status s = new Status(Decision.ERROR, "Failed to perform R:getNextDesign call on " + toString_ExperimentArray("finishedExperiments", finishedExperiments) + "\nR> " + R.getLastLogEntry() + "\nR!> " + R.getLastError());
                    R.note_text(s.toString());
                    return s;
                }

                R.set("Xnext" + currentiteration, Xn, Xnames);

                //V1 plugin are expected to scale themselve: List<Experiment> exps = DesignHelper.createExperiments(scaleParametersWithBounds(Xn), _parameters, prj);
                List<Experiment> exps = DesignHelper.createExperiments(Xn, _parameters, prj);
                returnedExperiments.addAll(exps);

                R.savels(new File(_repository.getAbsolutePath(), getName().replace(' ', '_') + getStatus() + ".Rdata"), "");//(currentiteration) );

                double[][] Xi = R.asMatrix(R.eval("rbind(X" + currentiteration + ",Xnext" + currentiteration + ")"));
                R.set("X" + (currentiteration + 1), Xi, Xnames);

                R.savels(new File(_repository.getAbsolutePath(), getName().replace(' ', '_') + getStatus() + ".Rdata"), "");//(currentiteration) );

                currentiteration++;

                Status s = new Status(Decision.READY_FOR_NEXT_ITERATION);
                R.note_text(s.toString());
                return s;
            } catch (Exception e) {
                Log.err(e, 1);
                String m = e.getMessage() + "\n" + toString_ExperimentArray("finishedExperiments", finishedExperiments);
                try {
                    m = m + "\nR: " + R.getLastLogEntry() + " !" + R.getLastError();
                } catch (Exception ex) {
                    Log.err(ex, 2);
                    m = m + "\nR: ?";
                }
                Status s = new Status(Decision.ERROR, m);
                R.note_text(s.toString());
                return s;
            }
        }

        boolean allNaN(double[][] X) {
            if (X == null) {
                return true;
            }
            if (X.length == 0) {
                return true;
            }
            for (int i = 0; i < X.length; i++) {
                if (X[i].length == 0) {
                    return true;
                }
                for (int j = 0; j < X[i].length; j++) {
                    if (!Double.isNaN(X[i][j])) {
                        return false;
                    }
                }
            }
            return true;
        }
        
        @Override
        public synchronized String displayResultsTmp(List<Experiment> experiments) {
            R.note_text("# Display results (tmp)");
            
            try {
                //System.err.println("experiments " + experiments);

                String suffix = currentiteration + "_" + experiments.size();
                double[][] ysdy = getOutputParams(experiments);
                R.set("Y" + suffix, ysdy, Ynames(ysdy));
                double[][] x = getInputArray(experiments);
                R.set("X" + suffix, x, Xnames);

                if (R.asLogical(R.eval("exists('displayResultsTmp')"))) {
                    String out = null;
                    try {
                        out = R.asString(R.eval("displayResultsTmp(DesignEnv,X" + suffix + ",Y" + suffix + ")"));
                                        
                        try {
                            if (!R.asLogical(R.eval("is.null(DesignEnv$files)"))) {
                                String[] analyse_files = (R.asLogical(R.eval("length(DesignEnv$files)>1"))) 
                                    ? R.asStrings(R.eval("DesignEnv$files")) 
                                    : new String[]{R.asString(R.eval("DesignEnv$files"))};
                                for (int i = 0; i < analyse_files.length; i++) {
                                    String analyse_file = analyse_files[i];
                                    boolean exists = (boolean) R.eval("file.exists('" + analyse_file + "')");
                                    R.note_text("" + exists);
                                    if (exists) {

                                        File f = new File(_repository, analyse_file);
                                        if (R instanceof RserveSession) {
                                            ((RserveSession) R).getFile(f, analyse_file);
                                        } else {
                                            Disk.copyFile(new File(R.eval("getwd()").toString(), analyse_file), f);
                                        }
                                        if (i == 0) {
                                            out = out.replace(analyse_file, DesignHelper.getResultsRelativePath(f, _repository.getName()));
                                        }

                                    }
                                }
                            }
                        } catch (Exception ex) {
                            Log.err(ex, 1);
                            out = out + "\nFile error:\n" + ex.getMessage();
                        }
                    } catch (Exception ex) {
                        Log.err(ex, 1);
                        R.note_text("Error: " + ex.getMessage());
                        out = ex.getMessage() + "\nError, failed to analyseDesignTmp " + R.getLastLogEntry() + " ! " + R.getLastError() + "\n on \n" + experiments;
                    }

                    String html = RendererHelper.tryHTMLize(out, getDesignOutputTitle());
                    R.note_text(html.replace(BASE, "."));
                    return html;
                } else {
                    String html = noAnalyse(experiments);
                    R.note_text(html.replace(BASE, "."));
                    return html;
                }
            } catch (Exception ex) {
                Log.err(ex, 1);
                R.note_text("Error: " + ex.getMessage());
                return ex.getMessage() + "\nException: failed to analyseDesignTmp on " + experiments + "\n" + noAnalyse(experiments) + "\n" + ex.getMessage();
            }
        }

        @Override
        public synchronized String displayResults(List<Experiment> experiments) {
            R.note_text("# Display results");

            String out = null;
            try {
                double[][] ysdy = getOutputParams(experiments);
                R.set("Yanalyse" + currentiteration, ysdy, Ynames(ysdy));
                double[][] x = getInputArray(experiments);
                R.set("Xanalyse" + currentiteration, x, Xnames);

                R.savels(new File(_repository.getAbsolutePath(), getName().replace(' ', '_') + ".Rdata"), "");//(currentiteration) );

                out = R.asString(R.eval("displayResults(DesignEnv,Xanalyse" + currentiteration + ",Yanalyse" + currentiteration + ")"));

                try {
                    if (!R.asLogical(R.eval("is.null(DesignEnv$files)"))) {
                        String[] analyse_files = (R.asLogical(R.eval("length(DesignEnv$files)>1"))) 
                          ? R.asStrings(R.eval("DesignEnv$files")) 
                          : new String[]{R.asString(R.eval("DesignEnv$files"))};
                        for (int i = 0; i < analyse_files.length; i++) {
                            String analyse_file = analyse_files[i];
                            if ((boolean) R.eval("file.exists('" + analyse_file + "')")) {

                                File f = new File(_repository, analyse_file);
                                if (R instanceof RserveSession) {
                                    ((RserveSession) R).getFile(f, analyse_file);
                                } else {
                                    Disk.copyFile(new File(R.eval("getwd()").toString(), analyse_file), f);
                                }
                                if (i == 0) {
                                    out = out.replace(analyse_file, DesignHelper.getResultsRelativePath(f, _repository.getName()));
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    Log.err(ex, 1);
                    out = out + "\nFile error:\n" + ex.getMessage();
                }
            } catch (Exception ex) {
                Log.err(ex, 1);
                R.note_text("Error: " + ex.getMessage());
                out = ex.getMessage() + "\nError, failed to analyseDesign " + R.getLastLogEntry() + " ! " + R.getLastError() + "\n on \n" + experiments + "\n " + ex.getMessage();
            }

            String html = RendererHelper.tryHTMLize(out, getDesignOutputTitle());
            R.note_text(html.replace(BASE, "."));
            return html;
        }

        @Override
        public void saveNotebook() {
            ASCII.saveFile(new File(_repository, getName() + ".Rmd"), R.notebook());
        }

    }
}
