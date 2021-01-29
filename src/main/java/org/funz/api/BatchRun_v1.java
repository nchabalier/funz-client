package org.funz.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import static java.lang.Thread.sleep;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.funz.Constants;
import org.funz.Project;
import org.funz.ProjectController;
import org.funz.Protocol;
import static org.funz.Protocol.ARCHIVE_FILTER;
import org.funz.log.Alert;
import org.funz.log.Log;
import org.funz.log.LogTicToc;
import org.funz.parameter.Cache;
import org.funz.parameter.Case;
import org.funz.parameter.Case.Observer;
import org.funz.parameter.CaseList;
import org.funz.parameter.OutputFunctionExpression;
import org.funz.run.Client;
import org.funz.run.Computer;
import static org.funz.util.Data.*;
import org.funz.util.Disk;
import static org.funz.util.Format.repeat;
import org.funz.util.ZipTool;
import org.math.array.IntegerArray;

/**
 * @author richet
 */
public abstract class BatchRun_v1 {

    Project prj;
    volatile boolean askToStop = false;
    Thread stopRun;
    Observer observer;
    long SLEEP_PERIOD = 1000;
    private volatile String state = "Not started.";
    private File archiveDirectory;

    List<RunCase> runCases = new LinkedList<>();
    public Map<String, Object[]> merged_results;

    public BatchRun_v1(Observer observer, Project prj, File archiveDirectory) {
        this.prj = prj;
        this.observer = observer;
        try {
            setArchiveDirectory(archiveDirectory);
        } catch (IOException ex) {
            ex.printStackTrace();
                    
            err(ex, 0);
        }
        // It should not be still here !!! addCacheDirectory(prj.getResultsDir());
        addCacheDirectory(prj.getOldResultsDir());
    }

    private List<Cache> cache = new LinkedList<>();

    /**
     * @return the cache
     */
    public List<Cache> getCache() {
        return cache;
    }

    /**
     * @param cache the cache to set
     */
    public void setCache(List<Cache> cache) {
        this.cache = cache;
    }

    /**
     * Add a given directory to cache for the project. Before any calculation to
     * be launched, Funz will chekc that it is not already in the cache. In this
     * case, the calculation is not re-launched, and cache results are used.
     *
     * @param dir Directory to add in cache
     */
    public void addCacheDirectory(File dir) {
        if (!dir.isDirectory()) {
            err(dir + " is not a directory.", 2);
            return;
        }
        getCache().add(new Cache(Collections.singletonList(dir), false, true, new Cache.CacheActivityReport() {
            public void report(String s) {
                out(s, 3);
            }
        }));
    }
////////////////////////////////////////////////////////////////////////
    static Funz_v1 Funz_v1;

    private NewClientProvider provider;
    private final Object provider_lock = new Object();
    static volatile int nmOfCompUsed = 0;
    volatile boolean waitForCalculator = true;


    void blacklistComputer(Computer comp, String because) {
        if (askToStop) {
            return;
        }
        Log.out("Blacklisting computer " + comp.host + ":" + comp.port + " for "+Math.round(prj.blacklistTimeout)+" s. because " + because, 4);
        Funz_v1.POOL.blacklistComputer(comp.host, comp.port, prj.blacklistTimeout);
        Alert.showInformation("Blacklisting computer " + comp.host + ":" + comp.port + " for "+Math.round(prj.blacklistTimeout)+" s. because " + because);
    }

    /**
     * @param state the state to set
     */
    private void setState(String state) {
        this.state = StringUtils.rightPad(state, 80);
    }

    class NewClientProvider extends Thread {

        private final Object client_lock = new Object();

        public NewClientProvider() {
            super("NewClientProvider");
            out(provideNewClient_HEAD + "NEW provider", 10);
        }
        volatile boolean waitingNextClient = false;
        volatile ReserverClient nextClient;

        @Override
        public void run() {

            while (waitForCalculator) {
                out(provideNewClient_HEAD + "Waiting client: " + waitingNextClient, 7);
                out(provideNewClient_HEAD + "Waiting calculator: " + waitForCalculator, 7);
                while (waitForCalculator && !waitingNextClient) {
                    try {
                        out(provideNewClient_HEAD + "O", 8);
                        synchronized (client_lock) {
                            out(provideNewClient_HEAD + "o", 8);
                            while (!waitingNextClient) {
                                client_lock.wait();//SLEEP_PERIOD);
                                if (!waitForCalculator) {
                                    out(provideNewClient_HEAD + "no longer waiting calculator. STOPPED.", 7);
                                    return;
                                }
                            }
                        }
                        out(provideNewClient_HEAD + "Got notify: waiting client: " + waitingNextClient, 8);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace(System.err);
                        continue;
                    }
                }
                out(provideNewClient_HEAD + "... client waited.", 7);
                out(provideNewClient_HEAD + "Wait for filling pool:", 7);
                while (waitForCalculator && (Funz_v1.POOL.getComputers().size() <= 0 || (prj.getMaxCalcs() > 0 && getNumOfCompsUsed() >= prj.getMaxCalcs()))) {
                    try {
                        out(provideNewClient_HEAD + "p", 8);
                        out("waitForCalculator:"+waitForCalculator+" POOL.getComputers().size():" + Funz_v1.POOL.getComputers().size() + "<=0 || prj.getMaxCalcs():" + prj.getMaxCalcs() + ">0 && getNumOfCompsUsed():" + getNumOfCompsUsed() + ">=" + prj.getMaxCalcs() + ":prj.getMaxCalcs()", 9);
                        sleep(SLEEP_PERIOD);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace(System.err);
                    }
                }
                out(provideNewClient_HEAD + " Pool filled.", 7);
                try {
                    for (final Computer computer : Funz_v1.POOL.getComputers()) {
                        if ((waitForCalculator && waitingNextClient) && (prj.getMaxCalcs() < 0 || getNumOfCompsUsed() < prj.getMaxCalcs())) {//Add this because following synchronized could add a lag so the limit of computers should be exceeded.
                            //synchronized (computer) {
                            //out(provideNewClient_HEAD + "Trying " + computer.host + ":" + computer.port + " to launch " + prj.getCode(), 6);
                            if (computer.isReady(prj.getCode(), prj.getMinCPU(), prj.getMinMEM(), prj.getMinDISK(), prj.getRegexpCalculators())) {
                                out("  " + computer.host + ":" + computer.port + " ready.", 8);
                                if (!computer.setUser(prj)) {
                                    out("  " + computer.host + ":" + computer.port + " reserved by same provider. Skipping...", 8);
                                    continue;
                                }
                                try {
                                    synchronized (client_lock) {
                                        if (computer.use && computer.getUser() == prj) { //check again not blacklisted
                                            int t = 5; // retry on "Socket creation failed" 5 times
                                            IOException ee = null;
                                            while (t-- >= 0) {
                                                try {
                                                    nextClient = new ReserverClient(computer) {
                                                        @Override
                                                        public void log(String m) {
                                                            super.log("[nextClient] " + m);
                                                        }

                                                        @Override
                                                        public synchronized boolean unreserve() throws IOException {
                                                            log("freeUser");
                                                            computer.freeUser();
                                                            return super.unreserve();
                                                        }
                                                    };
                                                    break;
                                                } catch (IOException e) {
                                                    ee = e;
                                                    Log.err(e, 2);
                                                }
                                            }
                                            if (t <= 0) {
                                                blacklistComputer(computer, "Failed to create ReserverClient 5 times: " + ee);
                                                throw ee;
                                            }
                                        } else {
                                            out("<new ReserverClient> rejected " + computer.host + ":" + computer.port + ": already used or blacklisted", 2);
                                            continue;
                                        }

                                        out("  " + computer.host + ":" + computer.port + " provided.", 7);
                                        //   waitingNextClient = false;

                                        out(provideNewClient_HEAD + "Fire new client found", 7);
                                        //client_lock.notify();//
                                        client_lock.notifyAll();

                                        client_lock.wait();
                                    }
                                    break;
                                } catch (Exception ex) {
                                    err("<new ReserverClient> failed to provide " + computer.host + ":" + computer.port + ":" + ex.getMessage(), 6);
                                    nextClient = null;
                                    computer.freeUser();
                                    continue;
                                }
                            } else {
                                out(computer.toString(), 6);
                                if (computer.getUser() != null) {
                                    out("<!computer.isReady> not available: user=" + computer.getUser(), 6);
                                }
                                if (!computer.use) {
                                    out("<!computer.isReady> not available: use=" + computer.use, 6);
                                }
                                if (!computer.codes.contains(prj.getCode())) {
                                    out("<!computer.isReady> not available: code!=" + prj.getCode(), 6);
                                }
                                if (computer.activity.startsWith(Client.ALREADY_RESERVED) || computer.activity.startsWith(Client.UNAVAILABLE_STATE)) {
                                    out("<!computer.isReady> not available: activity=" + computer.activity, 6);
                                }
                            }

                            //}
                        } else {
                            break;
                        }
                    }
                    if (waitingNextClient) {//means that no free computer found, so wait few seconds that POOL is updated
                        sleep(1000);
                        //synchronized (POOL) {
                        //Funz_v1.POOL.setRefreshing(true, provider_lock, "waitingNextClient");
                        //}
                    }
                } catch (Exception cme) {
                    if (!(cme instanceof IllegalAccessException)) {
                        err(cme, 1);
                    }
                }
            }
            out(provideNewClient_HEAD + "Provider STOPPED.", 10);
        }

