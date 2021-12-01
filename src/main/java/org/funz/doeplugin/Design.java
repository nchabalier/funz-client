package org.funz.doeplugin;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JLabel;
import org.funz.Project;
import org.funz.log.Log;
import org.funz.parameter.OutputFunctionExpression;
import org.funz.parameter.Parameter;
import org.funz.script.MathExpression;
import static org.funz.util.Data.*;
import org.math.array.DoubleArray;

/**
 * Interface for objects responsible for the design of experiments. Design
 * implementations will implement or delegate the algorithm of their modeling
 * strategy.
 *
 */
public abstract class Design implements DesignConstants/*, ResultPainter*/ {

    Observer _observer;

    public String getStatus() {
        return "";
    };

    public abstract void saveNotebook();

    /**
     * Design observer interface.
     */
    public static interface Observer {

        public void designUpdated(int numCases);
    }

    public void setObserver(Observer o) {
        _observer = o;
    }

    public Observer getObserver() {
        return _observer;
    }

    public File _repository;
    public Project prj;

    public Design(Designer designer, DesignSession session) {
        this.prj = designer.getProject();
        _designer = designer;
        _session = session;
        _session.setDesign(this);
        _f = _designer.getOutputFunctionExpression();
        _parameters = _designer.getParameters();
    }

    public String getOption(String o) {
        try {
            return asString(prj.getPlugin().getFormulaInterpreter().eval(_designer.getOptions().get(o), _session.getFixedParameters()));
        } catch (MathExpression.MathException ex) {
            Log.err(ex, 1);
            Log.err("Ignore evaluation of option " + o + " as " + _designer.getOptions().get(o) + " with "+_session.getFixedParameters(), 1);
            return _designer.getOptions().get(o);
        } catch (Exception e) {
            Log.err(e, 0);
            Log.err("Could not interpret option " + o + " in options list " + _designer.getOptions() + " with "+_session.getFixedParameters(), 0);
            return null;
        }
    }

    @Override
    public void finalize() throws Throwable {
        endDesign();
        super.finalize();
    }

    public void endDesign() {
        // to be overridden for proper gc, if needed
    }

    /**
     * Called when the designer is created.
     */
    public void init(File repository) throws Exception {
        _repository = repository;
        if (!_repository.isDirectory()) {
            if (!_repository.mkdirs()) {
                Log.err("Cannot create design repository " + _repository.getAbsolutePath(), 0);
                throw new IOException("Cannot create design repository " + _repository.getAbsolutePath());
            }
        }
    }

    /**
     * Creates the first batch of designed experiments
     *
     * @param ret experience list to perform as first design.
     * @return status of the design session
     */
    public Status buildInitialDesign(List<Experiment> returnedExperiments) {
        Status s = getInitialDesign(returnedExperiments);
        if (_observer != null) {
            _observer.designUpdated(returnedExperiments.size());
        }
        return s;
    }

    public abstract Status getInitialDesign(List<Experiment> returnedExperiments);

    /**
     * Creates next experience list for a project with already existing
     * experiences.
     * @param alreadyInQueueExperiments existing experience list
     * @param experimentsToAppendInQueue return list to be modified
     * @return status of the design session
     */
    public Status buildNextDesign(List<Experiment> alreadyInQueueExperiments, List<Experiment> experimentsToAppendInQueue) {
        Status s = getNextDesign(alreadyInQueueExperiments, experimentsToAppendInQueue);
        if (_observer != null) {
            _observer.designUpdated(experimentsToAppendInQueue.size());
        }
        return s;
    }

    public abstract Status getNextDesign(List<Experiment> alreadyInQueueExperiments, List<Experiment> experimentsToAppendInQueue);

    //TODO move to DesignHelper class for these kind of convenience methods
    /**
     * convenience method to call in getNextDesign impl
     */
    public Status noNextDesign(List<Experiment> alreadyInQueueExperiments, List<Experiment> experimentsToAppendInQueue) {
        for (Experiment experiment : alreadyInQueueExperiments) {
            if (experiment.getOutputValues() == null || experiment.getOutputValues().isEmpty()) {
                return new Status(Decision.READY_FOR_NEXT_ITERATION);
            }
        }
        return new Status(Decision.DESIGN_OVER);
    }

    /**
     * Provide conclusion of this DOE.
     *
     * @param experiments return list to be modified
     * @return double, double[], or HTML text containing conclusion provided in
     * a JTable (one line per discrete case).
     */
    public String displayResults(List<Experiment> experiments) {
        return null;
    }

    /**
     * Provide conclusion of this DOE.
     *
     * @param experiments return list to be modified
     * @return Image or any graphical component to render the results of this
     * design.
     */
    public JComponent renderDesign(List<Experiment> experiments) {
        return null;
    }

    /**
     * convenience method to call in displayResults impl
     */
    public double[] getOutputArray(List<Experiment> finishedExperiments) {
        return DesignHelper.getOutputArray(finishedExperiments, _f);
    }

