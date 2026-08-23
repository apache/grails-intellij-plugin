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
package org.apache.grails.intellij.plugin.gson;

import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.GsonConstants;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral;

public final class GsonTemplateReference extends PsiReferenceBase<GrLiteral> {

  public GsonTemplateReference(@NotNull GrLiteral literal) {
    super(literal);
  }

  @Override
  public @Nullable PsiElement resolve() {
    GrLiteral element = getElement();
    if (!(element.getValue() instanceof String value)) return null;
    if (element.getContainingFile() == null) return null;
    VirtualFile virtualFile = element.getContainingFile().getVirtualFile();
    if (virtualFile == null) return null;

    VirtualFile template;
    if (value.startsWith("/")) {
      // An absolute template path is resolved against the views source root, with the file name
      // rewritten to the underscore-prefixed template form.
      int slash = value.lastIndexOf('/');
      String templateName = value.substring(slash + 1);
      String templatePath = value.substring(0, slash + 1) + "_" + templateName + GsonConstants.FILE_SUFFIX;
      VirtualFile viewsRoot = ProjectFileIndex.getInstance(element.getProject()).getSourceRootForFile(virtualFile);
      if (viewsRoot == null) return null;
      template = VfsUtilCore.findRelativeFile(templatePath, viewsRoot);
    }
    else {
      VirtualFile parent = virtualFile.getParent();
      template = parent == null ? null : parent.findChild("_" + value + GsonConstants.FILE_SUFFIX);
    }

    return template == null ? null : PsiManager.getInstance(element.getProject()).findFile(template);
  }

  @Override
  public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
    if (!(getElement().getValue() instanceof String current)) throw new IllegalStateException();
    String templateName = GsonUtils.getGsonTemplateName(newElementName);
    // Keep an absolute path's directory part; a bare name is replaced outright.
    int slash = current.lastIndexOf('/');
    String newContent = current.startsWith("/") && slash >= 0
                        ? current.substring(0, slash + 1) + templateName
                        : templateName;
    return super.handleElementRename(newContent);
  }
}
