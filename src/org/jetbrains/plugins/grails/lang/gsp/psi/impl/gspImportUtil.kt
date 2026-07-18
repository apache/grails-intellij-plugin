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

@file:JvmName("GspImportUtil")

package org.jetbrains.plugins.grails.lang.gsp.psi.impl

import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.plugins.grails.lang.gsp.GspDirectiveKind
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GspGroovyFile
import org.jetbrains.plugins.groovy.lang.resolve.imports.GroovyFileImports
import org.jetbrains.plugins.groovy.lang.resolve.imports.impl.GroovyImportCollector
import java.util.StringTokenizer
import java.util.regex.Pattern

private val gspImportPattern = Pattern.compile(
  "(static\\s+)?(((?:\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}+\\.)*)(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}+))(?:(?:\\s+as\\s+(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}+))|(\\.\\*))?"
)

fun GspGroovyFile.getFileImports(): GroovyFileImports {
  return CachedValuesManager.getCachedValue(this) {
    CachedValueProvider.Result.create(doGetFileImports(), this)
  }
}

private fun GspGroovyFile.doGetFileImports(): GroovyFileImports {
  val collector = GroovyImportCollector(this)
  for (directive in gspLanguageRoot.getDirectiveTags(GspDirectiveKind.PAGE, true)) {
    val attribute = directive.getAttribute("import") ?: continue
    val value = attribute.value ?: continue
    val st = StringTokenizer(value, ";")
    while (st.hasMoreTokens()) {
      collector.addImport(st.nextToken().trim())
    }
  }
  return collector.build()
}

private fun GroovyImportCollector.addImport(str: String) {
  val matcher = gspImportPattern.matcher(str)
  if (!matcher.matches()) return

  val isStatic = matcher.group(1) != null
  val isStar = matcher.group(6) != null
  if (isStatic && isStar) {
    addStaticStarImport(classFqn = matcher.group(2))
  }
  else if (isStatic) {
    val className = matcher.group(3)
    if (className.isEmpty()) return
    addStaticImport(
      classFqn = className.removeSuffix("."),
      memberName = matcher.group(4),
      name = matcher.group(5) ?: matcher.group(4)
    )
  }
  else if (isStar) {
    addStarImport(packageFqn = matcher.group(2))
  }
  else {
    addRegularImport(
      classFqn = matcher.group(2),
      name = matcher.group(5) ?: matcher.group(4)
    )
  }
}
