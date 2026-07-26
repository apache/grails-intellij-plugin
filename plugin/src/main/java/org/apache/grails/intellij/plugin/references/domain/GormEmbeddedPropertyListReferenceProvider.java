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

package org.apache.grails.intellij.plugin.references.domain;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTypesUtil;
import org.apache.grails.intellij.plugin.util.GrailsPsiUtil;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;

import java.util.Iterator;
import java.util.Map;

public class GormEmbeddedPropertyListReferenceProvider extends GormPropertiesListReferenceReferenceProvider {

  @Override
  protected void filterFields(PsiClass domainClass, Map<String, PsiType> fields) {
    PsiField transientField = domainClass.findFieldByName("transients", false);
    if (transientField instanceof GrField && transientField.hasModifierProperty(PsiModifier.STATIC)) {
      GrExpression initializer = ((GrField)transientField).getInitializerGroovy();
      if (initializer instanceof GrListOrMap) {
        GrailsPsiUtil.removeValuesFromList(fields.keySet(), (GrListOrMap)initializer);
      }
    }

    for (Iterator<Map.Entry<String, PsiType>> itr = fields.entrySet().iterator(); itr.hasNext(); ) {
      Map.Entry<String, PsiType> entry = itr.next();

      PsiType type = entry.getValue();
      if (type instanceof PsiPrimitiveType) {
        itr.remove();
        continue;
      }

      PsiClass resolve = PsiTypesUtil.getPsiClass(type);
      if (resolve != null) {
        String qname = resolve.getQualifiedName();
        if (qname != null && (qname.startsWith("java.lang.") || qname.startsWith("java.util."))) {
          itr.remove();
        }
      }
    }
  }
}
