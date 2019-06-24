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
 * this test is designed to cause a bug when a newer experiment is the same that a previous one. 
 * Then, the cases are cloned in the Project, not terminated, and then no notification of the end of the case is sent. 
 * Design is then sleeping forever...
 */
public class TESTIterativeLoop extends Design {

    int shotdone = 0;

    public TESTIterativeLoop(Designer d, DesignSession ds) {
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
                
                e.setValueExpression(i, p.getName() + "=" +(min+(max-min)*batch*1.0/batchSize));
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

                e.setValueExpression(i, p.getName() + "=" +(min+(max-min)*(shotdone*1.0/4)*batch*1.0/batchSize));
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
