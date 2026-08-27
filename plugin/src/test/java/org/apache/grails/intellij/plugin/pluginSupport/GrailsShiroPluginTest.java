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

package org.apache.grails.intellij.plugin.pluginSupport;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.apache.grails.intellij.lib.testFramework.GrailsTestUtil.getTestRootPath;

public class GrailsShiroPluginTest extends GrailsTestCase {
  @Override
  protected void configureModule(Module module, ModifiableRootModel model, ContentEntry contentEntry) {
    try {
      VirtualFile applicationProperties = myFixture.getTempDirFixture().findOrCreateDir("application.properties");
      applicationProperties.setBinaryContent("plugins.shiro=1.1.3".getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void addAccessControllerBuilder() {
    myFixture.copyFileToProject("AccessControlBuilder.groovy", "src/groovy/org/apache/shiro/grails/AccessControlBuilder.groovy");
  }

  @Override
  protected String getTestDataPath() {
    return getTestRootPath("/testdata/grails/shiro/");
  }

  public void testSearchableFieldCompletion() {
    configureByController("""
                            class CccController {
                              static <caret>
                            }
                            """);
    checkCompletion("accessControl");
  }

  public void testCompletionMethodsFromAccessControlBuilder() {
    addAccessControllerBuilder();

    configureByController("""
                            class CccController {
                              static accessControl = {
                                <caret>
                              }
                            }
                            """);
    checkCompletion("role", "permission");
  }

  public void testResolveMethodsFromAccessControlBuilder() {
    addAccessControllerBuilder();

    PsiFile file = addController("""
                                   class CccController {
                                     static accessControl = {
                                       role(name: 'Editor', only:['createNews'] )
                                     }
                                   
                                     def createNews = {}
                                   }
                                   """);
    GrailsTestCase.checkResolve(file);
  }

  public void testActionReferenceRename() {
    addAccessControllerBuilder();

    configureByController("""
                            class CccController {
                              static accessControl = {
                                role(name: 'RoleName1', only:["xxx", "yyy"] )
                                permission(name: 'RoleName1', only:["xxx", "yyy"] )
                            
                                role(name: 'RoleName2', action: 'xxx')
                                permission(name: 'RoleName2', action: 'xxx')
                              }
                            
                              def xxx<caret> = {}
                            
                              def yyy = {}
                            }
                            """);

    myFixture.renameElementAtCaret("f");

    myFixture.checkResult("""
                            class CccController {
                              static accessControl = {
                                role(name: 'RoleName1', only:["f", "yyy"] )
                                permission(name: 'RoleName1', only:["f", "yyy"] )
                            
                                role(name: 'RoleName2', action: 'f')
                                permission(name: 'RoleName2', action: 'f')
                              }
                            
                              def f = {}
                            
                              def yyy = {}
                            }
                            """);
  }
}
