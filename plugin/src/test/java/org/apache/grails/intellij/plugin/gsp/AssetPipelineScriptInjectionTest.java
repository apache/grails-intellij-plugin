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
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

/**
 * Verifies that {@code <asset:script>} bodies (contributed via the asset-pipeline module through the
 * {@code gspJsInjectionTag} extension point) receive the same JavaScript treatment as
 * {@code <g:javascript>}: language injection and cross-tag symbol visibility.
 */
public class AssetPipelineScriptInjectionTest extends GrailsTestCase {

  private void addAssetsTagLib() {
    addTaglib("class AssetsTagLib { static namespace = \"asset\" }");
  }

  public void testCompletionAcrossAssetScriptBlocks() {
    addAssetsTagLib();

    PsiFile file = addView("a.gsp", """
      <html>
      <script>
        var xxx1 = 1;
      </script>
      <asset:script>
        var xxx2 = 2
      </asset:script>

      <asset:script>
        xxx<caret>
      </asset:script>
      </html>
      """);

    checkCompletionVariants(file, "xxx1", "xxx2");
  }

  public void testFunctionCompletionAcrossAssetScriptBlocks() {
    addAssetsTagLib();

    PsiFile file = addView("a.gsp", """
      <html>
      <script>
        function xxx1() {}
      </script>
      <asset:script>
        function xxx2() {}
      </asset:script>

      <asset:script>
        xxx<caret>
      </asset:script>
      </html>
      """);

    checkCompletionVariants(file, "xxx1", "xxx2");
  }
}
