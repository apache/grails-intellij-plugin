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

package org.jetbrains.plugins.grails.references.domain.persistent;

import com.intellij.jam.model.common.CommonModelElement;
import com.intellij.openapi.module.Module;
import com.intellij.persistence.model.PersistenceListener;
import com.intellij.persistence.model.PersistenceMappings;
import com.intellij.persistence.model.PersistenceQuery;
import com.intellij.persistence.model.PersistentEmbeddable;
import com.intellij.persistence.model.PersistentEntity;
import com.intellij.persistence.model.PersistentSuperclass;
import com.intellij.persistence.model.helpers.PersistenceMappingsModelHelper;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.util.PropertyMemberType;
import com.intellij.util.xml.GenericValue;
import com.intellij.util.xml.ReadOnlyGenericValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrClassDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GormPersistenceMapping extends CommonModelElement.ModuleBase implements PersistenceMappings, PersistenceMappingsModelHelper {

  private final Module myModule;

  public GormPersistenceMapping(Module module) {
    myModule = module;
  }

  @Override
  public @NotNull Module getModule() {
    return myModule;
  }

  @Override
  public PersistenceMappingsModelHelper getModelHelper() {
    return this;
  }

  @Override
  public GenericValue<PsiPackage> getPackage() {
    return ReadOnlyGenericValue.nullInstance();
  }

  @Override
  public PropertyMemberType getDeclaredAccess() {
    return null;
  }

  @Override
  public List<? extends PersistenceListener> getPersistentListeners() {
    return Collections.emptyList();
  }

  @Override
  public @NotNull List<? extends PersistentEntity> getPersistentEntities() {
    List<PersistentEntity> res = new ArrayList<>();

    for (GrClassDefinition domainClass : GrailsArtifact.DOMAIN.getInstances(myModule).values()) {
      PersistentEntity e = new GormEntity(myModule, domainClass);
      res.add(e);
    }

    return res;
  }

  @Override
  public @NotNull List<? extends PersistentSuperclass> getPersistentSuperclasses() {
    return Collections.emptyList();
  }

  @Override
  public @NotNull List<? extends PersistentEmbeddable> getPersistentEmbeddables() {
    return Collections.emptyList();
  }

  @Override
  public List<? extends PersistenceQuery> getNamedQueries() {
    return Collections.emptyList();
  }

  @Override
  public List<? extends PersistenceQuery> getNamedNativeQueries() {
    return Collections.emptyList();
  }
}
