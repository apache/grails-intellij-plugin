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
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.fileType.GspFileType;
import org.jetbrains.plugins.groovy.GroovyLanguage;
import org.jetbrains.plugins.groovy.GroovyProjectDescriptors;
import org.jetbrains.plugins.groovy.codeInspection.untypedUnresolvedAccess.GrUnresolvedAccessInspection;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.junit.Assert;

public class GspResolveIndexesTest extends LightJavaCodeInsightFixtureTestCase {
  @Override
  protected @NotNull LightProjectDescriptor getProjectDescriptor() {
    return GroovyProjectDescriptors.MOCK_JDK_11;
  }

  private PsiElement getElementAtCaret() {
    int caret = myFixture.getEditor().getCaretModel().getOffset();
    return myFixture.getFile().getViewProvider().findElementAt(caret, GroovyLanguage.INSTANCE);
  }

  private void assertType(String type) {
    PsiElement element = getElementAtCaret();

    PsiReference ref = element.getReference();
    while (ref == null) {
      element = element.getParent();
      ref = element.getReference();
    }


    PsiVariable var = (PsiVariable)ref.resolve();
    Assert.assertEquals(type, var.getType().getCanonicalText());
  }

  public void testEach() {
    PsiFile file = myFixture.addFileToProject("grails-app/views/test.gsp", """
      <g:each in='["aaa", "bbb", "ccc"]'>
        ${i<caret>t}
      </g:each>
      """);
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    assertType("java.lang.String");
    UsefulTestCase.assertInstanceOf(myFixture.getElementAtCaret().getNavigationElement(), XmlAttributeValue.class);
  }

  /**
   * Iterating a map goes over its entries, which have the map's key and value types, so that what the entry's value
   * holds resolves. The stock Grails index.gsp iterates the controllers grouped by namespace this way.
   */
  public void testEachOverMapHasTypedEntries() {
    PsiFile file = myFixture.addFileToProject("grails-app/views/test.gsp", """
      <g:each in="${[a: 1]}" var="entry">
        ${ent<caret>ry.value.byteValue()}
      </g:each>
      """);
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    assertType("java.util.Map.Entry<java.lang.String,java.lang.Integer>");
    PsiReference byteValue = file.getViewProvider().findReferenceAt(file.getText().indexOf("byteValue"), GroovyLanguage.INSTANCE);
    Assert.assertNotNull(byteValue);
    Assert.assertNotNull("entry.value.byteValue() should resolve", byteValue.resolve());
  }

  public void testEachStatus() {
    PsiFile file = myFixture.addFileToProject("grails-app/views/test.gsp", """
      <g:each in='["aaa", "bbb", "ccc"]' status='iii'>
        ${i<caret>ii}
      </g:each>
      """);
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    assertType("java.lang.Integer");
    UsefulTestCase.assertInstanceOf(myFixture.getElementAtCaret().getNavigationElement(), XmlAttributeValue.class);
  }

  public void testHighlighting() {
    myFixture.addFileToProject("grails-app/views/test.gsp", """
      ${<warning descr="Cannot resolve symbol 'v2'">v2</warning>} ${<warning descr="Cannot resolve symbol 'v1'">v1</warning>}
      <g:set var="v1" value="123"/>
      ${<warning descr="Cannot resolve symbol 'v2'">v2</warning>} ${v1}
      <g:set var="v2" value="123"/>
      ${<warning descr="Cannot resolve symbol 'zzz'">zzz</warning>} ${v2} ${v1}
      
      <g:each in="[1,2,3]">
        <g:each in="['a', 'b', 'c']" var="zzz">
          ${it} ${zzz}
        </g:each>${<warning descr="Cannot resolve symbol 'zzz'">zzz</warning>}
      </g:each>
      
      <g:each in="[1,2,3]">
        <g:each in="['a', 'b', 'c']">
          ${it}
        </g:each>
      </g:each>
      """);
    myFixture.enableInspections(new GrUnresolvedAccessInspection());
    myFixture.testHighlighting("grails-app/views/test.gsp");
  }

