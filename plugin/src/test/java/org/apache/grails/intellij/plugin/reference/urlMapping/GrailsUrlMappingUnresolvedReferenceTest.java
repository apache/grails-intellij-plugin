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

package org.apache.grails.intellij.plugin.reference.urlMapping;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.codeInspection.untypedUnresolvedAccess.GrUnresolvedAccessInspection;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

import java.io.IOException;
import java.io.UncheckedIOException;

public class GrailsUrlMappingUnresolvedReferenceTest extends GrailsTestCase {
  public void testUnresolvedHighlight() {
    myFixture.enableInspections(GrUnresolvedAccessInspection.class);

    PsiFile file = myFixture.addFileToProject("grails-app/conf/MyUrlMappings.groovy", """
      class MyUrlMappings {
        static mappings = {
          "/abc"(controller: <warning>aaa</warning>)
          "/aaa/$bbb"(controller: <warning>bbb</warning>)
      
          if (<warning>fff</warning>()) {
      
          }
      
          "/fff" {
            action = []
            controller = ""
            <warning>rrrr</warning> = ""
      
            constraints {
            }
          }
      
          name xxx: "/fff" {
            action = []
            controller = ""
            <warning>rrrr</warning> = ""
      
            constraints {
            }
          }
        }
      }
      """);

    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());

    myFixture.checkHighlighting(true, false, true);
  }

  @Override
  protected void configureModule(Module module, ModifiableRootModel model, ContentEntry contentEntry) {
    VirtualFile dir;
    try {
      dir = myFixture.getTempDirFixture().findOrCreateDir("grails-app/conf");
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    contentEntry.addSourceFolder(dir, false);
  }

  @Override
  public boolean needUrlMappings() {
    return true;
  }
}
