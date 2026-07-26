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

package org.apache.grails.intellij.plugin.fileType;

import com.intellij.ide.highlighter.XmlLikeFileType;
import com.intellij.lang.LanguageParserDefinitions;
import com.intellij.lang.ParserDefinition;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.ultimate.PluginVerifier;
import com.intellij.xml.util.XmlUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.GrailsBundle;
import org.apache.grails.intellij.plugin.GroovyMvcIcons;
import org.apache.grails.intellij.plugin.lang.gsp.GspLanguage;
import org.apache.grails.intellij.plugin.lang.gsp.lexer.core.GspTokenTypes;
import org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.impl.directive.GspDirectiveAttributeValueImpl;
import org.jetbrains.plugins.groovy.GroovyEnabledFileType;

import javax.swing.Icon;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;

public final class GspFileType extends XmlLikeFileType implements GroovyEnabledFileType {

  public static final String GSP_EXTENSION = "gsp";
  public static final GspFileType GSP_FILE_TYPE = new GspFileType();

  static {
    PluginVerifier.verifyUltimatePlugin();
  }

  private GspFileType() {
    super(GspLanguage.INSTANCE);
  }

  @Override
  public @NotNull String getDefaultExtension() {
    return GSP_EXTENSION;
  }

  @Override
  public @NotNull String getDescription() {
    //noinspection DialogTitleCapitalization
    return GrailsBundle.message("filetype.gsp.description");
  }

  @Override
  public Icon getIcon() {
    return GroovyMvcIcons.Gsp_logo;
  }

  @Override
  public @NotNull String getName() {
    return "GSP";
  }

  @Override
  public boolean isJVMDebuggingSupported() {
    return true;
  }

  @Override
  public Charset extractCharsetFromFileContent(Project project, @Nullable VirtualFile file, @NotNull CharSequence content) {
    String name = XmlUtil.extractXmlEncodingFromProlog(content);
    Charset charset = CharsetToolkit.forName(name);
    if (charset != null) return charset;

    charset = extractCharset(content);
    if (charset != null) return charset;

    return StandardCharsets.UTF_8;
  }

  private static @Nullable Charset extractCharset(@NotNull CharSequence content) {
    final ParserDefinition definition = LanguageParserDefinitions.INSTANCE.forLanguage(GspLanguage.INSTANCE);
    if (definition == null) return null;

    Lexer lexer = definition.createLexer(null);
    lexer.start(content);

    IElementType tokenType;
    while ((tokenType = lexer.getTokenType()) != null) {
      if (tokenType == GspTokenTypes.GSP_DIRECTIVE) {
        Matcher matcher = GspDirectiveAttributeValueImpl.CHARSET_PATTERN.matcher(lexer.getTokenSequence());
        if (matcher.find()) {
          String name = matcher.group(1);
          Charset charset = CharsetToolkit.forName(name);
          if (charset != null) {
            return charset;
          }
        }
      }
      lexer.advance();
    }

    return null;
  }
}
