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

package org.apache.grails.intellij.plugin.tagSupport;

import com.intellij.psi.PsiFile;
import junit.framework.TestCase;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

public class GspFieldTagTest extends GrailsTestCase {
  public void testRecursivePathRename1() {
    PsiFile javaFile = myFixture.addFileToProject("Jjj.java", """
      public class Jjj {
        public Jjj getJjj<caret>() {
          return this;
        }
        public boolean isZzz() {
           return true;
        }
      }
      """);

    PsiFile gsp = myFixture.addFileToProject("a.gsp", """
      <g:fieldValue bean="${new Jjj()}" field="jjj.jjj.jjj.jjj.zzz"/>
      <g:fieldError bean="${new Jjj().getJjj()}" field="jjj.jjj.zzz"/>
      
      ${g.fieldValue(bean: new Jjj(), field: 'jjj.jjj.jjj.jjj.zzz') }
      ${g.fieldValue([bean: new Jjj(), field: 'jjj.jjj.zzz']) }
      """);
    myFixture.configureFromExistingVirtualFile(javaFile.getVirtualFile());

    myFixture.renameElementAtCaret("getA");

    TestCase.assertEquals("""
                            <g:fieldValue bean="${new Jjj()}" field="a.a.a.a.zzz"/>
                            <g:fieldError bean="${new Jjj().getA()}" field="a.a.zzz"/>
                            
                            ${g.fieldValue(bean: new Jjj(), field: 'a.a.a.a.zzz') }
                            ${g.fieldValue([bean: new Jjj(), field: 'a.a.zzz']) }
                            """, gsp.getText());
  }

  public void testRecursivePathRename2() {
    PsiFile javaFile = myFixture.addFileToProject("Jjj.java", """
      public class Jjj {
        public Jjj getJjj<caret>() {
          return this;
        }
        public boolean isZzz() {
           return true;
        }
      }
      """);

    PsiFile gsp = myFixture.addFileToProject("a.gsp", """
      <g:fieldValue bean="${new Jjj()}" field="jjj.jjj.jjj.jjj.zzz"/>
      <g:fieldError bean="${new Jjj().getJjj()}" field="jjj.jjj.zzz"/>
      
      ${g.fieldValue(bean: new Jjj(), field: 'jjj.jjj.jjj.jjj.zzz') }
      ${g.fieldValue([bean: new Jjj(), field: 'jjj.jjj.zzz']) }
      """);
    myFixture.configureFromExistingVirtualFile(javaFile.getVirtualFile());

    myFixture.renameElementAtCaret("getJjj777");

    TestCase.assertEquals("""
                            <g:fieldValue bean="${new Jjj()}" field="jjj777.jjj777.jjj777.jjj777.zzz"/>
                            <g:fieldError bean="${new Jjj().getJjj777()}" field="jjj777.jjj777.zzz"/>
                            
                            ${g.fieldValue(bean: new Jjj(), field: 'jjj777.jjj777.jjj777.jjj777.zzz') }
                            ${g.fieldValue([bean: new Jjj(), field: 'jjj777.jjj777.zzz']) }
                            """, gsp.getText());
  }

  public void testRenameBooleanProperty() {
    PsiFile javaFile = myFixture.configureByText("Jjj.java", """
      public class Jjj {
        public Jjj getJjj() {
          return this;
        }
        public boolean isZzz<caret>() {
           return true;
        }
      }
      """);
    PsiFile gsp = myFixture.addFileToProject("a.gsp", """
      <g:fieldValue bean='${new Jjj()}' field='jjj.zzz'>
      ${g.fieldValue(bean: new Jjj(), field: 'jjj.zzz') }
      ${g.fieldValue(bean: new Jjj(), field: ""\"jjj.zzz""\") }
      ${g.fieldValue000(bean: new Jjj(), field: 'jjj.zzz') }
      """);

    myFixture.renameElementAtCaret("isZ");

    TestCase.assertEquals("""
                            <g:fieldValue bean='${new Jjj()}' field='jjj.z'>
                            ${g.fieldValue(bean: new Jjj(), field: 'jjj.z') }
                            ${g.fieldValue(bean: new Jjj(), field: ""\"jjj.z""\") }
                            ${g.fieldValue000(bean: new Jjj(), field: 'jjj.zzz') }
                            """, gsp.getText());
    TestCase.assertEquals("""
                            public class Jjj {
                              public Jjj getJjj() {
                                return this;
                              }
                              public boolean isZ() {
                                 return true;
                              }
                            }
                            """, javaFile.getText());
  }

