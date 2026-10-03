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
import com.intellij.testFramework.UsefulTestCase;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.jetbrains.plugins.groovy.codeInspection.untypedUnresolvedAccess.GrUnresolvedAccessInspection;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;

/**
 * Where queries ({@code Person.where { active == true }}) refer to the domain properties by their bare names. The
 * GORM 5+ {@code GormEntity} trait only gives the closure a raw {@code DetachedCriteria} delegate, so those names
 * are resolved by WhereQueryClosureMemberContributor. The GORM the other tests run against is older than that,
 * hence the stubs below.
 */
public class GormWhereQueryTest extends GrailsTestCase {
  private PsiFile myDomainFile;

  /** GormTraitContributor only picks GormEntity when {@code org.hibernate.Hibernate} is on the classpath. */
  @Override
  protected boolean needHibernate() {
    return true;
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    myFixture.addFileToProject("src/groovy/grails/gorm/DetachedCriteria.groovy", """
      package grails.gorm

      class DetachedCriteria<T> {
        DetachedCriteria(Class<T> targetClass) {}
        DetachedCriteria<T> build(@DelegatesTo(DetachedCriteria) Closure callable) { this }
        DetachedCriteria<T> where(@DelegatesTo(DetachedCriteria) Closure callable) { this }
        DetachedCriteria<T> eq(String propertyName, Object value) { this }
        List<T> list(Map args) { null }
      }
      """);

    // The Groovy the tests run against predates @CompileStatic; the Groovy plugin keys off the annotation name only.
    myFixture.addFileToProject("src/java/groovy/transform/CompileStatic.java", """
      package groovy.transform;

      public @interface CompileStatic {
      }
      """);

    // GormVersion.IS_5 is the lowest version GormTraitContributor injects the trait for.
    myFixture.addFileToProject("src/java/grails/gorm/annotation/Entity.java", """
      package grails.gorm.annotation;

      public @interface Entity {
      }
      """);

    // #CHECK# org.grails.datastore.gorm.GormEntity
    myFixture.addFileToProject("src/groovy/org/grails/datastore/gorm/GormEntity.groovy", """
      package org.grails.datastore.gorm

      import grails.gorm.DetachedCriteria

      trait GormEntity<D> {
        static DetachedCriteria<D> where(@DelegatesTo(DetachedCriteria) Closure callable) { null }
        static DetachedCriteria<D> whereAny(@DelegatesTo(DetachedCriteria) Closure callable) { null }
        static D find(@DelegatesTo(DetachedCriteria) Closure callable) { null }
        static List<D> findAll(@DelegatesTo(DetachedCriteria) Closure callable) { null }
      }
      """);

    myDomainFile = addDomain("""

                class Pessoa {
                  String nome
                  Integer idade
                  Boolean ativo = true

                  static transients = ['apelido']
                  String apelido

                  static constraints = {
                    nome blank: false, maxSize: 100
                  }

                  static List<Pessoa> adultos() {
                    where { idade >= 18 }.list([:])
                  }
                }
                """);

    myFixture.enableInspections(GrUnresolvedAccessInspection.class);
  }

  /** The reported case: under {@code @CompileStatic} an unresolved property is an error, not just a warning. */
  public void testCompileStaticWhereQueryHasNoErrors() {
    PsiFile file = myFixture.addFileToProject("src/groovy/PessoaService.groovy", """
      import grails.gorm.DetachedCriteria
      import groovy.transform.CompileStatic

      @CompileStatic
      class PessoaService {
        List<Pessoa> buscarAtivasPorIdadeWhere(Integer idadeMinima) {
          DetachedCriteria<Pessoa> query = Pessoa.where {
            ativo == true && idade >= idadeMinima
          }
          query.list(sort: 'nome', order: 'asc')
        }

        List<Pessoa> composta() {
          DetachedCriteria<Pessoa> query = Pessoa.whereAny { nome == 'a' || id == 1L }
          query.where { version == 0L }.list([:])
        }

        Pessoa primeira() {
          Pessoa.find { nome == 'Ana' }
        }

        List<Pessoa> todas() {
          Pessoa.findAll { idade < 10 }
        }

        // Proves the class really is type checked: an unknown name is still an error.
        def desconhecida() {
          Pessoa.where { <error descr="Cannot resolve symbol 'inexistente'">inexistente</error> == 1 }
        }
      }
      """);
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    myFixture.checkHighlighting(true, false, true);
  }

  public void testNavigateToDomainProperty() {
    assertNavigatesToField("""
      class PessoaService {
        def buscar() {
          Pessoa.where { ati<caret>vo == true }
        }
      }
      """, "ativo");
  }

  public void testNavigateInsideComposedQuery() {
    assertNavigatesToField("""
      class PessoaService {
        def buscar() {
          def query = Pessoa.where { ativo == true }
          query.where { ida<caret>de > 3 }
        }
      }
      """, "idade");
  }

  public void testNavigateInsideDomainClass() {
    myFixture.configureFromExistingVirtualFile(myDomainFile.getVirtualFile());
    myFixture.getEditor().getCaretModel().moveToOffset(myDomainFile.getText().indexOf("idade >= 18") + 1);

    PsiElement target = myFixture.getElementAtCaret();
    UsefulTestCase.assertInstanceOf(target, GrField.class);
    assertEquals("idade", ((GrField)target).getName());
  }

  /** The reference resolves through the getter, which must still be renamed together with the field. */
  public void testRenamePropertyFromWhereQuery() {
    PsiFile file = myFixture.addFileToProject("src/groovy/PessoaService.groovy", """
      class PessoaService {
        def buscar() {
          Pessoa.where { ati<caret>vo == true }
        }
      }
      """);
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());

    myFixture.renameElementAtCaret("habilitado");

    myFixture.checkResult("""
      class PessoaService {
        def buscar() {
          Pessoa.where { habilitado == true }
        }
      }
      """);
    assertTrue(myDomainFile.getText().contains("Boolean habilitado = true"));
  }

  /** Only persistent properties take part in a where query; transients and unknown names stay unresolved. */
  public void testTransientAndUnknownPropertiesAreNotResolved() {
    PsiFile file = myFixture.addFileToProject("src/groovy/PessoaService.groovy", """
      class PessoaService {
        def buscar() {
          Pessoa.where { <warning descr="Cannot resolve symbol 'apelido'">apelido</warning> == 'x' && <warning descr="Cannot resolve symbol 'inexistente'">inexistente</warning> == 1 }
        }
      }
      """);
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    myFixture.checkHighlighting(true, false, true);
  }

  /** The properties belong to the where closure itself, not to closures nested in it or to unrelated calls. */
  public void testPropertiesAreNotContributedOutsideWhereClosures() {
    PsiFile file = myFixture.addFileToProject("src/groovy/PessoaService.groovy", """
      class PessoaService {
        def buscar(List<String> nomes) {
          nomes.find { <warning descr="Cannot resolve symbol 'idade'">idade</warning> > 1 }
          Pessoa.where { nomes.each { <warning descr="Cannot resolve symbol 'ativo'">ativo</warning> } }
        }
      }
      """);
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    myFixture.checkHighlighting(true, false, true);
  }

  private void assertNavigatesToField(String serviceText, String fieldName) {
    PsiFile file = myFixture.addFileToProject("src/groovy/PessoaService.groovy", serviceText);
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());

    PsiElement target = myFixture.getElementAtCaret();
    UsefulTestCase.assertInstanceOf(target, GrField.class);
    GrField field = (GrField)target;
    assertEquals(fieldName, field.getName());
    assertEquals("Pessoa", field.getContainingClass().getName());
  }
}
