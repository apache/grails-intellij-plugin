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
package org.apache.grails.intellij.plugin.lang.gsp.psi.impl;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.xml.XmlAttribute;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.lang.gsp.GspDirectiveKind;
import org.apache.grails.intellij.plugin.lang.gsp.psi.groovy.api.GspGroovyFile;
import org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.api.directive.GspDirective;
import org.jetbrains.plugins.groovy.lang.resolve.imports.GroovyFileImports;
import org.jetbrains.plugins.groovy.lang.resolve.imports.impl.GroovyImportCollector;

import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Imports a GSP declares through {@code <%@ page import="..." %>} directives, in the form the
 * Groovy resolver consumes.
 */
public final class GspImportUtil {

  private GspImportUtil() {
  }

  private static final Pattern GSP_IMPORT_PATTERN = Pattern.compile(
    "(static\\s+)?(((?:\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}+\\.)*)(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}+))(?:(?:\\s+as\\s+(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}+))|(\\.\\*))?"
  );

  public static @NotNull GroovyFileImports getFileImports(@NotNull GspGroovyFile file) {
    return CachedValuesManager.getCachedValue(file, () -> CachedValueProvider.Result.create(doGetFileImports(file), file));
  }

  private static @NotNull GroovyFileImports doGetFileImports(@NotNull GspGroovyFile file) {
    GroovyImportCollector collector = new GroovyImportCollector(file);
    for (GspDirective directive : file.getGspLanguageRoot().getDirectiveTags(GspDirectiveKind.PAGE, true)) {
      XmlAttribute attribute = directive.getAttribute("import");
      if (attribute == null) continue;
      String value = attribute.getValue();
      if (value == null) continue;
      StringTokenizer st = new StringTokenizer(value, ";");
      while (st.hasMoreTokens()) {
        addImport(collector, st.nextToken().trim());
      }
    }
    return collector.build();
  }

  private static void addImport(@NotNull GroovyImportCollector collector, @NotNull String str) {
    Matcher matcher = GSP_IMPORT_PATTERN.matcher(str);
    if (!matcher.matches()) return;

    boolean isStatic = matcher.group(1) != null;
    boolean isStar = matcher.group(6) != null;
    if (isStatic && isStar) {
      collector.addStaticStarImport(matcher.group(2));
    }
    else if (isStatic) {
      String className = matcher.group(3);
      if (className.isEmpty()) return;
      String memberName = matcher.group(4);
      String alias = matcher.group(5);
      collector.addStaticImport(StringUtil.trimEnd(className, "."), memberName, alias != null ? alias : memberName);
    }
    else if (isStar) {
      collector.addStarImport(matcher.group(2));
    }
    else {
      String alias = matcher.group(5);
      collector.addRegularImport(matcher.group(2), alias != null ? alias : matcher.group(4));
    }
  }
}
