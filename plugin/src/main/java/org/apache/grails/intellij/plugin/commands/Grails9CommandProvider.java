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

package org.apache.grails.intellij.plugin.commands;

import org.apache.grails.intellij.plugin.structure.Grails9Projects;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * The commands of a project of Grails 9 and later: those {@code grails help --json} lists in the project
 * ({@link Grails9CommandCatalog}), so that commands Grails 9 removed are not offered and those of the
 * application and its plugins are. None when its commands run in the Grails shell ({@link Grails9CommandLines}), whose
 * commands {@link Grails3CommandProvider} offers.
 */
final class Grails9CommandProvider implements GrailsCommandProvider {
  @Override
  public @NotNull Collection<String> addCommands(@NotNull GrailsApplication application) {
    if (!Grails9Projects.isGrails9OrLater(application)) return Collections.emptyList();
    return Grails9CommandCatalog.getInstance(application.getProject()).getDocument(application).getCommandNames();
  }
}
