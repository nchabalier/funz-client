package org.funz.doeplugin;

import java.util.LinkedList;
import java.util.List;
import org.funz.log.Log;
import org.funz.log.LogCollector.SeverityLevel;
import org.funz.util.URLMethods;

public class DesignPluginsLoader {

    public static List<DesignerInterface> doeplugins = new LinkedList<DesignerInterface>();

    public static Designer newInstance(String id) {
        List<String> names = new LinkedList<>();
        for (DesignerInterface d : doeplugins) {
            names.add(d.getName());
            if (d.getName().equals(id)) {
                try {
                    Designer newd = null;
                    if (d instanceof RDesigner_V1) {
                        newd = ((RDesigner_V1) d).newInstance();
                    } else if (d instanceof RDesigner_V0) {
                        newd = ((RDesigner_V0) d).newInstance();
                    } else {
                        newd = (Designer) d.getClass().newInstance();
                    }

                    /*newd.setProject(parent);
                     parent.setDesigner(newd);
                     parent.setDesignerId(id);*/
                    return newd;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Log.logException(false, ex);
                }
            }
        }
        Log.err("Could not instanciate designer " + id + " (available are: " + names + ")", 1);
        return null;
    }

    public static boolean loadURL(String urlstr) {
        Log.out("Loading plugin from " + urlstr, 3);
        try {
            if (urlstr.endsWith(".jar")) {
                DesignerInterface d = (DesignerInterface) URLMethods.scanURLJar(urlstr, "org.funz.doeplugin.DesignerInterface");
                if (d != null) {
                    //System.out.println("+ Found plugin " + urlstr + " (class " + d.getClass().getName() + ")");
                    Log.logMessage(d, SeverityLevel.INFO, false, "+ Found plugin " + urlstr + " (class " + d.getClass().getName() + ")");
                    doeplugins.add(d);
                    return true;
                } else {
                    //System.out.println("Bad interface plugin " + urlstr);
                    Log.logMessage(urlstr, SeverityLevel.WARNING, false, "Bad interface plugin " + urlstr);
                    return false;
                }

            } else if (urlstr.endsWith(".R")) {
                Designer d = null;
                try {
                    String content = URLMethods.readURL(urlstr);
                    if (content.contains("nextDesign")) {                //backward compatibiity with funz 1.x
                        d = new RDesigner_V0(urlstr);
                    } else if (content.contains("getNextDesign")) {
                        d = new RDesigner_V1(urlstr);
                    } else {
                        Log.logMessage(urlstr, SeverityLevel.ERROR, true, "Could not identify interface version.");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (d != null) {
                    Log.logMessage(d, SeverityLevel.INFO, false, "+ Found basic plugin " + urlstr);
                    doeplugins.add(d);
                    return true;
                } else {
                    Log.logMessage(urlstr, SeverityLevel.WARNING, false, "Bad syntax plugin " + urlstr);
                    return false;
                }
            }
            return false;
        } catch (Exception e) {
            Log.logException(true, e);
            return false;
        }
    }
}
