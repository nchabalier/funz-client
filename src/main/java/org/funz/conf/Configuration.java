package org.funz.conf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import javax.swing.JFileChooser;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.io.FileUtils;
import org.funz.Constants;
import org.funz.Proxy;
import org.funz.api.Funz;
import org.funz.doeplugin.DesignPluginsLoader;
import org.funz.doeplugin.DesignerInterface;
import org.funz.ioplugin.IOPluginInterface;
import org.funz.ioplugin.IOPluginsLoader;
import org.funz.log.Log;
import org.funz.log.LogCollector;
import org.funz.log.LogCollector.SeverityLevel;
import org.funz.log.LogNull;
import org.funz.util.ASCII;
import static org.funz.util.ParserUtils.getASCIIFileContent;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Keeps the list of slots and project types. Is able to read XML and MD5
 * configuration files.
 */
public class Configuration {

    public static final String ELEM_CONFIG = "CONFIGURATION",
            ELEM_SLOTS = "SLOTS",
            ELEM_SLOT = "SLOT",
            ELEM_MODELS = "MODELS",
            ELEM_MODEL = "MODEL",
            ATTR_VERSION = "version",
            ATTR_UID = "userid",
            ATTR_FREE = "free",
            ATTR_EXPIRES = "expires",
            ATTR_MAX_CALCS = "max-calcs",
            ATTR_MAX_PROJECTS = "max-projects",
            ATTR_PORT = "port",
            ATTR_MULTICAST_IP = "multicast-ip",
            ATTR_TYPE = "type",
            ATTR_NAME = "name",
            ATTR_CODE = "code",
            ATTR_DOEPLUGIN = "doeplugin",
            ATTR_IOPLUGIN = "ioplugin",
            IP_PORT_SEPARATOR = ":";
    private static final String PRIVATE_KEY = "zeg6yddze1pf2gfuglf", // never change it
            LINE_SIGNATURE = "154 "; // never change it
    private static final char[] toHex = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    private static final int LINE_LENGTH = 128;
    static Configuration instance;

    public Configuration(File file, LogCollector log) {
        if (log == null) {
            Log.setCollector(new LogNull(""));
        } else {
            Log.setCollector(log);
        }

        try {
            //System.out.println("New configuration "+file); do not uncomment, otherwise will corrupt quotas.hex !!!!
            if (instance != null && !FileUtils.contentEquals(instance.file, file)) {
                System.err.println("Cannot instanciate another configuration with file " + file.getAbsolutePath() + " (already use " + instance.file.getAbsolutePath() + ")");
                Log.logMessage(this, SeverityLevel.ERROR, true, "Cannot instanciate another configuration with file " + file.getAbsolutePath() + " (already use " + Configuration.instance.file.getAbsolutePath() + ")");
                System.exit(-1);
            } else {
                this.file = file;
                instance = this;
            }
        } catch (IOException ex) {
            Log.logMessage(this, SeverityLevel.ERROR, true, "Cannot access configuration file " + Configuration.instance.file.getAbsolutePath());
            System.exit(-2);
        }

        if (file.getName().endsWith(".xml")) {
            try {
                readQuotaXML(file);
            } catch (Exception ex) {
                System.err.println("Cannot read XML configuration file " + file.getAbsolutePath());
                Log.logMessage(this, SeverityLevel.ERROR, true, "Cannot read XML configuration file " + file.getAbsolutePath());
                ex.printStackTrace();
                System.exit(-2);
            }
        } else {
            try {
                readQuota(file);
            } catch (Exception ex) {
                System.err.println("Cannot read configuration file " + file.getAbsolutePath());
                Log.logMessage(this, SeverityLevel.ERROR, true, "Cannot read configuration file " + file.getAbsolutePath());
                ex.printStackTrace();
                System.exit(-2);
            }
        }

    }

    public static class Slot {

        Slot(/*String multicastIp,*/int port) {
            //this.multicastIp = multicastIp;
            this.port = port;
        }

        @Override
        public String toString() {
            return slotToString(this);
        }
        //public final String multicastIp;
        public final int port;
    }

