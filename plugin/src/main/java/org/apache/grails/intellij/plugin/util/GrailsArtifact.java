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

package org.apache.grails.intellij.plugin.util;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTagValue;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.GroovyMvcIcons;
import org.apache.grails.intellij.plugin.config.GrailsFramework;
import org.apache.grails.intellij.plugin.config.GrailsStructure;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.structure.GrailsApplicationManager;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrClassDefinition;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;
import org.apache.grails.intellij.plugin.mvc.MvcModuleStructureSynchronizer;
import org.jetbrains.plugins.groovy.util.GroovyUtils;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public enum GrailsArtifact {
  DOMAIN(NotNullLazyValue.createValue(() -> AllIcons.Nodes.DataTables), "", "domain"),
  CONTROLLER(NotNullLazyValue.createValue(() -> AllIcons.Nodes.Controller), "Controller", "controllers"),
  TAGLIB(NotNullLazyValue.createValue(() -> GroovyMvcIcons.Taglib), "TagLib", "taglib"),
  SERVICE(NotNullLazyValue.createValue(() -> AllIcons.FileTypes.Config), "Service", "services"),
  JOB(null, "Job", "jobs"),
  CODEC(null, "Codec", "utils"),
  FILTER(null, "Filters", "conf"),
  REALM(null, "Realm", "realms"),
  URLMAPPINGS(null, "UrlMappings", "conf", true) {
    @Override
    public String getDirectory(Module module) {
      return GrailsStructure.isVersionAtLeast("3.0", module) ? "controllers" : "conf";
    }
  },
  BOOTSTRAP(null, "BootStrap", "conf", true) {
    @Override
    public String getDirectory(Module module) {
      return GrailsStructure.isVersionAtLeast("3.0", module) ? "init" : "conf";
    }
  },
  RESOURCES(null, "Resources", "conf", true),
  RESOURCE_MAPPER(null, "ResourceMapper", "resourceMappers"),
  INTERCEPTOR(NotNullLazyValue.createValue(() -> AllIcons.General.Filter), "Interceptor", "controllers", false);

  private final @Nullable NotNullLazyValue<Icon> myIcon;
  public final String suffix;
  public final String fileSuffix;
  private final String myDirectoryName;
  public final boolean mayHaveEmptyName;

  private final Key<CachedValue<GrailsArtifactCache>> cacheKey;

  GrailsArtifact(@Nullable NotNullLazyValue<Icon> icon, String suffix, String dir) {
    this(icon, suffix, dir, false);
  }

  GrailsArtifact(@Nullable NotNullLazyValue<Icon> icon, String suffix, String dir, boolean mayHaveEmptyName) {
    myIcon = icon;
    this.suffix = suffix;
    fileSuffix = suffix + ".groovy";
    myDirectoryName = dir;
    cacheKey = Key.create("GrailArtifact " + dir);
    this.mayHaveEmptyName = mayHaveEmptyName;
  }

  public @Nullable Icon getIcon() {
    return myIcon.getValue();
  }

  public String getDirectory(Module module) {
    return myDirectoryName;
  }

  public @NotNull String getDirectory(@NotNull GrailsApplication application) {
    return myDirectoryName;
  }

  private static @Nullable VirtualFile getSourceRoot(@NotNull VirtualFile file, Project project) {
    if (file.isDirectory()) return null;

    ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    VirtualFile sourceRoot = fileIndex.getSourceRootForFile(file);
    if (sourceRoot != null) {
      VirtualFile parent = sourceRoot.getParent();

      if (parent == null || !parent.getName().equals(GrailsUtils.GRAILS_APP_DIRECTORY)) return null;

      return sourceRoot;
    }

    VirtualFile root = fileIndex.getContentRootForFile(file);
    if (root == null) return null;

    VirtualFile f = file;
    VirtualFile parent = f.getParent();
    if (parent == null) return null;

    while (true) {
      VirtualFile p = parent.getParent();
      if (Comparing.equal(p, root) || p == null) break;
      f = parent;
      parent = p;
    }

    if (parent.getName().equals(GrailsUtils.GRAILS_APP_DIRECTORY)) return f;

    return null;
  }

  private boolean endWithSuffix(String name) {
    return name.endsWith(suffix) && (mayHaveEmptyName || name.length() > suffix.length());
  }

  private boolean endWithFileSuffix(String name) {
    return name.endsWith(fileSuffix) && (mayHaveEmptyName || name.length() > fileSuffix.length());
  }

  public boolean isInstance(@Nullable VirtualFile virtualFile, @NotNull Project project) {
    return getGrailsApp(virtualFile, project) != null;
  }

  private @Nullable VirtualFile getGrailsApp(@Nullable VirtualFile file, Project project) {
    if (file == null) return null;

    if (!endWithFileSuffix(file.getName())) return null;

    Module module = ModuleUtilCore.findModuleForFile(file, project);
    if (module == null) return null;

    VirtualFile sourceRoot = getSourceRoot(file, project);
    if (sourceRoot == null) return null;

    if (!sourceRoot.getName().equals(getDirectory(module))) return null;

    return sourceRoot.getParent();
  }

  private boolean isNameEndWithSuffix(PsiClass psiClass) {
    String className = psiClass.getName();
    return className != null && endWithSuffix(className);
  }

  public @Nullable VirtualFile getGrailsApp(@Nullable PsiClass psiClass) {
    if (!(psiClass instanceof GrClassDefinition)) return null;
    if (!isNameEndWithSuffix(psiClass) || PsiTreeUtil.getParentOfType(psiClass, PsiClass.class) != null) return null;
    return getGrailsApp(psiClass.getContainingFile().getOriginalFile().getVirtualFile(), psiClass.getProject());
  }

  public static @Nullable GrailsArtifact getType(@Nullable PsiClass psiClass) {
    if (!(psiClass instanceof GrClassDefinition)) return null;

    PsiFile psiFile = psiClass.getContainingFile().getOriginalFile();
    VirtualFile virtualFile = psiFile.getVirtualFile();
    if (virtualFile == null) return null;

    final Module module = ModuleUtilCore.findModuleForPsiElement(psiClass);
    if (module == null) return null;

    VirtualFile sourceRoot = getSourceRoot(virtualFile, psiFile.getProject());
    if (sourceRoot == null) return null;

    GrailsArtifact[] artifacts = getSourceRootMap(module).get(sourceRoot.getName());
    if (artifacts == null) return null;

    String className = psiClass.getName();
    if (className == null) return null;

    for (GrailsArtifact candidate : artifacts) {
      if (candidate.endWithSuffix(className)) return candidate;
    }

    return null;
  }

  public boolean isInstance(@Nullable PsiClass psiClass) {
    return getGrailsApp(psiClass) != null;
  }

  public @NotNull String getArtifactName(@NotNull PsiClass psiClass) {
    return getArtifactName(psiClass.getName());
  }

  public @NotNull String getArtifactNameByFileName(String fileName) {
    assert endWithFileSuffix(fileName);
    return StringUtil.decapitalize(fileName.substring(0, fileName.length() - fileSuffix.length()));
  }

  public @NotNull String getArtifactName(@NotNull String className) {
    assert endWithSuffix(className);
    return StringUtil.decapitalize(className.substring(0, className.length() - suffix.length()));
  }

  @Deprecated
  public @Nullable VirtualFile findDirectory(@Nullable Module module) {
    VirtualFile appDir = GrailsFramework.getInstance().findAppDirectory(module);
    if (appDir == null) return null;
    return appDir.findChild(getDirectory(module));
  }

  public @Nullable VirtualFile findDirectory(@NotNull GrailsApplication application) {
    return application.getAppRoot().findChild(getDirectory(application));
  }

  public MultiMap<String, VirtualFile> getVirtualFileMap(@NotNull Module module) {
    return getCache(module).getVirtualFileMap();
  }

  public @NotNull Collection<GrClassDefinition> getInstances(@NotNull Module module, @Nullable String artefactName) {
    if (artefactName == null) return Collections.emptyList();
    return getInstances(module, null, artefactName);
  }

  public @NotNull Collection<GrClassDefinition> getInstances(@NotNull Module module, @Nullable String packageName, @NotNull String artefactName) {
    GrailsArtifactCache cache = getCache(module);
    return packageName == null
           ? cache.getClasses(artefactName)
           : ContainerUtil.filter(cache.getClasses(artefactName), c -> {
             String qualifiedName = c.getQualifiedName();
             return qualifiedName != null && StringUtil.getPackageName(qualifiedName).equals(packageName);
           });
  }

  private GrailsArtifactCache getCache(final @NotNull Module module) {
    final Project project = module.getProject();
    return CachedValuesManager.getManager(project).getCachedValue(module, cacheKey, () -> {
      MultiMap<String, VirtualFile> map = calculateInstances(module);

      MvcModuleStructureSynchronizer synchronizer = MvcModuleStructureSynchronizer.getInstance(project);
      return CachedValueProvider.Result.create(
        new GrailsArtifactCache(map, project),
        synchronizer.getFileAndRootsModificationTracker(),
        GrailsApplicationManager.getInstance(project),
        // calculateInstances() degrades in dumb mode; expire that result when indexing ends.
        DumbService.getInstance(project).getModificationTracker()
      );
    }, false);
  }

  public @NotNull MultiMap<String, GrClassDefinition> getInstances(final @NotNull Module module) {
    return getCache(module).getClassesMap();
  }

  private void calculateInstancesInDirectory(@NotNull VirtualFile contentEntry,
                                             final MultiMap<String, VirtualFile> map,
                                             ProjectFileIndex fileIndex) {
    fileIndex.iterateContentUnderDirectory(contentEntry, fileOrDir -> {
      if (!fileOrDir.isDirectory()) {
        String name = fileOrDir.getName();

        if (endWithFileSuffix(name)) {
          map.putValue(getArtifactNameByFileName(name), fileOrDir);
        }
      }
      return true;
    });
  }

  private @NotNull MultiMap<String, VirtualFile> calculateInstances(final @NotNull Module module) {
    MultiMap<String, VirtualFile> res = new MultiMap<>();

    ProjectFileIndex fileIndex = ProjectRootManager.getInstance(module.getProject()).getFileIndex();

    Set<Module> modules = getRelatedModules(module);

    for (Module m : modules) {
      if (GrailsFramework.isCommonPluginsModule(m)) {
        for (VirtualFile virtualFile : ModuleRootManager.getInstance(m).getContentRoots()) {
          VirtualFile appDirectory = virtualFile.findChild(GrailsUtils.GRAILS_APP_DIRECTORY);
          if (appDirectory != null) {
            VirtualFile dir = appDirectory.findChild(getDirectory(m));
            if (dir != null) {
              calculateInstancesInDirectory(dir, res, fileIndex);
            }
          }
        }
      }
      else {
        VirtualFile directory = findDirectory(m);
        if (directory != null) {
          calculateInstancesInDirectory(directory, res, fileIndex);
        }
      }
    }

    // Grails plugin descriptors are resolved against the module's whole dependency+libraries scope,
    // so this must run once for the module - not once per dependency, which would add every declared
    // artefact as many times as there are modules in the dependency graph (duplicate navigate targets).
    //
    // Skipped during indexing: it queries FilenameIndex and JavaPsiFacade, which throw
    // IndexNotReadyException in dumb mode. The directory scan above needs no index, so artefacts
    // declared in source still resolve; only plugin-descriptor ones are missing until indexing
    // finishes. getCache() depends on the DumbService modification tracker, so the partial result
    // is discarded and recomputed as soon as we are smart again.
    if (!DumbService.isDumb(module.getProject())) {
      collectFromPluginXmls(module, res);
    }

    return res;
  }

  /**
   * The set of modules that share artefacts/views with {@code module} in a multi-project build:
   * {@code module} itself, its (transitive) dependencies, and the modules that (transitively) depend
   * on it. Used both for artefact instance discovery and for cross-module "Go To View" navigation.
   */
  public static @NotNull Set<Module> getRelatedModules(@NotNull Module module) {
    Set<Module> modules = new HashSet<>();
    ModuleUtilCore.getDependencies(module, modules); // module + its (transitive) dependencies
    collectDependentModules(module, modules);        // + modules that (transitively) depend on it
    return modules;
  }

  /**
   * Adds every module that (transitively) depends on {@code module} to {@code result}. This lets a
   * "Go To ..." action on an artefact in an upstream project (e.g. a shared plugin) resolve artefacts
   * declared in downstream projects that depend on it - the reverse of {@link ModuleUtilCore#getDependencies}.
   */
  private static void collectDependentModules(@NotNull Module module, @NotNull Set<Module> result) {
    ModuleManager moduleManager = ModuleManager.getInstance(module.getProject());
    Deque<Module> queue = new ArrayDeque<>();
    queue.add(module);
    while (!queue.isEmpty()) {
      for (Module dependent : moduleManager.getModuleDependentModules(queue.poll())) {
        if (result.add(dependent)) {
          queue.add(dependent);
        }
      }
    }
  }

  private void collectFromPluginXmls(Module module, MultiMap<String, VirtualFile> result) {
    Project project = module.getProject();
    JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
    GlobalSearchScope scope = module.getModuleWithDependenciesAndLibrariesScope(false);
    PsiManager psiManager = PsiManager.getInstance(project);
    JavaDirectoryService directoryService = JavaDirectoryService.getInstance();
    for (VirtualFile virtualFile : FilenameIndex.getVirtualFilesByName("grails-plugin.xml", scope)) {
      PsiFile file = psiManager.findFile(virtualFile);
      if (!(file instanceof XmlFile)) continue;

      PsiDirectory directory = file.getContainingDirectory();
      if (directory == null) continue;

      PsiPackage pckg = directoryService.getPackage(directory);
      if (pckg == null || !pckg.getQualifiedName().equals("META-INF")) continue;

      XmlTag rootTag = ((XmlFile)file).getRootTag();
      if (rootTag == null) continue;
      XmlTag[] subTags = rootTag.findSubTags("resources");
      if (subTags.length != 1) continue;
      XmlTag resourcesTag = subTags[0];
      for (XmlTag tag : resourcesTag.getSubTags()) {
        if (!tag.getName().equals("resource")) continue;
        XmlTagValue value = tag.getValue();
        String fqn = value.getTrimmedText();
        if (!endWithSuffix(fqn)) continue;
        PsiClass clazz = facade.findClass(fqn, scope);
        if (clazz == null) continue;
        String name = clazz.getName();
        if (name == null) continue;
        if (!endWithSuffix(name)) continue;
        result.putValue(getArtifactName(name), clazz.getContainingFile().getVirtualFile());
      }
    }
  }

  private static @NotNull Map<String, GrailsArtifact[]> getSourceRootMap(final @Nullable Module module) {
    if (module == null) return Collections.emptyMap();
    final Project project = module.getProject();
    return CachedValuesManager.getManager(project).getCachedValue(
      module,
      () -> {
        final MultiMap<String, GrailsArtifact> multiMap = new MultiMap<>();
        for (GrailsArtifact artifact : values()) {
          multiMap.putValue(artifact.getDirectory(module), artifact);
        }

        final Map<String, GrailsArtifact[]> result = new HashMap<>();
        for (Map.Entry<String, Collection<GrailsArtifact>> entry : multiMap.entrySet()) {
          Collection<GrailsArtifact> artifacts = entry.getValue();
          result.put(entry.getKey(), artifacts.toArray(new GrailsArtifact[0]));
        }

        return CachedValueProvider.Result.create(
          result,
          MvcModuleStructureSynchronizer.getInstance(project).getFileAndRootsModificationTracker()
        );
      }
    );
  }
}

