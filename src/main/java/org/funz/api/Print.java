package org.funz.api;

import java.util.LinkedList;
import java.util.List;
import org.funz.parameter.Case;
import org.funz.parameter.Case.Node;
import org.funz.parameter.Variable;
import org.funz.run.Computer;
import org.funz.util.ASCII;
import static org.funz.util.Format.MapToMDString;

/**
 *
 * @author richet
 */
public class Print {

    /**
     * Provide information about available input.
     *
     * @return a String of variables available (and related info) in the input
     * files with given code/model.
     */
    public static String variablesInformation(AbstractShell shell) {
        StringBuilder stringBuilder = new StringBuilder();
        List<Variable> vars = shell.prj.getVariables();
        for (Variable v : vars) {
            stringBuilder.append(v.toInfoString()).append("\n");
        }
        return stringBuilder.toString();
    }

    public static String csvState(AbstractShell shell) {
        List<String> columns = new LinkedList<String>();
        for (Node n : shell.prj.getCases().get(0).getNodes()) {
            columns.add(Case.PROP_VAR + "." + n.getParamName());
        }
        for (String iv : shell.prj.getVoidIntermediate().keySet()) {
            columns.add(Case.PROP_INTER + "." + iv);
        }
        for (String o : shell.prj.getOutputNames()) {
            columns.add(Case.PROP_OUT + "." + o);
        }

        columns.add(Case.PROP_STATE);

        columns.add(Case.PROP_OUT);

        return shell.prj.getCases().toCSV(';', columns.toArray(new String[columns.size()]));
    }

    /**
     * Convenience method to see experiments to run.
     *
     * @return A table / string representation of all experiments.
     */
    public static String inputInformation(AbstractShell shell) {
        return shell.prj.getCases().toString();
    }

    /**
     * Provide information about the whole project.
     *
     * @return general information about a project.
     */
    public static String projectInformation(AbstractShell shell) {
        return "Project " + shell.prj.getName() + " model " + shell.prj.getModel()
                + "\n  * Plugin: " + shell.prj.getPlugin().getPluginInformation()
                + "\n  * Input variables:\n" + variablesInformation(shell)
                + "  * Output expressions: " + outputInformation(shell) + "\n"
                + (shell.prj.getDesigner() != null ? "\n  * Designer " + shell.prj.getDesigner().getName()
                + "\n    * Type: " + shell.prj.getDesigner().getType()
                + "\n    * Return: " + shell.prj.getDesigner().getDesignOutputTitle()
                + "\n    * Options:\n  " + MapToMDString(shell.prj.getDesigner().getOptions()).replace("\n", "\n    ")
                + "\n    * Parameters: " + ASCII.cat(",", shell.prj.getDesigner().getParameters()) : "")
                + "\n  * Archive in " + shell.getArchiveDirectory().getAbsolutePath();
    }

    /**
     * Provide information about available output.
     *
     * @return List of available output from input files and given code/model.
     */
    public static String outputInformation(AbstractShell shell) {
        return ASCII.cat(" , ", shell.getOutputAvailable());
    }

    public static String gridStatusInformation() {
        Object o = new Object();
        if (!Funz_v1.POOL.isRefreshing()) {
            Funz_v1.POOL.setRefreshing(true, o,"gridStatusInformation");
            for (int i = 0; i < 5; i++) {
                try {
                    Thread.sleep(1000); // to let enough time to actualize grid
                } catch (InterruptedException ex) {
                }
                System.out.print(".");
            }
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Computer.toStringTitle()).append('\n');
        synchronized (Funz_v1.POOL) {
            System.out.print("|");
            for (Computer c : Funz_v1.POOL.getComputers()) {
                System.out.print("o");
                stringBuilder.append(c.toString()).append('\n');
            }
            System.out.print("|");
        }
        Funz_v1.POOL.setRefreshing(false, o, "end gridStatusInformation");
        System.out.print("\n");
        return stringBuilder.toString();
    }
}
