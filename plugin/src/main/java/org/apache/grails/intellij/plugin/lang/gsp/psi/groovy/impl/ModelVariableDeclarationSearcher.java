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

package org.apache.grails.intellij.plugin.lang.gsp.psi.groovy.impl;

import com.intellij.pom.PomDeclarationSearcher;
import com.intellij.pom.PomTarget;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.Consumer;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.lang.gsp.GspLanguage;
import org.apache.grails.intellij.plugin.lang.gsp.psi.groovy.api.GrGspExprInjection;
import org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.api.GspFile;
import org.apache.grails.intellij.plugin.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentLabel;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightVariable;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;

public final class ModelVariableDeclarationSearcher extends PomDeclarationSearcher {

  @Override
  public void findDeclarationsAt(@NotNull PsiElement element, int offsetInElement, @NotNull Consumer<? super PomTarget> consumer) {
    if (!(element instanceof GrArgumentLabel)) return;

    PsiElement namedArgument = element.getParent();
    if (!(namedArgument instanceof GrNamedArgument)) return;

    PsiElement listOrMap = namedArgument.getParent();
    if (!(listOrMap instanceof GrListOrMap)) return;

    if (processVariable((GrNamedArgument)namedArgument, findGspByClosureReturn(listOrMap), element, consumer)) {
      return;
    }

    processVariable((GrNamedArgument)namedArgument, findGspByModelMap(listOrMap), element, consumer);
  }

  private static boolean processVariable(GrNamedArgument namedArgument,
                                         @Nullable GspFile gspFile,
                                         PsiElement element,
                                         Consumer<? super PomTarget> consumer) {
    if (gspFile == null) return false;

    PsiVariable variable = GspModelVariableModel.getInstance(gspFile).getVariable(namedArgument.getLabelName());

    if (variable instanceof GrLightVariable && ((GrLightVariable)variable).getDeclarations().contains(element)) {
      consumer.consume(variable);
      return true;
    }

    return false;
  }

  private static @Nullable GspFile findGspByModelMap(PsiElement modelMap) {
    PsiElement fileReferenceElement = null;
    
    PsiElement parent = modelMap.getParent();
    if (parent instanceof GrNamedArgument namedArgument) {
      if ("model".equals(namedArgument.getLabelName())) {
        fileReferenceElement = PsiUtil.getNamedArgumentValue(namedArgument, "view");
        if (fileReferenceElement == null) {
          fileReferenceElement = PsiUtil.getNamedArgumentValue(namedArgument, "template");
        }
      }
    }
    else if (parent instanceof GrGspExprInjection) {
      PsiElement gspElement = parent.getContainingFile().getViewProvider().findElementAt(modelMap.getTextOffset(), GspLanguage.INSTANCE);
      XmlAttribute xmlAttribute = PsiTreeUtil.getParentOfType(gspElement, XmlAttribute.class);
      if (xmlAttribute != null && "model".equals(xmlAttribute.getName())) {
        XmlTag xmlTag = xmlAttribute.getParent();
        if (xmlTag != null) {
          XmlAttribute attribute = xmlTag.getAttribute("view");
          if (attribute == null) {
            attribute = xmlTag.getAttribute("template");
          }

          if (attribute != null) {
            fileReferenceElement = attribute.getValueElement();
          }
        }
      }
    }

    if (fileReferenceElement != null) {
      for (PsiReference reference : fileReferenceElement.getReferences()) {
        if (reference instanceof FileReference) {
          final FileReference lastReference = ((FileReference)reference).getFileReferenceSet().getLastReference();
          if (lastReference == null) break;

          PsiFileSystemItem resolve = lastReference.resolve();
          if (resolve instanceof GspFile) return (GspFile)resolve;
          break;
        }
      }
    }

    return null;
  }

  private static @Nullable GspFile findGspByClosureReturn(PsiElement returnMap) {
    PsiElement action = PsiTreeUtil.getParentOfType(returnMap, GrField.class, GrMethod.class);
    if (action == null) return null;
    return (GspFile)ContainerUtil.find(GrailsUtils.getViewPsiByAction(action), view -> view instanceof GspFile);
  }

}
