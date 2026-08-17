/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package org.apache.grails.intellij.plugin.v3;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.LightGroovyTestCase;
import org.jetbrains.plugins.groovy.codeInspection.assignment.GroovyAssignabilityCheckInspection;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;

import java.util.ArrayList;
import java.util.List;

/**
 * A Grails 3+ controller carries the {@code grails.artefact.Controller} trait, so
 * {@code render}/{@code redirect}/{@code params} are real code members. ControllerMembersProvider must not
 * inject its Grails 1.x replacements on top of them: a second applicable {@code render(Map, Closure = null)}
 * left {@code render(view: '...')} reported as "Method call is ambiguous" under static compilation.
 */
public class GrailsControllerTraitMembersTest extends LightGroovyTestCase {

  private final GrailsProjectDescriptor projectDescriptor = new GrailsProjectDescriptor("grails-app/controllers/");

  @Override
  public final @NotNull GrailsProjectDescriptor getProjectDescriptor() {
    return projectDescriptor;
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    getFixture().addClass("""
                            package grails.artefact;
                            public @interface Enhances { String[] value(); }
                            """);
    getFixture().addClass("""
                            package groovy.transform;
                            public @interface CompileStatic {}
                            """);
    getFixture().addClass("""
                            package groovy.lang;
                            public abstract class Closure<V> {}
                            """);
    getFixture().addClass("""
                            package groovy.lang;
                            public interface Writable {}
                            """);
    // The overload set of grails.artefact.controller.support.ResponseRenderer, applied to controllers
    // through grails.artefact.Controller
    getFixture().addFileToProject("grails/artefact/controller/support/ResponseRenderer.groovy", """
      package grails.artefact.controller.support

      @grails.artefact.Enhances("Controller")
      trait ResponseRenderer {
        void render(Object o) {}
        void render(String txt) {}
        void render(CharSequence txt) {}
        void render(Map args) {}
        void render(Closure c) {}
        void render(Map args, Closure c) {}
        void render(Map args, CharSequence body) {}
        void render(Map args, Writable body) {}
      }
      """);
    getFixture().addFileToProject("grails/artefact/controller/support/ResponseRedirector.groovy", """
      package grails.artefact.controller.support

      @grails.artefact.Enhances("Controller")
      trait ResponseRedirector {
        void redirect(Object o) {}
        void redirect(Map args) {}
      }
      """);
    getFixture().addFileToProject("grails/artefact/Controller.groovy", """
      package grails.artefact

      trait Controller implements grails.artefact.controller.support.ResponseRenderer,
                                  grails.artefact.controller.support.ResponseRedirector {}
      """);
  }

  private void configureController(String statement) {
    PsiFile file = getFixture().addFileToProject("com/bar/CccController.groovy", """
      package com.bar

      @groovy.transform.CompileStatic
      class CccController {
        String errorPage = 'error'

        def index() {
          """ + statement + """

        }
      }
      """);
    getFixture().configureFromExistingVirtualFile(file.getVirtualFile());
    getFixture().enableInspections(GroovyAssignabilityCheckInspection.class);
  }

  private @NotNull List<PsiMethod> resolveCandidates(String methodName) {
    GrReferenceExpression ref = null;
    for (GrReferenceExpression candidate : PsiTreeUtil.findChildrenOfType(getFixture().getFile(), GrReferenceExpression.class)) {
      if (methodName.equals(candidate.getReferenceName())) {
        ref = candidate;
        break;
      }
    }
    assertNotNull(methodName + "() call not found", ref);

    List<PsiMethod> methods = new ArrayList<>();
    for (GroovyResolveResult result : ref.multiResolve(false)) {
      PsiElement element = result.getElement();
      if (element instanceof PsiMethod method) methods.add(method);
    }
    return methods;
  }

  private void assertNoAmbiguity(String methodName) {
    List<PsiMethod> candidates = resolveCandidates(methodName);
    assertEquals(methodName + "() must resolve to the single trait method, got " + describe(candidates),
                 1, candidates.size());
    assertEquals("com.bar.CccController", candidates.get(0).getContainingClass().getQualifiedName());

    for (HighlightInfo info : getFixture().doHighlighting()) {
      String description = info.getDescription();
      assertFalse("unexpected highlight: " + description,
                  description != null && description.contains("ambiguous"));
    }
  }

  private static String describe(List<PsiMethod> methods) {
    StringBuilder sb = new StringBuilder();
    for (PsiMethod method : methods) {
      if (!sb.isEmpty()) sb.append("; ");
      sb.append(method.getContainingClass() == null ? "<none>" : method.getContainingClass().getQualifiedName())
        .append('#').append(method.getName()).append(method.getParameterList().getText());
    }
    return sb.toString();
  }

  public void testRenderViewNamedArgument() {
    configureController("render(view: errorPage)");
    assertNoAmbiguity("render");
  }

  public void testRenderTextNamedArgument() {
    configureController("render(text: 'hello')");
    assertNoAmbiguity("render");
  }

  public void testRenderExplicitMap() {
    configureController("render([view: errorPage] as Map)");
    assertNoAmbiguity("render");
  }

  public void testRedirectNamedArguments() {
    configureController("redirect(controller: 'ccc', action: 'index')");
    assertNoAmbiguity("redirect");
  }
}