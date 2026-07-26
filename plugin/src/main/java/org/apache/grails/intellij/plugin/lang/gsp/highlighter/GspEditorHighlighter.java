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

package org.apache.grails.intellij.plugin.lang.gsp.highlighter;

import com.intellij.lang.html.HTMLLanguage;
import com.intellij.openapi.editor.JspHighlighterColors;
import com.intellij.openapi.editor.XmlHighlighterColors;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.util.LayerDescriptor;
import com.intellij.openapi.editor.ex.util.LayeredLexerEditorHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.templateLanguages.TemplateDataHighlighterWrapper;
import org.apache.grails.intellij.plugin.lang.gsp.lexer.GspTokenTypesEx;
import org.apache.grails.intellij.plugin.lang.gsp.lexer.core.GspTokenTypes;
import org.jetbrains.plugins.groovy.highlighter.GroovySyntaxHighlighter;

public class GspEditorHighlighter extends LayeredLexerEditorHighlighter {
  public GspEditorHighlighter(EditorColorsScheme scheme, Project project, VirtualFile virtualFile) {
    super(new GspSyntaxHighlighter(), scheme);

    // Register Groovy Highlighter
    SyntaxHighlighter groovyHighlighter = new GroovySyntaxHighlighter();
    final LayerDescriptor groovyLayer = new LayerDescriptor(groovyHighlighter, "\n", JspHighlighterColors.JSP_SCRIPTING_BACKGROUND);
    registerLayer(GspTokenTypes.GROOVY_CODE, groovyLayer);
    registerLayer(GspTokenTypes.GROOVY_EXPR_CODE, groovyLayer);
    registerLayer(GspTokenTypes.GSP_MAP_ATTR_VALUE, groovyLayer);
    registerLayer(GspTokenTypes.GROOVY_ATTR_VALUE, groovyLayer);
    registerLayer(GspTokenTypes.GROOVY_DECLARATION, groovyLayer);

    // Register html highlighter
    SyntaxHighlighter htmlHighlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(HTMLLanguage.INSTANCE, project, virtualFile);
    final LayerDescriptor htmlLayer = new LayerDescriptor(new TemplateDataHighlighterWrapper(htmlHighlighter), "\n", XmlHighlighterColors.HTML_TAG);
    registerLayer(GspTokenTypesEx.GSP_TEMPLATE_DATA, htmlLayer);

    final SyntaxHighlighter directiveHighlighter = new GspDirectiveHighlighter();
    final LayerDescriptor directiveLayer = new LayerDescriptor(directiveHighlighter, "\n", JspHighlighterColors.JSP_ACTION_AND_DIRECTIVE_BACKGROUND);
    registerLayer(GspTokenTypes.GSP_DIRECTIVE, directiveLayer);
  }
}
