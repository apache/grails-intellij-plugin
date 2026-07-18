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

package org.jetbrains.plugins.grails.feature.linkable

import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiTypes
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.GrModifierFlags
import org.jetbrains.plugins.groovy.transformations.AstTransformationSupport
import org.jetbrains.plugins.groovy.transformations.TransformationContext

class LinkableTransformationSupport : AstTransformationSupport {

  override fun applyTransformation(context: TransformationContext) {
    addLinkMethods(context, "grails.rest.Linkable")
    addLinkMethods(context, "grails.rest.Resource")
  }
}

private fun addLinkMethods(context: TransformationContext, fqn: String) {
  context.getAnnotation(fqn)?.let {
    // public void link(Map)
    context.addMethod(context.memberBuilder.method("link") {
      addModifier(GrModifierFlags.PUBLIC_MASK)
      returnType = PsiTypes.voidType()
      addParameter("link", CommonClassNames.JAVA_UTIL_MAP)
      navigationElement = it
      putUserData(LINK_METHOD_KEY, LINK_METHOD_MARKER)
    })

    // public void link(Link)
    context.addMethod(context.memberBuilder.method("link") {
      addModifier(GrModifierFlags.PUBLIC_MASK)
      returnType = PsiTypes.voidType()
      addParameter("link", LINK_FQN)
      navigationElement = it
    })
  }
}