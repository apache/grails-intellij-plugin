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
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.apache.grails.intellij.plugin.actions.ArtefactData;
import org.apache.grails.intellij.plugin.editor.toolbar.GoToControllerAction;
import org.apache.grails.intellij.plugin.editor.toolbar.GoToServiceAction;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.structure.GrailsApplicationManager;
import org.apache.grails.intellij.plugin.util.GrailsArtifact;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;
import org.jetbrains.plugins.groovy.util.GroovyUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A GORM data service is an interface ({@code @Service(Quest) interface QuestService}), not a class,
 * and the artefact lookups used to accept classes only - so the domain-class toolbar said "Nothing
 * here" for the service that {@code generate-all} had just written, while a hand-written
 * {@code class CastleService} resolved fine.
 */
public class GoToServiceActionTest extends GrailsTestCase {

  private static final String QUEST_DOMAIN = """

package com.kousenit

class Quest {
  String name
}
""";

  private static final String QUEST_DATA_SERVICE = """

package com.kousenit

import grails.gorm.services.Service

@Service(Quest)
interface QuestService {
  Quest get(Serializable id)
  Quest save(Quest quest)
}
""";

  private @NotNull ArtefactData artefactDataFor(@NotNull PsiFile artefact, @NotNull String artefactName) {
    GrailsApplication application = GrailsApplicationManager.findApplication(artefact);
    assertNotNull("Grails application not found for " + artefact.getName(), application);

    VirtualFile file = artefact.getVirtualFile();
    assertNotNull(file);

    return new ArtefactData(getProject(), getModule(), file, "com.kousenit", artefactName, application, false);
  }

  private static @NotNull List<String> names(@NotNull Collection<? extends PsiClass> classes) {
    List<String> names = new ArrayList<>();
    for (PsiClass aClass : classes) names.add(aClass.getQualifiedName());
    return names;
  }

  private @NotNull PsiClass publicClass(@NotNull PsiFile file) {
    PsiClass aClass = GroovyUtils.getPublicClass(getProject(), file.getVirtualFile());
    assertNotNull("no public class in " + file.getName(), aClass);
    return aClass;
  }

  /** The reported gap: "Go To Service" from the domain found nothing for the interface. */
  public void testDataServiceInterfaceIsOfferedFromDomain() {
    PsiFile domain = addDomain(QUEST_DOMAIN);
    addService(QUEST_DATA_SERVICE);

    Collection<GrTypeDefinition> targets = new GoToServiceAction().getNavigateTargets(artefactDataFor(domain, "quest"));

    assertEquals(List.of("com.kousenit.QuestService"), names(targets));
    assertTrue("the target must be the interface itself", targets.iterator().next().isInterface());
  }

  /** The way it always worked: a service declared as a class keeps resolving. */
  public void testClassServiceIsStillOffered() {
    PsiFile domain = addDomain("""

package com.kousenit

class Castle {
  String name
}
""");
    addService("""

package com.kousenit

class CastleService {
  def listCastles() { Castle.list() }
}
""");

    Collection<GrTypeDefinition> targets = new GoToServiceAction().getNavigateTargets(artefactDataFor(domain, "castle"));

    assertEquals(List.of("com.kousenit.CastleService"), names(targets));
  }

  /** The interface is a service artefact in its own right, so its own editor gets the toolbar and the related actions. */
  public void testDataServiceInterfaceIsAServiceArtefact() {
    PsiClass service = publicClass(addService(QUEST_DATA_SERVICE));

    assertSame(GrailsArtifact.SERVICE, GrailsArtifact.getType(service));
    assertTrue(GrailsArtifact.SERVICE.isInstance(service));
    assertEquals("quest", GrailsArtifact.SERVICE.getArtifactName(service));
  }

  /** The annotation is not checked: a {@code *Service} interface under {@code services} is a service without {@code @Service} too. */
  public void testUnannotatedServiceInterfaceIsAServiceArtefact() {
    PsiClass service = publicClass(addService("""

package com.kousenit

interface PricingService {
  BigDecimal priceOf(Quest quest)
}
"""));

    assertSame(GrailsArtifact.SERVICE, GrailsArtifact.getType(service));
    assertTrue(GrailsArtifact.SERVICE.isInstance(service));
    assertEquals("pricing", GrailsArtifact.SERVICE.getArtifactName(service));
  }

  /** From the service's own toolbar, "Go To Domain" and "Go To Controller" still resolve the siblings. */
  public void testNavigationFromDataServiceInterface() {
    addDomain(QUEST_DOMAIN);
    addController("""

package com.kousenit

class QuestController {
  QuestService questService
  def index() {}
}
""");
    PsiFile service = addService(QUEST_DATA_SERVICE);

    ArtefactData data = artefactDataFor(service, "quest");
    assertEquals(List.of("com.kousenit.QuestController"), names(new GoToControllerAction().getNavigateTargets(data)));
    assertEquals(List.of("com.kousenit.QuestService"), names(new GoToServiceAction().getNavigateTargets(data)));
  }

  /** Only services may be interfaces: an interface under {@code controllers} is not a controller. */
  public void testInterfaceIsNotAnArtefactOfOtherKinds() {
    PsiClass controller = publicClass(addController("""

package com.kousenit

interface QuestController {
  def index()
}
"""));

    assertNull(GrailsArtifact.getType(controller));
    assertFalse(GrailsArtifact.CONTROLLER.isInstance(controller));
    assertTrue("an interface must not be offered as a controller",
               GrailsArtifact.CONTROLLER.getInstances(getModule(), "quest").isEmpty());
  }
}
