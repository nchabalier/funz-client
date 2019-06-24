package org.funz.api;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import org.funz.Constants;
import org.funz.log.Log;
import org.funz.log.LogFile;

/**
 *
 * @author richet
 */
public class Funz {

    static {
        Locale.setDefault(new Locale("en", "US"));
        //System.setProperty("user.language", "en");
        //System.setProperty("user.country", "US");
        DecimalFormatSymbols decimalFormatSymbols = DecimalFormatSymbols.getInstance();
        decimalFormatSymbols.setDecimalSeparator('.');
        //Log.out("Decimal separator set to " + decimalFormatSymbols.getDecimalSeparator(),1);
    }

    static void init() {
        String OutErr = System.getProperty("outerr", "?");
        if (!OutErr.equals("?")) {
            try {
                File outerr = new File(OutErr);
                if (outerr.isFile()) {
                    if (!outerr.canWrite()) {
                        throw new FileNotFoundException("File " + outerr + " not writable.");
                    }
                }
                Log.setCollector(new LogFile(outerr));
            } catch (FileNotFoundException ex) {
                System.err.println("[Log] Cannot log: " + ex.getMessage());
            }
        }

        Log.out("Funz " + Constants.APP_VERSION + " <build " + Constants.APP_BUILD_DATE + ">", 0);
    }

    static String[] MODELS;
    static String[] DESIGNS;

    /**
     * Get the available code plugins on the dispatcher. On funz v2 also updates them if needed.
     *
     * @return Plugins names.
     */
    public static String[] getModelList() {
        return MODELS;
    }

    /**
     * Get the available doe plugins on the dispatcher. On funz v2 also updates them if needed.
     *
     * @return Plugins names.
     */
    public static String[] getDesignList() {
        return DESIGNS;
    }

    public static void setVerbosity(int l) {
        Log.level = l;
    }

    public static int getVerbosity() {
        return (Log.level);
    }

    /*static List<Project> projects = new LinkedList();

     public static List<Project> getProjects() {
     return projects;
     }
    
     public static List<String> getProjectsNames() {
     List<String> n = new ArrayList<>(projects.size());
     for (int i = 0; i < projects.size(); i++) {
     n.add(projects.get(i).getName());
     }
     return n;
     }
    
     public static Project getProject(String name) {
     for (int i = 0; i < projects.size(); i++) {
     if (projects.get(i).getName().equals(name)) return projects.get(i);
     }
     return null;
     }

     public static void addProject(Project p) {
     projects.add(p);
     }

     public static void removeProject(Project p) {
     projects.remove(p);
     }*/
    static List<AbstractShell> shells = new LinkedList();

    public static List<AbstractShell> getShells() {
        return shells;
    }

    public static List<String> getShellsNames() {
        List<String> n = new ArrayList<>(shells.size());
        for (int i = 0; i < shells.size(); i++) {
            n.add(shells.get(i).getProject().getName());
        }
        return n;
    }

    public static AbstractShell getShell(String name) {
        for (int i = 0; i < shells.size(); i++) {
            if (shells.get(i).getProject().getName().equals(name)) {
                return shells.get(i);
            }
        }
        return null;
    }

    public static void addShell(AbstractShell p) {
        shells.add(p);
    }

    public static void removeShell(AbstractShell p) {
        shells.remove(p);
    }

}
