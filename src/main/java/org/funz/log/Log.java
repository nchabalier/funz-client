package org.funz.log;

import java.io.File;
import java.io.IOException;
import org.funz.conf.Configuration;
import org.funz.util.Disk;

/**
 *
 * @author richet
 */
public class Log /*implements LogCollector*/ {

    static {
         System.setProperty("org.apache.commons.logging.Log",
                         "org.apache.commons.logging.impl.NoOpLog");
    }
    
    public static String[] SPACES = new String[]{"", " ", "  ", "   ", "    ", "     ", "      ", "       ", "        ", "         ", "          ", "           ", "             ", "              ", "               ", "                "};
    public static int level = 1;

    static synchronized void out_noln(String message, int l) {
        if (l < level) {
            Collector.logMessage(LogCollector.SeverityLevel.INFO, true, SPACES(l) + message);
        }
    }

    public static synchronized void out(String message, int l) {
        try {
            if (l < level) {
                Collector.logMessage(LogCollector.SeverityLevel.INFO, true, SPACES(l) + message);
            }
        } catch (Exception e) {
            err(e, -1);
        }
    }

    public static synchronized void err(String message, int l) {
        if (l < level) {
            Collector.logException(true, new Exception(SPACES(l) + message));
        }
    }

    public static synchronized void err(Exception ex, int l) {
        if (l < level) {
            Collector.logException(true, ex);
        }
    }

    static synchronized void err_noln(String message, int l) {
        if (l < level) {
            Collector.logException(true, new Exception(SPACES(l) + message));
        }
    }

    private static String SPACES(int l) {
        if (l >= 0 && l < SPACES.length) {
            return SPACES[l];
        } else {
            return "*** ";
        }
    }

    public static void backupFile(File f) {
        if (f.exists()) {
            File backup = new File(f.getParent(), f.getName() + ".old");
            int i = 0;
            while (backup.exists()) {
                backup = new File(f.getParent(), f.getName() + ".old." + (i++));
            }
            try {
                Disk.moveFile(f, backup);
            } catch (IOException ex) {
                f.delete();
            }
        }
        if (f.getParentFile() != null && !f.getParentFile().exists()) {
            f.getParentFile().mkdirs();
        }
    }

    public static boolean tictoc = Boolean.valueOf(System.getProperty("tictoc", "false"));

    public final static LogCollector SystemCollector = new LogCollector() {

        @Override
        public void logException(boolean sync, Exception ex) {
            if (tictoc) {
                System.err.println("<+" + LogTicToc.DT() + "> " + ex.getMessage());
            } else {
                System.err.println(ex.getMessage());
            }
        }

        @Override
        public void logMessage(SeverityLevel severity, boolean sync, String message) {
            if (tictoc) {
                System.out.println("<+" + LogTicToc.DT() + "> " + message);
            } else {
                System.out.println(message);
            }
        }

        @Override
        public void resetCollector(boolean sync) {
        }

        @Override
        public void close() {
        }

        @Override
        public String toString() {
            return "LogCollector " + this.getClass() + ", verb=" + level;
        }
    };

    public static LogCollector Collector = SystemCollector;

    public static void setCollector(LogCollector collector) {
        Collector = collector;
    }

    public static void logException(boolean sync, final Exception ex) {
        Collector.logException(sync, ex);
    }

    public static void logMessage(Object src, final LogCollector.SeverityLevel severity, boolean sync, final String message) {
        if (src == null) {
            Collector.logMessage(severity, sync, message);
            return;
        }
        if (src instanceof String) {
            if (Configuration.isLog((String) src)) {
                Collector.logMessage(severity, sync, "[" + src + "] " + message);
            }
            return;
        }
        if (Configuration.isLog(src.getClass().getSimpleName())) {
            Collector.logMessage(severity, sync, "[" + src.getClass().getSimpleName() + "] " + message);
            return;
        }
    }

    public void resetCollector(boolean sync) {
        Collector.resetCollector(sync);
    }

    public void close() {
        Collector.close();
    }

}
