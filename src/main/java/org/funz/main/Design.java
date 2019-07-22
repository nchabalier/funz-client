package org.funz.main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.funz.Project;
import static org.funz.api.AbstractShell.*;
import org.funz.api.DesignShell_v1;
import static org.funz.api.DesignShell_v1.DEFAULT_FUNCTION_NAME;
import org.funz.api.DesignShell_v1.Function;
import org.funz.api.Funz_v1;
import org.funz.api.LoopDesign_v1;
import org.funz.log.Log;
import static org.funz.main.MainUtils.CLEAR_LINE;
import static org.funz.main.MainUtils.init;
import org.funz.util.ASCII;
import org.funz.util.Data;
import static org.funz.util.Data.*;
import static org.funz.util.Format.ArrayMapToCSVString;
import static org.funz.util.Format.ArrayMapToJSONString;
import static org.funz.util.Format.ArrayMapToMDString;
import static org.funz.util.Format.ArrayMapToXMLString;
import static org.funz.util.Format.MapToMDString;
import org.funz.util.ParserUtils;
import org.funz.util.Process;

/**
 *
 * @author richet
 */
public class Design extends MainUtils {

    static String name = "Design";

    static {
        init(name, 10);
    }

    static enum Option {

        HELP("Help", "help", "h", "Display help"),
        FUNCTION("Function", "function", "f", "Executable to launch with input variables as arguments: /path/to/exec 0.1 0.2"),
        FUNARGS("Function arguments", "function_args", "fa", "Input variables order to pass to function: x1 x2"),
        FUNPAR("Function parallel evaluations", "function_parallel", "fp", "Number of function parallel evaluations"),
        DESIGN("Design", "design", "d", "Algorithm to drive calculations points:\n" + S + ASCII.cat("\n" + S, Funz_v1.getDesignList())),
        INPUT_VARIABLES("Input variables", "input_variables", "iv", "Input variables definition 'name=values|model', e.g. x1=0.1,0.2,0.3 x2=[0,1] x3=-0.5,-0.6"),
        DESIGN_OPTIONS("Design options", "design_options", "do", "Options of the algorithm definition name=value, e.g. n=100 iMax=20"),
        RUN_CONTROL("Run control", "run_control", "rc", "Features of the run, e.g. retry=3 cache=/tmp/MyCache"),
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
        //args = "Design -d GradientDescent -do nmax=1 epsilon=0.000000001 delta=0.01 target=-10 -f ./mult.sh -fa x1 x2 -iv x1=[-0.5,.0001] x2=[-0.3,.8] -v 10 -ad tmp".split(" ");
        //args = "Design -d GradientDescent -do nmax=3 epsilon=0.000000001 delta=0.01 target=-10 -f ./mult.sh -fa x1 x2 -iv x1=[-0.5,.0001] x2=[-0.3,.8] -v 10 -ad tmp".split(" ");
  
        if (args.length == 1 || Option.HELP.is(args[1])) {
            System.out.println(help());
            System.exit(0);
        }

        //tic("options");
        String _function = null;
        List<String> _funargs = null;
        String _designer = null;
        Map _variableModel = null;
        Map _designOptions = null;
        Map<String, String> _runControl = new HashMap<>();
        Map<String, String> _monitorControl = new HashMap<>();
        List<String> _filter = null;
        File _print = new File(name + ".csv");
        boolean X = false;
        int _funpar = 1;
        int verb = 3;
        File _archiveDir = new File(".");