    //public static FunzWindow getFunzWindow() {
    //    return _appwindow;
    //}
    private static String appendSpaces(String str) {
        assert str.length() < LINE_LENGTH;
        /*if (str.length() > LINE_LENGTH) {
         throw new IllegalArgumentException("String <" + str + "> length exceeds max length: " + LINE_LENGTH);
         }*/
        StringBuilder sb = new StringBuilder();
        sb.append(str);
        for (int i = str.length(); i < LINE_LENGTH; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private static byte[] hexStringToBytes(String str) {
        byte ret[] = new byte[str.length() / 2];
        for (int i = 0; i < str.length(); i += 2) {
            ret[i / 2] = (byte) (hexToInt(str.charAt(i)) * 16 + hexToInt(str.charAt(i + 1)));
        }
        return ret;
    }

    private static int hexToInt(char c) {
        if (c >= '0' && c <= '9') {
            return (c - '0');
        }
        return 10 + (c - 'A');
    }

    /**
     * Used as main point in conftool.jar
     */
    public static void main(String[] args) {
        //args= new String[] {"-extend_date"};
        if (args.length < 1) {
            usage();
        }

        try {
            if (args[0].equals("-extend_date")) {
                File f = null;
                if (args.length != 2) {
                    JFileChooser fc = new JFileChooser(new File(""));
                    fc.setMultiSelectionEnabled(false);
                    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    int o = fc.showOpenDialog(null);
                    if (o == JFileChooser.APPROVE_OPTION && fc.getSelectedFile() != null) {
                        f = fc.getSelectedFile();
                    } else {
                        return;
                    }
                } else {
                    f = new File(args[1]);
                }
                Configuration conf = new Configuration(f, null);
                conf.justry = true;
                System.out.println("\nPrevious quotas:");
                conf.print(System.out);
                Configuration._expires = Configuration._expires + 10000 - 600;
                System.out.println("\nNew quotas:");
                conf.print(System.out);
                conf.encrypt("never modify this file", new PrintStream(new File(f.getParentFile(), f.getName() + ".new")));
            } else if (args[0].equals("-print")) {
                if (args.length != 2) {
                    usage();
                }
                Configuration conf = new Configuration(new File(args[1]), null);
                conf.print(System.out);
            } else if (args[0].equals("-dump")) {
                if (args.length != 2) {
                    usage();
                }
                Configuration conf = new Configuration(new File(args[1]), null);
                conf.justry = true;
                System.out.println("\nQuotas parsing:");
                conf.print(System.out);
            } else if (args[0].equals("-lowcrypt")) {
                if (args.length < 2) {
                    usage();
                }
                for (int i = 1; i < args.length; i++) {
                    System.out.println(args[i] + " > " + lowCrypt(args[i]));
                }

            } else if (args[0].equals("-lowdecrypt")) {
                if (args.length < 2) {
                    usage();
                }
                for (int i = 1; i < args.length; i++) {
                    System.out.println(args[i] + " > " + lowdeCrypt(args[i]));
                }

            } else {
                Configuration conf = new Configuration(new File(args[0]), null);
                conf.encrypt("never modify this file", System.out);
            }
        } catch (Exception e) {
            err(e.getMessage(), 0);
        }
    }

    public static String lowdeCrypt(String s) {
        return new String(xorIt(hexStringToBytes(s), makeSimpleMask())).trim();
    }

    public static String lowCrypt(String s) {

        //System.out.println("MD5("+s+")=");

        /*MessageDigest md5 = null;
         try {
         md5 = MessageDigest.getInstance("MD5");
         } catch (NoSuchAlgorithmException e) {
         e.printStackTrace(System.err);
         }
        
         md5.reset();*/
        //System.out.println(""+appendSpaces(s).getBytes().length);
        //System.out.println(""+makeSimpleMask().length);
        String ret = toHexString(xorIt(appendSpaces(s).getBytes(), makeSimpleMask()));
        //System.out.println(ret);
        return ret;

    }

    public static String toHexString(byte b[]) {
        int pos = 0;
        char[] c = new char[b.length * 2];
        for (int i = 0; i < b.length; i++) {
            c[pos++] = toHex[(b[i] >> 4) & 0x0f];
            c[pos++] = toHex[b[i] & 0x0f];
        }
        return new String(c);
    }

    /**
     * Prints out the usage syntax.
     */
    private static void usage() {
        err("usage: java -jar conftool.jar ( [-print] config.xml | -dump url-of-config.hex | -lowcrypt anystring)", 0);
        System.exit(1);
    }

    private static byte[] xorIt(byte a[], byte b[]) {
        byte ret[] = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            ret[i] = (byte) (a[i] ^ b[i % b.length]);
        }
        return ret;
    }
    private byte _key[];
    private int _line;

    /*private static String intToHex(int i) {
     if (i < 10)
     return "" + i;
     return "" + (char) (i + (int) 'A');
     }*/
    /**
     * Constructs an empty configuration. After that either readXMLConf or
     * readMD5Conf must be invoked.
     */
    boolean justry = false;

    //public static void setFunzWindow(FunzWindow w) {
    //    _appwindow = w;
    //}
    private String decodeLine(String line) throws Exception {
        if (line == null) {
            throw new Exception("unexpected eof");
        }
        if (line.length() != LINE_LENGTH * 2) {
            throw new Exception("bad line length: " + line.length() + "\n" + line);
        }
        String in = new String(xorIt(hexStringToBytes(line), makeNextLineMask()));
        if (!in.startsWith(LINE_SIGNATURE)) {
            throw new Exception("bad configuration line");
        }
        return in.substring(LINE_SIGNATURE.length()).trim();
    }

    /**
     * Encrypts this configuration and prints out into a stream.
     *
     * @param comment signle line comment
     * @param w output stream
     */
    public void encrypt(String comment, PrintStream w) {
        try {
            w.println((comment == null ? "" : comment) + " " + lowCrypt(System.getProperty("user.name") + " " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(Calendar.getInstance().getTime()) + " " + InetAddress.getLocalHost().getHostName()));
            _key = new byte[LINE_LENGTH];
            // build random public key
            MessageDigest md = MessageDigest.getInstance("MD5");
            for (int i = 0; i < LINE_LENGTH;) {
                md.update(PRIVATE_KEY.getBytes());
                md.update(("" + Math.random()).getBytes());
                byte digest[] = md.digest();
                for (int j = 0; j < digest.length && i < LINE_LENGTH; j++) {
                    _key[i++] = digest[j];
                }
            }

            w.println(toHexString(_key));

            _line = 0;
            w.println(makeLine(LINE_SIGNATURE + _version));
            w.println(makeLine(LINE_SIGNATURE + _expires));
            w.println(makeLine(LINE_SIGNATURE + _uid));
            w.println(makeLine(LINE_SIGNATURE + _free));
            w.println(makeLine(LINE_SIGNATURE)); //reserve a couple of lines
            w.println(makeLine(LINE_SIGNATURE));

            w.println(makeLine(LINE_SIGNATURE + _maxCalcs));
            w.println(makeLine(LINE_SIGNATURE + _maxProjects));
            if (multicastIp == null) {
                w.println(makeLine(LINE_SIGNATURE));
            } else {
                w.println(makeLine(LINE_SIGNATURE + multicastIp));
            }
            w.println(makeLine(LINE_SIGNATURE + _slots.size()));
            for (Slot s : _slots) {
                w.println(makeLine(LINE_SIGNATURE + s));
            }

            w.println(makeLine(LINE_SIGNATURE + _model_code.size()));
            for (String p : _models) {
                w.println(makeLine(LINE_SIGNATURE + p));
                //if (_prjs_plugin.get(p).length() > 0)
                w.println(makeLine(LINE_SIGNATURE + _model_plugin.get(p)));
                //else w.println(makeLine(LINE_SIGNATURE + "NOPLUGIN"));
                w.println(makeLine(LINE_SIGNATURE + _model_code.get(p)));
            }

            /* w.println(makeLine(LINE_SIGNATURE + _designers.size()));
             for (String d : _designers) {
             w.println(makeLine(LINE_SIGNATURE + d));
             }*/
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    public static String getCode(String model) {
        return _model_code.get(model);
    }

    /**
     * Returns the expiration date in format YYYYMMDD
     */
    public static int getExpiration() {
        return _expires;
    }
    public final static String NOT_EXPIRED = "ok";
    public final static String SOON_EXPIRED = "Expiration date in ";
    public final static String EXPIRED = "Expiration date reached:";

    /**
     * Returns the expiration date in format YYYYMMDD
     */
    public static String checkDate(String expiration/*, boolean crypted*/) {
        if (expiration == null) {
            return NOT_EXPIRED;
        }

        /*if (crypted)
         expiration = new String(xorIt(hexStringToBytes(expiration), makeSimpleMask()));*/
        Calendar now = Calendar.getInstance();
        Calendar last = Calendar.getInstance();
        last.set(Integer.parseInt(expiration.substring(0, 4)), Integer.parseInt(expiration.substring(4, 6)) - 1, Integer.parseInt(expiration.substring(6, 8)));
        Log.logMessage(null, SeverityLevel.INFO, false, "Current date is " + now.getTime().toString() + " ... expiration date is " + last.getTime().toString());
        if (last.before(now)) {
            String mess = EXPIRED + last.getTime().toString();
            Log.logMessage(null, SeverityLevel.PANIC, false, mess);
            return mess;
        } else if (((int) ((last.getTimeInMillis() - now.getTimeInMillis()) / 86400000L)) < 30) {
            int remaining = (int) ((last.getTimeInMillis() - now.getTimeInMillis()) / 86400000L);
            String mess = SOON_EXPIRED + remaining + " days.";
            Log.logMessage(null, SeverityLevel.WARNING, false, mess);
            return mess;
        }
        return NOT_EXPIRED;
    }

    /**
     * Returns the maximum number calculators involved into a project.
     */
    public static int maxCalcs() {
        return _maxCalcs;
    }

    /**
     * Returns the maximum number calculators involved into a project.
     */
    public static int defaultCalcs() {
        return Math.min(_maxCalcs, 10);
    }

    public static String getIOPlugin(String model) {
        //System.out.println(ASCII.cat((HashMap) _prjs_plugin));
        return _model_plugin.get(model);
    }

    /// Shortcut to get a project linked to given plugin
    public static String getModel(IOPluginInterface plugin) {
        if (plugin == null) {
            return null;
        }

        for (String model : _models) {
            URI test_uri;
            URI plugin_uri;
            try {
                test_uri = new URI(getIOPlugin(model)).normalize();
                plugin_uri = new URI(plugin.getSource()).normalize();
            } catch (URISyntaxException ex) {
                continue;
            }
            if (test_uri.compareTo(plugin_uri) == 0) {
                return model;
            }

        }
        return null;
    }

    // to replace ports as a constraint 
    public LinkedList<Integer> getIds() {
        LinkedList<Integer> ids = new LinkedList<Integer>();
        for (int i = 0; i < _maxProjects; i++) {
            ids.add(i);
        }
        return ids;
    }

    /**
     * Returns the slot ports.
     */
    public List<Integer> getPorts() {
        LinkedList<Integer> ports = new LinkedList<Integer>();
        for (Configuration.Slot s : _slots) {
            ports.add(s.port);
        }
        return ports;
    }

    public LinkedList<Slot> getSlots() {
        return _slots;
    }

    /**
     * Returns the available project types.
     */
    public static List<String> getModels() {
        return _models;
    }

    /**
     * Returns the available project types.
     */
    public static List<String> getDesigners() {
        return _designers;
    }

    public static String uid() {
        return _uid;
    }

    /**
     * Returns the version number
     */
    public static String version() {
        return _version;
    }
    public boolean docrypt = true;

    public static boolean isProVersion() {
        return _free;
    }

    private String makeLine(String str) {
        if (docrypt == true) {
            return toHexString(xorIt(appendSpaces(str).getBytes(), makeNextLineMask()));
        } else {
            return str;
        }
    }

    private static byte[] makeSimpleMask() {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(PRIVATE_KEY.getBytes());
            return md.digest();
        } catch (Exception e) {
            // do nothing
            e.printStackTrace(System.err);
        }
        return null;
    }

    private byte[] makeNextLineMask() {
        try {
            //TODO add masks for 64 compat.
            MessageDigest md = MessageDigest.getInstance("MD5");
            _key[_line % LINE_LENGTH] = (byte) _line;
            _line++;

            byte mask[] = new byte[LINE_LENGTH];
            for (int i = 0; i < LINE_LENGTH;) {
                md.update(PRIVATE_KEY.getBytes());
                md.update(_key);
                byte digest[] = md.digest();
                for (int j = 0; j < digest.length && i < LINE_LENGTH; j++) {
                    mask[i++] = digest[j];
                }
            }
            return mask;
        } catch (Exception e) {
            // do nothing
            e.printStackTrace(System.err);
        }
        return null;
    }

    /**
     * Print the configuration content into a PrintStream.
     *
     * @param w output stream
     */
    public void print(PrintStream w) {
        w.println(ELEM_CONFIG + " " + ATTR_VERSION + "=" + _version + " " + ATTR_EXPIRES + "=" + _expires + " " + ATTR_UID + "=" + _uid + ATTR_FREE + "=" + _free);
        if (multicastIp != null && multicastIp.length() > 0) {
            w.println(ELEM_SLOTS + " " + ATTR_MAX_CALCS + "=" + _maxCalcs + " " + ATTR_MAX_PROJECTS + "=" + _maxProjects + " " + ATTR_MULTICAST_IP + "=" + multicastIp);
        } else {
            w.println(ELEM_SLOTS + " " + ATTR_MAX_CALCS + "=" + _maxCalcs + " " + ATTR_MAX_PROJECTS + "=" + _maxProjects);
        }

        if (_slots != null) {
            for (Slot s : _slots) {
                w.print("\t" + ELEM_SLOT + " " + ATTR_PORT + "=" + s.port);
                /*if (s.multicastIp != null) {
                 w.print(" " + ATTR_MULTICAST_IP + "=" + s.multicastIp);
                 }*/
                w.println();
            }
        }

        w.println(ELEM_MODELS);
        if (_model_code != null) {
            for (String p : _models) {
                w.println("\t" + ELEM_MODEL + " " + ATTR_NAME + " = " + p + "  " + ATTR_IOPLUGIN + " = " + _model_plugin.get(p) + "  " + ATTR_CODE + " = " + _model_code.get(p));
            }
        }

        /*w.println(ELEM_DESIGNERS);
         if (_designers != null) {
         for (String a : _designers) {
         w.println("\t" + ELEM_DESIGNER + "  " + ATTR_DOEPLUGIN + " = " + a);
         }
         }*/
    }

    private static String cleanURL(String url) {
        url = url.replace("file:.", Constants.APP_INSTALL_DIR.toURI().toString());
        url = url.replace("/./", "/");
        url = url.replace("//", "/");
        return url;
    }

    /**
     * Loads configuration parameters from a MD5 url.
     *
     * @param urlstr any valid UDL string
     */
    public void readQuota(String urlstr) throws Exception {
        out("Reading quota file " + urlstr, 1);
        URL url = new URL(urlstr);
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));

        try {

            // first line is always comment
            /*String line = */
            reader.readLine();
            _key = hexStringToBytes(reader.readLine());
            _line = 0;

            _version = (decodeLine(reader.readLine()));
            _expires = Integer.parseInt(decodeLine(reader.readLine()));
            _uid = decodeLine(reader.readLine());
            _free = Boolean.parseBoolean(decodeLine(reader.readLine()));
            //Funz.userID = _uid;

            decodeLine(reader.readLine()); // reserved lines
            decodeLine(reader.readLine());

            _maxCalcs = Integer.parseInt(decodeLine(reader.readLine()));
            _maxProjects = Integer.parseInt(decodeLine(reader.readLine()));
            //System.out.println("_maxCalcs = "+_maxCalcs);
            //BACKDOOR
            File bkdoor = new File(Constants.APP_USER_DIR + File.separator + ".backdoor");
            if (bkdoor.exists()) {
                _maxCalcs = Integer.parseInt(getASCIIFileContent(bkdoor).trim());
                out("Backdoor enabled: max number of calculators=" + _maxCalcs, 0);
            }
            //BACKDOOR

            multicastIp = decodeLine(reader.readLine());

            int nslots = Integer.parseInt(decodeLine(reader.readLine()));
            //System.out.println("nslots = "+nslots);
            _slots = new LinkedList<Slot>();
            for (int i = 0; i < nslots; i++) {
                _slots.add(stringToSlot(decodeLine(reader.readLine())));
            }

            int nprjs = Integer.parseInt(decodeLine(reader.readLine()));
            _model_code = new HashMap<String, String>();
            _model_plugin = new HashMap<String, String>();
            _models = new LinkedList<String>();

            // quotas codes
            for (int i = 0; i < nprjs; i++) {
                try {
                    String model = decodeLine(reader.readLine());
                    String plugin = cleanURL(decodeLine(reader.readLine()));
                    String code = decodeLine(reader.readLine());
                    if (justry || IOPluginsLoader.loadURL(/*new LogConsole(),*/plugin, model)) {
                        _models.add(model);
                        _model_code.put(model, code);
                        _model_plugin.put(model, plugin);
                        out("Accepted code plugin from quota " + model + " (code=" + code + ", plugin=" + plugin + ")", 1);
                    } else {
                        //_model_plugin.put(model, "");
                        err("Code plugin " + plugin + " not suitable.", 1);
                    }
                } catch (Exception e) {
                    err("Code plugin loading failed:" + e.getMessage(), 1);
                }
            }

            // local codes
            File plugins_dir = new File(Constants.APP_INSTALL_DIR + File.separator + Constants.PLUGINS_DIR + File.separator + Constants.IO_SUBDIR);
            if (plugins_dir.isDirectory()) {
                File[] local_ioplugins = plugins_dir.listFiles(new FileFilter() {

                    public boolean accept(File pathname) {
                        if (pathname.isDirectory()) {
                            return false;
                        }
                        for (String t : _model_plugin.keySet()) {

                            try {
                                if (new File(new URL(_model_plugin.get(t)).getFile()).equals(pathname)) {
                                    err("Ignore " + pathname.getAbsolutePath() + ": already used in quotas.", 2);
                                    return false;
                                }
                            } catch (MalformedURLException ex) {
                                ex.printStackTrace(System.err);
                                err("Invalid URL contruction: " + _model_plugin.get(t), 1);
                                //return false;
                            }
                        }
                        return pathname.getName().endsWith(IOPluginsLoader.BASIC_EXTENSION) || pathname.getName().endsWith(IOPluginsLoader.EXT_EXTENSION);// && ASCII.contains(pathname, "variableStartSymbol=", false);
                    }
                });
                out("Local IO plugins:" + Arrays.asList(local_ioplugins), 3);
                if (local_ioplugins != null && local_ioplugins.length > 0) {
                    for (File plugin : local_ioplugins) {
                        out("Trying to load plugin " + plugin, 10);
                        try {
                            String code = plugin.getName().substring(0, plugin.getName().lastIndexOf(IOPluginsLoader.BASIC_EXTENSION));
                            String model = code;//.replaceAll("_", " ");
                            String urlplugin = cleanURL(plugin.toURI().toURL().toString());
                            if (_model_plugin.containsValue(urlplugin)) {
                                err("Already defined plugin source " + urlplugin + " in quotas.", 1);
                            } else if (justry || IOPluginsLoader.loadURL(urlplugin, null)) {
                                _models.add(model);
                                _model_code.put(model, code);
                                _model_plugin.put(model, urlplugin);
                                out("Accepted code plugin from dir '" + plugins_dir.getName() + "' " + model + " (code=" + code + ", plugin=" + urlplugin + ")", 1);
                            } else {
                                err("Code plugin " + plugin.getAbsolutePath() + " not suitable.", 1);
                            }
                        } catch (Exception e) {
                            err("Code plugin loading failed:" + e.getMessage(), 1);
                        }
                    }
                }
            }
            out("IO plugins:" + _model_plugin, 2);

            // local designers
            _designers = new LinkedList<String>();
            DesignPluginsLoader.doeplugins.clear();
            File plugins_doe_dir = new File(Constants.APP_INSTALL_DIR + File.separator + Constants.PLUGINS_DIR + File.separator + Constants.DOE_SUBDIR);
            if (plugins_doe_dir.isDirectory()) {
                File[] doeplugins = plugins_doe_dir.listFiles(new FileFilter() {

                    public boolean accept(File pathname) {
                        return pathname.getName().endsWith(".jar") || pathname.getName().endsWith(".R");// && ASCII.contains(pathname, "variableStartSymbol=", false);
                    }
                });
                out("Local Design plugins:" + Arrays.asList(doeplugins), 3);
                if (doeplugins != null && doeplugins.length > 0) {
                    for (File plugin : doeplugins) {
                        out("Trying to load plugin " + plugin, 10);
                        try {
                            String doe = plugin.toURI().toURL().toString();
                            doe = doe.substring(doe.lastIndexOf('/') + 1);
                            doe = doe.substring(0, doe.lastIndexOf("."));
                            if (justry || DesignPluginsLoader.loadURL(plugin.toURI().toURL().toString())) {
                                //_designers.add(doe);
                                out("Accepted design plugin " + doe, 1);
                            } else {
                                err("Design plugin " + plugin + " not suitable.", 1);
                            }
                        } catch (Exception e) {
                            err("Design plugin loading failed:" + e.getMessage(), 1);
                        }
                    }
                    for (DesignerInterface d : DesignPluginsLoader.doeplugins) {
                        _designers.add(d.getName());
                    }
                }
            }
            out("Design plugins:" + _designers, 2);

//            int nalgos = Integer.parseInt(decodeLine(reader.readLine()));
//            for (int i = 0; i < nalgos; i++) {
//                String plugin = decodeLine(reader.readLine());
//                if (DesignPluginsLoader.loadURL(/*new LogConsole(),*/plugin)) {
//                    _designers.add(plugin);
//                } else {
//                    System.err.println("Design plugin " + plugin + " not suitable.");
//                }
//            }
        } catch (Exception e) {
            throw e;
        } finally {
            reader.close();
        }
    }