class GrailsArtifactCache {
  private final MultiMap<String, VirtualFile> myVirtualFileMap;
  private final PsiManager myManager;
  private long myModificationStamp = -1;
  private volatile MultiMap<String, GrClassDefinition> myClassMap;

  GrailsArtifactCache(MultiMap<String, VirtualFile> virtualFileMap, Project project) {
    myVirtualFileMap = virtualFileMap;
    myManager = PsiManager.getInstance(project);
  }

  public MultiMap<String, VirtualFile> getVirtualFileMap() {
    return myVirtualFileMap;
  }

  private @Nullable GrClassDefinition getClassDefinition(@Nullable VirtualFile file) {
    GrTypeDefinition typeDefinition = GroovyUtils.getPublicClass(file, myManager);

    if (typeDefinition instanceof GrClassDefinition) {
      return (GrClassDefinition)typeDefinition;
    }

    return null;
  }

  public @NotNull List<GrClassDefinition> getClasses(@NotNull String artifactName) {
    if (myModificationStamp == myManager.getModificationTracker().getModificationCount()) {
      return (List<GrClassDefinition>)myClassMap.get(artifactName);
    }

    List<VirtualFile> vfList = (List<VirtualFile>)myVirtualFileMap.get(artifactName);
    if (vfList.isEmpty()) {
      return Collections.emptyList();
    }

    return getClasses(vfList);
  }

