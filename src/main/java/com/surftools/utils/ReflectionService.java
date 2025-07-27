/**

The MIT License (MIT)

Copyright (c) 2025, Robert Tykulsker

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
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReflectionService {
  protected String packageName;

  public ReflectionService(String packageName) {
    this.packageName = packageName;
  }

  /**
   * find all classes implementing a given interface
   *
   * @param <T>
   * @param targetInterface
   * @param includeAbstractClasses,
   *          if false, only concrete classes
   * @return
   */
  @SuppressWarnings("unchecked")
  public <T> List<Class<? extends T>> findClassesImplementingInterface(Class<T> targetInterface,
      boolean includeAbstractClasses) {
    var implementingClasses = new ArrayList<Class<? extends T>>();
    var classLoader = Thread.currentThread().getContextClassLoader();
    var packagePath = packageName.replace('.', '/');

    try {
      var resources = classLoader.getResources(packagePath);
      while (resources.hasMoreElements()) {
        var resource = resources.nextElement();
        var dirName = resource.getFile();
        dirName = dirName.replaceAll("%20", " ");
        var directory = new File(dirName);
        if (!directory.exists()) {
          continue;
        }

        // Iterate over files in the directory
        var files = directory.listFiles();
        if (files == null) {
          continue;
        }

        for (var file : files) {
          if (file.isFile() && file.getName().endsWith(".class")) {
            var className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
            try {
              var clazz = Class.forName(className);
              if (targetInterface.isAssignableFrom(clazz) && !targetInterface.equals(clazz)) {
                if (Modifier.isAbstract(clazz.getModifiers()) && !includeAbstractClasses) {
                  continue;
                }
                implementingClasses.add((Class<? extends T>) clazz);
              }
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
              ;
            }
          } // end if isFile
        } // end loop over files in directory

      } // end loop over resources
    } catch (java.io.IOException e) {
      e.printStackTrace();
    }

    Collections.sort(implementingClasses, (c1, c2) -> c1.getName().compareTo(c2.getName()));
    return implementingClasses;
  }
}
