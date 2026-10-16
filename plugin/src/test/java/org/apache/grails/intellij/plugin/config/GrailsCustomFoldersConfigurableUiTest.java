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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.testFramework.IdeaTestUtil;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ThrowableRunnable;
import com.intellij.util.messages.MessageBusConnection;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JSpinner;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;


/**
 * The settings page as the platform drives it: built once, reset from the services, edited through
 * its rows and spinner, applied back to them, and disposed. The rows and the preview are reached
 * through the built component because they are private fields, and the page is only ever used
 * through the {@code Configurable} API.
 */
public class GrailsCustomFoldersConfigurableUiTest extends GrailsTestCase {

  // The page is Swing, which the minimal Mock JDK 11 does not ship.
  @Override
  protected @NotNull Supplier<Sdk> getTestJdk() {
    return () -> JavaSdk.getInstance().createJdk("TEST_JDK", IdeaTestUtil.requireRealJdkHome(), false);
  }

  /** FR23: the panel is built once and reused, so the settings dialog never loses a user's edits. */
  public void testCreateComponentBuildsOnePanel() {
    GrailsCustomFoldersConfigurable configurable = page();

    JComponent first = configurable.createComponent();
    assertNotNull(first);
    assertSame("the built panel is cached and handed back on the second call",
               first, configurable.createComponent());
  }

  /** FR25: {@code reset()} seeds both row lists and the spinner from the services. */
  public void testResetSeedsBothRowListsAndTheSpinnerFromTheServices() throws ConfigurationException {
    withGlobalDefaults(() -> {
      appService().setFolders(List.of("global/a", "global/b"));
      projectService().setFolders(List.of("project/c"));
      appService().setMaxFolders(10);

      GrailsCustomFoldersConfigurable configurable = page();
      configurable.createComponent();
      configurable.reset();

      assertEquals(List.of("global/a", "global/b"), globalRows(configurable).getRows());
      assertEquals(List.of("project/c"), projectRows(configurable).getRows());
      assertEquals("the spinner is seeded from the persisted setting, not from a constant",
                   10, spinnerValue(configurable));
      assertFalse("nothing is edited yet, so the page is not modified", configurable.isModified());
    });
  }

  /**
   * FR22: the page's description states the cap the user is editing, so the text is right whether
   * the setting is the default of six or anything else.
   */
  public void testDescriptionCarriesTheCapBeingEdited() throws ConfigurationException {
    withGlobalDefaults(() -> {
      appService().setMaxFolders(10);
      GrailsCustomFoldersConfigurable configurable = page();
      configurable.createComponent();

      assertTrue(descriptionText(configurable).contains("at most 10 folders in total"));
      assertFalse("the description no longer claims a compiled-in cap",
                  descriptionText(configurable).contains("at most 3"));
    });
  }

  /**
   * A row edit or a spinner edit is what {@code isModified()} must see: the page is offered as
   * modified exactly when the services hold something other than what is on screen.
   */
  public void testIsModifiedFollowsTheRowsAndTheSpinner() throws ConfigurationException {
    withGlobalDefaults(() -> {
      appService().setFolders(List.of("global/a"));
      projectService().setFolders(List.of("project/c"));
      GrailsCustomFoldersConfigurable configurable = page();
      configurable.createComponent();
      configurable.reset();
      assertFalse("a freshly reset page differs from nothing", configurable.isModified());

      globalRows(configurable).addRow("global/edited");
      assertTrue("an appended row makes the page modified", configurable.isModified());

      configurable.reset();
      assertFalse("reset() brings the page back in line with the services", configurable.isModified());

      spinner(configurable).setValue(9);
      assertTrue("a spinner edit alone also makes the page modified", configurable.isModified());
    });
  }

