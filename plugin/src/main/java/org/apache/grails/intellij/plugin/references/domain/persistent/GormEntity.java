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

package org.apache.grails.intellij.plugin.references.domain.persistent;

import com.intellij.jam.model.common.CommonModelElement;
import com.intellij.jpa.model.common.persistence.mapping.AttributeBase;
import com.intellij.jpa.model.common.persistence.mapping.PersistentObject;
import com.intellij.jpa.model.xml.persistence.mapping.AccessType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Pair;
import com.intellij.persistence.model.PersistenceInheritanceType;
import com.intellij.persistence.model.PersistenceQuery;
import com.intellij.persistence.model.PersistentAttribute;
import com.intellij.persistence.model.PersistentEntity;
import com.intellij.persistence.model.TableInfoProvider;
import com.intellij.persistence.model.helpers.PersistentEntityModelHelper;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PropertyMemberType;
import com.intellij.util.xml.GenericValue;
import com.intellij.util.xml.ReadOnlyGenericValue;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.references.domain.DomainDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GormEntity extends CommonModelElement.PsiBase implements PersistentEntity, PersistentEntityModelHelper, PersistentObject, TableInfoProvider {

  private final Module myModule;
  private final PsiClass myDomainClass;

  private volatile List<AttributeBase> myAttributes;
  
  public GormEntity(Module module, PsiClass domainClass) {
    myModule = module;
    myDomainClass = domainClass;
  }

  @Override
  public GenericValue<String> getName() {
    return ReadOnlyGenericValue.getInstance(myDomainClass.getName());
  }

  @Override
  public GenericValue<PsiClass> getClazz() {
    return ReadOnlyGenericValue.getInstance(myDomainClass);
  }

  @Override
  public List<? extends AttributeBase> getAllAttributes() {
    List<AttributeBase> res = myAttributes;
    if (res == null) {
      res = new ArrayList<>();

      DomainDescriptor descriptor = DomainDescriptor.getDescriptor(myDomainClass);
      
      for (Map.Entry<String, Pair<PsiType, PsiElement>> entry : descriptor.getPersistentProperties().entrySet()) {
        if (descriptor.isToManyRelation(entry.getKey())) {
          res.add(new GormCollectionAttribute(this, entry.getKey(), entry.getValue().first, entry.getValue().second));
        }
        else {
          GormPersistentAttribute attr;

          if ("id".equals(entry.getKey())) {
            attr = new GormIdAttribute(this, entry.getKey(), entry.getValue().first, entry.getValue().second);
          }
          else if (descriptor.getEmbeddedPropertyNames().contains(entry.getKey())) {
            attr = new GormEmbeddedAttribute(this, entry.getKey(), entry.getValue().first, entry.getValue().second);
          }
          else {
            attr = new GormBasicAttribute(this, entry.getKey(), entry.getValue().first, entry.getValue().second);
          }

          res.add(attr);
        }
      }

      myAttributes = res;
    }

    return res;
  }

  @Override
  public AccessType getEffectiveAccessType() {
    return null;
  }

  @Override
  public @NotNull PersistentEntityModelHelper getObjectModelHelper() {
    return this;
  }

  @Override
  public GenericValue<PsiClass> getIdClassValue() {
    return ReadOnlyGenericValue.nullInstance();
  }

  @Override
  public @NotNull PsiElement getPsiElement() {
    return myDomainClass;
  }

  @Override
  public Module getModule() {
    return myModule;
  }

  @Override
  public TableInfoProvider getTable() {
    return this;
  }

  @Override
  public GenericValue<String> getTableName() {
    return ReadOnlyGenericValue.nullInstance();
  }

  @Override
  public GenericValue<String> getCatalog() {
    return ReadOnlyGenericValue.nullInstance();
  }

  @Override
  public GenericValue<String> getSchema() {
    return ReadOnlyGenericValue.nullInstance();
  }

  @Override
  public List<? extends TableInfoProvider> getSecondaryTables() {
    return Collections.emptyList();
  }

  @Override
  public PersistenceInheritanceType getInheritanceType(PersistentEntity descendant) {
    return null;
  }

  @Override
  public List<? extends PersistenceQuery> getNamedQueries() {
    return Collections.emptyList();
  }

  @Override
  public List<? extends PersistenceQuery> getNamedNativeQueries() {
    return Collections.emptyList();
  }

  @Override
  public @NotNull List<? extends PersistentAttribute> getAttributes() {
    return getAllAttributes();
  }

  @Override
  public PropertyMemberType getDefaultAccessMode() {
    return null;
  }

  @Override
  public boolean isAccessModeFixed() {
    return false;
  }

}
