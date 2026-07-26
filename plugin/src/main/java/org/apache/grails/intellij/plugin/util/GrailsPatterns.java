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

package org.apache.grails.intellij.plugin.util;

import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.InitialPatternCondition;
import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PsiClassPattern;
import com.intellij.patterns.PsiFilePattern;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.patterns.XmlAttributeValuePattern;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.api.gtag.GspAttributeValue;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFileBase;

public final class GrailsPatterns {

  private GrailsPatterns() {

  }

  public static XmlAttributeValuePattern gspAttributeValue(ElementPattern<? extends XmlAttribute> attributePattern) {
    return gspAttributeValue().withParent(attributePattern);
  }

  public static PsiFilePattern.Capture<PsiFile> buildConfig() {
    return new PsiFilePattern.Capture<>(new InitialPatternCondition<>(PsiFile.class) {
      @Override
      public boolean accepts(@Nullable Object o, ProcessingContext context) {
        return o instanceof GroovyFileBase && GrailsUtils.isBuildConfigFile((PsiFile)o);
      }
    });
  }

  public static PsiClassPattern artifact(final @NotNull GrailsArtifact artifact) {
    return PsiJavaPatterns.psiClass().with(new PatternCondition<>("Grails artifact") {
      @Override
      public boolean accepts(@NotNull PsiClass aClass, ProcessingContext context) {
        return artifact.isInstance(aClass);
      }
    });
  }

  public static XmlAttributeValuePattern gspAttributeValue() {
    return new XmlAttributeValuePattern(CONDITION);
  }

  private static final InitialPatternCondition CONDITION = new InitialPatternCondition<>(GspAttributeValue.class) {
    @Override
    public boolean accepts(final @Nullable Object o, final ProcessingContext context) {
      return o instanceof GspAttributeValue;
    }
  };

}
