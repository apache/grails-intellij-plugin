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

package org.apache.grails.intellij.plugin.reference.taglib;

import com.intellij.codeInspection.deadCode.UnusedDeclarationInspectionBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.searches.MethodReferencesSearch;
import com.intellij.testFramework.UsefulTestCase;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.apache.grails.intellij.plugin.lang.gsp.resolve.taglib.GspTagLibUtil;
import org.apache.grails.intellij.plugin.lang.gsp.resolve.taglib.GspTagMethodUtil;
import org.apache.grails.intellij.plugin.lang.gsp.resolve.taglib.TagLibNamespaceDescriptor;
import org.jetbrains.plugins.groovy.codeInspection.GroovyUnusedDeclarationInspection;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

/**
 * Tags declared as methods, which Grails 8 added next to the closure-field form.
 */
public class GspMethodTagTest extends GrailsTestCase {

  /**
   * Puts the annotations that arrived with method tags on the classpath, which is how the plugin
   * tells a Grails that dispatches tags to methods from one that does not.
   */
  private void addMethodTagSupport() {
    myFixture.addFileToProject("src/java/grails/gsp/Tag.java", "package grails.gsp; public @interface Tag {}");
    myFixture.addFileToProject("src/java/grails/gsp/NotATag.java", "package grails.gsp; public @interface NotATag {}");
  }

  private Set<String> tagNames(String prefix) {
    TagLibNamespaceDescriptor descriptor = GspTagLibUtil.getTagLibClasses(getModule()).get(prefix);
    assertNotNull("No tag library with prefix " + prefix, descriptor);

    Set<String> names = new TreeSet<>();
    for (PsiMethod tag : descriptor.getAllTags()) {
      names.add(tag.getName());
    }
    return names;
  }

  public void testConventionalSignaturesAreTags() {
    addMethodTagSupport();
    addTaglib("""
                class MyTagLib {
                  static namespace = 'my'

                  def onlyAttrs(Map attrs) {}
                  def onlyBody(Closure body) {}
                  def attrsAndBody(Map attrs, Closure body) {}
                  def defaultedBody(Map attrs, Closure body = null) {}
                  def closureField = { attrs, body -> }
                }
                """);

    assertEquals(Set.of("onlyAttrs", "onlyBody", "attrsAndBody", "defaultedBody", "closureField"), tagNames("my"));
  }

  public void testMethodsThatAreNotTags() {
    addMethodTagSupport();
    addTaglib("""
                import grails.gsp.NotATag

                class MyTagLib {
                  static namespace = 'my'

                  def tag(Map attrs) {}

                  def untyped(attrs, body) {}
                  def wrongAttrsName(Map params) {}
                  def wrongBodyName(Map attrs, Closure content) {}
                  def typedParameter(String name) {}
                  def noParameters() {}
                  def threeParameters(Map attrs, Closure body, String extra) {}
                  private hidden(Map attrs) {}
                  protected guarded(Map attrs) {}
                  static shared(Map attrs) {}
                  def setThing(Map attrs) {}
                  def raw(Map attrs) {}
                  def toString(Map attrs) {}
                  @NotATag
                  def helper(Map attrs) {}
                }
                """);

    assertEquals(Set.of("tag"), tagNames("my"));
  }

  public void testTagAnnotationAdmitsOtherSignatures() {
    addMethodTagSupport();
    addTaglib("""
                import grails.gsp.Tag

                class MyTagLib {
                  static namespace = 'my'

                  @Tag
                  def greeting(String name) {}
                  @Tag
                  def noParameters() {}
                  @Tag
                  private hidden(String name) {}
                }
                """);

    assertEquals(Set.of("greeting", "noParameters"), tagNames("my"));
  }

  public void testInheritedMethodIsNotATag() {
    addMethodTagSupport();
    addSimpleGroovyFile("""
                          class Parent {
                            def inheritedMethod(Map attrs) {}
                            def inheritedClosure = { attrs -> }
                          }
                          """);
    addTaglib("""
                class MyTagLib extends Parent {
                  static namespace = 'my'

                  def own(Map attrs) {}
                }
                """);

    assertEquals(Set.of("own", "inheritedClosure"), tagNames("my"));
  }

  public void testMethodsAreNotTagsBeforeGrails8() {
    addTaglib("""
                class MyTagLib {
                  static namespace = 'my'

                  def method(Map attrs, Closure body) {}
                  def closureField = { attrs, body -> }
                }
                """);

    assertEquals(Set.of("closureField"), tagNames("my"));
  }

