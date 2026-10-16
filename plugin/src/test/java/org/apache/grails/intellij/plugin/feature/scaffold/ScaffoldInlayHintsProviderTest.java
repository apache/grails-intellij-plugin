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
package org.apache.grails.intellij.plugin.feature.scaffold;

import com.intellij.codeInsight.hints.declarative.CollapseState;
import com.intellij.codeInsight.hints.declarative.CollapsiblePresentationTreeBuilder;
import com.intellij.codeInsight.hints.declarative.AboveLineIndentedPosition;
import com.intellij.codeInsight.hints.declarative.HintFormat;
import com.intellij.codeInsight.hints.declarative.InlayActionData;
import com.intellij.codeInsight.hints.declarative.InlayHintsCollector;
import com.intellij.codeInsight.hints.declarative.InlayPayload;
import com.intellij.codeInsight.hints.declarative.InlayPosition;
import com.intellij.codeInsight.hints.declarative.InlayTreeSink;
import com.intellij.codeInsight.hints.declarative.InlineInlayPosition;
import com.intellij.codeInsight.hints.declarative.PresentationTreeBuilder;
import com.intellij.codeInsight.hints.declarative.PsiPointerInlayActionPayload;
import com.intellij.codeInsight.hints.declarative.SharedBypassCollector;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;

import java.util.ArrayList;
import java.util.List;

/**
 * The hints that show what {@code @Scaffold} added: the superclass after the class name, and the
 * inherited actions at the end of the header line, minus the ones the class declares itself.
 */
public class ScaffoldInlayHintsProviderTest extends GrailsTestCase {

  /** One entry per hint: its text, position and the elements its links navigate to. */
  private record Hint(@NotNull String text, @NotNull InlayPosition position, @NotNull List<PsiElement> targets) {
  }

  private static final class RecordingSink implements InlayTreeSink {
    final List<Hint> hints = new ArrayList<>();

    @Override
    public void addPresentation(@NotNull InlayPosition position,
                                @Nullable List<InlayPayload> payloads,
                                @Nullable String tooltip,
                                @NotNull HintFormat hintFormat,
                                @NotNull Function1<? super PresentationTreeBuilder, Unit> builder) {
      StringBuilder text = new StringBuilder();
      List<PsiElement> targets = new ArrayList<>();
      builder.invoke(new PresentationTreeBuilder() {
        @Override
        public void list(@NotNull Function1<? super PresentationTreeBuilder, Unit> block) {
          block.invoke(this);
        }

        @Override
        public void collapsibleList(@NotNull CollapseState state,
                                    @NotNull Function1<? super CollapsiblePresentationTreeBuilder, Unit> expanded,
                                    @NotNull Function1<? super CollapsiblePresentationTreeBuilder, Unit> collapsed) {
          throw new UnsupportedOperationException();
        }

        @Override
        public void text(@NotNull String fragment, @Nullable InlayActionData action) {
          text.append(fragment);
          if (action != null && action.getPayload() instanceof PsiPointerInlayActionPayload pointer) {
            targets.add(pointer.getPointer().getElement());
          }
        }

        @Override
        public void clickHandlerScope(@NotNull InlayActionData action, @NotNull Function1<? super PresentationTreeBuilder, Unit> block) {
          block.invoke(this);
        }
      });
      hints.add(new Hint(text.toString(), position, targets));
    }

    @Override
    public void whenOptionEnabled(@NotNull String option, @NotNull Function0<Unit> block) {
      block.invoke();
    }
  }

  private @NotNull List<Hint> hintsFor(@NotNull PsiFile file) {
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    InlayHintsCollector collector = new ScaffoldInlayHintsProvider().createCollector(myFixture.getFile(), myFixture.getEditor());
    assertNotNull(collector);
    RecordingSink sink = new RecordingSink();
    GrTypeDefinition clazz = ((GroovyFile)myFixture.getFile()).getTypeDefinitions()[0];
    ((SharedBypassCollector)collector).collectFromElement(clazz, sink);
    return sink.hints;
  }

  private void addScaffolding() {
    ScaffoldingStubs.addTo(myFixture);
    addDomain("""

package com.kousenit

class Quest {
  String name
}
""");
  }

  /** The body hints come in threes per method - blank line, comment, method - after the inline "extends" hint. */
  private static @NotNull List<String> bodyLines(@NotNull List<Hint> hints) {
    return every(hints, 3);
  }

  private static @NotNull List<String> commentLines(@NotNull List<Hint> hints) {
    return every(hints, 2);
  }

  private static @NotNull List<String> blankLines(@NotNull List<Hint> hints) {
    return every(hints, 1);
  }

