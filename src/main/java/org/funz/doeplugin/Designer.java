package org.funz.doeplugin;

import org.funz.Project;
import org.funz.conf.Configuration;
import org.funz.parameter.OutputFunctionExpression;
import org.funz.parameter.Parameter;

import java.util.*;

public abstract class Designer implements DesignerInterface {

    public static String VALID = "OK";
    private Project prj;
    protected Map<String, String> _opts = new HashMap<>();
    protected Map<String, String> _optsModel = new HashMap<>();
    private boolean isCompleteURI = false; // True to replace DesignHelper.BASE in html by the complete URI of file.

    public Designer(/*Project prj*/) {//impossible because URLMethods dynamic loader does not accept args for contructor (use of newInstance() )
        //this.prj = prj;
    }

    public static int getMaxCalculationsAtSameTime() {
        return Configuration.defaultCalcs();
    }

    /**
     * Sets options to edit.
     *
     * @param opts options stored as key/value paires
     */
    public void setOptions(Map<String, String> opts) {
        _opts.putAll(opts);
        prj.setDesignOptions(getName(), _opts);
    }

    public void setOptions(String[] names, String[] values) {
        for (int i = 0; i < names.length; i++) {
            _opts.put(names[i], (values.length > i ? values[i] : null));
        }
        prj.setDesignOptions(getName(), _opts);
    }

    public void setOption(String optionKey, String get) {
        _opts.put(optionKey, get);
        prj.setDesignOptions(getName(), _opts);
    }

    /**
     * Returns the options after editing.
     */
    public Map<String, String> getOptions() {
        return _opts;
    }

    public String getOptionModel(String o) {
        return _optsModel.get(o);
    }

    /**
     * Set parameters used for design of experiments.
     *
     * @param params parameter array
     */
    @Override
    public void setParameters(Parameter[] params) {
        for (Parameter parameter : params) {
            assert parameter.isContinuous() : "Parameter " + parameter.getName() + " (among " + Arrays.deepToString(params) + ") is not continuous: " + Parameter.PARAM_TYPE_NAMES[parameter.getParameterType()];
        }

        _parameters = params;
    }

    /**
     * wrapper
     */
    @Override
    public void setParameters(List<Parameter> params) {
        setParameters(params.toArray(new Parameter[params.size()]));
    }

    public Parameter[] getParameters() {
        return _parameters;
    }

    /**
     * Set output fnction used for analysis and design of experiments.
     *
     * @param f parameter array
     */
    @Override
    public void setOutputFunction(OutputFunctionExpression f) {
        _f = f;
    }

    @Override
    public OutputFunctionExpression getOutputFunctionExpression() {
        return _f;
    }

    protected Parameter[] _parameters;
    protected OutputFunctionExpression _f;

    @Override
    public boolean viewManagedParams() {
        return false;
    }

    /**
     * @return the prj
     */
    public Project getProject() {
        return prj;
    }

    /**
     * @param prj the prj to set
     */
    public void setProject(Project prj) {
        this.prj = prj;
    }

    /**
     * This method may return false if we have an asynchronized algorithm and
     * all experimetnts are not yet erminiated. asynchronized algorithm =
     * supports not-ended experiments as a valuable information. See async EGO
     * for instance But usually (for synchronized algorithm) returns true unless
     * all experiments are completed.
     */
    public boolean skipDesign(ArrayList<Experiment> previousExperiments) {
        return !DesignHelper.IsOutputFull(previousExperiments);
    }

    public void setUseCompleteURI(boolean useCompleteURI) {
        this.isCompleteURI = useCompleteURI;
    }

    public boolean isCompleteURI() {
        return isCompleteURI;
    }
}
