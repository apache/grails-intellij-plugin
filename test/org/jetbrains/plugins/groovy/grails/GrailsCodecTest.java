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

package org.jetbrains.plugins.groovy.grails;

import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.IdeaTestUtil;
import com.intellij.usageView.UsageInfo;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.function.Supplier;

public class GrailsCodecTest extends GrailsTestCase {
  // MmmCodec returns javax.swing types (JButton/JPanel), so this test needs a real JDK:
  // the minimal Mock JDK 11 omits javax.swing/java.awt, leaving getBackground()/getUI() unresolved.
  @Override
  protected @NotNull Supplier<Sdk> getTestJdk() {
    return () -> JavaSdk.getInstance().createJdk("TEST_JDK", IdeaTestUtil.requireRealJdkHome(), false);
  }

  public void testHighlighting() {
    myFixture.addFileToProject("grails-app/utils/ccc/MmmCodec.groovy", """
      
      package ccc
      
      import javax.swing.JButton
      import javax.swing.JPanel
      
      class MmmCodec {
      
       static encode = { str ->
        return new JButton();
       }
      
       public static decode(Object target) {
           if (target != null) {
               return new JPanel();
           }
           return null;
       }
      
      }
      """);

    myFixture.addFileToProject("grails-app/controllers/Foo.java", "public class Foo { static byte[] aaa; }");

    PsiFile file = addController("""
                                   
                                   class CccController {
                                   
                                    def index = {
                                     "a".encodeAsMmm().getBackground();
                                     [1,2,3].encodeAsMmm().getBackground();
                                     "a".decodeMmm().getUI();
                                     [1,2,3].decodeMmm().getUI();
                                   
                                     "a".encodeAsHTML().substring(1)
                                     [1,2,3].encodeAsHTML().substring(1)
                                     "a".encodeAsMD5Bytes().length
                                     [1,2,3].decodeMD5Bytes()
                                   
                                     Foo.aaa.encodeAsHTML().substring(1)
                                   
                                     CccController.encodeAsMD5()
                                    }
                                   
                                   }
                                   """);
    GrailsTestCase.checkResolve(file, "decodeMD5Bytes", "encodeAsMD5");
  }

  public void testDontEncodeStaticContextCompletion() {
    configureByController("""
                            
                            class CccController {
                              def index = {
                                CccController.<caret>
                              }
                            }
                            """);

    checkCompletion("log");
    checkNonExistingCompletionVariants("encodeAsMD5", "decodeMD5Bytes", "encodeAsHTML");
  }

  public void testDontEncodeNamespacePrefix() {
    PsiFile c = addController("""
                                
                                class CccController {
                                  def index = {
                                    g.encodeAsHtml()
                                    g.link().encodeAsMD5Bytes()
                                  }
                                }
                                """);

    GrailsTestCase.checkResolve(c, "encodeAsHtml");
  }

  public void testFindUsages() {
    addController("""
                    
                    class Ccc1Controller {
                      def index = {
                       render("aaa".encodeAsHTML())
                      }
                    }
                    """);
    PsiFile c = addController("""
                                
                                class Ccc2Controller {
                                  def index = {
                                   render("bbb".encodeAsHTML<caret>())
                                  }
                                }
                                """);

    Collection<UsageInfo> usages = myFixture.testFindUsages(getFilePath(c));
    assert usages.size() == 2;
  }
}
