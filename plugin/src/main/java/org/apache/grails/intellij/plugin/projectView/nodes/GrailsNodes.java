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

package org.apache.grails.intellij.plugin.projectView.nodes;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.ClassTreeNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.artefact.api.GrailsDisplayableArtefactHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class GrailsNodes {

  private GrailsNodes() {
  }

  static @NotNull String fqnString(@NotNull List<String> parts) {
    return String.join(".", parts);
  }

  static @NotNull Collection<AbstractTreeNode<?>> getArtefactNodes(@NotNull Project project,
                                                                   @NotNull ViewSettings settings,
                                                                   @NotNull GrailsDisplayableArtefactHandler artefactHandler,
                                                                   @NotNull Collection<PsiClass> artefacts) {
    if (settings.isFlattenPackages()) {
      Collection<String> packages = getPackagesFlattened(artefacts, settings.isHideEmptyMiddlePackages());
      List<AbstractTreeNode<?>> result = new ArrayList<>();
      for (String packageFqn : packages) {
        result.add(new GrailsFlatPackageNode(project, settings, packageFqn));
      }
      result.addAll(getClassNodes(artefacts, project, settings, artefactHandler, ""));
      return result;
    }
    return getNodesRegular(artefacts, project, settings, artefactHandler, List.of());
  }

  private static @NotNull Collection<String> getPackagesFlattened(@NotNull Collection<PsiClass> classes,
                                                                  boolean hideEmptyMiddlePackages) {
    Collection<String> classPackageFqns = new LinkedHashSet<>();
    for (PsiClass clazz : classes) {
      String packageName = getPackageName(clazz);
      if (packageName != null) classPackageFqns.add(packageName);
    }

    Set<String> packageFqns = new LinkedHashSet<>(classPackageFqns);
    if (!hideEmptyMiddlePackages) {
      for (String fqn : classPackageFqns) {
        String current = fqn;
        while (!current.isEmpty()) {
          current = StringUtil.getPackageName(current);
          packageFqns.add(current);
        }
      }
    }
    packageFqns.remove("");
    return packageFqns;
  }

  static @NotNull Collection<AbstractTreeNode<?>> getClassNodes(@NotNull Collection<PsiClass> classes,
                                                                @NotNull Project project,
                                                                @NotNull ViewSettings settings,
                                                                @NotNull GrailsDisplayableArtefactHandler artefactHandler,
                                                                @NotNull List<String> packageFqn) {
    return getClassNodes(classes, project, settings, artefactHandler, fqnString(packageFqn));
  }

  static @NotNull Collection<AbstractTreeNode<?>> getClassNodes(@NotNull Collection<PsiClass> classes,
                                                                @NotNull Project project,
                                                                @NotNull ViewSettings settings,
                                                                @NotNull GrailsDisplayableArtefactHandler artefactHandler,
                                                                @NotNull String packageFqn) {
    List<AbstractTreeNode<?>> result = new ArrayList<>();
    for (PsiClass clazz : classes) {
      if (!packageFqn.equals(getPackageName(clazz))) continue;
      AbstractTreeNode<?> node = artefactHandler.createNode(clazz, settings);
      result.add(node != null ? node : new ClassTreeNode(project, clazz, settings));
    }
    return result;
  }

  static @NotNull Collection<AbstractTreeNode<?>> getNodesRegular(@NotNull Collection<PsiClass> classes,
                                                                  @NotNull Project project,
                                                                  @NotNull ViewSettings settings,
                                                                  @NotNull GrailsDisplayableArtefactHandler artefactHandler,
                                                                  @NotNull List<String> packageFqn) {
    Collection<List<String>> packageFqns = getPackagesRegular(classes, packageFqn, settings.isHideEmptyMiddlePackages());
    List<AbstractTreeNode<?>> result = new ArrayList<>();
    for (List<String> relativeParts : packageFqns) {
      result.add(new GrailsRegularPackageNode(project, settings, new CompactedFqn(packageFqn, relativeParts)));
    }
    result.addAll(getClassNodes(classes, project, settings, artefactHandler, packageFqn));
    return result;
  }

  static @NotNull Collection<List<String>> getPackagesRegular(@NotNull Collection<PsiClass> classes,
                                                              @NotNull List<String> basePackage,
                                                              boolean hideEmptyMiddlePackages) {
    StringBuilder prefixBuilder = new StringBuilder();
    for (String part : basePackage) {
      prefixBuilder.append(part).append('.');
    }
    String packagePrefix = prefixBuilder.toString();

    Collection<String> fullPackageNames = new LinkedHashSet<>();
    for (PsiClass clazz : classes) {
      String packageName = getPackageName(clazz);
      if (packageName != null && packageName.startsWith(packagePrefix)) {
        fullPackageNames.add(packageName);
      }
    }

    // Faithful to the Kotlin: substringAfter(prefix, "") strips everything up to and including
    // the first occurrence of the prefix, and split(".") is a literal split. The original's
    // trailing `filter { !it.isEmpty() }` dropped empty *lists*, which split never produces --
    // so a class in the default package (relative == "") contributed [""] and was kept. That
    // surfaces as a package node with an empty name; preserved here rather than quietly changed,
    // because it is a pre-existing behaviour and not part of this conversion.
    List<List<String>> relativePackageParts = new ArrayList<>();
    for (String fullPackageName : fullPackageNames) {
      int idx = fullPackageName.indexOf(packagePrefix);
      String relative = idx < 0 ? "" : fullPackageName.substring(idx + packagePrefix.length());
      relativePackageParts.add(List.of(relative.split("\\.", -1)));
    }

    if (hideEmptyMiddlePackages) {
      List<List<String>> nonEmptyPackageParts = new ArrayList<>();
      List<List<String>> emptyPackageParts = new ArrayList<>();
      for (List<String> parts : relativePackageParts) {
        (parts.size() == 1 ? nonEmptyPackageParts : emptyPackageParts).add(parts);
      }

      Set<String> nonEmptyPackageNames = new HashSet<>();
      for (List<String> parts : nonEmptyPackageParts) {
        nonEmptyPackageNames.add(parts.get(0));
      }

      List<List<String>> result = new ArrayList<>(nonEmptyPackageParts);
      for (List<String> parts : emptyPackageParts) {
        if (!nonEmptyPackageNames.contains(parts.get(0))) {
          result.add(parts);
        }
      }
      return result;
    }

    // Only the first segment matters here, de-duplicated, each wrapped as a single-element list.
    Set<String> firstParts = new HashSet<>();
    for (List<String> parts : relativePackageParts) {
      firstParts.add(parts.get(0));
    }
    List<List<String>> result = new ArrayList<>(firstParts.size());
    for (String part : firstParts) {
      result.add(List.of(part));
    }
    return result;
  }

  private static @Nullable String getPackageName(@NotNull PsiClass clazz) {
    String qualifiedName = clazz.getQualifiedName();
    return qualifiedName != null ? StringUtil.getPackageName(qualifiedName) : null;
  }
}