        try {
            for (int i = 1; i < args.length; i++) {
                if (Option.FUNCTION.is(args[i])) {
                    List<String> farg = new LinkedList<>();
                    int j = 1;
                    while (i + j < args.length && !isOption(args[i + j])) {
                        try {
                            farg.add(args[i + j]);
                        } catch (Exception e) {
                            System.err.println("[ERROR] Cannot parse option " + args[i + j]);
                            throw e;
                        }
                        j++;
                    }
                    _function = ASCII.cat(" ", farg.toArray(new String[farg.size()]));
                    //System.err.println(_filter);
                    i += j - 1;
                } else if (Option.FUNARGS.is(args[i])) {
                    List<String> farg = new LinkedList<>();
                    int j = 1;
                    while (i + j < args.length && !isOption(args[i + j])) {
                        try {
                            farg.add(args[i + j]);
                        } catch (Exception e) {
                            System.err.println("[ERROR] Cannot parse option " + args[i + j]);
                            throw e;
                        }
                        j++;
                    }
                    _funargs = farg;
                    //System.err.println(_filter);
                    i += j - 1;
                } else if (Option.FUNPAR.is(args[i])) {
                    _funpar = Integer.parseInt(args[i + 1]);
                    //System.err.println(_filter);
                    i++;
                } else if (Option.DESIGN.is(args[i])) {
                    _designer = args[i + 1];
                    //System.err.println(_designer);
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
                    //System.err.println(_variableModel);
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
                    //System.err.println(_designOptions);
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
                    //System.err.println(_runControl);
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
                    //System.err.println(_monitorControl);
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
                    //System.err.println(_filter);
                    i += j - 1;
                } else if (Option.PRINT.is(args[i])) {
                    _print = new File(args[i + 1]);
                    //System.err.println(_format);
                    i++;
                } else if (Option.ARCHIVE_DIR.is(args[i])) {
                    _archiveDir = new File(args[i + 1]);
                    //System.err.println(_archiveDir);
                    i++;
                } else if (Option.VERBOSITY.is(args[i])) {
                    verb = Integer.parseInt(args[i + 1]);
                    //System.err.println(verb);
                    i++;
                    //System.err.println("VERBOSITY "+verb);
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
        //toc("options");        //toc("options");

        if (_function == null) {
            System.err.println("[ERROR] Function not defined.\n" + help("--function"));
            System.exit(FUNCTION_ERROR);
        }

        if (_designer == null) {
            System.err.println("[ERROR] Design not defined.\n" + help("--design"));
            System.exit(DESIGN_ERROR);
        }

        //tic("setVerbosity");
        Funz_v1.setVerbosity(verb);
        //toc("setVerbosity");        //toc("setVerbosity");

        final String exe = _function;
        Function f = new DesignShell_v1.Function(exe, _funpar, _funargs.toArray(new String[_funargs.size()])) {
            File fdir;

            public void init() {
                fdir = new File(".f");
                if (!fdir.exists() && !fdir.mkdir()) {
                    Log.err("Cannot create working directory: " + fdir, 0);
                }
            }

            @Override
            public String toString() {
                return exe + " " + ASCII.cat(" ", args);
            }

            @Override
            public Map f(Object... x) throws Exception {
                Process p = new Process(exe + " " + ASCII.cat(" ", x), fdir, null);
                int exit;
                String[] y = {};
                try {
                    File dat = File.createTempFile(".Design_y", ".dat", fdir);
                    File out = File.createTempFile(".Design_y", ".out", fdir);
                    File err = File.createTempFile(".Design_y", ".err", fdir);
                    OutputStream yout = new FileOutputStream(dat);
                    exit = p.runCommand(yout, new FileOutputStream(out), new FileOutputStream(err));
                    yout.flush();
                    y = ParserUtils.getASCIIFileLines(dat);
                    yout.close();
                    if (exit != 0) {
                        throw new Exception("Bad exit status: " + exit);
                    }
                } catch (Exception ex) {
                    throw new Exception("[FAILED] call "+DEFAULT_FUNCTION_NAME+"(x=" + asString(x) + ") " + p.getFailReason() + " " + ex.getMessage());
                }

                Map<String, String> ym = new HashMap<>();
                if (y.length > 1) {
                    for (int i = 0; i < y.length; i++) {
                        ym.put(DEFAULT_FUNCTION_NAME + (i + 1), y[i]);
                    }
                    ym.put(DEFAULT_FUNCTION_NAME, Data.asString(y));
                } else {
                    ym.put(DEFAULT_FUNCTION_NAME, y[0]);
                }

                return ym;
            }
        };

        DesignShell_v1 shell = null;
        try {
            //tic("new Shell_v1");
            shell = new DesignShell_v1(f, _designer, _variableModel, _designOptions);
            //toc("new Shell_v1");

            //tic("setArchiveDirectory");
            shell.setArchiveDirectory(_archiveDir);
            //toc("setArchiveDirectory");

            for (String prop : _runControl.keySet()) {
                shell.setProjectProperty(prop, _runControl.get(prop));
            }
        } catch (Exception e) {
            System.err.println("[ERROR] failed to CREATE Funz shell: " + e.getMessage() + "\n" + ArrayMapToMDString(shell.getResultsArrayMap()));
            //e.printStackTrace();
            System.exit(CREATE_SHELL_ERROR);
        }

        final DesignShell_v1 tokill = shell;
        final int v = verb;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    Thread.sleep(200);
                    if (v > 0) {
                        System.out.print("Shutdown ...");
                    }
                    tokill.shutdown();
                    if (v > 0) {
                        System.out.println(CLEAR_LINE + "Shutdown ... done.");
                    }
                } catch (InterruptedException e) {
                }
            }
        });

