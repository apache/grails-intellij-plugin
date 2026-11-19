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

package org.apache.grails.intellij.plugin.gsp;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.xml.XmlElementDescriptor;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.apache.grails.intellij.plugin.lang.gsp.resolve.taglib.GspTagLibUtil;
import org.apache.grails.intellij.plugin.lang.gsp.resolve.taglib.TagLibNamespaceDescriptor;
import org.jetbrains.plugins.groovy.GroovyLanguage;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;

import java.util.List;

/**
 * Tags a tag library declares as methods, which Grails 8 accepts besides Closure fields and uses for its own tag
 * libraries. The tag library below has a method for each of the rules of Grails 8's TagDiscoveryRules.
 */
public class GspMethodTagTest extends GrailsTestCase {

  private static final String TAG_LIB = """
    class MyTagLib {
      /**
       * @attr name REQUIRED who to greet
       */
      def greet(Map attrs) { out << attrs.title }
      def wrap(Map attrs, Closure body) { out << body() }
      def bodyOnly(Closure body) { out << body() }
      def withDefault(Map attrs, String suffix = '!') { out << suffix }
      @grails.gsp.Tag
      def annotated(String text) { out << text }
      def legacy = { attrs -> out << 'closure' }

      @grails.gsp.NotATag
      def excluded(Map attrs) {}
      def helper(String text) {}
      def untypedAttrs(attrs) {}
      def wrongName(Map options) {}
      def setThing(Map attrs) {}
      static def util(Map attrs) {}
      private def secret(Map attrs) {}
      def raw(Map attrs) {}
    }
    """;

  private static final List<String> TAGS = List.of("greet", "wrap", "bodyOnly", "withDefault", "annotated", "legacy");

  private static final List<String> NOT_TAGS =
    List.of("excluded", "helper", "untypedAttrs", "wrongName", "setThing", "thing", "util", "secret", "raw");

  public void testTagsFollowTheGrails8Rules() {
    addGrails8TagLib(TAG_LIB);

    // Each tag on its own first, before listing them all fills the cache getTag also reads.
    for (String name : TAGS) {
      assertNotNull(name + " should be a tag", defaultNamespace().getTag(name));
    }
    for (String name : NOT_TAGS) {
      assertNull(name + " should not be a tag", defaultNamespace().getTag(name));
    }

    List<String> allTags = ContainerUtil.map(defaultNamespace().getAllTags(), PsiMethod::getName);
    assertContainsElements(allTags, TAGS);
    assertDoesntContain(allTags, NOT_TAGS);
  }

  /** Before Grails 8, a method of a tag library is an ordinary helper, whatever its shape. */
  public void testMethodsAreNotTagsBeforeGrails8() {
    addTaglib(TAG_LIB);

    List<String> methodTags = ContainerUtil.filter(TAGS, name -> !name.equals("legacy"));
    for (String name : methodTags) {
      assertNull(name + " should not be a tag", defaultNamespace().getTag(name));
    }

    List<String> allTags = ContainerUtil.map(defaultNamespace().getAllTags(), PsiMethod::getName);
    assertContainsElements(allTags, "legacy");
    assertDoesntContain(allTags, methodTags);
  }

  /** The stock Grails index.gsp calls the message and meta tags of Grails 8 this way. */
  public void testMethodTagResolvesAsAMethodCall() {
    addGrails8TagLib(TAG_LIB);
    PsiFile gsp = addView("ccc/index.gsp", "${greet(name: 'x')}");

    PsiReference reference = gsp.getViewProvider().findReferenceAt(gsp.getText().indexOf("greet"), GroovyLanguage.INSTANCE);
    assertNotNull(reference);
    PsiElement tag = reference.resolve();
    assertInstanceOf(tag, TagLibNamespaceDescriptor.GspTagMethod.class);
    assertGreetMethod(tag.getNavigationElement());
  }

