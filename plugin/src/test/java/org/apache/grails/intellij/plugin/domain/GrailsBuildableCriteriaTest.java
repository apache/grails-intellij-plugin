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

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.testFramework.UsefulTestCase;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Since GORM 4, {@code createCriteria()} returns {@code BuildableCriteria} instead of
 * {@code HibernateCriteriaBuilder}, so out of the box only the closure-terminal calls that interface
 * declares itself ({@code get}, {@code list}, ...) resolve. The GORM libraries the other tests run against
 * are older than that, hence the stubs below for the pieces of a GORM 5 classpath the plugin looks at.
 */
public class GrailsBuildableCriteriaTest extends GrailsTestCase {
  /**
   * The bundled GORM is older than 4, and so is its {@code grails.orm.HibernateCriteriaBuilder}: it does not
   * implement {@code BuildableCriteria}. That hierarchy is what CriteriaBuilderUtil.isCriteriaBuilderMethod()
   * keys off, so the builder is stubbed below instead of taken from the library.
   */
  @Override
  protected boolean needGormLibrary() {
    return false;
  }

  /** GormTraitContributor only picks GormEntity when {@code org.hibernate.Hibernate} is on the classpath. */
  @Override
  protected boolean needHibernate() {
    return true;
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    myFixture.addFileToProject("src/java/org/grails/datastore/mapping/query/api/Criteria.java", """
      package org.grails.datastore.mapping.query.api;

      import groovy.lang.Closure;

      public interface Criteria {
        Criteria eq(String propertyName, Object value);
        Criteria ge(String propertyName, Object value);
        Criteria order(String propertyName, String direction);
        Criteria and(Closure callable);
      }
      """);

    myFixture.addFileToProject("src/java/org/grails/datastore/mapping/query/api/BuildableCriteria.java", """
      package org.grails.datastore.mapping.query.api;

      import groovy.lang.Closure;

      public interface BuildableCriteria extends Criteria {
        Object get(Closure callable);
        Object list(Closure callable);
        Object listDistinct(Closure callable);
        Object scroll(Closure callable);
      }
      """);

    myFixture.addFileToProject("src/java/grails/orm/HibernateCriteriaBuilder.java", """
      package grails.orm;

      import groovy.lang.Closure;
      import org.grails.datastore.mapping.query.api.BuildableCriteria;
      import org.grails.datastore.mapping.query.api.Criteria;

      public class HibernateCriteriaBuilder implements BuildableCriteria {
        public Criteria eq(String propertyName, Object value) { return this; }
        public Criteria ge(String propertyName, Object value) { return this; }
        public Criteria order(String propertyName, String direction) { return this; }
        public Criteria and(Closure callable) { return this; }
        public Object get(Closure callable) { return null; }
        public Object list(Closure callable) { return null; }
        public Object listDistinct(Closure callable) { return null; }
        public Object scroll(Closure callable) { return null; }
      }
      """);

    // GormVersion.IS_5 is the lowest version GormTraitContributor injects the trait for.
    myFixture.addFileToProject("src/java/grails/gorm/annotation/Entity.java", """
      package grails.gorm.annotation;

      public @interface Entity {
      }
      """);

    myFixture.addFileToProject("src/groovy/org/grails/datastore/gorm/GormEntity.groovy", """
      package org.grails.datastore.gorm

      import org.grails.datastore.mapping.query.api.BuildableCriteria

      trait GormEntity<D> {
        static BuildableCriteria createCriteria() { null }
        static Object withCriteria(Closure callable) { null }
      }
      """);

    addDomain("""

                class Ddd {
                  String aaa
                  String bbb
                }
                """);
  }

  /**
   * {@code count} exists only in {@code AbstractHibernateCriteriaBuilder.invokeMethod(...)}, so it has to be
   * contributed to {@code BuildableCriteria} explicitly.
   */
  public void testResolveCountCall() {
    PsiFile file = myFixture.addFileToProject("src/groovy/Ggg.groovy", """

      class Ggg {
        void someMethod() {
          Ddd.createCriteria().cou<caret>nt {
            ge('aaa', 'bbb')
          }
        }
      }
      """);

    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());

