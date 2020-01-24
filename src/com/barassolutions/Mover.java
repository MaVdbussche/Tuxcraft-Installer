package com.barassolutions;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class Mover {

    /**
     * @param replace True if copying, False if moving
     * @param source  a valid file
     * @param dest    a valid destination folder
     */
    static boolean moveFile(boolean replace, Path source, Path dest) {

        //if (!Files.isRegularFile(source)) {
        //    System.err.println("Could not move the file, as " + source.toString() + " is not a valid file.");
        //    return false;
        //}
        //System.out.println("[MOVER] Started moving " + source.toString() + " to " + dest.toString());
        try {
            List<CopyOption> options = new ArrayList<>();
            if(replace){
                options.add(StandardCopyOption.REPLACE_EXISTING);
            }
            if(Main.Values.copyAttributes){
                options.add(StandardCopyOption.COPY_ATTRIBUTES);
            }

            Files.copy(source, dest, options.toArray(new CopyOption[0]));
            //System.out.println("[MOVER] Copy of file " + source.toString() + " succeeded.");
            return true;
        } catch (IOException e) {
            System.err.println("An IOException error occurred during the actual copy/move of the file : " + e.getMessage());
            e.printStackTrace();
            System.out.println("The copy/move of file \"" + source.toString() + "\" has probably failed due to an IOException error.");
            return false;
        }
    }
}
