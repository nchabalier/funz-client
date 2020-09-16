package org.funz.run;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.funz.*;
import static org.funz.Protocol.END_OF_REQ;
import static org.funz.Protocol.RET_HEARTBEAT;
import static org.funz.Protocol.RET_INFO;
import static org.funz.Protocol.RET_YES;
import org.funz.util.ASCII;
import org.funz.util.Disk;
import org.funz.util.TimeOut;
import org.funz.util.TimePeriod;

/**
 * Marshals the network requests from GUI to Calculator.
 */
public class Client implements Protocol {

    public static class CalculatorInfo {

        public CodeInfo codes[];
        public String comment;
        public TimePeriod periods[];
        public PluginInfo plugins[];
        public String spool;
        public String userName;
    }

    public static class CodeInfo {

        public String command;
        public String name;
        public String pluginFileName;
    }

    public static interface DataListener {

        public void informationLineArrived(String str);
    }

    public static class PluginInfo {

        public String className;
        public String name;
        public String type;
    }
    private DataInputStream _dis;
    private DataOutputStream _dos;
    private String _host;
    private byte _key[];
    protected DataListener _listener;
    private int _port;
    protected BufferedReader _reader;
    protected String _reason, _return, _secretCode = "";
    private volatile boolean _reserved = false;
    private boolean _isSecure = false;
    protected ArrayList _response = new ArrayList(1);
    protected Socket _socket;
    protected PrintWriter _writer;
    boolean log = false;

    public void log(String s) {
        //System.err.println("<<<<<<<<<<<<<<<<<<<<<<<<< " + s);
    }

    public static String getProtocol() {
        return "METHOD_RESERVE = " + METHOD_RESERVE + ",\n"
                + "METHOD_UNRESERVE = " + METHOD_UNRESERVE + ",\n"
                + "METHOD_GET_CODES = " + METHOD_GET_CODES + ",\n"
                + "METHOD_NEW_CASE = " + METHOD_NEW_CASE + ",\n"
                + "METHOD_EXECUTE = " + METHOD_EXECUTE + ",\n"
                + "METHOD_INTERRUPT = " + METHOD_INTERRUPT + ", \n"
                + "METHOD_PUT_FILE = " + METHOD_PUT_FILE + ", \n"
                + "METHOD_ARCH_RES = " + METHOD_ARCH_RES + ",\n"
                + "METHOD_GET_ARCH = " + METHOD_GET_ARCH + ",\n"
                + "METHOD_KILL = " + METHOD_KILL + ",\n"
                + "METHOD_GET_INFO = " + METHOD_GET_INFO + ",\n"
                + "METHOD_GET_ACTIVITY = " + METHOD_GET_ACTIVITY + ",\n"
                + "RET_YES = " + RET_YES + ", \n"
                + "RET_ERROR = " + RET_ERROR + ",\n"
                + "RET_NO = " + RET_NO + ",\n"
                + "RET_SYNC = " + RET_SYNC + ",\n"
                + "RET_INFO = " + RET_INFO + ", \n"
                + "RET_FILE = " + RET_FILE + ", \n"
                + "RET_HEARTBEAT = " + RET_HEARTBEAT + ",\n"
                + "END_OF_REQ = " + END_OF_REQ + ", \n"
                + "ARCHIVE_FILE = " + ARCHIVE_FILE + ",\n"
                + "ARCHIVE_FILTER = " + ARCHIVE_FILTER + ",\n"
                + "UNAVAILABLE_STATE = " + UNAVAILABLE_STATE + ",\n"
                + "ALREADY_RESERVED = " + ALREADY_RESERVED + ",\n"
                + "IDLE_STATE = " + IDLE_STATE + ",\n"
                + "PRIVATE_KEY = " + PRIVATE_KEY + ",\n"
                + "SOCKET_BUFFER_SIZE = " + SOCKET_BUFFER_SIZE + ";";
    }