    /**
     * Loads configuration parameters from a file.
     */
    protected void readQuota(File conf) throws Exception {
        //System.out.println(conf.toURI().toURL().toString());
        readQuota(conf.toURI().toURL().toString());
    }

    public void readQuotaXML(Document d) throws Exception {

        Element e = d.getDocumentElement();
        if (!e.getTagName().equals(ELEM_CONFIG)) {
            throw new Exception("bad document type " + e.getTagName() /*+ " in " + conf*/);
        }

        //_version = 0;
        try {
            _version = (e.getAttribute(ATTR_VERSION).trim());
        } catch (Exception ex) {
            _version = null;
        }

        if (_version == null) {
            throw new Exception("configuration version not defined");
        }
        // TODO check version

        _expires = 0;
        try {
            _expires = Integer.parseInt(e.getAttribute(ATTR_EXPIRES).trim());
        } catch (Exception ex) {
            _expires = -1;
        }

        if (_expires == -1) {
            throw new Exception("bad expiration date");
        }

        try {
            //System.out.println("UID="+e.getAttribute(ATTR_UID));
            _uid = e.getAttribute(ATTR_UID);
        } catch (Exception ex) {
            _uid = "UNDEFINED";
        }

        try {
            //System.out.println("UID="+e.getAttribute(ATTR_UID));
            _free = Boolean.parseBoolean(e.getAttribute(ATTR_FREE));
        } catch (Exception ex) {
            _free = false;
        }

        NodeList slots = e.getElementsByTagName(ELEM_SLOTS);

        if (slots.getLength() == 0) {
            throw new Exception("slots not declared");
        }
        if (slots.getLength() > 1) {
            throw new Exception("too much slot declared");
        }

        Element slotsRoot = (Element) slots.item(0);

        try {
            _maxCalcs = Integer.parseInt(slotsRoot.getAttribute(ATTR_MAX_CALCS).trim());
        } catch (Exception ex) {
            _maxCalcs = 0;
        }

        try {
            _maxProjects = Integer.parseInt(slotsRoot.getAttribute(ATTR_MAX_PROJECTS).trim());
        } catch (Exception ex) {
            _maxProjects = 10;
        }

        if (slotsRoot.hasAttribute(ATTR_MULTICAST_IP)) {
            multicastIp = slotsRoot.getAttribute(ATTR_MULTICAST_IP);
        }

        slots = slotsRoot.getElementsByTagName(ELEM_SLOT);
        _slots = new LinkedList<Slot>();
        for (int i = 0; i < slots.getLength(); i++) {
            Element s = (Element) slots.item(i);
            int port = 0;
            String ip = null;
            try {
                port = Integer.parseInt(s.getAttribute(ATTR_PORT));
                if (multicastIp != null) {
                    InetAddress ia = InetAddress.getByName(multicastIp);
                    if (ia == null || !ia.isMulticastAddress()) {
                        throw new Exception("Bad multicast address " + ip + " (must be in range 224.0.0.0-239.255.255.255)");
                    }
                }
            } catch (NumberFormatException ex) {
                port = 0;
            }
            if (port == 0) {
                throw new Exception("bad port number in slot");
            }
            _slots.add(new Slot(/*ip, */port));
        }

        if (_slots.size() == 0) {
            throw new Exception("no slot declared");
        }

        NodeList models = ((Element) e.getElementsByTagName(ELEM_MODELS).item(0)).getElementsByTagName(ELEM_MODEL);
        //System.out.println("models " + models.getLength());
        _model_code = new HashMap<String, String>();
        _model_plugin = new HashMap<String, String>();
        _models = new LinkedList<String>();
        for (int i = 0; i < models.getLength(); i++) {
            Element p = (Element) models.item(i);

            String model = p.hasAttribute(ATTR_NAME) ? p.getAttribute(ATTR_NAME).trim() : p.getAttribute(ATTR_TYPE).trim();
            if (model == null || model.length() == 0) {
                throw new Exception("project type empty");
            }

            String code = p.getAttribute(ATTR_CODE).trim();
            if (code == null || code.length() == 0) {
                throw new Exception("code empty");
            }

            String plugin = p.getAttribute(ATTR_IOPLUGIN).trim();

            if (plugin == null) {
                plugin = "";
            }

            _model_code.put(model, code);
            //if (IOPluginsLoader.loadURL(/*new LogConsole(),*/plugin)) {
            _model_plugin.put(model, plugin);
            //} else {
            //    _model_plugin.put(type, "");
            //}
            _models.add(model);

            //System.out.println("Model="+type+" Code="+code+" Plugin="+plugin);
        }

        /*if (_prjs_code.size() == 0) {
         System.out.println("No project declared");
         }*/
//        NodeList algos = ((Element) e.getElementsByTagName(ELEM_DESIGNERS).item(0)).getElementsByTagName(ELEM_DESIGNER);
//        //System.out.println("algos " + algos.getLength());
//        _designers = new LinkedList<String>();
//        for (int i = 0; i < algos.getLength(); i++) {
//            Element p = (Element) algos.item(i);
//
//            String plugin = p.getAttribute(ATTR_DOEPLUGIN).trim();
//            if (plugin == null || plugin.length() == 0) {
//                throw new Exception("designer empty");
//            }
//
//            //if (DesignPluginsLoader.loadURL(/*new LogConsole(),*/plugin)) {
//            _designers.add(plugin);
//        //System.out.println("+ " + plugin);
//        //} else {
//        //    throw new Exception("designer not valid");
//        //}
//        }

        /*if (_designers.size() == 0) {
         System.out.println("No designer declared");
         }*/
    }

