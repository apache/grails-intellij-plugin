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

package org.apache.grails.intellij.plugin.lang.gsp.psi.groovy.impl;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlToken;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.util.IncorrectOperationException;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.lang.gsp.psi.groovy.api.GspGroovyFile;
import org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.api.GspTag;
import org.apache.grails.intellij.plugin.util.GrailsPsiUtil;
import org.jetbrains.plugins.groovy.GroovyLanguage;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("SSBasedInspection")
public final class GspPageSkeleton {
  private IntArrayList defOffsets;
  private List<DefTagDescription> defTags;

  private final IntArrayList offsetsEach = new IntArrayList();
  private final List<TagDescription> eachTag = new ArrayList<>();

  GspPageSkeleton(GspGroovyFile gspGroovyFile) {
    processTag(gspGroovyFile.getGspLanguageRoot().getRootTag(), 0, null, Integer.MAX_VALUE);
  }

  public boolean processElementAtOffset(int offset, @NotNull PsiScopeProcessor processor,
                                     @Nullable String name, @NotNull ResolveState state) {
    // Process variables defined in each like tags.
    TagDescription tagDescription = getTagDescriptionByOffset(offset);

    for (; tagDescription != null; tagDescription = tagDescription.parent) {
      if (!tagDescription.process(processor, name, state)) return false;
    }

    // Process variables defined in g:set and g:def tags.
    if (defTags != null) {
      int index = IntArrays.binarySearch(defOffsets.elements(), 0, defOffsets.size(), offset);
      if (index < 0) {
        index = -(index + 1);
      }

      for (int i = index - 1; i >= 0; i--) {
        DefTagDescription description = defTags.get(i);
        if (offset >= description.scopeEnd) continue;
        if (name == null || name.equals(description.name)) {
          if (!processor.execute(description.getVariable(), state)) return false;
        }
      }
    }

    return true;
  }

  public @Nullable PsiVariable getVariableByDefTag(GspTag tag) {
    if (defTags == null) return null;

    for (DefTagDescription description : defTags) {
      if (description.tag == tag) {
        return description.getVariable();
      }
    }

    return null;
  }

  public @Nullable EachTagDescription getEachTagDescription(GspTag tag) {
    assert tag.getName().equals("g:each");

    for (TagDescription description : eachTag) {
      if (description != null && description.tag == tag) {
        return (EachTagDescription)description;
      }
    }

    return null;
  }

