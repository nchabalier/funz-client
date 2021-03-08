package org.funz.run;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.funz.api.Funz_v1;
import static org.funz.Protocol.PING_PERIOD;
import org.funz.conf.Configuration;
import org.funz.log.Alert;
import org.funz.log.Log;
import org.funz.log.LogCollector.SeverityLevel;
import org.funz.run.Computer.ComputerGuard;
import org.funz.run.Computer.ComputerStatusListener;

/**
 *
 * @author richet
 */
public class CalculatorsPool implements ComputerGuard, ComputerStatusListener {

    private int sleeping_period = 2000;
    private final List<Computer> _comps = new LinkedList<Computer>();
    private List<String> _codes = new ArrayList<String>();
    private final int _port;
    protected volatile boolean refreshing = false;
    ComputerListKeeper _computerListKeeper;
    DyingComputerDetector _dyingComputerDetector;
    private volatile boolean stop = true;
    public DatagramSocket UDP_socket;

//    public static void main(String[] args) throws InterruptedException {
//        CalculatorsPool pool = new CalculatorsPool(getFreePort(true), "239.192.0.100") {
//
//            @Override
//            public void fireUpdateComputerInfo(Computer comp) {
//                System.out.println("!" + comp);
//                super.fireUpdateComputerInfo(comp);
//            }
//
//            @Override
//            public void addComputer(Computer comp) {
//                System.out.println("+" + comp);
//                super.addComputer(comp); 
//            }
//
//        };
//        pool.setRefreshing(true, pool, "main");
//        pool.start();
//        Thread.sleep(60000);
//        pool.stop();
//    }

    public CalculatorsPool(DatagramSocket s) {
        this.UDP_socket = s;       
        _port = s.getLocalPort();
        Log.out("Starting computers pool on port " + _port + (Configuration.multicastIp != null && Configuration.multicastIp.length() > 0 ? " with multicast:" + Configuration.multicastIp : ""), 3);
    }

    public int getPort() {
        return _port;
    }

    public synchronized void shutdown() {
        for (Thread b : blackList) {
            b.interrupt();
            try {
                b.join();
            } catch (InterruptedException ex) {
            }
        }
        blackList.clear();

        stop();
    }

    void stop() {
        Log.logMessage(this, SeverityLevel.INFO, true, "stop...");

        stop = true;

        if (_computerListKeeper != null) {
            _computerListKeeper.interrupt();
        }
        if (_dyingComputerDetector != null) {
            _dyingComputerDetector.interrupt();
        }

        if (_computerListKeeper != null) {
            synchronized (_computerListKeeper) {
                //System.err.println("computerListKeeper notify");
                if (_computerListKeeper != null) {
                    _computerListKeeper.notify();
                }
            }
        }
        if (_dyingComputerDetector != null) {
            synchronized (_dyingComputerDetector) {
                //System.err.println("dyingComputerDetector notify");
                if (_dyingComputerDetector != null) {
                    _dyingComputerDetector.notify();
                }
            }
        }

        if (_computerListKeeper != null) {
            try {
                _computerListKeeper.join();
                //System.err.println("computerListKeeper joined");
            } catch (InterruptedException ex) {
                ex.printStackTrace(System.err);
            }
        }
        if (_dyingComputerDetector != null) {
            try {
                _dyingComputerDetector.join();
                //System.err.println("dyingComputerDetector joined");
            } catch (InterruptedException ex) {
                ex.printStackTrace(System.err);
            }
        }

        _computerListKeeper = null;
        _dyingComputerDetector = null;

        Log.logMessage(this, SeverityLevel.INFO, true, "    ...stopped");
    }

    void start() {
        Log.logMessage(this, SeverityLevel.INFO, true, "start...");

        if (!stop) {
            return;
        }
        stop = false;
        if (_computerListKeeper == null) {
            _computerListKeeper = new ComputerListKeeper(this);
            _computerListKeeper.start();
        }
        if (_dyingComputerDetector == null) {
            _dyingComputerDetector = new DyingComputerDetector();
            _dyingComputerDetector.start();
        }
        //System.err.println("computerListKeeper started");
        //System.err.println("DyingComputerDetector started");
        Log.logMessage(this, SeverityLevel.INFO, true, "     ...started");
    }

    public Computer getComputer(String ip, String name, String host, int port) {
        synchronized (_comps) {
            for (Iterator it = getComputers().iterator(); it.hasNext();) {
                Computer comp = (Computer) it.next();
                if ((ip == null || comp.ip.equals(ip)) && (name == null || comp.name.equals(name)) && (host == null || comp.host.equals(host)) && comp.port == port) {
                    return comp;
                }
            }
        }
        return null;
    }