    /**
     * convenience method to call in displayResults impl
     */
    public double[][] getOutputParams(List<Experiment> finishedExperiments) {
        return DesignHelper.getOutputParams(finishedExperiments, _f);
    }

    public double[] getOutputParam(List<Experiment> finishedExperiments, int numparam) {
        double[][] params = DesignHelper.getOutputParams(finishedExperiments, _f);
        //System.out.println(ASCII.cat(",", params));
        if (params == null || params.length < numparam) {
            return null;
        } else {
            return DoubleArray.getColumnCopy(params, numparam);
        }
    }

    /**
     * convenience method to call in displayResults impl
     */
    public double[][] getInputArray(List<Experiment> finishedExperiments) {
        return DesignHelper.getInputArray(finishedExperiments);
    }

    /**
     * wrapper
     */
    /*public Object displayResults(LinkedList<Case> experiments) {
     ArrayList<Experiment> exps = new ArrayList<Experiment>();
     exps.addAll(experiments);
     return displayResults(exps);
     }*/
    /**
     * Provide temporary conclusion of this DOE.
     *
     * @param experiments return list to be modified
     * @return temporary conclusion or progress
     */
    //public abstract Object displayResultsTmp(  ArrayList<Experiment> experiments);
    public String displayResultsTmp(List<Experiment> experiments) {
        return analyseWhenAllFinished(experiments);
    }

    public JComponent renderDesignTmp(List<Experiment> experiments) {
        return null;//renderWhenAllFinished(experiments);
    }

    /**
     * convenience method to call in displayResultsTmp impl
     */
    public String analyseOnlyFinishedExperiments(List<Experiment> experiments/*, File repo*/) {
        List<Experiment> finished = DesignHelper.getFinishedExperiments(experiments);
        if (finished != null && !finished.isEmpty()) {
            return displayResults(finished/*, repo*/);
        } else {
            return "Empty";
        }
    }

    /**
     * convenience method to call in displayResultsTmp impl
     */
    public String analyseWhenAllFinished(List<Experiment> experiments/*, File repo*/) {
        //System.out.println(Experiment.toString_ExperimentArray("Design.analyseWhenAllFinished", experiments));
        List<Experiment> finishedExperiments = DesignHelper.getFinishedExperiments(experiments);
        if (finishedExperiments.size() == experiments.size()) {
            return displayResults(experiments/*, repo*/);
        } else {
            return finishedExperiments.size() + "/" + experiments.size();
        }
    }

    public Component renderWhenAllFinished(List<Experiment> experiments) {
        //System.out.println(Experiment.toString_ExperimentArray("Design.analyseWhenAllFinished", experiments));
        List<Experiment> finishedExperiments = DesignHelper.getFinishedExperiments(experiments);
        if (finishedExperiments.size() == experiments.size()) {
            return renderDesign(experiments);
        } else {
            return new JLabel(finishedExperiments.size() + "/" + experiments.size());
        }
    }

    /**
     * convenience method to call in displayResultsTmp impl
     */
    public String noAnalyse(List<Experiment> experiments) {
        return "No result yet...";
    }

    /**
     * wrapper
     */
    /*public Object displayResultsTmp(LinkedList<Case> experiments) {
     ArrayList<Experiment> exps = new ArrayList<Experiment>();
     exps.addAll(experiments);
     return displayResultsTmp(exps);
     }*/

    /*public boolean handlesResultPainting()
     {
     return false;
     }
    
     public void paintResult( Graphics2D g, Rectangle rect )
     {
     }*/
    protected Designer _designer;
    protected DesignSession _session;
    protected Parameter[] _parameters;
    protected OutputFunctionExpression _f;

    public double[][] scaleParametersWithBounds(double[]... normdoe) {
        double[][] doe = new double[normdoe.length][_parameters.length];
        for (int i = 0; i < doe.length; i++) {
            for (int j = 0; j < doe[i].length; j++) {
                if (_parameters[j].getLowerBound() != Double.NEGATIVE_INFINITY && _parameters[j].getUpperBound() != Double.POSITIVE_INFINITY) {
                    doe[i][j] = _parameters[j].getLowerBound() + normdoe[i][j] * (_parameters[j].getUpperBound() - _parameters[j].getLowerBound());
                } else {
                    doe[i][j] = normdoe[i][j];
                }
            }
        }
        return doe;
    }

    public double[][] normalizeParametersWithBounds(double[]... doe) {
        double[][] normdoe = new double[doe.length][_parameters.length];
        for (int i = 0; i < doe.length; i++) {
            for (int j = 0; j < doe[i].length; j++) {
                normdoe[i][j] = (doe[i][j] - _parameters[j].getLowerBound()) / (_parameters[j].getUpperBound() - _parameters[j].getLowerBound());
            }
        }
        return normdoe;
    }
}