  /** FR32: no row is dropped for being over the cap, and both lists plus the cap are persisted. */
  public void testApplyWritesBothServicesAndTheSpinner() throws ConfigurationException {
    withGlobalDefaults(() -> {
      appService().setFolders(Collections.<String>emptyList());
      projectService().setFolders(Collections.<String>emptyList());
      GrailsCustomFoldersConfigurable configurable = page();
      configurable.createComponent();
      configurable.reset();

      globalRows(configurable).addRow("global/one");
      globalRows(configurable).addRow("global/two");
      projectRows(configurable).addRow("project/three");
      spinner(configurable).setValue(12);

      configurable.apply();

      assertEquals(List.of("global/one", "global/two"), appService().getFolders());
      assertEquals(List.of("project/three"), projectService().getFolders());
      assertEquals(12, appService().getMaxFolders());
      assertFalse("the page now matches what was written", configurable.isModified());
    });
  }

  /**
   * Two spellings of one folder in a list are one folder once stored, and the page has to know that:
   * comparing the raw rows against the store leaves the page modified forever after such an Apply,
   * which reads as an edit that was not saved.
   */
  public void testApplyOfARowListWithADuplicateLeavesThePageUnmodified() throws ConfigurationException {
    withGlobalDefaults(() -> {
      appService().setFolders(Collections.<String>emptyList());
      projectService().setFolders(Collections.<String>emptyList());
      GrailsCustomFoldersConfigurable configurable = page();
      configurable.createComponent();
      configurable.reset();

      globalRows(configurable).addRow("global/one");
      globalRows(configurable).addRow("global/one/");
      assertTrue("a duplicate spelling is a change worth applying", configurable.isModified());

      configurable.apply();

      assertEquals("the duplicate is stored once", List.of("global/one"), appService().getFolders());
      assertFalse("the page now matches what was written, duplicate and all",
                  configurable.isModified());
    });
  }

  /** One Apply must cost one pane rebuild per open project, not one per field written. */
  public void testApplyPublishesOneGlobalChange() throws ConfigurationException {
    withGlobalDefaults(() -> {
      appService().setFolders(Collections.<String>emptyList());
      projectService().setFolders(Collections.<String>emptyList());
      int[] published = {0};
      MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus()
        .connect(getTestRootDisposable());
      connection.subscribe(GrailsCustomFoldersAppService.GlobalFoldersChangedListener.TOPIC,
                          () -> published[0]++);
      GrailsCustomFoldersConfigurable configurable = page();
      configurable.createComponent();
      configurable.reset();

      globalRows(configurable).addRow("global/one");
      spinner(configurable).setValue(12);
      configurable.apply();

      assertEquals("the folder list and the cap are written in a single publish", 1, published[0]);
    });
  }

  /**
   * FR17: a path the Grails view could not use is refused, naming its area and row.
   */
  public void testApplyRejectsAnAbsolutePathNamingTheAreaAndTheRow() throws ConfigurationException {
    withGlobalDefaults(() -> {
      appService().setFolders(List.of("global/stored"));
      projectService().setFolders(Collections.<String>emptyList());
      GrailsCustomFoldersConfigurable configurable = page();
      configurable.createComponent();
      configurable.reset();

      projectRows(configurable).addRow("docs");
      projectRows(configurable).addRow("/etc/passwd");

      try {
        configurable.apply();
        fail("an absolute path cannot be resolved against the Grails application root");
      }
      catch (ConfigurationException e) {
        String message = String.valueOf(e.getMessage());
        assertTrue("the message names the area: " + message,
                   message.contains("This project's folders"));
        assertTrue("the message names the row: " + message, message.contains("row 2"));
        assertTrue("the message quotes the path: " + message, message.contains("/etc/passwd"));

        assertEquals("nothing of the global defaults is written either",
                     List.of("global/stored"), appService().getFolders());
        assertEquals("nor is anything this project owned",
                     Collections.<String>emptyList(), projectService().getFolders());
      }
    });
  }

