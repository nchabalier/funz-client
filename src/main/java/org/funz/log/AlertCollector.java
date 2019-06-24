package org.funz.log;

import java.io.File;

/**
 *
 * @author richet
 */
public interface AlertCollector {

    public void showInformation(String string);

    public void showError(String string);

    public void showException(Exception i);
    
    public String askInformation(String question);

    public boolean askYesNo(String question);
    
    public File askPath(String question);
}
