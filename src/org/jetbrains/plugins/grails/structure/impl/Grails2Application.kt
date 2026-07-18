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

package org.jetbrains.plugins.grails.structure.impl

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.grails.config.GrailsModuleStructureUtil
import org.jetbrains.plugins.grails.config.GrailsSettingsService.getGrailsSettings
import org.jetbrains.plugins.grails.config.PrintGrailsSettingsConstants
import org.jetbrains.plugins.grails.util.version.Version
import org.jetbrains.plugins.grails.util.version.VersionImpl

internal class Grails2Application(root: VirtualFile, module: Module) : OldGrailsModuleBasedApplication(module, root) {

  override val name: String get() = getApplicationPropertiesValue("app.name") ?: module.name

  override val appVersion: String? get() = getApplicationPropertiesValue("app.version")

  val applicationPropertiesVersion: Version? get() {
    val version = getApplicationPropertiesValue(GrailsModuleStructureUtil.GRAILS_VERSION_KEY)
    return if (version.isNullOrBlank()) null else VersionImpl(version)
  }

  val isRunForked: Boolean get() = getBooleanSetting(PrintGrailsSettingsConstants.DEBUG_RUN_FORK)

  val isTestForked: Boolean get() = getBooleanSetting(PrintGrailsSettingsConstants.DEBUG_TEST_FORK)

  private fun getApplicationPropertiesValue(key: String) = runReadAction { applicationProperties?.getPropertyValue(key) }

  private fun getBooleanSetting(key: String) = runReadAction { getGrailsSettings(module).properties[key]?.toBoolean() ?: false }
}
