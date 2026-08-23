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

package org.apache.grails.intellij.plugin.references.tagSupport;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.xml.XmlAttributeValue;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.lang.gsp.GspFileViewProvider;
import org.apache.grails.intellij.plugin.references.common.GspTagWrapper;
import org.apache.grails.intellij.plugin.references.controller.ActionReference;
import org.apache.grails.intellij.plugin.util.GrailsArtifact;
import org.apache.grails.intellij.plugin.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;

public class GspActionAttributeSupport extends TagAttributeReferenceProvider {

  public GspActionAttributeSupport() {
    super("action", "g", null);
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                         @NotNull String text,
                                                         int offset,
                                                         @NotNull GspTagWrapper gspTagWrapper) {
    String controllerName;

    PsiElement attributeController = gspTagWrapper.getAttributeValue("controller");
    if (attributeController == null) {
      PsiFile psiFile = element.getContainingFile();
      if (psiFile == null) return PsiReference.EMPTY_ARRAY;

      VirtualFile virtualFile = psiFile.getOriginalFile().getVirtualFile();
      if (virtualFile == null) return PsiReference.EMPTY_ARRAY;

      if (psiFile.getViewProvider() instanceof GspFileViewProvider) {
        controllerName = GrailsUtils.getControllerNameByGsp(virtualFile);
        if (controllerName == null) return PsiReference.EMPTY_ARRAY;
      }
      else {
        PsiClass controllerClass = PsiUtil.getContainingNotInnerClass(element);
        if (!GrailsArtifact.CONTROLLER.isInstance(controllerClass)) return PsiReference.EMPTY_ARRAY;
        assert controllerClass != null;
        controllerName = GrailsArtifact.CONTROLLER.getArtifactName(controllerClass);
      }
    }
    else {
      controllerName = gspTagWrapper.getAttributeText(attributeController);
      if (controllerName == null) {
        return PsiReference.EMPTY_ARRAY;
      }
    }

    return new PsiReference[]{new ActionReference(element, element instanceof XmlAttributeValue, controllerName)};
  }

}
