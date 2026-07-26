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
package org.apache.grails.intellij.plugin.editor;

import com.intellij.diagnostic.LoadingState;
import com.intellij.ide.actions.DistractionFreeModeController;
import com.intellij.ide.ui.UISettings;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.GsonConstants;
import org.apache.grails.intellij.plugin.fileType.GspFileType;
import org.apache.grails.intellij.plugin.util.GrailsArtifact;
import org.apache.grails.intellij.plugin.util.GrailsUtils;
import org.jetbrains.plugins.groovy.GroovyFileType;
import org.jetbrains.plugins.groovy.util.GroovyUtils;

/**
 * Which editors get the Grails artefact toolbar, and whether the user wants it at all.
 *
 * @see GrailsEditorDecorator
 * @see ViewGrailsEditorToolbarAction
 */
public final class GrailsEditorToolbar {

  private GrailsEditorToolbar() {
  }

  /** Artefact types whose editors carry the toolbar. */
  public static final GrailsArtifact[] DECORATED_ARTEFACT_TYPES = {
    GrailsArtifact.DOMAIN,
    GrailsArtifact.CONTROLLER,
    GrailsArtifact.SERVICE
  };

  private static final String SHOW_TOOLBAR_PROPERTY = "grails.show.editor.toolbar";

  private static final boolean SHOW_TOOLBAR_DEFAULT =
    Boolean.parseBoolean(System.getProperty(SHOW_TOOLBAR_PROPERTY, "true"));

  /**
   * Mirrors what {@code propComponentProperty} did for the Kotlin {@code var} this replaces: read
   * from the application-level {@link PropertiesComponent}, fall back to the default before
   * components are loaded, and store nothing when the value equals the default.
   */
  public static boolean isShowEditorToolbar() {
    if (!LoadingState.COMPONENTS_LOADED.isOccurred()) return SHOW_TOOLBAR_DEFAULT;
    return PropertiesComponent.getInstance().getBoolean(SHOW_TOOLBAR_PROPERTY, SHOW_TOOLBAR_DEFAULT);
  }

  public static void setShowEditorToolbar(boolean value) {
    PropertiesComponent.getInstance().setValue(SHOW_TOOLBAR_PROPERTY, value, SHOW_TOOLBAR_DEFAULT);
  }

  public static boolean shouldBeDecorated(@NotNull VirtualFile file,
                                          @NotNull FileEditor fileEditor,
                                          @NotNull Project project) {
    return shouldBeDecorated(fileEditor) && shouldBeDecorated(project, file);
  }

  static boolean shouldBeDecorated(@NotNull FileEditor fileEditor) {
    return fileEditor instanceof TextEditor
           && !UISettings.getInstance().getPresentationMode()
           && !DistractionFreeModeController.isDistractionFreeModeEnabled();
  }

  static boolean shouldBeDecorated(@NotNull Project project, @NotNull VirtualFile file) {
    var fileType = file.getFileType();
    if (fileType == GroovyFileType.GROOVY_FILE_TYPE) {
      if (GsonConstants.EXTENSION.equals(file.getExtension()) || GrailsUtils.isInGrailsTests(file, project)) {
        return true;
      }
      else {
        PsiClass classDefinition = GroovyUtils.getPublicClass(project, file);
        GrailsArtifact artifact = GrailsArtifact.getType(classDefinition);
        for (GrailsArtifact decorated : DECORATED_ARTEFACT_TYPES) {
          if (decorated == artifact) return true;
        }
      }
    }
    else if (fileType == GspFileType.GSP_FILE_TYPE || "jsp".equals(file.getExtension())) {
      String controllerName = GrailsUtils.getControllerNameByGsp(file);
      if (controllerName != null && !"layouts".equals(controllerName) && StringUtil.isJavaIdentifier(controllerName)) {
        return true;
      }
    }
    return false;
  }
}