  public void testClosureFieldWinsOverMethodOfTheSameName() {
    addMethodTagSupport();
    addTaglib("""
                class MyTagLib {
                  static namespace = 'my'

                  def same = { attrs -> }
                  def same(Map attrs) {}
                }
                """);

    configureByView("a.gsp", "<my:sa<caret>me/>");
    PsiElement tag = myFixture.getElementAtCaret();
    UsefulTestCase.assertInstanceOf(tag, TagLibNamespaceDescriptor.GspTagMethod.class);
    assertFalse(tag.getNavigationElement() instanceof PsiMethod);
  }

  public void testResolveFromGsp() {
    addMethodTagSupport();
    addTaglib("""
                class MyTagLib {
                  def emoticon(Map attrs, Closure body) {}
                }
                """);

    configureByView("a.gsp", "<g:emo<caret>ticon happy='true'>Hi</g:emoticon>");
    PsiElement tag = myFixture.getElementAtCaret();

    UsefulTestCase.assertInstanceOf(tag, TagLibNamespaceDescriptor.GspTagMethod.class);
    PsiElement declaration = tag.getNavigationElement();
    UsefulTestCase.assertInstanceOf(declaration, GrMethod.class);
    assertEquals("emoticon", ((GrMethod)declaration).getName());
  }

  public void testTagNameCompletion() {
    addMethodTagSupport();
    addTaglib("""
                import grails.gsp.Tag

                class MyTagLib {
                  static namespace = 'my'

                  def zzzFirst(Map attrs) {}
                  @Tag
                  def zzzSecond(String name) {}
                  def zzzHelper(String name) {}
                }
                """);

    PsiFile file = addView("a.gsp", "<my:zzz<caret>");
    checkCompletionVariants(file, "zzzFirst", "zzzSecond");
  }

  public void testCallAsMethodFromGspAndFromAnotherTagLib() {
    addMethodTagSupport();
    addTaglib("""
                class MyTagLib {
                  def emoticon(Map attrs, Closure body) {}
                }
                """);
    PsiFile other = addTaglib("""
                class OtherTagLib {
                  static namespace = 'other'

                  def wrapper(Map attrs) {
                    out << emoticon(happy: 'true')
                    out << g.emoticon(happy: 'true')
                  }
                }
                """);

    PsiFile gsp = addView("a.gsp", "<% out << emoticon(happy: 'true'); out << g.emoticon(happy: 'true') %>");
    checkResolve(gsp);
    checkResolve(other);
  }

  public void testAttributeCompletion() {
    addMethodTagSupport();
    addTaglib("""
                import grails.gsp.Tag

                class MyTagLib {
                  /**
                   * @attr documented a documented attribute
                   */
                  def myField(Map attrs) {
                    out << attrs.fromBody
                  }

                  @Tag
                  def myField(String type, Map attrs) {
                    myField(attrs)
                  }
                }
                """);

    PsiFile file = addView("a.gsp", "<g:myField <caret> />");
    myFixture.testCompletionVariants(getFilePath(file), "documented", "fromBody", "type");
  }

  public void testRequiredAttributeFromJavadoc() {
    addMethodTagSupport();
    addTaglib("""
                class MyTagLib {
                  /**
                   * @attr aaa required
                   * @attr bbb
                   */
                  def xxx(Map attrs, Closure body) {}
                }
                """);

    configureByView("a.gsp", "<g:xx<caret>");
    myFixture.completeBasic();
    myFixture.checkResult("<g:xxx aaa=\"\"");
  }

  public void testTagWithoutBodyParameterCompletesAsEmptyTag() {
    addMethodTagSupport();
    addTaglib("""
                class MyTagLib {
                  def tagWithoutBody(Map attrs) {}
                }
                """);

    configureByView("a.gsp", "<g:tagWithout<caret>");
    myFixture.completeBasic();
    myFixture.checkResult("<g:tagWithoutBody/><caret>");
  }

  public void testTagWithBodyParameterIsNotAnEmptyTag() {
    addMethodTagSupport();
    addTaglib("""
                class MyTagLib {
                  def tagWithBody(Map attrs, Closure body) {}
                }
                """);

    configureByView("a.gsp", "<g:tagWithBo<caret>");
    myFixture.completeBasic();
    myFixture.checkResult("<g:tagWithBody<caret>");
  }

  public void testFindUsagesOfTheMethod() {
    addMethodTagSupport();
    PsiFile tagLib = addTaglib("""
                                 class MyTagLib {
                                   def customTag(Map attrs) {}
                                 }
                                 """);
    addView("page.gsp", "<% out << customTag(); out << g.customTag() %> <g:customTag/>");

    GrMethod method = (GrMethod)((GroovyFile)tagLib).getClasses()[0].findMethodsByName("customTag", false)[0];
    Collection<PsiReference> references = MethodReferencesSearch.search(method).findAll();

    assertEquals(3, references.size());
  }

