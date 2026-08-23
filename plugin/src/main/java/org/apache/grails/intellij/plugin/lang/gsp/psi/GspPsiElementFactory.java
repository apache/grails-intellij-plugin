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

package org.apache.grails.intellij.plugin.lang.gsp.psi;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.lang.gsp.GspDirectiveKind;
import org.apache.grails.intellij.plugin.lang.gsp.psi.groovy.api.GspOuterHtmlElement;
import org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.api.GspScriptletTag;
import org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.api.directive.GspDirective;
import org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.api.directive.GspDirectiveAttribute;

public abstract class GspPsiElementFactory {

  public static GspPsiElementFactory getInstance(Project project) {
    return project.getService(GspPsiElementFactory.class);
  }

  public abstract GspDirective createDirectiveByKind(GspDirectiveKind kind);

  public abstract GspDirectiveAttribute createDirectiveAttribute(@NotNull String name, @NotNull String value);

  public abstract GspScriptletTag createScriptletTagFromText(String text);

  public abstract GspOuterHtmlElement createOuterHtmlElement(String text);

  public abstract <T> T createElementFromText(String text);

}