    /**
     * Loads configuration parameters from an XML file.
     */
    public void readQuotaXML(File conf) throws Exception {
        readQuotaXML(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(conf));
    }

    static volatile String last_timeDigest = "";

    public static synchronized String timeDigest() {
        String sb="";
        do {
            long time = System.currentTimeMillis();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
            sb = sdf.format(new Date(time));
        } while (last_timeDigest.equals(sb)); // ensure never return same timeDigest value

        last_timeDigest = sb;
        return last_timeDigest;
    }

    public static void logCode(String code, String data) {
        if (!getBoolProperty("print.file.code")) {
            return;
        }
        if (!logCodeFile.containsKey(code)) {
            logCodeFile.put(code, new File(logDir, "code." + code.replace(" ", "_") + "." + timeDigest() + ".log"));
        }
        ASCII.appendFile(logCodeFile.get(code), "[" + timeDigest() + "] " + data + "\n");
    }

    public static void logDOE(String doe, String data) {
        if (!getBoolProperty("print.file.doe")) {
            return;
        }
        if (!logDOEFile.containsKey(doe)) {
            logDOEFile.put(doe, new File(logDir, "doe." + doe.replace(" ", "_") + "." + timeDigest() + ".log"));
        }
        ASCII.appendFile(logDOEFile.get(doe), "[" + timeDigest() + "] " + data + "\n");
    }
    static List<String> logs;
    static boolean logall = false;

