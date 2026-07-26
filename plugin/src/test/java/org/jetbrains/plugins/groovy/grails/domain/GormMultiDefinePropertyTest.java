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

package org.jetbrains.plugins.groovy.grails.domain;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.plugins.groovy.grails.Grails14TestCase;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;

import java.util.List;

public class GormMultiDefinePropertyTest extends Grails14TestCase {
  public void testMultiDefineProperty() {
    addDomain("""
                
                class WcmContent {
                }
                """);

    GroovyFile file = (GroovyFile)addDomain("""
                               
                               class Ddd {
                                 static belongsTo = [foo: WcmContent]
                               //...
                                 static hasOne = [foo: WcmContent]
                               }
                               """);

    PsiClass c = file.getClasses()[0];
    List<PsiField> fields = List.of(c.getFields());
    assertEquals(1, ContainerUtil.count(fields, f -> f.getName().equals("foo")));
  }
}
