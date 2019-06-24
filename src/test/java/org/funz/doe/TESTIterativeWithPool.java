package org.funz.doe;

import java.util.*;
import org.funz.doeplugin.Design;
import org.funz.doeplugin.DesignHelper;
import org.funz.doeplugin.DesignSession;
import org.funz.doeplugin.DesignedExperiment;
import org.funz.doeplugin.Designer;
import org.funz.doeplugin.Experiment;
import org.funz.parameter.Parameter;

/** 
 *
 * @author Y.Richet
 *
 */
public class TESTIterativeWithPool extends Design {

    int shotdone = 0;

    public TESTIterativeWithPool(Designer d, DesignSession ds) {
        super(d, ds);
    }

    @Override
    public Status getInitialDesign(List<Experiment> ret) {
        int batchSize = (int) Double.parseDouble(getOption("N"));

        for (int batch = 0; batch < batchSize; batch++) {
            DesignedExperiment e = new DesignedExperiment(_parameters.length,prj);
            int i = 0;
            for (Parameter p : _parameters) {
                double min = p.getLowerBound();
                double max = p.getUpperBound();

                assert min < max;

                e.setValueExpression(i, p.getName() + "=" + (min + ((batch + 1) * 1.0) / (batchSize - 1.0) * (max - min)));
                i++;
            }
            ret.add(e);
        }
        shotdone++;
        return new Status(Decision.READY_FOR_NEXT_ITERATION);
    }

    @Override
    public synchronized Status getNextDesign(List<Experiment> exps, List<Experiment> ret) {
        if (shotdone > (int) Double.parseDouble(getOption("repeat"))) {
            return new Status(Decision.DESIGN_OVER);
        }
        try {
            Thread.sleep(Math.round(Math.random() * 10000.0));
        } catch (InterruptedException ex) {
            ex.printStackTrace(System.err);
        }

        int batchSize = (int) Double.parseDouble(getOption("N"));

        for (int batch = 0; batch < batchSize; batch++) {
            DesignedExperiment e = new DesignedExperiment(_parameters.length,prj);
            int i = 0;
            for (Parameter p : _parameters) {
                double min = p.getLowerBound();
                double max = p.getUpperBound();

                assert min < max;

                e.setValueExpression(i, p.getName() + "=" + (min + ((batch + 1) * 1.0) / (batchSize - 1.0 / (shotdone + 1.0)) * (max - min)));
                i++;
            }
            ret.add(e);
        }
        shotdone++;
        return new Status(Decision.READY_FOR_NEXT_ITERATION);
    }

    @Override
    public synchronized String displayResults(List<Experiment> finishedExperiments/*, File _repository*/) {
        return DesignHelper.HTMLTable(finishedExperiments, _f);
    }

    @Override
    public synchronized String displayResultsTmp(List<Experiment> experiments/*, File _repository*/) {
        return analyseOnlyFinishedExperiments(experiments/*, _repository*/);
    }  
    
    @Override
    public void saveNotebook() {
        // do nothing
    }
}
