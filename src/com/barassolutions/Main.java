package com.barassolutions;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class Main {

    // See JFileChooser
    // Ask user for MultiMC path ? OR run this from right directory
    // Ask user for path to new, zipped pack

    public static void main(String[] args) {
        try {
            Gui.initValues();

            /*Extract the archive*/
            extract(Values.updateArchive, Values.unzippedArchive);

            /*****
             * 1) check if Tuxcraft already exists (update vs fresh install)
             */
            List<File> existingTuxcraftInstances = new LinkedList<>();

            List<File> existingInstances = Arrays.asList(Objects.requireNonNull(Values.rootInstancesFolder.toFile().listFiles()));
            existingInstances.forEach(dir -> {
                if (dir.isDirectory() && dir.getName().toLowerCase().contains("tuxcraft")) {
                    /*List the files in this tuxcraft directory*/
                    File[] content = dir.listFiles();
                    if (content != null) {
                        /*Look for a file called tuxcraft-update.json*/
                        Arrays.asList(content).forEach(file -> {
                            if (file.getName().toLowerCase().equals("tuxcraft-update.json")) {
                                /*This is a TuxCraft instance*/
                                existingTuxcraftInstances.add(dir);
                            } //else we skip it
                        });
                    } // else we skip it
                } // else we skip it
            });
            existingTuxcraftInstances.sort(null);

            boolean freshInstall = existingTuxcraftInstances.isEmpty();
            Path newFolder = new File(Values.rootInstancesFolder.toString() + "/" + Values.unzippedArchive.getName()).toPath();
            Set<Path> preservedFiles;


            /*****
             * 2.1) Case fresh install :
             */
            if (freshInstall) {
                preservedFiles = new HashSet<>();
            }
            /*****
             * 2.2) Case update :
             */
            else {
                /*Make a copy of the instance folder*/
                Path oldInstance = existingTuxcraftInstances.get(existingTuxcraftInstances.size() - 1).toPath();
                try {
                    Files.copy(oldInstance, newFolder,
                            StandardCopyOption.REPLACE_EXISTING
                    );
                } catch (IOException e) {
                    System.err.print("An IOException error occurred during the copy/move of the directory : " + e.getMessage());
                    e.printStackTrace();
                    System.out.println("The copy/move of directory \"" + oldInstance.toString() + "\" has probably failed due to an IOException error.");
                }
                preservedFiles = loadWhitelist(Values.unzippedArchive);
            }


            /*****
             * 3) Proceed to copy/move
             */
            copyRecursively(Values.unzippedArchive.toPath(), preservedFiles);
            /*try {
                Files.walk(Values.unzippedArchive.toPath()).filter(Files::isRegularFile).forEach(file -> move(file, newFolder, preservedFiles));
            } catch (IOException e){
                System.err.print("An IOException error occurred during the copy/move of a file : " + e.getMessage());
                System.err.println("Skipping it.");
                e.printStackTrace();
            }*/
        }
        finally {
            Gui.exit();
        }
    }

    private static void copyRecursively(Path folderRoot, Set<Path> preservedFiles){
        //TODO reprendre ici, mon cerveau fond
        //Depth-first -> stack
        Stack<File> frontier = new Stack<>();
        Collections.addAll(frontier, Objects.requireNonNull(folderRoot.toFile().listFiles()));

        while(! frontier.empty()){
            File newFile = frontier.pop();
            Path filePathRelative = folderRoot.relativize(Paths.get(folderRoot.toString(), newFile.toString())); // Path of the file relative to the instance folder root
            if(! preservedFiles.contains(filePathRelative)){
                /*This file will be overwritten*/
                Mover.moveFile(true, filePathRelative.toAbsolutePath(), null);
            }
        }
    }

    private static Set<Path> loadWhitelist(File whitelistFile){
        JSONParser jsonParser = new JSONParser();
        try(FileReader fr = new FileReader(whitelistFile)){
            Object obj = jsonParser.parse(fr);
            return parseObject((JSONObject) obj);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        return null;
    }
    @SuppressWarnings("unchecked")
    private static Set<Path> parseObject(JSONObject pathsObject){
        Set<Path> out = new HashSet<>();
        JSONArray pathsList = (JSONArray) pathsObject.get("paths");
        pathsList.forEach(elem -> out.add(Paths.get((String)elem)));
        return out;
    }

    private static void extract(File archive, File destination){
        try {
            ZipFile zipFile = new ZipFile(archive);
            zipFile.extractAll(destination.toString());
        } catch (ZipException e) {
            e.printStackTrace();
        }
    }

    public static class Values {
        static Path rootInstancesFolder; // This is an absolute path obtained from the user, WITHOUT trailing /
        static File updateArchive; // This is an absolute path to the zip archive, obtained from the user, WITHOUT trailing /
        static File unzippedArchive; // This is an absolute path to the unzipped folder, WITHOUT trailing /

        static boolean copyAttributes = false;
    }
}
