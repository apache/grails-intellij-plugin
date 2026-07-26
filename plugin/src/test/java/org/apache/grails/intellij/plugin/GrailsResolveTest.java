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

package org.apache.grails.intellij.plugin;


import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;

import java.io.IOException;

import static org.apache.grails.intellij.lib.testFramework.GrailsTestUtil.getTestRootPath;

/**
 * @author Maxim.Medvedev
 */
public class GrailsResolveTest extends GrailsTestCase {

  @Override
  protected String getTestDataPath() {
    return getTestRootPath("/testdata/grails/resolve/");
  }

  public void testResolvePropertiesFromBelongsTo() {
    configure();
    final PsiElement resolved = myFixture.getElementAtCaret();
    assertInstanceOf(resolved, GrField.class);
    assertEquals("prop", ((GrField)resolved).getName());
  }

  public void testResolvePropertiesFromBelongsTo2() {
    configure();
    final PsiElement resolved = myFixture.getElementAtCaret();
    assertInstanceOf(resolved, PsiField.class);
  }

  public void testResolvePropertiesFromHasMany() {
    configure();
    final PsiElement resolved = myFixture.getElementAtCaret();
    assertInstanceOf(resolved, GrField.class);
    assertEquals("prop2", ((GrField)resolved).getName());
  }

  public void testResolvePropertyFromMappedBy() throws Exception{
    final PsiElement resolved=resolveReference();
    assertInstanceOf(resolved, GrField.class);
    assertEquals("departureAirport", ((GrField)resolved).getName());
  }

  public void testStaticsOk() {
    final PsiReference ref = findReference();
    assertTrue(ref instanceof GrReferenceExpression);
    final GroovyResolveResult resolveResult = ((GrReferenceExpression)ref).advancedResolve();
    assertTrue(resolveResult.isStaticsOK());
  }

  private void configure() {
    myFixture
      .configureFromExistingVirtualFile(myFixture.copyFileToProject(getTestName(false) + ".groovy", "grails-app/domain/MyDomain.groovy"));
  }

  @Nullable
  private PsiReference findReference() {
    configure();
    return TargetElementUtil.findReference(myFixture.getEditor());
  }

  @Nullable
  private PsiElement resolveReference() throws IOException {
    final PsiReference ref = findReference();
    if (ref == null) {
      System.out.println(myFixture.getEditor().getCaretModel().getOffset());
      if (StringUtil.isEmpty(myFixture.getEditor().getDocument().getText().trim())) {
        System.out.println("Empty file: " + myFixture.getFile().getName());
        System.out.println("Empty file: " + myFixture.getFile().getVirtualFile().getName());
        System.out.println("Empty file: " + VfsUtilCore.loadText(myFixture.getFile().getVirtualFile()));
      }
    }
    assertNotNull(ref);
    return ref.resolve();
  }

  public void testResolveNamespace() {
    PsiFile ccc = addController("""
                                  class CccController {
                                    def index() {
                                      g()
                                      sitemesh
                                      sitemesh.captureBody()
                                    }
                                  }
                                  """);

    checkResolve(ccc, "g", "sitemesh");
  }
}
