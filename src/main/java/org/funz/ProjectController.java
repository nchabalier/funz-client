package org.funz;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import org.apache.commons.io.FileUtils;
import static org.funz.Project.FILES_DIR;
import static org.funz.Project.PROJECT_FILE;
import org.funz.conf.Configuration;
import org.funz.doeplugin.DesignConstants;
import org.funz.doeplugin.DesignPluginsLoader;
import org.funz.ioplugin.IOPluginInterface;
import org.funz.log.Log;
import org.funz.log.LogCollector.SeverityLevel;
import org.funz.parameter.Case;
import org.funz.parameter.CaseList;
import org.funz.parameter.Parameter;
import org.funz.parameter.SyntaxRules;
import org.funz.parameter.VarGroup;
import org.funz.parameter.Variable;
import org.funz.util.ASCII;
import org.funz.util.Disk;
import static org.funz.util.ParserUtils.getASCIIFileContent;

public class ProjectController {

    public static Map<String, Object> getCaseIntermediates(Project prj, int caseIdx) {
        return prj.getCases().get(caseIdx).getIntermediateValues();
    }

    public static class RendererParamHolder {
        //      LERD
        //public String groupMember[];

        public String resultName;
        public ArrayList<String[]> values;
        public String varNames[];
        public String[] varTypes;
    }

    /// Used to retrieve result values like keff.
    public static interface ResultDriver {

        /**
         * Called each time a calculation case is over.
         *
         * @param prj project
         * @param caseIdx current case
         * @param isHot says whether we are in dynamic or static post-processing
         */
        public Map<String, Object> readCaseOutputs(Project prj, int caseIdx, boolean isHot) throws Exception;
    }
    public static ResultDriver _driver = new FunzResultDriver();
    static final String LAST_OPEN_DIR = "FUNZ2/LASTOPENDIR", ORIG_FILE_PATH = "orig.file.path";
    public static final String PROJECT_DIR_SUFFIX = "pmp";
    public static final String PROJECT_DIR_EXTENSION = "." + PROJECT_DIR_SUFFIX;

    public static String buildToolTipText(Project prj) {
        File files[] = prj.getFilesAsArray();
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");

        for (File f : files) {
            sb.append(f.getName());
            sb.append("<br>");

        }
        sb.append("</html>");

        return sb.toString();
    }

    public static Project createProject(String name, File f, String model, IOPluginInterface plugin) throws Exception {
        File dir = null;
        if (Project.getDirectoryForProject(f.getName()).exists()) {
            dir = new File(Project.getDirectoryForProject(f.getName()).getAbsolutePath() + "_" + Configuration.timeDigest());
            Log.out("Project directory " + Project.getDirectoryForProject(f.getName()) + " already taken. Using " + dir + " instead.", 0);
        }

        Project prj = new Project(name, dir);
        setupProject(prj, name, f, model, plugin);
        return prj;
    }

    public static void setupProject(Project prj, String name, File f, String model, IOPluginInterface plugin) throws Exception {
        try {
            if (prj.getResultsDir().isDirectory()) {
                File olddir = new File(prj.getOldResultsDir(), Configuration.timeDigest());
                if (prj.getOldResultsDir().getParentFile().equals(prj.getResultsDir())) {
                    for (File ff : prj.getResultsDir().listFiles()) {
                        if (ff.isFile()) {
                            FileUtils.moveFileToDirectory(ff, olddir, true);
                        } else if (ff.isDirectory() && !ff.equals(prj.getOldResultsDir())) {
                            FileUtils.moveDirectoryToDirectory(ff, olddir, true);
                        } else {
                            Log.out("Cannot move " + ff + " to " + olddir,3);
                        }
                    }
                } else {
                    Disk.moveDir(prj.getResultsDir(), olddir);
                }
            }

            prj.setModel(model);
            prj.setPlugin(plugin);
            prj.setCode(Configuration.getCode(model));

            prj.setVariableSyntax(new SyntaxRules(plugin.getVariableStartSymbol(), plugin.getVariableLimit()));
            prj.setFormulaSyntax(new SyntaxRules(plugin.getFormulaStartSymbol(), plugin.getFormulaLimit()));

            Map<String, String> tvalues = new HashMap<String, String>();
            tvalues.put(ORIG_FILE_PATH, f.getPath());
            prj.setTaggedValues(tvalues);

            prj.importFileOrDir(f);

            prj.buildParameterList();

            prj.saveInSpool();
        } catch (Exception e) {
            if (prj != null) {
                File dir = prj.getDirectory();
                if (dir.exists()) {
                    Disk.removeDir(dir);
                }
            }
            throw e;
        }
    }

