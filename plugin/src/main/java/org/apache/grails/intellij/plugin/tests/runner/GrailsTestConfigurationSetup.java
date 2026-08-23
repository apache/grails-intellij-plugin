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
package org.apache.grails.intellij.plugin.tests.runner;

import com.intellij.openapi.roots.PackageIndex;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.runner.GrailsRunConfiguration;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.util.version.Version;

import java.util.List;

/**
 * Builds the {@code test-app} command line for a run configuration created from a test class or a
 * test directory. Grails 3 selects unit vs integration tests with a flag; earlier versions used the
 * name of the test source root.
 */
public final class GrailsTestConfigurationSetup {

  private GrailsTestConfigurationSetup() {
  }

  static final String UNIT_TEST_KEY_V3 = "-unit";
  static final String INTEGRATION_TEST_KEY_V3 = "-integration";

  private static final List<String> TEST_FOLDER_NAMES = List.of("integration", "unit", "functional");
  private static final String[] TEST_SUFFIXES = {"Tests", "Test"};

  public static @Nullable PsiElement setupConfigurationByClass(@NotNull GrailsRunConfiguration configuration,
                                                               @NotNull GrailsApplication application,
                                                               @NotNull PsiClass aClass,
                                                               @Nullable PsiMethod method) {
    String classFqn = aClass.getQualifiedName();
    if (classFqn == null) return null;
    String className = aClass.getName();
    if (className == null) return null;

    ProjectFileIndex fileIndex = ProjectRootManager.getInstance(aClass.getProject()).getFileIndex();
    VirtualFile srcRoot = fileIndex.getSourceRootForFile(aClass.getContainingFile().getVirtualFile());
    if (srcRoot == null) return null;
    boolean isV3 = application.getGrailsVersion().isAtLeast(Version.GRAILS_3_0);

    StringBuilder parameters = new StringBuilder("test-app");

    if (!isV3) {
      String testKind = getParamKeyByTestRoot(application, srcRoot);
      if (testKind == null) return null;
      parameters.append(" ").append(testKind);
    }

    String testName = trimTestSuffix(classFqn);

    String configurationName;
    String testFilter;
    PsiElement sourceElement;

    if (method != null && useMethod(application, method)) {
      String methodName = method.getName();
      configurationName = methodName + "()";
      testFilter = ParametersListUtil.join(testName + "." + methodName);
      sourceElement = method;
    }
    else {
      configurationName = className;
      testFilter = testName;
      sourceElement = aClass;
    }

    parameters.append(" ").append(testFilter);

    if (isV3) {
      VirtualFile integrationTestRoot = findIntegrationTestDirV3(application);
      boolean integrationTest = integrationTestRoot != null && VfsUtilCore.isAncestor(integrationTestRoot, srcRoot, true);
      parameters.append(" ").append(integrationTest ? INTEGRATION_TEST_KEY_V3 : UNIT_TEST_KEY_V3);
    }

    configuration.setName(configurationName);
    configuration.setProgramParameters(parameters.toString());

    return sourceElement;
  }

  private static boolean useMethod(@NotNull GrailsApplication application, @NotNull PsiMethod method) {
    Version version = application.getGrailsVersion();
    if (!version.isAtLeast(Version.GRAILS_1_1) || !GrailsTestConfigurationProducer.isGrailsTestMethod(method)) return false;
    // Spock method names with spaces cannot be passed through the Grails 3 test filter.
    if (version.compareTo(Version.GRAILS_3_0) > 0 && method.getName().contains(" ")) return false;
    return true;
  }

  private static @NotNull String trimTestSuffix(@NotNull String name) {
    for (String suffix : TEST_SUFFIXES) {
      if (name.endsWith(suffix)) return StringUtil.trimEnd(name, suffix);
    }
    return name;
  }

  public static boolean setupConfigurationByDir(@NotNull GrailsRunConfiguration configuration,
                                                @NotNull GrailsApplication application,
                                                @NotNull PsiDirectory dir) {
    return application.getGrailsVersion().isAtLeast(Version.GRAILS_3_0)
           ? setupConfigurationByDirV3(configuration, application, dir)
           : setupConfigurationByDirOld(configuration, application, dir);
  }

