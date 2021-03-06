package org.funz.doe;


import java.util.*;
import org.funz.doeplugin.Design;
import org.funz.doeplugin.DesignSession;
import org.funz.doeplugin.Designer;
import org.funz.parameter.OutputFunctionExpression;
import org.funz.parameter.Parameter;

/** 
 * @author Y.Richet
 *
 */
public class TESTDirectWithPoolDesigner extends Designer {

    final static String QUICKHELP = "Basic direct designer with pool";

    public TESTDirectWithPoolDesigner() {
    _opts.put("N", "5");
    }

    @Override
    public String getName() {
        return "Test direct with pool";
    }

    @Override
    public String getType() {
        return "TEST";
    }

    @Override
    public String getDesignOutputTitle() {
        return "Output values";
    }

    @Override
    public String getQuickHelp() {
        return QUICKHELP;
    }

    @Override
    public String isValid(List<Parameter> params, OutputFunctionExpression f) {
        StringBuffer answer = new StringBuffer();

        if (f == null) {
            answer.append("Output function must be defined.\n");
        }

        if (params.size() <= 0) {
            answer.append("Needs at least 1 parameter.\n");
        }

        return answer.length() == 0 ? VALID : answer.toString();
    }

    @Override
    public Design createDesign(DesignSession ds) {
        return new TESTDirectWithPool(this, ds);
    }

    @Override
    public boolean viewManagedParams() {
        return true;
    }
}
