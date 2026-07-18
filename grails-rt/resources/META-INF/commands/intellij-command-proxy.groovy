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
import org.grails.cli.gradle.GradleInvoker

String initScriptPath = System.getenv("INTELLIJ_GRADLE_INIT_SCRIPT")
String[] allArgs = commandLine.remainingArgs
String commandToRun = grails.util.GrailsNameUtils.getNameFromScript(allArgs.head())
String[] commandArguments = allArgs.tail() + commandLine.systemProperties.collect { key, value ->
  "-D${key}=$value".toString()
}

if (initScriptPath) {
  MetaMethod original = GradleInvoker.metaClass.getMetaMethod("invokeMethod", [String.class, Object.class] as Object[])
  GradleInvoker.metaClass.invokeMethod = { String name, Object args ->
    def argsList = args as List<Object>
    argsList.add('--init-script')
    argsList.add(initScriptPath)
    original.invoke(delegate, [name, argsList] as Object[])
  }
}

"$commandToRun"(*commandArguments)
