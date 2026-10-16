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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.FormBuilder;
import org.apache.grails.intellij.plugin.GrailsBundle;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.structure.GrailsApplicationManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import java.util.List;

/**
 * The Grails settings page. It edits the global defaults (application service) and
 * this project's own folders (project service) together and previews the folders the Grails view
 * will actually show. The global defaults apply to every Grails project; this project's folders
 * are added to them.
 */
public class GrailsCustomFoldersConfigurable implements Configurable {

  private final @NotNull GrailsCustomFoldersAppService appService;
  private final @NotNull GrailsCustomFoldersProjectService projectService;
  private final @NotNull Project project;

  private @Nullable JPanel component;
  private @Nullable GrailsCustomFoldersRows globalRows;
  private @Nullable GrailsCustomFoldersRows projectRows;
  private @Nullable JSpinner maxFoldersSpinner;
  private @Nullable JBTextArea effectiveArea;
  private @Nullable JBLabel sourceLabel;
  private @Nullable JBLabel warningLabel;
  private @Nullable JBLabel descriptionLabel;

  public GrailsCustomFoldersConfigurable(@NotNull Project project) {
    this.project = project;
    this.appService = ApplicationManager.getApplication().getService(GrailsCustomFoldersAppService.class);
    this.projectService = project.getService(GrailsCustomFoldersProjectService.class);
  }

  @Override
  public @NotNull String getDisplayName() {
    return GrailsBundle.message("grails.settings.display.name");
  }

  @Override
  public @NotNull JComponent createComponent() {
    if (component == null) {
      globalRows = new GrailsCustomFoldersRows();
      projectRows = new GrailsCustomFoldersRows();

      // The two row lists and the cap feed the same preview, so each registers the one repaint.
      globalRows.addRowsChangedListener(this::updateEffectivePreview);
      projectRows.addRowsChangedListener(this::updateEffectivePreview);

      // The range is the app service's, so the control cannot offer a value the setting rejects.
      maxFoldersSpinner = new JSpinner(new SpinnerNumberModel(appService.getMaxFolders(),
                                                             GrailsCustomFoldersAppService.MIN_FOLDERS,
                                                             GrailsCustomFoldersAppService.MAX_FOLDERS, 1));
      // Without this the cap is the one field on the page that renders nothing as it is edited: the
      // preview and the warning would keep quoting the value a row edit last read.
      maxFoldersSpinner.addChangeListener((ChangeEvent event) -> updateEffectivePreview());

      sourceLabel = new JBLabel();
      warningLabel = new JBLabel();
      warningLabel.setIcon(AllIcons.General.Warning);

      JBLabel description = new JBLabel(description(appService.getMaxFolders()));
      descriptionLabel = description;

      JBTextArea preview = new JBTextArea();
      preview.setEditable(false);
      effectiveArea = preview;

      component = FormBuilder.createFormBuilder()
        .addComponent(description)
        .addComponent(warningLabel)
        .addLabeledComponent(new JBLabel(key("grails.settings.custom.folders.global.defaults")),
                             globalRows.getComponent(), 1, false)
        .addLabeledComponent(new JBLabel(key("grails.settings.custom.folders.maximum.folders")),
                             maxFoldersSpinner, 1, false)
        .addLabeledComponent(new JBLabel(key("grails.settings.custom.folders.project.folders")),
                             projectRows.getComponent(), 1, false)
        .addComponent(sourceLabel)
        .addLabeledComponent(new JBLabel(key("grails.settings.custom.folders.effective.folders")),
                             new JBScrollPane(preview), 1, false)
        .getPanel();

      reset();
    }
    return component;
  }

  @Override
  public boolean isModified() {
    if (maxFoldersSpinner != null
        && ((Integer)maxFoldersSpinner.getValue()) != appService.getMaxFolders()) return true;
    // The rows are normalized before they are compared, the same way the store normalizes them on
    // save: otherwise a list holding two spellings of one folder never stops reading as modified.
    if (globalRows != null && !FolderPaths.dedupe(globalRows.getRows()).equals(appService.getFolders())) return true;
    return projectRows != null
           && !FolderPaths.dedupe(projectRows.getRows()).equals(projectService.getFolders());
  }

