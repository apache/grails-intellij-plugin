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
package org.jetbrains.plugins.grails.feature.linkable;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PsiMethodPattern;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.infos.CandidateInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.extensions.GroovyNamedArgumentProvider;
import org.jetbrains.plugins.groovy.extensions.NamedArgumentDescriptor;
import org.jetbrains.plugins.groovy.extensions.impl.StringTypeCondition;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElement;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentLabel;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCall;

import java.util.Map;

public final class LinkableNamedArgumentsProvider extends GroovyNamedArgumentProvider {

  private static final NamedArgumentDescriptor STRING_DESCRIPTOR = new LinkDescriptor(NamedArgumentDescriptor.TYPE_STRING);
  private static final NamedArgumentDescriptor BOOL_DESCRIPTOR = new LinkDescriptor(NamedArgumentDescriptor.TYPE_BOOL);

  private static final Map<String, NamedArgumentDescriptor> LINK_NAMED_ARGS = Map.of(
    "rel", STRING_DESCRIPTOR,
    "href", STRING_DESCRIPTOR,
    "hreflang", new LinkDescriptor(new StringTypeCondition("java.util.Locale")),
    "contentType", STRING_DESCRIPTOR,
    "title", STRING_DESCRIPTOR,
    "deprecated", BOOL_DESCRIPTOR,
    "templated", BOOL_DESCRIPTOR
  );

  // grails.rest.Link#createLink(java.util.Map)
  private static final PsiMethodPattern LINK_METHOD_PATTERN = new PsiMethodPattern()
    .withName("createLink")
    .definedInClass(Linkable.LINK_FQN)
    .withParameters(CommonClassNames.JAVA_UTIL_MAP);

  @Override
  public void getNamedArguments(@NotNull GrCall call,
                               @NotNull GroovyResolveResult resolveResult,
                               @Nullable String argumentName,
                               boolean forCompletion,
                               @NotNull Map<String, NamedArgumentDescriptor> result) {
    PsiElement resolved = resolveResult.getElement();
    boolean isSynthetic = resolved != null && resolved.getUserData(Linkable.LINK_METHOD_KEY) == Linkable.LINK_METHOD_MARKER;
    if (isSynthetic || LINK_METHOD_PATTERN.accepts(resolved)) {
      result.putAll(LINK_NAMED_ARGS);
    }
  }

  /**
   * Delegating wrapper that only replaces reference creation, so each label resolves to the matching
   * field on {@code grails.rest.Link}. Kotlin expressed this with interface delegation.
   */
  private static final class LinkDescriptor implements NamedArgumentDescriptor {

    private final NamedArgumentDescriptor myDelegate;

    private LinkDescriptor(NamedArgumentDescriptor delegate) {
      myDelegate = delegate;
    }

    @Override
    public Priority getPriority() {
      return myDelegate.getPriority();
    }

    @Override
    public boolean checkType(@NotNull PsiType type, @NotNull GroovyPsiElement context) {
      return myDelegate.checkType(type, context);
    }

    @Override
    public @Nullable PsiElement getNavigationElement() {
      return myDelegate.getNavigationElement();
    }

    @Override
    public LookupElement customizeLookupElement(@NotNull LookupElementBuilder builder) {
      return myDelegate.customizeLookupElement(builder);
    }

    @Override
    public @NotNull PsiPolyVariantReference createReference(@NotNull GrArgumentLabel label) {
      return new LinkReference(label);
    }
  }

  private static final class LinkReference extends PsiPolyVariantReferenceBase<GrArgumentLabel> {

    private LinkReference(@NotNull GrArgumentLabel label) {
      super(label);
    }

    @Override
    public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
      String name = getElement().getName();
      if (name == null) return ResolveResult.EMPTY_ARRAY;
      PsiClass clazz = JavaPsiFacade.getInstance(getElement().getProject())
        .findClass(Linkable.LINK_FQN, getElement().getResolveScope());
      if (clazz == null) return ResolveResult.EMPTY_ARRAY;
      PsiField field = clazz.findFieldByName(name, false);
      if (field == null) return ResolveResult.EMPTY_ARRAY;
      return new ResolveResult[]{new CandidateInfo(field, PsiSubstitutor.EMPTY)};
    }
  }
}
