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

package org.apache.grails.intellij.plugin.references.domain;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.literals.GrLiteralImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GormPropertyReferenceUnique extends GormPropertyReference {

  private volatile List<String> myExistsVariants;

  public GormPropertyReferenceUnique(PsiElement element, boolean soft, PsiClass domainClass) {
    super(element, soft, domainClass);
  }

  @Override
  protected boolean isValidForCompletion(String fieldName, PsiType type, DomainDescriptor descriptor) {
    if (myExistsVariants == null) {
      PsiElement parent = getElement().getParent();
      if (parent instanceof GrListOrMap) {
        List<String> res = new ArrayList<>();

        for (GrExpression expression : ((GrListOrMap)parent).getInitializers()) {
          if (expression instanceof GrLiteralImpl) {
            Object value1 = ((GrLiteralImpl)expression).getValue();
            if (value1 instanceof String) {
              res.add((String)value1);
            }
          }
        }

        myExistsVariants = res;
      }
      else {
        myExistsVariants = Collections.emptyList();
      }
    }

    return !myExistsVariants.contains(fieldName);
  }
}
