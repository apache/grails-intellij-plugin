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

package org.jetbrains.plugins.grails.gson

import org.jetbrains.plugins.groovy.lang.psi.GroovyFile
import org.jetbrains.plugins.groovy.lang.resolve.imports.GrImportContributor
import org.jetbrains.plugins.groovy.lang.resolve.imports.GroovyImport
import org.jetbrains.plugins.groovy.lang.resolve.imports.StaticStarImport

class GsonImportContributor : GrImportContributor {

  private val imports: List<GroovyImport> by lazy {
    mutableListOf<GroovyImport>().apply {
      add(StaticStarImport("org.springframework.http.HttpStatus"))
      add(StaticStarImport("org.springframework.http.HttpMethod"))
      add(StaticStarImport("grails.web.http.HttpHeaders"))
    }
  }

  override fun getFileImports(file: GroovyFile): List<GroovyImport> = if (isGsonFile(file)) imports else emptyList()
}