    public static long estimateResultsVolume(Project prj) {
        long size = -1;
        try {
            size = Disk.getDirSize(prj.getResultsDir()/* Dir(getResultsDirName())*/);
        } catch (Exception e) {
        }
        return size;
    }

    public static Map<String, Object> getCaseOutputs(Project prj, int caseIdx, boolean isHot) throws Exception {
        if (_driver == null) {
            return new HashMap<String, Object>();
        } else {
            return _driver.readCaseOutputs(prj, caseIdx, isHot);
        }
    }

    public static Map<String, Object> getCaseInputs(Project prj, int caseIdx) {
        if (prj.getCases().size() < caseIdx) {
            Log.logMessage(prj, SeverityLevel.ERROR, true, "Case " + caseIdx + " does not exist !");
            return null;
        }
        return prj.getCases().get(caseIdx).getInputValues();
    }

    public static String[][] getDiscreteParameters(Project prj, Case c) {
        String[] varNames = new String[prj.getDiscreteParameters().size()];
        String[] varValues = new String[prj.getDiscreteParameters().size()];

        int i = 0;
        for (Parameter p : prj.getDiscreteParameters()) {
            varNames[i] = c.getParamName(p.getIndex());
            varValues[i] = c.getParamValue(p.getIndex());
            i++;
        }

        return new String[][]{varNames, varValues};

    }

    public static List<Case> getContinousCases(Project prj, int disCaseId) {
        LinkedList<Case> cases = new LinkedList<Case>();

        for (Case c : prj.getCases()) {
            if (c.getDiscreteCaseId() == disCaseId) {
                cases.add(c);
            }
        }
        return cases;
    }


    /*public static HashMap<String, Object>[] readCaseOutputs(Project prj, LinkedList<Case> cases, boolean isHot) {
     if (_driver == null) {
     System.err.println("ProjectController.readCaseOutputs : No driver found!");
     return null;
     } else {
     HashMap<String, Object>[] ret = new HashMap[cases.size()];
     int i = 0;
     for (Case c : cases) {
     ret[i++] = _driver.readCaseOutputs(prj, c, isHot);
     }
    
     return ret;
     }
     }*/
    public static File getLastOpenDir() {
        File dir = new File(Preferences.userRoot().get(LAST_OPEN_DIR, ""));

        return dir.exists() ? dir : null;
    }

    public static File getProjectOriginalFileDir(Project prj) {
        if (prj.getTaggedValues().containsKey(ORIG_FILE_PATH)) {
            String filename = prj.getTaggedValues().get(ORIG_FILE_PATH);
            if (filename != null) {
                File f = new File(filename);
                if (f.getParentFile().isDirectory()) {
                    return f.getParentFile();
                } else {
                    //MessageDialog.showError("Directory " + f.getParentFile() + " no longer available.", 5000);
                    return getLastOpenDir();
                }
            }
        }
        return getLastOpenDir();
    }

