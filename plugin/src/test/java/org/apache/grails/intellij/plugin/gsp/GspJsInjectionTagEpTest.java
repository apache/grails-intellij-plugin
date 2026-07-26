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

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.apache.grails.intellij.plugin.addins.js.GspJsInjectionTagBean;
import org.apache.grails.intellij.plugin.addins.js.JavaScriptIntegrationUtil;

/**
 * Verifies the {@code org.intellij.grails.gspJsInjectionTag} extension point: contributed tags are
 * recognized by {@link JavaScriptIntegrationUtil#isJsInjectionTag(String)}, and the modification
 * count that drives cache invalidation bumps when contributions change.
 */
public class GspJsInjectionTagEpTest extends BasePlatformTestCase {

  public void testCoreDefaultsAlwaysRecognized() {
    assertTrue(JavaScriptIntegrationUtil.isJsInjectionTag("g:javascript"));
    assertTrue(JavaScriptIntegrationUtil.isJsInjectionTag("r:script"));
    // asset:script ships as a core gspJsInjectionTag contribution (see plugin.xml).
    assertTrue(JavaScriptIntegrationUtil.isJsInjectionTag("asset:script"));
  }

  public void testContributedTagRecognizedAndModCountBumps() {
    // A tag not shipped by core: recognized only once contributed through the EP.
    String tag = "myplugin:script";
    assertFalse(JavaScriptIntegrationUtil.isJsInjectionTag(tag));

    long before = JavaScriptIntegrationUtil.getInjectionTagModificationCount();

    GspJsInjectionTagBean bean = new GspJsInjectionTagBean();
    bean.name = tag;
    GspJsInjectionTagBean.EP_NAME.getPoint().registerExtension(bean, getTestRootDisposable());

    assertTrue(JavaScriptIntegrationUtil.isJsInjectionTag(tag));
    assertTrue("mod count must bump on EP change",
               JavaScriptIntegrationUtil.getInjectionTagModificationCount() > before);
  }
}
