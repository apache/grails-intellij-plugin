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

public class GspIncludeViewAttributeTest extends GrailsTestCase {
  public void testHighlighting() {
    addView("ccc/a.gsp", "");

    configureByView("ccc/test.gsp", """
      <g:include view="/ccc/a.gsp" />
      <g:include view="ccc/a.gsp" />
      
      <g:include view="<error>a.gsp</error>" />
      <g:include view="ccc/a.gsp/<error></error>" />
      <g:include view="/<error>a.gsp</error>" />
      <g:include view="<error>a</error>" />
      <g:include view="/ccc/<error>a</error>" />
      """);

    myFixture.checkHighlighting(true, false, true);
  }

  public void testRename() {
    addView("ccc/a.gsp", "");

    configureByView("ccc/test.gsp", """
      <g:include view="/ccc/a.gsp" />
      <g:include view="ccc/a.gsp<caret>" />
      ${ include(view: ""\"ccc/a.gsp""\") }
      ${ include(view: '/ccc/a.gsp') }
      """);

    myFixture.renameElementAtCaret("zzz.gsp");

    myFixture.checkResult("""
                            <g:include view="/ccc/zzz.gsp" />
                            <g:include view="ccc/zzz.gsp" />
                            ${ include(view: ""\"ccc/zzz.gsp""\") }
                            ${ include(view: '/ccc/zzz.gsp') }
                            """);
  }
}