    public static boolean isLog(String from) {
        //System.out.println("isLog "+from);
        if (logall) {//shortcut
            return true;
        }
        if (logs == null) {
            String log = getProperty("log.filter", null);
            if (log == null || log.length() == 0) {
                return false;
            }
            logs = Arrays.asList(log.split(" "));
            out("To log:" + logs, 3);
            logall = logs.contains("*");

            return isLog(from);
        }
        if (logs.contains(from)) {//shortcut
            return true;
        }
        for (String l : logs) {
            if (l.equals("*")) {
                return true;
            }
            if (l.endsWith("*")) {
                if (l.startsWith(from)) {
                    return true;
                }
                if (l.startsWith("!" + from)) {
                    return false;
                }
            } else {
                if (l.equals(from)) {
                    return true;
                }
                if (l.equals("!" + from)) {
                    return false;
                }
            }
        }
        return false;
    }

    public final static String properties = Constants.APP_NAME + ".conf";

    public static boolean initialized() {
        return _propertiesUrl != null;
    }

    public static void readProperties(String urlstr) {
        if (urlstr == null) {
            _propertiesUrl = "file:"+properties;
        } else {
            _propertiesUrl = urlstr;
        }

        Properties props = new Properties();
        if (_propertiesUrl != null && _propertiesUrl.length() > 0) {
            try {
                URL url = new URL(_propertiesUrl);
                props.load(url.openStream());
            } catch (IOException e) {
                err("Properties unreachable:\n" + e.getMessage(), 0);
            }
        }
        verboselevel = Integer.parseInt(props.getProperty("verbosity", "1"));

        // user overload
        File userConf = new File(Constants.APP_USER_DIR, properties);
        if (userConf.exists() && userConf.canRead()) {
            try {
                props.load(userConf.toURI().toURL().openStream());             //Proxy
            } catch (IOException e) {
                err("User properties unreachable:\n" + e.getMessage(), 0);
            }
        }

        //Proxy
        Proxy.init(props);

        //GUI
        for (String k : props.stringPropertyNames()) {
            out("  " + k + " -> " + props.getProperty(k), 2);
            //if (k.startsWith("gui.")) {
            GUIproperties.put(k, props.getProperty(k));
            //}
        }
    }

