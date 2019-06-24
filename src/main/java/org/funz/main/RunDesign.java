package org.funz.main;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.funz.Project;
import static org.funz.api.AbstractShell.SHELL_ERROR;
import org.funz.api.BatchRun_v1;
import org.funz.api.Funz_v1;
import org.funz.api.LoopDesign_v1;
import org.funz.api.Shell_v1;
import org.funz.log.Log;
import static org.funz.main.MainUtils.CLEAR_LINE;
import static org.funz.main.MainUtils.init;
import static org.funz.parameter.OutputFunctionExpression.OutputFunctions;
import org.funz.util.ASCII;
import org.funz.util.Data;
import static org.funz.util.Format.ArrayMapToCSVString;
import static org.funz.util.Format.ArrayMapToJSONString;
import static org.funz.util.Format.ArrayMapToMDString;
import static org.funz.util.Format.ArrayMapToXMLString;
import org.funz.util.ParserUtils;
import org.funz.util.Process;

/**
 *
 * @author richet
 */
public class RunDesign extends MainUtils {

    static String name = "RunDesign";

    static {
        init(name, 10);
    }

    static enum Option {

        HELP("Help", "help", "h", "Display help"),
        MODEL("Model", "model", "m", "Code to launch:\n" + S + ASCII.cat("\n" + S, Funz_v1.getModelList())),
        INPUT_FILES("Input files", "input_files", "if", "List of input files for code"),
        OUTPUT_EXPRESSION("Output expression", "output_expression", "oe", "Output expression to parse when calculation finished:\n" + S + ASCII.cat("\n" + S, OutputFunctions.toArray()).replaceAll("(class)(.*)(\\$)", "")),
        DESIGN("Design", "design", "d", "Algorithm to drive calculations points:\n" + S + ASCII.cat("\n" + S, Funz_v1.getDesignList())),
        INPUT_VARIABLES("Input variables", "input_variables", "iv", "Input variables definition 'name=values|model', e.g. x1=0.1,0.2,0.3 x2=[0,1] x3=-0.5,-0.6"),
        DESIGN_OPTIONS("Design options", "design_options", "do", "Options of the algorithm definition name=value, e.g. n=100 iMax=20"),
        RUN_CONTROL("Run control", "run_control", "rc", "Features of the run, e.g. retry=3 cache=/tmp/MyCache archiveFilter=\"(.*)\" blacklistTimeout=60"),
        MONITOR_CONTROL("Monitor control", "monitor_control", "mc", "Monitoring options, e.g. sleep=5 display=/usr/local/command_to_display_results"),
        VERBOSITY("Verbosity", "verbosity", "v", "Verbosity level in 0-10"),
        ARCHIVE_DIR("Archiving directory", "archive_dir", "ad", "Directory where to store output files and data"),
        PRINT_FILTER("Print filter", "print_filter", "pf", "Output data filter: x1 x2 y code duration ..."),
        PRINT("Print file", "print", "p", "Filename with data format: xml json or csv (default if not recognized)");

        String name;
        String key;
        String short_key;
        String help;

        Option(String name, String key, String short_key, String help) {
            this.name = name;
            this.key = key;
            this.short_key = short_key;
            this.help = help;
        }

        public boolean is(String word) {
            return (word.equals("--" + key) | word.equals("-" + short_key));

        }

        @Override
        public String toString() {
            return StringUtils.rightPad("--" + key + ", -" + short_key, 30) + name + "\n" + StringUtils.rightPad("", 40) + help;
        }

    }

    private static boolean isOption(String word) {
        for (Option o : Option.values()) {
            if (o.is(word)) {
                return true;
            }
        }
        return false;
    }

    private static String help() {
        String h = "Funz.(sh|bat) " + name + " ...\n";
        for (Option o : Option.values()) {
            h = h + "\n" + o.toString();
        }
        return h;
    }

    private static String help(String word) {
        for (Option o : Option.values()) {
            if (o.is(word)) {
                return o.toString();
            }
        }
        return null;
    }