        Map<String, Object> results = null;
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

                double sleep = 1000;
                if (_monitorControl.containsKey("sleep")) {
                    sleep = Math.floor(1000 * Double.parseDouble(_monitorControl.get("sleep")));
                }

                boolean finished = false;
                String state;
                String pointstatus = "-";
                String new_pointstatus = "-";
                while (!finished) {
                    try {
                        Thread.sleep((long) sleep);
                        state = shell.getState();
                        if (state.contains(SHELL_ERROR)) {
                            System.err.println("[ERROR] " + name + " failed:" + "\n" + ArrayMapToMDString(shell.getResultsArrayMap()));
                            System.exit(RUN_ERROR);
                        }
                        
                        finished = state.startsWith(SHELL_OVER) || state.startsWith(SHELL_ERROR) || state.startsWith(SHELL_EXCEPTION);

                        if (verb > 0) {
//                            if (!new_state.equals(state)) {
                              System.out.println(CLEAR_LINE + state.replaceAll("\n", " | "));
//                            } else {
//                                System.out.print("~");
//                            }
                        }

                        if (_monitorControl.containsKey("display")) {
                            new_pointstatus = ArrayMapToMDString(shell.getCalculationPointsStatus());
                            if (!new_pointstatus.equals(pointstatus)) {
                                Process p = new Process(_monitorControl.get("display") + " " + new_pointstatus, null, null);
                                p.runCommand();
                                pointstatus = new_pointstatus;
                            }
                        }
                    } catch (SecurityException e) {
                        throw e;
                    } catch (Exception e) {
                        System.err.println("[WARNING] Cannot process progress: " + e.getMessage());
                        //e.printStackTrace();
                    }
                }
                System.out.println(StringUtils.repeat(" ", 80));
            }

            //toc("startComputation");
            //tic("getResultsStringArrayMap");
            results = Data.get(shell.getResultsArrayMap(),0);
            //toc("getResultsStringArrayMap");

            if (results.containsKey("error")) {
                boolean all = true;
                Object e = results.get("error");
                    if (e == null || e.toString().length() == 0) {
                        all = false;
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
            Map<String, Object> print_results = new HashMap();
            if (_filter == null) {
                print_results = Data.keep(results,"analysis(.*)");
                print_results = Data.remove(print_results,"analysis(.*)\\.(\\d+)");
            } else {
                if (results != null) {
                    for (String s : _filter) {
                        for (String r : results.keySet()) {
                            if (r.equals(s) || r.matches(s)) {
                                print_results.put(r, results.get(r));
                            }
                        }
                    }
                }
            }
            
            System.out.println(MapToMDString(print_results));
            Log.out(MapToMDString(print_results), 0);

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

        System.exit(0);
    }
    
}
