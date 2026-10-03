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

package org.apache.grails.intellij.plugin.references.domain.detachedCriteria;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.ElementClassHint;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.grails.intellij.plugin.references.domain.DomainDescriptor;
import org.apache.grails.intellij.plugin.util.GrailsArtifact;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyPropertyUtils;
import org.jetbrains.plugins.groovy.lang.resolve.ClosureMemberContributor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

import java.util.Set;

/**
 * Resolves the bare property names of a GORM where query ({@code Person.where { active == true && age >= 18 }})
 * to the persistent properties of the queried domain class.
 * <p>
 * The GORM 5+ {@code GormEntity} trait declares {@code where(@DelegatesTo(DetachedCriteria) Closure)}: the delegate
 * is a raw {@code DetachedCriteria}, which has no such properties, because GORM rewrites the closure at compile time
 * (DetachedCriteriaTransformer) into criteria calls. Without this contributor the properties stay unresolved, which
 * {@code @CompileStatic}/{@code @GrailsCompileStatic} code reports as an error although it compiles.
 */
final class WhereQueryClosureMemberContributor extends ClosureMemberContributor {

  // See DetachedCriteriaTransformer: the methods whose closure argument is transformed into a where query.
  private static final Set<String> WHERE_METHODS = Set.of("where", "whereAny", "whereLazy", "find", "findAll");

  private static final Set<String> IMPLICIT_PROPERTIES = Set.of("id", "version");

  @Override
  protected void processMembers(@NotNull GrClosableBlock closure,
                                @NotNull PsiScopeProcessor processor,
                                @NotNull PsiElement place,
                                @NotNull ResolveState state) {
    String nameHint = ResolveUtil.getNameHint(processor);
    if (nameHint == null) return;

    if (!(place instanceof GrReferenceExpression refExpr) || refExpr.isQualified()) return;
    if (closure != PsiTreeUtil.getParentOfType(place, GrClosableBlock.class)) return;

    ElementClassHint classHint = processor.getHint(ElementClassHint.KEY);
    boolean processProperties = ResolveUtil.shouldProcessProperties(classHint);
    boolean processMethods = ResolveUtil.shouldProcessMethods(classHint);
    if (!processProperties && !processMethods) return;

    PsiClass domainClass = getQueriedDomainClass(closure);
    if (domainClass == null) return;

    // A Groovy property is a private field plus accessors, and from outside its class it is read through the
    // getter: resolving to the field itself is an access violation under @CompileStatic. So the property is
    // contributed as its getter, which the reference resolves to via the accessor processor and which
    // navigates to (and renames with) the field. Only fields that can be read directly - e.g. the light
    // fields injected for hasMany, id and version - are handed to the property processor.
    if (processMethods) {
      PsiMethod getter = findGetter(domainClass, nameHint);
      if (getter != null && !processor.execute(getter, state)) return;
    }

    if (processProperties) {
      PsiField field = findReadableField(domainClass, nameHint);
      if (field != null) processor.execute(field, state);
    }
  }

  private static @Nullable PsiClass getQueriedDomainClass(@NotNull GrClosableBlock closure) {
    PsiElement parent = closure.getParent();
    if (parent instanceof GrArgumentList) parent = parent.getParent();
    if (!(parent instanceof GrMethodCall call)) return null;

    if (!(call.getInvokedExpression() instanceof GrReferenceExpression invoked)) return null;
    if (!WHERE_METHODS.contains(invoked.getReferenceName())) return null;

    GrExpression qualifier = invoked.getQualifierExpression();
    if (qualifier == null) {
      // An unqualified call from within the domain class itself.
      PsiClass containingClass = PsiTreeUtil.getParentOfType(call, PsiClass.class);
      return GrailsArtifact.DOMAIN.isInstance(containingClass) ? containingClass : null;
    }

    if (qualifier instanceof GrReferenceExpression qualifierRef && qualifierRef.resolve() instanceof PsiClass qualifierClass) {
      return GrailsArtifact.DOMAIN.isInstance(qualifierClass) ? qualifierClass : null;
    }

    // Composing an existing query: criteria.where { ... }
    return DetachedCriteriaUtil.getDomainClassByDetachedCriteriaExpression(qualifier.getType());
  }

  private static @Nullable PsiMethod findGetter(@NotNull PsiClass domainClass, @NotNull String getterName) {
    String propertyName = GroovyPropertyUtils.getPropertyNameByGetterName(getterName, true);
    if (propertyName == null || !isQueryableProperty(domainClass, propertyName)) return null;

    for (PsiMethod method : domainClass.findMethodsByName(getterName, true)) {
      if (GroovyPropertyUtils.isSimplePropertyGetter(method, propertyName) && !method.hasModifierProperty(PsiModifier.STATIC)) {
        return method;
      }
    }
    return null;
  }

  private static @Nullable PsiField findReadableField(@NotNull PsiClass domainClass, @NotNull String name) {
    if (!isQueryableProperty(domainClass, name)) return null;

    PsiField field = domainClass.findFieldByName(name, true);
    if (field == null || field.hasModifierProperty(PsiModifier.STATIC) || field.hasModifierProperty(PsiModifier.PRIVATE)) return null;
    return field;
  }

  private static boolean isQueryableProperty(@NotNull PsiClass domainClass, @NotNull String name) {
    return IMPLICIT_PROPERTIES.contains(name) || DomainDescriptor.getPersistentProperties(domainClass).containsKey(name);
  }
}
