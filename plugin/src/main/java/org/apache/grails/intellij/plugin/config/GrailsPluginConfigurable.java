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

package org.apache.grails.intellij.plugin.config;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Grouping node that places the Grails settings pages under Settings | Tools | Grails IntelliJ
 * Plugin. It owns no settings of its own; the pages are its children, which the settings tree
 * nests via {@code parentId}.
 */
public class GrailsPluginConfigurable implements Configurable {

  @Override
  public @NotNull String getDisplayName() {
    return "Grails IntelliJ Plugin";
  }

  @Override
  public @NotNull JComponent createComponent() {
    return new JPanel();
  }

  @Override
  public boolean isModified() {
    return false;
  }

  @Override
  public void apply() {
  }

  @Override
  public void reset() {
  }

  @Override
  public void disposeUIResources() {
  }
}