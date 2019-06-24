package org.funz.doe;

import edu.cornell.lassp.houle.RngPack.RanMT;
import edu.cornell.lassp.houle.RngPack.RandomSeedable;
import java.util.*;
import org.funz.doeplugin.Design;
import org.funz.doeplugin.DesignHelper;
import org.funz.doeplugin.DesignSession;
import org.funz.doeplugin.DesignedExperiment;
import org.funz.doeplugin.Designer;
import org.funz.doeplugin.Experiment;
import org.funz.parameter.OutputFunctionExpression;
import org.funz.parameter.Parameter;

/** 
 *
 * @author Y.Richet
 *
 */
public class TESTDirectWithPool extends Design {

    public TESTDirectWithPool(Designer d, DesignSession ds) {
        super(d, ds);
    }

    @Override
    public Status getInitialDesign(List<Experiment> ret) {
        int batchSize = (int) Double.parseDouble(getOption("N"));
        
        RandomSeedable r = new RanMT(1);
        System.out.println("batchSize = " + batchSize);//TODO bug: pourquoi pas actualisé ?
        for (int batch = 0; batch < batchSize; batch++) {
            DesignedExperiment e = new DesignedExperiment(_parameters.length, prj);
            int i = 0;
            for (Parameter p : _parameters) {
                double min = p.getLowerBound();
                double max = p.getUpperBound();

                assert min < max;

                e.setValueExpression(i, p.getName() + "=" + (min + (batch * r.raw()) / (batchSize - 1.0) * (max - min)));
                i++;
            }
            ret.add(e);
        }
        return new Status(Decision.DESIGN_OVER);
    }

    @Override
    public Status getNextDesign(List<Experiment> exps, List<Experiment> ret) {
        return noNextDesign(exps, exps);
    }

    @Override
    public String displayResults(List<Experiment> finishedExperiments/*, File _repository*/) {
        String out = "<HTML name='points'>" + DesignHelper.HTMLTable(finishedExperiments, _f) + "</HTML>";
        if (_f instanceof OutputFunctionExpression.Numeric) {
            out += "<Plot1D name='Z'>" + new OutputFunctionExpression.NumericArray().toNiceNumericString(DesignHelper.getOutputArray(finishedExperiments, _f)) + "</Plot1D>"
                    + "<Plot1D name='Z1,Z2'>Z1=" + new OutputFunctionExpression.NumericArray().toNiceNumericString(DesignHelper.getOutputArray(finishedExperiments, _f))
                    + "\nZ2=" + new OutputFunctionExpression.NumericArray().toNiceNumericString(DesignHelper.getOutputArray(finishedExperiments, _f)) + "</Plot1D>";
        }

        return out;

    }

    @Override
    public String displayResultsTmp(List<Experiment> experiments/*, File _repository*/) {
        return analyseOnlyFinishedExperiments(experiments/*, _repository*/);
    }    
    
    @Override
    public void saveNotebook() {
        // do nothing
    }
}
