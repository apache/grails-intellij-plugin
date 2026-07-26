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
package org.jetbrains.plugins.grails.gson;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.GsonConstants;
import org.jetbrains.plugins.grails.references.TraitInjectorService;
import org.jetbrains.plugins.groovy.lang.psi.GroovyElementVisitor;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariableDeclaration;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrScriptField;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GroovyScriptClass;
import org.jetbrains.plugins.groovy.transformations.AstTransformationSupport;
import org.jetbrains.plugins.groovy.transformations.TransformationContext;

public final class GsonTransformationSupport implements AstTransformationSupport {

  private static final String VIEW_TYPE = "views";
  private static final String GSON_VIEW_TYPE = "view.gson";

  @Override
  public void applyTransformation(@NotNull TransformationContext context) {
    if (!(context.getCodeClass() instanceof GroovyScriptClass scriptClass)) return;
    GroovyFile file = scriptClass.getContainingFile();
    if (!file.getName().endsWith(GsonConstants.FILE_SUFFIX)) return;

    GrClosableBlock modelClosure = GsonUtils.findModelClosure(file);
    if (modelClosure != null) {
      // Each variable declared in the model closure becomes a script field, so references to it
      // elsewhere in the view resolve.
      modelClosure.acceptChildren(new GroovyElementVisitor() {
        @Override
        public void visitVariableDeclaration(@NotNull GrVariableDeclaration variableDeclaration) {
          for (GrVariable variable : variableDeclaration.getVariables()) {
            context.addField(new GrScriptField(variable, scriptClass));
          }
        }
      });
    }

    context.setSuperType("grails.plugin.json.view.JsonViewTemplate");
    for (String fqn : TraitInjectorService.getInjectedTraits(context.getCodeClass(), VIEW_TYPE)) {
      context.addInterface(fqn);
    }
    for (String fqn : TraitInjectorService.getInjectedTraits(context.getCodeClass(), GSON_VIEW_TYPE)) {
      context.addInterface(fqn);
    }
  }
}