  @Override
  public void apply() throws ConfigurationException {
    if (globalRows == null || projectRows == null || maxFoldersSpinner == null) return;

    List<String> global = globalRows.getRows();
    List<String> project = projectRows.getRows();

    // Validated over the rows as typed, blanks included, so a message naming "row 3" names the field
    // the user sees beside the number 3: the trimmed reader would silently renumber a list that
    // holds an unused row.
    //
    // The global defaults are validated on shape alone (absolute path, escaping ".."), never against
    // an application root: they are shared by every open project, each with its own root, so a path
    // that resolves in one project need not resolve in another and is not invalid here. The
    // application-root check is what this project's own list gets, because its base is knowable.
    validateRows(globalRows.rawRows(), key("grails.settings.custom.folders.area.global"), null);
    validateRows(projectRows.rawRows(), key("grails.settings.custom.folders.area.project"), applicationRoot());

    appService.setGlobalDefaults(global, (Integer)maxFoldersSpinner.getValue());
    projectService.setFolders(project);
    updateEffectivePreview();
  }

  @Override
  public void reset() {
    if (globalRows != null) globalRows.setRows(appService.getFolders());
    if (projectRows != null) projectRows.setRows(projectService.getFolders());
    if (maxFoldersSpinner != null) maxFoldersSpinner.setValue(appService.getMaxFolders());
    updateEffectivePreview();
  }

  @Override
  public void disposeUIResources() {
    component = null;
    globalRows = null;
    projectRows = null;
    maxFoldersSpinner = null;
    effectiveArea = null;
    sourceLabel = null;
    warningLabel = null;
    descriptionLabel = null;
  }

  private void updateEffectivePreview() {
    if (globalRows == null || projectRows == null || effectiveArea == null
        || sourceLabel == null || warningLabel == null || maxFoldersSpinner == null
        || descriptionLabel == null) return;

    List<String> global = globalRows.getRows();
    List<String> project = projectRows.getRows();
    int max = (Integer)maxFoldersSpinner.getValue();

    descriptionLabel.setText(description(max));
    effectiveArea.setText(String.join("\n", combine(project, global, max)));
    sourceLabel.setText(project.isEmpty()
                          ? key("grails.settings.custom.folders.source.global")
                          : key("grails.settings.custom.folders.source.project"));

    String warning = overCapWarning(project, global, max);
    warningLabel.setText(warning);
    warningLabel.setVisible(!warning.isEmpty());
  }

  private static @NotNull String key(@NotNull String bundleKey) {
    return GrailsBundle.message(bundleKey);
  }

  /**
   * Rejects a path the Grails view could not use, naming the row it came from. Rows are never
   * truncated or dropped here: the cap limits what renders, not what is configured. A blank row is
   * not a path and is skipped, but it still holds its place in the numbering.
   */
  private static void validateRows(@NotNull List<String> rows, @NotNull String area, @Nullable VirtualFile appRoot)
    throws ConfigurationException {
    for (int i = 0; i < rows.size(); i++) {
      String path = rows.get(i);
      if (path == null || path.trim().isEmpty()) continue;
      String problem = invalidPathReason(path, appRoot);
      if (problem != null) {
        throw new ConfigurationException(
          GrailsBundle.message("grails.settings.custom.folders.invalid.row", area, i + 1, path, problem));
      }
    }
  }

  /**
   * Why the given path cannot be configured, or {@code null} when it can.
   * <p>
   * A null {@code appRoot} means shape-only validation: the first two checks decide and the
   * application-root check is skipped rather than run against the wrong base. That is the global
   * defaults' case, whose base is per project and so not knowable here — see {@link #apply()}.
   * <p>
   * The application-root check answers "does this resolve outside the root", and it deliberately
   * draws that line at resolution: a path that resolves to nothing yet is accepted, because a
   * folder created by a later build is a legitimate thing to configure. It follows that a path
   * resolving to a regular file is accepted too — the provider needs a directory, so it renders a
   * placeholder, but rejecting it here would refuse a path the check has no way to distinguish from
   * the not-created-yet case.
   */
  static @Nullable String invalidPathReason(@NotNull String path, @Nullable VirtualFile appRoot) {
    // Normalized once, up front: the row arrives exactly as typed, so without this the absolute-path
    // test reads a leading space where the other two read the trimmed path, and "  /etc" or
    // "\tC:\grails-app" is accepted.
    String normalized = FolderPaths.normalize(path);
    if (isAbsolutePath(normalized)) {
      return key("grails.settings.custom.folders.absolute.path");
    }
    if (escapesApplicationRoot(normalized)) {
      return key("grails.settings.custom.folders.escapes.application.root");
    }
    if (appRoot != null && !resolvesInsideApplicationRoot(appRoot, normalized)) {
      return key("grails.settings.custom.folders.outside.application.root");
    }
    return null;
  }

