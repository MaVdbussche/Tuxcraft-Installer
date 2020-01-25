package com.barassolutions;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Main {

  /**
   * Main method. 1) Launching the GUI 2) Extract the zip archive in its folder 3) Check if Tuxcraft
   * is already present in this MultiMC install (update vs fresh install) 4.1) In the case of a
   * fresh install 4.2) In the case of an update : 5) Proceed to copy/move the extracted archive to
   * the instances folder 6) Delete the temporary extracted archive 7) Close the GUI elements
   */
  public static void main(String[] args) {
    try {
      /*1) Launching the GUI*/
      Gui.initValues();

      /*2) Extract the zip archive in its folder*/
      Gui.onExtract();
      extract(Values.updateArchive, Values.updateArchive.getParentFile());

      /*3) Check if Tuxcraft is already present in this MultiMC install (update vs fresh install)*/
      Gui.onInstancesCheck();
      List<File> existingTuxcraftInstances = new LinkedList<>();

      File[] instances = Values.rootInstancesFolder.listFiles();
      if (instances != null) {
        Arrays.stream(instances)
            .filter(dir ->
                (dir.isDirectory() && dir.getName().toLowerCase().contains("tuxcraft")))
            .forEach(dir -> {
              /*List the files in this tuxcraft directory*/
              File[] content = dir.listFiles();
              if (content != null) {
                /*Look for a file called tuxcraft-update.json*/
                if (Arrays.stream(content)
                    .anyMatch(file -> file.getName().toLowerCase().equals(Values.updateFileName))) {
                  /*This is a TuxCraft instance*/
                  System.out
                      .println("Found one existing TuxCraft instance, named " + dir.getName());
                  existingTuxcraftInstances.add(dir);
                }
              } // else we skip it
            });
      }
      existingTuxcraftInstances.sort(null);

      boolean freshInstall = (existingTuxcraftInstances.size() == 0);
      Path newFolder = Paths.get(Values.rootInstancesFolder.toString(),
          Values.unzippedArchive.getName());
      System.out.println("New Folder will be placed at " + newFolder.toString());
      Set<Path> preservedFiles;

      if (freshInstall) {
        /*4.1) In the case of a fresh install :*/
        Gui.onSkipCopyOld();
        System.out.println("Fresh install !");
        preservedFiles = new HashSet<>();
      } else {
        /*4.2) In the case of an update :*/
        Gui.onCopyOld();
        System.out.println("Updating !");
        /*Make a copy of the instance folder*/
        Path oldInstance = existingTuxcraftInstances.get(existingTuxcraftInstances.size() - 1)
            .toPath();
        try {
          copyRecursively(oldInstance, newFolder, new HashSet<>());
          System.out.println("Done copying the old instance folder.");
        } catch (Exception e) {
          System.err.print(
              "An IOException error occurred during the copy/move of the directory : "
                  + e.getMessage());
          e.printStackTrace();
          System.out.println("The copy/move of directory \"" + oldInstance.toString()
              + "\" has probably failed due to an IOException error.");
        }
        preservedFiles = loadWhitelist(Values.unzippedArchive);
      }

      /*5) Proceed to copy/move the extracted archive to the instances folder*/
      Gui.onUpdating();
      copyRecursively(Values.unzippedArchive.toPath(), newFolder, preservedFiles);

    } finally {
      /*6) Delete the temporary extracted archive*/
      Gui.onCleanup();
      deleteRecursively(Values.unzippedArchive);
      /*7) Close the GUI elements*/

      Gui.onDone();
      try {
        Thread.sleep(2000);
      } catch (InterruptedException ignored) {
      }
      Gui.exit();
    }
  }

  /**
   * Copies recursively a folder, including all its sub-contents, and REPLACES files with
   * conflicting names. Allows to skip (not copy) certain files by passing them in the 3rd argument
   *
   * @param sourceFolder   the Path to the source folder
   * @param destFolder     the Path to the destination folder
   * @param preservedFiles a Set of Path to files that will not by copied to the destination
   */
  private static void copyRecursively(Path sourceFolder, Path destFolder,
      Set<Path> preservedFiles) {
    System.out.println("Importing new instance...");
    System.out.println("This update will ignore " + preservedFiles.size() + " elements.");
    int objects = 0;
    //Classic depth-first tree scan, using a stack as a frontier
    Stack<File> frontier = new Stack<>();
    frontier.add(sourceFolder.toFile());

    while (!frontier.empty()) {
      // pop it
      File newFile = frontier.pop();
      // handle it
      Path pathRelative = sourceFolder
          .relativize(newFile.toPath()); // Path relative to the root of instance folder
      // expand it (or not)
      if (!preservedFiles.contains(pathRelative)) {
        /*We have to bypass possible DirectoryNotEmptyExceptions, by checking if newFile is a
        directory and is present at destination. If so, we can safely ignore it (not its content)*/
        Path dest = destFolder.resolve(pathRelative);
        //noinspection ConstantConditions
        if (newFile.isDirectory() && dest.toFile().isDirectory() && Arrays
            .asList(dest.toFile().list()).contains(newFile.getName())) {
          /*Ignoring this one*/
        } else {
          Mover.moveFile(true, newFile.toPath(), dest);
          objects++;
        }
        /*In any case, we have to treat the children of this file/folder*/
        File[] children = newFile.listFiles();
        if (children != null) {
          Collections.addAll(frontier, children);
        }
      }
    }
    System.out.println("Done copying " + objects + " objects.");
  }

  /**
   * Deletes recursively a File, including all its sub-contents if it denotes a directory.
   * Be careful !
   *
   * @param file   the Path to the folder to be deleted
   */
  private static void deleteRecursively(File file) {
    File[] contents = file.listFiles();
    if (contents != null) {
      for (File f : contents) {
        if (! Files.isSymbolicLink(f.toPath())) {
          deleteRecursively(f);
        }
      }
    }
    file.delete();
  }

  /**
   * Extracting a zip archive to a given destination.
   *
   * @param archive     the zip file to extract
   * @param destination the folder where we should extract it
   */
  private static void extract(File archive, File destination) {
    try {
      ZipFile zipFile = new ZipFile(archive);
      zipFile.extractAll(destination.toString());
    } catch (ZipException e) {
      e.printStackTrace();
    }
  }

  /**
   * Read the update whitelist file.
   *
   * @param instanceFolder folder where the update json file is located
   * @return a Set of Path obtained from the whitelist file
   */
  private static Set<Path> loadWhitelist(File instanceFolder) {
    Set<Path> out = new HashSet<>();
    List<String> fileNames = new LinkedList<>();
    for (File file : Objects.requireNonNull(instanceFolder.listFiles())) {
      fileNames.add(file.getName());
    }

    if (!fileNames.contains(Values.updateFileName)) {
      System.err.println("*********************************************");
      System.err.println("*********************************************");
      System.err.println("[CRITICAL] Could not find tuxcraft-update.json !!!!!");
      System.err.println("*********************************************");
      System.err.println("*********************************************");
      System.err.println("We will proceed and replace all the files in the instance !");
    }
    JSONParser jsonParser = new JSONParser();
    try (FileReader fr = new FileReader(new File(instanceFolder, Values.updateFileName))) {
      Object obj = jsonParser.parse(fr);
      out = parseObject((JSONObject) obj);
    } catch (IOException | ParseException e) {
      e.printStackTrace();
    }
    System.out.println("Loaded whitelist, size=" + out.size());
    return out;
  }

  @SuppressWarnings("unchecked")
  private static Set<Path> parseObject(JSONObject pathsObject) {
    Set<Path> out = new HashSet<>();
    JSONArray pathsList = (JSONArray) pathsObject.get("paths");
    pathsList.forEach(elem -> out.add(Paths.get((String) elem)));
    return out;
  }


  static class Values {

    static File rootInstancesFolder; // This is an ABSOLUTE path obtained from user
    static File updateArchive; // This is an ABSOLUTE path to the zip archive, obtained from user
    static File unzippedArchive; // This is an ABSOLUTE path to the unzipped folder

    static boolean copyAttributes = false;
    static String updateFileName = "tuxcraft-update.json";
  }
}
