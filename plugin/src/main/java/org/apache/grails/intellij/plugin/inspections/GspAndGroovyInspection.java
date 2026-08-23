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

package org.apache.grails.intellij.plugin.inspections;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.XmlElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFileBase;
import org.jetbrains.plugins.groovy.lang.psi.GroovyRecursiveElementVisitor;

public abstract class GspAndGroovyInspection extends LocalInspectionTool {

  @Override
  public @NotNull PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, final boolean isOnTheFly) {
    GspElementVisitor visitor = createGspElementVisitor();
    assert visitor.getProblemHolder() == null; // Assert it's new visitor instance.

    visitor.setProblemHolder(holder);
    visitor.setOnTheFly(isOnTheFly);
    return visitor;
  }

  protected abstract GroovyRecursiveElementVisitor createGroovyFileVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly);

  protected abstract GspElementVisitor createGspElementVisitor();

  public class GspElementVisitor extends XmlElementVisitor {

    private ProblemsHolder myProblemHolder;
    private boolean myIsOnTheFly;

    public ProblemsHolder getProblemHolder() {
      return myProblemHolder;
    }

    public void setProblemHolder(ProblemsHolder holder) {
      myProblemHolder = holder;
    }

    public boolean isOnTheFly() {
      return myIsOnTheFly;
    }

    public void setOnTheFly(boolean onTheFly) {
      myIsOnTheFly = onTheFly;
    }

    @Override
    public void visitFile(@NotNull PsiFile psiFile) {
      if (psiFile instanceof GroovyFileBase) {
        GroovyRecursiveElementVisitor visitor = createGroovyFileVisitor(myProblemHolder, myIsOnTheFly);
        ((GroovyFileBase)psiFile).accept(visitor);
      }
    }
  }
}