  public void testEachVar() {
    myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      <g:each in='["aaa", "bbb", "ccc"]' var="iii">
        ${i<caret>ii}
      </g:each>
      """);
    assertType("java.lang.String");
    UsefulTestCase.assertInstanceOf(myFixture.getElementAtCaret().getNavigationElement(), XmlAttributeValue.class);
  }

  public void testEach2() {
    myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      <% def aList = [new Long(2), new Long(342)] %>
      <g:each in='${aList}'>
        ${i<caret>t}
      </g:each>
      """);
    assertType("java.lang.Long");
  }

  public void testEach3() {
    myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      <% def aList = new ArrayList<Map>() %>
      <g:each in='${aList}'>
        ${i<caret>t}
      </g:each>
      """);
    assertType("java.util.Map");
  }

  public void testEach4() {
    myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      <g:each in="dffsdfsdfdsf">
        ${i<caret>t}
      </g:each>
      """);
    assertType("java.lang.String");
  }

  public void testEach5() {
    myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      <g:each in=" ${[3, 4, 5]}">
        ${i<caret>t}
      </g:each>
      """);
    assertType("java.lang.String");
  }

  public void testEach6() {
    myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      <g:each in='${[3, 4, 5]} '>
        ${i<caret>t}
      </g:each>
      """);
    assertType("java.lang.String");
  }

  public void testEach7() {
    myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      <g:each in='${new Integer(34)}'>
        ${i<caret>t}
      </g:each>
      """);
    assertType("java.lang.Integer");
  }

  public void testRenameVariableEachVar() {
    PsiFile file = myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      <g:each var="v" in="[1,2,3]">
        <g:if test="${v > 0}">
          <%= v<caret>.byteValue() %>
          <% out << v + 10 %>
          ${v}
        </g:if> v
      </g:each>
      ${v}
      """);
    myFixture.renameElementAtCaret("vvv");
    Assert.assertEquals(
      """
        <g:each var="vvv" in="[1,2,3]">
          <g:if test="${vvv > 0}">
            <%= vvv.byteValue() %>
            <% out << vvv + 10 %>
            ${vvv}
          </g:if> v
        </g:each>
        ${v}
        """, file.getText());

    PsiReference ref = file.findReferenceAt(myFixture.getEditor().getCaretModel().getOffset());
    Assert.assertNotNull(ref.resolve());
  }

  public void testRenameVariableEachStatus() {
    PsiFile file = myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      <g:each var="i" status="v" in="[1,2,3]">
        <g:if test="${v > 0}">
          <%= v<caret>.byteValue() %>
          <% out << v + 10 %>
          ${v}
        </g:if> v
      </g:each>
      ${v}
      """);
    myFixture.renameElementAtCaret("vvv");
    Assert.assertEquals(
      """
        <g:each var="i" status="vvv" in="[1,2,3]">
          <g:if test="${vvv > 0}">
            <%= vvv.byteValue() %>
            <% out << vvv + 10 %>
            ${vvv}
          </g:if> v
        </g:each>
        ${v}
        """, file.getText());

    PsiReference ref = file.findReferenceAt(myFixture.getEditor().getCaretModel().getOffset());
    Assert.assertNotNull(ref.resolve());
  }

  public void testRenameVariableDef() {
    PsiFile file = myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      ${v}
      <g:set var="v" value="v"/>
      <g:if test="${v<caret> > 0}">
        <%=v.byteValue()%>
        <% out << v + 10 %>
        ${v}
      </g:if>
      """);
    myFixture.renameElementAtCaret("vvv");
    Assert.assertEquals("""
                          ${v}
                          <g:set var="vvv" value="v"/>
                          <g:if test="${vvv > 0}">
                            <%=vvv.byteValue()%>
                            <% out << vvv + 10 %>
                            ${vvv}
                          </g:if>
                          """, file.getText());

    PsiReference ref = file.findReferenceAt(myFixture.getEditor().getCaretModel().getOffset());
    Assert.assertNotNull(ref.resolve());
  }

  /** The stock Grails index.gsp counts the controllers this way. */
  public void testDefWithPrimitiveType() {
    PsiFile file = myFixture.addFileToProject("grails-app/views/test.gsp", """
      <g:def type="int" var="numControllers" value="${[1, 2].size()}"/>
      ${numCon<caret>trollers}
      """);
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    assertType("int");
    UsefulTestCase.assertInstanceOf(myFixture.getElementAtCaret().getNavigationElement(), XmlAttributeValue.class);
  }

  public void testDefWithImportedType() {
    myFixture.addClass("package com.foo; public class Bar {}");
    PsiFile file = myFixture.addFileToProject("grails-app/views/test.gsp", """
      <%@ page import="com.foo.Bar" %>
      <g:def type="Bar" var="bar" value="${null}"/>
      ${b<caret>ar}
      """);
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    assertType("com.foo.Bar");
  }

  /** Without a type, or with {@code def}, GroovyDefTag writes an untyped declaration, which takes the value's type. */
  public void testDefWithoutTypeTakesTheValueType() {
    PsiFile file = myFixture.addFileToProject("grails-app/views/test.gsp", """
      <g:def var="untyped" value="${'abc'}"/>
      <g:def type="def" var="defTyped" value="${'abc'}"/>
      ${untyped} ${def<caret>Typed}
      """);
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    assertType("java.lang.String");
    myFixture.getEditor().getCaretModel().moveToOffset(file.getText().indexOf("${untyped}") + 3);
    assertType("java.lang.String");
  }

  /** A g:def is a local variable of the enclosing tag's body, where a g:set stays visible for the rest of the page. */
  public void testDefScope() {
    PsiFile file = myFixture.addFileToProject("grails-app/views/test.gsp", """
      ${v1}
      <g:def var="v1" value="${1}"/>
      ${v1}
      <g:if test="${true}">
        <g:def var="v2" value="${2}"/>
        <g:set var="v3" value="${3}"/>
        ${v1} ${v2} ${v3}
      </g:if>
      ${v2} ${v3}
      """);
    assertResolvesInOrder(file, "v1", false, true, true);
    assertResolvesInOrder(file, "v2", true, false);
    assertResolvesInOrder(file, "v3", true, true);
  }

  /** Asserts, for each {@code ${name}} expression of the page in order, whether {@code name} resolves there. */
  private static void assertResolvesInOrder(PsiFile file, String name, boolean... resolves) {
    String expression = "${" + name + "}";
    int offset = -1;
    for (int i = 0; i < resolves.length; i++) {
      offset = file.getText().indexOf(expression, offset + 1);
      Assert.assertTrue("occurrence " + (i + 1) + " of " + expression + " not found", offset >= 0);
      PsiReference ref = file.getViewProvider().findReferenceAt(offset + 2, GroovyLanguage.INSTANCE);
      Assert.assertNotNull("no reference at occurrence " + (i + 1) + " of " + expression, ref);
      Assert.assertEquals("occurrence " + (i + 1) + " of " + expression + " resolves", resolves[i], ref.resolve() != null);
    }
    Assert.assertEquals("more occurrences of " + expression + " than expected", -1, file.getText().indexOf(expression, offset + 1));
  }

  public void testRenameDefVariable() {
    PsiFile file = myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      <g:def type="int" var="v" value="${1}"/>
      <g:if test="${v<caret> > 0}">
        <%=v.byteValue()%>
        ${v}
      </g:if>
      """);
    myFixture.renameElementAtCaret("vvv");
    Assert.assertEquals("""
                          <g:def type="int" var="vvv" value="${1}"/>
                          <g:if test="${vvv > 0}">
                            <%=vvv.byteValue()%>
                            ${vvv}
                          </g:if>
                          """, file.getText());
  }

  /** The stock Grails index.gsp looks the plugin manager up this way. */
  public void testSetBeanByKnownName() {
    myFixture.addClass("package grails.plugins; public interface GrailsPluginManager { Object[] getAllPlugins(); }");
    PsiFile file = myFixture.addFileToProject("grails-app/views/test.gsp", """
      <g:set var="pluginManager" bean="pluginManager"/>
      ${plugin<caret>Manager.allPlugins}
      """);
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    assertType("grails.plugins.GrailsPluginManager");
    PsiReference allPlugins = file.getViewProvider().findReferenceAt(file.getText().indexOf("allPlugins"), GroovyLanguage.INSTANCE);
    Assert.assertNotNull(allPlugins);
    Assert.assertNotNull("allPlugins should resolve", allPlugins.resolve());
  }

  public void testSetBeanByKnownNamePrefersTheNewestApi() {
    myFixture.addClass("package javax.servlet; public interface ServletContext {}");
    myFixture.addClass("package jakarta.servlet; public interface ServletContext {}");
    PsiFile file = myFixture.addFileToProject("grails-app/views/test.gsp", """
      <g:set var="servletContext" bean="servletContext"/>
      ${servlet<caret>Context}
      """);
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    assertType("jakarta.servlet.ServletContext");
  }

  public void testSetBeanByKnownNameOnGrails2() {
    myFixture.addClass("package org.codehaus.groovy.grails.plugins; public interface GrailsPluginManager {}");
    PsiFile file = myFixture.addFileToProject("grails-app/views/test.gsp", """
      <g:set var="pluginManager" bean="pluginManager"/>
      ${plugin<caret>Manager}
      """);
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    assertType("org.codehaus.groovy.grails.plugins.GrailsPluginManager");
  }

  /** A class rather than a name makes Grails look the bean up by type. */
  public void testSetBeanByType() {
    myFixture.addClass("package com.foo; public class Bar {}");
    PsiFile file = myFixture.addFileToProject("grails-app/views/test.gsp", """
      <g:set var="bar" bean="${com.foo.Bar}"/>
      ${b<caret>ar}
      """);
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    assertType("com.foo.Bar");
  }

  /** ApplicationTagLib.set ignores the value when the tag names a bean. */
  public void testSetBeanWinsOverValue() {
    myFixture.addClass("package grails.plugins; public interface GrailsPluginManager {}");
    PsiFile file = myFixture.addFileToProject("grails-app/views/test.gsp", """
      <g:set var="pluginManager" bean="pluginManager" value="${'abc'}"/>
      ${plugin<caret>Manager}
      """);
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    assertType("grails.plugins.GrailsPluginManager");
  }

  /** A bean is no String, which is what g:set stores without a bean or a value. */
  public void testSetUnknownBeanIsAnObject() {
    PsiFile file = myFixture.addFileToProject("grails-app/views/test.gsp", """
      <g:set var="service" bean="someService"/>
      ${ser<caret>vice}
      """);
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    assertType("java.lang.Object");
  }

  public void _testResolveIndexes() {
    PsiFile file = myFixture.addFileToProject("a.gsp", """
      <% def it = new Object(); %>
      <g:each in="['a', 'b', 'c']">
      ${it.substring(1)}
      <g:findAll in="[1, 2,3,4,5]" expr="it.byteValue() == 0" />
      </g:each>
      """);
    GrailsTestCase.checkResolve(file);
  }

  public void testResolveItOfUnknownType() {
    PsiFile file = myFixture.addFileToProject("a.gsp", """
      <g:collect in="${unresolved}" expr="it.hashCode() != 0">${it}</g:collect>
      <g:findAll in="${unresolved}" expr="it.hashCode() != 0">${it}</g:findAll>
      <g:each in="${unresolved}">${it}</g:each>
      <g:each in="${unresolved}" var="iii">${iii}</g:each>
      <g:grep in="${unresolved}" filter="sdsdfs">${it}</g:grep>
      """);
    GrailsTestCase.checkResolve(file.getViewProvider().getPsi(GroovyLanguage.INSTANCE), "unresolved", "unresolved", "unresolved",
                                "unresolved", "unresolved");
  }

  public void testNullSafeEach() {
    PsiFile file = myFixture.addFileToProject("a.gsp", """
      <% def ccc = [1, 2, 3] %>
      
      <g:collect in="${ccc?}" expr="it.byteValue() != 1">${it}</g:collect> <br>
      <g:findAll in="${ccc?}" expr="it.byteValue() != 1">${it}</g:findAll> <br>
      <g:each in="${ccc?}">${it.byteValue()}</g:each> <br>
      <g:grep in="${ccc?}" filter="${/[a-z]/}">${it.byteValue()}</g:grep> <br>
      
      <g:link in="${ccc?<error descr="<expression> expected">}</error>" />
      <g:each zzz="${ccc?<error descr="<expression> expected">}</error>" in="[1,2,3]"/>
      
      ${ccc?<error descr="<expression> expected">}</error>
      
      <% ccc?<error descr="<expression> expected, got '%>'"> </error>%>
      <% unresolver %>
      """);

    myFixture.testHighlighting(true, false, true, file.getVirtualFile());

    GrailsTestCase.checkResolve(file.getViewProvider().getPsi(GroovyLanguage.INSTANCE), "unresolver");
  }

  public void testCompletion() {
    PsiFile file = myFixture.addFileToProject("a.gsp", """
      <g:each in='${[3, 4, 5]}' var="g">
        ${g.<caret>}
      </g:each>
      """);
    GrailsTestCase.checkCompletionStatic(myFixture, file, "byteValue", "intValue", "longValue");
  }
}
