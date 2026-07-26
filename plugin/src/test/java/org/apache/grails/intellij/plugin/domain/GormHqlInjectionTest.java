/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.grails.intellij.plugin.domain;

import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral;

import java.util.List;

/**
 * Verifies {@link org.apache.grails.intellij.plugin.references.domain.GormHqlInjector}: the query string
 * of a GORM {@code find}/{@code findAll}/{@code executeQuery}/{@code executeUpdate} call on a domain
 * class receives an HQL language injection (which is what makes navigation and rename of domain
 * classes/properties work from inside the query string).
 */
public class GormHqlInjectionTest extends GrailsTestCase {

  private static final String HQL_LANGUAGE_ID = "HQL";

  public void testExecuteQueryIsInjected() {
    addDomain("class Book { String title }");
    PsiFile file = configureByDomain("""
                                       class Library {
                                         def all() {
                                           Book.executeQuery("from Book b where b.title = 'x'")
                                         }
                                       }
                                       """);
    assertInjectedLanguage(file, "from Book b where b.title = 'x'", HQL_LANGUAGE_ID);
  }

  public void testFindIsInjected() {
    addDomain("class Book { String title }");
    PsiFile file = configureByDomain("""
                                       class Library {
                                         def one() {
                                           Book.find("from Book b where b.title = ?", ['x'])
                                         }
                                       }
                                       """);
    assertInjectedLanguage(file, "from Book b where b.title = ?", HQL_LANGUAGE_ID);
  }

  public void testUnqualifiedCallInsideDomainIsInjected() {
    PsiFile file = configureByDomain("""
                                       class Book {
                                         String title
                                         static run() {
                                           findAll("from Book b where b.title = 'x'")
                                         }
                                       }
                                       """);
    assertInjectedLanguage(file, "from Book b where b.title = 'x'", HQL_LANGUAGE_ID);
  }

  public void testNonQueryMethodIsNotInjected() {
    addDomain("class Book { String title }");
    PsiFile file = configureByDomain("""
                                       class Library {
                                         def go() {
                                           Book.doSomething("from Book b where b.title = 'x'")
                                         }
                                       }
                                       """);
    assertNoInjection(file, "from Book b where b.title = 'x'");
  }

  public void testCallOnNonDomainIsNotInjected() {
    addSimpleGroovyFile("class NotADomain { static executeQuery(String q) {} }");
    PsiFile file = configureByDomain("""
                                       class Library {
                                         def go() {
                                           NotADomain.executeQuery("from Book b where b.title = 'x'")
                                         }
                                       }
                                       """);
    assertNoInjection(file, "from Book b where b.title = 'x'");
  }

  private void assertInjectedLanguage(PsiFile file, String literalContent, String expectedLanguageId) {
    GrLiteral literal = findStringLiteral(file, literalContent);
    List<Pair<PsiElement, TextRange>> injected =
      InjectedLanguageManager.getInstance(getProject()).getInjectedPsiFiles(literal);
    assertNotNull("Expected an injected language in " + literalContent, injected);
    assertFalse("Expected an injected language in " + literalContent, injected.isEmpty());
    assertEquals(expectedLanguageId, injected.get(0).first.getContainingFile().getLanguage().getID());
  }

  private void assertNoInjection(PsiFile file, String literalContent) {
    GrLiteral literal = findStringLiteral(file, literalContent);
    List<Pair<PsiElement, TextRange>> injected =
      InjectedLanguageManager.getInstance(getProject()).getInjectedPsiFiles(literal);
    assertTrue("Expected no injection in " + literalContent, injected == null || injected.isEmpty());
  }

  private static GrLiteral findStringLiteral(PsiFile file, String content) {
    for (GrLiteral literal : PsiTreeUtil.findChildrenOfType(file, GrLiteral.class)) {
      Object value = literal.getValue();
      if (value instanceof String && value.equals(content)) {
        return literal;
      }
    }
    throw new AssertionError("String literal not found: " + content);
  }
}
