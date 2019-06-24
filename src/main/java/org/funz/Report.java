package org.funz;

import java.io.File;
import java.io.FileFilter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.funz.doeplugin.DesignConstants;
import org.funz.doeplugin.DesignHelper;
import org.funz.log.Log;
import org.funz.parameter.Case;
import org.funz.parameter.Case.Node;
import org.funz.parameter.CaseList;
import org.funz.parameter.InputFile;
import org.funz.parameter.OutputFunctionExpression;
import org.funz.parameter.Parameter;
import org.funz.parameter.VarGroup;
import org.funz.parameter.Variable;
import org.funz.util.Disk;
import static org.funz.util.Format.toHTML;
import static org.funz.util.ParserUtils.getASCIIFileContent;
import org.math.array.DoubleArray;
import org.math.array.LinearAlgebra;

public class Report {

    public static final String CALCULATIONSCSV = ".calculations.csv";
    public static final String RESULTSCSV = ".results.csv";
    public static final String __INTER__ = "__inter__";
    public static final String __INTER_LINE__ = "__inter_line__";
    public static final String __OUT__ = "__out__";
    private static final String EOL = "\n";

    final static String designer_opt_li_tmpl = "  * __opt_name__: __opt_val__";

    static String getGenInfo(Project _project) {
        String genbuffer = "\n# General Information\n\n"
                + "  * Host name: __hostname__\n"
                + "  * Funz version: __funz_version__\n"
                + "\n***\n\n"
                + "Project code: __code_name__\n\n"
                + "  * plugin: __project_plugin_info__\n"
                + "  * output: __output__\n"
                + "\n***\n\n"
                + "Project designer: __designer__\n\n"
                + "__designer_opts__\n"
                + "\n***\n\n"
                + "Project cost: __runs__ runs\n\n"
                + "  * Cumulated run time: __cum_runtime__\n"
                + "  * Real run time: __real_runtime__\n"
                + "  * Parallel efficiency factor: __eff_runtime__\n"
                + "\n";

        String hostname = "?";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostname = "?";
        }
        genbuffer = replace(genbuffer, "__hostname__", hostname);

        genbuffer = replace(genbuffer, "__funz_version__", Constants.APP_VERSION + " (build " + Constants.APP_BUILD_DATE + ")");

        genbuffer = replace(genbuffer, "__project_model__", _project.getModel());
        genbuffer = replace(genbuffer, "__project_plugin_info__", _project.getPlugin().getPluginInformation().replace('\n', ';'));
        genbuffer = replace(genbuffer, "__code_name__", _project.getCode());

        genbuffer = replace(genbuffer, "__output__", _project.getMainOutputFunctionName());

        CaseList cases = _project.getCases();
        genbuffer = replace(genbuffer, "__runs__", "" + cases.size());
        double[] start = new double[cases.size()];
        double[] end = new double[cases.size()];
        for (int i = 0; i < start.length; i++) {
            start[i] = cases.get(i).getStart() + 0.0;
            end[i] = cases.get(i).getEnd() + 0.0;
        }
        //System.out.println("start=\n" + DoubleArray.toString(start));
        //System.out.println("end=\n" + DoubleArray.toString(end));
        //System.out.println("end-start=\n" + DoubleArray.toString(LinearAlgebra.minus(end, start)));
        double cum_runtime = DoubleArray.sum(LinearAlgebra.minus(end, start));
        //System.out.println("cum_runtime=" + cum_runtime);
        double real_runtime = DoubleArray.max(end) - DoubleArray.min(start);
        //System.out.println("real_runtime=" + real_runtime);

        genbuffer = replace(genbuffer, "__cum_runtime__", Case.longToDurationString((int) cum_runtime));
        genbuffer = replace(genbuffer, "__real_runtime__", Case.longToDurationString((int) real_runtime));
        genbuffer = replace(genbuffer, "__eff_runtime__", "" + Math.ceil((cum_runtime / real_runtime) * 100) / 100);

        // designer
        if (!_project.getDesignerId().equals(DesignConstants.NODESIGNER_ID)) {
            StringBuilder designer_opts = new StringBuilder();
            if (!_project.getDesignOptions(_project.getDesignerId()).isEmpty()) {
                for (String o : _project.getDesignOptions(_project.getDesignerId()).keySet()) {
                    designer_opts.append(designer_opt_li_tmpl
                            .replace("__opt_name__", o)
                            .replace("__opt_val__", _project.getDesignOptions(_project.getDesignerId()).get(o)));
                    designer_opts.append(EOL);
                }
            }
            genbuffer = replace(genbuffer, "__designer__", _project.getDesignerId());
            genbuffer = replace(genbuffer, "__designer_opts__", designer_opts.toString());
        } else {
            genbuffer = replace(genbuffer, "__designer__", "None");
            genbuffer = replace(genbuffer, "__designer_opts__", "");
        }

