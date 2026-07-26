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

package com.intellij.groovy.grails.copyright;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.maddyhome.idea.copyright.CopyrightProfile;
import com.maddyhome.idea.copyright.psi.UpdateCopyright;
import org.jetbrains.plugins.grails.fileType.GspFileType;

public class GspCopyrightUpdaterTest extends LightJavaCodeInsightFixtureTestCase {

  private static final String NOTICE = "Test notice line.";

  private String applyCopyright(String source) {
    PsiFile file = myFixture.configureByText(GspFileType.GSP_FILE_TYPE, source);

    CopyrightProfile profile = new CopyrightProfile("Test");
    profile.setNotice(NOTICE);
    profile.setKeyword("Test notice");

    UpdateCopyright updater = new UpdateGspCopyrightsProvider().createInstance(
      getProject(), getModule(), file.getVirtualFile(), GspFileType.GSP_FILE_TYPE, profile);

    WriteCommandAction.runWriteCommandAction(getProject(), () -> {
      updater.prepare();
      try {
        updater.complete();
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

    return file.getText();
  }

  private static final String COMMENT = "%{--\n  - " + NOTICE + "\n  --}%\n\n";

  public void testNoticeInsertedAsGspCommentBeforeRootTag() {
    assertEquals(COMMENT + "<html><body>hi</body></html>",
                 applyCopyright("<html><body>hi</body></html>"));
  }

  /**
   * A GSP page directive is a plain {@code XmlTag}, so it is the first tag the scan finds and the
   * notice lands above it. The JSP original skipped {@code JspDirective} tags here; that check never
   * matched a GSP tree, and this asserts dropping it did not move the comment.
   */
  public void testNoticeInsertedAbovePageDirective() {
    assertEquals(COMMENT + "<%@ page import=\"java.util.List\" %>\n<html><body>hi</body></html>",
                 applyCopyright("<%@ page import=\"java.util.List\" %>\n<html><body>hi</body></html>"));
  }

  public void testNoticeInsertedBeforeDoctype() {
    assertEquals(COMMENT + "<!DOCTYPE html>\n<html><body>hi</body></html>",
                 applyCopyright("<!DOCTYPE html>\n<html><body>hi</body></html>"));
  }

  public void testNoticeInsertedIntoEmptyFile() {
    assertEquals(COMMENT, applyCopyright(""));
  }

  /**
   * The updater must recognise a notice it already wrote rather than stack a second copy. Only the
   * occurrence count is asserted: re-running also re-pads the blank line after the comment, which is
   * blank-line handling inside the platform's {@code UpdatePsiFileCopyright} and not GSP-specific.
   */
  public void testNoticeNotDuplicatedOnSecondRun() {
    String twice = applyCopyright(applyCopyright("<html><body>hi</body></html>"));
    assertEquals(1, StringUtil.getOccurrenceCount(twice, NOTICE));
  }
}
