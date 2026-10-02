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

package org.apache.grails.intellij.plugin.spring;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.ElementClassHint;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.params.GrParameter;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightField;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;
import org.jetbrains.plugins.groovy.util.dynamicMembers.DynamicMemberUtils;

/**
 * Resolution, completion and type inference for the Grails 8 beans DSL (see {@link GrailsBeansDsl}):
 * <ul>
 *   <li>directly inside a {@code beans} closure or one of its groups, the {@code bean}/{@code field}/{@code method}/
 *   {@code group} declarations, whose results carry the qualifiers each of them chains with;</li>
 *   <li>inside a {@code bean(...)} or {@code method(...)} body, the members declared by the {@code field(...)} and
 *   {@code method(...)} declarations beside it, which the compiled factory methods share.</li>
 * </ul>
 */
public final class GrailsBeansDslMemberContributor extends NonCodeMembersContributor {

  public static final String MEMBER_ORIGIN_INFO = "via Grails beans DSL";

  /**
   * The declarations and their qualifier chains. Every qualifier that takes arguments takes {@code Object...}, so
   * named attributes, types, String class names and the trailing body closure are all accepted.
   */
  static final String DSL_SOURCE = """
    class GrailsBeansDsl {
      /** Declares a bean named after the decapitalized simple name of {@code type}, built by its no-argument constructor. */
      BeanDeclaration bean(Class type) {}
      /** Declares a bean named after the decapitalized simple name of {@code type}. The closure's typed parameters are injected; the closure body builds the bean, or, left empty, the constructor taking the parameters is called. */
      BeanDeclaration bean(Class type, Closure body) {}
      /** Declares a bean named {@code name}, built by the no-argument constructor of {@code type}. */
      BeanDeclaration bean(String name, Class type) {}
      /** Declares a bean named {@code name}. The closure's typed parameters are injected; the closure body builds the bean, or, left empty, the constructor taking the parameters is called. */
      BeanDeclaration bean(String name, Class type, Closure body) {}
      /** Declares a bean of type {@code type} built as {@code new implementation()}. */
      BeanDeclaration bean(Class type, Class implementation) {}
      /** Declares a bean of type {@code type} built by the {@code implementation} constructor taking the closure's typed parameters. */
      BeanDeclaration bean(Class type, Class implementation, Closure parameters) {}
      /** Declares a bean named {@code name} of type {@code type} built as {@code new implementation()}. */
      BeanDeclaration bean(String name, Class type, Class implementation) {}
      /** Declares a bean named {@code name} of type {@code type} built by the {@code implementation} constructor taking the closure's typed parameters. */
      BeanDeclaration bean(String name, Class type, Class implementation, Closure parameters) {}

      /** Declares a private field, shared by the bean and helper methods, named after the decapitalized simple name of {@code type}. */
      FieldDeclaration field(Class type) {}
      /** Declares a private field, shared by the bean and helper methods. */
      FieldDeclaration field(String name, Class type) {}

      /** Declares a private helper method, named after the decapitalized simple name of {@code type}, returning {@code type}. */
      MethodDeclaration method(Class type) {}
      /** Declares a private helper method, named after the decapitalized simple name of {@code type}, whose parameters and body are the closure's. */
      MethodDeclaration method(Class type, Closure body) {}
      /** Declares a private helper method returning {@code type}. */
      MethodDeclaration method(String name, Class type) {}
      /** Declares a private helper method returning {@code type}, whose parameters and body are the closure's. */
      MethodDeclaration method(String name, Class type, Closure body) {}

      /** Declares a nested configuration class holding the declarations in its body, conditioned as a whole. */
      GroupDeclaration group() {}
      /** Declares a nested configuration class holding the declarations in its body. */
      GroupDeclaration group(Closure body) {}
      /** Declares a nested configuration class named after {@code name}, conditioned as a whole. */
      GroupDeclaration group(String name) {}
      /** Declares a nested configuration class named after {@code name}, holding the declarations in its body. */
      GroupDeclaration group(String name, Closure body) {}

      static class BeanDeclaration {
        /** {@code @ConditionalOnMissingBean}: types positionally, the annotation's attributes by name. With no arguments the bean's own type is used. */
        BeanDeclaration conditionalOnMissingBean(Object... typesAndAttributes) {}
        /** {@code @ConditionalOnMissingBean} on this bean's own name; the annotation's other attributes by name. */
        BeanDeclaration conditionalOnMissingBeanName(Object... attributes) {}
        /** {@code @ConditionalOnBean}: types positionally, the annotation's attributes by name. */
        BeanDeclaration conditionalOnBean(Object... typesAndAttributes) {}
        /** {@code @ConditionalOnProperty}: property names positionally, the annotation's attributes ({@code havingValue}, {@code matchIfMissing}, ...) by name. */
        BeanDeclaration conditionalOnProperty(Object... namesAndAttributes) {}
        /** {@code @ConditionalOnExpression} with a SpEL expression; single-quote it so Groovy leaves its placeholders alone. */
        BeanDeclaration conditionalOnExpression(String expression) {}
        /** {@code @ConditionalOnExpression} with a SpEL expression; single-quote it so Groovy leaves its placeholders alone. */
        BeanDeclaration conditionalOnExpression(String expression, Closure body) {}
        /** {@code @ConditionalOnClass}: types and String class names positionally, the annotation's attributes by name. */
        BeanDeclaration conditionalOnClass(Object... typesNamesAndAttributes) {}
        /** Registers the bean only in the given Grails environments. */
        BeanDeclaration conditionalOnGrailsEnv(Object... environments) {}
        /** Additional names Spring resolves to the same bean. */
        BeanDeclaration aliases(Object... names) {}
        /** {@code @Primary} */
        BeanDeclaration primary() {}
        /** {@code @Primary} */
        BeanDeclaration primary(Closure body) {}
        /** {@code @Lazy} */
        BeanDeclaration lazy() {}
        /** {@code @Lazy} */
        BeanDeclaration lazy(Closure body) {}
        /** {@code @Scope}: the scope name positionally, the annotation's attributes ({@code proxyMode}, ...) by name. */
        BeanDeclaration scope(Object... nameAndAttributes) {}
        /** Makes the factory method static, as {@code BeanFactoryPostProcessor} and {@code BeanPostProcessor} beans require. */
        BeanDeclaration staticMethod() {}
        /** Makes the factory method static, as {@code BeanFactoryPostProcessor} and {@code BeanPostProcessor} beans require. */
        BeanDeclaration staticMethod(Closure body) {}
        /** Type arguments for the declared type, where the construction does not settle them. */
        BeanDeclaration typeArguments(Object... types) {}
        /** Attaches any annotation to the factory method, merging into one a qualifier already attached. */
        BeanDeclaration annotate(Class annotationType) {}
        /** Attaches any annotation to the factory method, merging into one a qualifier already attached. */
        BeanDeclaration annotate(Class annotationType, Closure body) {}
        /** Attaches any annotation to the factory method, with its attributes by name. */
        BeanDeclaration annotate(Map attributes, Class annotationType) {}
        /** Attaches any annotation to the factory method, with its attributes by name. */
        BeanDeclaration annotate(Map attributes, Class annotationType, Closure body) {}
      }

      static class FieldDeclaration {
        /** {@code @Value("${key}")}, or the expression verbatim when it already holds a {@code ${...}} placeholder or {@code #{...}} SpEL expression. */
        FieldDeclaration value(String keyOrExpression) {}
        /** {@code @Value("${key:defaultValue}")} */
        FieldDeclaration value(String key, Object defaultValue) {}
        /** Type arguments for the declared type. */
        FieldDeclaration typeArguments(Class... types) {}
        /** Attaches any annotation to the field. */
        FieldDeclaration annotate(Class annotationType) {}
        /** Attaches any annotation to the field, with its attributes by name. */
        FieldDeclaration annotate(Map attributes, Class annotationType) {}
      }

      static class MethodDeclaration {
        /** Type arguments for the declared return type. */
        MethodDeclaration typeArguments(Object... types) {}
        /** Attaches any annotation to the method. */
        MethodDeclaration annotate(Class annotationType) {}
        /** Attaches any annotation to the method. */
        MethodDeclaration annotate(Class annotationType, Closure body) {}
        /** Attaches any annotation to the method, with its attributes by name. */
        MethodDeclaration annotate(Map attributes, Class annotationType) {}
        /** Attaches any annotation to the method, with its attributes by name. */
        MethodDeclaration annotate(Map attributes, Class annotationType, Closure body) {}
      }

      static class GroupDeclaration {
        /** {@code @ConditionalOnMissingBean} on the group: types positionally, the annotation's attributes by name. */
        GroupDeclaration conditionalOnMissingBean(Object... typesAndAttributes) {}
        /** {@code @ConditionalOnBean} on the group: types positionally, the annotation's attributes by name. */
        GroupDeclaration conditionalOnBean(Object... typesAndAttributes) {}
        /** {@code @ConditionalOnProperty} on the group: property names positionally, the annotation's attributes by name. */
        GroupDeclaration conditionalOnProperty(Object... namesAndAttributes) {}
        /** {@code @ConditionalOnExpression} on the group. */
        GroupDeclaration conditionalOnExpression(String expression) {}
        /** {@code @ConditionalOnExpression} on the group. */
        GroupDeclaration conditionalOnExpression(String expression, Closure body) {}
        /** {@code @ConditionalOnClass} on the group: types and String class names positionally, the annotation's attributes by name. */
        GroupDeclaration conditionalOnClass(Object... typesNamesAndAttributes) {}
        /** Registers the group's beans only in the given Grails environments. */
        GroupDeclaration conditionalOnGrailsEnv(Object... environments) {}
        /** Attaches any annotation to the group's configuration class. */
        GroupDeclaration annotate(Class annotationType) {}
        /** Attaches any annotation to the group's configuration class. */
        GroupDeclaration annotate(Class annotationType, Closure body) {}
        /** Attaches any annotation to the group's configuration class, with its attributes by name. */
        GroupDeclaration annotate(Map attributes, Class annotationType) {}
        /** Attaches any annotation to the group's configuration class, with its attributes by name. */
        GroupDeclaration annotate(Map attributes, Class annotationType, Closure body) {}
      }
    }
    """;

