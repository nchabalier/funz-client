package org.funz;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.funz.log.Log;
import org.funz.log.LogCollector.SeverityLevel;
import org.funz.parameter.Case;

public class FunzResultDriver implements ProjectController.ResultDriver {

    /**
     * Retourne le resultat de calcul pour le cas caseidx dans le projet prj.
     *
     * @param prj projet
     * @param caseIdx le num?ro du cas ? traiter
     * @param isHot dit ci on est en cours de calcul ou pas (post-traitement)
     */
    public Map<String, Object> readCaseOutputs(Project prj, int caseIdx, boolean isHot) throws Exception {
        //System.err.println("readCaseOutputs "+prj+" "+caseIdx+" "+isHot);
        if (prj.getCases() == null || prj.getCases().size() < caseIdx) {
            prj.loadCases();
        }

        if (prj.getCases() == null || prj.getCases().size() < caseIdx) {
            throw new Exception("Project have no cases.");
        }

        if (prj.getCases().size() < caseIdx) {
            throw new Exception("Project have not case id=" + caseIdx);
        }

        Case c = prj.getCases().get(caseIdx);
        if (c == null) {
            throw new Exception("Case with id=" + caseIdx + " not found in project.");
        }
        if (!isHot) {
            try {
                Map<String, Object> ov = prj.getCases().get(caseIdx).getOutputValues();
                //System.err.println("  case "+prj.getCases().get(caseIdx).getName()+" : "+prj.getCases().get(caseIdx).hashCode());
                //System.err.println("   "+prj.getCases().get(caseIdx));
                if (ov != null) {
                    c.setInformation("Output loaded.");
                    Log.logMessage(this, SeverityLevel.INFO, false, "Case " + prj.getCases().get(caseIdx).getName() + " output loaded.");
                    return ov;
                } else {
                    c.setInformation("Output failed to load.");
                    Log.logMessage(this, SeverityLevel.WARNING, false, "Case " + prj.getCases().get(caseIdx).getName() + " output failed to load: null");
                }
            } catch (Exception e) {
                c.setInformation("Output failed to load because exception: " + e.getLocalizedMessage());
                Log.logMessage(this, SeverityLevel.WARNING, false, "Case " + prj.getCases().get(caseIdx).getName() + " output failed to load: " + e.getMessage());
            }
        }

        File casedir = isHot ? prj.getCaseTmpDir(caseIdx) : prj.getCaseResultDir(caseIdx);
        if (!casedir.exists()) {
            throw new Exception("Directory of case " + c + ": "+casedir.getAbsolutePath()+" does not exists.");
        }

        Map<String, Object> ret;
        try {
            ret = prj.getPlugin().readOutput(new File(casedir, Constants.OUTPUT_DIR));
            Log.logMessage(this, SeverityLevel.INFO, false, "Case " + prj.getCases().get(caseIdx).getName() + " has output " + ret);
            c.setInformation("Case has output");
            for (String o : prj.getPlugin().getOutputNames()) { // should not be needed if plugin well fills all output, including null values for results not found
                if (!ret.containsKey(o)) {
                    Log.logMessage(this, SeverityLevel.WARNING, false, "Case " + prj.getCases().get(caseIdx).getName() + " output do not contains " + o);
                    c.setInformation("Output do not contains " + o);
                    ret.put(o, null);
                }
            }
            Log.logMessage(this, SeverityLevel.INFO, false, "Case " + prj.getCases().get(caseIdx).getName() + " output parsed from " + casedir.getPath());
            c.setInformation("Output parsed from " + casedir.getPath());
            return ret;
        } catch (Exception ex) {
            c.setInformation("Output failed to read because exception:" + ex.getLocalizedMessage());
            Log.logException(false, ex);
            ret = new HashMap<String, Object>();
            for (String o : prj.getPlugin().getOutputNames()) { // should not be needed if plugin well fills all output, including null values for results not found
                ret.put(o, null);
            }
            return ret;
        }
    }
}
