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

package org.apache.grails.intellij.plugin.references.tagSupport;

import com.intellij.openapi.paths.PathReference;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.references.common.GspTagWrapper;
import org.apache.grails.intellij.plugin.references.common.WebAppFolderFileReferenceSet;

import java.util.Collection;
import java.util.Collections;

public class GspResourceContextPathAttributeSupport extends TagAttributeReferenceProvider {

  protected GspResourceContextPathAttributeSupport() {
    super("contextPath", "g", GspResourceDirAttributeSupport.TAGS);
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                         @NotNull String text,
                                                         int offset,
                                                         @NotNull GspTagWrapper gspTagWrapper) {
    String trimedUrl = PathReference.trimPath(text);

    if (trimedUrl.trim().isEmpty()) return PsiReference.EMPTY_ARRAY;

    final FileReferenceSet set = new WebAppFolderFileReferenceSet(trimedUrl, element, offset, null, true, true) {
      @Override
      public @NotNull Collection<PsiFileSystemItem> computeDefaultContexts() {
        if (!isAbsolutePathReference()) {
          return Collections.emptySet();
        }

        return super.computeDefaultContexts();
      }
    };

    return set.getAllReferences();
  }

}