    public static boolean hasProperty(String prop) {
        if (GUIproperties.containsKey(prop)) {
            //logMessage("Configuration", SeverityLevel.INFO, false, "Property " + prop + " = " + GUIproperties.get(prop));
            return true;
        } else {
            //logMessage("Configuration", SeverityLevel.INFO, false, "Property " + prop + " not found. Set to ''");
            return false;
        }
    }

    public static String getProperty(String prop, String default_value) {
        if (GUIproperties.containsKey(prop)) {
            //logMessage("Configuration", SeverityLevel.INFO, false, "Property " + prop + " = " + GUIproperties.get(prop));
            return GUIproperties.get(prop);
        } else {
            //logMessage("Configuration", SeverityLevel.INFO, false, "Property " + prop + " not found. Set to ''");
            return default_value;
        }
    }

    public static List<String> getKeyPropertiesStarting(String prop_prefix) {
        LinkedList<String> l = new LinkedList<String>();
        for (String k : GUIproperties.keySet()) {
            if (k.startsWith(prop_prefix)) {
                l.add(k);
            }
        }
        return l;
    }

    public static boolean getBoolProperty(String prop) {
        if (GUIproperties.containsKey(prop)) {
            //logMessage("Configuration", SeverityLevel.INFO, false, "Bool Property " + prop + " = " + (GUIproperties.get(prop)));
            return Boolean.parseBoolean(GUIproperties.get(prop));
        } else {
            //logMessage("Configuration", SeverityLevel.INFO, false, "Bool Property " + prop + " not found. Set to false");
            return false;
        }
    }

