package org.funz.run;

import java.io.File;
import java.util.LinkedList;


public interface ILauncher {

    /** Add files into JDD or Calculs workspace.
     * @param files files to import 
     * @param together says whether all files belong to a single project or not
     * @param runNow if true files a re added into the Calculs workspace (no parameters allowed in this case)
     */
    public void addFiles(LinkedList<File> files, boolean together);

    /// Stops all slots
    public void breakRunning();

}
