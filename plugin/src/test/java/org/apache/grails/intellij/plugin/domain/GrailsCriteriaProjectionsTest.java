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

package org.apache.grails.intellij.plugin.domain;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiType;
import junit.framework.TestCase;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariableDeclaration;

import java.util.LinkedHashMap;
import java.util.Map;

public class GrailsCriteriaProjectionsTest extends GrailsTestCase {
  @Override
  protected boolean needGormLibrary() {
    return true;
  }

  private void addDomainClass() {
    addDomain("""
      class Ddd {
        String aaa
        String bbb
        long iii
    
        static namedQueries = {
          namedQuery1 {
            projections {
              rowCount()
            }
          }
          namedQuery2 {
            projections {
              rowCount()
              rowCount()
            }
          }
        }
    
      }
    """);
  }

  public void testProjection() {
    addDomainClass();

    LinkedHashMap<String, String> map = new LinkedHashMap<>(24);
    map.put("""
      def variable = (Ddd.createCriteria()) {
        projections {
          countDistinct("aaa")
        }
      }
    """, "List<Integer>");
    map.put("""
      def variable = (Ddd.createCriteria()) {
        projections {
          countDistinct("aaa")
          countDistinct("bbb")
        }
      }
    """, "List<Object[]>");
    map.put("def variable = Ddd.namedQuery1.list()", "List<Integer>");
    map.put("def variable = Ddd.namedQuery2.list()", "List<Object[]>");
    map.put("""
      def variable = (Ddd.createCriteria()) {
        projections {
          countDistinct("aaa")
          countDistinct("bbb")
        }
        projections {
          countDistinct("aaa")
        }
      }
    """, "List<Integer>");
    map.put("""
      def variable = (Ddd.createCriteria()) {
        projections {
          avg("iii")
        }
      }
    """, "List<Double>");
    map.put("""
      def variable = (Ddd.createCriteria()) {
        projections {
          max("iii")
          distinct("iii")
        }
      }
    """, "List<Object>");
    map.put("""
      def variable = (Ddd.createCriteria()) {
        projections {
          distinct("iii")
        }
      }
    """, "List<Long>");
    map.put("""
      Collection c =[]
      def variable = (Ddd.createCriteria()) {
        projections {
          distinct(c)
        }
      }
    """, "List<Object[]>");
    map.put("""
      def variable = (Ddd.createCriteria()) {
        projections {
          distinct(["iii", "aaa"])
        }
      }
    """, "List<Object[]>");
    map.put("""
      def variable = (Ddd.createCriteria()) {
        projections {
          if (1 > 2) {
            avg("iii")
          }
          else {
            countDistinct("aaa")
          }
        }
      }
    """, "List<Object>");
    map.put("""
      def variable = (Ddd.createCriteria()) {
        projections {
          groupProperty("iii")
        }
      }
    """, "List<Long>");
    map.put("""
      def variable = (Ddd.createCriteria()).list {
        projections {
          groupProperty("aaa")
        }
      }

    """, "List<String>");
    map.put("""
      def variable = (Ddd.createCriteria()).get {
        projections {
          max("iii")
        }
      }
    """, "Long");
    map.put("""
      def variable = Ddd.createCriteria().get {
        projections {
          max("iiiiiiii")
        }
      }

    """, "Object");
    map.put("""
      def variable = (Ddd.createCriteria()) {
        projections {
          max("aaa")
        }
      }
    """, "List<String>");
    map.put("""
      def variable = (Ddd.createCriteria()).list([aaa:'aaa']){
        projections {
          max("aaa")
        }
      }
    """, "List<String>");
    map.put("""
      def variable = (Ddd.createCriteria()).list([aaa:'aaa'],{
        projections {
          max("aaa")
        }
      })
    """, "List<String>");
    map.put("""
      def variable = (Ddd.createCriteria()).list(aaa:'aaa', {
        projections {
          max("aaa")
        }
      })
    """, "List<String>");
    map.put("""
      def variable = (Ddd.createCriteria()).list({
        projections {
          max("aaa")
        }
      }, aaa:'aaa')
    """, "List<String>");
    map.put("""
      def variable = Ddd.createCriteria().count {
        projections {
          max(abc)
          count("iii")
        }
      }
    """, "Integer");
    map.put("""
      def variable = (Ddd.createCriteria()) {
        projections {
          rowCount()
        }
      }
    """, "List<Integer>");
    map.put("""
            def variable = Ddd.withCriteria {
      projections {
        rowCount()
      }
    }
    """, "List<Integer>");
    map.put("""
      def variable = Ddd.withCriteria([:]){
        projections {
          min("aaa")
        }
      }
    """, "List<String>");
    int i = 0;
    for (Map.Entry<String, String> entry : map.entrySet()) {
      PsiFile file = myFixture.addFileToProject("src/groovy/Ggg" + i++ + ".groovy", entry.getKey());
      GrVariable variable = null;
      for (PsiElement e = file.getFirstChild(); e != null; e = e.getNextSibling()) {
        if (e instanceof GrVariableDeclaration) {
          variable = ((GrVariableDeclaration)e).getVariables()[0];
        }
      }
      PsiType type = variable.getTypeGroovy();
      TestCase.assertNotNull(entry.getKey(), type);
      TestCase.assertEquals(entry.getKey(), entry.getValue(), type.getPresentableText());
    }
  }
}
