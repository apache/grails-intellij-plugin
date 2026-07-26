/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.grails.intellij.plugin.lang.gsp.editor.actions;

import com.intellij.application.options.CodeStyle;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.tree.IElementType;
import org.apache.grails.intellij.plugin.fileType.GspFileType;
import org.apache.grails.intellij.plugin.lang.gsp.lexer.core.GspTokenTypes;
import org.jetbrains.plugins.groovy.lang.lexer.GroovyElementType;
import org.jetbrains.plugins.groovy.lang.lexer.GroovyTokenTypes;

public final class GspEditorActionsUtil {

  private GspEditorActionsUtil() {

  }

  public static void insertSpacesByGspIndent(Editor editor, Project project) {
    int indentSize = CodeStyle.getSettings(editor).getIndentSize(GspFileType.GSP_FILE_TYPE);
    EditorModificationUtil.insertStringAtCaret(editor, StringUtil.repeatSymbol(' ', indentSize));
  }

  public static boolean areSciptletSeparatorsUnbalanced(HighlighterIterator iterator) {
    IElementType firstElementType = iterator.getTokenType();
    assert firstElementType == GspTokenTypes.JSCRIPT_BEGIN || firstElementType == GspTokenTypes.GSCRIPT_BEGIN;
    iterator.advance();

    IElementType prev = null;

    while (!iterator.atEnd()) {
      IElementType element = iterator.getTokenType();

      if (element instanceof GroovyElementType) {
        if (element == GroovyTokenTypes.mMOD && prev == GroovyTokenTypes.mLT
            || element == GroovyTokenTypes.mLCURLY && prev == GroovyTokenTypes.mMOD
            || element == GroovyTokenTypes.mMOD_ASSIGN && prev == GroovyTokenTypes.mLT) {
          return true;
        }
      }
      else if (element == GspTokenTypes.JSCRIPT_END || element == GspTokenTypes.GSCRIPT_END || element == GspTokenTypes.JEXPR_BEGIN) {
        return false;
      }

      prev = element;

      iterator.advance();
    }

    return true;
  }

}