  /**
   * FR17: the row the message names has to be the one the user sees. A list holding an unused blank
   * row is numbered over the rows as displayed, so an invalid path in the second field is "row 2"
   * even though it is the first row that holds a path — otherwise the message points at the wrong
   * field and Apply reports the same wrong number again.
   */
  public void testApplyNumbersTheRowTheUserSees() throws ConfigurationException {
    withGlobalDefaults(() -> {
      appService().setFolders(Collections.<String>emptyList());
      GrailsCustomFoldersConfigurable configurable = page();
      configurable.createComponent();
      configurable.reset();

      projectRows(configurable).addRow("");
      projectRows(configurable).addRow("docs");
      projectRows(configurable).addRow("../outside");

      try {
        configurable.apply();
        fail("a path that escapes the application root is refused");
      }
      catch (ConfigurationException e) {
        String message = String.valueOf(e.getMessage());
        assertTrue("the message names the row beside the offending field: " + message,
                   message.contains("row 3"));
        assertEquals("the rows behind the offending one are still fine",
                     Collections.<String>emptyList(), projectService().getFolders());
      }
    });
  }

  /**
   * The project rows are validated against this project's application root, which the light
   * fixture registers: a path resolving inside it survives {@code apply()}. The other branch of
   * that lookup — no application in the project — cannot be built by a light fixture, because
   * {@code GrailsTestCase.setUp} always registers one, so only the configured case is asserted.
   */
  public void testApplyAcceptsAPathInsideTheApplicationRoot() throws ConfigurationException {
    withGlobalDefaults(() -> {
      projectService().setFolders(Collections.<String>emptyList());
      GrailsCustomFoldersConfigurable configurable = page();
      configurable.createComponent();
      configurable.reset();

      projectRows(configurable).addRow("grails-app/domain");

      configurable.apply();

      assertEquals(List.of("grails-app/domain"), projectService().getFolders());
    });
  }

  /** FR17: the global defaults are validated on shape alone, because they belong to every project. */
  public void testApplyRejectsAnEscapingGlobalPath() throws ConfigurationException {
    withGlobalDefaults(() -> {
      appService().setFolders(Collections.<String>emptyList());
      GrailsCustomFoldersConfigurable configurable = page();
      configurable.createComponent();
      configurable.reset();

      globalRows(configurable).addRow("../outside");

      try {
        configurable.apply();
        fail("a path that escapes the application root is refused for the global defaults too");
      }
      catch (ConfigurationException e) {
        String message = String.valueOf(e.getMessage());
        assertTrue("the message names the area: " + message, message.contains("Global defaults"));
        assertTrue("the message names the row: " + message, message.contains("row 1"));
      }
    });
  }

  /**
   * FR33: the preview and the source indicator read the rows and the spinner as they are edited.
   * The page is only reset from the stored services once, so everything after that is live.
   */
  public void testPreviewAndSourceIndicatorTrackTheRowsAndTheCap() throws ConfigurationException {
    withGlobalDefaults(() -> {
      appService().setFolders(Collections.<String>emptyList());
      projectService().setFolders(Collections.<String>emptyList());
      GrailsCustomFoldersConfigurable configurable = page();
      configurable.createComponent();
      configurable.reset();

      globalRows(configurable).addRow("global/a");
      projectRows(configurable).addRow("project/b");

      assertEquals("this project's folders come first, then the global defaults",
                   "project/b\nglobal/a", previewText(configurable));
      assertEquals("using this project's folders plus global defaults",
                   "Using this project's folders plus global defaults", sourceText(configurable));
      assertFalse("under the cap there is no warning", warningVisible(configurable));

      // The cap is the field under test: moving it alone must re-render, with no row edit in
      // between to do it for me.
      spinner(configurable).setValue(1);
      assertEquals("the preview is truncated to the cap being edited, not to the stored one",
                   "project/b", previewText(configurable));
      assertTrue("over the cap the warning appears", warningVisible(configurable));
      assertEquals("You have entered 2 unique folders. Only the first 1 will be used.",
                   warningText(configurable));
      assertTrue("the description quotes the cap being edited, not the stored one",
                 descriptionText(configurable).contains("at most 1 folders in total"));

      globalRows(configurable).addRow("global/c");
      assertEquals("a row beyond the cap is not previewed either",
                   "project/b", previewText(configurable));
      assertEquals("You have entered 3 unique folders. Only the first 1 will be used.",
                   warningText(configurable));

      projectRows(configurable).removeRow(0);
      projectRows(configurable).removeRow(0);
      assertEquals("with no project folders the source indicator says so",
                   "Using global defaults only", sourceText(configurable));
    });
  }

