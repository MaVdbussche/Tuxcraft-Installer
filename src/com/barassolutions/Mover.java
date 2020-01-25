package com.barassolutions;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;
import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import com.barassolutions.Main.Values;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemLoopException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Mover {


  private static boolean isWhitelisted(Path elem, Set<Path> set) {
    return set.contains(elem);
  }

  /**
   * Simple FileVisitor implementation for our use-case.
   *
   * @author code inspired by https://docs.oracle.com/javase/tutorial/essential/io/examples/Copy.java
   */
  static class CopyFileVisitor extends SimpleFileVisitor<Path> {

    private final Path targetPath;
    private Path sourcePath;
    private final Set<Path> preserved;
    private final boolean preserve;

    public CopyFileVisitor(Path sourcePath, Path targetPath, Set<Path> preservedFiles) {
      this.sourcePath = sourcePath;
      this.targetPath = targetPath;
      this.preserved = preservedFiles;
      this.preserve = Values.copyAttributes;
    }

    @Override
    public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) {
      /*Before visiting entries in a directory, we check if we should skip it.
       * If not, we create it (if it does not already exist).*/
      if (isWhitelisted(sourcePath.relativize(dir), preserved)) {
        /*We need to skip this one !*/
        return SKIP_SUBTREE;
      } else {
        CopyOption[] options = (preserve)
            ? new CopyOption[]{COPY_ATTRIBUTES} : new CopyOption[0];
        Path newDir = targetPath.resolve(sourcePath.relativize(dir));
        try {
          Files.copy(dir, newDir, options);
        } catch (FileAlreadyExistsException ignored) {
          //ignored
        } catch (IOException e) {
          System.err.format("Unable to create: %s: %s%n", newDir, e);
          return SKIP_SUBTREE;
        }
        return CONTINUE;
      }
    }

    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
      /*Only copying a file if it is not in the whitelist.
       * In any other case, we still want to override existing files !*/
      Path target = targetPath.resolve(sourcePath.relativize(file));
      if (Files.notExists(target) || !isWhitelisted(sourcePath.relativize(file), preserved)) {
        CopyOption[] options = new CopyOption[]{REPLACE_EXISTING};
        try {
          Files.copy(file, target, options);
        } catch (IOException e) {
          System.err.format("Unable to copy: %s: %s%n", file, e);
        }
      }
      return CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
      /*Fixing up modification time of directory when done*/
      if (exc == null && preserve) {
        Path newDir = targetPath.resolve(sourcePath.relativize(dir));
        try {
          FileTime time = Files.getLastModifiedTime(dir);
          Files.setLastModifiedTime(newDir, time);
        } catch (IOException e) {
          System.err.format("Unable to copy all attributes to: %s: %s%n", newDir, e);
        }
      }
      return CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
      if (exc instanceof FileSystemLoopException) {
        System.err.println("cycle detected: " + file);
      } else {
        System.err.format("Unable to copy: %s: %s%n", file, exc);
      }
      return CONTINUE;
    }
  }
}
