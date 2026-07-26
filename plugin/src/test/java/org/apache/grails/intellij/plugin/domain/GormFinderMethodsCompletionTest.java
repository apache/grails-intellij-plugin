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

package org.apache.grails.intellij.plugin.domain;

import com.intellij.psi.PsiFile;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

public class GormFinderMethodsCompletionTest extends GrailsTestCase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    addDomain("""
                
                class Ddd {
                  String aaa
                  String bbb
                }
                """);
  }

  @Override
  protected boolean useGrails14() {
    return true;
  }

  public void testCompletion1() throws Exception {
    configureBySimpleGroovyFile("Ddd.<caret>");
    checkCompletion("findBy", "findAllBy", "countBy", "findOrCreateBy", "findOrSaveBy");
  }

  public void testCompletion2() throws Exception {
    PsiFile file = addSimpleGroovyFile("Ddd.findAllBy<caret>");
    checkCompletionVariants(file, "findAllByAaa", "findAllByBbb", "findAllById", "findAllByVersion");
  }

  public void testCompletion22() throws Exception {
    PsiFile file = addSimpleGroovyFile("Ddd.findOrCreateBy<caret>");
    checkCompletionVariants(file, "findOrCreateByAaa", "findOrCreateByBbb", "findOrCreateById", "findOrCreateByVersion");
  }

  public void testCompletion4() throws Exception {
    configureBySimpleGroovyFile("Ddd.findAllByAaa<caret>");
    checkCompletion("findAllByAaa()", "findAllByAaaAnd", "findAllByAaaOr", "findAllByAaaInList", "findAllByAaaNotInList",
                    "findAllByAaaNotEqual");
    checkNonExistingCompletionVariants("findAllByAaaEqual", "findAllByAaaNotNotEqual");
  }

  public void testCompletion5() throws Exception {
    configureBySimpleGroovyFile("Ddd.findAllByAaaEqual<caret>");
    checkCompletion("findAllByAaaEqual()", "findAllByAaaEqualOrBbb", "findAllByAaaEqualAndBbb");
    checkNonExistingCompletionVariants("findAllByAaaEqualOr", "findAllByAaaEqualAnd");
  }

  public void testCompletion6() throws Exception {
    configureBySimpleGroovyFile("Ddd.countByAaaGreaterThanEqualsAndAaaLessThanEqualsAndId<caret>");
    checkCompletion("countByAaaGreaterThanEqualsAndAaaLessThanEqualsAndId()",
                    "countByAaaGreaterThanEqualsAndAaaLessThanEqualsAndIdNotEqual",
                    "countByAaaGreaterThanEqualsAndAaaLessThanEqualsAndIdInRange",
                    "countByAaaGreaterThanEqualsAndAaaLessThanEqualsAndIdAnd");
    checkNonExistingCompletionVariants("countByAaaGreaterThanEqualsAndAaaLessThanEqualsAndIdOr");
  }

  public void testCompletion7() throws Exception {
    PsiFile file = addSimpleGroovyFile("Ddd.findOrCreateByAaa<caret>");
    checkCompletionVariants(file, "findOrCreateByAaa", "findOrCreateByAaaAndBbb", "findOrCreateByAaaAndId", "findOrCreateByAaaAndVersion");
  }

  public void testCompletion8() throws Exception {
    PsiFile file = addSimpleGroovyFile("Ddd.findOrCreateByAaaEqual<caret>");
    checkCompletionVariants(file, "findOrCreateByAaaEqual", "findOrCreateByAaaEqualAndBbb", "findOrCreateByAaaEqualAndId",
                            "findOrCreateByAaaEqualAndVersion");
  }
}
