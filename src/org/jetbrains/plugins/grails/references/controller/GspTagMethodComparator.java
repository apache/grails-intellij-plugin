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

package org.jetbrains.plugins.grails.references.controller;

import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.resolve.taglib.TagLibNamespaceDescriptor;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyMethodResult;
import org.jetbrains.plugins.groovy.lang.resolve.GrMethodComparator;

final class GspTagMethodComparator extends GrMethodComparator {
  @Override
  public Boolean dominated(@NotNull GroovyMethodResult result1,
                           @NotNull GroovyMethodResult result2,
                           @NotNull Context context) {

    final PsiMethod method1 = result1.getElement();
    final PsiMethod method2 = result2.getElement();

    if (method1 instanceof TagLibNamespaceDescriptor.GspTagMethod) {
      if (!(method2 instanceof TagLibNamespaceDescriptor.GspTagMethod)) {
        return true;
      }
    }
    else {
      if (method2 instanceof TagLibNamespaceDescriptor.GspTagMethod) {
        return false;
      }
    }

    return null;
  }
}