    public static void main(String[] args) {
        //args = "RunDesign -m R -d GradientDescent -do nmax=3 -oe N(1+cat,1) -if tmp/branin.R -iv x1=0.5 x2=[0.3,.4] -v 10 -ad tmp".split(" ");
        //args = "RunDesign -m R -d GradientDescent -do nmax=3 -oe 1+cat -if tmp/branin.R -iv x1=0.5 x2=[0.3,.4] -v 10 -ad tmp".split(" ");
        //args = "RunDesign -m R -d GradientDescent -do nmax=3 -if tmp/branin.R -iv x1=0.5,0.6 x2=[0.3,.4] -v 10 -ad tmp".split(" ");
        //args = "RunDesign -m R -d GradientDescent -do nmax=3 -if tmp/branin.R -iv x1=0,1 x2=[0.3,.4] -v 10 -ad tmp".split(" ");
        //args = "RunDesign -m R -d GradientDescent -do nmax=3 -if tmp/branin.R -iv x1=[-1.1,1] x2=[0.3,.4] -v 10 -ad tmp -oe x1+min(cat,10,na.rm=F)".split(" ");
        //args = "RunDesign -m R -d GradientDescent -do nmax=3 -if tmp/branin.R -iv x1=0,.3,.5,1 x2=[0.3,.4] -v 10 -ad tmp".split(" ");

        if (args.length == 1 || Option.HELP.is(args[1])) {
            System.out.println(help());
            System.exit(0);
        }

        //tic("options");
        String _model = null;
        File[] _input = null;
        String[] _outputExpressions = null;//OutputFunctionExpression.Text.fromNiceString("'?'");
        String _designer = null;
        Map _variableModel = null;
        Map _designOptions = null;
        Map<String, String> _runControl = new HashMap<>();
        Map<String, String> _monitorControl = new HashMap<>();
        List<String> _filter = null;
        File _print = new File(name + ".csv");
        boolean X = false;
        int verb = 3;
        File _archiveDir = new File(".");

        try {
            for (int i = 1; i < args.length; i++) {
                if (Option.MODEL.is(args[i])) {
                    _model = args[i + 1];
                    i++;
                } else if (Option.INPUT_FILES.is(args[i])) {
                    List<File> files = new LinkedList<>();
                    int j = 1;
                    while (i + j < args.length && !isOption(args[i + j])) {
                        File f = new File(args[i + j]);
                        if (!f.exists()) {
                            System.err.println("[WARNING] Cannot access file " + f.getAbsolutePath());
                        }
                        files.add(new File(args[i + j]));
                        j++;
                    }
                    _input = files.toArray(new File[files.size()]);
                    i += j - 1;
                } else if (Option.OUTPUT_EXPRESSION.is(args[i])) {
                    List<String> expressions = new LinkedList<>();
                    int j = 1;
                    while (i + j < args.length && !isOption(args[i + j])) {
                        String e = args[i + j];
                        expressions.add(e);
                        j++;
                    }
                    _outputExpressions = expressions.toArray(new String[expressions.size()]);
                    i += j - 1;
                } else if (Option.DESIGN.is(args[i])) {
                    _designer = args[i + 1];
                    i++;
                } else if (Option.INPUT_VARIABLES.is(args[i])) {
                    Map<String, Object> vars = new HashMap<>();
                    int j = 1;
                    while (i + j < args.length && !isOption(args[i + j])) {
                        try {
                            String[] name_mod = args[i + j].split("=");
                            if (name_mod[1].startsWith("[")) {
                                vars.put(name_mod[0], name_mod[1]);
                            } else {
                                vars.put(name_mod[0], name_mod[1].split(","));
                            }
                        } catch (Exception e) {
                            System.err.println("[ERROR] Cannot parse variable " + args[i + j]);
                            throw e;
                        }
                        j++;
                    }
                    _variableModel = vars;
                    i += j - 1;
                } else if (Option.DESIGN_OPTIONS.is(args[i])) {
                    Map<String, String> opts = new HashMap<>();
                    int j = 1;
                    while (i + j < args.length && !isOption(args[i + j])) {
                        try {
                            String[] name_mod = args[i + j].split("=");
                            if (!name_mod[1].startsWith("'")) {
                                name_mod[1] = "'" + name_mod[1];
                            }
                            if (!name_mod[1].endsWith("'")) {
                                name_mod[1] = name_mod[1] + "'";
                            }
                            opts.put(name_mod[0], name_mod[1]);
                        } catch (Exception e) {
                            System.err.println("[ERROR] Cannot parse option " + args[i + j]);
                            throw e;
                        }
                        j++;
                    }
                    _designOptions = opts;
                    i += j - 1;
                } else if (Option.RUN_CONTROL.is(args[i])) {
                    Map<String, String> opts = new HashMap<>();
                    int j = 1;
                    while (i + j < args.length && !isOption(args[i + j])) {
                        try {
                            String[] name_mod = args[i + j].split("=");
                            opts.put(name_mod[0], name_mod[1]);
                        } catch (Exception e) {
                            System.err.println("[ERROR] Cannot parse option " + args[i + j]);
                            throw e;
                        }
                        j++;
                    }
                    _runControl = opts;
                    i += j - 1;
                } else if (Option.MONITOR_CONTROL.is(args[i])) {
                    Map<String, String> opts = new HashMap<>();
                    int j = 1;
                    while (i + j < args.length && !isOption(args[i + j])) {
                        try {
                            String[] name_mod = args[i + j].split("=");
                            opts.put(name_mod[0], name_mod[1]);
                        } catch (Exception e) {
                            System.err.println("[ERROR] Cannot parse option " + args[i + j]);
                            throw e;
                        }
                        j++;
                    }
                    _monitorControl = opts;
                    i += j - 1;
                } else if (Option.PRINT_FILTER.is(args[i])) {
                    List<String> outs = new LinkedList<>();
                    int j = 1;
                    while (i + j < args.length && !isOption(args[i + j])) {
                        try {
                            outs.add(args[i + j]);
                        } catch (Exception e) {
                            System.err.println("[ERROR] Cannot parse option " + args[i + j]);
                            throw e;
                        }
                        j++;
                    }
                    _filter = outs;
                    i += j - 1;
                } else if (Option.PRINT.is(args[i])) {
                    _print = new File(args[i + 1]);
                    i++;
                } else if (Option.ARCHIVE_DIR.is(args[i])) {
                    _archiveDir = new File(args[i + 1]);
                    i++;
                } else if (Option.VERBOSITY.is(args[i])) {
                    verb = Integer.parseInt(args[i + 1]);
                    i++;
                } else {
                    throw new Exception("[ERROR] Unknown option: " + args[i]);
                }

            }
        } catch (Exception e) {
            System.err.println("[ERROR] failed to parse options: " + e.getMessage());
            //e.printStackTrace();
            System.err.println(help());
            System.exit(PARSE_ERROR);
        }
        //toc("options");

        if (_input == null) {
            System.err.println("[ERROR] Input files not defined.\n" + help("--input_files"));
            System.exit(INPUT_FILE_ERROR);
        }

        //tic("setVerbosity");
        Funz_v1.setVerbosity(verb);
        //toc("setVerbosity");        //toc("setVerbosity");

        Shell_v1 shell = null;
        try {
            //tic("new Shell_v1");
            shell = new Shell_v1(_model, _input, _outputExpressions, _designer, _variableModel, _designOptions);
            //toc("new Shell_v1");

            if (_model == null) {
                System.err.println("[WARNING] Model not defined, using default one: " + shell.getModel());
            }

            if (_outputExpressions == null) {
                System.err.println("[WARNING] Output expression not defined, using default one: " + shell.getOutputExpressions()[0]);
            }

            //tic("setArchiveDirectory");
            shell.setArchiveDirectory(_archiveDir);
            //toc("setArchiveDirectory");

            if (_runControl.containsKey("cache")) {
                shell.addCacheDirectory(new File(_runControl.get("cache")));
            }

            for (String prop : _runControl.keySet()) {
                shell.setProjectProperty(prop, _runControl.get(prop));
            }
        } catch (Exception e) {
            System.err.println("[ERROR] failed to CREATE Funz shell: " + e.getMessage() + "\n" + (shell != null ? ArrayMapToMDString(shell.getResultsArrayMap()) : "?"));
            //e.printStackTrace();
            System.exit(CREATE_SHELL_ERROR);
        }

        final Shell_v1 tokill = shell;
        final int v = verb;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    Thread.sleep(200);
                    if (v > 0) {
                        System.out.print("Shutdown ...");
                    }
                    tokill.shutdown();
                    Funz_v1.POOL.setRefreshing(false, "RunDesign", "Shuting down");
                    if (v > 0) {
                        System.out.println(CLEAR_LINE + "Shutdown ... done.");
                    }
                } catch (InterruptedException e) {
                }
            }
        });

        Map<String, Object[]> results = null;
        try {
            //tic("startComputation");
            if ((_monitorControl == null || _monitorControl.isEmpty()) && verb <= 0) {
                if (!shell.startComputationAndWait()) {
                    System.err.println("[ERROR] Cannot run computation:" + "\n" + ArrayMapToMDString(shell.getResultsArrayMap()));
                    System.exit(START_ERROR);
                }
            } else {
                if (!shell.startComputation()) {
                    System.err.println("[ERROR] Cannot run computation:" + "\n" + ArrayMapToMDString(shell.getResultsArrayMap()));
                    System.exit(START_ERROR);
                }

                double sleep = 5000;
                if (_monitorControl.containsKey("sleep")) {
                    sleep = Math.floor(1000 * Double.parseDouble(_monitorControl.get("sleep")));
                }

                boolean finished = false;
                String state;
                String status = "-";
                String new_status = "-";
                while (!finished) {
                    try {
                        Thread.sleep((long) sleep);
                        state = shell.getState();
                        if (state.contains(SHELL_ERROR)) {
                            System.err.println("[ERROR] " + name + " failed:" + "\n" + ArrayMapToMDString(shell.getResultsArrayMap()));
                            System.exit(RUN_ERROR);
                        }
                        
                        if (_designer == null) {
                            finished = state.contains(BatchRun_v1.BATCH_OVER) || state.contains(BatchRun_v1.BATCH_ERROR) || state.contains(BatchRun_v1.BATCH_EXCEPTION);
                        } else {
                            finished = true;
                            String[] states = state.split("\n");
                            if (states.length > 0) {
                                for (int i = 0; i < states.length; i++) {
                                    if (!(states[i].contains(LoopDesign_v1.DESIGN_OVER) || states[i].contains(LoopDesign_v1.DESIGN_ERROR) || states[i].contains(LoopDesign_v1.DESIGN_EXCEPTION))) {
                                        finished = false;
                                        break;
                                    }
                                }
                            }
                        }
                        if (verb > 0) {
//                            if (!new_state.equals(state)) {
                            System.out.print(CLEAR_LINE + state);//.replace("\n", " | "));
//                            } else {
//                                System.out.print("=");
//                            }
                        }
                        if (_monitorControl.containsKey("display")) {
                            new_status = ArrayMapToMDString(shell.getCalculationPointsStatus());
                            if (!new_status.equals(status)) {
                                Process p = new Process(_monitorControl.get("display") + " " + new_status, null, null);
                                p.runCommand();
                                status = new_status;
                            }
                        }
                    } catch (SecurityException e) {
                        throw e;
                    } catch (Exception e) {
                        System.err.println("[WARNING] Cannot process progress: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
                System.out.println(StringUtils.repeat(" ", 80));
            }
            //toc("startComputation");
            //tic("getResultsStringArrayMap");
            results = shell.getResultsArrayMap();
            //toc("getResultsStringArrayMap");
            if (results.containsKey("error")) {
                boolean all = true;
                for (Object e : results.get("error")) {
                    if (e == null || e.toString().length() == 0) {
                        all = false;
                        break;
                    }
                }
                if (all) {
                    System.err.println("[ERROR] failed to complete Funz shell\n" + ArrayMapToMDString(shell.getResultsArrayMap()));
                    System.exit(DESIGN_ERROR);
                }
            }
        } catch (Exception e) {
            System.err.println("[ERROR] failed to RUN Funz shell: " + e.getMessage() + "\n" + ArrayMapToMDString(shell.getResultsArrayMap()));
            //e.printStackTrace();
            System.exit(RUN_ERROR);
        } finally {
            Map<String, Object[]> print_results = new HashMap();
            if (_filter == null) {
                print_results = Data.keep_array(results, "case|analysis(.*)");
                print_results = Data.remove_array(print_results, "(.*)\\.(\\d+)");
            } else {
                if (results != null) {
                    for (String s : _filter) {
                        for (String r : results.keySet()) {
                            if (r.equals(s) || r.matches(s)) {
                                print_results.put(s, results.get(r));
                            }
                        }
                    }
                }
            }
            
            System.out.println(ArrayMapToMDString(print_results));
            Log.out(ArrayMapToMDString(print_results), 0);

            if (_print.getName().endsWith(".xml")) {
                ASCII.saveFile(_print, ArrayMapToXMLString(print_results));
            } else if (_print.getName().endsWith(".json")) {
                ASCII.saveFile(_print, ArrayMapToJSONString(print_results));
            } else {
                ASCII.saveFile(_print, ArrayMapToCSVString(print_results));
            }

            shell.shutdown();

            ASCII.saveFile(new File(_print.getParentFile(), name + ".md"),
                    ParserUtils.getASCIIFileContent(new File(shell.getArchiveDirectory(), Project.REPORT_FILE))
                            .replace("](./", "](" + _archiveDir + "/").replace("='./", "='" + _archiveDir + "/"));
        }

        Funz_v1.POOL.setRefreshing(false, "RunDesign", "System.exit");
        System.exit(0);
    }
}