        return genbuffer;
    }

    final static String process_files_tmpl = "Main input file: [__input_file__](__input_file_path__)\n"
            + "```__code__" + "\n__input_file_content__\n```\n"
            + "\n"
            + "__input_file_secondary__\n";
    final static String process_bin_tmpl = "__input_bin__\n";

    final static String input_files_secondary_li_tmpl = "  * [__input_file_2__](__input_file_2_path__)";
    final static String var_tr_tmpl = "| __var_name__ | __var_type__ | __var_comment__ |";
    final static String dparam_tr_tmpl = "| __dparam_grp__ | __dparam_name__ | __dparam_values__ |";
    final static String cparam_tr_tmpl = "| __cparam_name__ | __cparam_values__ |";

    static String getModelInfo(Project _project) {
        String modelbuffer = "\n# Modeling information\n\n"
                + "\n"
                + "__process__\n"
                + "\n"
                + "__vars_tr__\n"
                + "\n"
                + "__dparams_tr__\n"
                + "\n"
                + "__cparams_tr__\n"
                + "\n";

        //main input file
        InputFile[] inputFiles = _project.getInputFilesAsArray();
        if (inputFiles != null && inputFiles.length > 0) {

            modelbuffer = replace(modelbuffer, "__process__", process_files_tmpl);
            modelbuffer = replace(modelbuffer, "__code__", _project.getCode().toLowerCase());

            modelbuffer = replace(modelbuffer, "__input_file__", inputFiles[0].getFile().getName());
            modelbuffer = replace(modelbuffer, "__input_file_path__", inputFiles[0].getFile().getAbsolutePath());
            if (inputFiles[0].getFile().length() < 500 * 1024) {
                String text = getASCIIFileContent(inputFiles[0].getFile());
                modelbuffer = replace(modelbuffer, "__input_file_content__", text);
            } else {
                modelbuffer = replace(modelbuffer, "__input_file_content__", "Content of " + Disk.getHumanFileSizeString(inputFiles[0].getFile().length()) + " too large, not displayed.");
            }

            //others input file
            StringBuilder input_files_secondary = new StringBuilder();
            if (inputFiles.length > 1) {
                input_files_secondary.append("Secondary input files:\n\n");
                for (int i = 1; i < inputFiles.length; i++) {
                    input_files_secondary.append(input_files_secondary_li_tmpl
                            .replace("__input_file_2__", inputFiles[i].getFile().getName())
                            .replace("__input_file_2_path__", inputFiles[i].getFile().getAbsolutePath())
                    );
                    if (inputFiles[i].hasParameters) {
                        if (inputFiles[i].getFile().length() < 500 * 1024) {
                            String text = getASCIIFileContent(inputFiles[i].getFile());
                            input_files_secondary.append("\n```\n" + text + "\n```\n");
                        } else {
                            input_files_secondary.append("```\n" + "Content of " + Disk.getHumanFileSizeString(inputFiles[i].getFile().length()) + " too large, not displayed." + "\n```\n");
                        }
                    }
                    input_files_secondary.append(EOL);
                }
            }
            modelbuffer = replace(modelbuffer, "__input_file_secondary__", input_files_secondary.toString());
        } else { // this should occur for only Design projects
            modelbuffer = replace(modelbuffer, "__process__", process_bin_tmpl);
            modelbuffer = replace(modelbuffer, "__input_bin__", _project.getCode());
        }

        //variables
        LinkedList<Variable> vars = _project.getVariables();
        StringBuilder vars_tr = new StringBuilder();
        if (vars.size() >= 0) {
            vars_tr.append("Variables:\n\n");
            vars_tr.append("| Name | Type | Default value |\n");
            vars_tr.append("|------|------|---------------|\n");
            for (Variable v : vars) {
                String d = v.getDefaultValue();
                vars_tr.append(var_tr_tmpl
                        .replace("__var_name__", v.getName())
                        .replace("__var_type__", v.getType())
                        .replace("__var_comment__", d == null ? "" : d));
                vars_tr.append(EOL);
            }
        }
        modelbuffer = replace(modelbuffer, "__vars_tr__", vars_tr.toString());

        //discrete params
        List<Parameter> dparams = _project.getDiscreteParameters();
        if (dparams.size() > 0) {
            StringBuffer dparams_tr = new StringBuffer("Defined parameters:\n\n");
            dparams_tr.append("| Group | Name | Values |\n");
            dparams_tr.append("|-------|------|--------|\n");
            for (Parameter v : dparams) {
                if (v.isGroup()) {
                    LinkedList<Variable> gvars = ((VarGroup) v).getVariables();
                    for (Variable gv : gvars) {
                        dparams_tr.append(dparam_tr_tmpl
                                .replace("__dparam_name__", gv.getName())
                                .replace("__dparam_grp__", v.getName())
                                .replace("__dparam_values__", gv.getValuesString()));
                        dparams_tr.append(EOL);
                    }
                } else {
                    dparams_tr.append(dparam_tr_tmpl
                            .replace("__dparam_name__", v.getName())
                            .replace("__dparam_grp__", "     ")
                            .replace("__dparam_values__", ((Variable) v).getValuesString()));
                    dparams_tr.append(EOL);
                }
            }
            modelbuffer = replace(modelbuffer, "__dparams_tr__", dparams_tr.toString());
        } else {
            modelbuffer = replace(modelbuffer, "__dparams_tr__", "");
        }

        //continuous params
        List<Parameter> cparams = _project.getContinuousParameters();
        StringBuilder cparams_tr = new StringBuilder();
        if (cparams.size() > 0) {
            cparams_tr.append("Engineering parameters:\n\n");
            cparams_tr.append("| Name | Bounds |\n");
            cparams_tr.append("|------|--------|\n");
            for (int i = 0; i < cparams.size(); i++) {
                Parameter v = cparams.get(i);
                cparams_tr.append(cparam_tr_tmpl.replace("__cparam_name__", v.getName()).replace("__cparam_values__", ((Variable) v).getValuesString()));
                cparams_tr.append(EOL);
            }
        }
        modelbuffer = replace(modelbuffer, "__cparams_tr__", cparams_tr.toString());

        return modelbuffer;
    }

    final static String design_tr_tmpl = "__dparams_value_td__| __design_value__ | __designfiles__ |";
    final static String design_a_tmpl = "[__design_file__](__vars_path__/__design_file__)";

    static String getResults(Project _project) {
        String resbuffer = "\n# Results\n\n"
                + "__dparams_name_th__| __design__ | Files |\n"
                + "__dparams_line_th__|------------|-------|\n"
                + "__design_tr__\n"
                + "\n";
        String design = null;

        StringBuilder vars_name_th = new StringBuilder();
        StringBuilder vars_line_th = new StringBuilder();
        for (Parameter param : _project.getDiscreteParameters()) {
            vars_name_th.append("| ").append(param.getName());
            vars_line_th.append("|----");
        }
        if (_project.getDiscreteParameters().size() > 0) {
            vars_name_th.append(" ");
            vars_line_th.append("-");
        }

        resbuffer = replace(resbuffer, "__dparams_name_th__", vars_name_th.toString());
        resbuffer = replace(resbuffer, "__dparams_line_th__", vars_line_th.toString());
        resbuffer = replace(resbuffer, "__design__", _project.getDesigner().getDesignOutputTitle());

        CaseList cases = _project.getDiscreteCases();
        StringBuilder designs_tr = new StringBuilder();
        if (cases.size() == 1) {
            Case c = cases.get(0);

            String varspath = "." + c.getRelativePath().replace("\\" + File.separator, "/");

            StringBuilder casefiles_a = new StringBuilder();
            File[] caseFiles = _project.getCaseResultDir(c).listFiles(new FileFilter() {

                public boolean accept(File f) {
                    return f.isFile();
                }
            });
            if (caseFiles != null) {
                for (int j = 0; j < caseFiles.length; j++) {
                    casefiles_a.append(design_a_tmpl.replace("__vars_path__", varspath).replace("__design_file__", caseFiles[j].getName()));
                    if (j < caseFiles.length - 1) {
                        casefiles_a.append(" \\\\ ");
                    }
                }
            }
            design = c.getDesignSession().getAnalysis().replace(DesignHelper.BASE, ".");
            if (design == null) {
                design = "?";
            }
            designs_tr.append(design_tr_tmpl.replace("__dparams_value_td__", " ").replace("__design_value__", design).replace("__designfiles__", casefiles_a.toString()));

        } else {
            for (Case c : cases) {
                StringBuilder vars_value_td = new StringBuilder("| ");
                Map<String, String> case_vars = _project.getCaseParameters(c);
                //System.out.println("c=" + c.getName());
                //System.out.println("  case_vars=" + case_vars);
                for (Parameter p : _project.getDiscreteParameters()) {
                    if (p.isGroup() && !case_vars.containsKey(p.getName())) { // group without alias, i.e. display x1,x2=0,1 instead of alias
                        StringBuilder displ = new StringBuilder();
                        StringBuilder dispr = new StringBuilder();
                        for (Parameter v : p.getGroupComponents()) {
                            displ.append(v.getName());
                            displ.append(Node.GROUP_SEPARATOR);
                            dispr.append(case_vars.get(v.getName()));
                            dispr.append(Node.GROUP_SEPARATOR);
                        }
                        displ.deleteCharAt(displ.length() - 1);
                        dispr.deleteCharAt(dispr.length() - 1);
                        vars_value_td.append(displ.toString()).append("=").append(dispr.toString());
                    } else {
                        vars_value_td.append(case_vars.get(p.getName()));
                    }
                }

                String varspath = "." + c.getRelativePath().replace("\\" + File.separator, "/");

                StringBuilder casefiles_a = new StringBuilder();
                File[] caseFiles = _project.getCaseResultDir(c).listFiles(new FileFilter() {

                    public boolean accept(File f) {
                        return f.isFile();
                    }
                });
                if (caseFiles != null) {
                    for (int j = 0; j < caseFiles.length; j++) {
                        casefiles_a.append(design_a_tmpl.replace("__vars_path__", varspath).replace("__design_file__", caseFiles[j].getName()));
                        if (j < caseFiles.length - 1) {
                            casefiles_a.append(" \\\\ ");
                        }
                    }
                }
                design = c.getDesignSession().getAnalysis().replace(DesignHelper.BASE, ".");
                if (design == null) {
                    design = "?";
                }
                designs_tr.append(design_tr_tmpl
                        .replace("__dparams_value_td__", vars_value_td.toString())
                        .replace("__design_value__", design)
                        .replace("__designfiles__", casefiles_a.toString()));

                designs_tr.append(EOL);
            }
        }
        resbuffer = replace(resbuffer, "__design_tr__", designs_tr.toString());
        return resbuffer;
    }

    final static String case_tr_tmpl = "__vars_value_td____result_value__ | __files__ | __inputfiles__ | __outputfiles__ |";
    final static String file_a_tmpl = "[__file__](__vars_path__/__file__)";
    final static String inputfile_a_tmpl = "[__input_file__](__vars_path__/input/__input_file__)";
    final static String outputfile_a_tmpl = "[__output_file__](__vars_path__/output/__output_file__)";

    static String getCalcs(Project _project) {
        String calcs = "\n# Calculations\n\n"
                + "__vars_name_th____result_name_th__| Directory | Input | Output |\n"
                + "__vars_line_th____result_line_th__|-----------|-------|--------|\n"
                + "__cases_tr__\n"
                + "\n";

        Set<String> interKeys = null;

        StringBuilder vars_name_th = new StringBuilder();
        StringBuilder vars_line_th = new StringBuilder();

        for (Variable v : _project.getVariables()) {
            vars_name_th.append("| ").append(v.getName());
            vars_line_th.append("|----");
        }
        vars_name_th.append(" ");
        vars_line_th.append("-");

        vars_name_th.append(__INTER__);
        vars_line_th.append(__INTER_LINE__);

        StringBuilder result_name_th = new StringBuilder();
        StringBuilder result_line_th = new StringBuilder();
        for (OutputFunctionExpression f : _project.getOutputFunctionsList()) {
            result_name_th.append("| ").append(f.toNiceSymbolicString());
            result_line_th.append("|---------");
        }
        result_name_th.append(" ");
        result_line_th.append("-");

        calcs = replace(calcs, "__vars_name_th__", vars_name_th.toString());
        calcs = replace(calcs, "__vars_line_th__", vars_line_th.toString());
        calcs = replace(calcs, "__result_name_th__", result_name_th.toString());
        calcs = replace(calcs, "__result_line_th__", result_line_th.toString());

        CaseList cases = _project.getCases();
        StringBuilder cases_tr = new StringBuilder();
        for (Case c : cases) {
            Log.out("Printing Case " + c.getName(), 4);
            try {
                StringBuilder vars_value_td = new StringBuilder();
                Map<String, String> case_vars = _project.getCaseParameters(c);
                for (Variable v : _project.getVariables()) {
                    vars_value_td.append("| ")
                            .append(case_vars.get(v.getName()))
                            .append(" ");
                }
                //System.err.println("case_vars "+case_vars);

                String varspath = "." + c.getRelativePath().replace("\\" + File.separator, "/");

                StringBuilder casefiles_a = new StringBuilder();
                //System.out.println("  Appending case "+c.getName());
                File[] caseFiles = _project.getCaseResultDir(c).listFiles();
                if (caseFiles != null) {
                    for (int j = 0; j < caseFiles.length; j++) {
                        casefiles_a.append(file_a_tmpl
                                .replace("__vars_path__", varspath)
                                .replace("__file__", caseFiles[j].getName()));
                        if (j < caseFiles.length - 1) {
                            casefiles_a.append(" \\\\ ");
                        }
                    }
                }
                //System.err.println("casefiles_a "+casefiles_a);

                StringBuilder caseinputfiles_a = new StringBuilder();
                //System.out.println("  Appending case "+c.getName());
                File caseinput = new File(_project.getCaseResultDir(c) + File.separator + Constants.INPUT_DIR);
                if (caseinput.isDirectory()) {
                    File[] caseinputFiles = caseinput.listFiles();
                    if (caseinputFiles != null) {
                        for (int j = 0; j < caseinputFiles.length; j++) {
                            caseinputfiles_a.append(inputfile_a_tmpl
                                    .replace("__vars_path__", varspath)
                                    .replace("__input_file__", caseinputFiles[j].getName()));
                            if (j < caseinputFiles.length - 1) {
                                caseinputfiles_a.append(" \\\\ ");
                            }
                        }
                    }
                } else if (caseinput.isFile()) {
                    caseinputfiles_a.append( "```\n" + getASCIIFileContent(caseinput) + "\n```");
                }

                StringBuilder caseoutputfiles_a = new StringBuilder();
                //System.out.println("   path is "+_project.getCaseResultDir(c).getPath());
                File caseoutput = new File(_project.getCaseResultDir(c) + File.separator + Constants.OUTPUT_DIR);
                if (caseoutput.isDirectory()) {
                    File[] caseoutputFiles = caseoutput.listFiles();
                    if (caseoutputFiles != null) {
                        for (int j = 0; j < caseoutputFiles.length; j++) {
                            caseoutputfiles_a.append(outputfile_a_tmpl
                                    .replace("__vars_path__", varspath)
                                    .replace("__output_file__", caseoutputFiles[j].getName()));
                            if (j < caseoutputFiles.length - 1) {
                                caseoutputfiles_a.append(" \\\\ ");
                            }
                        }
                    }
                } else if (caseoutput.isFile()) {
                    caseoutputfiles_a.append( "```\n" + getASCIIFileContent(caseoutput) + "\n```");
                }
                //System.err.println("caseoutputfiles_a "+caseoutputfiles_a);

                Map<String, Object> interValues = null;
                try {
                    interValues = ProjectController.getCaseIntermediates(_project, c.getIndex());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (interKeys == null && interValues != null) {
                    interKeys = interValues.keySet();
                }
                if (interKeys != null && interKeys.size() > 0) {
                    StringBuilder htmlinter = new StringBuilder();
                    StringBuilder htmlinterline = new StringBuilder();
                    for (String ki : interKeys) {
                        htmlinter.append("| ").append(ki);
                        htmlinterline.append("|----").append(ki);
                    }
                    htmlinter.append(" ");
                    htmlinterline.append("-");

                    calcs = replace(calcs, __INTER__, htmlinter.toString());
                    calcs = replace(calcs, __INTER_LINE__, htmlinterline.toString());
                } else {
                    calcs = replace(calcs, __INTER__, "");
                    calcs = replace(calcs, __INTER_LINE__, "");
                }
                //System.err.println("calcs "+calcs);

                //workaround to update case information if case list was cleared before...
                Map<String, Object> outputValues = null;
                try {
                    outputValues = ProjectController.getCaseOutputs(_project, c.getIndex(), false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //outputValues.put(ParseExpression.FILES, caseoutputFiles);
                StringBuilder result_val = new StringBuilder();
                try {
                    if (outputValues == null) {
                        for (OutputFunctionExpression f : _project.getOutputFunctionsList()) {
                            result_val.append("| ? ");
                        }
                    } else {
//                        for (String ki : outputValues.keySet()) {
//                            result_val.append("| ").append(outputValues.get(ki));
//                        }
//                        result_val.append(" ");

                        c.setOutputValues(outputValues);
                        for (OutputFunctionExpression f : _project.getOutputFunctionsList()) {
                            Object hr = c.doEval(f);
                            String r = null;
                            if (hr != null) {
                                r = f.toNiceNumericString(hr);
                            } else {
                                r = "?";
                            }

                            result_val.append("| ").append(r);
                        }
                        result_val.append(" ");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.err(e, 1);
                    result_val.append("| ").append(e.getLocalizedMessage());
                }
                //System.err.println("result_val "+result_val);
                cases_tr.append(case_tr_tmpl
                        .replace("__vars_value_td__", vars_value_td.toString())
                        .replace("__result_value__", result_val.toString())
                        .replace("__files__", casefiles_a.toString())
                        .replace("__inputfiles__", caseinputfiles_a.toString())
                        .replace("__outputfiles__", caseoutputfiles_a.toString()));
            } catch (Exception e) {
                e.printStackTrace();
                Log.err(e, 1);
                cases_tr.append("| ").append(toHTML(e.getLocalizedMessage()));
            }
            cases_tr.append(EOL);
        }

        calcs = replace(calcs, "__cases_tr__", cases_tr.toString());
        return calcs;
    }

    public static String buildReport(Project _project) {
        Log.out("Writing report for project " + _project.getName(), 4);

        String reportbuffer = "---\n"
                + "title: '__project_name__'\n"
                + "author: '__user_name__'\n"
                + "date: '__date__'\n"
                + "---\n"
                + "\n"
                + "__general_information__\n"
                + "\n"
                + "__modeling_information__\n"
                + "\n"
                + "__results__\n"
                + "\n"
                + "__calculations__";

        try {
            try {
                reportbuffer = replace(reportbuffer, "__project_name__", _project.getName());
                reportbuffer = replace(reportbuffer, "__user_name__", System.getProperty("user.name"));
                reportbuffer = replace(reportbuffer, "__date__", Calendar.getInstance().getTime().toString());
            } catch (Exception e) {
                Log.err(e, 3);
                reportbuffer = replace(reportbuffer, "__project_name__", e.getMessage());
            }

            try {
                reportbuffer = replace(reportbuffer, "__general_information__", getGenInfo(_project));
            } catch (Exception e) {
                Log.err(e, 3);
                reportbuffer = replace(reportbuffer, "__general_information__", e.getMessage());
            }

            try {
                reportbuffer = replace(reportbuffer, "__modeling_information__", getModelInfo(_project));
            } catch (Exception e) {
                Log.err(e, 3);
                reportbuffer = replace(reportbuffer, "__modeling_information__", e.getMessage());
            }

            try {
                if (_project.getDesigner() == null) {
                    reportbuffer = replace(reportbuffer, "__results__", "");
                } else {
                    reportbuffer = replace(reportbuffer, "__results__", getResults(_project));
                }
            } catch (Exception e) {
                Log.err(e, 3);
                reportbuffer = replace(reportbuffer, "__results__", e.getMessage());
            }

            try {
                reportbuffer = replace(reportbuffer, "__calculations__", getCalcs(_project));
            } catch (Exception e) {
                Log.err(e, 3);
                reportbuffer = replace(reportbuffer, "__calculations__", e.getMessage());
            }
        } catch (Exception e) {
            Log.err(e, 3);
            return e.getMessage();
        }

        return reportbuffer;
    }

    public static void main(String[] args) throws Exception {
        for (String arg : args) {
            File project = new File(arg);
            if (project.isDirectory()) {
                project = new File(project, "project.xml");
            }
            if (project.isFile()) {
                Project p = new Project(project);
                System.out.println(Report.buildReport(p));
            } else {
                //System.err.println("Could not find file " + project.getAbsolutePath());
            }
        }
    }

    static String replace(String in, String exp, String content) {
        try {
            if (content == null) {
                content = "?";
            }
            return in.replace(exp, content);
        } catch (Exception e) {
            return in;
        }
    }
}
