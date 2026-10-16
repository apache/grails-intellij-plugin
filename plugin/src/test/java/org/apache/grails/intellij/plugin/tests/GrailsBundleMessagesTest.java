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

package org.apache.grails.intellij.plugin.tests;

import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;
import org.apache.grails.intellij.plugin.GrailsBundle;
import org.jetbrains.annotations.NotNull;

public class GrailsBundleMessagesTest extends TestCase {

  private static final String ISSUES_URL = "https://github.com/apache/grails-intellij-plugin/issues";

  /**
   * This message is the only thing a user sees when Grails detection silently turns itself off, so
   * it has to carry all four elements: what broke, what it costs them, what to do about it, and
   * where to report it. Asserting only the first two would let a remedy-free message through.
   */
  public void testProvidersFailedToLoadMessageNamesWhatFailedTheConsequenceTheRemedyAndWhereToReportIt() {
    String message = GrailsBundle.message("error.grails.application.providers.failed.to.load");

    assertNotBlank("", message);
    assertTrue("must name what failed: " + message, message.contains("load the Grails application providers"));
    assertTrue("must state the consequence: " + message,
               message.contains("Grails project detection is disabled for this project"));
    assertTrue("must give the remedy: " + message, message.contains("Settings | Plugins"));
    assertTrue("must say where to report it: " + message, message.contains(ISSUES_URL));
    assertFalse("the remedy must work on Community Edition too, so it must not point a CE user " +
                "at an Ultimate module: " + message, message.contains("Ultimate"));
  }

  public void testProviderFailedForMessageNamesTheProviderAndTheRoot() {
    String message = GrailsBundle.message("warning.grails.application.provider.failed.for",
                                         "com.example.MyProvider", "/projects/demo");

    assertNotBlank("", message);
    assertTrue(message.contains("com.example.MyProvider"));
    assertTrue(message.contains("/projects/demo"));
    assertTrue("must say the remaining providers were still tried: " + message,
               message.contains("; the remaining providers were still tried"));
    assertFalse("must not claim Grails support is lost: " + message, message.contains("disabled"));
  }

  public void testBothArgumentsAreConsumedByTheFormatter() {
    // MessageFormat throws on a malformed {n}, so reaching here proves the key's two placeholders
    // are well formed; the assertions prove neither was left unconsumed or misnumbered.
    String message = GrailsBundle.message("warning.grails.application.provider.failed.for",
                                         "FIRST", "SECOND");

    assertFalse("an unconsumed placeholder is left: " + message, message.contains("{"));
    assertTrue("arguments must appear in order: " + message,
               message.indexOf("FIRST") < message.indexOf("SECOND"));
  }

  /**
   * The custom-folders settings page takes all of its text from this bundle, and the escaping is the
   * trap. {@code BundleBaseKt.postprocessValue} returns the pattern untouched when it is given no
   * arguments, and only runs it through {@code MessageFormat} when arguments were passed and the
   * pattern contains a {@code &#123;}. So the rule is about the arguments, not the placeholder: a key
   * resolved with no arguments must carry a single apostrophe, and one resolved with arguments a
   * doubled one. Getting it backwards leaves a literal {@code ''} in the dialog — which is what
   * happened when the labels were keyed first, and only the settings-page tests caught.
   */
  public void testCustomFoldersMessagesResolveWithoutDoubledApostrophesOrLeftoverPlaceholders() {
    List<String> withoutArguments = Arrays.asList(
      "grails.settings.custom.folders.global.defaults",
      "grails.settings.custom.folders.maximum.folders",
      "grails.settings.custom.folders.project.folders",
      "grails.settings.custom.folders.effective.folders",
      "grails.settings.custom.folders.source.global",
      "grails.settings.custom.folders.source.project",
      "grails.settings.custom.folders.absolute.path",
      "grails.settings.custom.folders.escapes.application.root",
      "grails.settings.custom.folders.outside.application.root",
      "grails.settings.custom.folders.area.global",
      "grails.settings.custom.folders.area.project",
      "grails.settings.custom.folders.row.add",
      "grails.settings.custom.folders.row.remove");

    for (String key : withoutArguments) {
      String message = GrailsBundle.message(key);
      assertNotBlank(key + " must resolve", message);
      assertFalse(key + " must not carry a doubled apostrophe: " + message, message.contains("''"));
    }

    String description = GrailsBundle.message("grails.settings.custom.folders.description", 6);
    assertTrue("the description states the base: " + description,
               description.contains("resolved against the Grails application root"));
    assertTrue("the description states the cap: " + description, description.contains("at most 6"));
    assertFalse("the description must not carry a doubled apostrophe: " + description,
                description.contains("''"));
    assertTrue("the description must name whose folders come first: " + description,
               description.contains("this project's folders"));

    String warning = GrailsBundle.message("grails.settings.custom.folders.over.cap", 8, 3);
    assertEquals("You have entered 8 unique folders. Only the first 3 will be used.", warning);

    String row = GrailsBundle.message("grails.settings.custom.folders.invalid.row",
                                      "Global defaults", 2, "/etc/passwd",
                                      "must be relative to the Grails application root, not an absolute path");
    assertTrue("the row message names the area: " + row, row.contains("Global defaults"));
    assertTrue("the row message names the row: " + row, row.contains("row 2"));
    assertTrue("the row message quotes the path: " + row, row.contains("\"/etc/passwd\""));
    assertFalse("no leftover placeholder: " + row, row.contains("{"));

    String cap = GrailsBundle.message("grails.settings.custom.folders.cap.reached", 6);
    assertEquals("Maximum of 6 custom folders reached. Remove one first.", cap);
  }

  private static void assertNotBlank(@NotNull String key, String message) {
    assertNotNull(key + " must be registered", message);
    assertFalse(key + " must not be blank", message.trim().isEmpty());
  }
}
