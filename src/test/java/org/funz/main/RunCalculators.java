/*
This is just a main class to start calculator for backend testing. Only used in ant task spawn=true
 */
package org.funz.main;

import static org.funz.api.TestUtils.CONF_XML;
import static org.funz.api.TestUtils.CONF_XML_FAILING;
import static org.funz.api.TestUtils.calculators;
import static org.funz.api.TestUtils.startCalculator;
import static org.funz.api.TestUtils.verbose;
import org.funz.calculator.Calculator;
import org.funz.conf.Configuration;
import java.io.File;
import org.funz.util.ASCII;

/**
 *
 * @author richet
 */
public class RunCalculators {

    public static void main(String[] args) throws Exception {
        if (args == null || args.length != 2) {
            args = new String[]{"4", ""+(10 * 60)};
        }
        int n = Integer.parseInt(args[0]);
        int tmax = Integer.parseInt(args[1]);

        Configuration.setVerboseLevel(verbose);

        calculators = new Calculator[n];
        for (int i = 0; i < calculators.length - 1; i++) {
            calculators[i] = startCalculator(i, CONF_XML);
        }
        calculators[calculators.length - 1] = startCalculator(calculators.length - 1, CONF_XML_FAILING);

        File lock = new File("calculators.lock");
        ASCII.saveFile(lock,"1");
        for (int t=0; t < tmax; t = t+5) {
            if (!lock.exists()) break; // once file calculators.lock is removed, stop calculators.
            Thread.sleep(5000);
        }
        
        for (int i = 0; i < calculators.length; i++) {
            calculators[i].askToStop("end claculators");
        }

        System.exit(0);
    }
}
