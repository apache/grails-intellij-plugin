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

package org.jetbrains.plugins.grails.pluginSupport.assetPipeline

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.patterns.XmlPatterns
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceHelper
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet
import com.intellij.psi.util.CachedValueProvider.Result
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.ProcessingContext
import org.jetbrains.plugins.grails.plugins.getSourcePlugins
import org.jetbrains.plugins.grails.structure.GrailsApplication
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager

// #CHECK# asset.pipeline.grails.AssetsTagLib
private val ASSET_PLACES = listOf(
    "asset:javascript" to "src",
    "asset:stylesheet" to "href",
    "asset:stylesheet" to "src",
    "asset:image" to "src",
    "asset:link" to "href",
    "asset:assetPathExists" to "src"
)

class AssetReferenceContributor : PsiReferenceContributor() {

  override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
    val provider = AssetReferenceProvider()
    for ((tag, attribute) in ASSET_PLACES) {
      val tagPattern = XmlPatterns.xmlTag().withName(tag)
      val attributePattern = XmlPatterns.xmlAttribute(attribute).withParent(tagPattern)
      val attributeValuePattern = XmlPatterns.xmlAttributeValue(attributePattern)
      registrar.registerReferenceProvider(attributeValuePattern, provider, PsiReferenceRegistrar.HIGHER_PRIORITY)
    }
  }
}

class AssetReferenceProvider : PsiReferenceProvider() {

  override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<out PsiReference> {
    return AssetsFileReferenceSet(element).allReferences
  }
}

class AssetsFileReferenceSet(element: PsiElement) : FileReferenceSet(element) {

  override fun computeDefaultContexts(): Collection<PsiFileSystemItem> = GrailsApplicationManager.findApplication(element)?.getAssetFolders() ?: emptyList()

  override fun createFileReference(range: TextRange?, index: Int, text: String?): FileReference = object : FileReference(this, range, index, text) {
    override fun getContextsForBindToElement(curVFile: VirtualFile?, project: Project?, helper: FileReferenceHelper?) = contexts
  }

  override fun isSoft(): Boolean = true
}

private fun GrailsApplication.getAssetFolders() = CachedValuesManager.getManager(project).getCachedValue(this) {
  Result.create(doGesAssetFolders(), ProjectRootManager.getInstance(project))
}

private fun GrailsApplication.doGesAssetFolders(): Collection<PsiFileSystemItem> {
  fun GrailsApplication.findAssetsFolder() = appRoot.findChild("assets")?.children

  val ownAssets = this.findAssetsFolder()?.toList() ?: emptyList()
  val pluginAssets = this.getSourcePlugins().flatMap { plugin ->
    plugin.pluginApplication.findAssetsFolder()?.toList() ?: emptyList()
  }

  val manager = PsiManager.getInstance(project)
  val sourceAssets = (ownAssets + pluginAssets).mapNotNull {
    manager.findDirectory(it)
  }

  val facade = JavaPsiFacade.getInstance(project)
  val scope = getScope(true, false)
  val compiledAssets = ASSETS_PACKAGES.flatMap { pckg ->
    facade.findPackage(pckg)?.getDirectories(scope)?.toList() ?: emptyList()
  }

  return sourceAssets + compiledAssets
}

private val ASSETS_PACKAGES = arrayOf(
    "META-INF.assets",
    "META-INF.static",
    "META-INF.resources"
)
