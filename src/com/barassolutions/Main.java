package com.barassolutions;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
      File existingTuxcraftInstance = findMostRecentInstance(Values.rootInstancesFolder);

      boolean freshInstall = (existingTuxcraftInstance == null);
      File newFolder = new File(Values.rootInstancesFolder, Values.unzippedArchive.getName());
      //noinspection ResultOfMethodCallIgnored
      newFolder.mkdir();
      System.out.println("New Folder will be placed at " + newFolder.getAbsolutePath());
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
        copyFoldersContents(existingTuxcraftInstance, newFolder, new HashSet<>());
        preservedFiles = loadWhitelist(Values.unzippedArchive);
      }

      /*5) Proceed to copy/move the extracted archive to the instances folder*/
      Gui.onUpdating();
      copyFoldersContents(Values.unzippedArchive, newFolder, preservedFiles);

    } finally {
      /*6) Delete the temporary extracted archive*/
      Gui.onCleanup();
      deleteRecursively(Values.unzippedArchive);
      /*7) Close the GUI elements*/

      Gui.onDone();
      try {
        Thread.sleep(2000);
      } catch (InterruptedException ignored) {
        //ignored
      }
      Gui.exit();
    }
  }

  /**
   * Returns the most recent Tuxcraft instance installed in this folder, according to the
   * lexicographical order of the folder names. Version numbers are designed with this in mind.
   *
   * @param instancesFolder the instances folder of a MultiMC installation
   * @return the last Tuxcraft instance installed in the given directory, or null if none was found.
   */
  private static File findMostRecentInstance(File instancesFolder) {
    List<File> tuxcraftInstances = new LinkedList<>();
    File[] instances = instancesFolder.listFiles();
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
                tuxcraftInstances.add(dir);
              }
            } // else we skip it
          });
    }
    if (tuxcraftInstances.size() == 0) {
      return null;
    } else {
      tuxcraftInstances.sort(null);
      return tuxcraftInstances.get(tuxcraftInstances.size() - 1); //Returning the most recent one
    }
  }

  /**
   * Copies recursively a File, including all its sub-contents if it denotes a directory.
   *
   * @param sourceInstanceFolder a valid instance folder
   * @param destInstanceFolder   a directory. May be empty, or already contain a Tuxcraft instance
   * @param preserved            Set of Paths to ignore. In other words, the files denoted by these
   *                             paths won't be copied from sourceInstanceFolder
   */
  private static void copyFoldersContents(File sourceInstanceFolder, File destInstanceFolder,
      Set<Path> preserved) {
    if (sourceInstanceFolder.isDirectory() && destInstanceFolder.isDirectory()) {
      //noinspection ConstantConditions
      for (File f : sourceInstanceFolder.listFiles()) {
        Path sourcePath = f.toPath().toAbsolutePath();
        Path targetPath = destInstanceFolder.toPath().resolve(f.getName());
        try {
          Files.walkFileTree(sourcePath, new Mover.CopyFileVisitor(
              sourcePath, targetPath, preserved));
        } catch (IOException e) {
          System.err.println("An error occurred while copying " + sourcePath + " !");
        }
      }
    } else {
      System.err.println("[CRITICAL] passed arguments are not directories !");
    }
  }

  /**
   * Deletes recursively a File, including all its sub-contents if it denotes a directory. Be
   * careful !
   *
   * @param file the Path to the folder to be deleted
   */
  private static void deleteRecursively(File file) {
    File[] contents = file.listFiles();
    if (contents != null) {
      for (File f : contents) {
        if (!Files.isSymbolicLink(f.toPath())) {
          deleteRecursively(f);
        }
      }
    }
    //noinspection ResultOfMethodCallIgnored
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
