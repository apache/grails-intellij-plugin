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

package org.apache.grails.intellij.plugin.references.domain.namedQuery;

import com.intellij.pom.PomDeclarationSearcher;
import com.intellij.pom.PomTarget;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.references.domain.DomainDescriptor;
import org.apache.grails.intellij.plugin.references.domain.GormUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;

public final class GormNamedQueryDeclarationSearcher extends PomDeclarationSearcher {
  @Override
  public void findDeclarationsAt(@NotNull PsiElement element, int offsetInElement, @NotNull Consumer<? super PomTarget> consumer) {
    if (!GormUtils.isNamedQueryDeclaration(element)) return;

    PsiClass aClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);
    assert aClass != null;

    DomainDescriptor descriptor = DomainDescriptor.getDescriptor(aClass);

    NamedQueryDescriptor queryDescriptor = descriptor.getNamedQueries().get(((GrReferenceExpression)element).getReferenceName());

    if (queryDescriptor == null) return;

    consumer.consume(queryDescriptor.getVariable());
  }
}
