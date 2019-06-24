package org.funz;
 
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.funz.api.Funz_v1;
import org.funz.run.CalculatorsPool;
import org.funz.run.Computer;

/**
 *
 * @author richet
 */
public class StarterMonitor {

    public final static CalculatorsPool POOL;
    static Funz_v1 pv1;
    private int verb;

    static {
        pv1 = new Funz_v1();
        POOL = new CalculatorsPool(CalculatorsPool.getFreePort(true));
    }

    private StarterMonitor(String code, int delay, String cmd, int verb) {
        System.out.println("Starting monitor of code '" + code + "' every " + delay + "s., using command '" + cmd + "'");
        this.verb = verb;

        if (!poolHasCode(code)) {
            System.err.println("[WARNING] Code " + code + " is not displayed by any calculator ! \n    " + POOL.getCodes());
        }

        org.funz.util.Process proc = new org.funz.util.Process(cmd, null, null);
        while (true) {
            try {
                Thread.sleep(delay * 1000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            List<String> f = findFreeComputers(code);
            if (f.size() < 1) {
                int ret = 0;
                try {
                    if (verb > 0) {
                        System.out.println("Execute command " + cmd);
                    } else {
                        System.out.print("x");
                    }
                    ret = proc.runCommand();
                } catch (Exception ex) {
                    System.err.println("Failed to execute command: " + cmd + "\n  " + proc.getFailReason());
                }
                if (ret != 0) {
                    System.err.println("Bad return status from command: " + cmd + "\n  " + proc.getFailReason());
                }
            } else {
                if (verb > 0) {
                    System.out.println("Free computers: " + f);
                } else {
                    System.out.print(".");
                }
            }
        }

    }

    boolean poolHasCode(String code) {
        if (code.equals("*")) {
            return true;
        }
        if (!POOL.isRefreshing()) {
            POOL.setRefreshing(true,this,"StarterMonitor.poolHasCode");
            try {
                Thread.sleep(5000); // to let enough time to actualize grid
            } catch (InterruptedException ex) {
            }
        }
        synchronized (POOL) {
            for (Object code_i : POOL.getCodes()) {
                if (code_i.toString().matches(code)) {
                    return true;
                }
            }
        }
        return false;
    }

    List<String> findFreeComputers(String code) {
        List<String> free = new LinkedList<String>();
        int n = 0;
        if (!POOL.isRefreshing()) {
            POOL.setRefreshing(true,this,"StarterMonitor.findFreeComputers");
            try {
                Thread.sleep(5000); // to let enough time to actualize grid
            } catch (InterruptedException ex) {
            }
        }
        synchronized (POOL) {
            for (Iterator<Computer> i = POOL.getComputers().iterator(); i.hasNext();) {
                Computer c = i.next();
//            try {
//                for (Computer c : POOL.getComputers()) {
                    boolean match = code.equals("*");
                    if (!match) {
                        for (Object code_i : c.getCodes()) {
                            if (code_i.toString().matches(code)) {
                                match = true;
                                break;
                            }
                        }
                    }
                    if (match) {
                        if (c.use && c.activity != null && c.activity.startsWith(Protocol.IDLE_STATE)) {
                            free.add(c.name);
                        }
                    }
                }
//            } catch (ConcurrentModificationException c) {
//                if (verb > 0) {
//                    System.out.println("Skipping free computer update");
//                } else {
//                    System.out.println("?");
//                }
//            }
        }
        return free;
    }

    public static void main(String[] args) {

        //args = new String[]{"-code=moret5b", "-delay=5"};
        String code = "*";
        int delay = 5;
        String cmd = null;
        int verb = 0;
        if (args == null) {
            usage(null);
        }
        try {
            for (String arg : args) {
                if (arg.startsWith("-code=")) {
                    code = arg.split("=")[1];
                } else if (arg.startsWith("-delay=")) {
                    delay = Integer.parseInt(arg.split("=")[1]);
                } else if (arg.startsWith("-cmd=")) {
                    cmd = arg.split("=")[1];
                } else if (arg.startsWith("-verbose=")) {
                    verb = Integer.parseInt(arg.split("=")[1]);
                } else {
                    usage(arg);
                }
            }
        } catch (Exception e) {
            usage(null);
        }
        if (cmd == null) {
            cmd = "echo \"Funz pool is empty for code " + code + " \"";
            usage(null);
        }

        new StarterMonitor(code, delay, cmd, verb);
    }

    public static void usage(String unknown) {
        if (unknown != null) {
            System.err.println("Could not par argument: " + unknown);
        }
        System.out.println("Options:");
        System.out.println("-code=<codeToMonitor> ");
        System.out.println("-delay=<waitTimeBeforeAction> ");
        System.out.println("-cmd=<actionToWakeUpCalculator> ");
        System.out.println("-verbose=<n> ");
    }
}
