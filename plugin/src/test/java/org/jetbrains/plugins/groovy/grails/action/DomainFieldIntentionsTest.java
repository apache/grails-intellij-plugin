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
package org.jetbrains.plugins.groovy.grails.action;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.PostprocessReformattingAspect;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class DomainFieldIntentionsTest extends GrailsTestCase {


  public void testInnerClass() {
    PsiFile file = addDomain("""

class City {

  String name;

  private class InnerClass {
    String nam<caret>e;
  }
}
""");

    runIntention(file, "Make property nullable", false);
  }

  public void testTransientsField() {
    PsiFile file = addDomain("""

class City {

  String nam<caret>e;

  static transients = ["name"]
}
""");

    runIntention(file, "Make property nullable", false);
  }

  public void testConstraintsNotExists1() {
    PsiFile file = addDomain("""

class City {

    String nam<caret>e;
}
""");

    runIntention(file, "Make property nullable", true);

    checkResult("""

class City {

    String name;

    static constraints = {
        name(nullable: true)
    }
}
""");
  }

  public void testConstraintsNotExists2() {
    PsiFile file = addDomain("""

class City {

    String nam<caret>e;

}
""");

    runIntention(file, "Make property nullable", true);

    checkResult("""

class City {

    String name;

    static constraints = {
        name(nullable: true)
    }
}
""");
  }

  public void testConstraintsWithoutInitializer() {
    PsiFile file = addDomain("""

class City {

  String nam<caret>e;

  static constraints
}
""");

    runIntention(file, "Make property nullable", true);

    checkResult("""

class City {

  String name;

  static constraints = {
      name(nullable: true)
  }
}
""");
  }

  public void testConstraintsNull() {
    PsiFile file = addDomain("""

class City {

  String nam<caret>e;

  static constraints = null
}
""");

    runIntention(file, "Make property nullable", true);

    checkResult("""

class City {

  String name;

  static constraints = {
      name(nullable: true)
  }
}
""");
  }

  public void testConstraintsWithInvalidInitializer() {
    PsiFile file = addDomain("""

class City {

  String nam<caret>e;

  static constraints = "!!!!!!"
}
""");

    runIntention(file, "Make property nullable", false);
  }

  public void testConstraintsEmpty() {
    PsiFile file = addDomain("""

class City {

  String nam<caret>e;

  static constraints = { }
}
""");

    runIntention(file, "Make property nullable", true);

    checkResult("""

class City {

  String name;

  static constraints = {
      name(nullable: true)
  }
}
""");
  }

  private void checkResult(String text) {
    ApplicationManager.getApplication().runWriteAction(() -> {
      PostprocessReformattingAspect.getInstance(getProject()).doPostponedFormatting();
    });

    myFixture.checkResult(text);
  }

  public void testConstraintsHasFieldDescription() {
    PsiFile file = addDomain("""

class City {

  String nam<caret>e;

  static constraints = {
    name()
  }
}
""");

    runIntention(file, "Make property nullable", true);

    checkResult("""

class City {

  String name;

  static constraints = {
    name(nullable: true)
  }
}
""");
  }

  public void testNullableAlreadyExists() {
    PsiFile file = addDomain("""

class City {

  String nam<caret>e;

  static constraints = {
    name(nullable: true)
  }
}
""");

    runIntention(file, "Make property nullable", false);
  }

  public void testNonNullable() {
    PsiFile file = addDomain("""

class City {

  String nam<caret>e;

  static constraints = {
    name(nullable: false)
  }
}
""");

    runIntention(file, "Make property nullable", true);

    checkResult("""

class City {

  String name;

  static constraints = {
    name(nullable: true)
  }
}
""");
  }

  public void testInvalidValue() {
    PsiFile file = addDomain("""

class City {

  String nam<caret>e;

  static constraints = {
    name(nullable: 1 + 2)
  }
}
""");

    runIntention(file, "Make property nullable", false);
  }

  public void testMakeNullableOnPrimitiveField() {
    PsiFile file = addDomain("""

class City {

  int siz<caret>e;

}
""");

    runIntention(file, "Make property nullable", false);
  }

  public void testAppStatement1() {
    PsiFile file = addDomain("""

class City {

  String nam<caret>e;

  static constraints = {
    name nullable: false
  }
}
""");

    runIntention(file, "Make property nullable", true);

    checkResult("""

class City {

  String name;

  static constraints = {
    name nullable: true
  }
}
""");
  }

  public void testAppStatement2() {
    PsiFile file = addDomain("""

class City {

  String nam<caret>e;

  static constraints = {
    name asdasdasd: 4
  }
}
""");

    runIntention(file, "Make property nullable", true);

    checkResult("""

class City {

  String name;

  static constraints = {
    name nullable: true, asdasdasd: 4
  }
}
""");
  }

  public void testCreation1() {
    PsiFile file = addDomain("""

class City {

  String nam<caret>e;

  static constraints = {
    name(nullable:, size: 4)
  }
}
""");

    runIntention(file, "Make property nullable", true);

    checkResult("""

class City {

  String name;

  static constraints = {
    name(nullable: true, size: 4)
  }
}
""");
  }

  public void testCreation2() {
    PsiFile file = addDomain("""

class City {

  String nam<caret>e;

  static constraints = {
    name(nullable: false, size: 4)
  }
}
""");

    runIntention(file, "Make property nullable", true);

    checkResult("""

class City {

  String name;

  static constraints = {
    name(nullable: true, size: 4)
  }
}
""");
  }

  public void testCreation3() {
    PsiFile file = addDomain("""

class City {

  String nam<caret>e;

  static constraints = {
    name(size: 4, nullable: )
  }
}
""");

    runIntention(file, "Make property nullable", true);

    checkResult("""

class City {

  String name;

  static constraints = {
    name(size: 4, nullable: true)
  }
}
""");
  }

  public void testCreation4() {
    PsiFile file = addDomain("""

class City {

  String nam<caret>e;
  String description;

  static constraints = {
      description(nullable: false)
  }
}
""");

    runIntention(file, "Make property nullable", true);

    checkResult("""

class City {

  String name;
  String description;

  static constraints = {
      description(nullable: false)
      name(nullable: true)
  }
}
""");
  }

  public void testMakeUnique() {
    PsiFile file = addDomain("""

class City {

  String nam<caret>e;

  static constraints = {
    name()
  }
}
""");

    runIntention(file, "Make property unique", true);

    checkResult("""

class City {

  String name;

  static constraints = {
    name(unique: true)
  }
}
""");
  }

  public void testMakeUniqueInCommand() {
    PsiFile file = addController("""

class CccController {
  def index = { ZzzCommand com ->
  }
}

class ZzzCommand {

  String nam<caret>e;

  static constraints = {
    name()
  }
}
""");

    runIntention(file, "Make property unique", false);
  }

  public void testMakeNullableInCommand() {
    PsiFile file = addController("""

class CccController {
  def index = { ZzzCommand com ->
  }
}

class ZzzCommand {

  String nam<caret>e;

  static constraints = {
    name()
  }
}
""");

    runIntention(file, "Make property nullable", true);

    checkResult("""

class CccController {
  def index = { ZzzCommand com ->
  }
}

class ZzzCommand {

  String name;

  static constraints = {
    name(nullable: true)
  }
}
""");
  }

  public void testUniqueIsNotApplicable() {
    PsiFile file = addDomain("""

class City {

  Set stree<caret>t;

}
""");

    runIntention(file, "Make property unique", false);
  }

}
