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

package org.apache.grails.intellij.plugin.tests;

import com.intellij.openapi.util.Couple;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.ElementClassHint;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.util.GrailsArtifact;
import org.apache.grails.intellij.plugin.util.GrailsPsiUtil;
import org.apache.grails.intellij.plugin.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.GrModifierFlags;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.GrModifierList;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.annotation.GrAnnotation;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.annotation.GrAnnotationArrayInitializer;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightField;
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyCommonClassNames;
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

final class GrailsTestMemberContributor extends NonCodeMembersContributor {
  // #CHECH# "TestForTransformation.artefactTypeToTestMap" , Domain artifact added in "TestForTransformation.weaveMock"
  private static final Map<GrailsArtifact, Couple<String>> MAP = GrailsUtils.createMap(
    GrailsArtifact.CONTROLLER, Couple.of("controller", "grails.test.mixin.web.ControllerUnitTestMixin"),
    GrailsArtifact.TAGLIB, Couple.of("tagLib", "grails.test.mixin.web.GroovyPageUnitTestMixin"),
    GrailsArtifact.FILTER, Couple.of("filters", "grails.test.mixin.web.FiltersUnitTestMixin"),
    GrailsArtifact.URLMAPPINGS, Couple.of("urlMappings", "grails.test.mixin.web.UrlMappingsUnitTestMixin"),
    GrailsArtifact.SERVICE, Couple.of("service", "grails.test.mixin.services.ServiceUnitTestMixin"),

    GrailsArtifact.DOMAIN, Couple.of("domain", "grails.test.mixin.domain.DomainClassUnitTestMixin")
  );

  @Override
  public void processDynamicElements(@NotNull PsiType qualifierType,
                                     @Nullable PsiClass aClass,
                                     @NotNull PsiScopeProcessor processor,
                                     @NotNull PsiElement place,
                                     @NotNull ResolveState state) {
    if (!(aClass instanceof GrTypeDefinition testClass)) return;

    if (PsiTreeUtil.getParentOfType(place, GrAnnotation.class) != null) { // Prevent recursion.
      return;
    }

    GrModifierList modifierList = testClass.getModifierList();
    if (modifierList == null) return;

    PsiAnnotation testForAnnotation = modifierList.findAnnotation(GrailsTestUtils.TEST_FOR);
    PsiAnnotation testMixinAnnotation = modifierList.findAnnotation(GrailsTestUtils.TEST_MIXIN);
    PsiAnnotation testMockAnnotation = modifierList.findAnnotation(GrailsTestUtils.MOCK);

    if (testForAnnotation == null && testMixinAnnotation == null && testMockAnnotation == null) return;

    String nameHint = ResolveUtil.getNameHint(processor);
    ElementClassHint classHint = processor.getHint(ElementClassHint.KEY);

    if (ResolveUtil.shouldProcessProperties(classHint)) {
      if (!GrailsPsiUtil.processLogVariable(processor, testClass, nameHint)) return;
    }

    PsiClass processedMixinClass = null;

    if (testForAnnotation != null) {
      PsiAnnotationMemberValue value = testForAnnotation.findAttributeValue("value");
      if (value instanceof GrReferenceExpression) {
        PsiElement artifactClass = ((GrReferenceExpression)value).resolve();
        if (artifactClass instanceof PsiClass) {
          GrailsArtifact artifactType = GrailsArtifact.getType((PsiClass)artifactClass);
          Couple<String> testDataDescriptor = MAP.get(artifactType);
          if (testDataDescriptor != null) {
            if (ResolveUtil.shouldProcessProperties(classHint)) {
              String instancePropertyName = testDataDescriptor.first;
              if (nameHint == null || instancePropertyName.equals(nameHint)) {
                if (testClass.findFieldByName(instancePropertyName, false) == null) {
                  String qualifiedName = ((PsiClass)artifactClass).getQualifiedName();
                  if (qualifiedName != null) {
                    GrLightField field = new GrLightField(testClass, instancePropertyName, qualifiedName);
                    field.getModifierList().setModifiers(GrModifierFlags.PRIVATE_MASK);

                    if (!processor.execute(field, state)) return;
                  }
                }
              }
            }

            if (ResolveUtil.shouldProcessMethods(classHint)) {

              PsiClass mixinClass = JavaPsiFacade.getInstance(testClass.getProject()).findClass(testDataDescriptor.second,
                                                                                                            testClass.getResolveScope());
              if (mixinClass != null) {
                processedMixinClass = mixinClass;
                if (!processMethodsFromMixinClass(processor, testClass, mixinClass, nameHint, state)) return;
              }
            }
          }
        }
      }
    }

    if (ResolveUtil.shouldProcessMethods(classHint)) {
      if (!processAssertMethods(processor, testClass, state)) return;

      if (testMixinAnnotation != null) {
        List<PsiAnnotationMemberValue> values;

        PsiAnnotationMemberValue value = testMixinAnnotation.findAttributeValue("value");
        if (value instanceof GrReferenceExpression) {
          values = Collections.singletonList(value);
        }
        else if (value instanceof GrAnnotationArrayInitializer) {
          values = Arrays.asList(((GrAnnotationArrayInitializer)value).getInitializers());
        }
        else {
          values = Collections.emptyList();
        }

        for (PsiAnnotationMemberValue v : values) {
          if (v instanceof GrReferenceExpression) {
            PsiElement mixinClass = ((GrReferenceExpression)v).resolve();
            if (mixinClass instanceof PsiClass) {
              if (mixinClass != processedMixinClass) {
                if (!processMethodsFromMixinClass(processor, testClass, (PsiClass)mixinClass, nameHint, state)) return;
              }
            }
          }
        }
      }
    }
  }

  private static boolean processMethodsFromMixinClass(PsiScopeProcessor processor,
                                                      GrTypeDefinition testClass,
                                                      PsiClass mixinClass,
                                                      @Nullable String nameHint,
                                                      ResolveState state) {
    PsiMethod[] methods = nameHint == null ? mixinClass.getAllMethods() : mixinClass.findMethodsByName(nameHint, true);

    if (methods.length == 0) return true;

    PsiClass groovyObjectSupport = JavaPsiFacade.getInstance(testClass.getProject())
      .findClass(GroovyCommonClassNames.GROOVY_OBJECT_SUPPORT, testClass.getResolveScope());

    for (PsiMethod method : methods) {
      if (!isCandidateMethod(method, groovyObjectSupport)) continue;

      if (testClass.findCodeMethodsBySignature(method, true).length == 0) {
        if (!processor.execute(method, state)) return false;
      }
    }

    return true;
  }

  private static boolean processAssertMethods(PsiScopeProcessor processor, @NotNull PsiClass aClass, ResolveState state) {
    return GrailsPsiUtil.process("org.junit.Assert", processor, aClass, state);
  }

  /**
   * See TestMixinTransformation.isCandidateMethod()
   */
  private static boolean isCandidateMethod(@NotNull PsiMethod method, @Nullable PsiClass groovyObjectSupport) {
    String name = method.getName();
    if (name.indexOf('$') != -1) return false;
    if (method.hasModifierProperty(PsiModifier.PRIVATE)
        || method.hasModifierProperty(PsiModifier.PROTECTED)
        || method.hasModifierProperty(PsiModifier.ABSTRACT)) {
      return false;
    }

    if (groovyObjectSupport != null && groovyObjectSupport.findMethodBySignature(method, true) != null) {
      return false;
    }

    return true;
  }
}
