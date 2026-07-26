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

package org.apache.grails.intellij.plugin.lang.gsp.resolve.taglib;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMember;
import com.intellij.xml.XmlAttributeDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.impl.gtag.GspTagDescriptorService;
import org.jetbrains.plugins.groovy.extensions.GroovyNamedArgumentProvider;
import org.jetbrains.plugins.groovy.extensions.NamedArgumentDescriptor;
import org.jetbrains.plugins.groovy.extensions.impl.NamedArgumentDescriptorImpl;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCall;

import java.util.Map;
import java.util.Set;

public class GrailsTaglibNamedArgumentProvider extends GroovyNamedArgumentProvider {
  @Override
  public void getNamedArguments(@NotNull GrCall call,
                                @NotNull GroovyResolveResult resolveResult,
                                @Nullable String argumentName,
                                boolean forCompletion,
                                @NotNull Map<String, NamedArgumentDescriptor> result) {
    PsiElement resolve = resolveResult.getElement();
    if (!(resolve instanceof TagLibNamespaceDescriptor.GspTagMethod gspTagMethod)) return;

    PsiElement navigationElement = gspTagMethod.getNavigationElement();
    Pair<Map<String,XmlAttributeDescriptor>,Set<String>> pair = GspTagLibUtil.getAttributesDescriptorsFromJavadocs(navigationElement);

    Map<String, XmlAttributeDescriptor> javadocDescriptors = pair.first;
    if (argumentName == null) {
      for (XmlAttributeDescriptor descriptor : javadocDescriptors.values()) {
        convertAndPut(result, descriptor);
      }
    }
    else {
      XmlAttributeDescriptor descriptor = javadocDescriptors.get(argumentName);
      if (descriptor != null) {
        convertAndPut(result, descriptor);
        return;
      }
    }

    PsiClass containingClass = ((PsiMember)navigationElement).getContainingClass();
    if (containingClass == null) return;

    if (GspTagLibUtil.isSdkTagLib(containingClass)) {
      String tagName = gspTagMethod.getName();
      GspTagDescriptorService descriptorService = GspTagDescriptorService.getInstance(containingClass.getProject());

      if (argumentName == null) {
        for (XmlAttributeDescriptor descriptor : descriptorService.getAttributesDescriptors(tagName)) {
          if (!result.containsKey(descriptor.getName())) {
            convertAndPut(result, descriptor);
          }
        }
      }
      else {
        XmlAttributeDescriptor descriptor = descriptorService.getAttributesDescriptor(tagName, argumentName);
        if (descriptor != null) {
          convertAndPut(result, descriptor);
        }
      }
    }
    else {
      if (argumentName == null) {
        for (String attrName : pair.second) {
          if (!result.containsKey(argumentName)) {
            result.put(attrName, NamedArgumentDescriptor.SIMPLE_ON_TOP);
          }
        }
      }
    }
  }
  
  private static void convertAndPut(Map<String, NamedArgumentDescriptor> result, @NotNull XmlAttributeDescriptor descriptor) {
    PsiElement declaration = descriptor.getDeclaration();
    result.put(descriptor.getName(), declaration == null ? NamedArgumentDescriptor.SIMPLE_ON_TOP : new NamedArgumentDescriptorImpl(declaration));
  }
}