  /**
   * @param scopeEnd the end offset, exclusive, of the tag that encloses {@code tag}: the page compiles each tag body to a
   *                 block or closure, so a local variable {@code tag} declares is visible up to there
   */
  private void processTag(GspTag tag, int offset, TagDescription parentTagDescription, int scopeEnd) {
    String tagName = tag.getName();

    PsiElement e = tag.getFirstChild();

    TagDescription descr = parentTagDescription;

    if (tagName.equals("g:findAll") || tagName.equals("g:collect")) {
      descr = new NonEachTagDescription(tag, parentTagDescription);

      for (; e != null; e = e.getNextSibling()) {
        if (e instanceof XmlAttribute) {
          if (((XmlAttribute)e).getName().equals("expr")) {
            int attrStart = offset + e.getStartOffsetInParent();
            offsetsEach.add(attrStart);
            eachTag.add(descr);

            offsetsEach.add(attrStart + e.getTextLength() - 1);
            eachTag.add(parentTagDescription);
          }
        }
        else if (e instanceof XmlToken && ((XmlToken)e).getTokenType() == XmlTokenType.XML_TAG_END) {
          offsetsEach.add(offset + e.getStartOffsetInParent());
          eachTag.add(descr);
          break;
        }
      }
    }
    else {
      boolean isEach;
      if ((isEach = tagName.equals("g:each")) || tagName.equals("g:grep")) {
        descr = isEach ? new EachTagDescription(tag, parentTagDescription) : new NonEachTagDescription(tag, parentTagDescription);

        for (; e != null; e = e.getNextSibling()) {
          if (e instanceof XmlToken && ((XmlToken)e).getTokenType() == XmlTokenType.XML_TAG_END) {
            offsetsEach.add(offset + e.getStartOffsetInParent());
            eachTag.add(descr);
            break;
          }
        }
      }
    }

    for (; e != null; e = e.getNextSibling()) {
      if (e instanceof GspTag) {
        processTag((GspTag)e, offset + e.getStartOffsetInParent(), descr, offset + tag.getTextLength());
      }
    }

    if (descr != parentTagDescription) {
      offsetsEach.add(offset + tag.getTextLength() - 1);
      eachTag.add(parentTagDescription);
    }

    // g:set puts the variable in the page scope, so it is visible for the rest of the page. g:def compiles to a local
    // variable declaration (org.grails.gsp.compiler.tags.GroovyDefTag), which ends with the enclosing tag.
    boolean isDef = tagName.equals("g:def");
    if (isDef || tagName.equals("g:set")) {
      XmlAttributeValue varAttr = GrailsPsiUtil.getAttributeValue(tag, "var");
      if (varAttr != null) {
        if (GrailsPsiUtil.isSimpleAttribute(varAttr)) {
          String defName = varAttr.getValue().trim();
          if (StringUtil.isJavaIdentifier(defName)) {
            if (defTags == null) {
              defOffsets = new IntArrayList();
              defTags = new ArrayList<>();
            }

            defOffsets.add(offset + tag.getTextLength() - 1);
            defTags.add(new DefTagDescription(tag, defName, isDef, isDef ? scopeEnd : Integer.MAX_VALUE));
          }
        }
      }
    }
  }

  private @Nullable TagDescription getTagDescriptionByOffset(int offset) {
    int index = IntArrays.binarySearch(offsetsEach.elements(), 0, offsetsEach.size(), offset);
    if (index >= 0) {
      return eachTag.get(index);
    }

    index = -(index + 1) - 1;
    if (index == -1) return null;
    return eachTag.get(index);
  }

  /** A variable a {@code g:set} or {@code g:def} tag declares. */
  static class DefTagDescription {
    public final GspTag tag;
    public final String name;
    /** Whether the tag is {@code g:def}, which declares a local variable whose type the {@code type} attribute gives. */
    private final boolean isDef;
    /** The end offset, exclusive, up to which the variable is visible. */
    final int scopeEnd;
    private PsiVariable variable;

    DefTagDescription(GspTag tag, String name, boolean isDef, int scopeEnd) {
      this.tag = tag;
      this.name = name;
      this.isDef = isDef;
      this.scopeEnd = scopeEnd;
    }

    public PsiVariable getVariable() {
      if (variable == null) {
        XmlAttributeValue beanAttr = isDef ? null : GrailsPsiUtil.getAttributeValue(tag, "bean");

        // g:set stores the bean when it names one, and ignores the value then (ApplicationTagLib.set).
        PsiType type = isDef ? getDeclaredType() : beanAttr != null ? getBeanType(beanAttr) : null;

        if (type == null && beanAttr == null) {
          XmlAttributeValue valueAttr = GrailsPsiUtil.getAttributeValue(tag, "value");
          if (valueAttr != null) {
            type = GrailsPsiUtil.getAttributeExpressionType(valueAttr);
          }
        }

        if (type == null) {
          // g:set without a bean or a value stores its rendered body, a String. A bean, or the value g:def requires,
          // is of a type unknown here.
          type = isDef || beanAttr != null
                 ? TypesUtil.getJavaLangObject(tag)
                 : PsiType.getJavaLangString(tag.getManager(), tag.getResolveScope());
        }

        variable = new GrLightVariable(tag.getManager(), name, type, GrailsPsiUtil.getAttributeValue(tag, "var"));
      }

      return variable;
    }

