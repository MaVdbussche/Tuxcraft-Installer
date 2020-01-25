package com.barassolutions;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        try {
            Gui.initValues();

            /*Extract the archive*/
            extract(Values.updateArchive, Values.updateArchive.getParentFile());


            /*****
             * 1) check if Tuxcraft already exists (update vs fresh install)
             */
            List<File> existingTuxcraftInstances = new LinkedList<>();

            List<File> existingInstances = Arrays.asList(Objects.requireNonNull(Values.rootInstancesFolder.listFiles()));
            existingInstances.forEach(dir -> {
                if (dir.isDirectory() && dir.getName().toLowerCase().contains("tuxcraft")) {
                    /*List the files in this tuxcraft directory*/
                    File[] content = dir.listFiles();
                    if (content != null) {
                        /*Look for a file called tuxcraft-update.json*/
                        Arrays.asList(content).forEach(file -> {
                            if (file.getName().toLowerCase().equals(Values.updateFileName)) {
                                /*This is a TuxCraft instance*/
                                existingTuxcraftInstances.add(dir);
                                System.out.println("Found one existing TuxCraft instance, named " + dir.getName());
                            } //else we skip it
                        });
                    } // else we skip it
                } // else we skip it
            });
            existingTuxcraftInstances.sort(null);

            boolean freshInstall = (existingTuxcraftInstances.size() == 0); // This one instance is the newly extracted one
            Path newFolder = Paths.get(Values.rootInstancesFolder.toString(), Values.unzippedArchive.getName());
            System.out.println("New Folder will be placed at " + newFolder.toString());
            Set<Path> preservedFiles;


            /*****
             * 2.1) Case fresh install :
             */
            if (freshInstall) {
                System.out.println("Fresh install !");
                preservedFiles = new HashSet<>();
            }
            /*****
             * 2.2) Case update :
             */
            else {
                System.out.println("Updating !");
                /*Make a copy of the instance folder*/
                Path oldInstance = existingTuxcraftInstances.get(existingTuxcraftInstances.size() - 1).toPath();
                try {
                    copyRecursively(oldInstance, newFolder, new HashSet<>());
                    //Files.copy(oldInstance, newFolder,
                    //        StandardCopyOption.REPLACE_EXISTING
                    //);
                    System.out.println("Done copying the old instance folder.");
                } catch (Exception e) {
                    System.err.print("An IOException error occurred during the copy/move of the directory : " + e.getMessage());
                    e.printStackTrace();
                    System.out.println("The copy/move of directory \"" + oldInstance.toString() + "\" has probably failed due to an IOException error.");
                }
                preservedFiles = loadWhitelist(Values.unzippedArchive);
            }


            /*****
             * 3) Proceed to copy/move
             */
            copyRecursively(Values.unzippedArchive.toPath(), newFolder, preservedFiles);
        }
        catch (Exception e) {
            StringBuilder msg = new StringBuilder("An error occurred, stacktrace (most recent up):\n");
            for( StackTraceElement stackElem : e.getStackTrace()) msg.append(stackElem.toString());
            JOptionPane.showMessageDialog(null, msg.toString(), "Oh no !", JOptionPane.ERROR_MESSAGE);
        }
        finally {
            //TODO delete archive extracted at Values.unzippedArchive
            Gui.exit();
        }
    }

    private static void copyRecursively(Path sourceFolder, Path destFolder, Set<Path> preservedFiles){
        System.out.println("Importing new instance...");
        System.out.println("This update will ignore " + preservedFiles.size() + " elements, according to instructions in " + Values.updateFileName);
        int objects = 0;
        //Depth-first -> stack
        Stack<File> frontier = new Stack<>();
        frontier.add(sourceFolder.toFile());

        while(! frontier.empty()){
            // pop it
            File newFile = frontier.pop(); // this is an ABSOLUTE path
            //System.out.print("Now copying file " + newFile.toString() + " ... ");
            // handle it
            Path pathRelative = sourceFolder.relativize(newFile.toPath()); // Path relative to the root of instance folder
            //System.out.println("to " + dest.resolve(pathRelative).toString());
            // expand it (or not)
            if (! preservedFiles.contains(pathRelative)){
                /*We have to bypass possible DirectoryNotEmptyExceptions, by checking if newFile is a directory and is present at destination. If so, we can safely ignore it (not its content)*/
                Path dest = destFolder.resolve(pathRelative);
                if(newFile.isDirectory() && dest.toFile().isDirectory() && Arrays.asList(dest.toFile().list()).contains(newFile.getName())){
                    /*Ignoring this one*/
                } else {
                    Mover.moveFile(true, newFile.toPath(), dest);
                    objects++;
                }
                /*In any case, we have to treat the children of this file/folder*/
                File[] children = newFile.listFiles();
                if(children!=null) {
                    Collections.addAll(frontier, children);
                }
            }
        }
        System.out.println("Done copying " + objects + " objects.");
    }

    private static void extract(File archive, File destination){
        try {
            ZipFile zipFile = new ZipFile(archive);
            zipFile.extractAll(destination.toString());
        } catch (ZipException e) {
            e.printStackTrace();
        }
    }

    private static Set<Path> loadWhitelist(File instanceFolder){
        Set<Path> out = new HashSet<>();
        List<String> fileNames = new LinkedList<>();
        for (File file : Objects.requireNonNull(instanceFolder.listFiles())){
            fileNames.add(file.getName());
        }

        if (! fileNames.contains(Values.updateFileName)){
            System.err.println("*********************************************");
            System.err.println("*********************************************");
            System.err.println("[CRITICAL] Could not find tuxcraft-update.json !!!!!");
            System.err.println("*********************************************");
            System.err.println("*********************************************");
            System.err.println("We will proceed and replace all the files in the instance !");
        }
        JSONParser jsonParser = new JSONParser();
        try(FileReader fr = new FileReader(new File(instanceFolder, Values.updateFileName))){
            Object obj = jsonParser.parse(fr);
            out = parseObject((JSONObject) obj);
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
        System.out.println("Loaded whitelist, size=" + out.size());
        return out;
    }
    @SuppressWarnings("unchecked")
    private static Set<Path> parseObject(JSONObject pathsObject){
        Set<Path> out = new HashSet<>();
        JSONArray pathsList = (JSONArray) pathsObject.get("paths");
        pathsList.forEach(elem -> out.add(Paths.get((String)elem)));
        return out;
    }


    public static class Values {
        static File rootInstancesFolder; // This is an ABSOLUTE path obtained from the user
        static File updateArchive; // This is an ABSOLUTE path to the zip archive, obtained from the user
        static File unzippedArchive; // This is an ABSOLUTE path to the unzipped folder

        static boolean copyAttributes = false;
        static String updateFileName = "tuxcraft-update.json";
    }
}