    public static void setProperty(String prop, String val) {
        GUIproperties.put(prop, val);
        writeUserProperty(prop, val);
    }

    public static void setBoolProperty(String prop, boolean val) {
        GUIproperties.put(prop, Boolean.toString(val));
        writeUserProperty(prop, Boolean.toString(val));
    }

    public static boolean writeUserProperty = true;
    
    public static void writeUserProperty(String prop, String val) {
        if (!writeUserProperty) {System.err.println("Will not save user property "+prop+": "+val); return;}
        //String urlstr = userProperties;

        Properties props = new java.util.Properties();
        File userConf = new File(Constants.APP_USER_DIR, properties);
        if (userConf.exists() && userConf.canRead()) {
            try {
                props.load(userConf.toURI().toURL().openStream());             //Proxy
            } catch (IOException e) {
                err("Properties unreachable:\n" + e.getMessage(), 0);
            }
        }

        props.put(prop, val);

        try {
            File write = new File(Constants.APP_USER_DIR, properties);

            if (!write.exists() || write.canWrite()) {
                FileOutputStream fos = new FileOutputStream(write);
                props.store(fos, null);
                fos.close();
            } else {
                throw new IOException(write.getCanonicalPath() + " not writable.");
            }
        } catch (Exception e) {
            Log.logException(false, e);
        }
    }

