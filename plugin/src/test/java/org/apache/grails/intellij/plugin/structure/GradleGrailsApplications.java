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

package org.apache.grails.intellij.plugin.structure;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.extensions.LoadingOrder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.PlatformTestUtil;
import org.apache.grails.intellij.lib.gradle.tooling.builder.GrailsCommandLine;
import org.apache.grails.intellij.plugin.gradle.GrailsModuleData;
import org.apache.grails.intellij.plugin.util.version.Version;
import org.apache.grails.intellij.plugin.util.version.VersionImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.util.List;

/**
 * Grails applications as the Gradle import describes them ({@link Grails3Application}), of any version, for tests of
 * what depends on the version and on the class path the import resolved for the Grails commands. A Gradle import of
 * each version would need that version published; the model is all the code under test reads.
 */
public final class GradleGrailsApplications {

  public static final String PLUGIN_ID = "org.apache.grails.gradle.grails-web";

  private GradleGrailsApplications() {
  }

  /**
   * @param grailsVersion     the {@code grailsVersion} of the project
   * @param commandsClassPath what the import resolved to run the commands: the Grails shell up to Grails 8, the
   *                          command line from Grails 9 on (and no shell); empty when it could not be resolved
   */
  public static @NotNull Grails3Application application(@NotNull Project project,
                                                        @NotNull VirtualFile root,
                                                        @NotNull String grailsVersion,
                                                        @NotNull List<String> commandsClassPath) {
    return GrailsCommandLine.isUsedBy(grailsVersion)
           ? application(project, root, grailsVersion, commandsClassPath, List.of())
           : application(project, root, grailsVersion, List.of(), commandsClassPath);
  }

  /**
   * @param commandLineClassPath what the import resolved of the command line of Grails 9 and later
   * @param shellClassPath       what it resolved of the Grails shell of the version
   */
  public static @NotNull Grails3Application application(@NotNull Project project,
                                                        @NotNull VirtualFile root,
                                                        @NotNull String grailsVersion,
                                                        @NotNull List<String> commandLineClassPath,
                                                        @NotNull List<String> shellClassPath) {
    return new GradleGrailsApplication(project, root, grailsVersion, commandLineClassPath, shellClassPath, null);
  }

  /**
   * An application of Grails 9 whose Gradle project has {@code gradlePath} in its build, {@code :} for the root project.
   */
  public static @NotNull Grails3Application applicationAt(@NotNull Project project,
                                                          @NotNull VirtualFile root,
                                                          @NotNull String grailsVersion,
                                                          @NotNull String gradlePath) {
    return new GradleGrailsApplication(project, root, grailsVersion, List.of(), List.of(), gradlePath);
  }

  /**
   * Has {@link GrailsApplicationManager} take the application at {@code root} for one the Gradle import describes, of
   * {@code grailsVersion}, until {@code parentDisposable} is disposed.
   */
  public static void register(@NotNull Project project,
                              @NotNull VirtualFile root,
                              @NotNull String grailsVersion,
                              @NotNull List<String> commandsClassPath,
                              @NotNull Disposable parentDisposable) {
    GrailsApplicationProvider.APPLICATION_PROVIDER.getPoint().registerExtension(new GrailsApplicationProvider() {
      @Override
      public @Nullable GrailsApplication createApplication(@NotNull Project p, @NotNull VirtualFile r) {
        return r.equals(root) ? application(p, r, grailsVersion, commandsClassPath) : null;
      }
    }, LoadingOrder.FIRST, parentDisposable);
    PlatformTestUtil.waitForFuture(GrailsApplicationManager.getInstance(project).queueUpdate(), 60_000);
  }

  private static final class GradleGrailsApplication extends GrailsApplicationBase implements Grails3Application {
    private final GrailsModuleData myData;
    private final @Nullable String myGradlePath;

    private GradleGrailsApplication(@NotNull Project project, @NotNull VirtualFile root, @NotNull String grailsVersion,
                                    @NotNull List<String> commandLineClassPath, @NotNull List<String> shellClassPath,
                                    @Nullable String gradlePath) {
      super(project, root);
      myGradlePath = gradlePath;
      myData = new GrailsModuleData(GradleConstants.SYSTEM_ID, grailsVersion, PLUGIN_ID, shellClassPath, commandLineClassPath);
    }

    @Override
    public @NotNull GrailsModuleData getGradleData() {
      return myData;
    }

    @Override
    public @Nullable String getGradlePath() {
      return myGradlePath;
    }

    @Override
    public @NotNull String getName() {
      return getRoot().getName();
    }

    @Override
    public @NotNull Version getGrailsVersion() {
      return new VersionImpl(myData.getGrailsVersion());
    }

    @Override
    public @NotNull GlobalSearchScope getScope(boolean includeDependencies, boolean testsOnly) {
      return GlobalSearchScope.projectScope(getProject());
    }

    @Override
    public boolean equals(Object other) {
      return super.equals(other) && myData.equals(((GradleGrailsApplication)other).myData);
    }

    @Override
    public int hashCode() {
      return 31 * super.hashCode() + myData.hashCode();
    }
  }
}
