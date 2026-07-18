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

package org.jetbrains.plugins.grails.addins;

import org.jetbrains.annotations.NonNls;

public final class GrailsIntegrationUtil {
  private static final boolean myJsSupportEnabled = classExists("com.intellij.lang.javascript.psi.JSElement");
  private static final boolean myCssSupportEnabled = classExists("com.intellij.psi.css.CssElement");

  private GrailsIntegrationUtil() {
  }

  private static boolean classExists(@NonNls String qname) {
    try {
      Class.forName(qname);
      return true;
    }
    catch (ClassNotFoundException e) {
      return false;
    }
  }

  public static boolean isJsSupportEnabled() {
    return myJsSupportEnabled;
  }

  public static boolean isCssSupportEnabled() {
    return myCssSupportEnabled;
  }

}
