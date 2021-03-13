package org.funz.run;

import java.util.ArrayList;
import java.util.List;
import org.funz.Protocol;
import org.funz.parameter.Case;
import org.funz.util.Parser;

/**
 * Common computer attributes structure.
 */
public class Computer {

    public List codes = new ArrayList();
    public ComputerGuard guard = null;
    public String name = "";
    public String host = "";
    public String ip = "";
    public String os = "?";
    public String codeList = "";
    public volatile String activity = "unknown";
    public int port;
    public long since;
    public volatile long lastPing;
    public volatile boolean use = true;
    private volatile Object user = null;

    public synchronized boolean setUser(Object _user) {
        if (user != null) {
            return false;
        }
        user = _user;
        return true;
    }

    public synchronized void freeUser() {
        user = null;
    }

    public Object getUser() {
        return user;
    }

    //static double G = 1024*1024*1024;
    public boolean isReady(String code, double cpu, long mem, long disk, String regexp) {
        boolean suits = (user == null) && use
                && getCodes().contains(code)
                && activity != null && activity.startsWith(Protocol.IDLE_STATE)
                && (regexp == null || host.matches(regexp));

        if (!suits) {
            return false;
        }

        if (cpu > 0) {
            try {
                String comp_cpu = Parser.between(activity, "cpu=", ";");
                if (Double.parseDouble(comp_cpu) < cpu) {
                    return false;
                }
            } catch (Exception e) {
            }
        }

        if (mem > 0) {
            try {
                String comp_mem = Parser.between(activity, "mem=", ";");
                if (Double.parseDouble(comp_mem) < mem) {
                    return false;
                }
            } catch (Exception e) {
            }
        }

        if (disk > 0) {
            try {
                String comp_disk = Parser.between(activity, "disk=", ";");
                if (Double.parseDouble(comp_disk) < disk) {
                    return false;
                }
            } catch (Exception e) {
            }
        }

        return true;
        /* && (System.currentTimeMillis()-comp.lastPing<5000)*/
    }

    @Override
    public String toString() {
        return "| " + use + " " + '\u0009'
                + "| " + name + " " + '\u0009'
                + "| " + host + " " + '\u0009'
                + "| " + os + " " + '\u0009'
                + "| " + (ip + ":" + port) + " " + '\u0009'
                + "| " + (user != null ? "used by " + user : "free") + " " + '\u0009'
                + "| " + Case.longToTimeString(since) + " " + '\u0009'
                + "| " + activity + " " + '\u0009'
                + "| " + codeList + " " + '\u0009' + "|";
        //return "Computer " + name + " (" + host + " " + ip + ":" + port + ") \n  codes: " + codeList + "\n  since: " + Case.longToTimeString(since) + "\n  status: " + (use ? "used by " + user : "idle") + "\n  activity: " + activity;
    }

    public static String toStringTitle() {
        return "| Use " + '\u0009'
                + "| Computer " + '\u0009'
                + "| host name " + '\u0009'
                + "| OS " + '\u0009'
                + "| address:port " + '\u0009'
                + "| local status " + '\u0009'
                + "| since " + '\u0009'
                + "| activity " + '\u0009'
                + "| codes " + '\u0009' + "|";
    }

    /**
     * @return the codes
     */
    public synchronized List getCodes() {
        return codes;
    }

    /**
     * @param codes the codes to set
     */
    public synchronized void setCodes(List codes) {
        this.codes = codes;
    }

    void freeActivity() {
        activity = Protocol.IDLE_STATE;
    }

    /**
     * Simple code description structure.
     */
    public static class Code {

        /**
         * Command line syntax
         */
        public String command;
        /**
         * Code name
         */
        public String name;
    }

    /**
     * Computer connectivity callback interface
     */
    public static interface ComputerGuard {

        public void removeComputer(Computer comp);

        public void addComputer(Computer comp);
    }

    public static interface ComputerStatusListener {

        void fireComputerDied(Computer comp);

        void fireComputerStatusUnknown(Computer comp);

        void fireUpdateComputerInfo(Computer comp);
    }
}