        static final String provideNewClient_HEAD = "                                                       [ClientProvider] ";

        ReserverClient provideNewClient() {
            if (askToStop) {
                out(provideNewClient_HEAD + "Asked to stop, so providing null client.", 8);
                return null;
            }
            out(provideNewClient_HEAD + "Providing new client ... ", 8);

            // not necessary in general, but so we are sure that it will provide some calculators...
            Funz_v1.POOL.setRefreshing(true, provider_lock, "provideNewClient");

            synchronized (client_lock) {
                if (waitingNextClient) {
                    err(provideNewClient_HEAD + "!!! Already waiting next client !", 8);
                }
                out(provideNewClient_HEAD + "Waiting new client ... ", 8);

                waitingNextClient = true;
                out(provideNewClient_HEAD + "Fire waiting new client ... ", 8);

                //client_lock.notify();//
                client_lock.notifyAll();

                try {
                    client_lock.wait();

                    out(provideNewClient_HEAD + "Got notify: nextClient: " + nextClient, 8);
                } catch (InterruptedException ex) {
                }
                waitingNextClient = false; //
                //client_lock.notify();//
                client_lock.notifyAll();

                out(provideNewClient_HEAD + "Client returned: " + nextClient, 8);
                if (waitingNextClient) {
                    err(provideNewClient_HEAD + "!!! Still waiting next client !", 8);
                }
                if (nextClient == null) {
                    err(provideNewClient_HEAD + "!!! Returned null client !", 8);
                }
            }
            return nextClient;

        }

        private void pause() {
            //System.err.println("================================ pause() >  waitForCalculator = false;");
            waitForCalculator = false;
            Funz_v1.POOL.setRefreshing(false, provider_lock, "pause provideNewClient");
            synchronized (client_lock) {
                waitingNextClient = false;
                client_lock.notifyAll();
                //System.err.println("================================ pause() >   client_lock.notifyAll();");
            }
        }
    };

    class ReserverClient extends Client implements Case.Reserver {

        boolean killed = false;
        Computer computer;
        Case c;
        boolean log = true;

        public ReserverClient(Computer comp) throws Exception {
            super(comp.host, comp.port);
            this.computer = comp;
        }

        public String getReserverName() {
            return getHost() + ":" + getPort();
        }

        public Object getReserver() {
            return getHost();
        }
        
        @Override
        public synchronized boolean killRunningCode(String secretCode) throws Exception {
            log = true;
            killed = true;
            return super.killRunningCode(secretCode);
        }

        @Override
        public void log(String s) {
            super.log(s);
            if (log) {
                out(c, s, 10);
            }
        }

        @Override
        public synchronized void disconnect() {
            log("Disconnect " + getHost() + ":" + getPort());
            try {
                super.disconnect();
                log("Disconnected " + getHost() + ":" + getPort());
            } catch (Exception ex) {
                err(c, "Failed to disconnect " + getHost() + ":" + getPort() + ": " + getReason(), 5);
                err(c, ex, 2);
            }
        }

        @Override
        public void force_disconnect() {
            log = true;
            log("Force disconnect " + getHost() + ":" + getPort());
            try {
                super.force_disconnect();
                log("Force disconnected " + getHost() + ":" + getPort());
            } catch (Exception ex) {
                err(c, "Failed to force disconnect " + getHost() + ":" + getPort() + ": " + getReason(), 5);
                err(c, ex, 2);
            }
        }
        Object line_lock = new Object();

        @Override
        public synchronized boolean unreserve() throws IOException {
            log("Unreserve " + getHost() + ":" + getPort());
            try {
                boolean ok = false;
                if (isReserved()) {
                    ok = super.unreserve();
                }
                log("Unreserved " + getHost() + ":" + getPort());
                return ok;
            } catch (Exception ex) {
                err(c, "Failed to unreserve " + getHost() + ":" + getPort() + ": " + getReason(), 5);
                err(c, ex, 2);
                return false;
            }
        }
    }
    final Object nmOfComp_lock = new Object();

    void incNumOfComps() {
        synchronized (nmOfComp_lock) {
            out("Computer used: " + nmOfCompUsed + "++", 11);
            nmOfCompUsed++;
        }
    }

    void decNumOfComps() {
        synchronized (nmOfComp_lock) {
            out("Computer used: " + nmOfCompUsed + "--", 11);
            nmOfCompUsed--;
        }
    }

    int getNumOfCompsUsed() {
        synchronized (nmOfComp_lock) {
            out("Computer used: " + nmOfCompUsed, 11);
            return nmOfCompUsed;
        }
    }
    final Map<Case, Thread> running_cleaner = new HashMap<>();

    void beforeRunCases() {
        if (provider == null || provider.getState() == Thread.State.TERMINATED) {
            provider = new NewClientProvider();
        }

        waitForCalculator = true;
        if (!provider.waitingNextClient && !provider.isAlive()) {
            provider.start();
        }
    }

    Map<String, String> info_histories = new HashMap<>();

    String getInfoHistory(Case c) {
        return info_histories.get(c.getName());
    }

