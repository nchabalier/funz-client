package org.funz.api;

import java.io.File;
import java.util.Map;

/**
 */
public interface UnifiedShell {

    /**
     * Add a property to the project
     * @param property project property (retries, ...)
     * @param value project property value
     */
    void setProjectProperty(String property, String value);

    String getProjectProperty(String property);

    String[] getProjectPropertyKeys();

    /**
     * Defines code plugin to use.
     * @param codePluginName
     * @return
     */  /**
     * Input files given this way will be parsed and variables extracted.
     * You can get these variables using getUndefinedVariables() before setting them with setVariable() and (if needed) setVariableProperty().
     */
    void setInputModel(String modelName, File... fles) throws Exception;

    String getModel();

    /**
     * Defines doe plugin to use.
     * @param doePluginName
     */
    void setDesigner(String designerName) throws Exception;

    String getDesigner();

    void setDesignOption(String option, String value);

    String getDesignerOption(String option);

    String[] getDesignerOptionKeys();

    File[] getInputFiles();

    /**
     * Use this after setInputFiles() to get all available variables.
     * @return all variables in the project.
     */
    String[] getInputVariables();

    /**
     * Use this after setInputFiles() to get variables still not defined.
     * @return variable which still need to be defined.
     */
    String[] getUndefinedInputVariables();

    /**
     * Set undefined variable values and name
     * @param varName variables name or key
     * @param values discrete variables values
     */
    void setInputVariable(String varName, Object model) throws Exception;

    /**
     * Set undefined variable values and name
     * @param varName variables name or key
     * @param values discrete variables values
     */
    void setInputVariablesGroup(String groupName, Map/*<String, String[]>*/ var_model) throws Exception;

    /**
     * Add a property to the variable named varName
     * @param varName variables name or key
     * @param property variables property key
     * @param property variables property value
     */
    void setInputVariableProperty(String varName, String property, String value);

    Map<String, String[]> getInputDesign();

    String[] getOutputAvailable();

    void setOutputExpressions(String... expressions);

    String[] getOutputExpressions();

    /**
     * Return computation results.
     * You can only use this after computation ended.
     * @return A map containing computation results summary, parsed by the plugin. Objects nature may vary depending on the plugin used.
     */
    Map<String, Object[]> getResultsArrayMap();

    Map<String, String[]> getResultsStringArrayMap();

    Object[] getResultsArray(String dataName);
    
    String[] getResultsStringArray(String dataName);

    /**
     * Set verbosity level for error and/or output stream.
     * @param level new level of verbosity
     */
    void setVerbosity(int level);

    /**
     * Get informations on all calculators available.
     * @return A map typed like this : <calculator id, informations about this calculator>
     */
    Map<String, String[]> getCalculatorsInformation();

    /**
     * Get the available code plugins on the dispatcher.
     * On funz v2 also updates them if needed.
     * @return Plugins names.
     */
    String[] getModelList();

    /**
     * Get the available doe plugins on the dispatcher.
     * On funz v2 also updates them if needed.
     * @return Plugins names.
     */
    String[] getDesignList();

    /**
     * If project is correctly and enough set, launch calculation process.
     * FunzV2: client must be connected.
     * @return if computation started correctly.
     */
    boolean startComputation();

    /**
     * Asks for all calculation point to stop.
     * @return if calculation process stopped correctly
     */
    boolean stopComputation();

    /**
     * Return id's and status of all calculation point of current projects calculation.
     * @return A map typed like this: <Calculation point ID, status message>
     */
    Map<String, String> getCalculationPointsStatus();

    String getCalculationPointsStatus(String calculationPointID);
    /**
     * Ask status of given calculation point
     * @param calculationPointID ID of the calculation point which status we're asking for.
     * @return Calculation point status.
     */
    String askCalculationPointProgress(String calculationPointID);

    Map<String, String> askCalculationPointsProgress();

    /**
     * Ask global progress about currents project calculation.
     * @return global progress.
     */
    String getState();

    /**
     * On Funz asks dispatcher to send back locally in archive directory calculation point result content.
     * @param calculationPointId Calculation points's ID of which we want results.
     * @return pv2: return file of content if it was successfully sent back, null otherwise
     */
    File getCalculationPointContent(String calculationPointId);

    Map<String, Object> getCalculationPointProperties(String calculationPointID);

    Object getCalculationPointProperty(String calculationPointID, String propertyName);

    Map<String, String> getCalculationPointData(String calculationPointID);

    String getCalculationPointData(String calculationPointID, String dataName);

    String[] getCalculationPointsNames();

    void setArchiveDirectory(File directory);

    File getArchiveDirectory();
}