  public void testMethodTagResolvesAsATag() {
    addGrails8TagLib(TAG_LIB);
    PsiFile gsp = addView("ccc/index.gsp", "<g:greet name='x'/>\n<g:wrap>text</g:wrap>\n");

    XmlElementDescriptor greet = descriptorOf(gsp, "g:greet");
    assertGreetMethod(greet.getDeclaration().getNavigationElement());
    // Without a Closure parameter, a method tag takes no body, like a closure tag with one parameter.
    assertEquals(XmlElementDescriptor.CONTENT_TYPE_EMPTY, greet.getContentType());
    assertFalse(XmlElementDescriptor.CONTENT_TYPE_EMPTY == descriptorOf(gsp, "g:wrap").getContentType());
  }

  /** From the Javadoc, from the attributes the body reads, and from the parameters Grails 8 binds to attributes. */
  public void testMethodTagAttributes() {
    addGrails8TagLib(TAG_LIB);

    myFixture.testCompletionVariants(getFilePath(addView("ccc/greet.gsp", "<g:greet <caret>/>")), "name", "title");
    myFixture.testCompletionVariants(getFilePath(addView("ccc/default.gsp", "<g:withDefault <caret>/>")), "suffix");
  }

  /** A parameter is the attribute of its name, even a name that is not offered when the tag reads it from attrs. */
  public void testParameterNamedTypeIsAnAttribute() {
    addGrails8TagLib("""
      class MyTagLib {
        static namespace = 'my'

        @grails.gsp.Tag
        def field(String type, Map attrs) { out << type }
      }
      """);

    myFixture.testCompletionVariants(getFilePath(addView("ccc/field.gsp", "<my:field <caret>/>")), "type");
  }

  public void testTagCallReturnsTheGrails2BufferOnGrails2() {
    addTaglib(TAG_LIB);

    assertEquals("org.codehaus.groovy.grails.web.util.StreamCharBuffer", typeOfTagCall("legacy(name: 'x')"));
  }

  public void testTagCallReturnsTheGrails3BufferFromGrails3On() {
    myFixture.addClass("package org.grails.buffer; public class StreamCharBuffer {}");
    addTaglib(TAG_LIB);

    assertEquals("org.grails.buffer.StreamCharBuffer", typeOfTagCall("legacy(name: 'x')"));
  }

  public void testMethodTagCallReturnsTheBuffer() {
    myFixture.addClass("package org.grails.buffer; public class StreamCharBuffer {}");
    addGrails8TagLib(TAG_LIB);

    assertEquals("org.grails.buffer.StreamCharBuffer", typeOfTagCall("greet(name: 'x')"));
  }

  /**
   * Adds a tag library to a project whose Grails dispatches tags to methods, which the plugin tells by the annotations
   * that came with method tags in Grails 8.
   */
  private PsiFile addGrails8TagLib(String classText) {
    myFixture.addClass("package grails.gsp; public @interface Tag {}");
    myFixture.addClass("package grails.gsp; public @interface NotATag {}");
    return addTaglib(classText);
  }

  private TagLibNamespaceDescriptor defaultNamespace() {
    TagLibNamespaceDescriptor descriptor = GspTagLibUtil.getTagLibClasses(getModule()).get(GspTagLibUtil.DEFAULT_TAGLIB_PREFIX);
    assertNotNull(descriptor);
    return descriptor;
  }

  private String typeOfTagCall(String callText) {
    PsiFile gsp = addView("ccc/call.gsp", "${" + callText + "}");
    GrExpression call = ContainerUtil.find(
      PsiTreeUtil.findChildrenOfType(gsp.getViewProvider().getPsi(GroovyLanguage.INSTANCE), GrExpression.class),
      expression -> expression.getText().equals(callText));
    assertNotNull(call);
    PsiType type = call.getType();
    assertNotNull(type);
    return type.getCanonicalText();
  }

  private static XmlElementDescriptor descriptorOf(PsiFile gsp, String tagName) {
    XmlTag tag = ContainerUtil.find(PsiTreeUtil.findChildrenOfType(gsp, XmlTag.class), candidate -> tagName.equals(candidate.getName()));
    assertNotNull("no tag " + tagName, tag);
    XmlElementDescriptor descriptor = tag.getDescriptor();
    assertNotNull("no descriptor for " + tagName, descriptor);
    return descriptor;
  }

  private static void assertGreetMethod(PsiElement element) {
    GrMethod method = assertInstanceOf(element, GrMethod.class);
    assertEquals("greet", method.getName());
  }
}