    void addInfoHistory(Case c, String info) {
        if (!info_histories.containsKey(c.getName())) {
            info_histories.put(c.getName(), "## " + c.getName());
        }
        info_histories.replace(c.getName(), info_histories.get(c.getName()) + "\n  [" + LogTicToc.HMS() + "]  > " + info);
    }

    public boolean killCase(final Case c) {
        if (c == null || !running_cleaner.containsKey(c)) {
            return false;
        }
        Thread killer = running_cleaner.get(c);
        killer.start();
        /*try {
         killer.join();
         } catch (InterruptedException ex) {
         return false;
         }*/
        running_cleaner.remove(c);
        return true;
    }

    public boolean runCase(final Case c) {
        //LogUtils.tic("runCase " + c.getName());
        //startCase(c);

        File[] files = null;
        try {
            //LogUtils.tic("Instanciating " + c.getName());
            addInfoHistory(c, "Instanciating files");

            files = prj.prepareCaseFiles(c);
            if (files != null) {
                addInfoHistory(c, "Instanciated files" + Arrays.deepToString(files));
            } else {
                addInfoHistory(c, "No file instanciated.");
                throw new IOException(c.getName() + ": " + "No file instanciated.");
            }
            //LogUtils.toc("Instanciating " + c.getName());
        } catch (IOException ex) {
            addInfoHistory(c, "Failed to instanciate files: " + ex.getLocalizedMessage());
            errorCase(ex, c);
            return false;
        } catch (Exception ex) {
            addInfoHistory(c, "Failed to compute instanciation of files: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            failedCase(ex, c);
            return false;
        }

        //LogUtils.tic("getCache " + c.getName());
        if (prj.useCache) {
            for (Cache _c : getCache()) {
                addInfoHistory(c, "Searching in cache: " + getCache());
                File dir = files[0].getParentFile().getParentFile();
                File matching_output = _c.getMatchingOutputDirinCache(new File(dir + File.separator + Constants.INPUT_DIR), prj.getCode());
                if (matching_output != null) {
                    addInfoHistory(c, "Found in " + matching_output.getAbsolutePath());

                    File outdir = new File(dir + File.separator + Constants.OUTPUT_DIR);
                    try {
                        startCase(c);

                        addInfoHistory(c, "Copying cache ...");

                        Disk.copyDir(matching_output, outdir);
                        File parent_matching = matching_output.getParentFile();
                        File parent_outdir = outdir.getParentFile();
                        File oldinfo = new File(parent_matching, "old." + Case.FILE_INFO);
                        if (oldinfo.exists()) {
                            Disk.copyFile(oldinfo, new File(parent_outdir, "old." + Case.FILE_INFO));
                        } else {
                            File previousinfo = new File(parent_matching, Case.FILE_INFO);
                            assert (previousinfo.exists()) : "No " + Case.FILE_INFO + " found in " + parent_matching.getAbsolutePath();
                            Disk.copyFile(previousinfo, new File(parent_outdir, "old." + Case.FILE_INFO));
                        }
                        addInfoHistory(c, "Cache copied.");

                        parseResult(c, false);

                        endCase(c);

                        return true;
                    } catch (IOException e) {
                        errorCase(e, c);
                    } catch (Exception e) {
                        failedCase(e, c);
                    }
                } else {
                    addInfoHistory(c, "Cache not found.");
                }
            }
        }
        //LogUtils.toc("getCache " + c.getName());

        //LogUtils.tic("Searching " + c.getName());
        ReserverClient client = null;
        addInfoHistory(c, "Searching client ... ");
        StringBuffer sc = new StringBuffer(), ip = new StringBuffer();
        try {
            while (client == null && !askToStop) {
                //LogUtils.tic("synchronized (provider_lock) { " + c.getName());
                synchronized (provider_lock) {
                    if (!waitForCalculator) {
                        addInfoHistory(c, "Client was reserved, but asked to stop now.");
                        throw new Exception(c.getName() + ": " + "Client was reserved, but asked to stop now.");
                    }
                    addInfoHistory(c, "Got provider...");
                    //LogUtils.tic("provideNewClient " + c.getName());
                    client = provider.provideNewClient();
                    //LogUtils.toc("provideNewClient " + c.getName());
                }
                //LogUtils.toc("synchronized (provider_lock) { " + c.getName());

                //LogUtils.tic("reserve " + c.getName());
                if (client != null) {
                    addInfoHistory(c, "Got client from provider...");
                    if (!client.reserveTimeOut(10000, prj, ip, sc)) {
                        addInfoHistory(c, "Failed to reserve project: " + client.getReason());
                        blacklistComputer(client.computer, "Failed to reserve project: " + client.getReason());
                        client.disconnect();
                        client = null;
                    } else {
                        addInfoHistory(c, "reserved client ip=" + ip + " sc=" + sc);
                    }
                } else {
                    addInfoHistory(c, "Got NO client from provider.");
                }
                //LogUtils.toc("reserve " + c.getName());
            }
        } catch (Exception ex) {
            addInfoHistory(c, ex.getLocalizedMessage());
            if (client != null) {
                client.disconnect();
            }
            client = null;
        }
        //LogUtils.toc("Searching " + c.getName());

        boolean incNumOfCompsDone = false;
        try {
            if (client == null) {
                addInfoHistory(c, "Failed to get a client");
                throw new IOException(c.getName() + ": " + "Failed to get a client");
            } else {
                addInfoHistory(c, "Has a client.");
            }

            client.c = c;

            //LogUtils.tic("startCase " + c.getName());
            startCase(c);
            //LogUtils.toc("startCase " + c.getName());

            addInfoHistory(c, "Trying to reserve client " + client);
            if (!c.reserve(client)) {
                addInfoHistory(c, "Failed to reserve client: " + client.getReason() + " " + client.getReserverName() + " " + client.getReserver());
                blacklistComputer(client.computer, "Failed to reserve client: " + client.getReason() + " " + client.getReserverName() + " " + client.getReserver());
                client.disconnect();
                throw new IOException(c.getName() + ": " + "Failed to reserve client: " + client.getReason() + " " + client.getReserverName() + " " + client.getReserver());
            }
            addInfoHistory(c, "Using client " + ((ReserverClient) c.getReserver()).getHost() + ":" + ((ReserverClient) c.getReserver()).getPort());

            //LogUtils.tic("incNumOfComps " + c.getName());
            incNumOfComps();
            incNumOfCompsDone = true;
            //LogUtils.toc("incNumOfComps " + c.getName());

            //LogUtils.tic("newCase " + c);
            Map infos = prj.getCaseParameters(c);
            infos.put(ARCHIVE_FILTER, prj.getArchiveFilter());
            if (!client.newCase(infos)) {
                addInfoHistory(c, "Failed to instanciate new remote case: " + client.getReason() + " " + client.getReserverName() + "/" + client.getReserver());
                blacklistComputer(client.computer, "Failed to instanciate new remote case: " + client.getReason() + " " + client.getReserverName() + "/" + client.getReserver());
                client.disconnect();
                throw new IOException(c.getName() + ": " + "Failed to instanciate new remote case: " + client.getReason() + " " + client.getReserverName() + "/" + client.getReserver());
            }
            //LogUtils.tic("newCase " + c.getName());

            //LogUtils.tic("send files " + c.getName());
            for (File file : files) {
                if (file.getName().endsWith("_md5")) {
                    String src_path = file.getAbsolutePath().substring(0, file.getAbsolutePath().length() - 4).replace(new File(prj.getCaseTmpDir(c), Constants.INPUT_DIR).getAbsolutePath(), prj.getFilesDirectory().getAbsolutePath());
                    if (!client.putFile(new File(src_path), prj.getFilesDirectory())) {
                        addInfoHistory(c, "Failed to put file " + file + " : " + client.getReason());
                        blacklistComputer(client.computer, "Failed to put file " + file + " : " + client.getReason());
                        client.disconnect();
                        throw new IOException(c.getName() + ": " + "Failed to put file " + file + "  " + client.getReason());
                    }
                } else {
                    if (!client.putFile(file, new File(prj.getCaseTmpDir(c), Constants.INPUT_DIR))) {
                        addInfoHistory(c, "Failed to put file " + file + "  " + client.getReason());
                        blacklistComputer(client.computer, "Failed to put file " + file + "  " + client.getReason());
                        client.disconnect();
                        throw new IOException(c.getName() + ": " + "Failed to put file " + file + "  " + client.getReason());
                    }
                }
            }
            addInfoHistory(c, "Sent files " + Arrays.deepToString(files));
            //LogUtils.toc("send files " + c.getName());

            //LogUtils.tic("kill_client " + c.getName());
            final ReserverClient kill_client = new ReserverClient(client.computer) {
                public void log(String m) {
                    super.log(m);
                    if (c != null) {
                        addInfoHistory(c, "kill:" + m);
                    }
                }
            };
            kill_client.c = c;
            final ReserverClient final_client = client;
            kill_client.executing = true;
            final String secretCode = sc.toString();
            Thread killer = new Thread(new Runnable() {
                public void run() {
                    try {
                        addInfoHistory(c, "Killing code " + prj.getCode() + " (client " + kill_client.getHost() + ":" + kill_client.getPort() + ") (secret code:" + secretCode.toString() + ")");
                        kill_client.killed = kill_client.killRunningCode(secretCode.toString());
                        Alert.showInformation("Killed code " + prj.getCode() + " (client " + kill_client.getHost() + ":" + kill_client.getPort() + ")");
                        if (!kill_client.killed) {
                            throw new Exception("Not killed.");
                        }
                        addInfoHistory(c, "Killed code " + prj.getCode() + " (client " + kill_client.getHost() + ":" + kill_client.getPort() + ")");
                    } catch (Exception ex) {
                        addInfoHistory(c, "Failed to kill code of client " + kill_client.getHost() + ":" + kill_client.getPort() + ":" + ex.getLocalizedMessage());
                    } finally {
                        kill_client.disconnect();
                    }
                    try {
                        addInfoHistory(c, "Disconnecting (client " + final_client.getHost() + ":" + final_client.getPort() + ")");
                        //synchronized (final_client) {
                        final_client.force_disconnect();// to bypass unreserve
                        final_client.killed = true;
                        //}
                        addInfoHistory(c, "Disconnected (client " + final_client.getHost() + ":" + final_client.getPort() + ")");
                    } catch (Exception ex) {
                        addInfoHistory(c, "Failed to disconnect client " + final_client.getHost() + ":" + final_client.getPort() + ":" + ex.getLocalizedMessage());
                    }
                }
            }, c.getName() + ": " + "Kill client " + kill_client.getHost() + ":" + kill_client.getPort());
            synchronized (running_cleaner) {
                running_cleaner.put(c, killer);
            }
            //LogUtils.toc("kill_client " + c.getName());

            //LogUtils.tic("execute " + c.getName());
            c.setState(Case.STATE_RUNNING);
            final StringBuffer inline_informations = new StringBuffer();
            addInfoHistory(c, "Executing " + prj.getCode());
            if (!client.execute(prj.getCode(), new Client.DataListener() {
                public void informationLineArrived(final String str) {
                    addInfoHistory(c, "info: " + str);
                    c.setInformation(str);
                    inline_informations.append(str);
                    inline_informations.append('\n');
                    addInfoHistory(c, "info: " + str + " saved.");
                }
            })) {
                addInfoHistory(c, "Failed to execute on " + client.getHost() + ": " + (kill_client.killed ? "Killed" : client.getReason()));
                if (!kill_client.killed) {
                    blacklistComputer(client.computer, "Failed to execute on " + client.getHost() + ": " + (kill_client.killed ? "Killed" : client.getReason()));
                }
                transferOutput(client, c);
                client.disconnect();
                throw new IOException(c.getName() + ": " + "Failed to execute on " + client.getHost() + ": " + (kill_client.killed ? "Killed" : client.getReason()));
            } else {
                synchronized (running_cleaner) {
                    running_cleaner.remove(c);
                }
            }
            //LogUtils.toc("execute " + c.getName());

            if (kill_client.killed) {
                throw new IOException(c.getName() + ": " + "Code killed.");
            }

            addInfoHistory(c, "Execution done.");

            transferOutput(client, c);

            //LogUtils.tic("parseResult " + c.getName());
            parseResult(c, true);
            //LogUtils.toc("parseResult " + c.getName());

            endCase(c);
        } catch (IOException e) {
            errorCase(e, c);
        } catch (Exception e) {
            failedCase(e, c);
        } finally {
            if (incNumOfCompsDone) {
                decNumOfComps();
            }

            if (client != null) {
                try {
                    client.unreserve();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                client.disconnect(); // maybe release this client aso procvide new client may re-use it for another case... instead of reserve/unreserve all clients independantly ?
            }

            c.reserve(null);
        }
        //LogUtils.toc("runCase " + c.getName());

        return !c.isFailed();
    }

    void afterRunCases() {
        if (provider != null) {
            provider.pause();
        }
    }

    public void setRefreshingPeriod(long millis) {
        SLEEP_PERIOD = millis;
    }

////////////////////////////////////////////////////////////////////////
    public abstract void out(String string, int i);

    public abstract void err(String msg, int i);

    public abstract void err(Exception ex, int i);

    void out(Case c, String msg, int l) {
        out(c == null ? "Case ??? "+msg : (repeat(c.getIndex() + 1, "|", "        ")) + ": " + msg, l);
    }

    void err(Case c, String msg, int l) {
        err(c == null ? "Case ??? "+msg : (repeat(c.getIndex() + 1, "|", "        ")) + ": " + msg, l);
    }

    void err(Case c, Exception ex, int l) {
        err(c, ex.getMessage(), l);
    }

    void transferOutput(ReserverClient client, final Case c) throws IOException {
        //LogUtils.tic("archiving " + c.getName());
        if (!client.archiveResults()) {
            addInfoHistory(c, "Failed to archive results: " + client.getReason());
            blacklistComputer(client.computer, "Failed to archive results: " + client.getReason());
            client.disconnect();
            throw new IOException(c.getName() + ": " + "Failed to archive results: " + client.getReason());
        }
        addInfoHistory(c, "Remote archiving done.");

        File dir = prj.getCaseTmpDir(c);
        File outdir = new File(dir + File.separator + Constants.OUTPUT_DIR);
        if (outdir.isDirectory()) {
            out("Output directory " + outdir + " already exists", 2);
        }
        if (!outdir.mkdirs()) {
            out("Output directory " + outdir + " was not created", 2);
        }
        if (!outdir.exists()) {
            addInfoHistory(c, "Failed to create output directory: " + outdir);
            throw new IOException(c.getName() + ": " + "Failed to create output directory: " + outdir);
        }
        //LogUtils.toc("archiving " + c.getName());

        //LogUtils.tic("Transfer " + c.getName());
        if (!client.transferArchive(dir, null)) {
            addInfoHistory(c, "Failed to transfer: " + client.getReason());
            blacklistComputer(client.computer, "Failed to transfer: " + client.getReason());
            client.disconnect();
            throw new IOException(c.getName() + ": " + "Failed to transfer: " + client.getReason());
        }
        addInfoHistory(c, "Transfer done.");
        //LogUtils.toc("Transfer " + c.getName());

        //LogUtils.tic("unzip " + c.getName());
        File archive = new File(dir + File.separator + Protocol.ARCHIVE_FILE);
        FileInputStream is = null;
        try {
            is = new FileInputStream(archive);
        } catch (Exception e) {
            addInfoHistory(c, "Failed to unzip: " + e.getMessage());
            throw new IOException(c.getName() + ": " + "Failed to unzip: " + e.getMessage());
        }

        ZipTool.unzipWithinDirectory(is, outdir, new ZipTool.ProgressObserver() {
            public void nextEntry(String name) {
                addInfoHistory(c, name);
            }
        });
        is.close();
        if (!archive.delete()) {
            err("Could not delete temporary archive file " + archive, 1);
        }
        addInfoHistory(c, "Local unzip done.");
        //LogUtils.toc("unzip " + c.getName());
    }

    void parseResult(Case c, boolean doSummary) throws Exception {
        addInfoHistory(c, "Parsing results.");
        out("Parsing results of " + c.getName() + ".", 1);
        //LogTicToc.tic("IO");

        //LogTicToc.tic("inputValues");
        Map<String, Object> inputValues = (c.getInputValues() == null ? ProjectController.getCaseInputs(prj, c.getIndex()) : c.getInputValues());
        //c.setInputValues(inputValues);
        out("Set case " + c.getName() + " I", 5);
        //LogTicToc.toc("inputValues");

        //LogTicToc.tic("outputValues");
        Map<String, Object> outputValues = (c.getOutputValues() == null ? ProjectController.getCaseOutputs(prj, c.getIndex(), true) : c.getOutputValues());
        //System.err.println("outputValues "+        outputValues);
        c.setOutputValues(outputValues);
        out("Set case " + c.getName() + " O", 5);
        //LogTicToc.toc("outputValues");

        Map<String, Object> intermediateValues = (c.getIntermediateValues() == null ? ProjectController.getCaseIntermediates(prj, c.getIndex()) : c.getIntermediateValues());
        c.setIntermediateValues(intermediateValues);

        out("Set case " + c.getName() + " IO", 5);
        Map<String, Object> result = new HashMap<>();

        if (inputValues != null) {
            for (String var : inputValues.keySet()) {
                result.put(var, inputValues.get(var));
            }
        }
        if (intermediateValues != null) {
            for (String var : intermediateValues.keySet()) {
                result.put(var, intermediateValues.get(var));
            }
        }
        if (outputValues != null) {
            for (String res : outputValues.keySet()) {
                Object o = outputValues.get(res);
                if (o != null) {
                    result.put(res, outputValues.get(res));
                } else {
                    result.put(res, null);
                }
            }
        }

        out("Set IO results of " + c.getName(), 5);
        //LogTicToc.toc("IO");

        //LogTicToc.tic("OutputFunctionExpression");
        for (OutputFunctionExpression e : prj.getOutputFunctionsList()) {
            if (!result.containsKey(e.toNiceSymbolicString())) {
                try {
                    Object parametersvalues = c.doEval(e);
                    result.put(e.toNiceSymbolicString(), parametersvalues);//e.toNiceNumericString(parametersvalues));
                } catch (Exception ex) {
                    err(ex, 1);
                }
            }
        }
        out("Results of " + c.getName() + " parsed", 1);
        //LogTicToc.toc("OutputFunctionExpression");

        c.setInformation("Results parsed: " + result);

        c.setResult(result);

        if (doSummary) {
            c.writeInfoFile(new File(prj.getCaseTmpDir(c) + File.separator + Case.FILE_INFO));
        }

        c.setState(Case.STATE_OVER);
    }

    void startCase(Case c) {
        c.setInformation("Starting ...");
        out("Starting case " + c.getName() + ".", 1);

        c.setState(Case.STATE_INTACT);
        c.setStart(System.currentTimeMillis());

        c.incTriesDone();
    }

    void endCase(Case c) {
        c.setInformation("Ending case " + c.getName() + ".");
        out("Ending case " + c.getName() + ".", 1);

        c.setEnd(System.currentTimeMillis());
    }

    void errorCase(Exception e, Case c) { // typically happens when no space left on device, or stream cut, or ...
        if (c.getTriesDone() > prj.getMaxRetry()) {
            failedCase(e, c);
            return;
        }

        addInfoHistory(c, "Error: " + e.getMessage());
        out("Error for case " + c.getName() + ": " + e, 1);

        try {
            Disk.removeDir(new File(prj.getCaseTmpDir(c), Constants.INPUT_DIR));
        } catch (IOException ee) {
            err(ee, 2);
            err("removeDir: " + getInfoHistory(c), 3);
        }

        c.setInformation("Error: " + e.getMessage());

        c.setState(Case.STATE_ERROR);
    }

    void failedCase(Exception e, Case c) { // more serious failure than just an "errorCase"
        //e.printStackTrace();

        addInfoHistory(c, "Failed: " + e.getMessage());
        out("Failed running case " + c.getName() + ": " + e, 1);

        try {
            parseResult(c, true);
        } catch (Exception ex) {
            err("parseResult: " + ex.getMessage() + "\n" + getInfoHistory(c), 3);
        }

        c.setInformation(info_histories.get(c.getName()));

        c.setState(Case.STATE_FAILED);
    }

    class RunCase extends Thread {

        Case c;

        public RunCase(final Case c) {
            super(c.getName() + ": " + "RunCase " + c.getIndex() + " project " + prj.getName());
            out("Created new RunCase thread" + c.getName(), 10);
            this.c = c;
        }

        @Override
        public void run() {
            c.setInformation("Case not started");
            //System.err.println(c.getName()+" starting... "+!askToStop+" "+ waitForCalculator +" "+ !c.isOver() +" "+ (tries <= prj.getMaxRetries()));
            boolean success = false;
            while (!askToStop /*&& waitForCalculator*/ && !c.hasRun() /*&& tries <= prj.getMaxRetries()*/) {
                try {
                    success = runCase(c);
                } catch (Exception e) {
                    err(e, 0);
                }

                if (!success) {
                    Alert.showInformation(c.getName() + " Failed run try (" + c.getTriesDone() + "/" + (prj.getMaxRetry() + 1) + ")");
                    err(c.getName() + " Failed run try (" + c.getTriesDone() + "/" + (prj.getMaxRetry() + 1) + ")", 2);
                }
            }

            if (success) {
                if(c.isError()) {
                    c.setInformation("Run error.");
                } else {
                    c.setInformation("Run succeded.");
                }
            } else {
                if (c.getTriesDone() > prj.getMaxRetry()) {
                    c.setInformation("Run failed: too much retry (" + c.getStatusInformation() + ")");
                    Alert.showError(c.getName() + " Run failed: too much retry (" + c.getStatusInformation() + ")");
                } else if (c.isFailed()) {
                    c.setInformation("Run failed: case has failed (" + c.getStatusInformation() + ")");
                    Alert.showError(c.getName() + " Run failed: case has failed (" + c.getStatusInformation() + ")");
                } else if (askToStop) {
                    c.setInformation("Run failed: ask to stop");
                    Alert.showError(c.getName() + " Run failed: ask to stop");
                } else if (!waitForCalculator) {
                    c.setInformation("Run failed: not waiting for calculator");
                    Alert.showError(c.getName() + " Run failed: not waiting for calculator");
                } else if (c.isOver()) {
                    c.setInformation("Run failed: case was over");
                    Alert.showError(c.getName() + " Run failed: case was over");
                }
            }

            if (askToStop) {
                c.setState(Case.STATE_INTACT);
            }

            c.modified(Case.MODIFIED_STATE, "run");

            c.setObserver(null);

            c = null;
        }
    }

    class CloneCase extends Thread implements Case.Observer {

        Case clone, toclone;
        Observer transfer_observer;

        public CloneCase(final Case clone, final Case toclone) {
            super("CloneCase " + clone.getIndex() + " project " + prj.getName());
            this.clone = clone;
            this.toclone = toclone;
            transfer_observer = toclone.getObserver();
            toclone.setObserver(this);
        }

        @Override
        public void run() {
            while (!askToStop && !(toclone.isOver()/* && results[toclone.getIndex()] != null)*/)) {
                try {
                    synchronized (this) {
                        wait();
                    }
                } catch (InterruptedException ex) {
                }
            }
            out("Cloning results of case " + toclone.getName(), 1);

            if (!askToStop) {
                boolean succeded = true;

                try {
                    parseResult(toclone, false);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    succeded = false;
                }

                if (succeded) {
                    clone.setOutputValues(toclone.getOutputValues());
                    clone.setResult(toclone.getResult());
                    clone.setState(toclone.getState());
                    clone.setInformation("@" + toclone.getName());
                } else {
                    clone.setState(Case.STATE_FAILED);
                }
            }

            toclone.setObserver(null);
            clone.setObserver(null);

            toclone = null;
            clone = null;
        }

        @Override
        public void caseModified(int index, int what) {
            if (toclone != null) {
                if (index == toclone.getIndex() && what == Case.MODIFIED_STATE) {
                    synchronized (this) {
                        notify();
                    }
                }
            }
            if (transfer_observer != null) {
                transfer_observer.caseModified(index, what);
            }
            observer.caseModified(index, what);
            onCaseModified(index, what);
        }
    }

    public void onCaseModified(int index, int what) {
        if (what == Case.MODIFIED_CALC) {
            out("Modified calculator for case " + index, 2);
        } else if (what == Case.MODIFIED_STATE) {
            out("Modified state for case " + index, 2);
        } else if (what == Case.MODIFIED_INFO) {
            out("Modified information for case " + index, 2);
        }
    }

    public void caseModified(int index, int what) {
        if (what == Case.MODIFIED_STATE) {
            out("Modified state for case " + index + " ... synchronizing", 8);
            synchronized (this) {
                notify();
            }
            out("Modified state for case " + index + " notified.", 8);
        }
        onCaseModified(index, what);
    }

    public Map<String, String[]> getResultsStringArrayMap() {
        return MapArrayToMapStringArray(getResultsArrayMap());
    }

    public Map<String, Object[]> getResultsArrayMap() {
        if ((merged_results == null || merged_results.isEmpty()) && torun != null) {
            return merge(torun);
        }
        if (merged_results == null) {
            return new HashMap<>();
        }
        return merged_results;
    }

    /**
     * Asks for all calculation point to stop.
     *
     * @return if calculation process stopped correctly
     */
    public boolean stopBatch() {
        if (stopRun != null) {
            stopRun.start();
            try {
                stopRun.join();
            } catch (InterruptedException ex) {
                err(ex, 2);
                return false;
            }
        }

        try {
            setArchiveDirectory(archiveDirectory);
        } catch (IOException ex) {
            err(ex, 0);
        }

        return true;
    }

    public List<Case> getSelectedCases() {
        CaseList cases = new CaseList();
        if (prj!=null && prj.getCases()!=null)
        for (Case c : prj.getCases()) {
            if (c.isSelected()) {
                cases.add(c);
            }
        }
        return cases;
    }

    public List<Case> getPendingCases() {
        List<Case> theseCases = new LinkedList<>();
        for (Case t : getSelectedCases()) {
            if (!t.hasRun()) {
                theseCases.add(t);
            }
        }
        /*theseCases.removeIf(new Predicate<Case>() {
         public boolean test(Case t) {
         return t.hasRun();
         }
         });*/
        return theseCases;
    }

    public List<Case> getFinishedCases() {
        List<Case> theseCases = new LinkedList<>();
        for (Case t : getSelectedCases()) {
            if (t.hasRun()) {
                theseCases.add(t);
            }
        }
        /*theseCases.removeIf(new Predicate<Case>() {
         public boolean test(Case t) {
         return !t.hasRun();
         }
         });*/
        return theseCases;
    }

    private boolean hasPendingCases() {
        for (Case c : getPendingCases()) {
            if (!c.hasRun()) {
                return true;
            }
        }
        return false;
    }

    public List<Case> torun;

    public boolean runBatch() throws Exception {
        //LogUtils.tic("runBatch");

        if (Funz_v1.POOL == null) {
            err("Cannot run calculation batch because no POOL instanciated.", 0);
            setState(BATCH_ERROR);
            return false;
        }

        askToStop = false;
        merged_results = new HashMap<>();

        setState(BATCH_STARTING);
        
        //LogUtils.tic("checkVariablesAreValid");
        String cv = prj.checkVariablesAreValid();
        //LogUtils.toc("checkVariablesAreValid");

        if (cv != null) {
            err("Input variables are not correclty set: " + cv, 1);
            setState(BATCH_ERROR);
            //LogUtils.tic("cv != null");
            merged_results.putAll(mergeStringArrayMap(newMap("error", "Input variables are not correclty set: " + cv)));
            //LogUtils.toc("cv != null");
            Alert.showError("Input variables are not correclty set: " + cv);
            throw new Exception("Input variables are not correclty set: " + cv);
        }
        //LogUtils.tic("checkOutputFunctionIsValid");

        cv = prj.checkOutputFunctionIsValid();
        //LogUtils.toc("checkOutputFunctionIsValid");

        if (cv != null) {
            err("Output expressions are not correclty set: " + cv, 1);
            setState(BATCH_ERROR);
            //LogUtils.tic("cv != null 2");
            merged_results.putAll(mergeStringArrayMap(newMap("error", "Output expressions are not correclty set: " + cv)));
            //LogUtils.toc("cv != null 2");
            Alert.showError("Output expressions are not correclty set: " + cv);
            throw new Exception("Output expressions are not correclty set: " + cv);
        }

        int waited_time = 0;
        if (prj.waitingTimeout>0) {//otherwise ignore grid checking
            Funz_v1.POOL.setRefreshing(true, this, "Searching for code "+prj.getCode());
            while (waited_time< prj.waitingTimeout*1000 && !Funz_v1.POOL.getCodes().contains(prj.getCode())) {
                setState(BATCH_WAITINGCOMPUTERS+StringUtils.repeat(".",waited_time/1000));
                //synchronized (this) {
                sleep(SLEEP_PERIOD);
                //}
                waited_time += SLEEP_PERIOD;
            }
            if (!Funz_v1.POOL.getCodes().contains(prj.getCode())) {
                setState(BATCH_ERROR+": '"+ prj.getCode() + "' is missing in Funz grid.");
                Alert.showError("Code '" + prj.getCode() + "' is missing in Funz grid (only "+Funz_v1.POOL.getCodes()+" are available).");
                return false;
            } else if (waited_time>0) {
                setState(BATCH_STARTING+": '"+ prj.getCode() + "' was found in Funz grid.");
                Alert.showInformation("Code '"+prj.getCode()+"' was found in Funz grid.");
            }
        } else 
            Alert.showInformation("Bypass Funz grid checking for code '"+prj.getCode()+"'");
        
        try {
            runCases.clear();

            //LogUtils.tic("getPendingCases");
            torun = new ArrayList<>(getPendingCases());
            List<CloneCase> cloneCases = new ArrayList<CloneCase>();
            //LogUtils.toc("getPendingCases");

            int numToRun = torun.size();
            for (int i = 0; i < numToRun; i++) {
                final Case c = torun.get(i);
                c.setObserver(observer);
                boolean already_launched = false;
                boolean cloned = false;
                for (int j = 0; j < getFinishedCases().size(); j++) { //for old cases
                    final Case cprev = getFinishedCases().get(j);
                    //LogUtils.tic("synchronized (cprev) ");
                    synchronized (cprev) {
                        if (c.getName().equals(cprev.getName())) {
                            already_launched = true;
                            out("Case " + c.getName() + " already finished...", 1);
                            cloneCases.add(new CloneCase(c, cprev));
                            cloned = true;
                            break;
                        }
                    }
                    //LogUtils.toc("synchronized (cprev) ");
                }
                if (!already_launched && !cloned) {
                    for (int j = 0; j < i; j++) { // when same case is just asked before.
                        final Case cprev = getPendingCases().get(j);
                        //LogUtils.tic("synchronized (cprev) ");
                        synchronized (cprev) {
                            if (c.getName().equals(cprev.getName())) {
                                already_launched = true;
                                out("Case " + c.getName() + " already launched...", 1);
                                cloneCases.add(new CloneCase(c, cprev));
                                cloned = true;
                                break;
                            }
                        }
                        //LogUtils.toc("synchronized (cprev) ");
                    }
                }
                if (!already_launched && !cloned) {
                    //LogUtils.tic("new RunCase");
                    RunCase rc = new RunCase(c);
                    runCases.add(rc);
                    //LogUtils.toc("new RunCase");
                    //rc.start(); Do NOT start all threads at the same time...
                }
            }

            for(CloneCase cloneCase: cloneCases) {
                cloneCase.start();
            }

            if (numToRun > 0) {
                stopRun = new Thread(new Runnable() {
                    public void run() {
                        out("Shutdown hook called.", 3);
                        shutdown();
                    }
                }, "BatchRunStop");

                //LogUtils.tic("addShutdownHook");
                Runtime.getRuntime().addShutdownHook(stopRun);
                //LogUtils.toc("addShutdownHook");

                //LogUtils.tic("beforeRunCases");
                beforeRunCases();
                //LogUtils.toc("beforeRunCases");

                setState(BATCH_RUNNING);
                // let's start only some cases (to limit concurrent RunCase threads)
                for (int i = 0; i < prj.getMaxCalcs(); i++) {
                    if (i < runCases.size()) {
                        out("Starting case " + runCases.get(i).c.getName(), 3);
                        //LogUtils.tic("runCases.get(i).start()");
                        runCases.get(i).start();
                        //LogUtils.toc("runCases.get(i).start()");
                    }
                }

                int f = 0;
                String state_value = "";
                String state_name = "Running";
                if (Funz.getVerbosity() > 3) {
                    state_name = state_name + " (";
                    for (int i = 0; i < Case.STATE_STRINGS.length; i++) {
                        int ii = Case.STATE_ORDER[i];
                        state_name = state_name + (i > 0 ? "/" : "") + Case.STATE_STRINGS[ii].substring(0, 1);
                    }
                    state_name = state_name + ")";
                }
                int[][] states = new int[getSelectedCases().size()][Case.STATE_STRINGS.length];
                while (f < numToRun && !askToStop) {
                    try {
                        out(f + "<" + numToRun + " ... " + waitForCalculator + " " + (provider == null ? "null provider" : "provider.waitingNextClient=" + provider.waitingNextClient), 8);
                        synchronized (this) {
                            wait(SLEEP_PERIOD);
                        }
                    } catch (InterruptedException ex) {
                        err("trap InterruptedException: " + ex.getMessage(), 0);
                    }
                    //out_noln(" ? ", 5);
                    int f_old = f;
                    //LogUtils.tic("filled");
                    f = filled(torun);
                    //LogUtils.toc("filled");

                    // let's start only some cases (to limit concurrent RunCase threads)
                    for (int i = 0; i < /*f - f_old*/ Math.min(prj.getMaxCalcs(), numToRun - f); i++) {
                        for (int j = 0; j < runCases.size(); j++) {
                            //LogUtils.tic("runCases.get(j)");
                            RunCase rc = runCases.get(j);
                            //LogUtils.toc("runCases.get(j)");
                            if (!rc.isAlive()) {
                                if (rc.c != null && rc.c.getState() == Case.STATE_INTACT) {
                                    out("Starting case " + rc.c.getName(), 3);
                                    //LogUtils.tic("rc.start()");
                                    rc.start();
                                    //LogUtils.toc("rc.start()");
                                    //System.err.println("+");
                                    break;
                                }//else System.err.println(rc.c.getStatusInformation());
                            }
                        }
                    }

                    //out("Finished " + f + "/" + getCases().size() + " cases.", 2);
                    if (Funz.getVerbosity() > 3) {
                        //out("Cases status:", 3);
                        List<Case> selectedCases = getSelectedCases();
                        for (int i = 0; i < selectedCases.size(); i++) {
                            Case c = selectedCases.get(i);
                            if (states[i][c.getState()] == 0) {
                                out("Case " + c.getName() + " : " + c.getStateString(), 3);
                                states[i] = new int[Case.STATE_STRINGS.length];
                                states[i][c.getState()] = 1;
                            }
                        }
                        int[] sumstates = IntegerArray.sum(states);

                        String new_state_value = "";
                        for (int i = 0; i < Case.STATE_STRINGS.length; i++) {
                            int ii = Case.STATE_ORDER[i];
                            new_state_value = new_state_value + (i > 0 ? "/" : "") + sumstates[ii];
                        }
                        if (!new_state_value.equals(state_value)) {
                            state_value = new_state_value;
                            setState(state_name + ":\t" + state_value);
                        }
                    } else {
                        String new_state_value = f + "/" + numToRun;
                        if (!new_state_value.equals(state_value)) {
                            state_value = new_state_value;
                            setState(state_name + ":\t" + state_value);
                        }
                    }
                }
                if (askToStop) {
                    throw new Exception("Asked batch to stop");
                }

                //LogUtils.tic("afterRunCases");
                afterRunCases();
                //LogUtils.toc("afterRunCases");

                setState(BATCH_ARCHIVING);

                if (stopRun != null) {
                    //LogUtils.tic("removeShutdownHook");
                    Runtime.getRuntime().removeShutdownHook(stopRun);
                    //LogUtils.toc("removeShutdownHook");
                    stopRun = null;
                }
            } else {
                out("No more cases to run.", 2);
            }

            setArchiveDirectory(archiveDirectory);
        } catch (Exception ex) {
            err(ex, 0);
            ex.printStackTrace();
            //LogUtils.tic("merged_results.putAll(Utils.mergeStringArrayMap");
            merged_results.putAll(mergeStringArrayMap(newMap("error", ex.getMessage(), "trace", ex.fillInStackTrace())));
            //LogUtils.toc("merged_results.putAll(Utils.mergeStringArrayMap");
            setState(BATCH_EXCEPTION);
            return false;
        } finally {
            try {
                merged_results.putAll(merge(getSelectedCases()));//torun));
            } catch (Exception e) {
                err("Failed to merge results: " + torun, 0);
                throw new Exception("Failed to merge results: " + torun + "\n" + e.getMessage());
            }
        }

        setState(BATCH_OVER);
        //LogUtils.toc("runBatch");
        return true;
    }

    public void setArchiveDirectory(File ad) throws IOException {
        archiveDirectory = ad;
        if (archiveDirectory!=null && !archiveDirectory.exists()) {
            FileUtils.forceMkdir(archiveDirectory);
        }
        if (archiveDirectory == null) {
            archiveDirectory = prj.getResultsDir();
        } else {
            prj.setResultsDir(archiveDirectory);
            prj.moveCasesSpoolTo(archiveDirectory, getSelectedCases());
        }
    }

    public File getArchiveDirectory() {
        return archiveDirectory;
    }

    public static final String BATCH_OVER = "Batch over";
    public static final String BATCH_ARCHIVING = "Archiving...";
    public static final String BATCH_RUNNING = "Running...";
    public static final String BATCH_WAITINGCOMPUTERS = "Waiting...";
    public static final String BATCH_STARTING = "Starting...";
    public static final String BATCH_ERROR = "Batch failed";
    public static final String BATCH_EXCEPTION = "Batch exception";

    public String getState() {
        return state;
    }

    static int filled(List<Case> cases) {
        int n = 0;
        for (int i = 0; i < cases.size(); i++) {        //for (Case t : cases) {
            Case t = cases.get(i);
            if (t.hasRun()) {
                //System.err.print("x");
                n++;
            }//else                 System.err.print("-");

        }
        return n;
    }

    static Map<String, Object[]> merge(List<Case> cases) {
        if (cases == null) {
            return null;
        }
        Map<String, Object>[] results = new HashMap[cases.size()];
        for (int i = 0; i < cases.size(); i++) {
            results[i] = new HashMap<String, Object>();
            if (cases.get(i) != null) {

                if (cases.get(i).getInfo() != null && !cases.get(i).getInfo().isEmpty()) {
                    for (final String name : cases.get(i).getInfo().stringPropertyNames()) {
                        results[i].put(name, asObject(cases.get(i).getInfo().getProperty(name)));
                    }
                }
                if (cases.get(i).getOutputValues() != null) { // needed to get not only N(mean_keff,sigma_keff) (as string), but also individual mean_keff and sigma_keff values
                    results[i].putAll((Map<String, Object>) cases.get(i).getOutputValues());
                }
                if (cases.get(i).getResult() != null) {
                    results[i].putAll((Map<String, Object>) cases.get(i).getResult());
                }
            }
        }
        return mergeArrayMap(results);
    }

    public void shutdown() {
        if (askToStop) {
            out("Batch shutdown already in progress...", 4);
            return;
        }

        out("Ask for batch shutdown.", 3);

        //System.err.println("======================= shutdown() > waitForCalculator = false;");
        waitForCalculator = false;
        if (provider != null) {
            provider.waitingNextClient = false;
        }
        askToStop = true;

        out("Shutdown provider", 3);
        if (provider != null) {
            synchronized (provider.client_lock) {
                if (provider != null && provider.client_lock != null) {
                    provider.client_lock.notifyAll();
                }
            }
            try {
                provider.join();
            } catch (InterruptedException ex) {
                ex.printStackTrace(System.err);
            }
            if (provider != null) {
                provider.nextClient = null;
            }
        }
        //provider = null;

        out("Break runing cases", 3);
        synchronized (running_cleaner) {
            for (Thread clean : running_cleaner.values()) {
                clean.start();
            }
            for (Thread clean : running_cleaner.values()) {
                try {
                    clean.join();
                } catch (InterruptedException ex) {
                }
            }
            running_cleaner.clear();
        }

        //synchronized (POOL) {
        Funz_v1.POOL.setRefreshing(false, provider_lock, "BatchRun.shutdown");
        //}

        out("Waiting cases to stop", 3);
        for (RunCase runCase : runCases) {
            try {
                runCase.join();
            } catch (InterruptedException ex) {
            }
        }

        out("Cleanup cases", 3);
        runCases.clear();
    }

    /*public static void main(String[] args) throws Exception {
        Funz_v1.init();
        Funz.setVerbosity(1);

        String model = "R";

        Observer o = new Case.Observer() {

            @Override
            public void caseModified(int index, int what) {
                System.err.println(index + " " + what);
            }
        };

        File tmp_in = new File("tmp/branin.R");
        if (tmp_in.exists()) {
            tmp_in.delete();
        }
        Disk.copyFile(new File("src/main/resources/samples/branin.R"), tmp_in);

        IOPluginInterface plugin = IOPluginsLoader.newInstance(model, tmp_in);
        Project prj = ProjectController.createProject(tmp_in.getName(), tmp_in, model, plugin);

        plugin.setFormulaInterpreter(new RMathExpression(tmp_in.getName() + "_" + Configuration.timeDigest(), Configuration.isLog("R") ? new File(prj.getLogDir(), tmp_in + ".Rlog") : null));
        prj.setMainOutputFunction(plugin.suggestOutputFunctions().get(0));
        prj.setDesignerId(NODESIGNER_ID);

        Variable x1 = prj.getVariableByName("x1");
        x1.setType(Variable.TYPE_REAL);
        x1.setValues(Value.asValueList(".1", ".2", ".3"));

        Variable x2 = prj.getVariableByName("x2");
        x2.setType(Variable.TYPE_REAL);
        x2.setValues(Value.asValueList(".1", ".2"));

        prj.buildParameterList();

        prj.resetDiscreteCases(o);

        prj.setCases(prj.getDiscreteCases(), o);

        BatchRun_v1 batchRun = new BatchRun_v1(o, prj, new File("tmp")) {

            @Override
            public void out(String string, int i) {
                System.out.println(i + "! " + string);
            }

            @Override
            public void err(String msg, int i) {
                System.err.println(i + "! " + msg);
            }

            @Override
            public void err(Exception ex, int i) {
                System.err.println(i + "! " + ex.getLocalizedMessage());
            }
        };

        Utils.startCalculator(1);
        Utils.startCalculator(2);
        Utils.startCalculator(3);

        batchRun.runBatch();

        System.err.println(ArrayMapToMDString(batchRun.getResultsStringArrayMap()));
    }*/
}
