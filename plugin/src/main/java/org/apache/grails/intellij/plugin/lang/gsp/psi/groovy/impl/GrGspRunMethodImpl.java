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

package org.apache.grails.intellij.plugin.lang.gsp.psi.groovy.impl;

import com.intellij.lang.ASTNode;
import org.apache.grails.intellij.plugin.lang.gsp.psi.groovy.api.GrGspRunBlock;
import org.apache.grails.intellij.plugin.lang.gsp.psi.groovy.api.GrGspRunMethod;
import org.jetbrains.plugins.groovy.lang.psi.impl.GroovyPsiElementImpl;

public class GrGspRunMethodImpl extends GroovyPsiElementImpl implements GrGspRunMethod {

  private static final String GSPMETHOD_SYNTHETIC_NAME = "GspRunMethod";

  public GrGspRunMethodImpl(ASTNode node) {
    super(node);
  }

  @Override
  public String toString() {
    return GSPMETHOD_SYNTHETIC_NAME;
  }

  @Override
  public GrGspRunBlock getRunBlock() {
    GrGspRunBlock runBlock = findChildByClass(GrGspRunBlock.class);
    assert runBlock != null;
    return runBlock;
  }
}
