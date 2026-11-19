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
package org.apache.grails.intellij.plugin.lang.gsp.resolve.taglib;

import com.intellij.openapi.project.DumbService;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.util.InheritanceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.params.GrParameter;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyCommonClassNames;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Finds the tags a tag library declares as methods, which Grails 8 added next to the closure-field form:
 * <pre>
 * def emoticon(Map attrs, Closure body) { ... }
 *
 * &#64;Tag
 * def greeting(String name) { ... }
 * </pre>
 * The rules are those of {@code org.grails.taglib.discovery.TagDiscoveryRules}, which is the single
 * statement of them in Grails itself. They are restated here against PSI, so a change there has to be
 * followed here.
 */
public final class GspTagMethodUtil {

  public static final String TAG_ANNOTATION = "grails.gsp.Tag";
  public static final String NOT_A_TAG_ANNOTATION = "grails.gsp.NotATag";

  public static final String ATTRS_PARAMETER_NAME = "attrs";
  public static final String BODY_PARAMETER_NAME = "body";

  private static final Set<String> LANGUAGE_METHOD_NAMES = Set.of(
    "invokeMethod", "methodMissing", "propertyMissing", "getProperty", "setProperty",
    "getMetaClass", "setMetaClass", "equals", "hashCode", "toString");

  private static final Set<String> FRAMEWORK_METHOD_NAMES = Set.of(
    "afterPropertiesSet", "currentRequestAttributes", "destroy", "initializeTagLibrary",
    "onApplicationEvent", "raw", "throwTagError", "withCodec");

  private GspTagMethodUtil() {
  }

  /**
   * Whether the Grails on the classpath of {@code place} dispatches tags to methods at all. Before
   * Grails 8 a method with a tag-like signature is an ordinary helper, so it must not be offered as a
   * tag. The {@code @Tag} annotation arrived with the feature and is what this looks for, rather than
   * a version number, because the version is not known for every kind of project.
   */
  public static boolean isMethodTagSupported(@NotNull PsiElement place) {
    if (DumbService.isDumb(place.getProject())) return false;
    return JavaPsiFacade.getInstance(place.getProject()).findClass(TAG_ANNOTATION, place.getResolveScope()) != null;
  }

  /**
   * Whether Grails would dispatch a tag to this method. Checks the declaration only; whether the
   * containing class is a tag library, and whether Grails is new enough, is for the caller.
   */
  public static boolean isTagMethod(@NotNull PsiMethod method) {
    if (method.isConstructor()) return false;
    if (!method.hasModifierProperty(PsiModifier.PUBLIC) || method.hasModifierProperty(PsiModifier.STATIC)) return false;

    if (method.hasAnnotation(NOT_A_TAG_ANNOTATION)) return false;
    if (method.hasAnnotation(TAG_ANNOTATION)) return true;

    String name = method.getName();
    if (name.isEmpty() || name.charAt(0) == '<' || name.indexOf('$') >= 0) return false;
    if (LANGUAGE_METHOD_NAMES.contains(name) || FRAMEWORK_METHOD_NAMES.contains(name)) return false;

    PsiParameter[] parameters = method.getParameterList().getParameters();
    if (isPropertyAccessor(name, parameters.length)) return false;

    return hasInvocableTagShape(method, parameters);
  }

  /**
   * Whether {@code method} is a tag of a tag library that dispatches tags to methods.
   */
  public static boolean isTagOfMethodTagLibrary(@NotNull PsiMethod method) {
    return isTagMethod(method) && isMethodTagSupported(method);
  }

  private static boolean isPropertyAccessor(@NotNull String name, int parameterCount) {
    if (parameterCount == 0 && (name.startsWith("get") || name.startsWith("is"))) return true;
    return parameterCount == 1 && name.startsWith("set");
  }

  /**
   * A tag is called as {@code (attrs)} or {@code (attrs, body)}. Parameters with default values produce
   * further overloads, so every arity the declaration can be called at is considered.
   */
  private static boolean hasInvocableTagShape(@NotNull PsiMethod method, PsiParameter @NotNull [] parameters) {
    int required = 0;
    for (PsiParameter parameter : parameters) {
      if (!isOptional(parameter)) required++;
    }

    for (int arity = Math.max(required, 1); arity <= parameters.length; arity++) {
      if (arity == 1 && (isAttrsParameter(method, parameters[0]) || isBodyParameter(method, parameters[0]))) return true;
      if (arity == 2 && isAttrsParameter(method, parameters[0]) && isBodyParameter(method, parameters[1])) return true;
    }
    return false;
  }

