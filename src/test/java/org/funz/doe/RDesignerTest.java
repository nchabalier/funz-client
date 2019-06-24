package org.funz.doe;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Properties;
import org.funz.doeplugin.RDesigner_V1;
import org.funz.log.Log;
import org.funz.parameter.OutputFunctionExpression.Numeric;
import org.funz.parameter.Variable;
import org.funz.util.ASCII;
import org.funz.util.URLMethods;
import org.math.R.Rsession;

/**
 *
 * @author richet
 */
public class RDesignerTest extends TestUtils {
    
    String doeurl;
    String[] src;
    
    public RDesignerTest(HashMap<String, String> o, String... src) {
        super((Properties) null);
        options = o;
        this.doeurl = src[0];
        this.src = src;
        try {
            init();
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
    }
    
    public void initIO() {
        designer = new RDesigner_V1(doeurl);
        
        for (String tosrc : src) {
            try {
                System.err.println("Sourcing " + tosrc);
                String content = URLMethods.readURL(tosrc);
                String name = tosrc.substring(tosrc.lastIndexOf(":") + 1);
                name = name.substring(name.lastIndexOf("/") + 1);
                File Rsrc = File.createTempFile("RDesignerTest_" + name, "_" + hashCode());
                ASCII.saveFile(Rsrc, content);
                R.source(Rsrc);
            } catch (IOException ex) {
                ex.printStackTrace(System.err);
            }
        }
        
        R.note_header("Test: " + doeurl, "RDesignerTest");
        
        R.note_text("Init objective function f");
        R.note_code("print(f)");
        try {
            R.voidEval("ins<-formals(f)");
        } catch (Rsession.RException ex) {
            Log.err(ex, 1);
        }
        String[] vars = null;
        try {
            vars = R.asStrings(R.eval("names(ins)"));
        } catch (Exception ex) {
            Log.err(ex, 1);
        }
        nparam = vars.length;
        
        designer.setProject(prj);
        prj.setDesigner(designer);
        
        LinkedList<Variable> variables = prj.getVariables();
        for (int i = 0; i < nparam; i++) {
            Variable v = new Variable(prj, vars[i]);
            v.setType(Variable.TYPE_CONTINUOUS);
            v.setLowerBound(0);
            v.setUpperBound(1);
            variables.add(v);
        }
        
        prj.buildParameterList();
        
        designer.setParameters(prj.getParameters());
        designer.setOutputFunction(new Numeric(OUTPUT_NAME));
    }
    
    public Object f(double[] x) {
        try {
            return R.asDouble(R.eval("f(" + ASCII.cat(",", x) + ")"));
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
            return null;
        }
    }
    
    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            args = new String[]{"file:src/main/resources/plugins/doe/GradientDescent.R"};
        }
        
        HashMap<String, String> options = new HashMap<String, String>();
        System.out.println("Testing " + args[0]);
        RDesignerTest t = new RDesignerTest(options, args);
        
        System.out.println(((RDesigner_V1.RDesign)(t.design)).R.notebook());
    }
}
