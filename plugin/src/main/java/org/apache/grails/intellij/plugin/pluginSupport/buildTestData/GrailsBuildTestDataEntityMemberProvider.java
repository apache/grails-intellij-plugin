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
package org.apache.grails.intellij.plugin.pluginSupport.buildTestData;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.DelegatingScopeProcessor;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.PsiTypesUtil;
import org.apache.grails.intellij.plugin.references.MemberProvider;
import org.apache.grails.intellij.plugin.util.GrailsPsiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.util.dynamicMembers.DynamicMemberUtils;

/**
 * build-test-data 3.x+ puts {@code build}/{@code findOrBuild} on the domain class itself rather than
 * behind the Grails 2 {@code @Build} mixin, so they are available on any GORM entity without an
 * annotation. The signatures mirror {@code grails.buildtestdata.utils.MetaHelper#addBuildMetaMethods},
 * which registers them on the entity's static metaclass.
 *
 * <p>Contribution is gated on resolving {@code grails.buildtestdata.TestData} from the place's own
 * resolve scope. That is both the "is the plugin used at all" check and the scope check: it is a test
 * dependency, so production code cannot resolve it and gets no members. {@code MetaHelper} delegates
 * every one of these methods to {@code TestData}, so wherever the runtime methods exist that class
 * does too.
 *
 * @see GrailsBuildTestDataMemberProvider the Grails 2 {@code @Build} path, still supported
 */
public class GrailsBuildTestDataEntityMemberProvider extends MemberProvider {

  public static final Object METHOD_MARKER = "grails:plugins:buildTestData:entity";

  /** Present in every version that injects the metaclass methods; also the test-scope gate. */
  public static final String TEST_DATA_CLASS = "grails.buildtestdata.TestData";

  private static final String CLASS_SOURCE = """
    /** @originalInfo provided by 'build-test-data' plugin */
    class BuildTestDataEntityMethods<D> {
      public static D build() {}
      public static D build(Map args) {}
      public static D build(Map args, Map data) {}
      public static D findOrBuild() {}
      public static D findOrBuild(Map data) {}
    }""";

  @Override
  public void processMembers(PsiScopeProcessor processor, final PsiClass domainClass, PsiElement place) {
    if (JavaPsiFacade.getInstance(place.getProject()).findClass(TEST_DATA_CLASS, place.getResolveScope()) == null) {
      return;
    }

    DelegatingScopeProcessor delegateProcessor = new DelegatingScopeProcessor(processor) {

      private PsiSubstitutor mySubstitutor;

      @Override
      public boolean execute(@NotNull PsiElement element, @NotNull ResolveState state) {
        if (!(element instanceof PsiMethod)) return true;

        if (mySubstitutor == null) {
          mySubstitutor = PsiSubstitutor.EMPTY.putAll(((DynamicMemberUtils.DynamicElement)element).getSourceClass(),
                                                     new PsiType[]{PsiTypesUtil.getClassType(domainClass)});
        }

        GrLightMethodBuilder lightMethod = GrailsPsiUtil.substitute((PsiMethod)element, mySubstitutor);
        lightMethod.setMethodKind(METHOD_MARKER);
        lightMethod.setData(domainClass);

        return super.execute(lightMethod, state);
      }
    };

    DynamicMemberUtils.process(delegateProcessor, true, place, CLASS_SOURCE);
  }
}
