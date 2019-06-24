package org.funz.doeplugin;

import java.util.List;
import java.util.Map;
import org.funz.parameter.OutputFunctionExpression;
import org.funz.parameter.Parameter;

/**
 *
 * @author richet
 */
public interface DesignerInterface extends DesignConstants {

    Design createDesign(DesignSession sessionn) throws Exception;

    String getDesignOutputTitle();

    String getName();

    Map<String, String> getOptions();

    public boolean viewManagedParams();

    OutputFunctionExpression getOutputFunctionExpression();

    String getQuickHelp();

    String getType();

    String isValid(List<Parameter> params, OutputFunctionExpression f) throws Exception;

    void setOptions(Map<String, String> dOEOptions);

    /**
     * Set output fnction used for analysis and design of experiments.
     *
     * @param f parameter array
     */
    void setOutputFunction(OutputFunctionExpression f);

    /**
     * Set parameters used for design of experiments.
     *
     * @param params parameter array
     */
    void setParameters(Parameter[] params);

    /**
     * wrapper
     */
    void setParameters(List<Parameter> params);
}
