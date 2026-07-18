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

package org.jetbrains.plugins.groovy.grails.gsp;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.intellij.testFramework.UsefulTestCase;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspFile;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;
import org.junit.Assert;

public class GspNamespacePriorityTest extends GrailsTestCase {
  public void testTmplFirst() {
    addController("class CccController {}");

    addTaglib("""
                class MyTagLib {
                  static namespace = "tmpl"
                
                  def xxx = {}
                }
                """);

    addView("ccc/_xxx.gsp", "Template Text");
    configureByView("ccc/a.gsp", """      
      <%@ taglib prefix="tmpl" uri="http://java.sun.com/tmpl" %>
      <tmpl:xxx<caret>/>
      """);

    PsiElement element = myFixture.getElementAtCaret();
    UsefulTestCase.assertInstanceOf(element, GspFile.class);
  }

  public void testTagLibBeforeCustomTagsFirst() {
    addTaglib("""
                class MyTagLib {
                  static namespace = "fmt"
                
                  def xxx = {}
                }
                """);

    configureByView("a.gsp", """
      <%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
      <fmt:<caret>/>
      """);

    LookupElement[] lookup = myFixture.completeBasic();
    Assert.assertNotNull(lookup);
    Assert.assertEquals(0, lookup.length);
  }
}
