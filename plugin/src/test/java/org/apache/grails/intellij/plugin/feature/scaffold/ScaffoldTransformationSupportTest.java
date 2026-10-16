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

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;

/**
 * {@code @Scaffold} replaces a controller's or service's superclass at compile time. The PSI must show
 * the same superclass, or the scaffolded actions neither resolve nor complete, a {@code def show()} in
 * the class carries no "overrides" marker, and nothing tells the user what the annotation did.
 */
public class ScaffoldTransformationSupportTest extends GrailsTestCase {

  private static final String QUEST = """

package com.kousenit

class Quest {
  String name
}
""";

  private void addScaffolding() {
    ScaffoldingStubs.addTo(myFixture);
    addDomain(QUEST);
  }

  private static @NotNull GrTypeDefinition classOf(@NotNull PsiFile file) {
    return ((GroovyFile)file).getTypeDefinitions()[0];
  }

  private static @Nullable String superType(@NotNull GrTypeDefinition clazz) {
    PsiClassType[] types = clazz.getExtendsListTypes(true);
    return types.length == 0 ? null : types[0].getCanonicalText();
  }

  public void testDomainOnlyControllerExtendsRestfulController() {
    addScaffolding();
    GrTypeDefinition controller = classOf(addController("""

package com.kousenit

import grails.plugin.scaffolding.annotation.Scaffold

@Scaffold(Quest)
class QuestController {
}
"""));

    assertEquals("grails.rest.RestfulController<com.kousenit.Quest>", superType(controller));
    PsiClass superClass = controller.getSuperClass();
    assertNotNull(superClass);
    assertEquals("grails.rest.RestfulController", superClass.getQualifiedName());
  }

  /** {@code generate-all}'s own form: the base with the domain as its type argument. */
  public void testParameterisedBaseInValue() {
    addScaffolding();
    GrTypeDefinition controller = classOf(addController("""

package com.kousenit

import grails.plugin.scaffolding.RestfulServiceController
import grails.plugin.scaffolding.annotation.Scaffold

@Scaffold(RestfulServiceController<Quest>)
class QuestController {
}
"""));

    assertEquals("grails.plugin.scaffolding.RestfulServiceController<com.kousenit.Quest>", superType(controller));
  }

  public void testExplicitBaseAndDomain() {
    addScaffolding();
    GrTypeDefinition controller = classOf(addController("""

package com.kousenit

import grails.plugin.scaffolding.RestfulServiceController
import grails.plugin.scaffolding.annotation.Scaffold

@Scaffold(value = RestfulServiceController, domain = Quest)
class QuestController {
}
"""));

    assertEquals("grails.plugin.scaffolding.RestfulServiceController<com.kousenit.Quest>", superType(controller));
  }

  public void testDomainClassLiteral() {
    addScaffolding();
    GrTypeDefinition controller = classOf(addController("""

package com.kousenit

import grails.plugin.scaffolding.annotation.Scaffold

@Scaffold(domain = Quest.class)
class QuestController {
}
"""));

    assertEquals("grails.rest.RestfulController<com.kousenit.Quest>", superType(controller));
  }

  public void testServiceExtendsGormService() {
    addScaffolding();
    GrTypeDefinition service = classOf(addService("""

package com.kousenit

import grails.plugin.scaffolding.annotation.Scaffold

@Scaffold(Quest)
class CastleService {
}
"""));

    assertEquals("grails.plugin.scaffolding.GormService<com.kousenit.Quest>", superType(service));
    assertSize(1, service.findMethodsByName("save", true));
  }

  /** The deprecated form the controller injector still accepts and warns about. */
  public void testLegacyStaticScaffoldProperty() {
    addScaffolding();
    GrTypeDefinition controller = classOf(addController("""

package com.kousenit

class QuestController {
  static scaffold = Quest
}
"""));

    assertEquals("grails.rest.RestfulController<com.kousenit.Quest>", superType(controller));
  }

  /** The actions are inherited: they resolve, and declaring one overrides it (what the gutter marker reads). */
  public void testActionsAreInheritedAndOverridable() {
    addScaffolding();
    GrTypeDefinition controller = classOf(addController("""

package com.kousenit

import grails.plugin.scaffolding.annotation.Scaffold

@Scaffold(Quest)
class QuestController {
  def show() {
    respond Quest.get(params.id)
  }
}
"""));

    PsiMethod[] index = controller.findMethodsByName("index", true);
    assertSize(1, index);
    assertEquals("grails.rest.RestfulController", index[0].getContainingClass().getQualifiedName());
    assertSize(1, controller.findMethodsByName("queryForResource", true));

    PsiMethod show = controller.findCodeMethodsByName("show", false)[0];
    PsiMethod[] overridden = show.findSuperMethods();
    assertSize(1, overridden);
    assertEquals("grails.rest.RestfulController", overridden[0].getContainingClass().getQualifiedName());
  }

  /** ResourceTransform.addConstructor(): the generated no-arg constructor that hands the domain to the base. */
  public void testGeneratedConstructorNavigatesToTheAnnotation() {
    addScaffolding();
    GrTypeDefinition controller = classOf(addController("""

package com.kousenit

import grails.plugin.scaffolding.annotation.Scaffold

@Scaffold(Quest)
class QuestController {
}
"""));

    PsiMethod[] constructors = controller.getConstructors();
    assertSize(1, constructors);
    assertEmpty(constructors[0].getParameterList().getParameters());
    assertEquals("@Scaffold(Quest)", constructors[0].getNavigationElement().getText());
  }

  /** "Scaffolded controllers cannot extend other classes" is a compile error, so the declared superclass stays. */
  public void testDeclaredSuperclassIsKept() {
    addScaffolding();
    myFixture.addClass("package com.kousenit; public class Base {}");
    GrTypeDefinition controller = classOf(addController("""

package com.kousenit

import grails.plugin.scaffolding.annotation.Scaffold

@Scaffold(Quest)
class QuestController extends Base {
}
"""));

    assertEquals("com.kousenit.Base", superType(controller));
    assertNull(Scaffolding.resolve(controller));
  }

  /** The injectors only run on controllers and services. */
  public void testClassOutsideArtefactDirectoriesIsUntouched() {
    addScaffolding();
    GrTypeDefinition helper = classOf(myFixture.addFileToProject("src/main/groovy/com/kousenit/QuestHelper.groovy", """

package com.kousenit

import grails.plugin.scaffolding.annotation.Scaffold

@Scaffold(Quest)
class QuestHelper {
}
"""));

    assertNull(superType(helper));
    assertNull(Scaffolding.resolve(helper));
  }

  /** Without grails-scaffolding on the classpath there is no base to extend, and nothing to do. */
  public void testWithoutScaffoldingLibrary() {
    addDomain(QUEST);
    GrTypeDefinition controller = classOf(addController("""

package com.kousenit

import grails.plugin.scaffolding.annotation.Scaffold

@Scaffold(Quest)
class QuestController {
}
"""));

    assertNull(superType(controller));
    assertEmpty(controller.findMethodsByName("index", true));
  }

  public void testPlainControllerIsUntouched() {
    addScaffolding();
    GrTypeDefinition controller = classOf(addController("""

package com.kousenit

class QuestController {
  def index() {}
}
"""));

    assertNull(superType(controller));
    assertEmpty(controller.getConstructors());
  }
}
