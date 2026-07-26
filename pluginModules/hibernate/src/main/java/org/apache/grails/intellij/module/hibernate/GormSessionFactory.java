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

package org.apache.grails.intellij.module.hibernate;

import com.intellij.hibernate.model.HibernateConstants;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.persistence.model.PersistenceListener;
import com.intellij.persistence.model.PersistenceMappings;
import com.intellij.persistence.model.PersistencePackage;
import com.intellij.persistence.model.helpers.PersistenceUnitModelHelper;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.xml.GenericValue;
import com.intellij.util.xml.ReadOnlyGenericValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.references.domain.persistent.GormPersistenceMapping;
import org.apache.grails.intellij.plugin.mvc.MvcModuleStructureSynchronizer;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class GormSessionFactory extends UserDataHolderBase implements PersistencePackage, PersistenceUnitModelHelper {

  private static final GenericValue<String> NAME = ReadOnlyGenericValue.getInstance("Gorm");

  private final Module myModule;

  private final GormPersistenceMapping myPersistenceMapping;

  public GormSessionFactory(Module module) {
    myModule = module;
    myPersistenceMapping = new GormPersistenceMapping(module);
  }

  @Override
  public GenericValue<String> getName() {
    return NAME;
  }

  @Override
  public PersistenceUnitModelHelper getModelHelper() {
    return this;
  }

  @Override
  public boolean isValid() {
    return !myModule.isDisposed();
  }

  @Override
  public @Nullable XmlTag getXmlTag() {
    return null;
  }

  @Override
  public PsiManager getPsiManager() {
    return PsiManager.getInstance(myModule.getProject());
  }

  @Override
  public Module getModule() {
    return myModule;
  }

  @Override
  public PsiElement getIdentifyingPsiElement() {
    return null;
  }

  @Override
  public PsiFile getContainingFile() {
    return null;
  }

  @Override
  public GenericValue<Boolean> getExcludeUnlistedClasses() {
    return ReadOnlyGenericValue.getInstance(Boolean.TRUE);
  }

  @Override
  public @Nullable String getPersistenceProviderName() {
    return HibernateConstants.PERSISTENCE_PROVIDER_CLASS;
  }

  @Override
  public @Nullable PersistenceMappings getAdditionalMapping() {
    return myPersistenceMapping;
  }

  @Override
  public @NotNull <V extends PersistenceMappings> List<? extends GenericValue<V>> getMappingFiles(Class<V> mappingsClass) {
    return Collections.emptyList();
  }

  @Override
  public @NotNull List<? extends PersistenceListener> getPersistentListeners() {
    return Collections.emptyList();
  }

  @Override
  public @NotNull List<? extends GenericValue<PsiFile>> getJarFiles() {
    return Collections.emptyList();
  }

  @Override
  public @NotNull List<? extends GenericValue<PsiClass>> getClasses() {
    return Collections.emptyList();
    //Collection<? extends GrClassDefinition> domains = GrailsArtifact.DOMAIN.getInstances(myModule).values();
    //
    //List<GenericValue<PsiClass>> res = new ArrayList<GenericValue<PsiClass>>(domains.size());
    //
    //for (GrClassDefinition definition : domains) {
    //  res.add(ReadOnlyGenericValue.<PsiClass>getInstance(definition));
    //}
    //
    //return res;
  }

  @Override
  public @NotNull List<? extends GenericValue<PsiPackage>> getPackages() {
    return Collections.emptyList();

    //Set<String> packageNames = new HashSet<String>();
    //for (GrClassDefinition definition : GrailsArtifact.DOMAIN.getInstances(myModule).values()) {
    //  String qualifiedName = definition.getQualifiedName();
    //  if (qualifiedName != null) {
    //    packageNames.add(StringUtil.getPackageName(qualifiedName));
    //  }
    //}
    //
    //List<GenericValue<PsiPackage>> res = new ArrayList<GenericValue<PsiPackage>>(packageNames.size());
    //
    //JavaPsiFacade facade = JavaPsiFacade.getInstance(myModule.getProject());
    //
    //for (String packageName : packageNames) {
    //  PsiPackage aPackage = facade.findPackage(packageName);
    //  if (aPackage != null) {
    //    res.add(ReadOnlyGenericValue.getInstance(aPackage));
    //  }
    //}
    //
    //return res;
  }

  @Override
  public GenericValue<String> getDataSourceName() {
    return null;
  }

  @Override
  public @NotNull Properties getPersistenceUnitProperties() {
    return new Properties();
  }

  @Override
  public Collection<Object> getCacheDependencies() {
    MvcModuleStructureSynchronizer synchronizer = MvcModuleStructureSynchronizer.getInstance(myModule.getProject());
    return Collections.singletonList(synchronizer.getFileAndRootsModificationTracker());
  }
}
