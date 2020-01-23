package com.barassolutions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class Mover {

    /**
     * @param replace True if copying, False if moving
     * @param source  a valid file
     * @param dest    a valid destination folder
     */
    static boolean moveFile(boolean replace, Path source, Path dest) {

        if (!Files.isRegularFile(source)) {
            System.err.println("Could not move the file, as " + source.toString() + " is not a valid file.");
            return false;
        }
        if (!Files.isDirectory(dest)) { //TODO do this check before this point
            System.err.println("Could not move the file, as " + dest.toString() + " is not a valid directory.");
            return false;
        }

        try {
            Files.copy(source, dest,
                    replace ? StandardCopyOption.REPLACE_EXISTING : null,
                    Main.Config.copyAttributes ? StandardCopyOption.COPY_ATTRIBUTES : null
            );
            return true;
        } catch (IOException e) {
            System.err.print("An IOException error occurred during the actual copy/move of the file : " + e.getMessage());
            e.printStackTrace();
            System.out.println("The copy/move of file \"" + source.toString() + "\" has probably failed due to an IOException error.");
            return false;
        }
    }
}
