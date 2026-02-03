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

package com.surftools.utils.textEditor;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextEditorManager {
  private static final Logger logger = LoggerFactory.getLogger(TextEditorManager.class);

  public ITextEditor getTextEditor(String editorName) {
    if (editorName == null || editorName.isBlank()) {
      logger.debug("null editor name");
      return null;
    }
    final var PREFIXES = List.of("", "com.surftools.utils.textEditor.");
    final var SUFFIXES = List.of("TextEditor", "");

    ITextEditor editor = null;
    for (var prefix : PREFIXES) {
      for (var suffix : SUFFIXES) {
        var className = prefix + editorName + suffix;
        logger.debug("searching for className: " + className);
        try {
          var clazz = Class.forName(className);
          if (clazz != null) {
            editor = (ITextEditor) clazz.getDeclaredConstructor().newInstance();
            logger.debug("found  className: " + className + "(" + editor.getName() + ")");
            return editor;
          }
        } catch (Exception e) {
          ;
        }
      } // end loop over suffixes
    } // end loop over prefixes
    logger.error("Could not find an editor for: " + editorName);
    return null;
  }

}
