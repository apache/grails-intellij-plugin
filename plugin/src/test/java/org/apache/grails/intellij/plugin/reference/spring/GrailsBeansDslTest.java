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

package org.apache.grails.intellij.plugin.reference.spring;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.apache.grails.intellij.plugin.spring.GrailsBeansDsl;
import org.apache.grails.intellij.plugin.spring.GrailsResourceBeanExtractor;
import org.jetbrains.plugins.groovy.codeInspection.assignment.GroovyAssignabilityCheckInspection;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Grails 8 compile-time beans DSL ({@code grails.compiler.beans.GrailsBeans}): resolution of the declarations
 * and their qualifier chains, of the shared {@code field(...)}/{@code method(...)} members inside bean bodies, and
 * extraction of the declared beans. The DSL support is independent of the Spring Support plugin.
 */
public class GrailsBeansDslTest extends GrailsTestCase {

  private static final String DSL_BODY = """
        field('suffix', String).value('app.greeting-suffix', '!')
        field(Formatter)
        field('names', List).typeArguments(String)
        method('buildGreeting', String) { String name ->
            "Hello, ${name}${suffix}"
        }

        bean(MyService)
        bean(GREETER, Greeter) { MyService myService ->
            new Greeter(buildGreeting('World').toUpperCase() + formatter.format(suffix) + names.first().trim())
        }
        bean('special', Greeter).primary().lazy().scope('prototype', proxyMode: 'x').conditionalOnMissingBean(name: 'other') {
            new Greeter(suffix.trim())
        }
        bean('provider', Provider, DefaultProvider).conditionalOnProperty('app.enabled', havingValue: 'true').aliases('legacyProvider')
        bean(URLHolder).staticMethod().annotate(Deprecated).annotate(SuppressWarnings, value: 'x').conditionalOnGrailsEnv('development')
        group('optional').conditionalOnClass(name: 'com.example.Missing') {
            field('prefix', String).value('app.prefix')
            bean('optionalGreeter', Greeter) {
                new Greeter(prefix.trim())
            }
        }
    """;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture.addClass("package grails.compiler.beans; public @interface GrailsBeans {}");
    myFixture.addClass("package grails.plugins; public abstract class Plugin {}");
    myFixture.addClass("package grails.boot.config; public class GrailsAutoConfiguration {}");
    myFixture.addClass("package org.grails.testing; public interface GrailsUnitTest {}");

