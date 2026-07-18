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

package org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.gtag;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbAware;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlNSDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.references.urlMappings.UrlMappingUtil;

import java.util.Map;

public final class GspLinkNamespaceDescriptor implements XmlNSDescriptor, DumbAware {

  public static final String NAMESPACE_LINK = "link";

  public static final GspLinkNamespaceDescriptor INSTANCE = new GspLinkNamespaceDescriptor();

  private GspLinkNamespaceDescriptor() {

  }

  @Override
  public @Nullable XmlElementDescriptor getElementDescriptor(@NotNull XmlTag tag) {
    Module module = ModuleUtilCore.findModuleForPsiElement(tag);
    if (module == null) {
      // tag can be in dummy file.
      XmlTag parentTag = tag.getParentTag();
      if (parentTag == null) return null;
      module = ModuleUtilCore.findModuleForPsiElement(parentTag);
      if (module == null) return null;
    }

    UrlMappingUtil.NamedUrlMapping mapping = UrlMappingUtil.getNamedUrlMappings(module).get(tag.getLocalName());
    if (mapping == null) return null;

    return mapping.getElementDescriptor();
  }

  @Override
  public XmlElementDescriptor @NotNull [] getRootElementsDescriptors(@Nullable XmlDocument document) {
    if (document == null) return XmlElementDescriptor.EMPTY_ARRAY;
    Module module = ModuleUtilCore.findModuleForPsiElement(document);
    if (module == null) return XmlElementDescriptor.EMPTY_ARRAY;

    Map<String, UrlMappingUtil.NamedUrlMapping> mappings = UrlMappingUtil.getNamedUrlMappings(module);

    XmlElementDescriptor[] res = new XmlElementDescriptor[mappings.size()];

    int i = 0;
    for (UrlMappingUtil.NamedUrlMapping mapping : mappings.values()) {
      res[i++] = mapping.getElementDescriptor();
    }

    return res;
  }

  @Override
  public XmlFile getDescriptorFile() {
    return null;
  }

  @Override
  public @Nullable PsiElement getDeclaration() {
    return null;
  }

  @Override
  public String getName(PsiElement context) {
    return NAMESPACE_LINK;
  }

  @Override
  public String getName() {
    return NAMESPACE_LINK;
  }

  @Override
  public void init(PsiElement element) {
    throw new UnsupportedOperationException("Method init is not yet implemented in " + getClass().getName());
  }

  @Override
  public Object @NotNull [] getDependencies() {
    throw new UnsupportedOperationException("Method getDependencies is not yet implemented in " + getClass().getName());
  }

}