  private @NotNull List<GrClassDefinition> getClasses(@NotNull List<VirtualFile> vfList) {
    int size = vfList.size();

    if (size == 1) {
      GrClassDefinition classDefinition = getClassDefinition(vfList.get(0));
      if (classDefinition == null) return Collections.emptyList();

      return Collections.singletonList(classDefinition);
    }

    // The same artefact can be discovered through more than one path (e.g. a source directory and a
    // grails-plugin.xml descriptor), so dedupe files to avoid duplicate navigate targets.
    List<GrClassDefinition> list = new ArrayList<>(size);

    for (VirtualFile virtualFile : new LinkedHashSet<>(vfList)) {
      ContainerUtil.addIfNotNull(list, getClassDefinition(virtualFile));
    }

    return list;
  }

  public MultiMap<String, GrClassDefinition> getClassesMap() {
    long modificationStamp = myManager.getModificationTracker().getModificationCount();
    if (myModificationStamp == modificationStamp) {
      return myClassMap;
    }

    MultiMap<String, GrClassDefinition> res = new MultiMap<>();

    for (Map.Entry<String, Collection<VirtualFile>> entry : myVirtualFileMap.entrySet()) {
      List<GrClassDefinition> classes = getClasses((List<VirtualFile>)entry.getValue());
      if (!classes.isEmpty()) {
        res.put(entry.getKey(), classes);
      }
    }

    myClassMap = res;
    myModificationStamp = modificationStamp;

    return res;
  }
}