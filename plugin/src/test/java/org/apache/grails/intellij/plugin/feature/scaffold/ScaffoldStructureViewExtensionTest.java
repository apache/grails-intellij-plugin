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
package org.apache.grails.intellij.plugin.feature.scaffold;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.structureView.StructureViewFactoryEx;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.InheritedMembersNodeProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.SimpleTextAttributes;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * The Structure view lists the methods {@code @Scaffold} gives a class under its node, greyed and
 * located in the base, without "Show Inherited" - and steps aside when that toggle is on.
 */
public class ScaffoldStructureViewExtensionTest extends GrailsTestCase {

  private final ScaffoldStructureViewExtension myExtension = new ScaffoldStructureViewExtension();

  private @NotNull StructureViewTreeElement[] childrenOf(@NotNull PsiFile file) {
    GrTypeDefinition clazz = ((GroovyFile)file).getTypeDefinitions()[0];
    assertSame(GrTypeDefinition.class, myExtension.getType());
    return myExtension.getChildren(clazz);
  }

  private static @NotNull List<String> texts(StructureViewTreeElement @NotNull [] elements) {
    List<String> texts = new ArrayList<>();
    for (StructureViewTreeElement element : elements) texts.add(element.getPresentation().getPresentableText());
    return texts;
  }

  private @NotNull PsiFile addScaffoldedController() {
    ScaffoldingStubs.addTo(myFixture);
    addDomain("""

package com.kousenit

class Quest {
  String name
}
""");
    return addController("""

package com.kousenit

import grails.plugin.scaffolding.annotation.Scaffold

@Scaffold(Quest)
class QuestController {
  def show() {}
}
""");
  }

  public void testScaffoldedMethodsAreListedGreyedUnderTheClass() {
    StructureViewTreeElement[] children = childrenOf(addScaffoldedController());

    assertEquals(List.of("def index(Integer max)", "def create()", "def save()", "def edit()",
                         "def patch()", "def update()", "def delete()"),
                 texts(children));

    StructureViewTreeElement index = children[0];
    PresentationData presentation = (PresentationData)index.getPresentation();
    assertEquals("RestfulController", presentation.getLocationString());
    assertEquals(SimpleTextAttributes.GRAYED_ATTRIBUTES, presentation.getColoredText().get(0).getAttributes());
    assertEquals("index", ((PsiMethod)index.getValue()).getName());
    assertTrue(index.canNavigate());
  }

  /** With "Show Inherited" on, the platform lists them already (the superclass is set), so nothing is added twice. */
  public void testStepsAsideWhenShowInheritedIsOn() {
    PsiFile controller = addScaffoldedController();
    StructureViewFactoryEx factory = StructureViewFactoryEx.getInstanceEx(getProject());
    factory.setActiveAction(InheritedMembersNodeProvider.ID, true);
    try {
      assertEmpty(childrenOf(controller));
    }
    finally {
      factory.setActiveAction(InheritedMembersNodeProvider.ID, false);
    }
    assertSize(7, childrenOf(controller));
  }

  public void testPlainControllerAddsNothing() {
    ScaffoldingStubs.addTo(myFixture);
    PsiFile controller = addController("""

package com.kousenit

class QuestController {
  def index() {}
}
""");

    assertEmpty(childrenOf(controller));
  }
}
