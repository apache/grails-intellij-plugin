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
pluginManagement {
    includeBuild("build-logic")
}

rootProject.name = "Grails IntelliJ Plugin"

include("copyright")
include("coverage")
include("gradle-tooling")
include("grails-compiler-patch")
include("grails-rt")
include("hibernate")
include("i18n")
include("jps-plugin")
include("langInjection")
include("maven")
include("testFramework")