    public void addComputer(Computer comp) {
        synchronized (_comps) {
            getComputers().add(comp);
        }
    }

    public void removeComputer(Computer comp) {
        synchronized (_comps) {
            getComputers().remove(comp);
        }
    }

    public void removeComputer(String host, int port) {
        synchronized (_comps) {
            for (Iterator it = getComputers().iterator(); it.hasNext();) {
                Computer comp = (Computer) it.next();
                if (comp.host.equals(host) && comp.port == port) {
                    _comps.remove(comp);
                    return;
                }
            }
        }
    }

    final List<Thread> blackList = new ArrayList<>();

    public void blacklistComputer(final String host, final int port, final long time) {
        synchronized (_comps) {
            for (Iterator it = getComputers().iterator(); it.hasNext();) {
                final Computer comp = (Computer) it.next();
                if (comp.host.equals(host) && comp.port == port) {
                    Thread b = new BlackLister(comp, time*1000);
                    b.start();
                    blackList.add(b);
                    return;
                }
            }
        }
    }

    class BlackLister extends Thread {

        long time;
        Computer comp;

        public BlackLister(Computer comp, long time) {
            super("blacklist Computer " + comp.host + ":" + comp.port);
            this.time = time;
            this.comp = comp;
        }

        @Override
        public void run() {
            comp.use = false;
            comp.freeUser();
            comp.freeActivity(); // otherwise, will stay in already reserved state...
            synchronized (this) {
                try {
                    //System.err.println("[POOL] blackList + " + host + ":" + port);
                    wait(time);
                    Alert.showInformation("Blacklisted computer " + comp.host + ":" + comp.port + " coming back...");
                    Log.out("Blacklisted computer " + comp.host + ":" + comp.port + " coming back...", 1);
                    //System.err.println("[POOL] blackList - " + host + ":" + port);
                } catch (InterruptedException ex) {
                    Log.out("Interrupt blacklisting of computer " + comp.host + ":" + comp.port, 3);
                }
            }
            comp.use = true;
        }
    }

    public void fireComputerDied(Computer comp, int row) {
    }

    public void fireComputerStatusUnknown(Computer comp, int row) {
    }

    public void fireUpdateComputerInfo(Computer comp) {
        //System.err.println("Update computer " + comp.ip + ":" + comp.port+ " ("+comp.activity+")");
    }

    public boolean isRefreshing() {
        return refreshing;
    }

    final List<Object> needForPool = new LinkedList<>();

    public /*synchronized*/ void setRefreshing(final boolean refresh, final Object needer, final String cause) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                Log.logMessage(this, SeverityLevel.INFO, true, "setRefreshing " + refresh + " (previous state was " + refreshing + ") by " + needer + " because " + cause);
                boolean was_refreshing = refreshing;
                if (needer != null) {
                    synchronized (needForPool) {
                        if (refresh) {
                            if (!needForPool.contains(needer)) {
                                needForPool.add(needer);
                            }
                        } else {
                            needForPool.remove(needer);
                        }
                        was_refreshing = refreshing;
                        refreshing = !needForPool.isEmpty();
                    }
                } else {
                    refreshing = true; // in case of startup, to avoid necessary calling setRefreshing(true, ...)
                }

                if (refreshing && !was_refreshing) {
                    start();
                } else if (!refreshing && was_refreshing) {
                    stop();
                }