    public Client(String host, int port) throws Exception {
        _host = host;
        _port = port;

        createSocket();

        readWatcher = new Thread(new Runnable() { // this thread checks (every 10 s.) that last message from calculator is not too old (60 s.).
            @Override
            public void run() {
                while (_socket != null) {
                    try {
                        synchronized (readWatcher) {
                            readWatcher.wait(10000);
                        }
                    } catch (InterruptedException ex) {
                    }
                    if (Calendar.getInstance().getTimeInMillis() - tstamp_reader > PING_PERIOD*10) { 
                        break;
                    }
                }
                if (_socket != null && !_socket.isClosed()) {
                    force_disconnect();
                }
            }
        }, "socket watcher");
    }
    public final Thread readWatcher;

    volatile long tstamp_reader = 0L;

    public synchronized boolean archiveResults() throws IOException {
        log("#" + METHOD_ARCH_RES);
        _writer.println(METHOD_ARCH_RES);                                    
        _writer.println(END_OF_REQ);    
        _writer.flush();
        return readResponse() && _return.equals(RET_YES);
    }

    private class SocketBuilder extends Thread {

        @Override
        public void run() {
            _reserved = false;
            //_log.log("    _reserved = false");
            try {
                if (_socket != null) {
                    if (!_socket.isClosed() && !_socket.isInputShutdown()) {
                        _socket.shutdownInput();
                    }
                    if (!_socket.isClosed() && !_socket.isOutputShutdown()) {
                        _socket.shutdownOutput();
                    }
                    if (!_socket.isClosed()) {
                        _socket.close();
                    }
                }
                _socket = new Socket(_host, _port);
                _socket.setTcpNoDelay(true);
                _socket.setTrafficClass(0x04);
                _socket.setSoTimeout(PING_PERIOD*5); // this will avoid blocking operation on client side, like unreserve when network failure

                if (_reader != null) {
                    try {
                        _reader.close();
                    } catch (Exception e) {
                    }
                }
                _reader = new BufferedReader(new InputStreamReader(_socket.getInputStream(), ASCII.CHARSET),128) {
                    @Override
                    public String readLine() throws IOException {
                        String s = super.readLine();
                        log("Client.reader < " + s);
                        return s;
                    }

                    @Override
                    public void close() throws IOException {
                        log("Client.reader < CLOSE");
                        super.close();
                    }
                };

                if (_writer != null) {
                    try {
                        _writer.close();
                    } catch (Exception e) {
                    }
                }
                _writer = new PrintWriter(_socket.getOutputStream(), false) {

                    @Override
                    public void println(Object obj) {
                        log("Client.writer > " + obj);
                        super.println(obj);
                    }

                    @Override
                    public void println(String i) {
                        log("Client.writer > " + i);
                        super.println(i);
                    }                    
                    
                    @Override
                    public void println(int i) {
                        log("Client.writer > " + i);
                        super.println(i);
                    }

                    @Override
                    public void flush() {
                        log("Client.writer >>");
                        super.flush();
                    }
                };

                if (_dos != null) {
                    try {
                        _dos.close();
                    } catch (Exception e) {
                    }
                }
                _dos = new DataOutputStream(_socket.getOutputStream());

                if (_dis != null) {
                    try {
                        _dis.close();
                    } catch (Exception e) {
                    }
                }
                _dis = new DataInputStream(_socket.getInputStream());

            } catch (Exception e) {
                disconnect();
            }
        }
    }
    SocketBuilder socketBuilder;