  @Override
  public void processDynamicElements(@NotNull PsiType qualifierType,
                                     @Nullable PsiClass aClass,
                                     @NotNull PsiScopeProcessor processor,
                                     @NotNull PsiElement place,
                                     @NotNull ResolveState state) {
    if (!(place instanceof GrReferenceExpression ref) || ref.isQualified()) return;

    // Cheap rejection of everything not written inside a property named "beans"
    GrField field = PsiTreeUtil.getParentOfType(place, GrField.class);
    if (field == null || !GrailsBeansDsl.BEANS_PROPERTY.equals(field.getName())) return;

    GrClosableBlock closure = PsiTreeUtil.getParentOfType(place, GrClosableBlock.class);
    if (closure == null || !PsiTreeUtil.isAncestor(field, closure, true)) return;

    if (GrailsBeansDsl.isDeclarationContainer(closure)) {
      DynamicMemberUtils.process(processor, false, place, DSL_SOURCE);
      return;
    }

    for (GrClosableBlock c = closure; c != null; c = PsiTreeUtil.getParentOfType(c, GrClosableBlock.class, true)) {
      GrailsBeansDsl.Declaration declaration = GrailsBeansDsl.getDeclarationOfBody(c);
      if (declaration == null) continue;

      if (declaration.getKind() == GrailsBeansDsl.Kind.GROUP) return;
      if (declaration.getStatement().getParent() instanceof GrClosableBlock container) {
        processSharedMembers(container, processor, state);
      }
      return;
    }
  }