  private static boolean setupConfigurationByDirV3(@NotNull GrailsRunConfiguration configuration,
                                                   @NotNull GrailsApplication application,
                                                   @NotNull PsiDirectory dir) {
    VirtualFile directory = dir.getVirtualFile();
    String packageName = PackageIndex.getInstance(dir.getProject()).getPackageNameByDirectory(directory);

    VirtualFile integrationTestsDir = findIntegrationTestDirV3(application);
    VirtualFile testsDir = findTestDirV3(application);

    boolean integrationTest;
    if (packageName == null) {
      if (directory.equals(testsDir)) {
        integrationTest = false;
      }
      else if (directory.equals(integrationTestsDir)) {
        integrationTest = true;
      }
      else {
        return false;
      }
    }
    else {
      if (testsDir != null && VfsUtilCore.isAncestor(testsDir, directory, true)) {
        integrationTest = false;
      }
      else if (integrationTestsDir != null && VfsUtilCore.isAncestor(integrationTestsDir, directory, true)) {
        integrationTest = true;
      }
      else {
        return false;
      }
    }

    StringBuilder configurationName = new StringBuilder(integrationTest ? "Grails integration tests" : "Grails tests");
    StringBuilder parameters = new StringBuilder("test-app");

    if (!StringUtil.isEmpty(packageName)) {
      configurationName.append(": ").append(packageName);
      parameters.append(" ").append(packageName).append(".**");
    }

    parameters.append(" ").append(integrationTest ? INTEGRATION_TEST_KEY_V3 : UNIT_TEST_KEY_V3);

    configuration.setName(configurationName.toString());
    configuration.setProgramParameters(parameters.toString());

    return true;
  }

  private static boolean setupConfigurationByDirOld(@NotNull GrailsRunConfiguration configuration,
                                                    @NotNull GrailsApplication application,
                                                    @NotNull PsiDirectory dir) {
    VirtualFile directory = dir.getVirtualFile();

    ProjectFileIndex fileIndex = ProjectRootManager.getInstance(dir.getProject()).getFileIndex();
    if (!fileIndex.isInTestSourceContent(directory)) return false;

    VirtualFile srcRoot = fileIndex.getSourceRootForFile(directory);
    if (srcRoot == null) return false;
    String testKind = getParamKeyByTestRoot(application, srcRoot);
    if (testKind == null) return false;

    StringBuilder configurationName = new StringBuilder("Grails " + testKind + " tests");
    StringBuilder parameters = new StringBuilder("test-app");

    parameters.append(" ").append(testKind);

    String packageName = PackageIndex.getInstance(dir.getProject()).getPackageNameByDirectory(directory);
    if (!StringUtil.isEmpty(packageName)) {
      configurationName.append(": ").append(packageName);
      parameters.append(" ").append(packageName).append(".**");
    }

    configuration.setName(configurationName.toString());
    configuration.setProgramParameters(parameters.toString());

    return true;
  }

  private static @Nullable VirtualFile findTestDirV3(@NotNull GrailsApplication application) {
    return VfsUtil.findRelativeFile(application.getRoot(), "src", "test");
  }

  static @Nullable VirtualFile findIntegrationTestDirV3(@NotNull GrailsApplication application) {
    return VfsUtil.findRelativeFile(application.getRoot(), "src", "integration-test");
  }

  /**
   * Grails 1.3 switched the test-kind argument from {@code -unit} to {@code unit:}.
   */
  static @Nullable String getParamKeyByTestRoot(@NotNull GrailsApplication application, @NotNull VirtualFile testRoot) {
    String name = testRoot.getName();
    if (TEST_FOLDER_NAMES.contains(name)) {
      return application.getGrailsVersion().isAtLeast(Version.GRAILS_1_3) ? name + ":" : "-" + name;
    }
    return null;
  }
}