  /** FR22/FR33: the description follows the spinner, not only the setting the page opened on. */
  public void testDescriptionFollowsTheSpinnerRatherThanTheStoredSetting() throws ConfigurationException {
    withGlobalDefaults(() -> {
      appService().setMaxFolders(6);
      GrailsCustomFoldersConfigurable configurable = page();
      configurable.createComponent();
      assertTrue("the page opens quoting the stored cap",
                 descriptionText(configurable).contains("at most 6 folders in total"));

      spinner(configurable).setValue(20);

      assertTrue("raising the spinner must rewrite the description, not leave a stale cap",
                 descriptionText(configurable).contains("at most 20 folders in total"));
      assertFalse("the old cap is gone", descriptionText(configurable).contains("at most 6"));
    });
  }

  /** FR23: the live preview is wired to the rows' document events, not re-read on save only. */
  public void testRowsDriveThePreviewWhileTheyAreEdited() throws ConfigurationException {
    withGlobalDefaults(() -> {
      appService().setFolders(Collections.<String>emptyList());
      projectService().setFolders(Collections.<String>emptyList());
      GrailsCustomFoldersConfigurable configurable = page();
      configurable.createComponent();
      configurable.reset();

      assertEquals("nothing is configured yet", "", previewText(configurable));

      globalRows(configurable).addRow("docs");
      assertEquals("an appended row reaches the preview through the document listener",
                   "docs", previewText(configurable));

      projectRows(configurable).addRow("");
      projectRows(configurable).addRow("   ");
      assertEquals("a blank row is not a folder, so it never reaches the preview",
                   "docs", previewText(configurable));

      projectRows(configurable).setRows(List.of("first", "second"));
      assertEquals("first\nsecond\ndocs", previewText(configurable));
    });
  }

  /** FR23: the panel is thrown away on dispose and rebuilt on the next create, from scratch. */
  public void testDisposeUIResourcesDropsTheBuiltPanel() throws ConfigurationException {
    withGlobalDefaults(() -> {
      appService().setFolders(List.of("global/stored"));
      GrailsCustomFoldersConfigurable configurable = page();

      JComponent first = configurable.createComponent();
      globalRows(configurable).addRow("edited");
      configurable.disposeUIResources();

      JComponent second = configurable.createComponent();
      assertNotNull(second);
      assertNotSame("the disposed panel is not handed back", first, second);
      assertEquals("the rebuilt panel starts from the services again, not from the edited rows",
                   List.of("global/stored"), globalRows(configurable).getRows());
    });
  }

  private @NotNull GrailsCustomFoldersConfigurable page() {
    return new GrailsCustomFoldersConfigurable(getProject());
  }

  private static @NotNull GrailsCustomFoldersAppService appService() {
    return GrailsCustomFoldersAppService.getStaticInstance();
  }

  private @NotNull GrailsCustomFoldersProjectService projectService() {
    return getProject().getService(GrailsCustomFoldersProjectService.class);
  }

  /** The global-defaults row list, which the form puts before the cap spinner. */
  private static @NotNull GrailsCustomFoldersRows globalRows(@NotNull GrailsCustomFoldersConfigurable page) {
    return rowPanels(page).get(0);
  }

  /** This project's row list, which the form puts after the cap spinner. */
  private static @NotNull GrailsCustomFoldersRows projectRows(@NotNull GrailsCustomFoldersConfigurable page) {
    return rowPanels(page).get(1);
  }

