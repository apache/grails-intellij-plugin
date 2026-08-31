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
package org.apache.grails.intellij.plugin.action;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.apache.grails.intellij.plugin.actions.ArtefactData;
import org.apache.grails.intellij.plugin.editor.toolbar.GoToTestAction;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.structure.GrailsApplicationManager;
import org.apache.grails.intellij.plugin.util.GrailsArtifact;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * "Go To Test" collects the tests of every artefact that shares the current artefact's name, and used
 * to require them to share its package too - so a domain kept under {@code model} was invisible from a
 * controller under {@code web}, even though "Go To Domain" reaches it.
 */
public class GoToTestActionTest extends GrailsTestCase {

  private void addProjectWithSplitPackages() {
    addDomain("""

package com.example.model

class Book {
  String title
}
""");
    addController("""

package com.example.web

class BookController {
  def index() {}
}
""");
    myFixture.addFileToProject("test/unit/com/example/model/BookSpec.groovy", """

package com.example.model

class BookSpec {
}
""");
    myFixture.addFileToProject("test/unit/com/example/web/BookControllerSpec.groovy", """

package com.example.web

class BookControllerSpec {
}
""");
  }

  private @NotNull ArtefactData artefactDataForController(@NotNull PsiFile controller) {
    GrailsApplication application = GrailsApplicationManager.findApplication(controller);
    assertNotNull("Grails application not found for " + controller.getName(), application);

    VirtualFile file = controller.getVirtualFile();
    assertNotNull(file);

    return new ArtefactData(getProject(), getModule(), file, "com.example.web", "book", application, false);
  }

  private static @NotNull List<String> fileNames(@NotNull Collection<VirtualFile> files) {
    List<String> names = new ArrayList<>();
    for (VirtualFile file : files) names.add(file.getName());
    return names;
  }

  /** The reported gap: the sibling artefact lives in another package, so its spec was dropped. */
  public void testCollectsTestsOfSiblingArtefactInAnotherPackage() {
    addProjectWithSplitPackages();
    PsiFile controller = addController("""

package com.example.web

class OtherController {
  def index() {}
}
""");

    List<String> targets = fileNames(new GoToTestAction().getNavigateTargets(artefactDataForController(controller)));

    assertTrue("the controller's own spec must be offered, got " + targets, targets.contains("BookControllerSpec.groovy"));
    assertTrue("the domain's spec must be offered even from another package, got " + targets,
               targets.contains("BookSpec.groovy"));
  }

  /** What made the tests unreachable: the strict lookup finds nothing outside the current package. */
  public void testStrictLookupFindsNothingAcrossPackages() {
    addProjectWithSplitPackages();

    assertTrue("no domain named Book exists in com.example.web",
               GrailsArtifact.DOMAIN.getInstances(getModule(), "com.example.web", "book").isEmpty());
    assertFalse("the package-preferring lookup must reach com.example.model.Book",
                GrailsArtifact.DOMAIN.getInstancesPreferringPackage(getModule(), "com.example.web", "book").isEmpty());
  }

  /** An artefact in the current package still wins, so co-located projects keep the narrower result. */
  public void testSamePackageWins() {
    addDomain("""

package com.example.model

class Book {
  String title
}
""");
    addDomain("""

package com.example.web

class Book {
  String title
}
""");

    Collection<?> targets = GrailsArtifact.DOMAIN.getInstancesPreferringPackage(getModule(), "com.example.web", "book");
    assertEquals("only the same-package domain must be offered, got " + targets, 1, targets.size());
  }
}