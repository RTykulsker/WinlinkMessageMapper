/**

The MIT License (MIT)

Copyright (c) 2021, Robert Tykulsker

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.


*/

package com.surftools.utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * static methods to support basic file operations
 *
 * @author bobt
 *
 */
public class FileUtils {
  public static void makeDirIfNeeded(String fileName) {
    File outputFile = new File(fileName);
    if (outputFile.isAbsolute()) {
      File outputFilePath = new File(outputFile.getParent());
      if (!outputFilePath.exists()) {
        boolean ok = outputFilePath.mkdirs();
        if (!ok) {
          throw new RuntimeException("file path for: " + fileName + " not found and can't be created");
        }
      }
    }
  }

  /**
   * (recursively) create directory
   *
   * @param path
   */
  public static Path createDirectory(Path path) {
    try {
      return Files.createDirectories(path);
    } catch (Exception e) {
      throw new RuntimeException("exception creating directory: " + path.toString() + ", " + e.getLocalizedMessage());
    }
  }

  /**
   * recursively remove directory and all contents
   *
   * @param path
   */
  public static void deleteDirectory(Path path) {
    try {
      if (Files.exists(path)) {
        Files //
            .walk(path) //
              .map(Path::toFile) //
              .sorted((o1, o2) -> -o1.compareTo(o2)) //
              .forEach(File::delete);
      }
    } catch (Exception e) {
      throw new RuntimeException("exception deleting directory: " + path.toString() + ", " + e.getLocalizedMessage());
    }
  }

  /**
   * return a canonical suffix
   *
   * @param fileName
   * @return
   */
  public static String getFileNameSuffix(String fileName) {
    int index = fileName.lastIndexOf(".");
    if (index == -1) {
      return null;
    }

    String suffix = fileName.substring(index + 1).toUpperCase();
    return suffix;
  }

}
