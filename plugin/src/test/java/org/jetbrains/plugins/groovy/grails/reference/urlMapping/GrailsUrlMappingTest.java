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

package org.jetbrains.plugins.groovy.grails.reference.urlMapping;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

import java.io.IOException;
import java.io.UncheckedIOException;

public class GrailsUrlMappingTest extends GrailsTestCase {
  @Override
  protected void configureModule(Module module, ModifiableRootModel model, ContentEntry contentEntry) {
    VirtualFile dir;
    try {
      dir = myFixture.getTempDirFixture().findOrCreateDir("grails-app/conf");
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    contentEntry.addSourceFolder(dir, false);
  }

  public void testViewAttribute() {
    myFixture.addFileToProject("grails-app/views/index.gsp", "");
    myFixture.addFileToProject("grails-app/views/ccc.gsp", "");

    PsiFile urlMapping = myFixture.addFileToProject("grails-app/conf/UrlMappings.groovy", """
      class UrlMappings {
      
        static mappings = {
          "500"(view:'/<caret>')
        }
      }
      """);
    checkCompletionVariants(urlMapping, "ccc", "index");

    PsiFile myUrlMapping = myFixture.addFileToProject("grails-app/conf/MyUrlMappings.groovy", """
      class MyUrlMappings {
      
        static mappings = {
          "500"(view:'/<caret>')
        }
      }
      """);
    checkCompletionVariants(myUrlMapping, "ccc", "index");
  }

  public void testViewWithoutSlash() {
    myFixture.addFileToProject("grails-app/views/index.gsp", "");
    myFixture.addFileToProject("grails-app/views/ccc.gsp", "");

    PsiFile urlMapping = myFixture.addFileToProject("grails-app/conf/UrlMappings.groovy", """
      class UrlMappings {
      
        static mappings = {
          "500"(view:'<caret>')
        }
      }
      """);
    checkCompletionVariants(urlMapping, "ccc", "index");
  }

  public void testNamedArgumentCompletion() {
    PsiFile urlMapping = myFixture.addFileToProject("grails-app/conf/UrlMappings.groovy", """
      class UrlMappings {
      
        static mappings = {
          "500"(view:'/', <caret>)
        }
      }
      """);
    checkCompletion(urlMapping, "controller", "action", "uri");
  }

  public void testGroup() {
    PsiFile urlMapping = myFixture.addFileToProject("grails-app/conf/UrlMappings.groovy", """
      class UrlMappings {
      
        static mappings = {
           group("/aaa") {
             "500"(view:'/', <caret>)
           }
        }
      }
      """);
    checkCompletion(urlMapping, "controller", "action", "uri");
  }

  public void testConstraints1() {
    PsiFile urlMapping = myFixture.addFileToProject("grails-app/conf/UrlMappings.groovy", """
      class UrlMappings {
      
        static mappings = {
          "/aaa/$id" {
                constraints {
                  id(<caret>)
                }
          }
        }
      }
      """);
    checkCompletion(urlMapping, "creditCard", "email");
  }

  public void testConstraints2() {
    PsiFile urlMapping = myFixture.addFileToProject("grails-app/conf/UrlMappings.groovy", """
      
      class UrlMappings {
      
        static mappings = {
          "/aaa/$id"({
                constraints ({
                  id(<caret>)
                })
          })
        }
      }
      """);
    checkCompletion(urlMapping, "creditCard", "email");
  }

  public void testConstraintsInNamedMapping1() {
    PsiFile urlMapping = myFixture.addFileToProject("grails-app/conf/UrlMappings.groovy", """
      
      class UrlMappings {
      
        static mappings = {
          name eeeeee: "/asdadasd/$id"() {
                constraints {
                  id(<caret>)
                }
          }
        }
      }
      """);
    checkCompletion(urlMapping, "creditCard", "email");
  }

  public void testConstraintsInNamedMapping2() {
    PsiFile urlMapping = myFixture.addFileToProject("grails-app/conf/UrlMappings.groovy", """
      
      class UrlMappings {
      
        static mappings = {
          name(eeeeee: "/asdadasd/$id"({
                constraints({
                  id(<caret>)
                })
          }))
        }
      }
      """);
    checkCompletion(urlMapping, "creditCard", "email");
  }

  public void testConstraintCompletion() {
    PsiFile urlMapping = myFixture.addFileToProject("grails-app/conf/UrlMappings.groovy", """
      
      class UrlMappings {
      
        static mappings = {
          name eeeeee: "/asdadasd/$id" {
             <caret>
          }
        }
      }
      """);
    checkCompletion(urlMapping, "constraints");
  }

  public void testValidatorParameterType() {
    PsiFile urlMapping = myFixture.addFileToProject("grails-app/conf/UrlMappings.groovy", """
      
      class UrlMappings {
        static mappings = {
          name eeeeee: "/asdadasd/$id" {
             constraints {
               id(validator: {val -> val.<caret>  })
             }
          }
        }
      }
      """);
    checkCompletion(urlMapping, "substring", "length", "charAt");
  }

  public void testConstraintExistsCompletion() {
    PsiFile urlMapping = myFixture.addFileToProject("grails-app/conf/UrlMappings.groovy", """
      
      class UrlMappings {
      
        static mappings = {
          name eeeeee: "/asdadasd/$id" {
             constraint<caret>
             constraints {
             }
          }
        }
      }
      """);

    myFixture.configureFromExistingVirtualFile(urlMapping.getVirtualFile());

    myFixture.completeBasic();
    assertFalse(myFixture.getLookupElementStrings().contains("constraints"));
  }

  @Override
  public boolean needUrlMappings() {
    return true;
  }
}
