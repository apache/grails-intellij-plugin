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
package org.apache.grails.intellij.module.coverage;

import com.intellij.coverage.JavaCoverageEngineExtension;
import com.intellij.execution.configurations.RunConfigurationBase;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.runner.GrailsRunConfiguration;
import org.apache.grails.intellij.plugin.runner.GrailsRunnerSetup;

public final class GrailsCoverageExtension extends JavaCoverageEngineExtension {

  @Override
  public boolean isApplicableTo(@Nullable RunConfigurationBase<?> conf) {
    return conf instanceof GrailsRunConfiguration configuration
           && GrailsRunnerSetup.canPassVmArgs(configuration.getGrailsApplicationNullable());
  }
}
