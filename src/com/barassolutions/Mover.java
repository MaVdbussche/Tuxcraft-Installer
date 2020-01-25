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
   * Simple method copying a file from one place to another.
   *
   * @param replace True if copying, False if moving
   * @param source  a valid file
   * @param dest    a valid destination folder
   */
  static void moveFile(boolean replace, Path source, Path dest) {
    try {
      List<CopyOption> options = new ArrayList<>();
      if (replace) {
        options.add(StandardCopyOption.REPLACE_EXISTING);
      }
      if (Main.Values.copyAttributes) {
        options.add(StandardCopyOption.COPY_ATTRIBUTES);
      }

      Files.copy(source, dest, options.toArray(new CopyOption[0]));
    } catch (IOException e) {
      System.err.println(
          "An IOException error occurred during the actual copy/move of the file : " + e
              .getMessage());
      e.printStackTrace();
      System.out.println("The copy/move of file \"" + source.toString()
          + "\" has probably failed due to an IOException error.");
    }
  }
}