  private static @NotNull List<String> every(@NotNull List<Hint> hints, int first) {
    List<String> lines = new ArrayList<>();
    for (int i = first; i < hints.size(); i += 3) lines.add(hints.get(i).text().strip());
    return lines;
  }

  public void testControllerShowsBaseAndInheritedActions() {
    addScaffolding();
    PsiFile controller = addController("""

package com.kousenit

import grails.plugin.scaffolding.annotation.Scaffold

@Scaffold(Quest)
class QuestController {
  def show() {}
}
""");

    List<Hint> hints = hintsFor(controller);
    GrTypeDefinition clazz = ((GroovyFile)myFixture.getFile()).getTypeDefinitions()[0];

    Hint base = hints.get(0);
    assertEquals(" extends RestfulController<Quest>", base.text());
    assertEquals(clazz.getNameIdentifierGroovy().getTextRange().getEndOffset(), ((InlineInlayPosition)base.position()).getOffset());
    assertEquals("grails.rest.RestfulController", ((com.intellij.psi.PsiClass)base.targets().get(0)).getQualifiedName());

    // show is declared in the class (and overrides); getResource, render and the static getter are not actions
    assertEquals(List.of("def index(Integer max)", "def create()", "def save()", "def edit()",
                         "def patch()", "def update()", "def delete()"),
                 bodyLines(hints));

    // Above every method, a comment naming the annotation that put it there, linking back to it,
    // and a blank line separating it from what precedes
    assertEquals(7, commentLines(hints).size());
    assertEquals("// @Scaffold(Quest) - inherited from RestfulController<Quest>", commentLines(hints).get(0));
    assertEquals("@Scaffold(Quest)", hints.get(2).targets().get(0).getText());
    assertEquals(List.of("", "", "", "", "", "", ""), blankLines(hints));

    // Blank/comment/method triples above the closing brace, in declaration order, indented like the class's own members
    int rBrace = clazz.getBody().getRBrace().getTextRange().getStartOffset();
    assertEquals(1 + 3 * 7, hints.size());
    for (int i = 1; i < hints.size(); i++) {
      AboveLineIndentedPosition position = (AboveLineIndentedPosition)hints.get(i).position();
      assertEquals(rBrace, position.getOffset());
      assertEquals(i - 1, position.getVerticalPriority());
      String expectedStart = switch (i % 3) {
        case 1 -> " ";
        case 2 -> "  // ";
        default -> "  def ";
      };
      assertTrue(hints.get(i).text(), hints.get(i).text().startsWith(expectedStart));
    }
    PsiMethod index = (PsiMethod)hints.get(3).targets().get(0);
    assertEquals("index", index.getName());
    assertEquals("grails.rest.RestfulController", index.getContainingClass().getQualifiedName());
  }

  /** A base that extends another scaffold base contributes the whole chain, once per name. */
  public void testBaseChainIsWalked() {
    addScaffolding();
    PsiFile controller = addController("""

package com.kousenit

import grails.plugin.scaffolding.RestfulServiceController
import grails.plugin.scaffolding.annotation.Scaffold

@Scaffold(RestfulServiceController<Quest>)
class QuestController {
}
""");

    List<Hint> hints = hintsFor(controller);
    assertEquals(" extends RestfulServiceController<Quest>", hints.get(0).text());
    assertEquals("// @Scaffold(RestfulServiceController<Quest>) - inherited from RestfulServiceController<Quest>",
                 commentLines(hints).get(0));
    assertEquals(List.of("def index(Integer max)", "def show()", "def create()", "def save()", "def edit()",
                         "def patch()", "def update()", "def delete()"),
                 bodyLines(hints));
  }

  /** The base's type parameter is shown as the domain, the way the scaffolded class sees it. */
  public void testServiceShowsGormServiceMembersWithTheDomainSubstituted() {
    addScaffolding();
    PsiFile service = addService("""

package com.kousenit

import grails.plugin.scaffolding.annotation.Scaffold

@Scaffold(Quest)
class CastleService {
}
""");

    List<Hint> hints = hintsFor(service);
    assertEquals(" extends GormService<Quest>", hints.get(0).text());
    // isReadOnly is a property accessor
    assertEquals(List.of("Quest get(Serializable id)", "List<Quest> list(Map args)", "Long count(Map args)",
                         "void delete(Serializable id)", "Quest save(Quest instance)"),
                 bodyLines(hints));
  }

  public void testPlainControllerHasNoHints() {
    addScaffolding();
    PsiFile controller = addController("""

package com.kousenit

class QuestController {
  def index() {}
}
""");

    assertEmpty(hintsFor(controller));
  }
}
