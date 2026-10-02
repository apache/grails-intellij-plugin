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

import com.intellij.openapi.module.Module;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.util.CachedValueProvider.Result;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.lexer.GroovyTokenTypes;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrBinaryExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrParenthesizedExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrAccessorMethod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The Grails 8 compile-time beans DSL: a {@code beans} closure property whose top-level statements are
 * {@code bean(...)}, {@code field(...)}, {@code method(...)} and {@code group(...)} declarations, compiled by
 * {@code grails.compiler.beans.GrailsBeans} into real {@code @Bean} factory methods.
 * <p>
 * The property is compiled on a class annotated with {@code @GrailsBeans}, and implicitly on a plugin descriptor
 * ({@code grails.plugins.Plugin}), on the application class ({@code grails.boot.config.GrailsAutoConfiguration})
 * and on a unit test ({@code org.grails.testing.GrailsUnitTest}). Nothing is recognised unless the
 * {@code GrailsBeans} annotation is on the class path, so pre-8 projects are unaffected.
 *
 * @see GrailsBeansDslMemberContributor
 */
public final class GrailsBeansDsl {

  public static final String GRAILS_BEANS_ANNOTATION = "grails.compiler.beans.GrailsBeans";
  public static final String BEANS_PROPERTY = "beans";

  private static final String PLUGIN_CLASS = "grails.plugins.Plugin";
  private static final String GRAILS_AUTO_CONFIGURATION = "grails.boot.config.GrailsAutoConfiguration";
  private static final String GRAILS_UNIT_TEST = "org.grails.testing.GrailsUnitTest";

  private static final String TYPE_ARGUMENTS_CALL = "typeArguments";
  private static final int MAX_CONSTANT_DEPTH = 16;

  public enum Kind {
    BEAN("bean"), FIELD("field"), METHOD("method"), GROUP("group");

    private final String myCallName;

    Kind(String callName) {
      myCallName = callName;
    }

    public String getCallName() {
      return myCallName;
    }

    static @Nullable Kind byCallName(@Nullable String callName) {
      for (Kind kind : values()) {
        if (kind.myCallName.equals(callName)) return kind;
      }
      return null;
    }
  }

  private GrailsBeansDsl() {
  }

  /**
   * Whether the beans DSL is available to code in the given context, i.e. the project builds against Grails 8+.
   */
  public static boolean isAvailable(@NotNull PsiElement context) {
    return JavaPsiFacade.getInstance(context.getProject()).findClass(GRAILS_BEANS_ANNOTATION, context.getResolveScope()) != null;
  }

  /**
   * Whether the {@code beans} property of the given class is compiled as the beans DSL.
   */
  public static boolean isBeansHost(@Nullable PsiClass aClass) {
    if (aClass == null) return false;
    return aClass.hasAnnotation(GRAILS_BEANS_ANNOTATION)
           || InheritanceUtil.isInheritor(aClass, PLUGIN_CLASS)
           || InheritanceUtil.isInheritor(aClass, GRAILS_AUTO_CONFIGURATION)
           || InheritanceUtil.isInheritor(aClass, GRAILS_UNIT_TEST);
  }

  /**
   * The {@code beans} closure declared on the given class, if it is compiled as the beans DSL.
   */
  public static @Nullable GrClosableBlock getBeansClosure(@Nullable PsiClass aClass) {
    if (!(aClass instanceof GrTypeDefinition)) return null;
    PsiField field = ((GrTypeDefinition)aClass).findCodeFieldByName(BEANS_PROPERTY, false);
    if (!(field instanceof GrField) || !(((GrField)field).getInitializerGroovy() instanceof GrClosableBlock closure)) return null;
    return isBeansClosure(closure) ? closure : null;
  }

  /**
   * Whether the closure is the initializer of a {@code beans} property compiled as the beans DSL.
   */
  public static boolean isBeansClosure(@NotNull GrClosableBlock closure) {
    if (!(closure.getParent() instanceof GrField field) || !BEANS_PROPERTY.equals(field.getName())) return false;
    return isBeansHost(field.getContainingClass()) && isAvailable(closure);
  }

  /**
   * Whether DSL declarations are written directly inside the closure: the {@code beans} closure itself, or the
   * body of one of its top-level {@code group(...)} declarations.
   */
  public static boolean isDeclarationContainer(@NotNull GrClosableBlock closure) {
    if (isBeansClosure(closure)) return true;
    Declaration declaration = getDeclarationOfBody(closure);
    return declaration != null && declaration.getKind() == Kind.GROUP;
  }

