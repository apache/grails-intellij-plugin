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
package org.apache.grails.intellij.plugin.lang;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.util.GrailsArtifact;
import org.apache.grails.intellij.plugin.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.GrModifierFlags;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrMethodWrapper;
import org.jetbrains.plugins.groovy.lang.psi.util.GrTraitUtil;
import org.jetbrains.plugins.groovy.transformations.AstTransformationSupport;
import org.jetbrains.plugins.groovy.transformations.TransformationContext;

public final class DirtyCheckableContributor implements AstTransformationSupport {

  public static final String DIRTY_CHECK_FQN = "grails.gorm.dirty.checking.DirtyCheck";
  public static final String DIRTY_CHECKABLE_FQN = "org.grails.datastore.mapping.dirty.checking.DirtyCheckable";
  public static final String ORIGIN_INFO = "via @DirtyCheck";

  /**
   * DirtyCheckable is added in compile time when:
   *
   * <ol>
   *   <li>class annotated explicitly -&gt; org.codehaus.groovy.grails.compiler.gorm.DirtyCheckTransformation</li>
   *   <li>class is a GORM domain</li>
   * </ol>
   */
  private static boolean check(@NotNull GrTypeDefinition clazz) {
    return AnnotationUtil.isAnnotated(clazz, DIRTY_CHECK_FQN, 0)
           || GrailsUtils.calculateArtifactType(clazz) == GrailsArtifact.DOMAIN;
  }

  /**
   * In GORM 5, DirtyCheckable was introduced as a trait.
   * If it is a trait, then we do not collect its methods here,
   * because they will be collected in
   * {@link org.jetbrains.plugins.groovy.transformations.impl.TraitTransformationSupport TraitTransformationSupport}.
   * If it is a simple interface, then we are dealing with pre GORM 5, where all its methods are added one by one to the target class.
   */
  @Override
  public void applyTransformation(@NotNull TransformationContext context) {
    GrTypeDefinition clazz = context.getCodeClass();
    if (!check(clazz)) return;
    context.addInterface(DIRTY_CHECKABLE_FQN);
    PsiClass dirtyCheckable = JavaPsiFacade.getInstance(clazz.getProject())
      .findClass(DIRTY_CHECKABLE_FQN, clazz.getResolveScope());
    if (dirtyCheckable == null || !dirtyCheckable.isInterface() || GrTraitUtil.isTrait(dirtyCheckable)) return;
    for (PsiMethod interfaceMethod : dirtyCheckable.getMethods()) {
      GrMethodWrapper wrapper = GrMethodWrapper.wrap(interfaceMethod);
      wrapper.getModifierList().removeModifier(GrModifierFlags.ABSTRACT_MASK);
      wrapper.setOriginInfo(ORIGIN_INFO);
      context.addMethod(wrapper);
    }
  }
}
