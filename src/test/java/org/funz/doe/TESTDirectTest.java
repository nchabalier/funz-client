package org.funz.doe;

import java.util.HashMap;
import java.util.LinkedList;
import org.funz.parameter.OutputFunctionExpression.Numeric;
import org.funz.parameter.Parameter;
import org.funz.parameter.Variable;

/**
 *
 * @author Y.Richet
 *
 */
public class TESTDirectTest extends TestUtils {

    public void initIO() {
        LinkedList<Variable> variables = new LinkedList<Variable>();
        variables.add(new Variable(prj, "x1"));
        variables.add(new Variable(prj, "x2"));

        LinkedList<Parameter> params = new LinkedList<Parameter>();
        for (Variable variable : variables) {
            variable.setType(Variable.TYPE_CONTINUOUS);
            variable.setLowerBound(0);
            variable.setUpperBound(1);
            prj.getVariables().add(variable);
            params.add(variable);
        }

        nparam = params.size();

        designer = new TESTDirectDesigner();
        designer.setProject(prj);
        designer.setParameters(params);
        designer.setOutputFunction(new Numeric(OUTPUT_NAME));

        //prj.setDesigner(designer);
    }

    public Object f(double[] x) {
        return Math.random();
    }

    public static void main(String[] args) {
        HashMap<String, String> options = new HashMap<String, String>();
        options.put("N", "3");
        new TESTDirectTest(options);
    }

    public TESTDirectTest(HashMap<String, String> o) {
        super(o);
    }
}
