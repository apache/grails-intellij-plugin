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

package org.apache.grails.intellij.plugin;


import org.apache.grails.intellij.lib.testFramework.GrailsTestUtil;
import org.apache.grails.intellij.lib.testFramework.HddGrailsTestCase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.UsefulTestCase;
import org.apache.grails.intellij.plugin.config.GrailsFramework;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;

public class DoublePluginTest extends HddGrailsTestCase {
  public void testDoublePlugin() throws IOException {
    LinkedHashMap<String, String> map = new LinkedHashMap<>(1);
    map.put("grails.project.plugins.dir", "./myplugins");
    GrailsTestUtil.createBuildConfig(myFixture, ".", map);

    GrailsTestUtil.createGrailsApplication(myFixture, "./plugins/pluginA-1.0", false);
    GrailsTestUtil.createPluginXml(myFixture, "./plugins/pluginA-1.0");

    GrailsTestUtil.createGrailsApplication(myFixture, "./plugins/pluginB-1.0", false);
    GrailsTestUtil.createPluginXml(myFixture, "./plugins/pluginB-1.0");

    GrailsTestUtil.createGrailsApplication(myFixture, "./myplugins/pluginA-1.0", false);
    GrailsTestUtil.createPluginXml(myFixture, "./myplugins/pluginA-1.0");

    GrailsTestUtil.createGrailsApplication(myFixture, "./myplugins/pluginC-1.0", false);
    GrailsTestUtil.createPluginXml(myFixture, "./myplugins/pluginC-1.0");

    Collection<VirtualFile> plugins = GrailsFramework.getInstance().getAllPluginRoots(myFixture.getModule(), true);

    UsefulTestCase.assertSize(3, plugins);
  }
}
