package org.funz.main;

import org.funz.api.Print;
import static org.funz.main.MainUtils.init;

/**
 *
 * @author richet
 */
public class GridStatus extends MainUtils{

    static String name = "GridStatus";
    
    static {
        init(name,10);
    }


    public static void main(String[] args) {
        System.out.println(Print.gridStatusInformation());

        System.exit(0);
    }

}