  /**
   * The declaration the closure is the body of, when it is the body of a {@code bean(...)}, {@code method(...)}
   * or {@code group(...)} declared directly in a {@code beans} closure or in one of its groups.
   */
  public static @Nullable Declaration getDeclarationOfBody(@NotNull GrClosableBlock closure) {
    PsiElement parent = closure.getParent();
    if (parent instanceof GrArgumentList) parent = parent.getParent();
    if (!(parent instanceof GrMethodCall call)) return null;

    GrMethodCall statement = getChainStatement(call);
    if (!(statement.getParent() instanceof GrClosableBlock container)) return null;

    for (Declaration declaration : getDeclarations(container)) {
      if (declaration.getStatement() == statement) {
        if (declaration.getKind() == Kind.FIELD) return null;
        // Groups do not nest
        boolean declared = declaration.getKind() == Kind.GROUP ? isBeansClosure(container) : isDeclarationContainer(container);
        return declared ? declaration : null;
      }
    }
    return null;
  }

  /**
   * The DSL declarations written directly inside the closure. The closure is not checked to be a declaration
   * container: see {@link #isDeclarationContainer(GrClosableBlock)}.
   */
  public static @NotNull List<Declaration> getDeclarations(@NotNull GrClosableBlock container) {
    return CachedValuesManager.getCachedValue(container, () -> Result.create(computeDeclarations(container), container));
  }

  private static @NotNull List<Declaration> computeDeclarations(@NotNull GrClosableBlock container) {
    List<Declaration> result = new ArrayList<>();
    for (PsiElement statement : container.getStatements()) {
      if (!(statement instanceof GrMethodCall call)) continue;
      GrMethodCall root = getChainRoot(call);
      if (root == null) continue;
      Kind kind = Kind.byCallName(getUnqualifiedCallName(root));
      if (kind != null) {
        result.add(new Declaration(kind, root, call));
      }
    }
    return Collections.unmodifiableList(result);
  }

  /**
   * The beans the class declares through the DSL, including those inside its groups.
   */
  public static @NotNull List<GrailsResourceBeanExtractor.BeanDescriptor> getBeanDescriptors(@NotNull PsiClass aClass) {
    GrClosableBlock beansClosure = getBeansClosure(aClass);
    if (beansClosure == null) return Collections.emptyList();

    List<GrailsResourceBeanExtractor.BeanDescriptor> result = new ArrayList<>();
    collectBeanDescriptors(beansClosure, result);
    return result;
  }

  private static void collectBeanDescriptors(@NotNull GrClosableBlock container, @NotNull List<GrailsResourceBeanExtractor.BeanDescriptor> result) {
    for (Declaration declaration : getDeclarations(container)) {
      if (declaration.getKind() == Kind.GROUP) {
        GrClosableBlock body = declaration.getBody();
        if (body != null) collectBeanDescriptors(body, result);
        continue;
      }
      if (declaration.getKind() != Kind.BEAN) continue;

      String name = declaration.getName();
      if (name == null || !(declaration.getTypeExpression() instanceof GrReferenceExpression typeReference)) continue;

      GrailsResourceBeanExtractor.BeanDescriptor descriptor = new GrailsResourceBeanExtractor.BeanDescriptor(name);
      descriptor.getReferences().add(typeReference);
      result.add(descriptor);
    }
  }

  /**
   * The classes in the module (and the modules it depends on) whose {@code beans} property is compiled as the DSL.
   */
  public static @NotNull Collection<PsiClass> findBeansHosts(@NotNull Module module) {
    JavaPsiFacade facade = JavaPsiFacade.getInstance(module.getProject());
    GlobalSearchScope librariesScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false);
    PsiClass annotation = facade.findClass(GRAILS_BEANS_ANNOTATION, librariesScope);
    if (annotation == null) return Collections.emptyList();

