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

package org.apache.grails.intellij.plugin.gsp;

import com.intellij.jsp.impl.TldDescriptor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.xml.XmlElementDescriptor;
import org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.impl.gtag.GspTagDescriptorService;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

import java.util.Set;
import java.util.TreeSet;

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
    TldDescriptor tldDescriptor = GspTagDescriptorService.getTldDescriptor(getProject());

    PsiFile gspFile = myFixture.addFileToProject("a.gsp", "");

    XmlDocument document = (XmlDocument)gspFile.getFirstChild();

    Set<String> tagNames = new TreeSet<>();

    for (XmlElementDescriptor d : tldDescriptor.getRootElementsDescriptors(document)) {
      tagNames.add(d.getName());
    }


    for (XmlElementDescriptor d : document.getRootTag().getDescriptor().getElementsDescriptors(null)) {
      tagNames.add(d.getName());
    }


    tagNames.removeAll(GspTagDescriptorService.getAllTags());
    UsefulTestCase.assertEmpty(tagNames);
  }

  public void testCompletion() {
    configureByView("a.gsp", "<g:link onmouse<caret> />");
    checkCompletion("onmousedown", "onmousemove", "onmouseout", "onmouseover", "onmouseup");
  }
}
