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
package org.jetbrains.plugins.grails.gson;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.GsonConstants;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.toplevel.GrTopStatement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMember;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrScriptField;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GroovyScriptClass;

import java.util.ArrayList;
import java.util.List;

public final class GsonUtils {

  private GsonUtils() {
  }

  public static boolean isGsonFile(@NotNull PsiFile file) {
    return file.getName().endsWith(GsonConstants.FILE_SUFFIX);
  }

  static @NotNull List<GrScriptField> getModelFields(@Nullable PsiElement element) {
    return getModelFields(getScriptClass(element));
  }

  public static @Nullable GroovyScriptClass getScriptClass(@Nullable PsiElement element) {
    PsiElement parent = PsiTreeUtil.getParentOfType(element, GrField.class, GrMethod.class);
    if (!(parent instanceof GrMember action)) return null;

    for (PsiFile view : GrailsUtils.getViewPsiByAction(action)) {
      if (!isGsonFile(view)) continue;
      if (view instanceof GroovyFile groovyFile && groovyFile.getScriptClass() instanceof GroovyScriptClass scriptClass) {
        return scriptClass;
      }
      return null;
    }
    return null;
  }

  public static @NotNull List<GrScriptField> getModelFields(@Nullable GroovyScriptClass scriptClass) {
    if (scriptClass == null) return List.of();
    List<GrScriptField> result = new ArrayList<>();
    for (var field : scriptClass.getFields()) {
      if (field instanceof GrScriptField scriptField) result.add(scriptField);
    }
    return result;
  }

  public static boolean isModelVariable(@NotNull PsiElement variable) {
    return variable instanceof GrVariable grVariable && isModelVariable(grVariable);
  }

  public static boolean isModelVariable(@NotNull GrVariable variable) {
    PsiElement parent = variable.getParent();
    if (parent == null || !(parent.getParent() instanceof GrClosableBlock closure)) return false;
    if (!(variable.getContainingFile() instanceof GroovyFile file)) return false;
    return closure.equals(findModelClosure(file));
  }

  public static @Nullable GrClosableBlock findModelClosure(@NotNull GroovyFile file) {
    for (GrTopStatement statement : file.getTopStatements()) {
      if (!(statement instanceof GrMethodCallExpression call)) continue;
      if (!(call.getInvokedExpression() instanceof GrReferenceExpression ref)) continue;
      if (!"model".equals(ref.getReferenceName())) continue;
      GrClosableBlock[] arguments = call.getClosureArguments();
      return arguments.length == 1 ? arguments[0] : null;
    }
    return null;
  }

  public static boolean isGsonTemplate(@Nullable PsiElement element) {
    if (!(element instanceof GroovyFile file) || !file.isScript()) return false;
    VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null) return false;
    CharSequence name = virtualFile.getNameSequence();
    return name.length() > 0 && name.charAt(0) == '_' && name.toString().endsWith(GsonConstants.FILE_SUFFIX);
  }

  public static @Nullable String getGsonTemplateName(@NotNull GroovyFile file) {
    return isGsonTemplate(file) ? getGsonTemplateName(file.getName()) : null;
  }

  public static @NotNull String getGsonTemplateName(@NotNull String fileName) {
    String name = fileName.startsWith("_") ? fileName.substring(1) : fileName;
    return name.endsWith(GsonConstants.FILE_SUFFIX)
           ? name.substring(0, name.length() - GsonConstants.FILE_SUFFIX.length())
           : name;
  }
}
