package org.funz.main;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.funz.api.Funz_v1;
import org.funz.api.Utils;
import static org.funz.main.MainUtils.init;
import org.funz.util.ASCII;

/**
 *
 * @author richet
 */
public class CompileInput extends MainUtils {

    static String name = "CompileInput";

    static {
        init(name,10);
    }

    static enum Option {

        HELP("Help", "help", "h", "Display help"),
        MODEL("Model", "model", "m", "Code ID to launch:" + ASCII.cat(",", Funz_v1.getModelList())),
        INPUT_FILES("Input files", "input_files", "if", "List of input files for code"),
        INPUT_VARIABLES("Input variables", "input_variables", "iv", "Input variables definition 'name=values|model', e.g. x1=0.1,0.2,0.3 x2=[0,1] x3=-0.5,-0.6"),
        VERBOSITY("Verbosity", "verbosity", "v", "Verbosity level in 0-10"),
        ARCHIVE_DIR("Archiving directory", "archive_dir", "ad", "Directory where to store output files and data");

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
        //args = "CompileInput -m R -if tmp/branin.R -iv x1=.5 x2=0.3 -v 10 -ad tmp".split(" ");
        if (args.length == 1 || Option.HELP.is(args[1])) {
            System.out.println(help());
            System.exit(0);
        }

        //tic("options");
        String _model = null;
        File[] _input = null;
        Map _variableModel = null;
        int verb = -666;
        File _archiveDir = null;

        try {
            for (int i = 1; i < args.length; i++) {
                //System.err.print(args[i] + ": ");

                if (Option.MODEL.is(args[i])) {
                    if (_model != null) throw new Exception("[ERROR] Option "+Option.MODEL.key+" already set!");
                    _model = args[i + 1];
                    //System.err.println(_model);
                    i++;
                } else if (Option.INPUT_FILES.is(args[i])) {
                    if (_input != null) throw new Exception("[ERROR] Option "+Option.INPUT_FILES.key+" already set!");
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
                    //System.err.println(_input);
                    i += j - 1;
                } else if (Option.INPUT_VARIABLES.is(args[i])) {
                    if (_variableModel != null) throw new Exception("[ERROR] Option "+Option.INPUT_VARIABLES.key+" already set!");
                    Map<String, Object> vars = new HashMap<>();
                    int j = 1;
                    while (i + j < args.length && !isOption(args[i + j])) {
                        try {
                            String[] name_mod = args[i + j].split("=");
                            vars.put(name_mod[0], name_mod[1]);
                        } catch (Exception e) {
                            System.err.println("[ERROR] Cannot parse variable " + args[i + j]);
                            throw e;
                        }
                        j++;
                    }
                    _variableModel = vars;
                    //System.err.println(_variableModel);
                    i += j - 1;
                } else if (Option.ARCHIVE_DIR.is(args[i])) {
                    if (_archiveDir != null) throw new Exception("[ERROR] Option "+Option.ARCHIVE_DIR.key+" already set!");
                    _archiveDir = new File(args[i + 1]);
                    //System.err.println(_archiveDir);
                    i++;
                } else if (Option.VERBOSITY.is(args[i])) {
                    if (verb != -666) throw new Exception("[ERROR] Option "+Option.VERBOSITY.key+" already set!");
                    verb = Integer.parseInt(args[i + 1]);
                    //System.err.println(verb);
                    i++;
                    //System.err.println("VERBOSITY "+verb);
                } else {
                    throw new Exception("[ERROR] Unknwon option " + args[i]);
                }
            }
            if (verb == -666) verb = 3;
            if (_archiveDir == null) _archiveDir = new File(".");
        } catch (Exception e) {
            System.err.println("[ERROR] failed to parse options: " + e.getMessage());
            e.printStackTrace();
            System.err.println(help());
            System.exit(PARSE_ERROR);
        }

        //toc("options");        //toc("options");
        if (_input == null) {
            System.err.println("[ERROR] Input files not defined.\n" + help("--input_files"));
            System.exit(INPUT_FILE_ERROR);
        }

        //tic("setVerbosity");
        Funz_v1.setVerbosity(verb);
        //toc("setVerbosity");

        try {
            //tic("compileVariables");
            Utils.compileVariables(_model, _input, _variableModel, _archiveDir);
            //toc("compileVariables");
        } catch (Exception e) {
            System.err.println("[ERROR] failed to COMPILE: " + e.getMessage());
            e.printStackTrace();
            System.exit(COMPILE_ERROR);
        }

        System.exit(0);
    }
}
