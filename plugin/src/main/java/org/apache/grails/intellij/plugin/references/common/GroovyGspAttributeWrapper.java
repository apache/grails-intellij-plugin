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

package org.apache.grails.intellij.plugin.references.common;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.lang.gsp.resolve.taglib.TagLibNamespaceDescriptor;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.util.GrNamedArgumentsOwner;

public class GroovyGspAttributeWrapper implements GspAttributeWrapper {

  private final GrNamedArgument myNamedArgument;
  private final GroovyGspTagWrapper tagWrapper;

  public GroovyGspAttributeWrapper(GrNamedArgument namedArgument, GroovyGspTagWrapper tagWrapper) {
    myNamedArgument = namedArgument;
    this.tagWrapper = tagWrapper;
  }

  public GroovyGspAttributeWrapper(GrNamedArgument namedArgument, TagLibNamespaceDescriptor.GspTagMethod gspTagLibVariable) {
    this(namedArgument, new GroovyGspTagWrapper((GrNamedArgumentsOwner)namedArgument.getParent(), gspTagLibVariable));
  }

  @Override
  public @NotNull GspTagWrapper getTag() {
    return tagWrapper;
  }

  @Override
  public String getName() {
    return myNamedArgument.getLabelName();
  }

  @Override
  public PsiElement getValue() {
    return myNamedArgument.getExpression();
  }
}
