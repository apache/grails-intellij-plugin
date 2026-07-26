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
package org.jetbrains.plugins.grails.gson;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.resolve.imports.GrImportContributor;
import org.jetbrains.plugins.groovy.lang.resolve.imports.GroovyImport;
import org.jetbrains.plugins.groovy.lang.resolve.imports.StaticStarImport;

import java.util.List;

public final class GsonImportContributor implements GrImportContributor {

  // A constant, so no lazy initialisation is needed: the Kotlin wrapped this fixed list in a
  // lazy delegate, which bought nothing.
  private static final List<GroovyImport> IMPORTS = List.of(
    new StaticStarImport("org.springframework.http.HttpStatus"),
    new StaticStarImport("org.springframework.http.HttpMethod"),
    new StaticStarImport("grails.web.http.HttpHeaders")
  );

  @Override
  public @NotNull List<GroovyImport> getFileImports(@NotNull GroovyFile file) {
    return GsonUtils.isGsonFile(file) ? IMPORTS : List.of();
  }
}
