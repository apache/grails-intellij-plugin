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

package org.apache.grails.intellij.plugin.reference;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

import java.io.IOException;
import java.io.UncheckedIOException;

public class GrailsJobTest extends GrailsTestCase {
  @Override
  protected void configureModule(Module module, ModifiableRootModel model, ContentEntry contentEntry) {
    TempDirTestFixture tdf = myFixture.getTempDirFixture();
    VirtualFile file;
    try {
      file = tdf.findOrCreateDir("grails-app/jobs");
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    contentEntry.addSourceFolder(file, false);
  }

  public void testResolve() {
    PsiFile jobFile = myFixture.addFileToProject("grails-app/jobs/JjjJob.groovy", """
      class JjjJob {
        static triggers = {
          if (true) {
            simple [:]
          }
      
          cron [:]
        };
      
        {
          schedule(new Date())
          JjjJob.schedule(new Date())
          def l = getLog()
          l = log
        }
      
        static {
          schedule(new Date())
          JjjJob.schedule(new Date())
          def l = getLog()
          l = log
        }
      
      }
      """);
    GrailsTestCase.checkResolve(jobFile, "getLog", "getLog");
  }
}
