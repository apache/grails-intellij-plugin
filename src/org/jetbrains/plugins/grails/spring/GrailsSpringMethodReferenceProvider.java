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

package org.jetbrains.plugins.grails.spring;

import com.intellij.spring.references.SpringBeanNamesReferenceProvider;
import org.jetbrains.plugins.grails.references.GrailsMethodNamedArgumentReferenceProvider;

public class GrailsSpringMethodReferenceProvider implements GrailsMethodNamedArgumentReferenceProvider.Contributor {
  @Override
  public void register(GrailsMethodNamedArgumentReferenceProvider registrar) {
    ProviderAdapter refProfider = new ProviderAdapter(new SpringBeanNamesReferenceProvider());

    registrar.register(0, refProfider, new LightMethodCondition(GrailsResourcesGroovyMemberContributor.REF_METHOD_KIND), "ref");
    registrar.register(0, refProfider, new ClassNameCondition(GrailsResourcesGroovyMemberContributor.BEAN_BUILDER), "getBeanDefinition");

    ClassNameCondition runtimeSpringCfg = new ClassNameCondition("org.codehaus.groovy.grails.commons.spring.RuntimeSpringConfiguration");
    registrar.register(0, refProfider, runtimeSpringCfg, "containsBean");
    registrar.register(0, refProfider, runtimeSpringCfg, "getBeanConfig");
    registrar.register(1, refProfider, runtimeSpringCfg, "addAlias");
    registrar.register(0, refProfider, runtimeSpringCfg, "getBeanDefinition");


  }
}
