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

package org.apache.grails.intellij.plugin.config;

import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;

public final class BuildConfigSupport extends GrailsAbstractConfigSupport {

  @Override
  public PropertiesProvider getProvider(@NotNull GroovyFile file) {
    if (GrailsUtils.isBuildConfigFile(file)) {
      return this;
    }
    return null;
  }

  @Override
  protected String @NotNull [] getFinalProperties() {
    return new String[]{
      "grails.project.class.dir",
      "grails.project.test.class.dir",
      "grails.project.test.reports.dir",
      "grails.project.dependency.resolution",
      "grails.project.dependency.distribution",
      "grails.project.war.file"
    };
  }

  @Override
  protected String @NotNull [] getPrefixes() {
    return new String[]{
      "grails.plugin.location"
    };
  }
}
