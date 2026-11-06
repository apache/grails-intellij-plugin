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

package org.apache.grails.intellij.plugin.gsp;

import com.intellij.psi.PsiClass;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.apache.grails.intellij.plugin.lang.gsp.psi.groovy.api.GrGspClass;

import java.util.Map;
import java.util.Set;

/**
 * The implicit variables of a GSP and the class it extends, on the Grails 2 API of the mock Grails 1.3 jars plus the
 * {@code javax.servlet} API. Grails 3 and later are covered by {@link GspImplicitVariablesTest}.
 */
public class GspGrails2ImplicitVariablesTest extends GrailsTestCase {

  @Override
  protected boolean needServletApi() {
    return true;
  }

  public void testGrails2Types() {
    GspImplicitVariableAssertions.assertVariableTypes(this, Map.ofEntries(
      Map.entry("actionName", "java.lang.String"),
      Map.entry("controllerName", "java.lang.String"),
      Map.entry("application", "javax.servlet.ServletContext"),
      Map.entry("request", "javax.servlet.http.HttpServletRequest"),
      Map.entry("response", "javax.servlet.http.HttpServletResponse"),
      Map.entry("session", "javax.servlet.http.HttpSession"),
      Map.entry("pageScope", "groovy.lang.Binding"),
      Map.entry("applicationContext", "org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext"),
      Map.entry("flash", "org.codehaus.groovy.grails.web.servlet.FlashScope"),
      Map.entry("grailsApplication", "org.codehaus.groovy.grails.commons.GrailsApplication"),
      // GroovyPage.getOut() wins over the implicit `out` field.
      Map.entry("out", "java.io.Writer"),
      Map.entry("params", "org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap"),
      Map.entry("webRequest", "org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest"),
      Map.entry("exception", "org.codehaus.groovy.grails.web.errors.GrailsWrappedRuntimeException")),
      // The mock jars leave out grails-spring, which declares GrailsWebApplicationContext.
      Set.of("applicationContext"));
  }

  public void testPageExtendsTheGrails2GroovyPage() {
    GrGspClass page = GspImplicitVariableAssertions.pageClass(addView("ccc/index.gsp", "${this}"));

    PsiClass superClass = page.getSuperClass();
    assertNotNull(superClass);
    assertEquals("org.codehaus.groovy.grails.web.pages.GroovyPage", superClass.getQualifiedName());
    assertEquals("org.codehaus.groovy.grails.web.pages.GroovyPage", page.getSuperTypes()[0].getCanonicalText());
  }
}
