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

package org.jetbrains.plugins.grails.projectView.nodes;

import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;

import java.util.Objects;

public final class TreeNodeUtil {

  private TreeNodeUtil() {
  }

  /**
   * Walks up from the node's parent looking for the first ancestor value of the given type.
   * These were reified inline extensions in Kotlin; Java needs the type passed explicitly.
   */
  public static <T> @Nullable T findValueOfType(@NotNull AbstractTreeNode<?> node, @NotNull Class<T> type) {
    AbstractTreeNode<?> current = node.getParent();
    while (current != null) {
      Object value = current.getValue();
      if (type.isInstance(value)) {
        return type.cast(value);
      }
      current = current.getParent();
    }
    return null;
  }

  public static <T> @NotNull T findNotNullValueOfType(@NotNull AbstractTreeNode<?> node, @NotNull Class<T> type) {
    return Objects.requireNonNull(findValueOfType(node, type),
                                  () -> "no ancestor value of type " + type.getName() + " above " + node);
  }

  public static boolean mayContain(@NotNull GrailsApplication application, @NotNull VirtualFile file) {
    PsiFile psiFile = PsiManager.getInstance(application.getProject()).findFile(file);
    return psiFile instanceof GroovyFile groovyFile && groovyFile.getClasses().length == 1;
  }
}
