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

package org.jetbrains.groovy.grails.compiler;

import groovy.lang.GroovyResourceLoader;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.grails.compiler.injection.GrailsAwareInjectionOperation;
import org.jetbrains.groovy.compiler.rt.CompilationUnitPatcher;

import java.io.File;
import java.lang.reflect.Constructor;

public class EmptyGrailsAwarePatcher extends CompilationUnitPatcher {

  @Override
  public void patchCompilationUnit(CompilationUnit compilationUnit, GroovyResourceLoader resourceLoader, File[] srcFiles) {
    Object instance;

    try {
      Constructor c = GrailsAwareInjectionOperation.class.getConstructor();
      instance = c.newInstance();
    }
    catch (Exception ee) {
      throw new RuntimeException(ee);
    }

    compilationUnit.addPhaseOperation((GrailsAwareInjectionOperation)instance, Phases.CANONICALIZATION);
  }

}
