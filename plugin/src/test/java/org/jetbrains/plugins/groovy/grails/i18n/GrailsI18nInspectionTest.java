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
package org.jetbrains.plugins.groovy.grails.i18n;

import com.intellij.codeInspection.InspectionProfileEntry;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.groovy.grails.i18n.GrailsI18nInspection;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

import java.util.List;

public class GrailsI18nInspectionTest extends GrailsTestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture.enableInspections(GrailsI18nInspection.class);
  }

  private void setIgnoreIfDefault(boolean value) {
    InspectionToolWrapper<?, ?> tool = InspectionProjectProfileManager.getInstance(getProject()).getCurrentProfile()
      .getInspectionTool(new GrailsI18nInspection().getShortName(), getProject());
    InspectionProfileEntry inspection = tool.getTool();
    ((GrailsI18nInspection)inspection).ignoreTagsWithDefault = value;
  }

  public void testCodeReference() {
    myFixture.addFileToProject("grails-app/i18n/messages.properties", """

aaa1=Aaaa1
aaa2=Aaaa2
bbb=Bbbb
""");

    myFixture.addFileToProject("grails-app/views/a.gsp", "<g:message code='aa<caret>' />");

    assertEquals(List.of("aaa1", "aaa2"), myFixture.getCompletionVariants("grails-app/views/a.gsp"));
  }

  public void testHighlightingGsp() {
    setIgnoreIfDefault(true);

    myFixture.addFileToProject("grails-app/i18n/messages.properties", """

aaa.bbb=Aaa bbb
""");

    PsiFile gsp = addView("aaa.gsp", """

<g:message code="aaa.bbb" />
<g:message code="<error descr="Cannot resolve property key">aaa.bbb.sss</error>" />
<g:message code="aaa.${xxx}" />
<g:message code="asdaskdjaskdaj" default="Default Text" />

<<error descr="Element tooltip:tip is not allowed here">tooltip:tip</error> code="sdfsdfsdf" />

<g:sortableColumn titleKey="<error descr="Cannot resolve property key">sdfsdfsdf</error>" />
<g:sortableColumn titleKey="sdfsdfsdf" title="Default Text" />

<%--suppress InvalidI18nProperty --%>
<g:sortableColumn titleKey="sdfsdfsdf" />
""");

    myFixture.testHighlighting(true, false, true, gsp.getVirtualFile());
  }

  public void testSuppressionWarningForFile() {
    PsiFile gsp = addView("aaa.gsp", """

<%--suppress InvalidI18nProperty --%>

<div />
<g:sortableColumn titleKey="sdfsdfsdf" />
""");

    myFixture.testHighlighting(true, false, true, gsp.getVirtualFile());
  }

  public void testSuppressionAllWarningInFile() {
    PsiFile gsp = addView("aaa.gsp", """

<%--suppress ALL --%>

<div />
<g:sortableColumn titleKey="sdfsdfsdf" />
""");

    myFixture.testHighlighting(true, false, true, gsp.getVirtualFile());
  }

  public void testHighlightingGroovy() {
    setIgnoreIfDefault(true);

    myFixture.addFileToProject("grails-app/i18n/messages.properties", """

aaa.bbb=Aaa bbb
""");

    PsiFile file = addController("""

class CccController {
  def index = {
    g.message(code:"aaa.bbb")
    g.message(code:"<error descr="Cannot resolve property key">aaa.bbb.sss</error>")
    g.message(code:"aaa.${xxx}")
    g.message(code:"asdaskdjaskdaj", default:"Default Text")

    g.sortableColumn(titleKey:"<error descr="Cannot resolve property key">sdfsdfsdf</error>")
    g.sortableColumn(titleKey:"sdfsdfsdf", title:"Default Text")

    //noinspection InvalidI18nProperty
    g.sortableColumn(titleKey:"sdfsdfsdf")
  }

  @SuppressWarnings("InvalidI18nProperty")
  def zzz = {
    g.sortableColumn(titleKey:"sdfsdfsdf")
  }
}
""");

    myFixture.testHighlighting(true, false, true, file.getVirtualFile());
  }

  public void testHighlighting2() {
    setIgnoreIfDefault(false);

    PsiFile file = addController("""

class CccController {
  def index = {
    g.sortableColumn(titleKey:"<error>sdfsdfsdf</error>", title:"Default Text")
  }
}
""");

    myFixture.testHighlighting(true, false, true, file.getVirtualFile());
  }

  public void testCreatePropertyGsp() {
    setIgnoreIfDefault(false);

    myFixture.addFileToProject("grails-app/i18n/messages.properties", """

aaa.bbb=Aaa bbb
""");

    configureByView("aaa.gsp", "<g:message code='aaa.bbb.ccc<caret>' />");

    var intentions = myFixture.filterAvailableIntentions("Create property");
    assertSize(1, intentions);

    assertEmpty(myFixture.filterAvailableIntentions("Don't check"));
  }

  public void testCreatePropertyGsp2() {
    setIgnoreIfDefault(false);

    myFixture.addFileToProject("grails-app/i18n/messages.properties", """

aaa.bbb=Aaa bbb
""");

    configureByView("aaa.gsp", "<g:message code='aaa.bbb.ccc<caret>' default='aaa'/>");

    assertSize(1, myFixture.filterAvailableIntentions("Create property"));
    assertSize(1, myFixture.filterAvailableIntentions("Don't check"));
  }
}