  public void testRenameFromTagLib() {
    addMethodTagSupport();
    PsiFile gspFile = addView("page.gsp", "<% out << customTag(); out << g.customTag() %> <g:customTag/>");

    PsiFile tagLibFile = configureByTaglib("class MyTagLib {def customTag<caret>(Map attrs) {}}");
    myFixture.renameElementAtCaret("customTag2");

    assertEquals("<% out << customTag2(); out << g.customTag2() %> <g:customTag2/>", gspFile.getText());
    assertEquals("class MyTagLib {def customTag2(Map attrs) {}}", tagLibFile.getText());
  }

  public void testRenameFromGspTag() {
    addMethodTagSupport();
    PsiFile tagLibFile = addTaglib("""
                                     class MyTagLib {
                                       def customTag(Map attrs) {}
                                       def customTag(Map attrs, Closure body) {}
                                     }
                                     """);
    PsiFile other = addView("other.gsp", "<% out << customTag(); out << g.customTag() %> <g:customTag/>");
    PsiFile gspFile = configureByView("page.gsp", "<% out << customTag(); out << g.customTag() %> <g:customTag<caret>/>");

    myFixture.renameElementAtCaret("customTag2");

    assertEquals("<% out << customTag2(); out << g.customTag2() %> <g:customTag2/>", gspFile.getText());
    assertEquals("<% out << customTag2(); out << g.customTag2() %> <g:customTag2/>", other.getText());
    assertEquals("""
                   class MyTagLib {
                     def customTag2(Map attrs) {}
                     def customTag2(Map attrs, Closure body) {}
                   }
                   """, tagLibFile.getText());
  }

  public void testTagMethodsAreNotReportedAsUnused() {
    addMethodTagSupport();
    myFixture.enableInspections(new GroovyUnusedDeclarationInspection(), new UnusedDeclarationInspectionBase(true));

    configureByTaglib("""
                        import grails.gsp.NotATag
                        import grails.gsp.Tag

                        class MyTagLib {
                          def emoticon(Map attrs, Closure body) { out << attrs << body() }

                          @Tag
                          def greeting(String name) { out << name }

                          @NotATag
                          def <warning>helper</warning>(Map attrs) { out << attrs }

                          def <warning>notATag</warning>(String name) { out << name }
                        }
                        """);

    myFixture.checkHighlighting(true, false, true);
  }

  public void testParametersOfTagMethodsAreNotReportedAsUnused() {
    addMethodTagSupport();
    myFixture.enableInspections(new GroovyUnusedDeclarationInspection(), new UnusedDeclarationInspectionBase(true));

    configureByTaglib("""
                        import grails.gsp.Tag

                        class MyTagLib {
                          def emoticon(Map attrs, Closure body) {}

                          @Tag
                          def greeting(String name) {}

                          def <warning>notATag</warning>(String <warning>name</warning>) {}
                        }
                        """);

    myFixture.checkHighlighting(true, false, true);
  }

  public void testParametersOfTagLikeMethodsAreStillReportedAsUnusedBeforeGrails8() {
    myFixture.enableInspections(new GroovyUnusedDeclarationInspection(), new UnusedDeclarationInspectionBase(true));

    configureByTaglib("""
                        class MyTagLib {
                          def <warning>emoticon</warning>(Map <warning>attrs</warning>, Closure <warning>body</warning>) {}
                        }
                        """);

    myFixture.checkHighlighting(true, false, true);
  }

  public void testTagLikeMethodsAreStillReportedAsUnusedBeforeGrails8() {
    myFixture.enableInspections(new GroovyUnusedDeclarationInspection(), new UnusedDeclarationInspectionBase(true));

    configureByTaglib("""
                        class MyTagLib {
                          def <warning>emoticon</warning>(Map attrs, Closure body) { out << attrs << body() }
                        }
                        """);

    myFixture.checkHighlighting(true, false, true);
  }

  public void testIsTagMethodIgnoresTheGrailsVersion() {
    PsiFile tagLib = addTaglib("class MyTagLib { def emoticon(Map attrs) {} }");
    PsiMethod method = ((GroovyFile)tagLib).getClasses()[0].findMethodsByName("emoticon", false)[0];

    assertTrue(GspTagMethodUtil.isTagMethod(method));
    assertFalse(GspTagMethodUtil.isTagOfMethodTagLibrary(method));
  }
}
