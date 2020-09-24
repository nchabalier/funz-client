package org.funz.main;

import org.apache.commons.lang.StringUtils;
import org.funz.api.Funz_v1;
import org.funz.log.Log;
import org.funz.log.LogFile;

/**
 *
 * @author richet
 */
public class MainUtils {

    public static final int RUN_ERROR = 1;
    public static final int START_ERROR = -1;
    public static final int CREATE_SHELL_ERROR = -2;
    public static final int FUNZ_ERROR = -3;
    public static final int INPUT_FILE_ERROR = -1000;
    public static final int PARSE_ERROR = -10000;
    public static final int READ_ERROR = -20000;
    public static final int DESIGN_ERROR = 2;
    public static final int FUNCTION_ERROR = 3;
    public static final int COMPILE_ERROR = -30000;
    public static final int FIND_ERROR = -10;

    public static String CLEAR_LINE = "\r";

    public static void init(String name, int level) {
        //tic("init");
        try {
            Log.setCollector(new LogFile(name + ".log"));
            Funz_v1.init();
            Log.level = level; // keep after Funz.init(), otherwise it will be reseted !
        } catch (Exception e) {
            System.err.println("[ERROR] failed to INIT Funz: " + e.getMessage());
            //e.printStackTrace();
            System.exit(FUNZ_ERROR);
        }
        //toc("init");
    }

    final static String S = StringUtils.repeat(" ", 50);
}
