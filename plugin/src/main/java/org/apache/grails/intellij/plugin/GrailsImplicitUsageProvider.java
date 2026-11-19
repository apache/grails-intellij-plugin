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

package org.apache.grails.intellij.plugin;

import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifier;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.lang.gsp.resolve.taglib.GspTagLibUtil;
import org.apache.grails.intellij.plugin.util.GrailsArtifact;
import org.apache.grails.intellij.plugin.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.params.GrParameter;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrClassDefinition;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMember;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;

import java.util.Set;

public final class GrailsImplicitUsageProvider implements ImplicitUsageProvider {

  private static final Set<String> DOMAIN_METHODS = Set.of("beforeInsert", "beforeUpdate", "beforeDelete", "beforeValidate",
                                                                  "afterInsert", "afterUpdate", "afterDelete", "onLoad", "afterLoad");

  @Override
  public boolean isImplicitUsage(@NotNull PsiElement element) {
    if (element instanceof GrClassDefinition) {
      if (GrailsArtifact.getType((PsiClass)element) != null) {
        return true;
      }
    }
    else if (element instanceof GrParameter parameter) {
      // The parameters of a tag method are its contract with Grails rather than its own business: (Map attrs, Closure body)
      // is what makes it a tag, and the other parameters of a @Tag method name the attributes Grails selects it by.
      if (parameter.getDeclarationScope() instanceof GrMethod method && isTagMethod(method)) {
        return true;
      }
    }
    else if (element instanceof GrMember member) {

      if (member instanceof GrField field) {

        if (GrailsStaticFieldCompletionProvider.isGrailsField(field)) {
          return true;
        }

        if (GrailsUtils.isGrailsPluginClass(field.getContainingClass())) {
          return true;
        }
      }

      GrailsArtifact artifact = GrailsArtifact.getType(member.getContainingClass());
      if (artifact != null) {
        switch (artifact) {
          case CONTROLLER -> {
            if (GrailsUtils.isControllerAction(member)) {
              return true;
            }
          }
          case DOMAIN -> {
            if (member instanceof GrMethod) {
              if (DOMAIN_METHODS.contains(member.getName())) {
                return true;
              }
            }
          }
          case TAGLIB -> {
            // Grails dispatches a page's tags to the tag methods, which may well be called from no page of the project.
            if (member instanceof GrMethod method && isTagMethod(method)) {
              return true;
            }
          }
          case BOOTSTRAP -> {
            if (member instanceof GrField) {
              String filedName = member.getName();
              if ("init".equals(filedName) || "destroy".equals(filedName)) {
                return true;
              }
            }
          }
          case CODEC -> {
            if (member instanceof GrField && member.hasModifierProperty(PsiModifier.STATIC)) {
              String filedName = member.getName();
              if ("encode".equals(filedName) || "decode".equals(filedName)) {
                return true;
              }
            }
          }
        }
      }
    }

    return false;
  }

  /** Whether a method is a tag of a tag library, in a module whose Grails dispatches tags to methods. */
  private static boolean isTagMethod(@NotNull GrMethod method) {
    if (GrailsArtifact.getType(method.getContainingClass()) != GrailsArtifact.TAGLIB || !GspTagLibUtil.isTagMethod(method)) {
      return false;
    }
    Module module = ModuleUtilCore.findModuleForPsiElement(method);
    return module != null && GspTagLibUtil.supportsMethodTags(module);
  }

  @Override
  public boolean isImplicitRead(@NotNull PsiElement element) {
    return false;
  }

  @Override
  public boolean isImplicitWrite(@NotNull PsiElement element) {
    return false;
  }
}
