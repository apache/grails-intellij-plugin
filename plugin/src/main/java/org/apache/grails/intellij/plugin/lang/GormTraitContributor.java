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
package org.apache.grails.intellij.plugin.lang;

import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.util.CachedValueProvider.Result;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.gorm.GormClassNames;
import org.apache.grails.intellij.plugin.gorm.GormVersion;
import org.apache.grails.intellij.plugin.util.GrailsArtifact;
import org.apache.grails.intellij.plugin.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;
import org.jetbrains.plugins.groovy.lang.psi.impl.GroovyDirectInheritorsSearcher;
import org.jetbrains.plugins.groovy.lang.psi.util.GrTraitUtil;
import org.jetbrains.plugins.groovy.transformations.AstTransformationSupport;
import org.jetbrains.plugins.groovy.transformations.TransformationContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class GormTraitContributor implements AstTransformationSupport {

  private static final Map<String, String> MARKER_CLASSES = Map.of(
    "grails.mongodb.MongoEntity", "com.mongodb.MongoClient",
    "grails.neo4j.Neo4jEntity", "org.neo4j.driver.v1.Driver"
  );

  @Override
  public void applyTransformation(@NotNull TransformationContext context) {
    GrTypeDefinition clazz = context.getCodeClass();
    if (GrailsUtils.calculateArtifactType(clazz) != GrailsArtifact.DOMAIN) return;

    GormVersion gormVersion = GormVersion.forElement(clazz);
    if (gormVersion == null) return;
    if (gormVersion.compareTo(GormVersion.IS_5) < 0) return;
    if (gormVersion.compareTo(GormVersion.IS_6) >= 0 && context.isInheritor("grails.gorm.rx.RxEntity")) return;

    /*
    Repeat org.grails.compiler.gorm.GormEntityTransformation#pickGormEntityTrait().
     */
    String mapWith = computeMapWithValue(clazz);
    boolean hibernatePresent = JavaPsiFacade.getInstance(clazz.getProject())
                                 .findClass("org.hibernate.Hibernate", clazz.getResolveScope()) != null;
    String traitFqn;
    if (hibernatePresent && mapWith == null) {
      traitFqn = GormClassNames.ENTITY_TRAIT;
    }
    else {
      Collection<PsiClass> providers = findTraitsFromProviders(clazz);
      if (providers.isEmpty()) {
        traitFqn = GormClassNames.ENTITY_TRAIT;
      }
      else if (mapWith == null || mapWith.isEmpty()) {
        traitFqn = providers.size() == 1 ? qualifiedNameOrEntityTrait(providers.iterator().next())
                                         : GormClassNames.ENTITY_TRAIT;
      }
      else {
        PsiClass traitFromProvider = null;
        for (PsiClass provider : providers) {
          String name = provider.getName();
          if (name != null && name.startsWith(mapWith)) {
            traitFromProvider = provider;
            break;
          }
        }
        traitFqn = traitFromProvider == null ? GormClassNames.ENTITY_TRAIT : qualifiedNameOrEntityTrait(traitFromProvider);
      }
    }
    Grails3TraitInjectorContributor.injectTraits(clazz, context, List.of(traitFqn));
  }

  private static @NotNull String qualifiedNameOrEntityTrait(@NotNull PsiClass clazz) {
    String qualifiedName = clazz.getQualifiedName();
    return qualifiedName == null ? GormClassNames.ENTITY_TRAIT : qualifiedName;
  }

  private static @Nullable String computeMapWithValue(@NotNull GrTypeDefinition clazz) {
    return CachedValuesManager.getCachedValue(
      clazz,
      () -> Result.create(doComputeMapWithValue(clazz), PsiModificationTracker.MODIFICATION_COUNT));
  }

  private static @Nullable String doComputeMapWithValue(@NotNull GrTypeDefinition clazz) {
    PsiField mapWith = clazz.findCodeFieldByName("mapWith", true);
    if (mapWith instanceof GrField grField) {
      GrExpression initializer = grField.getInitializerGroovy();
      if (initializer instanceof GrLiteral literal && literal.getValue() instanceof String value) {
        if (value.isEmpty()) return value;
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
      }
    }
    return null;
  }

  private static @NotNull Collection<PsiClass> findTraitsFromProviders(@NotNull PsiElement context) {
    return CachedValuesManager.getCachedValue(
      context,
      () -> Result.create(doFindTraitsFromProviders(context), ProjectRootManager.getInstance(context.getProject())));
  }

  /**
   * Grails loads GormEntityTraitProvider using Java Services API and calls getEntityTrait().
   * To reduce processing time we assume that GormEntity's inheritors are registered in META-INF/services via GormEntityTraitProvider.
   * I.e. we do not run script with user classpath to determine traits, but just search for GormEntity inheritors.
   */
  private static @NotNull Collection<PsiClass> doFindTraitsFromProviders(@NotNull PsiElement context) {
    GlobalSearchScope scope = context.getResolveScope();
    JavaPsiFacade facade = JavaPsiFacade.getInstance(context.getProject());
    PsiClass gormEntity = facade.findClass(GormClassNames.ENTITY_TRAIT, scope);
    if (gormEntity == null) return Collections.emptyList();
    GormVersion version = GormVersion.forElement(context);
    if (version == null) return Collections.emptyList();

    List<PsiClass> traits = new ArrayList<>();
    gormEntity.putUserData(GroovyDirectInheritorsSearcher.IGNORE_INHERITANCE_CHECK, true);
    try {
      for (PsiClass candidate : ClassInheritorsSearch.search(gormEntity, scope, true).findAll()) {
        if (GrTraitUtil.isTrait(candidate)) traits.add(candidate);
      }
    }
    finally {
      gormEntity.putUserData(GroovyDirectInheritorsSearcher.IGNORE_INHERITANCE_CHECK, null);
    }

    // Dedupe by qualified name: the same trait can be seen through several scopes.
    Collection<PsiClass> result = new ObjectOpenCustomHashSet<>(new Hash.Strategy<>() {
      @Override
      public int hashCode(@Nullable PsiClass o) {
        String qualifiedName = o == null ? null : o.getQualifiedName();
        return qualifiedName == null ? 0 : qualifiedName.hashCode();
      }

      @Override
      public boolean equals(@Nullable PsiClass o1, @Nullable PsiClass o2) {
        if (o1 == o2) return true;
        String q1 = o1 == null ? null : o1.getQualifiedName();
        String q2 = o2 == null ? null : o2.getQualifiedName();
        return Objects.equals(q1, q2);
      }
    });

    for (PsiClass trait : traits) {
      // From GORM 6 a DB-specific trait only applies when its driver is on the classpath.
      if (version.compareTo(GormVersion.IS_6) >= 0) {
        String markerFqn = MARKER_CLASSES.get(trait.getQualifiedName());
        if (markerFqn != null && facade.findClass(markerFqn, scope) == null) continue;
      }
      result.add(trait);
    }

    return result;
  }
}
