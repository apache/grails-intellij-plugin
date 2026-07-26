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

package org.apache.grails.intellij.plugin.references.filter;

import com.intellij.psi.PsiClass;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.apache.grails.intellij.plugin.references.MemberProvider;
import org.apache.grails.intellij.plugin.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.util.dynamicMembers.DynamicMemberUtils;

public class FilterMemberProvider extends MemberProvider {

  private static final String CLASS_SOURCE = "class FilterElements {" +
                                             GrailsUtils.COMMON_WEB_PROPERTIES +
                                             " private final org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext applicationContext;" +
                                             " private void redirect(Map params){def z = params.uri + params.url + params.controller + params.action + params.id + params.fragment + params.params}" +
                                             " private void render(Closure cl){}" +
                                             " private void render(Map params, Closure cl = null){def z = params.text + params.builder " +
                                             "+ params.view + params.template + params.var + params.bean + params.model + params.collection " +
                                             "+ params.contentType + params.encoding + params.converter + params.plugin + params.status + params.contextPath}" +
                                             " private void render(String text){}" +
                                             " private void render(org.codehaus.groovy.grails.web.converters.Converter converter){}" +
                                             "}";

  @Override
  public void processMembers(PsiScopeProcessor processor, PsiClass psiClass, GrReferenceExpression ref) {
    DynamicMemberUtils.process(processor, psiClass, ref, CLASS_SOURCE);
  }
}
