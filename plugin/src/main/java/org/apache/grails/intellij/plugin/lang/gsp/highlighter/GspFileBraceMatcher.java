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
package org.apache.grails.intellij.plugin.lang.gsp.highlighter;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.tree.IElementType;
import com.intellij.xml.impl.XmlBraceMatcher;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.fileType.GspFileType;
import org.apache.grails.intellij.plugin.lang.gsp.lexer.IGspElementType;
import org.apache.grails.intellij.plugin.lang.gsp.lexer.core.GspTokenTypes;

public final class GspFileBraceMatcher extends XmlBraceMatcher {
  private static final int GSP_TOKEN_GROUP = 3;

  @Override
  public int getBraceTokenGroupId(final @NotNull IElementType tokenType) {
    if (tokenType instanceof IGspElementType) {
      return GSP_TOKEN_GROUP;
    }
    return super.getBraceTokenGroupId(tokenType);
  }

  @Override
  public boolean areTagsCaseSensitive(final @NotNull FileType fileType, final int braceGroupId) {
    if (braceGroupId == GSP_TOKEN_GROUP) return true;
    return super.areTagsCaseSensitive(fileType, braceGroupId);
  }

  @Override
  public boolean isStrictTagMatching(final @NotNull FileType fileType, final int braceGroupId) {
    if (braceGroupId == GSP_TOKEN_GROUP) return true;
    return super.isStrictTagMatching(fileType, braceGroupId);
  }

  @Override
  protected boolean isWhitespace(final IElementType tokenType1) {
    return tokenType1 == GspTokenTypes.GSP_WHITE_SPACE || super.isWhitespace(tokenType1);
  }

  @Override
  protected boolean isFileTypeWithSingleHtmlTags(final FileType fileType) {
    return fileType == GspFileType.GSP_FILE_TYPE || super.isFileTypeWithSingleHtmlTags(fileType);
  }
}
