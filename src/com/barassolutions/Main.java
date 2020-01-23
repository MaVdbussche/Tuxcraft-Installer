package com.barassolutions;

import com.sun.istack.internal.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class Main {

    // See JFileChooser
    // Ask user for MultiMC path ? OR run this from right directory
    // Ask user for path to new, zipped pack

    public static void main(String[] args) {

        /*Extract the archive*/
        Config.unzippedArchive = extract(Config.updateArchive);

        /*****
         * 1) check if Tuxcraft already exists (update vs fresh install)
         */
        List<File> existingTuxcraftInstances = new LinkedList<>();

        List<File> existingInstances = Arrays.asList(Objects.requireNonNull(Config.rootInstancesFolder.toFile().listFiles()));
        existingInstances.forEach(dir -> {
            if(dir.isDirectory() && dir.getName().toLowerCase().contains("tuxcraft")){
                /*List the files in this tuxcraft directory*/
                File[] content = dir.listFiles();
                if (content!=null){
                    /*Look for a file called tuxcraft-update.json*/
                    Arrays.asList(content).forEach(file -> {
                        if (file.getName().toLowerCase().equals("tuxcraft-update.json")){
                            /*This is a TuxCraft instance*/
                            existingTuxcraftInstances.add(dir);
                        } //else we skip it
                    });
                } // else we skip it
            } // else we skip it
        });
        existingTuxcraftInstances.sort(null);

        boolean freshInstall = existingTuxcraftInstances.isEmpty();
        Path newFolder = new File(Config.rootInstancesFolder.toString() + "/" + Config.unzippedArchive.getName()).toPath();
        Set<Path> preservedFiles;


        /*****
         * 2.1) Case fresh install :
         */
        if (freshInstall){
            preservedFiles = new HashSet<>();
        }
        /*****
         * 2.2) Case update :
         */
        else {
            /*Make a copy of the instance folder*/
            Path oldInstance = existingTuxcraftInstances.get(existingTuxcraftInstances.size()-1).toPath();
            try{
                Files.copy(oldInstance, newFolder,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (IOException e) {
                System.err.print("An IOException error occurred during the copy/move of the directory : " + e.getMessage());
                e.printStackTrace();
                System.out.println("The copy/move of directory \"" + oldInstance.toString() + "\" has probably failed due to an IOException error.");
            }
            preservedFiles = loadWhitelist(Config.unzippedArchive);
        }


        /*****
         * 3) Proceed to copy/move
         */
        try {
            Files.walk(Config.unzippedArchive.toPath()).filter(Files::isRegularFile).forEach(file -> move(file, newFolder, preservedFiles));
        } catch (IOException e){
            //TODO
        }
    }

    private static void move(Path file, Path newFolder, Set<Path> preservedFiles){
        if (preservedFiles.contains(file)){// We must preserve this file
            return; // Do nothing
        } else {
            Mover.moveFile(true, file, newFolder); //TODO use return value to check for soft errors
        }
    }

    @NotNull
    private static Set<Path> loadWhitelist(File whitelistFile){
        //TODO read the whitelist json
        return null;
    }

    private static File extract(File archive){
        //TODO
        return null;
    }

    // Extract (2) to (1) -> what to replace, what to ignore ?
    public static class Config{
        static Path rootInstancesFolder; // This is an absolute path obtained from the user, WITHOUT trailing /
        static File updateArchive; // This is an absolute path to the zip archive, obtained from the user, WITHOUT trailing /
        static File unzippedArchive; // This is an absolute path to the unzipped folder, WITHOUT trailing /

        static boolean copyAttributes = false;
    }
}
