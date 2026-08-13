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
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.UsefulTestCase;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariableDeclaration;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor;

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

    // #CHECK# org.grails.datastore.mapping.query.api.BuildableCriteria
    // The @DelegatesTo annotations are part of the fixture, not decoration: in real GORM they give every
    // terminal closure a Criteria delegate, which is a candidate source competing with everything the
    // criteria contributors put inside those closures. The getTargetClass/cache/readOnly/join/select members
    // the real interface also declares are left out — nothing in the plugin looks at them.
    myFixture.addFileToProject("src/java/org/grails/datastore/mapping/query/api/BuildableCriteria.java", """
      package org.grails.datastore.mapping.query.api;

      import groovy.lang.Closure;
      import groovy.lang.DelegatesTo;

      import java.util.Map;

      public interface BuildableCriteria extends Criteria {
        Object get(@DelegatesTo(Criteria.class) Closure callable);
        Object list(@DelegatesTo(Criteria.class) Closure callable);
        Object list(Map params, @DelegatesTo(Criteria.class) Closure callable);
        Object listDistinct(@DelegatesTo(Criteria.class) Closure callable);
        Object scroll(@DelegatesTo(Criteria.class) Closure callable);
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
        public Object list(java.util.Map params, Closure callable) { return null; }
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
   * The {@code list(Map, Closure)} overload is real on BuildableCriteria too, so the paginated form belongs
   * here rather than in a test of its own.
   */
  public void testNavigateToPropertyInsideEveryTerminalClosure() {
    PsiFile file = myFixture.addFileToProject("src/groovy/Ggg.groovy", """

      class Ggg {
        void someMethod() {
          def c = Ddd.createCriteria()

          Ddd.createCriteria().count { ge('aaa', 'x') }
          Ddd.createCriteria().get { ge('aaa', 'x') }
          Ddd.createCriteria().list { ge('aaa', 'x') }
          Ddd.createCriteria().list(max: 10) { ge('aaa', 'x') }
          Ddd.createCriteria().listDistinct { ge('aaa', 'x') }
          Ddd.createCriteria().scroll { ge('aaa', 'x') }
          c { ge('aaa', 'x') }
          c({ ge('aaa', 'x') })
        }
      }
      """);

    // Covers the terminal calls themselves, which the property loop below says nothing about: without this
    // listDistinct, scroll and list(Map, Closure) are not asserted to resolve anywhere in the class.
    GrailsTestCase.checkResolve(file);

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

    assertEquals("Not every terminal form was checked", 8, propertyCount);
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
   * {@code call} backs the {@code def c = Ddd.createCriteria(); c { ... }} form — Groovy's implicit-call
   * resolution looks for {@code call}, not {@code doCall} — and is dynamic in the very same way
   * {@code count} is.
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

    // 'count' is contributed, 'get' is declared by BuildableCriteria itself.
    for (String member : new String[]{"count", "get"}) {
      assertEquals("Unexpected number of '" + member + "' variants in " + variants,
                   1, Collections.frequency(variants, member));
    }

    // 'list' is the exception: BuildableCriteria really declares both list(Closure) and list(Map, Closure),
    // so two variants are the two overloads rather than a member contributed on top of a declared one.
    assertEquals("Expected exactly the two real 'list' overloads in " + variants,
                 2, Collections.frequency(variants, "list"));
  }

  /**
   * Since GORM 4 HibernateCriteriaBuilder implements BuildableCriteria, so a qualifier typed as the builder
   * matches both contributors and BuildableCriteriaImplicitMemberContributor bails out on it — the builder is
   * already served by CriteriaBuilderImplicitMemberContributor.
   *
   * <p>Resolution and completion cannot observe that guard: both contributors hand the same
   * {@code CLASS_SOURCE} to DynamicMemberUtils, which caches its synthetic class per source string, so the
   * duplicate is the very same PsiMethod instance and both dedupe it by element. Nothing in the plugin
   * enforces that shared-{@code CLASS_SOURCE} invariant though, hence the guard — and hence this test, which
   * counts contributions through a bare {@link PsiScopeProcessor} because that does no deduplication at all.
   */
  public void testBuilderQualifierIsServedByOneContributorOnly() {
    PsiFile file = myFixture.addFileToProject("src/groovy/Hhh.groovy", """

      class Hhh {
        void someMethod(grails.orm.HibernateCriteriaBuilder builder) {
          builder.count { ge('aaa', 'bbb') }
        }
      }
      """);

    assertEquals("'count' was contributed more than once to a HibernateCriteriaBuilder-typed qualifier",
                 1, countContributionsOf("count", file));
  }

  /**
   * The counterpart of {@link #testBuilderQualifierIsServedByOneContributorOnly()}: on a qualifier typed as
   * BuildableCriteria only this contributor applies, so a single contribution here is what proves the
   * counting above sees contributed members at all and cannot pass vacuously.
   */
  public void testBuildableCriteriaQualifierIsServedOnce() {
    PsiFile file = myFixture.addFileToProject("src/groovy/Hhh.groovy", """

      class Hhh {
        void someMethod() {
          Ddd.createCriteria().count { ge('aaa', 'bbb') }
        }
      }
      """);

    assertEquals("'count' was not contributed exactly once to a BuildableCriteria-typed qualifier",
                 1, countContributionsOf("count", file));
  }

  /**
   * The projections-to-result-type logic in CriteriaBuilderUtil.getResultType0() is only reached for methods
   * CriteriaReturnTypeCalculator recognises, i.e. the ones CriteriaBuilderImplicitMemberContributor
   * contributes plus {@code withCriteria}. On a GORM 4+ classpath the shorthand form is what gets there: it
   * resolves to the contributed {@code call}, whose declared {@code List} return type sends the calculator
   * looking for the domain class and the projections block. The GrailsCriteriaProjections* tests cover this
   * logic only on the bundled pre-4 jars, where every terminal is contributed.
   */
  public void testProjectionResultTypeOnShorthandForm() {
    assertVariableType("List<Integer>", """
      def c = Ddd.createCriteria()
      def variable = c {
        projections { countDistinct('aaa') }
      }
      """);

    assertVariableType("List<String>", """
      def c = Ddd.createCriteria()
      def variable = c {
        projections { max('aaa') }
      }
      """);

    // Two projections in one block widen the element type instead of picking the first.
    assertVariableType("List<Object[]>", """
      def c = Ddd.createCriteria()
      def variable = c {
        projections {
          countDistinct('aaa')
          countDistinct('bbb')
        }
      }
      """);

    // A projection on an unknown property falls back to Object rather than failing.
    assertVariableType("List<Object>", """
      def c = Ddd.createCriteria()
      def variable = c {
        projections { max('nonExistent') }
      }
      """);
  }

  /**
   * Known gap, pinned so that closing it shows up as a diff: on a GORM 4+ classpath the terminals
   * BuildableCriteria declares itself resolve to the real interface methods, which
   * CriteriaReturnTypeCalculator does not recognise — it only applies the projections logic to the members
   * CriteriaBuilderImplicitMemberContributor contributes and to {@code withCriteria}. So the declared
   * {@code Object} return type stands and neither the domain class nor the projections block reaches the
   * result type. This is about return typing only; resolution and navigation inside those closures work,
   * which is what the rest of this class covers.
   */
  public void testProjectionResultTypeIsNotAppliedToDeclaredTerminals() {
    assertVariableType("Object", """
      def variable = Ddd.createCriteria().list {
        projections { countDistinct('aaa') }
      }
      """);

    assertVariableType("Object", """
      def variable = Ddd.createCriteria().get {
        projections { max('aaa') }
      }
      """);

    assertVariableType("Object", """
      def variable = Ddd.createCriteria().list(max: 10) {
        projections { max('aaa') }
      }
      """);

    // count is contributed rather than declared, so it keeps its Integer return type — the calculator hands
    // the projections block back for List and Object returns only.
    assertVariableType("Integer", """
      def variable = Ddd.createCriteria().count {
        projections { countDistinct('aaa') }
      }
      """);
  }

  /**
   * grails.gorm.CriteriaBuilder is the non-Hibernate implementor of BuildableCriteria (the MongoDB/Neo4j
   * path). It declares its own {@code count(Closure)} and does not inherit HibernateCriteriaBuilder, so the
   * guard in BuildableCriteriaImplicitMemberContributor does not fire for it and the contributed {@code count}
   * meets a real one. The real method has to win, and there has to be exactly one candidate.
   */
  public void testNonHibernateImplementorKeepsItsOwnCount() {
    myFixture.addFileToProject("src/java/grails/gorm/CriteriaBuilder.java", """
      package grails.gorm;

      import groovy.lang.Closure;
      import org.grails.datastore.mapping.query.api.BuildableCriteria;
      import org.grails.datastore.mapping.query.api.Criteria;

      public class CriteriaBuilder implements BuildableCriteria {
        public Criteria eq(String propertyName, Object value) { return this; }
        public Criteria ge(String propertyName, Object value) { return this; }
        public Criteria order(String propertyName, String direction) { return this; }
        public Criteria and(Closure callable) { return this; }
        public Object get(Closure callable) { return null; }
        public Object list(Closure callable) { return null; }
        public Object list(java.util.Map params, Closure callable) { return null; }
        public Object listDistinct(Closure callable) { return null; }
        public Object scroll(Closure callable) { return null; }
        public Number count(Closure callable) { return null; }
      }
      """);

    PsiFile file = myFixture.addFileToProject("src/groovy/Iii.groovy", """

      class Iii {
        void someMethod(grails.gorm.CriteriaBuilder builder) {
          builder.cou<caret>nt { ge('aaa', 'bbb') }
        }
      }
      """);

    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());

    PsiReference reference = file.findReferenceAt(myFixture.getCaretOffset());
    UsefulTestCase.assertInstanceOf(reference, GrReferenceExpression.class);

    GroovyResolveResult[] results = ((GrReferenceExpression)reference).multiResolve(false);
    UsefulTestCase.assertSize(1, results);

    PsiMethod resolved = (PsiMethod)results[0].getElement();
    assertNotNull(resolved);
    assertEquals("count", resolved.getName());

    PsiType returnType = resolved.getReturnType();
    assertNotNull(returnType);
    assertEquals("The contributed 'count' won over the one the class declares", "Number", returnType.getPresentableText());
  }

  /**
   * Puts {@code text} in a file of its own and checks the {@code variable} it declares has {@code type}.
   */
  private void assertVariableType(@NotNull String type, @NotNull String text) {
    PsiFile file = myFixture.addFileToProject("src/groovy/Type" + myTypeCheckCount++ + ".groovy", text);

    GrVariable variable = null;
    for (PsiElement e = file.getFirstChild(); e != null; e = e.getNextSibling()) {
      if (e instanceof GrVariableDeclaration declaration) variable = declaration.getVariables()[0];
    }

    assertNotNull("No variable declaration in " + text, variable);

    PsiType variableType = variable.getTypeGroovy();
    assertNotNull(text, variableType);
    assertEquals(text, type, variableType.getPresentableText());
  }

  private int myTypeCheckCount;

  /**
   * Runs every applicable {@link NonCodeMembersContributor} against the qualifier of {@code .<memberName>}
   * and counts how many times {@code memberName} is contributed.
   */
  private static int countContributionsOf(@NotNull String memberName, @NotNull PsiFile file) {
    int offset = file.getText().indexOf('.' + memberName);
    assertTrue("No '." + memberName + "' call in the file", offset >= 0);

    GrReferenceExpression place = PsiTreeUtil.getParentOfType(file.findElementAt(offset + 1), GrReferenceExpression.class);
    assertNotNull(place);

    GrExpression qualifier = place.getQualifierExpression();
    assertNotNull(qualifier);

    PsiType qualifierType = qualifier.getType();
    assertNotNull("The qualifier has no type, so no contributor would run", qualifierType);

    int[] contributions = {0};

    NonCodeMembersContributor.runContributors(qualifierType, new PsiScopeProcessor() {
      @Override
      public boolean execute(@NotNull PsiElement element, @NotNull ResolveState state) {
        if (element instanceof PsiMethod method && memberName.equals(method.getName())) contributions[0]++;
        return true;
      }
    }, place, ResolveState.initial());

    return contributions[0];
  }
}