    PsiElement elementAtCaret = myFixture.getElementAtCaret();
    UsefulTestCase.assertInstanceOf(elementAtCaret, PsiMethod.class);
    assertEquals("count", ((PsiMethod)elementAtCaret).getName());
  }

  /**
   * Resolving {@code count} is what lets CriteriaBuilderUtil.checkCriteriaClosure() find the domain class of
   * the closure, which in turn is what makes the properties inside it navigable.
   */
  public void testNavigateToPropertyInsideCountClosure() {
    PsiFile file = myFixture.addFileToProject("src/groovy/Ggg.groovy", """

      class Ggg {
        void someMethod() {
          Ddd.createCriteria().count {
            ge('aa<caret>a', 'bbb')
          }
        }
      }
      """);

    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());

    PsiElement elementAtCaret = myFixture.getElementAtCaret();
    UsefulTestCase.assertInstanceOf(elementAtCaret, GrField.class);
    assertEquals("aaa", ((GrField)elementAtCaret).getName());
  }

  /**
   * Every closure-terminal form has to end up with the same delegate, whether the method is contributed
   * ({@code count}, {@code call}) or declared by BuildableCriteria itself ({@code get}, {@code list}, ...).
   */
  public void testNavigateToPropertyInsideEveryTerminalClosure() {
    PsiFile file = myFixture.addFileToProject("src/groovy/Ggg.groovy", """

      class Ggg {
        void someMethod() {
          def c = Ddd.createCriteria()

          Ddd.createCriteria().count { ge('aaa', 'x') }
          Ddd.createCriteria().get { ge('aaa', 'x') }
          Ddd.createCriteria().list { ge('aaa', 'x') }
          Ddd.createCriteria().listDistinct { ge('aaa', 'x') }
          Ddd.createCriteria().scroll { ge('aaa', 'x') }
          c { ge('aaa', 'x') }
          c({ ge('aaa', 'x') })
        }
      }
      """);

    String text = file.getText();
    List<String> unresolvedForms = new ArrayList<>();
    int propertyCount = 0;

    for (int i = text.indexOf("'aaa'"); i >= 0; i = text.indexOf("'aaa'", i + 1)) {
      PsiReference reference = file.findReferenceAt(i + 1);
      PsiElement resolved = reference == null ? null : reference.resolve();

      if (!(resolved instanceof GrField field) || !"aaa".equals(field.getName())) {
        int lineStart = text.lastIndexOf('\n', i) + 1;
        int lineEnd = text.indexOf('\n', i);
        unresolvedForms.add(text.substring(lineStart, lineEnd < 0 ? text.length() : lineEnd).trim());
      }

      propertyCount++;
    }

    assertEquals("Not every terminal form was checked", 7, propertyCount);
    UsefulTestCase.assertEmpty("The property does not resolve in these forms", unresolvedForms);
  }

  public void testCountReturnsInteger() {
    PsiFile file = myFixture.addFileToProject("src/groovy/Ggg.groovy", """

      class Ggg {
        void someMethod() {
          Ddd.createCriteria().count {
            ge('aaa', 'bbb')
          }.intValue()
        }
      }
      """);

    GrailsTestCase.checkResolve(file);
  }

  /**
   * {@code call}/{@code doCall} back the {@code def c = Ddd.createCriteria(); c { ... }} form and are
   * dynamic in the very same way {@code count} is.
   */
  public void testShorthandCallForm() {
    PsiFile file = myFixture.addFileToProject("src/groovy/Ggg.groovy", """

      class Ggg {
        void someMethod() {
          def c = Ddd.createCriteria()

          String withTrailingClosure = c { ge('aaa', 'bbb') } .get(0).aaa
          String withClosureArgument = c({ ge('aaa', 'bbb') }).get(0).aaa
        }
      }
      """);

    GrailsTestCase.checkResolve(file);
  }

  /**
   * The forms that already worked before {@code count} was contributed must keep resolving to a single,
   * unambiguous method.
   */
  public void testDeclaredMemberStillResolvesUnambiguously() {
    PsiFile file = myFixture.addFileToProject("src/groovy/Ggg.groovy", """

      class Ggg {
        void someMethod() {
          Ddd.createCriteria().ge<caret>t {
            ge('aaa', 'bbb')
          }
        }
      }
      """);

    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());

    PsiReference reference = file.findReferenceAt(myFixture.getCaretOffset());
    UsefulTestCase.assertInstanceOf(reference, GrReferenceExpression.class);

    GroovyResolveResult[] results = ((GrReferenceExpression)reference).multiResolve(false);
    UsefulTestCase.assertSize(1, results);
    UsefulTestCase.assertInstanceOf(results[0].getElement(), PsiMethod.class);
  }

  /**
   * Contributing a member BuildableCriteria declares itself does not make resolution ambiguous — the Groovy
   * resolver just picks one candidate — but it does list the member twice in completion, so this is the check
   * that catches it. Completion is also the case with no name hint to narrow the contributed members by.
   */
  public void testCompletionListsEveryMemberOnce() {
    PsiFile file = myFixture.addFileToProject("src/groovy/Ggg.groovy", """

      class Ggg {
        void someMethod() {
          Ddd.createCriteria().<caret>
        }
      }
      """);

    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    myFixture.complete(CompletionType.BASIC);

    List<String> variants = myFixture.getLookupElementStrings();
    assertNotNull(variants);

    // 'count' is contributed, 'get' and 'list' are declared by BuildableCriteria itself.
    for (String member : new String[]{"count", "get", "list"}) {
      assertEquals("Unexpected number of '" + member + "' variants in " + variants,
                   1, Collections.frequency(variants, member));
    }
  }
}