    addSimpleGroovyFile("class MyService {}");
    addSimpleGroovyFile("class Greeter { Greeter(String greeting) {} }");
    addSimpleGroovyFile("class Formatter { String format(String s) { s } }");
    addSimpleGroovyFile("interface Provider {}");
    addSimpleGroovyFile("class DefaultProvider implements Provider {}");
    addSimpleGroovyFile("class URLHolder {}");
  }

  public void testResolveInApplicationClass() {
    PsiFile file = addSimpleGroovyFile("""
      class Application extends grails.boot.config.GrailsAutoConfiguration {
        static final String GREETER = 'greeter'
        def beans = {
      """ + DSL_BODY + """
        }
      }
      """);

    GrailsTestCase.checkResolve(file);
  }

  public void testResolveInPluginDescriptor() {
    PsiFile file = addSimpleGroovyFile("""
      class GreetingGrailsPlugin extends grails.plugins.Plugin {
        static final String GREETER = 'greeter'
        def beans = {
      """ + DSL_BODY + """
        }
      }
      """);

    GrailsTestCase.checkResolve(file);
  }

  public void testResolveInAnnotatedClass() {
    PsiFile file = addSimpleGroovyFile("""
      @grails.compiler.beans.GrailsBeans
      class GreetingBeans {
        static final String GREETER = 'greeter'
        def beans = {
      """ + DSL_BODY + """
        }
      }
      """);

    GrailsTestCase.checkResolve(file);
  }

  public void testResolveInUnitTest() {
    PsiFile file = addSimpleGroovyFile("""
      class GreeterSpec implements org.grails.testing.GrailsUnitTest {
        def beans = {
          bean(Greeter) { new Greeter('test') }
          bean(MyService).primary()
        }
      }
      """);

    GrailsTestCase.checkResolve(file);
  }

  public void testUnresolvedQualifier() {
    PsiFile file = addSimpleGroovyFile("""
      class Application extends grails.boot.config.GrailsAutoConfiguration {
        def beans = {
          bean(MyService).primry()
          field(Formatter).primary()
          group('g').lazy() {
            bean(Greeter) { new Greeter(undeclared) }
          }
        }
      }
      """);

    GrailsTestCase.checkResolve(file, "primry", "primary", "lazy", "undeclared");
  }

  public void testDslNotAvailableInOtherClasses() {
    PsiFile file = addSimpleGroovyFile("""
      class NotAHost {
        def beans = {
          bean(MyService).primary()
        }
      }
      """);

    assertUnresolved(file, "bean", "primary");
  }

  public void testDslNotAvailableOnlyInBeansProperty() {
    PsiFile file = addSimpleGroovyFile("""
      class Application extends grails.boot.config.GrailsAutoConfiguration {
        def other = {
          bean(MyService)
        }
      }
      """);

    assertUnresolved(file, "bean");
  }

  public void testDeclarationsNotAvailableInBeanBodies() {
    PsiFile file = addSimpleGroovyFile("""
      class Application extends grails.boot.config.GrailsAutoConfiguration {
        def beans = {
          bean(Greeter) {
            bean(MyService)
            new Greeter('')
          }
        }
      }
      """);

    List<GrReferenceExpression> beanCalls = ContainerUtil.filter(PsiTreeUtil.findChildrenOfType(file, GrReferenceExpression.class),
                                                                 ref -> "bean".equals(ref.getReferenceName()));
    assertSize(2, beanCalls);
    assertNotNull(beanCalls.get(0).resolve());
    assertNull(beanCalls.get(1).resolve());
  }

  public void testGroupMembersNotSharedWithTopLevel() {
    PsiFile file = addSimpleGroovyFile("""
      class Application extends grails.boot.config.GrailsAutoConfiguration {
        def beans = {
          group('g') {
            field('prefix', String)
          }
          bean(Greeter) { new Greeter(prefix) }
        }
      }
      """);

    GrailsTestCase.checkResolve(file, "prefix");
  }

  public void testSharedMemberTypes() {
    configureBySimpleGroovyFile("""
      class Application extends grails.boot.config.GrailsAutoConfiguration {
        def beans = {
          field('names', List).typeArguments(String)
          method('greeting', String) { int count -> 'x' * count }
          bean(Greeter) { new Greeter(greeting(2) + names<caret>) }
        }
      }
      """);

    PsiElement resolved = resolveAtCaret();
    assertInstanceOf(resolved, PsiVariable.class);
    assertEquals("java.util.List<java.lang.String>", ((PsiVariable)resolved).getType().getCanonicalText());
    assertEquals("'names'", resolved.getNavigationElement().getText());

    GrReferenceExpression greeting = findReference("greeting(2)");
    PsiElement method = greeting.resolve();
    assertInstanceOf(method, PsiMethod.class);
    PsiType returnType = ((PsiMethod)method).getReturnType();
    assertNotNull(returnType);
    assertEquals("java.lang.String", returnType.getCanonicalText());
    assertEquals(1, ((PsiMethod)method).getParameterList().getParametersCount());
    assertEquals("'greeting'", method.getNavigationElement().getText());
  }

  public void testDerivedSharedMemberName() {
    PsiFile file = addSimpleGroovyFile("""
      class Application extends grails.boot.config.GrailsAutoConfiguration {
        def beans = {
          field(Formatter)
          bean(Greeter) { new Greeter(formatter.format('x')) }
        }
      }
      """);

    GrailsTestCase.checkResolve(file);
  }

  public void testHighlighting() {
    myFixture.enableInspections(GroovyAssignabilityCheckInspection.class);
    configureBySimpleGroovyFile("""
      class Application extends grails.boot.config.GrailsAutoConfiguration {
        static final String GREETER = 'greeter'
        def beans = {
      """ + DSL_BODY + """
        }
      }
      """);

    myFixture.checkHighlighting(true, false, true);
  }

  public void testDeclarationCompletion() {
    configureBySimpleGroovyFile("""
      class Application extends grails.boot.config.GrailsAutoConfiguration {
        def beans = {
          <caret>
        }
      }
      """);

    myFixture.completeBasic();
    assertContainsElements(myFixture.getLookupElementStrings(), "bean", "field", "method", "group");
  }

  public void testQualifierCompletion() {
    configureBySimpleGroovyFile("""
      class Application extends grails.boot.config.GrailsAutoConfiguration {
        def beans = {
          bean(MyService).<caret>
        }
      }
      """);

    myFixture.completeBasic();
    assertContainsElements(myFixture.getLookupElementStrings(),
                           "conditionalOnMissingBean", "conditionalOnMissingBeanName", "conditionalOnBean", "conditionalOnProperty",
                           "conditionalOnExpression", "conditionalOnClass", "conditionalOnGrailsEnv", "aliases", "primary", "lazy",
                           "scope", "staticMethod", "typeArguments", "annotate");
  }

  public void testFieldQualifierCompletion() {
    configureBySimpleGroovyFile("""
      class Application extends grails.boot.config.GrailsAutoConfiguration {
        def beans = {
          field('x', String).<caret>
        }
      }
      """);

    myFixture.completeBasic();
    List<String> variants = myFixture.getLookupElementStrings();
    assertContainsElements(variants, "value", "annotate", "typeArguments");
    assertDoesntContain(variants, "primary", "conditionalOnMissingBean");
  }

  public void testSharedMemberCompletion() {
    configureBySimpleGroovyFile("""
      class Application extends grails.boot.config.GrailsAutoConfiguration {
        def beans = {
          field('greetingSuffix', String)
          method('greetingPrefix', String) { 'Hello' }
          bean(Greeter) { new Greeter(greeting<caret>) }
        }
      }
      """);

    myFixture.completeBasic();
    assertContainsElements(myFixture.getLookupElementStrings(), "greetingSuffix", "greetingPrefix");
  }

  public void testBeanDescriptors() {
    addSimpleGroovyFile("class Names { static final String SPECIAL = 'special' }");
    PsiFile file = addSimpleGroovyFile("""
      class Application extends grails.boot.config.GrailsAutoConfiguration {
        static final String GREETER = 'greeter'
        def beans = {
          field('suffix', String)
          method('helper', String) { 'x' }
          bean(MyService)
          bean(URLHolder)
          bean(GREETER, Greeter) { new Greeter('') }
          bean(Names.SPECIAL + 'Greeter', Greeter).primary() { new Greeter('') }
          bean('provider', Provider, DefaultProvider)
          group('g').conditionalOnClass(name: 'x') {
            bean(Formatter)
          }
        }
      }
      """);

    PsiClass application = PsiTreeUtil.findChildOfType(file, PsiClass.class);
    Map<String, String> beans = new HashMap<>();
    for (GrailsResourceBeanExtractor.BeanDescriptor descriptor : GrailsBeansDsl.getBeanDescriptors(application)) {
      PsiType type = descriptor.getType();
      beans.put(descriptor.getName(), type == null ? null : type.getCanonicalText());
    }

    assertEquals(Map.of("myService", "MyService",
                        "URLHolder", "URLHolder",
                        "greeter", "Greeter",
                        "specialGreeter", "Greeter",
                        "provider", "Provider",
                        "formatter", "Formatter"), beans);
  }

  public void testNoBeanDescriptorsWithoutDsl() {
    PsiFile file = addSimpleGroovyFile("""
      class NotAHost {
        def beans = {
          bean(MyService)
        }
      }
      """);

    PsiClass aClass = PsiTreeUtil.findChildOfType(file, PsiClass.class);
    assertEmpty(GrailsBeansDsl.getBeanDescriptors(aClass));
  }

  /**
   * Asserts the named references do not resolve. Not {@link GrailsTestCase#checkResolve}: on Ultimate the Spring
   * plugin's own Groovy bean DSL support claims a call such as {@code bean(Foo)} as a bean declaration, which that
   * check counts as resolved.
   */
  private static void assertUnresolved(PsiFile file, String... names) {
    Set<String> expected = Set.of(names);
    int count = 0;
    for (GrReferenceExpression ref : PsiTreeUtil.findChildrenOfType(file, GrReferenceExpression.class)) {
      if (expected.contains(ref.getReferenceName())) {
        assertNull(ref.getText(), ref.resolve());
        count++;
      }
    }
    assertTrue(count >= names.length);
  }

  private PsiElement resolveAtCaret() {
    PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset() - 1);
    GrReferenceExpression ref = PsiTreeUtil.getParentOfType(element, GrReferenceExpression.class);
    assertNotNull(ref);
    return ref.resolve();
  }

  private GrReferenceExpression findReference(String text) {
    int offset = myFixture.getFile().getText().indexOf(text);
    assertTrue(offset >= 0);
    GrExpression expression = PsiTreeUtil.getParentOfType(myFixture.getFile().findElementAt(offset), GrReferenceExpression.class);
    assertNotNull(expression);
    return (GrReferenceExpression)expression;
  }
}
