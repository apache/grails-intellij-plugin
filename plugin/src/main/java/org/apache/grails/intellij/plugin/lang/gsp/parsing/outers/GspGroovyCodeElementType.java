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

package org.apache.grails.intellij.plugin.lang.gsp.parsing.outers;

import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.ILeafElementType;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.lang.gsp.lexer.IGspElementType;
import org.apache.grails.intellij.plugin.lang.gsp.psi.groovy.impl.GspOuterGroovyElementImpl;

public class GspGroovyCodeElementType extends IGspElementType implements ILeafElementType {

  public GspGroovyCodeElementType() {
    super("GSP_GROOVY_CODE");
  }

  @Override
  public @NotNull ASTNode createLeafNode(@NotNull CharSequence leafText) {
    return new GspOuterGroovyElementImpl(this, leafText);
  }
}
