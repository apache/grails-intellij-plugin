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
package org.apache.grails.intellij.plugin.plugins;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AllClassesSearch;
import com.intellij.psi.util.CachedValueProvider.Result;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.structure.GrailsApplicationManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/** Discovery of the Grails plugins an application depends on, both source and compiled. */
public final class GrailsPlugins {

  private GrailsPlugins() {
  }

  static final String PLUGIN_CLASS_SUFFIX = "GrailsPlugin";

  public static @NotNull Collection<GrailsPluginDescriptor> computePlugins(@NotNull GrailsApplication application) {
    return ReadAction.compute(() -> CachedValuesManager.getManager(application.getProject()).getCachedValue(
      application,
      () -> {
        List<GrailsPluginDescriptor> all = new ArrayList<>(getSourcePlugins(application));
        all.addAll(doComputeCompiledPlugins(application));
        return Result.create(all, ProjectRootManager.getInstance(application.getProject()));
      }));
  }

  public static @NotNull Collection<Grails3SourcePluginDescriptor> getSourcePlugins(@NotNull GrailsApplication application) {
    return ReadAction.compute(() -> CachedValuesManager.getManager(application.getProject()).getCachedValue(
      application,
      () -> Result.create(doComputeSourcePlugins(application),
                          ProjectRootManager.getInstance(application.getProject()))));
  }

  private static @NotNull Collection<Grails3SourcePluginDescriptor> doComputeSourcePlugins(@NotNull GrailsApplication application) {
    List<Grails3SourcePluginDescriptor> result = new ArrayList<>();
    Collection<PsiClass> candidates = AllClassesSearch
      .search(application.getScope(true, false), application.getProject(), name -> name.endsWith(PLUGIN_CLASS_SUFFIX))
      .findAll();
    for (PsiClass candidate : candidates) {
      GrailsApplication owner = GrailsApplicationManager.findApplication(candidate);
      // Skip a plugin class belonging to this application itself; only siblings count as plugins.
      if (owner == null || owner.equals(application)) continue;
      result.add(new Grails3SourcePluginDescriptor(candidate, owner));
    }
    return result;
  }

  private static @NotNull Collection<Grails3CompiledPluginDescriptor> doComputeCompiledPlugins(@NotNull GrailsApplication application) {
    GlobalSearchScope scope = application.getScope(true, false);
    JavaPsiFacade facade = JavaPsiFacade.getInstance(application.getProject());
    PsiPackage metaInf = facade.findPackage("META-INF");
    if (metaInf == null) return Collections.emptyList();

    List<Grails3CompiledPluginDescriptor> result = new ArrayList<>();
    for (PsiDirectory directory : metaInf.getDirectories(scope)) {
      PsiFile file = directory.findFile("grails-plugin.xml");
      if (!(file instanceof XmlFile pluginXml)) continue;
      String pluginClassFqn = findPluginClassFqn(pluginXml);
      if (pluginClassFqn == null) continue;
      PsiClass pluginClass = facade.findClass(pluginClassFqn, scope);
      if (pluginClass == null) continue;
      result.add(new Grails3CompiledPluginDescriptor(pluginClass, () -> {
        XmlTag rootTag = pluginXml.getRootTag();
        return rootTag == null ? null : rootTag.getAttributeValue("version");
      }));
    }
    return result;
  }

  private static @Nullable String findPluginClassFqn(@NotNull XmlFile pluginXml) {
    XmlTag rootTag = pluginXml.getRootTag();
    if (rootTag == null) return null;
    XmlTag[] typeTags = rootTag.findSubTags("type");
    if (typeTags.length == 0) return null;
    return typeTags[0].getValue().getTrimmedText();
  }
}