    /**
     * The type g:def declares the variable with, or {@code null} when it declares none: GroovyDefTag writes
     * {@code def} when the {@code type} attribute is missing, and an untyped declaration takes the value's type.
     */
    private @Nullable PsiType getDeclaredType() {
      XmlAttributeValue typeAttr = GrailsPsiUtil.getAttributeValue(tag, "type");
      if (typeAttr == null || !GrailsPsiUtil.isSimpleAttribute(typeAttr)) return null;

      String typeName = typeAttr.getValue().trim();
      if (typeName.isEmpty() || typeName.equals("def")) return null;

      // Resolved from the page's Groovy file, so that the page's imports apply to simple names.
      PsiElement context = tag.getContainingFile().getViewProvider().getPsi(GroovyLanguage.INSTANCE);
      try {
        return GroovyPsiElementFactory.getInstance(tag.getProject()).createTypeElement(typeName, context).getType();
      }
      catch (IncorrectOperationException ignored) {
        return null;
      }
    }

    /**
     * The type of the bean g:set's {@code bean} attribute names, or {@code null} when it is unknown. The attribute is
     * either an expression giving the bean's class, for which Grails looks the bean up by type, or a bean name.
     */
    private @Nullable PsiType getBeanType(@NotNull XmlAttributeValue beanAttr) {
      if (!GrailsPsiUtil.isSimpleAttribute(beanAttr)) {
        PsiType type = GrailsPsiUtil.getAttributeExpressionType(beanAttr);
        if (type instanceof PsiClassType classType && CommonClassNames.JAVA_LANG_CLASS.equals(classType.rawType().getCanonicalText())) {
          PsiType[] parameters = classType.getParameters();
          if (parameters.length == 1 && parameters[0] instanceof PsiClassType beanType) return beanType;
        }
        return null;
      }

      List<String> candidates = KNOWN_BEAN_TYPES.get(beanAttr.getValue().trim());
      if (candidates == null) return null;

      JavaPsiFacade facade = JavaPsiFacade.getInstance(tag.getProject());
      for (String candidate : candidates) {
        PsiClass beanClass = facade.findClass(candidate, tag.getResolveScope());
        if (beanClass != null) return JavaPsiFacade.getElementFactory(tag.getProject()).createType(beanClass);
      }
      return null;
    }
  }

  /**
   * The beans every Grails application has that a page looks up by name, with the type each has in the newest API
   * first. Grails and Spring register them in code, so no bean model knows them. #CHECK#
   *
   * @see grails.core.GrailsApplication#APPLICATION_ID
   * @see grails.plugins.GrailsPluginManager#BEAN_NAME
   * @see org.springframework.web.context.WebApplicationContext#SERVLET_CONTEXT_BEAN_NAME
   */
  private static final Map<String, List<String>> KNOWN_BEAN_TYPES = Map.of(
    "grailsApplication", List.of("grails.core.GrailsApplication", "org.codehaus.groovy.grails.commons.GrailsApplication"),
    "pluginManager", List.of("grails.plugins.GrailsPluginManager", "org.codehaus.groovy.grails.plugins.GrailsPluginManager"),
    "servletContext", List.of("jakarta.servlet.ServletContext", "javax.servlet.ServletContext"));

  public abstract static class TagDescription {
    public final GspTag tag;
    public final TagDescription parent;

    protected TagDescription(GspTag tag, TagDescription parent) {
      this.tag = tag;
      this.parent = parent;
    }

    public abstract boolean process(@NotNull PsiScopeProcessor processor,
                        @Nullable String name,
                        @NotNull ResolveState state);
  }

  public static class NonEachTagDescription extends TagDescription {

    protected volatile boolean isVariableInit;
    protected PsiVariable variable;

    protected NonEachTagDescription(GspTag tag, TagDescription parent) {
      super(tag, parent);
    }