    public static void writeUserProperties(Properties prop) {
        if (!writeUserProperty) {System.err.println("Will not save user property "+prop); return;}
        //String urlstr = userProperties;

        Properties props = new java.util.Properties();
        File userConf = new File(Constants.APP_USER_DIR, properties);
        if (userConf.exists() && userConf.canRead()) {
            try {
                props.load(userConf.toURI().toURL().openStream());             //Proxy
            } catch (IOException e) {
                err("Properties unreachable:\n" + e.getMessage(), 0);
            }
        }

        for (String p : prop.stringPropertyNames()) {
            props.put(p, prop.getProperty(p));
        }

        try {
            File write = new File(Constants.APP_USER_DIR, properties);

            if (!write.exists() || write.canWrite()) {
                FileOutputStream fos = new FileOutputStream(write);
                props.store(fos, null);
                fos.close();
            } else {
                throw new IOException(write.getCanonicalPath() + " not writable.");
            }
        } catch (Exception e) {
            Log.logException(false, e);
        }
    }

    public static void setWWWConnected(boolean b) {
        wwwconnetced = b;
    }

    public static boolean isWWWConnected() {
        return wwwconnetced;
    }

    // converts a string like 239.0.0.1:8000 into a slot
    // if ip == null only port is present
    private static Slot stringToSlot(String ipWithPort) {
        int separatorPos = ipWithPort.indexOf(IP_PORT_SEPARATOR);
        if (separatorPos > 0) {
            return new Slot(/*ipWithPort.substring(0, separatorPos),*/
                    Integer.parseInt(ipWithPort.substring(separatorPos + IP_PORT_SEPARATOR.length())));
        }
        return new Slot(/*null,*/Integer.parseInt(ipWithPort));
    }

    private static String slotToString(Slot s) {
        StringBuilder sb = new StringBuilder();
        /*if (s.multicastIp != null) {
         sb.append(s.multicastIp + IP_PORT_SEPARATOR);
         }*/
        sb.append(s.port);
        return sb.toString();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n  file: " + file);
        sb.append("\n  version: " + _version);
        sb.append("\n  expires: " + _expires);

        sb.append("\n  models: " + _models);
        sb.append("\n  designs: " + _designers);

        sb.append("\n  www: " + wwwconnetced);
        sb.append("\n  uid: " + _uid);

        sb.append("\n  verbosity: " + verboselevel);
        sb.append("\n  log: " + Log.Collector);
        sb.append("\n  log directory: " + logDir);
        sb.append("\n  slots: " + _slots);
        sb.append("\n  multicastIp: " + multicastIp);
        sb.append("\n  max. calcs: " + _maxCalcs);
        sb.append("\n  max. projects: " + _maxProjects);

        sb.append("\n  user configuration: " + GUIproperties);

        return sb.toString();
    }

    public static File logDir;

    static {
        File userDir = Constants.APP_USER_DIR;
        boolean ok = true;
        if (!userDir.exists()) {
            ok = userDir.mkdirs();
        }
        if (!ok) {
            err("Impossible to create user directory in " + userDir.getAbsolutePath(), 0);
            System.exit(-1);
        }

        logDir = new File(Constants.APP_USER_DIR, "log");
        ok = true;
        if (!logDir.exists()) {
            ok = logDir.mkdirs();
        }
        if (!ok) {
            err("Impossible to create log directory in " + logDir.getAbsolutePath(), 0);
            System.exit(-1);
        }
    }

    static HashMap<String, File> logCodeFile = new HashMap<String, File>();
    static HashMap<String, File> logDOEFile = new HashMap<String, File>();

    private File file;
    public static String multicastIp;

    private LinkedList<Slot> _slots = null;
    private static HashMap<String, String> _model_code = null, _model_plugin = null;
    private static LinkedList<String> _models = null, _designers = null;

    private static boolean wwwconnetced = false;
    private static String _propertiesUrl = null;
    private static String _uid = "";
    public static boolean _free = true;
    private static String _version = "NOTSET";
    private static int _maxCalcs = 0, _maxProjects = 10, _expires = 0;
    private static HashMap<String, String> GUIproperties = new HashMap<String, String>();
    //private static boolean _fulliconify = false,_viewcalculatorstab = false,  _notifyVisual = true,  _notifySound = true,  _verboseR = false,  _testFormulas = true,  _printLog = true;
    //private static String _browser,  _filemanager,  _log = "";
    static int verboselevel = 10;//verboselevel();

    /*static int verboselevel() {
     int vl = 1;
     String verboselevel_property = System.getProperty("verbose.level");
     if (verboselevel_property != null) {
     try {
     vl = Integer.parseInt(verboselevel_property);
     } catch (NumberFormatException nfe) {
     }
     }
     return vl;
     }*/
    public static void setVerboseLevel(int l) {
        Funz.setVerbosity(l);
        //verboselevel = l;
    }

    //static LogCollector log = new LogNull("Configuration");
    static void out(String message, int l) {
        if (l < verboselevel) {
            Log.logMessage("Configuration", SeverityLevel.INFO, true, message);
        }
    }

    static void err(String message, int l) {
        if (l < verboselevel) {
            Log.logMessage("Configuration", SeverityLevel.ERROR, true, message);
        }
    }
}
