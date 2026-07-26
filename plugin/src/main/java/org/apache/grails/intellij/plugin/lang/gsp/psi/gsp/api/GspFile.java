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

package org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.api;

import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.xml.XmlFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.lang.gsp.GspDirectiveKind;
import org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.api.directive.GspDirective;
import org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.api.gtag.GspXmlRootTag;
import org.apache.grails.intellij.plugin.lang.gsp.psi.html.impl.GspHtmlFileImpl;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFileBase;
import org.jetbrains.plugins.groovy.lang.psi.api.toplevel.imports.GrImportStatement;

import java.util.List;

public interface GspFile extends XmlFile {

  GroovyFileBase getGroovyLanguageRoot();

  List<GspDirective> getDirectiveTags(GspDirectiveKind directiveKind, boolean searchInIncludes);

  void addImportForClass(PsiClass aClass) throws IncorrectOperationException;

  void addImportStatement(GrImportStatement statement);

  PsiElement createGroovyScriptletFromText(String text) throws IncorrectOperationException;

  @Override
  GspXmlRootTag getRootTag();

  @Override
  @NotNull
  FileViewProvider getViewProvider();

  boolean processJsInJavascriptTags(@NotNull PsiScopeProcessor processor, @NotNull ResolveState state, @NotNull PsiElement place);

  GspHtmlFileImpl getHtmlLanguageRoot();
}