    @Override
    public boolean process(@NotNull PsiScopeProcessor processor,
                           @Nullable String name,
                           @NotNull ResolveState state) {
      if (name == null || name.equals("it")) {
        if (!isVariableInit) {
          XmlAttributeValue attrIn = GrailsPsiUtil.getAttributeValue(tag, "in");
          if (attrIn != null) {
            PsiType typeElement = null;

            PsiType typeCollection = GrailsPsiUtil.getAttributeExpressionType(attrIn);
            if (typeCollection != null) {
              typeElement = GrailsPsiUtil.getElementTypeByCollectionType(typeCollection, tag.getProject(), tag.getResolveScope());
            }

            if (typeElement == null) typeElement = TypesUtil.getJavaLangObject(tag);

            variable = new GrLightVariable(tag.getManager(), "it", typeElement, attrIn);
          }

          isVariableInit = true;
        }

        if (variable != null) {
          if (!processor.execute(variable, state)) return false;
        }
      }

      return true;
    }
  }

  public static class EachTagDescription extends TagDescription {

    private PsiVariable varVariable;
    private PsiVariable statusVariable;

    private volatile boolean isNameInit;

    private PsiElement varNavigationElement;
    private String varName;

    protected EachTagDescription(GspTag tag, TagDescription parent) {
      super(tag, parent);
    }

    private void ensureInit() {
      if (!isNameInit) {
        XmlAttributeValue attrVar = GrailsPsiUtil.getAttributeValue(tag, "var");
        if (attrVar == null) {
          varName = "it";
        }
        else {
          if (GrailsPsiUtil.isSimpleAttribute(attrVar)) {
            String nameVar = attrVar.getValue().trim();
            if (StringUtil.isJavaIdentifier(nameVar)) {
              this.varName = nameVar;
              varNavigationElement = attrVar;
            }
          }
        }

        XmlAttributeValue statusAttr = GrailsPsiUtil.getAttributeValue(tag, "status");
        if (statusAttr != null) {
          if (GrailsPsiUtil.isSimpleAttribute(statusAttr)) {
            String nameStatus = statusAttr.getValue().trim();
            if (StringUtil.isJavaIdentifier(nameStatus)) {
              statusVariable = new GrLightVariable(tag.getManager(), nameStatus, CommonClassNames.JAVA_LANG_INTEGER, statusAttr);
            }
          }
        }

        isNameInit = true;
      }
    }

    @Override
    public boolean process(@NotNull PsiScopeProcessor processor, @Nullable String name, @NotNull ResolveState state) {
      ensureInit();

      if (varName != null) {
        if (name == null || name.equals(varName)) {
          PsiVariable varVariable = getVarVariable();

          if (varVariable != null) {
            if (!processor.execute(varVariable, state)) return false;
          }
        }
      }

      PsiVariable statusVariable = getStatusVariable();
      if (statusVariable != null) {
        if (name == null || name.equals(statusVariable.getName())) {
          if (!processor.execute(statusVariable, state)) return false;
        }
      }

      return true;
    }

    public @Nullable PsiVariable getVarVariable() {
      ensureInit();

      if (varVariable != null) return varVariable;

      if (varName == null) return null;

      PsiType typeElement = null;
      XmlAttributeValue attrIn = GrailsPsiUtil.getAttributeValue(tag, "in");
      if (attrIn != null) {
        PsiType typeCollection = GrailsPsiUtil.getAttributeExpressionType(attrIn);
        if (typeCollection != null) {
          typeElement = GrailsPsiUtil.getElementTypeByCollectionType(typeCollection, tag.getProject(), tag.getResolveScope());
        }
      }

      if (typeElement == null) {
        typeElement = TypesUtil.getJavaLangObject(tag);
      }

      varVariable = new GrLightVariable(tag.getManager(), varName, typeElement,
                                      varNavigationElement != null ? varNavigationElement : (attrIn == null ? tag : attrIn));
      return varVariable;
    }

    public PsiVariable getStatusVariable() {
      ensureInit();
      return statusVariable;
    }
  }

}
