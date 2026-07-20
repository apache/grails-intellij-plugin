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

package org.jetbrains.plugins.groovy.grails.tagSupport;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;
import org.junit.Assert;

import java.util.List;

/**
 * File-reference support for asset-pipeline path attributes, contributed by the asset-pipeline
 * module through the {@code gspTagAttributeReferenceProvider} extension point. Resolution is rooted
 * at {@code grails-app/assets/<type>}.
 */
public class AssetPipelineSrcTest extends GrailsTestCase {

  private void addAssetsTagLib() {
    addTaglib("class AssetsTagLib { static namespace = \"asset\" }");
  }

  private void assertResolvesTo(String expectedFileName) {
    PsiElement target = myFixture.getElementAtCaret();
    Assert.assertNotNull(target);
    Assert.assertEquals(expectedFileName, target.getContainingFile().getName());
  }

  public void testJavascriptSrcResolves() {
    addAssetsTagLib();
    myFixture.addFileToProject("grails-app/assets/javascripts/site/forms.js", "");

    configureByView("a.gsp", "<asset:javascript src=\"site/forms.js<caret>\"/>");

    assertResolvesTo("forms.js");
  }

  public void testStylesheetSrcResolves() {
    addAssetsTagLib();
    myFixture.addFileToProject("grails-app/assets/stylesheets/main.css", "");

    configureByView("a.gsp", "<asset:stylesheet src=\"main.css<caret>\"/>");

    assertResolvesTo("main.css");
  }

  public void testStylesheetHrefResolves() {
    addAssetsTagLib();
    myFixture.addFileToProject("grails-app/assets/stylesheets/main.css", "");

    configureByView("a.gsp", "<asset:stylesheet href=\"main.css<caret>\"/>");

    assertResolvesTo("main.css");
  }

  public void testImageSrcResolves() {
    addAssetsTagLib();
    myFixture.addFileToProject("grails-app/assets/images/logo.png", "");

    configureByView("a.gsp", "<asset:image src=\"logo.png<caret>\"/>");

    assertResolvesTo("logo.png");
  }

  public void testMissingAssetDoesNotResolve() {
    addAssetsTagLib();
    // a sibling file makes grails-app/assets/javascripts exist, so the provider attaches a
    // reference; the referenced file itself is absent and must not resolve.
    myFixture.addFileToProject("grails-app/assets/javascripts/present.js", "");

    configureByView("a.gsp", "<asset:javascript src=\"missing.js<caret>\"/>");

    PsiReference reference = myFixture.getReferenceAtCaretPosition();
    Assert.assertNotNull("expected a file reference on the src attribute", reference);
    Assert.assertNull("a missing asset must not resolve", reference.resolve());
  }

  public void testResolutionSkippedWithoutAssetRoot() {
    // Vanilla project without grails-app/assets: the reference set is soft (no default contexts),
    // so a reference may be attached but must never resolve and must not be flagged as an error.
    addAssetsTagLib();

    configureByView("a.gsp", "<asset:javascript src=\"anything.js<caret>\"/>");

    PsiReference reference = myFixture.getReferenceAtCaretPosition();
    if (reference != null) {
      Assert.assertNull("a missing asset root must not resolve", reference.resolve());
    }
  }

  public void testCompletion() {
    addAssetsTagLib();
    myFixture.addFileToProject("grails-app/assets/javascripts/aaa.js", "");
    myFixture.addFileToProject("grails-app/assets/javascripts/bbb.js", "");

    configureByView("a.gsp", "<asset:javascript src=\"<caret>\"/>");

    myFixture.completeBasic();
    Assert.assertTrue(myFixture.getLookupElementStrings().containsAll(List.of("aaa.js", "bbb.js")));
  }

  public void testRename() {
    addAssetsTagLib();
    myFixture.addFileToProject("grails-app/assets/javascripts/old.js", "");

    configureByView("a.gsp", "<asset:javascript src=\"old.js<caret>\"/>");

    myFixture.renameElementAtCaret("new.js");

    Assert.assertEquals("<asset:javascript src=\"new.js\"/>", myFixture.getFile().getText());
  }
}