  private static @NotNull JSpinner spinner(@NotNull GrailsCustomFoldersConfigurable page) {
    JSpinner found = firstOfType(page.createComponent(), JSpinner.class);
    assertNotNull("the page carries a spinner for the cap", found);
    return found;
  }

  private static int spinnerValue(@NotNull GrailsCustomFoldersConfigurable page) {
    return (Integer)spinner(page).getValue();
  }

  private static @NotNull String previewText(@NotNull GrailsCustomFoldersConfigurable page) {
    JBTextArea area = firstOfType(page.createComponent(), JBTextArea.class);
    assertNotNull("the page previews the effective folders in a text area", area);
    return area.getText();
  }

  private static @NotNull String descriptionText(@NotNull GrailsCustomFoldersConfigurable page) {
    return labelContaining(page, "A path is resolved");
  }

  private static @NotNull String labelContaining(@NotNull GrailsCustomFoldersConfigurable page,
                                                 @NotNull String fragment) {
    for (JBLabel label : allOfType(page.createComponent(), JBLabel.class)) {
      if (label.getText() != null && label.getText().contains(fragment)) return label.getText();
    }
    fail("no label containing \"" + fragment + "\"");
    return "";
  }

  private static @NotNull String sourceText(@NotNull GrailsCustomFoldersConfigurable page) {
    return labelWithPrefix(page, "Using ");
  }

  private static @NotNull String warningText(@NotNull GrailsCustomFoldersConfigurable page) {
    return labelWithPrefix(page, "You have entered ");
  }

  private static boolean warningVisible(@NotNull GrailsCustomFoldersConfigurable page) {
    JBLabel label = labelFor(page, "You have entered ");
    return label != null && label.isVisible();
  }

  private static @NotNull String labelWithPrefix(@NotNull GrailsCustomFoldersConfigurable page,
                                                 @NotNull String prefix) {
    JBLabel label = labelFor(page, prefix);
    assertNotNull("no label starting with \"" + prefix + "\"", label);
    return label.getText();
  }

  private static @Nullable JBLabel labelFor(@NotNull GrailsCustomFoldersConfigurable page, @NotNull String prefix) {
    for (JBLabel label : allOfType(page.createComponent(), JBLabel.class)) {
      if (label.getText() != null && label.getText().startsWith(prefix)) return label;
    }
    return null;
  }

  private static @NotNull List<GrailsCustomFoldersRows> rowPanels(@NotNull GrailsCustomFoldersConfigurable page) {
    List<GrailsCustomFoldersRows> rows = allOfType(page.createComponent(), GrailsCustomFoldersRows.class);
    assertTrue("the page holds one row list for the global defaults and one for this project",
               rows.size() >= 2);
    return rows;
  }

  private static <T> @Nullable T firstOfType(@NotNull JComponent root, @NotNull Class<T> type) {
    List<T> found = allOfType(root, type);
    return found.isEmpty() ? null : found.get(0);
  }

  private static <T> @NotNull List<T> allOfType(@NotNull JComponent root, @NotNull Class<T> type) {
    List<T> found = new ArrayList<>();
    collectOfType(root, type, found);
    return found;
  }

  private static <T> void collectOfType(@NotNull JComponent root, @NotNull Class<T> type, @NotNull List<T> found) {
    if (type.isInstance(root)) {
      found.add(type.cast(root));
    }
    for (Component child : root.getComponents()) {
      if (child instanceof JComponent) {
        collectOfType((JComponent)child, type, found);
      }
    }
  }

  /**
   * Runs {@code body} with the global defaults left as it found them. They are application state,
   * so leaving them mutated here would change the effective folders of every later test in the run.
   */
  private static void withGlobalDefaults(
    @NotNull ThrowableRunnable<ConfigurationException> body) throws ConfigurationException {
    GrailsCustomFoldersAppService appService = appService();
    List<String> original = new ArrayList<>(appService.getFolders());
    int originalMax = appService.getMaxFolders();
    try {
      body.run();
    }
    finally {
      appService.setFolders(original);
      appService.setMaxFolders(originalMax);
    }
  }
}