    public static File getProjectOriginalFile(Project prj) {
        if (prj.getTaggedValues().containsKey(ORIG_FILE_PATH)) {
            String filename = prj.getTaggedValues().get(ORIG_FILE_PATH);
            if (filename != null) {
                File f = new File(filename);
                if (f.isFile()) {
                    return f;
                } else {
                    try {
                        File parent = f.getParentFile();
                        while (!parent.isDirectory()) {
                            parent = parent.getParentFile();
                        }
                        if (parent != null) {
                            return new File(parent, f.getName());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }

    public static Project loadProject(String name, boolean loadCases, boolean loadDesignSessions, boolean deleteIfFailed) throws Exception {
        Project prj = null;
        try {
            //String name = name;//f.getName();
            File dir = Project.getDirectoryForProject(name);

            if (!dir.exists()) {
                return null;
            }

            prj = new Project(new File(dir + File.separator + Project.PROJECT_FILE));
            prj.buildParameterList();
            if (loadCases) {
                prj.loadCases();
            }
            if (loadDesignSessions) {
                prj.loadDesignSessions();
            }
        } catch (Exception e) {
            if (deleteIfFailed && prj != null) {
                File dir = Project.getDirectoryForProject(name/*f.getName()*/);
                if (dir.exists()) {
                    Disk.removeDir(dir);
                }

            }
            throw e;
        } finally {
            // TODO optimiser
            //System.gc();
        }

        return prj;
    }

    public static String makeVarList(Project prj) {
        StringBuilder sb = new StringBuilder();
        for (Variable o : prj.getVariables()) {
            sb.append((o).getName());
            sb.append(" ");
        }
        return sb.toString();
    }

    public static void prepareRenderingDiscreteParams(Project prj, RendererParamHolder rparams) {
        if (prj == null) {
            return;
        }

        int caseCount = prj.getNmOfDiscreteCases();
        List<Parameter> dparams = prj.getDiscreteParameters();
        //to get only discrete variables, no groups
        LinkedList<Variable> dvars = new LinkedList<Variable>();
        for (Parameter parameter : dparams) {
            if (parameter.isGroup()) {
                dvars.addAll(((VarGroup) parameter).getVariables());
            } else {
                dvars.add((Variable) parameter);
            }

        }

        int nvars = dvars.size();

        if (rparams.resultName == null) {
            if (prj.getDesignerId().equals(DesignConstants.NODESIGNER_ID)) {
                rparams.resultName = prj.getMainOutputFunction().toNiceSymbolicString();
            } else {
                rparams.resultName = prj.getDesigner() == null ? DesignPluginsLoader.newInstance(prj.getDesignerId()).getDesignOutputTitle() : prj.getDesigner().getDesignOutputTitle();
            }
        }

        if (nvars == 0) {
            rparams.varNames = new String[]{Project.SINGLE_PARAM_NAME};
            rparams.varTypes = new String[]{Variable.TYPE_STRING};
            rparams.values = new ArrayList<String[]>();
            rparams.values.add(new String[]{""});
        } else {
            CaseList cases = prj.getDiscreteCases();
            if (cases == null) {
                prj.resetDiscreteCases(null);
                cases = prj.getDiscreteCases();
            }

            rparams.varNames = new String[nvars];
            rparams.varTypes = new String[nvars];
            rparams.values = new ArrayList<String[]>(caseCount);
            for (int i = 0; i < caseCount; i++) {
                rparams.values.add(new String[nvars]);
            }

            int v = 0;
            for (Variable var : dvars) {
                rparams.varNames[v] = var.getName() + (var.getGroup() != null ? " [" + var.getGroup().getName() + "]" : "");
                rparams.varTypes[v] = var.getType();
                int row = 0;
                for (Case c : cases) {
                    rparams.values.get(row)[v] = prj.getCaseParameters(c).get(var.getName());
                    row++;
                }
                v++;
            }
        }
    }

    public static List<String[]> prepareAddedCases(Project prj, int nmOfAddedCases) {
        ArrayList<String[]> ret = new ArrayList<String[]>();
        ret.ensureCapacity(nmOfAddedCases);

        int caseCount = prj.getNmOfDiscreteCases();
        LinkedList<Variable> vars = prj.getVariables();
        int nvars = vars.size();
        CaseList cases = prj.getCases();
        String varNames[] = new String[nvars];

        for (int i = 0; i < nmOfAddedCases; i++) {
            String strs[] = new String[nvars];
            Case c = cases.get(caseCount - nmOfAddedCases + i);
            Map<String, String> cvars = prj.getCaseParameters(c);
            int col = 0;
            for (Variable var : vars) {
                strs[col] = cvars.get(var.getName());
                col++;
            }

            ret.add(strs);
        }

        return ret;
    }

    public static Project reloadProject(File projectdir, boolean withresults) throws Exception {
        File oldPrjFile = new File(projectdir + File.separator + Project.PROJECT_FILE);
        Project oldprj = new Project(oldPrjFile);
        Project prj = null;
        try {
            File newdir = Project.getDirectoryForProject(oldprj.getName());

            if (!projectdir.getParent().equals(Project.getDefaultRepository())) { // else this project is already in projects directory
                if (newdir.exists()) {
                    Disk.emptyDir(newdir);
                } else {
                    newdir.mkdir();
                }

                Disk.copyDir(projectdir, newdir);
            }

            prj = loadProject(newdir.getName(), withresults, withresults, false);
            if (prj._tvalues.containsKey(ORIG_FILE_PATH)) {
                prj._tvalues.put(ORIG_FILE_PATH, projectdir.getAbsolutePath().replace(".pmp", ""));
            }
            prj.saveProject(newdir, null);
        } catch (Exception e) {
            if (prj != null) {
                File dir = Project.getDirectoryForProject(oldprj.getName());
                if (dir.exists()) {
                    Disk.removeDir(dir);
                }

            }
            throw e;
        }

        return prj;
    }

    public static Project copyProject(Project init, String newname) throws Exception {
        Project prj = null;
        String oldname = init.getName();
        try {
            File newdir = Project.getDirectoryForProject(newname);
            if (newdir.exists()) {
                Disk.emptyDir(newdir);
            } else {
                newdir.mkdir();
            }

            Disk.copyDir(init.getDirectory(), newdir);
            File files_dir = new File(newdir + File.separator + FILES_DIR);
            File input0 = new File(files_dir, oldname);
            if (input0.isFile()) {
                input0.renameTo(new File(files_dir, newname));
            }

            File prj_file = new File(newdir, PROJECT_FILE);
            if (prj_file.isFile()) {
                ASCII.saveFile(prj_file, getASCIIFileContent(prj_file).replace(oldname, newname));
            }
            File results_csv = new File(newdir, init.getName() + Report.RESULTSCSV);
            if (results_csv.isFile()) {
                results_csv.delete();
            }
            File calculations_csv = new File(newdir, init.getName() + Report.CALCULATIONSCSV);
            if (calculations_csv.isFile()) {
                calculations_csv.delete();
            }

            //File newPrjFile = new File(newdir + File.separator + Project.PROJECT_FILE);
            //Disk.copyFile(oldPrjFile, newPrjFile);
            // Disk.copyDir(new File(resdir + File.separator + Project.FILES_DIR), new File(newdir + File.separator + Project.FILES_DIR));
            //prj.resultsPool.add(new File(resdir + File.separator + Project.OLD_DIR));
            prj = loadProject(newdir.getName(), false, false, true);

            prj.setName(newname);

        } catch (Exception e) {
            if (prj != null) {
                File dir = Project.getDirectoryForProject(newname);
                if (dir.exists()) {
                    Disk.removeDir(dir);
                }

            }
            throw e;
        }

        return prj;
    }

    public static void removeProject(Project prj) throws Exception {
        if (prj == null) {
            return;
        }

        File dir = prj.getRootDirectory();
        if (dir.exists()) {
            Disk.removeDir(dir);
        }
    }

    public static void setLastOpenDir(File dir) {
        Preferences.userRoot().put(LAST_OPEN_DIR, dir.getPath());
    }
}
