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

import com.intellij.facet.FacetManager;
import com.intellij.javaee.web.facet.WebFacet;
import com.intellij.javaee.web.WebRoot;
import com.intellij.maven.testFramework.MavenMultiVersionImportingTestCase;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.grails.intellij.plugin.config.GrailsFramework;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GrailsMavenImporterTest extends MavenMultiVersionImportingTestCase {

  private void createGrailsStdFolders() {
    createProjectSubDirs("grails-app/conf/hibernate",
                         "grails-app/controllers",
                         "grails-app/domain",
                         "grails-app/i18n",
                         "grails-app/services",
                         "grails-app/taglib",
                         "grails-app/utils",
                         "grails-app/views/layouts",
                         "lib",
                         "src/groovy",
                         "src/java",
                         "test/unit",
                         "test/integration",
                         "web-app/WEB-INF");
  }

  @Test
  public void testImportGrailsProject() {
    createStdProjectFolders("");
    createGrailsStdFolders();

    importProject("  <groupId>test</groupId>" +
                  "  <artifactId>project</artifactId>" +
                  "  <packaging>war</packaging>" +
                  "  <version>1</version>" +
                  "  <dependencies>" +
                  "    <dependency>" +
                  "      <groupId>org.grails</groupId>" +
                  "      <artifactId>grails-crud</artifactId>" +
                  "      <version>1.1</version>" +
                  "    </dependency>" +
                  "    <dependency>" +
                  "      <groupId>org.grails</groupId>" +
                  "      <artifactId>grails-gorm</artifactId>" +
                  "      <version>1.1</version>" +
                  "    </dependency>" +
                  "  </dependencies>" +
                  "  <build>" +
                  "    <pluginManagement />" +
                  "    <plugins>" +
                  "      <plugin>" +
                  "        <groupId>org.grails</groupId>" +
                  "        <artifactId>grails-maven-plugin</artifactId>" +
                  "        <version>1.0</version>" +
                  "      </plugin>" +
                  "    </plugins>" +
                  "  </build>", false);

    assertModules("project");

    assertSources("project",
                  "grails-app/controllers",
                  "grails-app/domain",
                  "grails-app/i18n",
                  "grails-app/services",
                  "grails-app/taglib",
                  "grails-app/utils",
                  "grails-app/jobs",
                  "grails-app/realms",
                  "src/groovy",
                  "src/java",
                  "src/main/java",
                  "src/gwt",
                  "src/scala");
    assertDefaultResources("project", "grails-app/resources");

    assertTestSources("project",
                      "src/test/java",
                      "test/integration",
                      "test/functional",
                      "test/unit");
    assertDefaultTestResources("project");

    Module module = getModule("project");

    WebFacet webFacet = FacetManager.getInstance(module).findFacet(WebFacet.ID, "GrailsWeb");
    assertNotNull(webFacet);

    VirtualFile appRoot = GrailsFramework.getInstance().findAppRoot(module);
    assertNotNull(appRoot);

    List<VirtualFile> shouldBeRoot = new ArrayList<>(Arrays.asList(
      VfsUtil.findRelativeFile(appRoot, "web-app"),
      VfsUtil.findRelativeFile(appRoot, "grails-app", "views")
    ));

    for (WebRoot webRoot : webFacet.getWebRoots()) {
      shouldBeRoot.remove(webRoot.getFile());
    }

    assertTrue("Following web folder was not added to web roots: " + shouldBeRoot + "; " + webFacet.getWebRoots(),
               shouldBeRoot.isEmpty());
  }
}
