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

package org.jetbrains.plugins.grails.structure

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.grails.GroovyMvcIcons
import org.jetbrains.plugins.grails.config.GrailsConstants
import javax.swing.Icon

abstract class GrailsApplicationBase(
  override val project: Project,
  override val root: VirtualFile
) : UserDataHolderBase(), GrailsApplication {

  override val appRoot: VirtualFile get() = root.findChild(GrailsConstants.APP_DIRECTORY)!!

  override val icon: Icon = GroovyMvcIcons.Grails_app

  private var myValid = true

  override val isValid: Boolean get() = myValid && root.findChild(GrailsConstants.APP_DIRECTORY) != null && !project.isDisposed

  override fun invalidate() {
    myValid = false
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other?.javaClass != javaClass) return false

    other as GrailsApplicationBase

    if (project != other.project) return false
    if (root != other.root) return false

    return true
  }

  override fun hashCode(): Int = 31 * project.hashCode() + root.hashCode()

  override fun toString(): String = "${javaClass.simpleName}{name: $name, root: $root, version: $grailsVersion}"
}