    GlobalSearchScope sourceScope = GlobalSearchScope.moduleWithDependenciesScope(module);
    Set<PsiClass> result = new LinkedHashSet<>(AnnotatedElementsSearch.searchPsiClasses(annotation, sourceScope).findAll());
    for (String superClassName : new String[]{GRAILS_AUTO_CONFIGURATION, PLUGIN_CLASS}) {
      PsiClass superClass = facade.findClass(superClassName, librariesScope);
      if (superClass != null) {
        result.addAll(ClassInheritorsSearch.search(superClass, sourceScope, true).findAll());
      }
    }
    return result;
  }

  /**
   * The outermost call of a qualifier chain such as {@code bean(Foo).primary().lazy()}, given any call in it.
   */
  private static @NotNull GrMethodCall getChainStatement(@NotNull GrMethodCall call) {
    GrMethodCall result = call;
    while (result.getParent() instanceof GrReferenceExpression ref
           && ref.getQualifierExpression() == result
           && ref.getParent() instanceof GrMethodCall outer
           && outer.getInvokedExpression() == ref) {
      result = outer;
    }
    return result;
  }

  /**
   * The unqualified call at the root of a qualifier chain such as {@code bean(Foo).primary().lazy()}.
   */
  private static @Nullable GrMethodCall getChainRoot(@NotNull GrMethodCall call) {
    GrMethodCall result = call;
    while (true) {
      if (!(result.getInvokedExpression() instanceof GrReferenceExpression ref)) return null;
      GrExpression qualifier = ref.getQualifierExpression();
      if (qualifier == null) return result;
      if (!(qualifier instanceof GrMethodCall qualifierCall)) return null;
      result = qualifierCall;
    }
  }

  private static @Nullable String getUnqualifiedCallName(@NotNull GrMethodCall call) {
    return call.getInvokedExpression() instanceof GrReferenceExpression ref && !ref.isQualified() ? ref.getReferenceName() : null;
  }

  private static boolean isTypeArgument(@NotNull GrExpression expression) {
    if (!(expression instanceof GrReferenceExpression ref)) return false;
    PsiElement resolved = ref.resolve();
    if (resolved != null) return resolved instanceof PsiClass;
    String name = ref.getReferenceName();
    return name != null && !name.isEmpty() && Character.isUpperCase(name.charAt(0)) && !name.equals(name.toUpperCase());
  }

  /**
   * Folds a compile-time String constant the way the transform does for a declared name: a literal, a reference to
   * a {@code static final} field initialised to one, or a concatenation of those.
   */
  static @Nullable String evaluateStringConstant(@Nullable GrExpression expression, int depth) {
    if (expression == null || depth > MAX_CONSTANT_DEPTH) return null;

    if (expression instanceof GrParenthesizedExpression parenthesized) {
      return evaluateStringConstant(parenthesized.getOperand(), depth + 1);
    }
    if (expression instanceof GrLiteral literal) {
      return literal.getValue() instanceof String value ? value : null;
    }
    if (expression instanceof GrBinaryExpression binary && binary.getOperationTokenType() == GroovyTokenTypes.mPLUS) {
      String left = evaluateStringConstant(binary.getLeftOperand(), depth + 1);
      String right = left == null ? null : evaluateStringConstant(binary.getRightOperand(), depth + 1);
      return right == null ? null : left + right;
    }
    if (!(expression instanceof GrReferenceExpression ref)) return null;

    PsiElement resolved = ref.resolve();
    // A Groovy property is reached through its getter from outside its class
    if (resolved instanceof GrAccessorMethod accessor) resolved = accessor.getProperty();
    if (!(resolved instanceof PsiField field)
        || !field.hasModifierProperty(PsiModifier.STATIC) || !field.hasModifierProperty(PsiModifier.FINAL)) {
      return null;
    }
    if (field instanceof GrField grField) {
      return evaluateStringConstant(grField.getInitializerGroovy(), depth + 1);
    }
    return field.computeConstantValue() instanceof String value ? value : null;
  }

  /**
   * The bean name Grails derives from a type, following {@code java.beans.Introspector#decapitalize}.
   */
  static @NotNull String decapitalize(@NotNull String name) {
    if (name.isEmpty() || (name.length() > 1 && Character.isUpperCase(name.charAt(0)) && Character.isUpperCase(name.charAt(1)))) {
      return name;
    }
    return Character.toLowerCase(name.charAt(0)) + name.substring(1);
  }

  /**
   * One top-level {@code bean}/{@code field}/{@code method}/{@code group} statement, with its chained qualifiers.
   */
  public static final class Declaration {
    private final Kind myKind;
    private final GrMethodCall myRoot;
    private final GrMethodCall myStatement;

    private Declaration(@NotNull Kind kind, @NotNull GrMethodCall root, @NotNull GrMethodCall statement) {
      myKind = kind;
      myRoot = root;
      myStatement = statement;
    }

    public @NotNull Kind getKind() {
      return myKind;
    }

    /**
     * The whole statement, i.e. the last call of the qualifier chain.
     */
    public @NotNull GrMethodCall getStatement() {
      return myStatement;
    }

    public @Nullable GrExpression getNameExpression() {
      GrExpression[] arguments = myRoot.getExpressionArguments();
      if (myKind == Kind.GROUP) return arguments.length > 0 ? arguments[0] : null;
      return arguments.length > 0 && !isTypeArgument(arguments[0]) ? arguments[0] : null;
    }

    public @Nullable GrExpression getTypeExpression() {
      if (myKind == Kind.GROUP) return null;
      GrExpression[] arguments = myRoot.getExpressionArguments();
      int index = getNameExpression() == null ? 0 : 1;
      return arguments.length > index ? arguments[index] : null;
    }

    /**
     * The element to navigate to for the declared member: its name when given, else its type.
     */
    public @NotNull PsiElement getNavigationElement() {
      GrExpression name = getNameExpression();
      if (name != null) return name;
      GrExpression type = getTypeExpression();
      return type != null ? type : myRoot;
    }

    /**
     * The declared name: a String constant when given, else the decapitalized simple name of the declared type.
     */
    public @Nullable String getName() {
      GrExpression nameExpression = getNameExpression();
      if (nameExpression != null) {
        return evaluateStringConstant(nameExpression, 0);
      }
      if (myKind != Kind.GROUP && getTypeExpression() instanceof GrReferenceExpression typeReference) {
        String typeName = typeReference.getReferenceName();
        return typeName == null ? null : decapitalize(typeName);
      }
      return null;
    }

    /**
     * The declared type, carrying any {@code .typeArguments(...)} chained onto the declaration.
     */
    public @Nullable PsiType getType() {
      if (!(getTypeExpression() instanceof GrReferenceExpression typeReference)
          || !(typeReference.resolve() instanceof PsiClass typeClass)) {
        return null;
      }

      JavaPsiFacade facade = JavaPsiFacade.getInstance(typeClass.getProject());
      List<PsiType> typeArguments = getTypeArguments();
      if (typeArguments.size() == typeClass.getTypeParameters().length && !typeArguments.isEmpty()) {
        return facade.getElementFactory().createType(typeClass, typeArguments.toArray(PsiType.EMPTY_ARRAY));
      }
      return facade.getElementFactory().createType(typeClass);
    }

    private @NotNull List<PsiType> getTypeArguments() {
      for (GrMethodCall call = myStatement; call != myRoot; ) {
        if (!(call.getInvokedExpression() instanceof GrReferenceExpression ref)) break;
        if (TYPE_ARGUMENTS_CALL.equals(ref.getReferenceName())) {
          List<PsiType> result = new ArrayList<>();
          for (GrExpression argument : call.getExpressionArguments()) {
            if (!(argument instanceof GrReferenceExpression argumentReference)
                || !(argumentReference.resolve() instanceof PsiClass argumentClass)) {
              return Collections.emptyList();
            }
            PsiClassType argumentType = JavaPsiFacade.getElementFactory(argumentClass.getProject()).createType(argumentClass);
            result.add(argumentType);
          }
          return result;
        }
        if (!(ref.getQualifierExpression() instanceof GrMethodCall qualifier)) break;
        call = qualifier;
      }
      return Collections.emptyList();
    }

    /**
     * The closure the declaration's body is written in, wherever in the qualifier chain it is passed.
     */
    public @Nullable GrClosableBlock getBody() {
      for (GrMethodCall call = myStatement; ; ) {
        GrClosableBlock[] closures = call.getClosureArguments();
        if (closures.length > 0) return closures[closures.length - 1];
        for (GrExpression argument : call.getExpressionArguments()) {
          if (argument instanceof GrClosableBlock closure) return closure;
        }
        if (call == myRoot
            || !(call.getInvokedExpression() instanceof GrReferenceExpression ref)
            || !(ref.getQualifierExpression() instanceof GrMethodCall qualifier)) {
          return null;
        }
        call = qualifier;
      }
    }

    /**
     * The class the {@code beans} property is declared on.
     */
    public @Nullable PsiClass getHostClass() {
      return PsiTreeUtil.getParentOfType(myRoot, PsiClass.class);
    }
  }
}
