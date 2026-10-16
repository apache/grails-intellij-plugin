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
package org.apache.grails.intellij.plugin.editor;

import com.intellij.psi.PsiFile;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

/**
 * Which Groovy files get the Grails artefact toolbar. A GORM data service is an interface, and it
 * must get the toolbar like a service written as a class does.
 */
public class GrailsEditorToolbarTest extends GrailsTestCase {

  private boolean decorated(PsiFile file) {
    return GrailsEditorToolbar.shouldBeDecorated(getProject(), file.getVirtualFile());
  }

  public void testDataServiceInterfaceGetsTheToolbar() {
    PsiFile service = addService("""

package com.kousenit

import grails.gorm.services.Service

@Service(Quest)
interface QuestService {
  Quest get(Serializable id)
}
""");

    assertTrue(decorated(service));
  }

  public void testClassServiceGetsTheToolbar() {
    PsiFile service = addService("""

package com.kousenit

class CastleService {
  def listCastles() {}
}
""");

    assertTrue(decorated(service));
  }

  public void testInterfaceUnderControllersIsNotDecorated() {
    PsiFile controller = addController("""

package com.kousenit

interface QuestController {
  def index()
}
""");

    assertFalse(decorated(controller));
  }
}
