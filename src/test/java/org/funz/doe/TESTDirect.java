package org.funz.doe;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.*;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
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
public class TESTDirect extends Design {

    public TESTDirect(Designer d, DesignSession ds) {
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

                assert min < max : "Bad model: min=" + min + " max=" + max;

                e.setValueExpression(i, p.getName() + "=" + (min + Math.random() * (max - min)));
                i++;
            }
            ret.add(e);
            if (batch == batchSize - 1) { // to add same point and check everything is ok.
                ret.add(e);
            }
        }
        return new Status(Decision.DESIGN_OVER);
    }

    @Override
    public Status getNextDesign(List<Experiment> exps, List<Experiment> ret) {
        return noNextDesign(exps, exps);
    }

    @Override
    public String displayResults(List<Experiment> finishedExperiments/*, File _repository*/) {
        String out = "<HTML name='points'>" + DesignHelper.HTMLTable(finishedExperiments, _f) + /*DesignHelper.buildPNGPlot(new File(_repository, "plot.png"), new Plot2DPanel(), 400, 400) +*/ "</HTML>";
        if (_f instanceof OutputFunctionExpression.Numeric) {
            out += "<Plot1D name='Z'>" + new OutputFunctionExpression.NumericArray().toNiceNumericString(DesignHelper.getOutputArray(finishedExperiments, _f)) + "</Plot1D>"
                    + "<Plot1D name='Z1,Z2'>Z1=" + new OutputFunctionExpression.NumericArray().toNiceNumericString(DesignHelper.getOutputArray(finishedExperiments, _f))
                    + "\nZ2=" + new OutputFunctionExpression.NumericArray().toNiceNumericString(DesignHelper.getOutputArray(finishedExperiments, _f)) + "</Plot1D>";
        }
        return out;
    }

    @Override
    public JComponent renderDesign(List<Experiment> experiments/*, File _repository*/) {
        return new JButton(new AbstractAction("" + (experiments.size() + displayResults(experiments))) {

            public void actionPerformed(ActionEvent e) {
                System.err.println("renderDesign.actionPerformed");
            }
        });
    }

    @Override
    public JComponent renderDesignTmp(List<Experiment> experiments/*, File _repository*/) {
        return new JButton(new AbstractAction("" + (experiments.size() + displayResults(experiments))) {

            public void actionPerformed(ActionEvent e) {
                System.err.println("renderDesignTmp.actionPerformed");
            }
        });
    }

    @Override
    public void saveNotebook() {
        // do nothing
    }
}
