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

import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.impl.gtag.GspTagDescriptorService;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.apache.grails.intellij.lib.testFramework.UltimateOnlyTest;
import org.junit.experimental.categories.Category;

import java.util.Set;
import java.util.TreeSet;

/**
 * Asserts the GSP tag cache against the JSP TLD descriptor (com.intellij.jsp is Ultimate-only):
 * excluded from the Community Edition test run.
 */
@Category(UltimateOnlyTest.class)
public class GspHtmlAttributesTest extends GrailsTestCase {
  @Override
  protected boolean useGrails14() {
    return true;
  }

  /**
   * Check that all SDK tags are present in GspHtmlAttributeCache.tagMap.
   * If new SDK tags will added on grails release this test will fail.
   */
  public void testAllTagsInMap() {
    PsiFile gspFile = myFixture.addFileToProject("a.gsp", "");

    XmlDocument document = (XmlDocument)gspFile.getFirstChild();

    Set<String> tagNames = new TreeSet<>(GspTagDescriptorService.getTldTags());


    for (XmlElementDescriptor d : document.getRootTag().getDescriptor().getElementsDescriptors(null)) {
      tagNames.add(d.getName());
    }


    tagNames.removeAll(GspTagDescriptorService.getAllTags());
    UsefulTestCase.assertEmpty(tagNames);
  }

  /**
   * The attributes of the built-in control flow tags resolve through the descriptor service,
   * whichever of its two sources supplied them.
   */
  public void testBuiltInTagAttributesAreDescribed() {
    GspTagDescriptorService service = GspTagDescriptorService.getInstance(getProject());

    Set<String> each = new TreeSet<>();
    for (XmlAttributeDescriptor descriptor : service.getAttributesDescriptors("each")) {
      each.add(descriptor.getName());
    }
    UsefulTestCase.assertContainsElements(each, "in", "var", "status");

    assertNotNull(service.getAttributesDescriptor("if", "test"));
    assertNotNull(service.getAttributesDescriptor("while", "test"));
  }

  /**
   * The fallback used when {@code com.intellij.jsp} is absent, so that the TLD's attributes stay
   * available for completion: the file is read directly instead of through its platform metadata.
   */
  public void testAttributeNamesAreReadFromTheBundledTld() {
    UsefulTestCase.assertContainsElements(GspTagDescriptorService.getTldAttributes("each"), "in", "var", "status");
    UsefulTestCase.assertContainsElements(GspTagDescriptorService.getTldAttributes("if"), "test", "env");
    UsefulTestCase.assertContainsElements(GspTagDescriptorService.getTldAttributes("link"), "controller", "action");
    assertEmpty(GspTagDescriptorService.getTldAttributes("no-such-tag"));
    assertEquals(60, GspTagDescriptorService.getTldTags().size());
  }

  public void testCompletion() {
    configureByView("a.gsp", "<g:link onmouse<caret> />");
    checkCompletion("onmousedown", "onmousemove", "onmouseout", "onmouseover", "onmouseup");
  }
}
