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
package org.apache.grails.intellij.plugin.feature.scaffold;

import com.intellij.psi.CommonClassNames;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.util.PropertyUtilBase;
import com.intellij.psi.util.PsiFormatUtil;
import com.intellij.psi.util.PsiFormatUtilBase;
import com.intellij.psi.util.TypeConversionUtil;
import org.apache.grails.intellij.plugin.util.GrailsArtifact;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.GrModifierList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrExtendsClause;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;
import org.jetbrains.plugins.groovy.lang.psi.impl.GrAnnotationUtil;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrAccessorMethod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * What {@code @Scaffold} does to a Grails controller or service, resolved from the source the IDE sees.
 *
 * <p>The annotation generates no methods. Grails' {@code ScaffoldingControllerInjector} and
 * {@code ScaffoldingServiceInjector} replace the class's superclass at compile time - {@code Object}
 * becomes {@code RestfulController<Domain>} or {@code GormService<Domain>}, or the class named in the
 * annotation - and add a constructor that hands the domain class to it. The CRUD actions are therefore
 * inherited, not generated, and the IDE models them the same way: {@link ScaffoldTransformationSupport}
 * sets that superclass on the PSI, so completion, navigation, the "overrides" gutter marker and the
 * structure view's inherited members all come from the platform, and {@link ScaffoldInlayHintsProvider}
 * shows what the annotation added.
 *
 * <p>The annotation is read the way the injectors read it:
 * <ul>
 *   <li>{@code @Scaffold(Domain)} - domain {@code Domain}, default base for the artefact type;</li>
 *   <li>{@code @Scaffold(Base<Domain>)} - base {@code Base}, domain from the type argument;</li>
 *   <li>{@code @Scaffold(value = Base, domain = Domain)} - the documented explicit form;</li>
 *   <li>{@code @Scaffold(domain = Domain)} - default base.</li>
 * </ul>
 * A controller may also use the deprecated {@code static scaffold = Domain} property. A class that
 * declares its own superclass is left alone: the compiler rejects it, so there is nothing to model.
 */
public final class Scaffolding {

  public static final String SCAFFOLD_ANNOTATION = "grails.plugin.scaffolding.annotation.Scaffold";
  public static final String RESTFUL_CONTROLLER = "grails.rest.RestfulController";
  public static final String GORM_SERVICE = "grails.plugin.scaffolding.GormService";
  static final String LEGACY_SCAFFOLD_PROPERTY = "scaffold";

  private static final String GROOVY_GENERATED = "groovy.transform.Generated";
  private static final String GROOVY_INTERNAL = "groovy.transform.Internal";

  private Scaffolding() {
  }

  /**
   * The superclass {@code @Scaffold} gives a class.
   *
   * @param baseType    the base parameterised by the domain when it takes exactly one type parameter,
   *                    otherwise the raw base
   * @param domainClass {@code null} when the annotation names no domain, which the compiler reports
   * @param anchor      the annotation or the legacy {@code scaffold} property, for navigation
   */
  public record ScaffoldInfo(@NotNull GrailsArtifact artefact,
                             @NotNull PsiClass baseClass,
                             @NotNull PsiClassType baseType,
                             @Nullable PsiClass domainClass,
                             @NotNull PsiElement anchor) {
  }

  public static @Nullable ScaffoldInfo resolve(@Nullable PsiClass psiClass) {
    if (!(psiClass instanceof GrTypeDefinition clazz)) return null;
    if (clazz.isInterface() || clazz.isEnum() || clazz.isAnnotationType() || clazz.isTrait()) return null;

    GrailsArtifact artefact = GrailsArtifact.getType(clazz);
    String defaultBase;
    if (artefact == GrailsArtifact.CONTROLLER) {
      defaultBase = RESTFUL_CONTROLLER;
    }
    else if (artefact == GrailsArtifact.SERVICE) {
      defaultBase = GORM_SERVICE;
    }
    else {
      return null;
    }

    // "Scaffolded controllers cannot extend other classes" - a compile error, so there is nothing to model.
    GrExtendsClause extendsClause = clazz.getExtendsClause();
    if (extendsClause != null && extendsClause.getReferenceElementsGroovy().length > 0) return null;

    PsiClass base = null;
    PsiClass domain = null;
    PsiElement anchor;

    GrModifierList modifierList = clazz.getModifierList();
    PsiAnnotation annotation = modifierList == null ? null : modifierList.findAnnotation(SCAFFOLD_ANNOTATION);
    if (annotation != null) {
      anchor = annotation;
      ClassRef value = classRef(annotation.findDeclaredAttributeValue("value"));
      ClassRef explicitDomain = classRef(annotation.findDeclaredAttributeValue("domain"));
      if (explicitDomain != null) domain = explicitDomain.psiClass();
      if (value != null) {
        if (value.typeArgument() != null) {
          base = value.psiClass();
          if (domain == null) domain = value.typeArgument();
        }
        else if (domain != null) {
          base = value.psiClass();
        }
        else {
          domain = value.psiClass();
        }
      }
    }
    else if (artefact == GrailsArtifact.CONTROLLER) {
      PsiField field = clazz.findCodeFieldByName(LEGACY_SCAFFOLD_PROPERTY, false);
      if (!(field instanceof GrField scaffoldProperty) || !scaffoldProperty.hasModifierProperty(PsiModifier.STATIC)) return null;
      ClassRef ref = classRef(scaffoldProperty.getInitializerGroovy());
      if (ref == null) return null;
      domain = ref.psiClass();
      anchor = scaffoldProperty;
    }
    else {
      return null;
    }

    if (base == null) {
      base = JavaPsiFacade.getInstance(clazz.getProject()).findClass(defaultBase, clazz.getResolveScope());
      if (base == null) return null; // scaffolding is not on the classpath
    }

    PsiElementFactory factory = JavaPsiFacade.getElementFactory(clazz.getProject());
    PsiClassType baseType = domain != null && base.getTypeParameters().length == 1
                            ? factory.createType(base, factory.createType(domain))
                            : factory.createType(base);
    return new ScaffoldInfo(artefact, base, baseType, domain, anchor);
  }

