/*
 * Created on 14 juin 06 by richet
 */
package org.funz.ioplugin;

import org.funz.Project;
import org.funz.parameter.InputFile;
import org.funz.parameter.OutputFunctionExpression;
import org.funz.script.MathExpression;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Interace for code specific information and behaviour: documentation, links,
 * syntax, ...
 */
public interface IOPluginInterface {

    public MathExpression getFormulaInterpreter();

    public void setFormulaInterpreter(MathExpression engine);

    /**
     * Test if given file is a suitable input for code
     *
     * @param f file to test
     * @return test result
     */
    public boolean acceptsDataSet(File f);

    /**
     * Test if given directory is a suitable input for code
     *
     * @param f dir to test
     * @return test result
     */
    public boolean acceptsDataDirectory(File f);

    /**
     * Get the comment line for this code
     *
     * @return string of line comment
     */
    public String getCommentLine();

    public String getSource();

    /**
     * Get delimiters of formula. () = 0 {} = 1 [] = 2 <> = 3
     *
     * @return int matching formula delimiter
     */
    public int getFormulaLimit();

    /**
     * Get symbol prefix of formula. $ = 0 # = 1 � = 2 & = 3
     *
     * @ = 4
     * ! = 5
     * % = 6
     * @return int matching formula character prefix
     */
    public int getFormulaStartSymbol();

    public File[] getInputFiles();

    public void setProject(Project p);

    public Project getProject();

    /**
     * Returns information
     *
     * @return String information
     */
    public String getPluginInformation();

    /**
     * Returns the associated files to given dataset file
     *
     * @param f main dataset file
     * @return LinkedList<File> all files including given one
     */
    public List<File> getRelatedFiles(File f);

    /**
     * Get delimiters of variable. () = 0 {} = 1 [] = 2
     * <> = 3
     *
     * @return int matching variable delimiter
     */
    public int getVariableLimit();

    /**
     * Get symbol prefix of variables. $ = 0 # = 1 � = 2 & = 3
     *
     * @ = 4
     * ! = 5
     * % = 6
     * @return int matching variable character prefix
     */
    public int getVariableStartSymbol();

    /**
     * Get output names from input files
     *
     * @return String information
     */
    public String[] getOutputNames();

    /**
     * Get a map of output names associated to a default fake value for this
     * output
     *
     * @return HashMap<String, Object> default output
     */
    public Map<String, Object> getVoidOutput();

    /**
     * Extract results names&values from result directory and files
     *
     * @return HasMap<String,Object> list of names&values of results
     */
    public Map<String, Object> readOutput(File outputdir) throws Exception;

    /**
     * Get proposed output by plugin. Will be displayed in user interface.
     *
     * @return LinkedList<OutputFunctionExpression> default output expressions
     */
    public LinkedList<OutputFunctionExpression> suggestOutputFunctions();

    public void initializeDefaultDisplayedOutput();

    public List<String> getDefaultDisplayedOutput();

    public void initializeOutputFormat();

    public Map<String, String> getOutputFormat();

    /**
     * Get numerical rounding for this code
     *
     * @return int decimal rounding
     */
    public int getRoundOff();

    /**
     * Set formula syntax
     *
     * @param int forumla start char id
     * @param int forumla delimiter id
     */
    public void setFormulaSyntax(int s, int l);

    /**
     * Set comment line character syntax
     *
     * @param String comment line syntax
     */
    public void setCommentLine(String c);

    void setID(String pluginFileName);

    String getID();

    /**
     * Set input files for this plugin. Also initialize the possible output that
     * will be available in the end of a calculation.
     *
     * @param File[] input files
     */
    public void setInputFiles(File[] inputFilesAsArray);

    /**
     * Set variables syntax
     *
     * @param int variables start char id
     * @param int variables delimiter id
     */
    public void setVariableSyntax(int s, int l);

    /**
     * Patch method (can call a dedicated GUI or automated patcher) to be called
     * when a case of calculation failed.
     *
     * @param File input directory to be patched
     * @return boolean input patched, so calculation to be restarted
     */
    public boolean inputDirRetryPatch(File file);

    /**
     * Import (copy & instanciate as InputFile) input files.
     *
     * @param File directory or file to import (possibly recursive)
     * @return InputFile[] imported InputFile objects, copied in project "files"
     * dir
     */
    public InputFile[] importFileOrDir(File src) throws Exception;
}
