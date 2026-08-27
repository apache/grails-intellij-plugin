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

package org.apache.grails.intellij.plugin.lang.gsp.completion;

import com.intellij.lang.Language;
import com.intellij.lang.html.HTMLLanguage;
import com.intellij.lang.xhtml.XHTMLLanguage;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlFileNSInfoProvider;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.xml.util.XmlUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.fileType.GspFileType;
import org.apache.grails.intellij.plugin.lang.gsp.GspFileViewProvider;
import org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.api.GspFile;
import org.apache.grails.intellij.plugin.lang.gsp.resolve.taglib.GspTagLibUtil;

public final class GspXmlFileNSInfoProvider implements XmlFileNSInfoProvider, Function<String, String[]> {

  private static final String[][] HTML_PREFIXES = new String[][]{new String[]{"", XmlUtil.XHTML_URI}};

  @Override
  public String[] @Nullable [] getDefaultNamespaces(@NotNull XmlFile file) {
    if (file instanceof GspFile) {
      return ContainerUtil.map2Array(GspTagLibUtil.getTagLibClasses(file).keySet(), String[].class, this);
    }
    else {
      FileViewProvider viewProvider = file.getViewProvider();
      if (viewProvider instanceof GspFileViewProvider && viewProvider.getFileType() == GspFileType.GSP_FILE_TYPE) {
        final Language baseLanguage = ((GspFileViewProvider)viewProvider).getTemplateDataLanguage();
        if (baseLanguage == HTMLLanguage.INSTANCE || baseLanguage == XHTMLLanguage.INSTANCE) {
          return HTML_PREFIXES;
        }
      }
    }

    return null;
  }

  @Override
  public boolean overrideNamespaceFromDocType(@NotNull XmlFile file) {
    return false;
  }

  @Override
  public String[] fun(String s) {
    return new String[]{s, s};
  }
}