  /**
   * The public methods a scaffolded class inherits from its scaffold base and does not declare itself:
   * the API {@code @Scaffold} adds. Walks the base's superclass chain but not its traits or interfaces,
   * and skips what a compiled Groovy class carries besides the actions - property accessors,
   * trait-implemented methods and Groovy's own plumbing, all of which it marks {@code @Generated}.
   */
  public static @NotNull List<PsiMethod> inheritedMembers(@NotNull GrTypeDefinition clazz, @NotNull ScaffoldInfo info) {
    Set<String> declared = new HashSet<>();
    for (GrMethod method : clazz.getCodeMethods()) declared.add(method.getName());
    for (GrField field : clazz.getCodeFields()) declared.add(field.getName()); // closure actions: def index = { }

    List<PsiMethod> result = new ArrayList<>();
    Set<String> seen = new HashSet<>(declared);
    for (PsiClass base = info.baseClass();
         base != null && !CommonClassNames.JAVA_LANG_OBJECT.equals(base.getQualifiedName());
         base = base.getSuperClass()) {
      for (PsiMethod method : base.getMethods()) {
        if (isScaffoldedMember(method) && seen.add(method.getName())) {
          result.add(method);
        }
      }
    }
    return result;
  }

  /**
   * The method as the scaffolded class sees it, Groovy style: {@code def index(Integer max)},
   * {@code Quest get(Serializable id)} - the base's type parameter already replaced by the domain.
   */
  public static @NotNull String signature(@NotNull PsiMethod method, @NotNull ScaffoldInfo info) {
    PsiClass declaring = method.getContainingClass();
    PsiSubstitutor substitutor = declaring == null
                                 ? PsiSubstitutor.EMPTY
                                 : TypeConversionUtil.getSuperClassSubstitutor(declaring, info.baseType());
    PsiType returnType = substitutor.substitute(method.getReturnType());
    String prefix = returnType == null || returnType.equalsToText(CommonClassNames.JAVA_LANG_OBJECT)
                    ? "def"
                    : returnType.getPresentableText();
    String parameters = PsiFormatUtil.formatMethod(method, substitutor,
                                                   PsiFormatUtilBase.SHOW_NAME | PsiFormatUtilBase.SHOW_PARAMETERS,
                                                   PsiFormatUtilBase.SHOW_TYPE | PsiFormatUtilBase.SHOW_NAME);
    return prefix + " " + parameters;
  }

  private static boolean isScaffoldedMember(@NotNull PsiMethod method) {
    if (method.isConstructor()) return false;
    if (!method.hasModifierProperty(PsiModifier.PUBLIC) || method.hasModifierProperty(PsiModifier.STATIC)) return false;
    if (method.getName().contains("$")) return false; // trait bridges and other Groovy internals
    if (method instanceof GrAccessorMethod || PropertyUtilBase.isSimplePropertyAccessor(method)) return false;
    PsiModifierList modifiers = method.getModifierList();
    return !modifiers.hasAnnotation(GROOVY_GENERATED) && !modifiers.hasAnnotation(GROOVY_INTERNAL);
  }

  /** A class named in an annotation attribute or a property initializer, with its first type argument. */
  private record ClassRef(@NotNull PsiClass psiClass, @Nullable PsiClass typeArgument) {
  }

  private static @Nullable ClassRef classRef(@Nullable PsiElement value) {
    if (!(value instanceof PsiAnnotationMemberValue memberValue)) return null;
    PsiClass psiClass = GrAnnotationUtil.getPsiClass(memberValue);
    if (psiClass == null || isVoid(psiClass)) return null;

    PsiClass typeArgument = null;
    if (value instanceof GrReferenceExpression reference) {
      PsiType[] typeArguments = reference.getTypeArguments();
      if (typeArguments.length > 0 && typeArguments[0] instanceof PsiClassType classType) {
        typeArgument = classType.resolve();
      }
    }
    return new ClassRef(psiClass, typeArgument);
  }

  private static boolean isVoid(@NotNull PsiClass psiClass) {
    return CommonClassNames.JAVA_LANG_VOID.equals(psiClass.getQualifiedName());
  }
}