  private static void processSharedMembers(@NotNull GrClosableBlock container,
                                           @NotNull PsiScopeProcessor processor,
                                           @NotNull ResolveState state) {
    ElementClassHint classHint = processor.getHint(ElementClassHint.KEY);
    boolean processFields = ResolveUtil.shouldProcessProperties(classHint);
    boolean processMethods = ResolveUtil.shouldProcessMethods(classHint);
    String nameHint = ResolveUtil.getNameHint(processor);

    for (GrailsBeansDsl.Declaration declaration : GrailsBeansDsl.getDeclarations(container)) {
      GrailsBeansDsl.Kind kind = declaration.getKind();
      if (kind == GrailsBeansDsl.Kind.FIELD ? !processFields : kind != GrailsBeansDsl.Kind.METHOD || !processMethods) continue;

      String name = declaration.getName();
      if (name == null || (nameHint != null && !nameHint.equals(name))) continue;

      PsiElement member = kind == GrailsBeansDsl.Kind.FIELD ? createField(declaration, name) : createMethod(declaration, name);
      if (!processor.execute(member, state)) return;
    }
  }

  private static @NotNull PsiElement createField(@NotNull GrailsBeansDsl.Declaration declaration, @NotNull String name) {
    PsiElement navigationElement = declaration.getNavigationElement();
    PsiType type = declaration.getType();
    if (type == null) type = PsiType.getJavaLangObject(navigationElement.getManager(), navigationElement.getResolveScope());

    GrLightField field = new GrLightField(declaration.getHostClass(), name, type, navigationElement);
    field.setOriginInfo(MEMBER_ORIGIN_INFO);
    return field;
  }

  private static @NotNull PsiElement createMethod(@NotNull GrailsBeansDsl.Declaration declaration, @NotNull String name) {
    PsiElement navigationElement = declaration.getNavigationElement();
    GrLightMethodBuilder method = new GrLightMethodBuilder(navigationElement.getManager(), name);
    method.setNavigationElement(navigationElement);
    method.setContainingClass(declaration.getHostClass());
    method.setOriginInfo(MEMBER_ORIGIN_INFO);

    PsiType returnType = declaration.getType();
    if (returnType != null) method.setReturnType(returnType);

    GrClosableBlock body = declaration.getBody();
    if (body != null) {
      for (GrParameter parameter : body.getParameters()) {
        method.addParameter(parameter.getName(), parameter.getType());
      }
    }
    return method;
  }
}
