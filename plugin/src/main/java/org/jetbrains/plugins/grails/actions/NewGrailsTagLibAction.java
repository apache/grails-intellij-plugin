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

package org.jetbrains.plugins.grails.actions;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.artefact.api.GrailsArtefactHandler;
import org.jetbrains.plugins.grails.artefact.impl.TaglibArtefactHandler;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.grails.util.version.Version;

import java.util.List;

public class NewGrailsTagLibAction extends NewGrailsXXXAction {

  public NewGrailsTagLibAction() {
    super("action.Grails.NewTagLib.text");
  }

  @Override
  protected @NotNull String getCommand(@NotNull GrailsApplication application) {
    if (application.getGrailsVersion().isAtLeast(Version.GRAILS_3_0)) {
      return "create-taglib";
    }
    else {
      return "create-tag-lib";
    }
  }

  @Override
  protected @Nullable VirtualFile getTargetDirectory(@NotNull GrailsApplication application) {
    return GrailsArtifact.TAGLIB.findDirectory(application);
  }

  @Override
  protected void doAction(@NotNull GrailsApplication application, @NotNull String name) {
    name = StringUtil.trimEnd(name, GrailsArtifact.TAGLIB.suffix);
    super.doAction(application, name);
  }

  @Override
  protected void fillGeneratedNamesList(@NotNull String name, @NotNull List<String> names) {
    name = StringUtil.trimEnd(name, GrailsArtifact.TAGLIB.suffix);
    names.add("grails-app/taglib/" + canonicalize(name) + "TagLib.groovy");
    names.add("test/unit/" + canonicalize(name) + "TagLibSpec.groovy");
    names.add(GrailsUtils.GRAILS_INTEGRATION_TESTS + canonicalize(name) + "TagLibTests.groovy");
  }

  @Override
  protected @Nullable GrailsArtefactHandler getArtefactHandler() {
    return TaglibArtefactHandler.INSTANCE;
  }
}
