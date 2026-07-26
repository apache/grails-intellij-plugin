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

import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.util.execution.ParametersListUtil
import org.jetbrains.plugins.grails.runner.GrailsRunConfiguration
import org.jetbrains.plugins.grails.structure.GrailsApplication
import org.jetbrains.plugins.grails.tests.runner.GrailsTestConfigurationProducer.isGrailsTestMethod
import org.jetbrains.plugins.grails.util.version.v11
import org.jetbrains.plugins.grails.util.version.v3

internal fun setupConfigurationByClass(configuration: GrailsRunConfiguration,
                                       application: GrailsApplication,
                                       aClass: PsiClass,
                                       method: PsiMethod?): PsiElement? {
  val classFqn = aClass.qualifiedName ?: return null
  val className = aClass.name ?: return null

  val fileIndex = ProjectRootManager.getInstance(aClass.project).fileIndex
  val srcRoot = fileIndex.getSourceRootForFile(aClass.containingFile.virtualFile) ?: return null
  val v3 = application.grailsVersion >= v3

  val parameters = StringBuilder("test-app")

  if (!v3) {
    val testKind = getParamKeyByTestRoot(application, srcRoot) ?: return null
    parameters.append(" ").append(testKind)
  }

  val testName = trimTestSuffix(classFqn)

  val configurationName: String
  val testFilter: String
  val sourceElement: PsiElement

  if (method != null && useMethod(application, method)) {
    val methodName = method.name
    configurationName = "$methodName()"
    testFilter = ParametersListUtil.join("$testName.$methodName")
    sourceElement = method
  }
  else {
    configurationName = className
    testFilter = testName
    sourceElement = aClass
  }

  parameters.append(" ").append(testFilter)

  if (v3) {
    val integrationTestRoot = application.findIntegrationTestDirV3()
    val integrationTest = integrationTestRoot != null && VfsUtilCore.isAncestor(integrationTestRoot, srcRoot, true)
    parameters.append(" ").append(if (integrationTest) integrationTestKeyV3 else unitTestKeyV3)
  }

  configuration.name = configurationName
  configuration.programParameters = parameters.toString()

  return sourceElement
}

private fun useMethod(application: GrailsApplication, method: PsiMethod): Boolean {
  val version = application.grailsVersion
  if (version < v11 || !isGrailsTestMethod(method)) return false
  if (version > v3 && method.name.contains(" ")) return false
  return true
}

private val suffixes = arrayOf("Tests", "Test")

private fun trimTestSuffix(name: String): String {
  for (suffix in suffixes) {
    if (name.endsWith(suffix)) return name.removeSuffix(suffix)
  }
  return name
}
