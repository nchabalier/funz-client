package org.funz.doeplugin;

import java.util.HashMap;
import java.util.Map;
import org.funz.Project;

/** Default implementation of Experience.
 * It stores the values as array of strings.
 *
 */
public class DesignedExperiment extends Experiment {

    public DesignSession getDesignSession() {
        return null;
    }

    /** Constucts a DesignedExperiment to store <b>nbOfValues</b> values.
     * @apram nbOfValues number of valaues corresponding to the number of parameters.
     */
    public DesignedExperiment(int nbOfValues, Project prj) {
        super(prj);
        _value = new String[nbOfValues];
    }

    /** No result for default experiences. */
    public Map<String, Object> getOutputValues() {
        return null;
    }

    public Map<String, Object> getInputValues() {
        HashMap<String, Object> in = new HashMap<String, Object>();
        for (int i = 0; i < _value.length; i++) {
            String ve = getValueExpression(i);
            in.put(ve.substring(0, ve.indexOf("=")), getDoubleValue(i));
        }
        return in;
    }

    /** Stores value at position paramIdx.
     * @param paramIdx parameter index, must be < getNmOfParameters()
     * @param value value to store
     */
    public void setValueExpression(int paramIdx, String value) {
        assert value.contains("=") : value;
        _value[paramIdx] = value;
    }

    @Override
    public String getValueExpression(int paramIdx) {
        //System.out.println("DesignedExperiment.getValueExpression("+paramIdx+") = "+_value[paramIdx]);
        return _value[paramIdx];
    }

    @Override
    public int getNmOfParameters() {
        return _value.length;
    }
    private String[] _value;

    @Override
    public Map<String, Object> getIntermediateValues() {
        return new HashMap<String, Object>();
    }
}