                if (_computerListKeeper != null) {
                    synchronized (_computerListKeeper) {
                        if (_computerListKeeper != null) {
                            _computerListKeeper.notify();
                        }
                    }
                }
                if (_dyingComputerDetector != null) {
                    synchronized (_dyingComputerDetector) {
                        if (_dyingComputerDetector != null) {
                            _dyingComputerDetector.notify();
                        }
                    }
                }
            }
        }, "setRefreshing " + refresh + " (" + cause + ")").start();

    }

    public void setRefreshPeriod(int p) {
        sleeping_period = p;
    }

    public List<String> getCodes() {
        return _codes;
    }

    public List<Computer> getComputers() {
        return _comps;
    }

    public class ComputerListKeeper extends Thread {

        ComputerGuard guard;

        public ComputerListKeeper(ComputerGuard guard) {
            super("ComputerListKeeper");
            this.guard = guard;
        }

        public void update(DatagramSocket s) throws IOException {
            byte[] buf = new byte[1024];
            DatagramPacket p = new DatagramPacket(buf, buf.length);
            for (int i = 0; i < buf.length; i++) {
                buf[i] = 0;
            }
            try{            
                s.receive(p); // blocking as long as a packet not received / or SoTimeout exceeded
            }catch(SocketTimeoutException t) {
                force_close(s);
                s = getSocket(_port);
                return;
            }catch(Exception t) {
                Log.err(t,2);
            }
            String data = new String(p.getData());
            buf = null;

            StringReader sreader = new StringReader(data);
            BufferedReader reader = new BufferedReader(sreader);
            try {
                String name = reader.readLine();
                int port = Integer.parseInt(reader.readLine());
                String host = p.getAddress().getCanonicalHostName();
                String ip = p.getAddress().getHostAddress();
                long since = Long.parseLong(reader.readLine());
                String os = reader.readLine();
                String activity = reader.readLine();
                int ncodes = Integer.parseInt(reader.readLine());
                LinkedList codes = new LinkedList();
                StringBuilder clist = new StringBuilder();
                for (int i = 0; i < ncodes; i++) {
                    String c = reader.readLine();
                    codes.add(c);
                    clist.append(c);
                    if (i < ncodes - 1) {
                        clist.append(", ");
                    }
                }
                if (reader != null) {
                    reader.close();
                    reader = null;
                }
                if (sreader != null) {
                    sreader.close();
                    sreader = null;
                }

                //this last test may be usefull to release mass update on _compModel
                /*if ((_prj == null || _prj.getCode() == null) || codes.contains(_prj.getCode())) {// in order to skip update of this calculator if no good code found
                 //System.out.println(" +");
                 } else {
                 return;
                 }*/
                Computer comp = getComputer(ip, null, null, port);
                if (comp == null) {
                    comp = new Computer();
                    comp.ip = ip;
                    comp.host = host;
                    comp.name = name;
                    comp.port = port;
                    comp.since = since;
                    comp.os = os;
                    comp.activity = activity;
                    comp.setCodes(codes);
                    for (Iterator it = comp.getCodes().iterator(); it.hasNext();) {
                        String code = (String) it.next();
                        if (!_codes.contains(code)) {
                            getCodes().add(code);
                        }
                    }
                    comp.guard = guard;
                    comp.codeList = clist.toString();
                    comp.lastPing = System.currentTimeMillis();
                    addComputer(comp);
                } else {
                    synchronized (comp) {
                        boolean changed = false;
                        if (!changed && comp.port != port) {
                            comp.port = port;
                            changed = true;
                        }
                        if (!changed && comp.since != since) {
                            comp.since = since;
                            changed = true;
                        }
                        if (!changed && !comp.os.equals(os)) {
                            comp.os = os;
                            changed = true;
                        }
                        if (!changed && !comp.activity.equals(activity)) {
                            comp.activity = activity;
                            changed = true;
                        }
                        if (!changed && !comp.getCodes().toString().equals(codes.toString())) {
                            comp.setCodes(codes);
                            changed = true;
                        }
                        for (Iterator it = comp.getCodes().iterator(); it.hasNext();) {
                            String code = (String) it.next();
                            if (!_codes.contains(code)) {
                                getCodes().add(code);
                            }
                        }
                        if (!changed && !comp.codeList.equals(clist.toString())) {
                            comp.codeList = clist.toString();
                            changed = true;
                        }
                        comp.lastPing = System.currentTimeMillis();
                        if (changed) {
                            fireUpdateComputerInfo(comp);
                        }
                    }
                }
            } catch (Exception e) {
                Log.err(e, 1);
            }
        }
        
        void force_close(DatagramSocket s) {
            try {
                if (s != null) {
                    s.close();
                    s = null;
                }
            } catch (Exception e) {
            }
        }

        @Override
        protected void finalize() throws Throwable {
//            if (runtout != null) {
//                runtout.interrupt();
//            }

            super.finalize();
        }

//        TimeOut runtout;
//        volatile int numtout = 0;

        @Override
        public void run() {
            try {
                while (!stop) {
                    if (!isRefreshing()) {
                        //force_close();
                        synchronized (this) {
                            try {
                                wait(sleeping_period);
                            } catch (InterruptedException i) {
                            }
                        }
                    } else {
                        if (UDP_socket == null || UDP_socket.isClosed()) {
                            if (UDP_socket != null) {
                                if (UDP_socket.isBound()) {
                                    //return null;
                                } else {
                                    force_close(UDP_socket);
                                }
                            }
                            UDP_socket = getSocket(_port);
                        }
//Using TimeOut wrapper is a bit too much CPU cost... So, now replaced by recreating socket when no computer found within SoTimeout
//                        try {
//                            numtout++;
//                            //System.err.println("++ TimeOut ComputerListKeeper.update: " + (numtout));
//                            runtout = new TimeOut("ComputerListKeeper.update " + numtout) {// needed because update may be blocking if no computer pings
//
//                                @Override
//                                protected Object defaultResult() {
//                                    return false;
//                                }
//
//                                @Override
//                                protected Object command() {
//                                    try {
                                        update(UDP_socket);
//                                    } catch (IOException ex) {
//                                        return false;
//                                    }
//                                    return true;
//                                }
//
//                                @Override
//                                protected boolean break_command() {
//                                    s.close();
//                                    if (s != null) {
//                                        if (s.isBound()) {
//                                            //return null;
//                                        } else {
//                                            force_close(s);
//                                        }
//                                    }
//                                    s = getSocket(_port);
//                                    return true;
//                                }
//                            };
//                            runtout.execute(sleeping_period);
//                        } catch (TimeOut.TimeOutException te) {
//                            //seems a bit laggy, but still loop. Not blocking anyway
//                        }
                    }
                }
//                runtout.interrupt();
                force_close(UDP_socket);
            } catch (Exception e) {
                Log.logException(false, e);
            }
        }
    };

    public static DatagramSocket getFreePort(boolean exitIfNotFound) {
        int port = -1;
        for (Integer p : Funz_v1.CONF.getPorts()) {
            DatagramSocket s = null;
            try {
                s = new DatagramSocket(p);
                port = p;
                break;
            } catch (Exception ex) {
                if (ex instanceof BindException) {
                    continue;
                }
            } finally {
                try {
                    if (s != null) {
                        s.close();
                    }
                } catch (Exception e) {
                }
            }
        }
        if (exitIfNotFound && port < 0) {
            System.err.println("Did not find free port... Exit jvm.");
            System.exit(-66);
        }
        Log.out("Found free port: " + port, 2);
        return getSocket(port);
    }

    public static DatagramSocket getSocket(int port) {

           DatagramSocket s = null;
           
            try {
                if (Configuration.multicastIp == null) {
                    s = new DatagramSocket(port);
                } else {
                    MulticastSocket mcs = new MulticastSocket(port);
                    String[] mips = Configuration.multicastIp.split(",");
                    for (String ip : mips) {
                        try {
                            mcs.joinGroup(InetAddress.getByName(ip));
                        } catch (Exception e) {
                            Log.logMessage("[Socket] ", SeverityLevel.WARNING, false, "MultiCast join group failed:\n  " + e.getMessage());
                        }
                    }
                    s = mcs;
                }
            } catch (Exception ex) {
                Log.logException(false, ex);
                if (ex instanceof BindException) {
                    Alert.showError("Another instance of Funz client may be already running on port " + port);
                    System.exit(-1);
                }
            }
            try {
                s.setSoTimeout(PING_PERIOD*10); // to let network latency not blocking receive() for too long. Will re-create socket otherwise.
            } catch (SocketException ex) {
                ex.printStackTrace();
            }
            return s;
        }

    
    public class DyingComputerDetector extends Thread {

        public DyingComputerDetector() {
            super("DyingComputerDetector");
        }

        @Override
        public void run() {
            while (!stop) {
                if (!isRefreshing()) {
                    try {
                        synchronized (this) {
                            wait(sleeping_period); // TODO replace that by a synchronized !!
                        }
                    } catch (Exception e) {
                    }
                } else {
                    synchronized (_comps) {
                        long now = System.currentTimeMillis();
                        int row = 0;
                        for (Iterator it = getComputers().iterator(); it.hasNext(); row++) {
                            final Computer comp = (Computer) it.next();
                            long diff = now - comp.lastPing;
                            if (diff > PING_PERIOD*10) {
                                fireComputerDied(comp, row);
                                row--;
                                it.remove();
                            } else if (diff > PING_PERIOD*5) {
                                comp.activity = "?";
                                fireComputerStatusUnknown(comp, row);
                            }
                        }
                    }

                    synchronized (this) {
                        try {
                            wait(PING_PERIOD);
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }
    };

    // Hard shortcut to force reset of computers list. Should not be used in general, as DyingComputerDetector should do it automatically & softly.
    public void forceResetComputers() {
        synchronized (_comps) {
            int row = 0;
            for (Iterator it = getComputers().iterator(); it.hasNext(); row++) {
                final Computer comp = (Computer) it.next();
                fireComputerDied(comp, row);
                row--;
                it.remove();
            }
        }
    }
}
