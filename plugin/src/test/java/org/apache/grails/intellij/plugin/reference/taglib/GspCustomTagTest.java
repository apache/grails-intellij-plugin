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

package org.apache.grails.intellij.plugin.reference.taglib;

import com.intellij.openapi.util.RecursionManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.UsefulTestCase;
import org.apache.grails.intellij.plugin.lang.gsp.resolve.taglib.TagLibNamespaceDescriptor;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;

public class GspCustomTagTest extends GrailsTestCase {
  public void testRenameTagFromTagLib() {
    PsiFile gspFile =
      myFixture.addFileToProject("grails-app/views/page.gsp", "<% out << customTag(); out << g.customTag() %> <g:customTag/>");

    String path = "grails-app/taglib/MyTagLib.groovy";
    PsiFile tagLibFile = myFixture.addFileToProject(path, "class MyTagLib {def customTag<caret> = {render 'customTag'}}");
    myFixture.configureByFile(path);
    myFixture.renameElementAtCaret("customTag2");

    assertEquals("<% out << customTag2(); out << g.customTag2() %> <g:customTag2/>", gspFile.getText());
    assertEquals("class MyTagLib {def customTag2 = {render 'customTag'}}", tagLibFile.getText());
  }

  public void testRenameTagFromGsp() {
    PsiFile tagLibFile = addTaglib("class MyTagLib {def customTag = {render 'customTag'}}");

    PsiFile gspFile =
      myFixture.addFileToProject("grails-app/views/page.gsp", "<% out << customTag<caret>(); out << g.customTag() %> <g:customTag/>");
    PsiFile gspFile2 =
      myFixture.addFileToProject("grails-app/views/page2.gsp", "<% out << customTag(); out << g.customTag() %> <g:customTag/>");
    myFixture.configureFromExistingVirtualFile(gspFile.getVirtualFile());

    myFixture.renameElementAtCaret("customTag2");

    assertEquals("<% out << customTag2(); out << g.customTag2() %> <g:customTag2/>", gspFile.getText());
    assertEquals("<% out << customTag2(); out << g.customTag2() %> <g:customTag2/>", gspFile2.getText());
    assertEquals("class MyTagLib {def customTag2 = {render 'customTag'}}", tagLibFile.getText());
  }

  public void testRenameTagFromGsp2() {
    PsiFile tagLibFile = addTaglib("class MyTagLib {def customTag = {render 'customTag'}}");

    PsiFile gspFile =
      myFixture.addFileToProject("grails-app/views/page.gsp", "<% out << customTag(); out << g.customTag() %> <g:customTag<caret>/>");
    PsiFile gspFile2 =
      myFixture.addFileToProject("grails-app/views/page2.gsp", "<% out << customTag(); out << g.customTag() %> <g:customTag/>");
    myFixture.configureFromExistingVirtualFile(gspFile.getVirtualFile());

    myFixture.renameElementAtCaret("customTag2");

    assertEquals("<% out << customTag2(); out << g.customTag2() %> <g:customTag2/>", gspFile.getText());
    assertEquals("<% out << customTag2(); out << g.customTag2() %> <g:customTag2/>", gspFile2.getText());
    assertEquals("class MyTagLib {def customTag2 = {render 'customTag'}}", tagLibFile.getText());
  }

  public void testRenameTagWithPrefix() {
    PsiFile tagLibFile = addTaglib("class MyTagLib { static namespace='xxx'; def customTag = {render 'customTag'}}");

    PsiFile pageFile =
      myFixture.addFileToProject("grails-app/views/page.gsp", "<% out << customTag(); out << xxx.customTag<caret>() %> <xxx:customTag/>");
    PsiFile pageFile2 =
      myFixture.addFileToProject("grails-app/views/page2.gsp", "<% out << customTag(); out << xxx.customTag() %> <xxx:customTag/>");
    myFixture.configureFromExistingVirtualFile(pageFile.getVirtualFile());

    myFixture.renameElementAtCaret("customTag2");

    assertEquals("<% out << customTag(); out << xxx.customTag2() %> <xxx:customTag2/>", pageFile.getText());
    assertEquals("<% out << customTag(); out << xxx.customTag2() %> <xxx:customTag2/>", pageFile2.getText());
    assertEquals("class MyTagLib { static namespace='xxx'; def customTag2 = {render 'customTag'}}", tagLibFile.getText());
  }

  public void testRecursion() {
    RecursionManager.disableMissedCacheAssertions(getTestRootDisposable());
    PsiFile file = configureByTaglib("""
                                       
                                       class MyTagLib {
                                         def xxx = xx<caret>x();
                                       }
                                       """);

    int caret = myFixture.getEditor().getCaretModel().getOffset();
    GrReferenceExpression reference = PsiTreeUtil.getParentOfType(file.findElementAt(caret), GrReferenceExpression.class);
    assertNotNull(reference);
    assertNotNull(reference.resolve());
  }

  public void testRecursion2() {
    addTaglib("""
                
                class My1TagLib {
                  public def getXxx1() {
                    return { render "xxx" }
                  }
                
                  def xxx2 = xxx1
                
                  public def getXxx3() {
                    return xxx2
                  }
                
                  def xxx0 = w.xxx0
                }
                """);

    addTaglib("""
                
                class My2TagLib {
                  static namespace = "w"
                
                  public def getXxx1() {
                    g.xxx3
                  }
                
                  def xxx2 = xxx1;
                
                  def xxx0 = g.xxx0
                }
                """);

    PsiFile a = myFixture.addFileToProject("grails-app/views/a.gsp", "<g:xxx<caret>");
    checkCompletionVariants(a, "xxx1", "xxx2", "xxx3");

    PsiFile b = myFixture.addFileToProject("grails-app/views/b.gsp", "<w:xxx<caret>");
    checkCompletionVariants(b, "xxx1", "xxx2");
  }

  public void testFindNamespaceFieldInParent() throws Exception {
    addSimpleGroovyFile("class Parent { static namespace = 'nnn' }");
    addTaglib("""
                
                class MyTagLib extends Parent {
                  def xxx = {}
                }
                """);

    configureByView("a.gsp", "<nnn:xxx<caret>/>");
    PsiElement element = myFixture.getElementAtCaret();
    UsefulTestCase.assertInstanceOf(element, TagLibNamespaceDescriptor.GspTagMethod.class);
  }

  public void testEmptyNamespaceField() {
    addTaglib("""
                
                class MyTagLib {
                  static namespace = "  "
                
                  def xxx = {}
                }
                """);
    configureByView("a.gsp", "<g:xxx<caret>/>");
    PsiElement element = myFixture.getElementAtCaret();

    UsefulTestCase.assertInstanceOf(element, TagLibNamespaceDescriptor.GspTagMethod.class);
  }

  public void testNamespaceFieldHasSpaces() {
    addTaglib("""
                
                class MyTagLib {
                  static namespace = " sss "
                
                  def xxx = {}
                }
                """);
    configureByView("a.gsp", "<sss:xxx<caret>/>");
    PsiElement element = myFixture.getElementAtCaret();

    UsefulTestCase.assertInstanceOf(element, TagLibNamespaceDescriptor.GspTagMethod.class);
  }
}
