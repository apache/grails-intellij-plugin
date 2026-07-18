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

package com.intellij.groovy.grails.hibernate;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Key;
import com.intellij.persistence.extensions.PersistencePackagesProvider;
import com.intellij.persistence.facet.PersistenceFacet;
import com.intellij.persistence.model.PersistencePackage;
import com.intellij.util.ConcurrencyUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.config.GrailsStructure;

import java.util.Collections;
import java.util.List;

final class GormSessionFactoryContributor implements PersistencePackagesProvider {
  private static final Key<List<PersistencePackage>> SESSION_FACTORY_KEY = Key.create("GormSessionFactoryContributor.SESSION_FACTORY_KEY");

  @Override
  public @NotNull List<PersistencePackage> getPersistencePackages(PersistenceFacet facet) {
    Module module = facet.getModule();
    GrailsStructure instance = GrailsStructure.getInstance(module);
    if (instance == null) return Collections.emptyList();

    return ConcurrencyUtil.computeIfAbsent(module, SESSION_FACTORY_KEY, () -> Collections.singletonList(new GormSessionFactory(module)));
  }
}
