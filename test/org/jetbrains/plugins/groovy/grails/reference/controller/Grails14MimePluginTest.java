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

package org.jetbrains.plugins.groovy.grails.reference.controller;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.PsiTestUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.grails.Grails14TestCase;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;
import org.jetbrains.plugins.groovy.grails.GrailsTestUtil;

public class Grails14MimePluginTest extends Grails14TestCase {
  @Override
  protected void configureGrails(@NotNull Module module, @NotNull ModifiableRootModel model, ContentEntry contentEntry) {
    super.configureGrails(module, model, contentEntry);
    PsiTestUtil.addLibrary(model, "MimeTypes", GrailsTestUtil.getMockGrails14LibraryHome(), "grails-plugin-mimetypes-1.4.0.M1.jar");
  }

  public void testResolve() {
    PsiFile file = addController("""
                                   class CccController {
                                     def index = {
                                       withFormat {
                                         html {}
                                         js {}
                                         unresolved(1, 2)
                                       }
                                     }
                                   }
                                   """);
    GrailsTestCase.checkResolve(file, "unresolved");
  }

  public void testWithFormatReturnType() {
    PsiFile file = addController("""
                                   class CccController {
                                     def index = {
                                       withFormat {
                                       }.<caret>
                                     }
                                   }
                                   """);
    checkCompletion(file, "size", "putAll", "containsKey");
  }
}