  /**
   * Takes a {@link FolderPaths#normalize normalized} path. That is why the backslash branch below is
   * dead — normalize has already turned every one into a forward slash, which is also why a Windows
   * drive path is caught by the {@code ':'} test rather than needing a separator of its own.
   */
  private static boolean isAbsolutePath(@NotNull String path) {
    if (path.isEmpty()) return false;
    char first = path.charAt(0);
    if (first == '/' || first == '\\') return true;
    return path.length() > 1 && path.charAt(1) == ':' && isAsciiLetter(first);
  }

  private static boolean isAsciiLetter(char c) {
    return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z');
  }

  private static boolean escapesApplicationRoot(@NotNull String normalizedPath) {
    int depth = 0;
    for (String segment : normalizedPath.split("/")) {
      if (segment.isEmpty() || segment.equals(".")) continue;
      if (segment.equals("..")) {
        depth--;
        if (depth < 0) return true;
      }
      else {
        depth++;
      }
    }
    return false;
  }

  private static boolean resolvesInsideApplicationRoot(@NotNull VirtualFile appRoot, @NotNull String path) {
    // Under a read action, exactly like applicationRoot() wraps the model lookup above: the VFS is
    // read here, and the platform has no EDT carve-out for that.
    return ReadAction.computeBlocking(() -> {
      VirtualFile resolved = appRoot.findFileByRelativePath(path);
      // A path that does not resolve yet is left alone: it becomes a placeholder in the Grails view.
      return resolved == null || VfsUtilCore.isAncestor(appRoot, resolved, false);
    });
  }

  private @Nullable VirtualFile applicationRoot() {
    return ReadAction.computeBlocking(() -> {
      GrailsApplicationManager manager = GrailsApplicationManager.getInstance(project);
      if (!manager.hasApplications()) return null;
      GrailsApplication application = manager.findApplication(ProjectUtil.guessProjectDir(project));
      return application == null ? null : application.getRoot();
    });
  }

  /** The description of the page, with the cap the user is editing interpolated into it. */
  static @NotNull String description(int max) {
    return GrailsBundle.message("grails.settings.custom.folders.description", max);
  }

  /**
   * The folders the Grails view renders for the given lists: this project's folders first, then
   * the global defaults, counting unique folders (after {@link FolderPaths#normalize(String)})
   * and keeping only the first {@code max} of them. The cap is passed in so the preview follows the
   * value being edited. Nothing is dropped for being over the cap here or in the rows: only the
   * preview and the rendered tree truncate.
   * <p>
   * Delegates to the merge the pane itself reads, {@link GrailsCustomFoldersProjectService#combine(List, List, int)}:
   * a preview computed by its own copy of the rule would promise folders the view does not render the
   * moment the two drifted. {@link FolderPaths#dedupe(Collection)} supplies the same blank and
   * null tolerance the rows already have.
   */
  static @NotNull List<String> combine(@NotNull List<String> project, @NotNull List<String> global, int max) {
    return GrailsCustomFoldersProjectService.combine(FolderPaths.dedupe(project), FolderPaths.dedupe(global), max);
  }

  /**
   * The over-cap warning for the given lists, or an empty string when the number of unique folders
   * is within {@code max}. It names the count of unique folders and the cap, and counts duplicates
   * (after {@link FolderPaths#normalize(String)}) once, exactly as {@link #combine(List, List, int)}
   * does, so it describes the folders the preview actually shows. The count is that merge with the
   * cap lifted, so it cannot disagree with it.
   */
  static @NotNull String overCapWarning(@NotNull List<String> project, @NotNull List<String> global, int max) {
    int combined = combine(project, global, Integer.MAX_VALUE).size();
    if (combined > max) {
      return GrailsBundle.message("grails.settings.custom.folders.over.cap", combined, max);
    }
    return "";
  }
}
