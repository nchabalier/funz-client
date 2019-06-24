package org.funz.doe;

import java.util.*;
import org.funz.doeplugin.Design;
import org.funz.doeplugin.DesignHelper;
import org.funz.doeplugin.DesignSession;
import org.funz.doeplugin.DesignedExperiment;
import org.funz.doeplugin.Designer;
import org.funz.doeplugin.Experiment;
import org.funz.parameter.Parameter;
import org.funz.util.ASCII;
import static org.funz.util.Data.asString;

/**
 *
 * @author Y.Richet
 *
 */
public class TESTIterative extends Design {

    int shotdone = 0;

    public TESTIterative(Designer d, DesignSession ds) {
        super(d, ds);
    }

    @Override
    public Status getInitialDesign(List<Experiment> ret) {
        int batchSize = (int) Double.parseDouble(getOption("N"));

        for (int batch = 0; batch < batchSize; batch++) {
            DesignedExperiment e = new DesignedExperiment(_parameters.length, prj);
            int i = 0;
            for (Parameter p : _parameters) {
                double min = p.getLowerBound();
                double max = p.getUpperBound();

                assert min < max;

                e.setValueExpression(i, p.getName() + "=" + (min + Math.random() * (max - min)));
                i++;
            }
            ret.add(e);
        }
        shotdone++;
        return new Status(Decision.READY_FOR_NEXT_ITERATION);
    }

    @Override
    public Status getNextDesign(List<Experiment> exps, List<Experiment> ret) {
        double[][] out = DesignHelper.getOutputParams(exps, _f);
        for (double[] ds : out) {
            if (ds == null || ds.length == 0 || Double.isNaN(ds[0])) {
                System.err.println("Bad output!");
                throw new IllegalArgumentException("Bad output in \n" + ASCII.cat(",", out));
            } else {
                System.err.println("Good output:\n" + ASCII.cat(",", ds));
            }
        }

        if (shotdone > (int) Double.parseDouble(getOption("repeat"))) {
            return new Status(Decision.DESIGN_OVER);
        }

        try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
            ex.printStackTrace(System.err);
        }
        int batchSize = (int) Double.parseDouble(getOption("N"));

        for (int batch = 0; batch < batchSize; batch++) {
            DesignedExperiment e = new DesignedExperiment(_parameters.length, prj);
            int i = 0;
            for (Parameter p : _parameters) {
                double min = p.getLowerBound();
                double max = p.getUpperBound();

                assert min < max;

                e.setValueExpression(i, p.getName() + "=" + (min + Math.random() * (max - min)));
                i++;
            }
            ret.add(e);
        }
        DesignedExperiment already = new DesignedExperiment(_parameters.length, prj);//to test for alreadyDone in PanelCalculation
        int i = 0;
        for (Parameter p : _parameters) {
            already.setValueExpression(i, exps.get(0).getValueExpression(i));
            i++;
        }
        ret.add(already);

        shotdone++;
        /*try {
        Thread.sleep(3000);
        } catch (InterruptedException ex) {
        ex.printStackTrace(System.err);
        }*/

 /*if (Math.random() > 0.5) {
        throw new IllegalArgumentException("Random error !!");
        }*/
        return new Status(Decision.READY_FOR_NEXT_ITERATION);
    }

    @Override
    public String displayResults(List<Experiment> finishedExperiments/*, File _repository*/) {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            ex.printStackTrace(System.err);
        }
        return "<HTML>" + DesignHelper.HTMLTable(finishedExperiments, _f) + "</HTML>"
                + "<Plot1D name='" + _f.toNiceSymbolicString() + "'>" + asString(DesignHelper.getOutputArray(finishedExperiments, _f)) + "</Plot1D>";

    }

    @Override
    public String displayResultsTmp(List<Experiment> experiments/*, File _repository*/) {
        /*if (Math.random() > 0.3) {
        throw new IllegalArgumentException("Random error !!");
        }*/
        try {
            Thread.sleep(10000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        return analyseOnlyFinishedExperiments(experiments/*, _repository*/);
    }   
    
    @Override
    public void saveNotebook() {
        // do nothing
    }
}
