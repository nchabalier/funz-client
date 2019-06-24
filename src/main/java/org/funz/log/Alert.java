package org.funz.log;

import java.io.File;
import java.util.Scanner;

/**
 *
 * @author richet
 */
public class Alert {

    public static void setCollector(AlertCollector alert) {
        Collector = alert;
    }

    static AlertCollector Collector = new AlertCollector() {

        @Override
        public void showInformation(String string) {
            Log.logMessage("Alert", LogCollector.SeverityLevel.INFO, true, string);
            System.out.println("[Info] " + string);
        }

        @Override
        public void showError(String string) {
            Log.logMessage("Alert", LogCollector.SeverityLevel.ERROR, true, string);
            System.err.println("[Error] " + string);
        }

        @Override
        public void showException(Exception i) {
            Log.logException(true, i);
            System.err.println("[Exception] " + i);
        }

        @Override
        public String askInformation(String question) {
            System.out.println(question);
            Scanner reader = new Scanner(System.in);  // Reading from System.in
            String a = reader.next();
            reader.close();
            Log.logMessage("Alert", LogCollector.SeverityLevel.INFO, true, question + " >> " + a);
            return a;
        }

        @Override
        public boolean askYesNo(String question) {
            System.out.println(question + " [y/n]");
            Scanner reader = new Scanner(System.in);  // Reading from System.in
            boolean a = reader.next().equals("y");
            reader.close();

            Log.logMessage("Alert", LogCollector.SeverityLevel.INFO, true, question + " >> " + a);
            return a;
        }

        @Override
        public File askPath(String question) {
//if (!GraphicsEnvironment.isHeadless()) {
//                    JFileChooser fc = new JFileChooser();
//                    fc.setDialogTitle("");
//                    fc.setMultiSelectionEnabled(false);
//                    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
//                    if (fc.showOpenDialog(null) == JFileChooser.APPROVE_OPTION && fc.getSelectedFiles() != null) {
//                        return fc.getSelectedFile();
//                    } else return null;
//                } else {
            System.out.println(question);
            Scanner reader = new Scanner(System.in);  // Reading from System.in
            File f = new File(reader.next());
            reader.close();
            Log.logMessage("Alert", LogCollector.SeverityLevel.INFO, true, question + " >> " + f);
            return f;
//                }
        }
    };

    public static void showInformation(String string) {
        Collector.showInformation(string);
    }

    public static void showError(String string) {
        Collector.showError(string);
    }

    public static void showException(Exception i) {
        Collector.showException(i);
    }
    
    public static String askInformation(String q) {
        return Collector.askInformation(q);
    }

    public static boolean askYesNo(String q) {
        return Collector.askYesNo(q);
    }
    
    public static File askPath(String q) {
        return Collector.askPath(q);
    }
}
