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

package org.apache.grails.intellij.plugin.references.domain;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.references.domain.namedQuery.NamedQueryDescriptor;
import org.apache.grails.intellij.plugin.util.GrailsArtifact;
import org.apache.grails.intellij.plugin.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightVariable;

import static org.apache.grails.intellij.plugin.gorm.GormClassNames.ENTITY_ANNO;
import static org.apache.grails.intellij.plugin.gorm.GormClassNames.OLD_ENTITY_ANNO;

public final class GormUtils {

  public static final String NAMED_CRITERIA_PROXY_CLASS_NAME = "org.codehaus.groovy.grails.orm.hibernate.cfg.NamedCriteriaProxy";

  private GormUtils() {
  }

  public static boolean isGormBean(@Nullable PsiClass aClass) {
    if (GrailsArtifact.DOMAIN.isInstance(aClass)) return true;
    return isStandaloneGormBean(aClass);
  }

  public static boolean isStandaloneGormBean(@Nullable PsiClass aClass) {
    return aClass != null && (
      AnnotationUtil.isAnnotated(aClass, OLD_ENTITY_ANNO, 0) ||
      AnnotationUtil.isAnnotated(aClass, ENTITY_ANNO, 0)
    );
  }

  public static boolean isNamedQueryDeclaration(PsiElement element) {
    if (!(element instanceof GrReferenceExpression) || ((GrReferenceExpression)element).isQualified()) return false;

    PsiElement eCall = element.getParent();
    if (!(eCall instanceof GrMethodCall call)) return false;

    PsiElement eClosure = call.getParent();
    if (!(eClosure instanceof GrClosableBlock)) return false;

    PsiElement eField = eClosure.getParent();
    if (!(eField instanceof GrField field)) return false;

    if (!"namedQueries".equals(field.getName()) || !field.hasModifierProperty(PsiModifier.STATIC)) return false;

    if (GrailsUtils.getClosureArgument(call) == null) return false;

    PsiClass domainClass = field.getContainingClass();

    return GrailsArtifact.DOMAIN.isInstance(domainClass);
  }

  /**
   * Example:
   * For {@code Ddd.someNamedQuery.count()} returns descriptor of named query Ddd.someNamedQuery
   */
  public static @Nullable NamedQueryDescriptor getQueryDescriptorByProxyMethod(@NotNull GrMethodCall callExpression) {
    GrExpression eInvokedExpression = callExpression.getInvokedExpression();
    if (!(eInvokedExpression instanceof GrReferenceExpression)) return null;

    return getQueryDescriptorByProxyMethod((GrReferenceExpression)eInvokedExpression);
  }

  public static @Nullable NamedQueryDescriptor getQueryDescriptorByProxyMethod(@NotNull GrReferenceExpression invokedExpression) {
    GrExpression eQualifierExpression = invokedExpression.getQualifierExpression();

    if (eQualifierExpression instanceof GrMethodCall) {
      eQualifierExpression = ((GrMethodCall)eQualifierExpression).getInvokedExpression();
    }

    if (!(eQualifierExpression instanceof GrReferenceExpression)) return null;

    PsiElement resolve = ((GrReferenceExpression)eQualifierExpression).resolve();

    Object key = null;

    if (resolve instanceof GrLightVariable) {
      key = ((GrLightVariable)resolve).getCreatorKey();
    }
    else if (GrLightMethodBuilder.checkKind(resolve, NamedQueryDescriptor.NAMED_QUERY_METHOD_MARKER)) {
      //noinspection ConstantConditions
      key = ((GrLightMethodBuilder)resolve).getData();
    }

    if (!(key instanceof NamedQueryDescriptor)) {
      return null;
    }

    return (NamedQueryDescriptor)key;
  }
}