    protected void createSocket() throws IOException {
        log("# creating socket...");
        socketBuilder = new SocketBuilder();
        socketBuilder.start();
        //createSocket();
        int tries = 10;
        while (tries > 0) {
            tries--;
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
            }
            if (_socket != null) {
                log("#     socket created.");
                return;
            }
        }
        log("#    socket not created !");
        throw new IOException("Socket creation failed!");
    }

    public synchronized void disconnect() {
        log("#disconnect");
        try {
            if (_reserved) {
                unreserve();
            }
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
        if (isConnected()) {
            force_disconnect();
        }
        _reserved = false;
        if (socketBuilder != null) {
            socketBuilder.interrupt();
            socketBuilder = null;
        }
        readWatcher.interrupt();
        log("#disconnect DONE");
    }

    public void force_disconnect() {
        log = true;
        log("#force_disconnect");
        try {
            if (_socket != null) {
                if (!_socket.isClosed() && !_socket.isInputShutdown()) {
                    log(" !! socket.shutdownInput");
                    _socket.shutdownInput();
                }
                if (!_socket.isClosed() && !_socket.isOutputShutdown()) {
                    log(" !! socket.shutdownOutput");
                    _socket.shutdownOutput();
                }
                if (!_socket.isClosed()) {
                    log(" !! socket.close");
                    _socket.close();
                }
                _socket = null;
            }
        } catch (Exception e) {
        }
        try {
            if (_reader != null) {
                log(" !! reader.close");
                _reader.close();
                _reader = null;
            }
        } catch (Exception e) {
        }
        try {
            if (_writer != null) {
                log(" !! writer.close");
                _writer.close();
                _writer = null;
            }
        } catch (Exception e) {
        }
        try {
            if (_dis != null) {
                log(" !! dis.close");
                _dis.close();
                _dis = null;
            }
        } catch (IOException ex) {
        }
        try {
            if (_dos != null) {
                log(" !! dos.close");
                _dos.close();
                _dos = null;
            }
        } catch (IOException ex) {
        }
        readWatcher.interrupt();
    }
    public volatile boolean executing = false;

    public synchronized boolean execute(String codeName, DataListener listener) throws Exception {
        log("#" + METHOD_EXECUTE + " " + codeName);
        if (executing) {
            throw new IllegalArgumentException("Already executing !");
        }
        executing = true;
        _writer.println(METHOD_EXECUTE);
        _writer.println(codeName);
        _writer.println(END_OF_REQ);
        _writer.flush();
        _listener = listener;
        if (readResponse()) {
            boolean ret = readResponse();
            _listener = null;
            executing = false;
            log("..." + METHOD_EXECUTE + " " + codeName + " readResponse:" + ret);
            return ret && _return.equals(RET_YES);
        } else {
            _listener = null;
            executing = false;
            log("..." + METHOD_EXECUTE + " " + codeName + " !readResponse: "+_reason);
            return false;
        }
    }

    public String getHost() {
        return _host;
    }

    public int getPort() {
        return _port;
    }

    public synchronized boolean getInfo(CalculatorInfo ci) throws Exception {
        log("#" + METHOD_GET_INFO);
        _writer.println(METHOD_GET_INFO);
        _writer.println(END_OF_REQ);
        _writer.flush();
        if (!readResponse()) {
            return false;
        }

        ci.userName = readLineTimeout();
        ci.spool = readLineTimeout();
        ci.comment = readLineTimeout();

        log(" ! " + ci.userName + " " + ci.spool + " " + ci.comment);
        //System.err.println("getInfo:"+ci.userName+" "+ci.spool+ " "+ci.comment);

        int n = Integer.parseInt(readLineTimeout());
        ci.codes = new CodeInfo[n];
        for (int i = 0; i < n; i++) {
            CodeInfo c = new CodeInfo();
            ci.codes[i] = c;
            c.name = readLineTimeout();
            c.pluginFileName = readLineTimeout();
            c.command = readLineTimeout();
            //System.err.println("getInfo: Code: "+c.name+" "+c.command);
            log(" ! Code: " + c.name + " " + c.command);
        }

        n = Integer.parseInt(readLineTimeout());
        //System.err.println("getInfo: "+n+" plugins");
        ci.plugins = new PluginInfo[n];
        for (int i = 0; i < n; i++) {
            PluginInfo p = new PluginInfo();
            ci.plugins[i] = p;
            p.name = readLineTimeout();
            //System.err.println("getInfo: Plugin "+i+"/"+n+" "+p.name);
            //p.type = readLineTimeout();
            //System.err.println("getInfo: Plugin: "+p.name+" "+p.type);
            // p.className = readLineTimeout();
            // System.err.println("getInfo: Plugin: "+p.name+" "+p.type+" "+p.className);
            log(" >> Plugin: " + p.name + " " + p.type + " " + p.className);
        }

        n = Integer.parseInt(readLineTimeout());
        ci.periods = new TimePeriod[n];
        for (int i = 0; i < n; i++) {
            String t1 = readLineTimeout();
            String t2 = readLineTimeout();
            TimePeriod p = new TimePeriod(t1, t2);
            ci.periods[i] = p;
            log(" ! Period: " + p);
        }
        return true;
    }

    public synchronized String getActivity() throws Exception {
        log("#" + METHOD_GET_ACTIVITY);
        _writer.println(METHOD_GET_ACTIVITY);
        _writer.println(END_OF_REQ);
        _writer.flush();
        if (!readResponse()) {
            return "unknown activity";
        }

        return readLineTimeout();
    }

    public String getReason() {
        return _reason;
    }

    public boolean isConnected() {
        return _socket != null && _socket.isConnected() && !_socket.isClosed();
    }

    public boolean isReserved() {
        return _reserved;
    }

    public synchronized boolean killRunningCode(String secretCode) throws Exception {
        log = true;
        log("#" + METHOD_KILL + " (" + secretCode + ")");
        if (!executing) {
            return false;
        }
        _writer.println(METHOD_KILL);
        _writer.println(secretCode);
        _writer.println(END_OF_REQ);
        _writer.flush();
        boolean res = readResponse();
        _reason = "killed by user";
        log("#" + METHOD_KILL + " " + (res ? "DONE" : "FAILED"));
        return res && _return.equals(RET_YES);
    }

    public synchronized boolean newCase(Map vars) throws Exception {
        log("#" + METHOD_NEW_CASE + " " + vars);
        _writer.println(METHOD_NEW_CASE);
        _writer.println(END_OF_REQ);
        _writer.flush();
        
        if (vars == null) {
            vars = new HashMap();
        }
        vars.put("USERNAME", System.getProperty("user.name"));

        _writer.println(vars.size());
        for (Iterator it = vars.keySet().iterator(); it.hasNext();) {
            String key = (String) it.next();
            _writer.println(key);
            //PATCH pour n'envoyer que la 1ere ligne de la valeur de la variable. sinon, la seconde ligne est interprete comme une commande par le calculator...
            String firstlinevalue = (String) vars.get(key);
            int returnCharIndex = firstlinevalue.indexOf("\n");
            if (returnCharIndex > -1) {
                firstlinevalue = firstlinevalue.substring(0, returnCharIndex) + "...";
            }
            _writer.println(firstlinevalue);
        }
        _writer.flush();
        
        return readResponse() && _return.equals(RET_YES);
    }

    public synchronized boolean putFile(File file, File root) throws Exception {
        if (!file.exists()) {
            throw new IOException("File " + file + " does not exists, so cannot putFile");
        }

        log("#" + METHOD_PUT_FILE + " " + root + " / " + file);
        if (file == null || !file.exists()) {
            return false;
        }
        if (_dos == null) {
            return false;
        }

        _writer.println(METHOD_PUT_FILE);
        String relpath = file.getAbsolutePath().replace(root.getAbsolutePath(), "").replace(File.separatorChar, '/');
        _writer.println(relpath);
        _writer.println(file.length());
        _writer.println(END_OF_REQ);
        _writer.flush();
        if (!readResponse() || !_return.equals(RET_YES)) {
            return false;
        }
        if (!_isSecure) {
            Disk.serializeFile(_dos, file, file.length(), null);
        } else {
            Disk.serializeEncryptedFile(_dos, file, file.length(), _key, null);
        }
        return readResponse() && _return.equals(RET_YES);
    }
    List<TimeOut> timeouts = new LinkedList<>();

    @Override
    protected void finalize() throws Throwable {
        for (TimeOut t : timeouts) {
            if (t != null) {
                t.interrupt();
            }
        }
        super.finalize(); 
    }

    String readLineTimeOut(long timeout) {
        TimeOut to = new TimeOut("Client.readLine") {
            @Override
            protected Object command() {
                try {
                    return readLineTimeout();
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            protected Object defaultResult() {
                return null;
            }
        };
        timeouts.add(to);
        try {
            to.execute(timeout);
        } catch (TimeOut.TimeOutException e) {
            log(e.getLocalizedMessage());
        }
        if (to.getResult()==null) return null;
        return to.getResult().toString();
    }

    String readLineTimeout() throws IOException {
        tstamp_reader = Calendar.getInstance().getTimeInMillis();
        if (_reader==null) return null;
        return _reader.readLine();
    }

    protected synchronized boolean readResponse() throws IOException {
        log("#readResponse");
        try {
            if (_response != null) {
                _response.clear();
                _response.ensureCapacity(1);
            }

            /*log("readResponse ... _reader.ready(): " + _reader.ready());
            System.err.print("Z");
            int sleep = 0;
            try {
                while (!_reader.ready() && sleep<=1000 ) {
                    Thread.sleep((sleep++) * 100);
                    System.err.print("Z");
                }
                System.err.println("Z");
            } catch (InterruptedException e) {
                System.err.println("X");
            } catch(Exception e){
                e.printStackTrace();
                return false;
            }
            if (sleep>=1000) {      
                _return = "unknown error";
                _reason = "no response";
                return false;
            }*/

            String line;
            int counter = 0;
            while ((line = readLineTimeout()) != null) {
                log(" < " + line);
                if (line.equals(END_OF_REQ)) {
                    log(":EOR");
                    break;
                }

                if (counter == 0 && line.equals(RET_HEARTBEAT)) {
                    log(":HEARTBEAT");
                    continue;
                }
                if (counter == 0 && line.equals(RET_INFO)) {
                    log(":INFO: ");
                    if (_listener != null) {
                        String info = readLineTimeout();
                        _listener.informationLineArrived(info);
                        log("       " + info);
                    }
                    continue;
                }
                _response.add(line);
                counter++;
            }
            _return = "unknown error";
            _reason = "unknown reason";

            if (line == null) {
                _reason = "no stream";
                log(":NULL");
                throw new IOException("no stream");
            }

            if (!_socket.isConnected()) {
                log(":DISCONNECT");
                _reason = "connection lost";
                throw new IOException("connection lost");
            }

            if (_response == null || _response.size() == 0) {
                log(":0");
                _reason = "empty response";
                return false;
            } else {                
                log(":readResponse: " + _response);
                _return = (String) _response.get(0);
                if (!_return.equals(RET_YES)) {
                    _reason = (String) _response.get(1);
                    log(" << ERROR ret=" + _return + " reason=" + _reason);
                    return true;
                } else 
                    _reason = "-";
                return true;
            }
        } catch (IOException e) {
            log(":IOException: " + e.getMessage());
            log(":reason: " + _reason);
            return false;
        }
    }

    public void reconnect() throws Exception {
        log("#reconnect");
        createSocket();
    }
    public TimeOut reserveTimeOut;

    public boolean reserveTimeOut(long timeout, final Project prj, final StringBuffer ip, final StringBuffer secretCode) {
        log("#reserveTimeOut");
        if (reserveTimeOut != null) {
            reserveTimeOut.interrupt();
        }
        reserveTimeOut = new TimeOut("Client.reserve") {

            @Override
            protected Object defaultResult() {
                return false;
            }

            @Override
            protected Object command() {
                try {
                    boolean res = reserveAsync(prj, ip, secretCode);
                    log("# reserve " + res);
                    return res;
                } catch (Exception e) {
                    log("# reserve Exception");
                    return false;
                }
            }

            @Override
            protected boolean break_command() {
                force_disconnect();
                return true;
            }
            
        };
        try {
            reserveTimeOut.execute(timeout);
        } catch (TimeOut.TimeOutException ex) {
            log("#reserveTimeOut: " + ex.getLocalizedMessage());
            //return false;
        }
        boolean ret = (Boolean) reserveTimeOut.getResult();
        log("#reserveTimeOut = " + ret);
        return ret;
    }

    synchronized boolean reserve(Project prj, StringBuffer ip, StringBuffer secretCode) throws Exception {
        return reserveAsync(prj, ip, secretCode);
    }
    
    boolean reserveAsync(Project prj, StringBuffer ip, StringBuffer secretCode) throws Exception {
        log("#" + METHOD_RESERVE + " " + prj.getName() + " ip=" + ip + " (" + secretCode + ")");
        _writer.println(METHOD_RESERVE);
        _writer.println(END_OF_REQ);
        _writer.flush();

        readWatcher.start();

        if (!readResponse()) {
            log("reserve: !readResponse 1 "+_reason);
            return false;
        } else {
            log("reserve: readResponse 1 " + _response);
            if (! _return.equals(RET_YES)) {
                return false;
            }
        }

        _writer.println(prj.getCode());
        _writer.println(prj.getTaggedValues().size());
        for (String k : prj.getTaggedValues().keySet()) {
            _writer.println(k);
            _writer.println(prj.getTaggedValues().getOrDefault(k, ""));
        }
        _writer.flush();

        if (!readResponse()) {
            log("reserve: !readResponse 2 "+_reason);
            return false;
        } else {
            log("reserve: readResponse 2 " + _response);
        }

        _secretCode = (String) _response.get(1);
        secretCode.append(_secretCode);
        ip.append((String) _response.get(2));
        String secure = (String) _response.get(3);
        _isSecure = secure.equals("Y");
        if (_isSecure) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                md.update(PRIVATE_KEY.getBytes());
                md.update(_secretCode.getBytes());
                _key = md.digest();
            } catch (Exception e) {
                _key = new byte[]{0};
            }
        }

        _reserved = _return.equals(RET_YES);
        log(" ! _reserved = " + _reserved);
        //to test auto unreserve on server-side : Thread.sleep(NetworkClient.RESERVE_TIMEOUT*2);
        return _reserved;
    }

    public synchronized boolean transferArchive(File path, Disk.ProgressObserver observer) throws IOException {
        log("#" + METHOD_GET_ARCH + " " + path);
        _writer.println(METHOD_GET_ARCH);
        _writer.println(END_OF_REQ);
        _writer.flush();

        if (readResponse() && _return.equals(RET_YES)) {
            try {
                long size = Long.parseLong(readLineTimeout());
                _writer.println(RET_SYNC);
                _writer.flush();
                File archive = new File(path.getPath() + File.separator + ARCHIVE_FILE);
                if (!_isSecure) {
                    //toClose.add(Disk.deserializeFile(_dis, archive.getPath(), size, observer));
                    Disk.deserializeFile(_dis, archive.getPath(), size, observer);
                } else {
                    //toClose.add(Disk.deserializeEncryptedFile(_dis, archive.getPath(), size, _key, observer));
                    Disk.deserializeEncryptedFile(_dis, archive.getPath(), size, _key, observer);
                }
            } catch (Exception e) {
                _reason = e.toString();
                return false;
            }
            return true;
        }
        return false;
    }

    public synchronized boolean unreserve() throws Exception {
        log("#" + METHOD_UNRESERVE);
        _writer.println(METHOD_UNRESERVE);
        _writer.println(END_OF_REQ);
        _writer.flush();
        boolean ok = readResponse() && _return.equals(RET_YES);
        _reserved = !ok;
        log(" ! unreserved = " + ok + " response=" + _response);
        return ok;
    }
}
