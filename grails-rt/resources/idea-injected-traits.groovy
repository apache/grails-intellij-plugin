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
import grails.compiler.traits.TraitInjector
import org.grails.core.io.support.GrailsFactoriesLoader

def MARKER = '--------------------------------------'

def injectors = GrailsFactoriesLoader.loadFactories(
  TraitInjector.class, Thread.currentThread().contextClassLoader
)

try {
  injectors = org.grails.compiler.injection.TraitInjectionSupport.resolveTraitInjectors(injectors)
}
catch (Throwable ignored) {
}

println()
println()
println()
println MARKER
injectors.each {
  Class traitClass = it.trait
  println traitClass.name
  println it.artefactTypes.length
  it.artefactTypes.each {
    println it
  }
}
println MARKER
