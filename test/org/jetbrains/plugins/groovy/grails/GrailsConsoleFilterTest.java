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

package org.jetbrains.plugins.groovy.grails;

import com.intellij.execution.filters.Filter;
import com.intellij.testFramework.UsefulTestCase;
import org.jetbrains.plugins.grails.GrailsConsoleFilterProvider;
import org.junit.Assert;

public class GrailsConsoleFilterTest extends GrailsTestCase {

  private void doTest(String str, String res) {
    addView("index.gsp", "");

    Filter[] filters = new GrailsConsoleFilterProvider().getDefaultFilters(getProject());
    UsefulTestCase.assertSize(1, filters);

    Filter filter = filters[0];

    Filter.Result r = filter.applyFilter(str, str.length());
    if (res == null) {
      Assert.assertNull(r);
    }
    else {
      Assert.assertNotNull(r);

      String s = str.substring(0, r.getHighlightStartOffset()) + "!" + str.substring(r.getHighlightEndOffset());
      Assert.assertEquals(res, s);
    }
  }

  public void testConsoleFilter1() {
    doTest(
      "\tat media_SSD_ideaProjects_src_grails_app_views_index_gsp$_run_closure2.doCall(media_SSD_ideaProjects_src_grails_app_views_index_gsp:32)\n\n",
      "\tat media_SSD_ideaProjects_src_grails_app_views_index_gsp$_run_closure2.doCall(!:32)\n\n");
  }

  public void testConsoleFilter2() {
    doTest(
      "\tat media_SSD_ideaProjects_src_grails_app_views_index_gsp$_run_closure2.doCall(media_SSD_ideaProjects_src_grails_app_views_index_gsp)",
      "\tat media_SSD_ideaProjects_src_grails_app_views_index_gsp$_run_closure2.doCall(!)");
  }

  public void testConsoleFilter3() {
    doTest("\tat media_SSD_ideaProjects_src_grails_app_views_index_gsp.run(media_SSD_ideaProjects_src_grails_app_views_index_gsp:86)",
           "\tat media_SSD_ideaProjects_src_grails_app_views_index_gsp.run(!:86)");
  }

  public void testConsoleFilter4() {
    doTest("\tat media_SSD_ideaProjects_kkk_src_grails_app_views_index_gsp.run(index.gsp:53) ",
           "\tat media_SSD_ideaProjects_kkk_src_grails_app_views_index_gsp.run(!:53) ");
  }

  public void testConsoleFilter5() {
    doTest("\tat media_SSD_ideaProjects_kkk_src_grails_app_views_index_gsp$aaa.run(index.gsp) ",
           "\tat media_SSD_ideaProjects_kkk_src_grails_app_views_index_gsp$aaa.run(!) ");
  }

  public void testConsoleFilter6() {
    doTest("\tat media_SSD_ideaProjects_src_grails_app_views_index_gsp.run(media_SSD_ideaProjects_src_grails_app_views_index_gsp:-86)",
           "\tat media_SSD_ideaProjects_src_grails_app_views_index_gsp.run(!:-86)");
  }
}
