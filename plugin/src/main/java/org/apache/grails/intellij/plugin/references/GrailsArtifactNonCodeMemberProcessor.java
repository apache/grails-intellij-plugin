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

package org.apache.grails.intellij.plugin.references;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.pluginSupport.buildTestData.GrailsBuildTestDataEntityMemberProvider;
import org.apache.grails.intellij.plugin.pluginSupport.buildTestData.GrailsBuildTestDataMemberProvider;
import org.apache.grails.intellij.plugin.pluginSupport.seachable.GrailsSearchableMemberProvider;
import org.apache.grails.intellij.plugin.references.bootstrap.GrailsBootStrapMemberProvider;
import org.apache.grails.intellij.plugin.references.controller.ControllerMembersProvider;
import org.apache.grails.intellij.plugin.references.domain.DomainMembersProvider;
import org.apache.grails.intellij.plugin.references.filter.FilterMemberProvider;
import org.apache.grails.intellij.plugin.references.jobs.JobsMemberProvider;
import org.apache.grails.intellij.plugin.references.taglib.TaglibMembersProvider;
import org.apache.grails.intellij.plugin.references.urlMappings.UrlMappingMemberProvider;
import org.apache.grails.intellij.plugin.util.GrailsArtifact;
import org.apache.grails.intellij.plugin.util.GrailsPsiUtil;
import org.apache.grails.intellij.plugin.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

import java.util.EnumMap;
import java.util.Map;

public final class GrailsArtifactNonCodeMemberProcessor extends NonCodeMembersContributor {

  private static volatile Map<GrailsArtifact, MemberProvider[]> MAP;

  public static Map<GrailsArtifact, MemberProvider[]> getMemberProviderMap() {
    Map<GrailsArtifact, MemberProvider[]> res = MAP;
    if (res == null) {
      res = new EnumMap<>(GrailsArtifact.class);

      res.put(GrailsArtifact.CONTROLLER, new MemberProvider[]{new ControllerMembersProvider()});
      res.put(GrailsArtifact.DOMAIN, new MemberProvider[]{new DomainMembersProvider(), new GrailsSearchableMemberProvider(), new GrailsBuildTestDataMemberProvider(),
                                                     new GrailsBuildTestDataEntityMemberProvider()});
      res.put(GrailsArtifact.TAGLIB, new MemberProvider[]{new TaglibMembersProvider()});
      res.put(GrailsArtifact.JOB, new MemberProvider[]{new JobsMemberProvider()});
      res.put(GrailsArtifact.FILTER, new MemberProvider[]{new FilterMemberProvider()});
      res.put(GrailsArtifact.URLMAPPINGS, new MemberProvider[]{new UrlMappingMemberProvider()});
      res.put(GrailsArtifact.BOOTSTRAP, new MemberProvider[]{new GrailsBootStrapMemberProvider()});

      MAP = res;
    }

    return res;
  }

  private static boolean isTagLibByPackage(@NotNull PsiClass aClass) {
    String qualifiedName = aClass.getQualifiedName();

    return qualifiedName != null && qualifiedName.endsWith("TagLib") && qualifiedName.startsWith("org.codehaus.groovy.grails.plugins.web.taglib.");
  }
  
  @Override
  public void processDynamicElements(@NotNull PsiType qualifierType,
                                     @Nullable PsiClass psiClass,
                                     @NotNull PsiScopeProcessor processor,
                                     @NotNull PsiElement place,
                                     @NotNull ResolveState state) {
    // Since 2026.2 a bare class-reference qualifier (e.g. `Domain.findAllBy...`) is passed
    // with a null psiClass and an Object qualifier type, so recover the referenced class
    // from the static qualifier. getArtifact() below still gates members to Grails artifacts.
    final PsiClass targetClass = psiClass != null ? psiClass : resolveStaticQualifierClass(place);
    if (targetClass == null) return;

    GrailsArtifact artifact = CachedValuesManager.getCachedValue(targetClass, () ->
      CachedValueProvider.Result.create(getArtifact(targetClass), PsiModificationTracker.MODIFICATION_COUNT));
    if (artifact == null) return;

    // See org.codehaus.groovy.grails.compiler.logging.LoggingTransformer
    if (!GrailsPsiUtil.processLogVariable(processor, targetClass, ResolveUtil.getNameHint(processor))) return;

    MemberProvider[] providers = getMemberProviderMap().get(artifact);

    if (providers != null) {
      for (MemberProvider provider : providers) {
        provider.processMembers(processor, targetClass, place);
      }
    }
  }

  private static @Nullable PsiClass resolveStaticQualifierClass(@NotNull PsiElement place) {
    if (!(place instanceof GrReferenceExpression)) return null;
    GrExpression qualifier = ((GrReferenceExpression)place).getQualifierExpression();
    if (qualifier == null) return null;
    PsiReference ref = qualifier.getReference();
    PsiElement resolved = ref == null ? null : ref.resolve();
    return resolved instanceof PsiClass ? (PsiClass)resolved : null;
  }

  private static @Nullable GrailsArtifact getArtifact(@NotNull PsiClass psiClass) {
    GrailsArtifact artifact = GrailsUtils.calculateArtifactType(psiClass);
    return artifact != null ? artifact : isTagLibByPackage(psiClass) ? GrailsArtifact.TAGLIB : null;
  }
}
