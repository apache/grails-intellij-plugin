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

package org.jetbrains.plugins.groovy.grails.tagSupport;

import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GspJsSrcTest extends GrailsTestCase {
  public void testHighlighting() {
    myFixture.addFileToProject("web-app/js/site/forms.js", "");

    configureByView("a.gsp", """
      <g:javascript src="site/forms.js" />
      <g:javascript src="site/<error>aaa.js</error>" />
      <g:javascript src="<error>aaa.js</error>" />
      """);

    myFixture.checkHighlighting(true, false, true);
  }
}
