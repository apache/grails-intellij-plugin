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

package org.jetbrains.plugins.grails.tests.runner

import com.intellij.openapi.roots.PackageIndex
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VfsUtil.findRelativeFile
import com.intellij.openapi.vfs.VfsUtilCore.isAncestor
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import org.jetbrains.plugins.grails.runner.GrailsRunConfiguration
import org.jetbrains.plugins.grails.structure.GrailsApplication
import org.jetbrains.plugins.grails.util.version.v3

internal fun setupConfigurationByDir(configuration: GrailsRunConfiguration, application: GrailsApplication, dir: PsiDirectory): Boolean {
  return if (application.grailsVersion >= v3) {
    setupConfigurationByDirV3(configuration, application, dir)
  }
  else {
    setupConfigurationByDirOld(configuration, application, dir)
  }
}

private fun setupConfigurationByDirV3(configuration: GrailsRunConfiguration, application: GrailsApplication, dir: PsiDirectory): Boolean {
  val directory = dir.virtualFile
  val packageName = PackageIndex.getInstance(dir.project).getPackageNameByDirectory(directory)

  val integrationTestsDir = application.findIntegrationTestDirV3()
  val testsDir = application.findTestDirV3()

  val integrationTest: Boolean = if (packageName == null) {
    when (directory) {
      testsDir -> false
      integrationTestsDir -> true
      else -> return false
    }
  }
  else {
    when {
      testsDir != null && isAncestor(testsDir, directory, true) -> false
      integrationTestsDir != null && isAncestor(integrationTestsDir, directory, true) -> true
      else -> return false
    }
  }

  val configurationName = StringBuilder(if (integrationTest) "Grails integration tests" else "Grails tests")
  val parameters = StringBuilder("test-app")

  if (!packageName.isNullOrEmpty()) {
    configurationName.append(": ").append(packageName)
    parameters.append(" ").append(packageName).append(".**")
  }

  parameters.append(" ").append(if (integrationTest) integrationTestKeyV3 else unitTestKeyV3)

  configuration.name = configurationName.toString()
  configuration.programParameters = parameters.toString()

  return true
}

internal const val unitTestKeyV3 = "-unit"
internal const val integrationTestKeyV3 = "-integration"

private fun GrailsApplication.findTestDirV3(): VirtualFile? = findRelativeFile(root, "src", "test")
internal fun GrailsApplication.findIntegrationTestDirV3(): VirtualFile? = findRelativeFile(root, "src", "integration-test")

private fun setupConfigurationByDirOld(configuration: GrailsRunConfiguration, application: GrailsApplication, dir: PsiDirectory): Boolean {
  val directory = dir.virtualFile

  val fileIndex = ProjectRootManager.getInstance(dir.project).fileIndex
  if (!fileIndex.isInTestSourceContent(directory)) return false

  val srcRoot = fileIndex.getSourceRootForFile(directory) ?: return false
  val testKind = getParamKeyByTestRoot(application, srcRoot) ?: return false

  val configurationName = StringBuilder("Grails $testKind tests")
  val parameters = StringBuilder("test-app")

  parameters.append(" ").append(testKind)

  val packageName = PackageIndex.getInstance(dir.project).getPackageNameByDirectory(directory)
  if (!packageName.isNullOrEmpty()) {
    configurationName.append(": ").append(packageName)
    parameters.append(" ").append(packageName).append(".**")
  }

  configuration.name = configurationName.toString()
  configuration.programParameters = parameters.toString()

  return true
}
