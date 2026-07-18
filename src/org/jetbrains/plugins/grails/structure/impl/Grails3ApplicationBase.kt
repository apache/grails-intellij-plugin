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

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.lazyUnsafe
import org.jetbrains.plugins.grails.gradle.GrailsModuleData
import org.jetbrains.plugins.grails.structure.Grails3Application
import org.jetbrains.plugins.grails.structure.GrailsApplicationBase
import org.jetbrains.plugins.grails.util.version.VersionImpl

internal abstract class Grails3ApplicationBase(
    project: Project,
    root: VirtualFile,
    private val moduleDataNode: DataNode<ModuleData>
) : GrailsApplicationBase(project, root), Grails3Application {

  override val name: String get() = moduleDataNode.data.externalName

  override val appVersion: String? get() = moduleDataNode.data.version

  override val grailsVersion by lazyUnsafe { VersionImpl(gradleData.grailsVersion) }

  override val gradleData by lazyUnsafe { ExternalSystemApiUtil.find(moduleDataNode, GrailsModuleData.KEY)!!.data }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other?.javaClass != javaClass) return false
    if (!super.equals(other)) return false

    other as Grails3ApplicationBase

    return moduleDataNode == other.moduleDataNode
  }

  override fun hashCode() = 31 * super.hashCode() + moduleDataNode.hashCode()
}