  public void testMakeGetterNonGetter() {
    PsiFile javaFile = myFixture.configureByText("Jjj.java", """
      public class Jjj {
        public String getZzz<caret>() {
           return "";
        }
      }
      """);
    PsiFile gsp = myFixture.addFileToProject("a.gsp", "<g:fieldValue bean='${new Jjj()}' field='zzz'>");

    myFixture.renameElementAtCaret("isZzz");

    TestCase.assertEquals("<g:fieldValue bean='${new Jjj()}' field='zzz'>", gsp.getText());
    TestCase.assertEquals("""
                            public class Jjj {
                              public String isZzz() {
                                 return "";
                              }
                            }
                            """, javaFile.getText());
  }

  public void testRenameMethodProperty2() {
    PsiFile javaFile = myFixture.configureByText("Jjj.java", """
      public class Jjj {
        public Jjj getJjj<caret>() {
           return null;
        }
        public String getZzz() {
           return "";
        }
      }
      """);
    PsiFile gsp = myFixture.addFileToProject("a.gsp", """
      <g:fieldValue bean='${new Jjj()}' field='jjj.zzz'/>
      ${g.fieldValue([bean: new Jjj(), field: 'jjj.zzz'])}
      ${fieldValue bean: new Jjj(), field: 'jjj.zzz'}
      """);

    myFixture.renameElementAtCaret("getRrrrr");

    TestCase.assertEquals("""
                            <g:fieldValue bean='${new Jjj()}' field='rrrrr.zzz'/>
                            ${g.fieldValue([bean: new Jjj(), field: 'rrrrr.zzz'])}
                            ${fieldValue bean: new Jjj(), field: 'rrrrr.zzz'}
                            """, gsp.getText());
    TestCase.assertEquals("""
                            public class Jjj {
                              public Jjj getRrrrr() {
                                 return null;
                              }
                              public String getZzz() {
                                 return "";
                              }
                            }
                            """, javaFile.getText());
  }

  public void testRenameProperty1() {
    PsiFile groovyFile = myFixture.configureByText("Ggg.groovy", """
      public class Ggg {
        def zzz<caret>
      }
      """);
    PsiFile gsp = myFixture.addFileToProject("a.gsp", "<g:fieldValue bean='${new Ggg()}' field='zzz'>");

    myFixture.renameElementAtCaret("getVvv");

    TestCase.assertEquals("<g:fieldValue bean='${new Ggg()}' field='getVvv'>", gsp.getText());
    TestCase.assertEquals("""
                            public class Ggg {
                              def getVvv
                            }
                            """, groovyFile.getText());
  }

  public void testCompletion() {
    myFixture.addFileToProject("Ggg.groovy", """
      public class Ggg extends Parent {
        Ggg ggg
        boolean zzz
      
        private int notAProperty;
      
        private int getNotAProperty2() {
          return 5;
        }
      }
      """);
    myFixture.addFileToProject("Parent.groovy", """
      class Parent {
        def xxx = 45
      
        public int getXxx2() {
        }
      }
      """);

    myFixture.addFileToProject("a.gsp", "<g:fieldValue bean='${new Ggg()}' field='ggg.ggg.<caret>'>");
    myFixture.addFileToProject("b.gsp", "<g:fieldValue bean='${new Ggg()}' field='<caret>'>");
    myFixture.addFileToProject("c.gsp", "${g.fieldValue(bean: new Ggg(), field: '<caret>') }");
    myFixture.addFileToProject("d.gsp", "${g.fieldValue(bean: new Ggg(), field: \"\"\"ggg.ggg.<caret>\"\"\") }");

    myFixture.testCompletionVariants("a.gsp", "ggg", "metaClass", "xxx", "xxx2", "zzz");
    myFixture.testCompletionVariants("b.gsp", "ggg", "metaClass", "xxx", "xxx2", "zzz");
    myFixture.testCompletionVariants("c.gsp", "ggg", "metaClass", "xxx", "xxx2", "zzz");
    myFixture.testCompletionVariants("d.gsp", "ggg", "metaClass", "xxx", "xxx2", "zzz");
  }
}
