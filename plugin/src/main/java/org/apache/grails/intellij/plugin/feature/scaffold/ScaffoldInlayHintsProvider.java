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

import com.intellij.application.options.CodeStyle;
import com.intellij.codeInsight.hints.declarative.AboveLineIndentedPosition;
import com.intellij.codeInsight.hints.declarative.HintColorKind;
import com.intellij.codeInsight.hints.declarative.HintFontSize;
import com.intellij.codeInsight.hints.declarative.HintFormat;
import com.intellij.codeInsight.hints.declarative.HintMarginPadding;
import com.intellij.codeInsight.hints.declarative.InlayActionData;
import com.intellij.codeInsight.hints.declarative.InlayHintsCollector;
import com.intellij.codeInsight.hints.declarative.InlayHintsProvider;
import com.intellij.codeInsight.hints.declarative.InlayTreeSink;
import com.intellij.codeInsight.hints.declarative.InlineInlayPosition;
import com.intellij.codeInsight.hints.declarative.PsiPointerInlayActionNavigationHandler;
import com.intellij.codeInsight.hints.declarative.PsiPointerInlayActionPayload;
import com.intellij.codeInsight.hints.declarative.SharedBypassCollector;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbService;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.SmartPointerManager;
import com.intellij.util.text.CharArrayUtil;
import kotlin.Unit;
import org.apache.grails.intellij.plugin.GrailsBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinitionBody;

import java.util.List;

/**
 * Shows what {@code @Scaffold} adds to a controller or service, since none of it is in the source:
 * {@code extends RestfulController<Quest>} after the class name, and the inherited methods as greyed
 * lines at the bottom of the class body - {@code def index(Integer max)}, {@code def show()}, ... -
 * each a link to the method it inherits, under a comment that names the annotation responsible and
 * links back to it, with a blank line between methods. A method the class declares itself is left
 * out: it is in the source, with the platform's "overrides" marker in the gutter.
 *
 * @see Scaffolding#inheritedMembers
 * @see ScaffoldStructureViewExtension
 */
public final class ScaffoldInlayHintsProvider implements InlayHintsProvider {

  public static final String PROVIDER_ID = "grails.scaffold.members";

  private static final HintFormat FORMAT =
    new HintFormat(HintColorKind.TextWithoutBackground, HintFontSize.AsInEditor, HintMarginPadding.OnlyPadding);

  @Override
  public @Nullable InlayHintsCollector createCollector(@NotNull PsiFile file, @NotNull Editor editor) {
    if (!(file instanceof GroovyFile) || DumbService.isDumb(file.getProject())) return null;
    return new Collector();
  }

  private static final class Collector implements SharedBypassCollector {

    @Override
    public void collectFromElement(@NotNull PsiElement element, @NotNull InlayTreeSink sink) {
      if (!(element instanceof GrTypeDefinition clazz)) return;
      Scaffolding.ScaffoldInfo info = Scaffolding.resolve(clazz);
      if (info == null) return;

      PsiElement nameIdentifier = clazz.getNameIdentifierGroovy();
      String baseText = info.baseType().getPresentableText();
      sink.addPresentation(new InlineInlayPosition(nameIdentifier.getTextRange().getEndOffset(), true, 0),
                           null,
                           GrailsBundle.message("inlay.scaffold.base.tooltip"),
                           FORMAT,
                           builder -> {
                             builder.text(" extends ", null);
                             builder.text(baseText, navigateTo(info.baseClass()));
                             return Unit.INSTANCE;
                           });

      GrTypeDefinitionBody body = clazz.getBody();
      PsiElement rBrace = body == null ? null : body.getRBrace();
      if (rBrace == null) return;
      List<PsiMethod> members = Scaffolding.inheritedMembers(clazz, info);
      if (members.isEmpty()) return;

      // Above the closing brace, so the inherited methods follow the class's own, as in the hierarchy.
      // A block hint takes the indent of its anchor line - the brace's - so one level is added to
      // line the methods up with the class's own members.
      int anchor = rBrace.getTextRange().getStartOffset();
      String indent = indentUnit(clazz.getContainingFile());
      String tooltip = GrailsBundle.message("inlay.scaffold.members.tooltip", baseText);
      // A comment above each method names the annotation that put it there and links back to it.
      String comment = indent + "// " + GrailsBundle.message("inlay.scaffold.members.comment",
                                                             info.anchor().getText(), baseText);
      String commentTooltip = GrailsBundle.message("inlay.scaffold.members.comment.tooltip");
      InlayActionData annotationLink = navigateTo(info.anchor());
      for (int i = 0; i < members.size(); i++) {
        PsiMethod member = members.get(i);
        String signature = indent + Scaffolding.signature(member, info);
        InlayActionData link = navigateTo(member);
        // Higher vertical priority sits closer to the anchor line. Per method: a blank line, the
        // comment, the method - so each group reads like a method in the source.
        sink.addPresentation(new AboveLineIndentedPosition(anchor, 3 * i, 0), null, null, FORMAT, builder -> {
          builder.text(" ", null);
          return Unit.INSTANCE;
        });
        sink.addPresentation(new AboveLineIndentedPosition(anchor, 3 * i + 1, 0), null, commentTooltip, FORMAT, builder -> {
          builder.text(comment, annotationLink);
          return Unit.INSTANCE;
        });
        sink.addPresentation(new AboveLineIndentedPosition(anchor, 3 * i + 2, 0), null, tooltip, FORMAT, builder -> {
          builder.text(signature, link);
          return Unit.INSTANCE;
        });
      }
    }

    private static @NotNull String indentUnit(@NotNull PsiFile file) {
      Document document = file.getViewProvider().getDocument();
      if (document != null) {
        // Prefer what the file already does: the indent of its first indented line.
        CharSequence text = document.getCharsSequence();
        for (int line = 0; line < document.getLineCount(); line++) {
          int start = document.getLineStartOffset(line);
          int textStart = CharArrayUtil.shiftForward(text, start, " \t");
          if (textStart > start && textStart < document.getLineEndOffset(line)) {
            return text.subSequence(start, textStart).toString();
          }
        }
      }
      return CodeStyle.getIndentOptions(file).USE_TAB_CHARACTER ? "\t" : " ".repeat(CodeStyle.getIndentOptions(file).INDENT_SIZE);
    }

    private static @NotNull InlayActionData navigateTo(@NotNull PsiElement target) {
      return new InlayActionData(new PsiPointerInlayActionPayload(SmartPointerManager.createPointer(target)),
                                 PsiPointerInlayActionNavigationHandler.HANDLER_ID);
    }
  }
}
