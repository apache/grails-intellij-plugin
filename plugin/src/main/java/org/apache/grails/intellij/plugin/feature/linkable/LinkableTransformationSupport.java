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
package org.apache.grails.intellij.plugin.feature.linkable;

import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.GrModifierFlags;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.transformations.AstTransformationSupport;
import org.jetbrains.plugins.groovy.transformations.TransformationContext;

public final class LinkableTransformationSupport implements AstTransformationSupport {

  @Override
  public void applyTransformation(@NotNull TransformationContext context) {
    addLinkMethods(context, "grails.rest.Linkable");
    addLinkMethods(context, "grails.rest.Resource");
  }

  private static void addLinkMethods(@NotNull TransformationContext context, @NotNull String fqn) {
    PsiAnnotation annotation = context.getAnnotation(fqn);
    if (annotation == null) return;

    // public void link(Map)
    GrLightMethodBuilder linkMap = new GrLightMethodBuilder(context.getManager(), "link");
    linkMap.addModifier(GrModifierFlags.PUBLIC_MASK);
    linkMap.setReturnType(PsiTypes.voidType());
    linkMap.addParameter("link", CommonClassNames.JAVA_UTIL_MAP);
    linkMap.setNavigationElement(annotation);
    linkMap.putUserData(Linkable.LINK_METHOD_KEY, Linkable.LINK_METHOD_MARKER);
    context.addMethod(linkMap);

    // public void link(Link)
    GrLightMethodBuilder linkTyped = new GrLightMethodBuilder(context.getManager(), "link");
    linkTyped.addModifier(GrModifierFlags.PUBLIC_MASK);
    linkTyped.setReturnType(PsiTypes.voidType());
    linkTyped.addParameter("link", Linkable.LINK_FQN);
    linkTyped.setNavigationElement(annotation);
    context.addMethod(linkTyped);
  }
}
