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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.GrModifierFlags;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.transformations.AstTransformationSupport;
import org.jetbrains.plugins.groovy.transformations.TransformationContext;

/**
 * Applies {@code @Scaffold} to the PSI the way Grails applies it to the AST: the class extends
 * {@code RestfulController<Domain>} / {@code GormService<Domain>} (or the base the annotation names)
 * and gets the no-arg constructor the transform adds.
 *
 * <p>Everything else follows from the superclass: the scaffolded actions resolve and complete, a
 * method declared in the class gets the platform's "overrides" gutter marker, the structure view lists
 * the inherited actions, and "Go To View" and GSP action references see them as the controller's actions.
 *
 * @see Scaffolding
 */
public final class ScaffoldTransformationSupport implements AstTransformationSupport {

  @Override
  public void applyTransformation(@NotNull TransformationContext context) {
    GrTypeDefinition clazz = context.getCodeClass();
    Scaffolding.ScaffoldInfo info = Scaffolding.resolve(clazz);
    if (info == null) return;

    context.setSuperType(info.baseType());

    // ResourceTransform.addConstructor(): Foo() { super(Domain, readOnly) }
    String name = clazz.getName();
    if (name != null) {
      GrLightMethodBuilder constructor = new GrLightMethodBuilder(context.getManager(), name);
      constructor.setConstructor(true);
      constructor.addModifier(GrModifierFlags.PUBLIC_MASK);
      constructor.setContainingClass(clazz);
      constructor.setNavigationElement(info.anchor());
      context.addMethod(constructor);
    }
  }
}