  private static boolean isOptional(@NotNull PsiParameter parameter) {
    return parameter instanceof GrParameter && ((GrParameter)parameter).isOptional();
  }

  public static boolean isAttrsParameter(@NotNull PsiMethod method, @NotNull PsiParameter parameter) {
    return InheritanceUtil.isInheritor(parameter.getType(), CommonClassNames.JAVA_UTIL_MAP)
           && carriesName(method, parameter, ATTRS_PARAMETER_NAME);
  }

  public static boolean isBodyParameter(@NotNull PsiMethod method, @NotNull PsiParameter parameter) {
    return InheritanceUtil.isInheritor(parameter.getType(), GroovyCommonClassNames.GROOVY_LANG_CLOSURE)
           && carriesName(method, parameter, BODY_PARAMETER_NAME);
  }

  /**
   * A parameter qualifies when it carries the expected name, or when its name is not known. Source
   * always has names; a compiled class may have been built without them, and Grails then has nothing
   * to check against either.
   */
  private static boolean carriesName(@NotNull PsiMethod method, @NotNull PsiParameter parameter, @NotNull String expectedName) {
    return method instanceof PsiCompiledElement || expectedName.equals(parameter.getName());
  }

  /**
   * The methods a class declares itself. Grails reads method tags from the declaring class alone: an
   * inherited method is not callable as a tag, unlike an inherited closure field.
   */
  private static PsiMethod @NotNull [] declaredMethods(@NotNull PsiClass aClass) {
    // getCodeMethods() leaves out the accessors Groovy generates for properties, which are never tags.
    return aClass instanceof GrTypeDefinition ? ((GrTypeDefinition)aClass).getCodeMethods() : aClass.getMethods();
  }

  /**
   * Every method tag the classes declare, by tag name. A tag declared through several overloads is
   * represented by the first of them; see {@link #getTagOverloads}.
   */
  public static @NotNull Map<String, PsiMethod> collectTagMethods(@NotNull Collection<? extends PsiClass> classes) {
    Map<String, PsiMethod> res = null;
    for (PsiClass aClass : classes) {
      if (!isMethodTagSupported(aClass)) continue;

      for (PsiMethod method : declaredMethods(aClass)) {
        if (isTagMethod(method)) {
          if (res == null) res = new LinkedHashMap<>();
          res.putIfAbsent(method.getName(), method);
        }
      }
    }
    return res == null ? Collections.emptyMap() : res;
  }

  public static @Nullable PsiMethod findTagMethod(@NotNull Collection<? extends PsiClass> classes, @NotNull String tagName) {
    for (PsiClass aClass : classes) {
      PsiMethod[] candidates = aClass.findMethodsByName(tagName, false);
      if (candidates.length == 0 || !isMethodTagSupported(aClass)) continue;

      for (PsiMethod method : declaredMethods(aClass)) {
        if (tagName.equals(method.getName()) && isTagMethod(method)) return method;
      }
    }
    return null;
  }

  /**
   * All the methods that together implement the tag {@code method} belongs to: the method itself and
   * the tag methods of the same name declared next to it.
   */
  public static @NotNull List<PsiMethod> getTagOverloads(@NotNull PsiMethod method) {
    PsiClass aClass = method.getContainingClass();
    if (aClass == null) return Collections.singletonList(method);

    List<PsiMethod> res = new ArrayList<>();
    for (PsiMethod candidate : declaredMethods(aClass)) {
      if (method.getName().equals(candidate.getName()) && isTagMethod(candidate)) {
        res.add(candidate);
      }
    }
    return res.isEmpty() ? Collections.singletonList(method) : res;
  }

  /**
   * Whether the tag takes a body, i.e. whether any of its overloads has a body parameter.
   */
  public static boolean acceptsBody(@NotNull PsiMethod method) {
    for (PsiMethod overload : getTagOverloads(method)) {
      for (PsiParameter parameter : overload.getParameterList().getParameters()) {
        if (isBodyParameter(overload, parameter)) return true;
      }
    }
    return false;
  }

  /**
   * The attributes a tag binds straight to parameters, which is what a {@code @Tag} method does with
   * every parameter that is neither the attribute map nor the body.
   */
  public static @NotNull Set<String> getParameterAttributeNames(@NotNull PsiMethod method) {
    Set<String> res = null;
    for (PsiMethod overload : getTagOverloads(method)) {
      for (PsiParameter parameter : overload.getParameterList().getParameters()) {
        if (isAttrsParameter(overload, parameter) || isBodyParameter(overload, parameter)) continue;

        if (res == null) res = new LinkedHashSet<>();
        res.add(parameter.getName());
      }
    }
    return res == null ? Collections.emptySet() : res;
  }
}
