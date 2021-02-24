package org.funz.ioplugin;

import java.io.File;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import org.funz.log.Log;
import org.funz.util.URLMethods;

public class IOPluginsLoader {

    private static List<IOPluginInterface> url_ioplugins = new LinkedList<IOPluginInterface>();

    public static IOPluginInterface getFirstSuitablePlugin(File f) {
        //System.out.println(url_ioplugins);
        for (IOPluginInterface iop : url_ioplugins) {
            long tic = Calendar.getInstance().getTimeInMillis();
            try {
                boolean ok = false;
                if (f.isFile()) {
                    if (iop.acceptsDataSet(f)) {
                        ok = true;
                    }
                } else if (f.isDirectory()) {
                    if (iop.acceptsDataDirectory(f)) {
                        ok = true;
                    }
                }
                long toc = Calendar.getInstance().getTimeInMillis();
                Log.out("IOPlugin " + iop.getID() + " accept/reject ioplugin in " + ((toc - tic) / 1000.0) + " s.", 9);
                if (ok) {
                    IOPluginInterface p = iop;

                    if (p == null) {
                        Log.out("Empty DefaultIOPlugin selected.", 1);
                        p = new ExtendedIOPlugin();
                        //p.setFileName("");
                    } else {
                        Log.out("IOPlugin " + p.getID() + " selected.", 1);
                        if (p instanceof BasicIOPlugin) {
                            p = ((BasicIOPlugin) p).newInstance();
                        } else {
                            IOPluginInterface newp = p.getClass().newInstance();
                            newp.setID(p.getID());
                            p = newp;
                        }
                    }

                    return p;
                }
            } catch (Exception ex) {
                Log.err(ex, 1);
            }
        }
        return null;
    }

    public static IOPluginInterface newInstance(String model, File... inputFiles) throws Exception {
        if (model == null) {
            model = getFirstSuitablePlugin(inputFiles[0]).getID();
        }

        for (IOPluginInterface p : url_ioplugins) {
            /*if (p == null) {
                throw new Exception("Cannot identify model " + model + " in (" + url_ioplugins + ")");
            }*/
            if (p.getID().equals(model)) {
                long tic = Calendar.getInstance().getTimeInMillis();
                try {
                    //System.out.println(pluginName+" ->"+url_ioplugins.get(pluginName));
                    if (p instanceof BasicIOPlugin) {
                        p = ((BasicIOPlugin) p).newInstance();
                    } else {
                        IOPluginInterface newp = p.getClass().newInstance();
                        newp.setID(p.getID());
                        p = newp;
                    }
                    try {
                        p.setInputFiles(inputFiles);
                    } catch (Exception i) {
                        Log.err(i, 1);
                    }
                    Log.out("Loading plugin " + p.toString(), 1);
                } catch (Exception e) {
                    Log.err(e, 0);
                }
                long toc = Calendar.getInstance().getTimeInMillis();

                Log.out("IOPluginLoader.newInstance instanciated " + model + " in " + ((toc - tic) / 1000.0) + " s.", 3);
                return p;
            }
        }
        Log.err("Could not find model " + model+" in "+url_ioplugins, 0);
        return null;
    }
    public final static String BASIC_EXTENSION = ".ioplugin";
    public final static String EXT_EXTENSION = BASIC_EXTENSION + ".jar";

    public static boolean loadURL(String urlstr, String id) {
        Log.out("Loading plugin from " + urlstr + " for id=" + id, 3);
        try {
            if (urlstr.length() == 0) {
                IOPluginInterface iop =new ExtendedIOPlugin();
                if (id!=null) iop.setID(id);
                url_ioplugins.add(iop);
                return true;
            }
            
            if (urlstr.endsWith(EXT_EXTENSION)) {
                IOPluginInterface iop = (IOPluginInterface) URLMethods.scanURLJar(urlstr, "org.funz.ioplugin.IOPluginInterface");
                if (iop != null) {
                    if (id != null)
                        iop.setID(id);
                    Log.out(iop + " + Found plugin " + iop.getID() + " (class " + iop.getClass().getName() + ")", 3);
                    url_ioplugins.add(iop);
                    return true;
                } else {
                    Log.err(urlstr + "Bad interface plugin " + urlstr, 3);
                    return false;
                }

            } else if (urlstr.endsWith(BASIC_EXTENSION)) {
                IOPluginInterface iop = new BasicIOPlugin(urlstr);
                if (iop != null) {
                    if (id != null)
                        iop.setID(id);
                    Log.out(iop + " + Found basic plugin " + iop.getID() + " (file " + iop.getSource() + ")", 3);
                    url_ioplugins.add(iop);
                    return true;
                } else {
                    Log.err(urlstr + "Bad syntax plugin " + urlstr, 3);
                    return false;
                }
            } else {
                Log.err("Plugin Loader: Bad name plugin " + urlstr, 2);
                return false;
            }
        } catch (Exception e) {
            Log.err("IOPluginsLoader: "+ e.getMessage(),2);
            return false;
        }
    }
}
