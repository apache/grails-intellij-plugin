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

package org.apache.grails.intellij.plugin.gsp;

import com.intellij.openapi.extensions.LoadingOrder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.PsiTestUtil;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.apache.grails.intellij.lib.testFramework.GrailsTestUtil;
import org.apache.grails.intellij.plugin.lang.gsp.psi.groovy.api.GrGspClass;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.structure.GrailsApplicationManager;
import org.apache.grails.intellij.plugin.structure.GrailsApplicationProvider;
import org.apache.grails.intellij.plugin.structure.impl.OldGrailsModuleBasedApplication;
import org.apache.grails.intellij.plugin.util.version.Version;
import org.apache.grails.intellij.plugin.util.version.VersionImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.util.TestUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * The implicit variables of a GSP and the class it extends, on the Grails 3+ API: Grails 3 to 6 with the
 * {@code javax.servlet} API, Grails 7 and later with {@code jakarta.servlet}, and the variables later releases added,
 * which follow the application's Grails version. Grails 2 is covered by {@link GspGrails2ImplicitVariablesTest}.
 * <p>
 * The Grails and servlet classes are source stubs rather than jars. Each request stub declares a method only its own
 * servlet API has: Servlet 6 removed {@code isRequestedSessionIdFromUrl()} and added {@code getRequestId()}.
 */
public class GspImplicitVariablesTest extends GrailsTestCase {

  private static final String JAVAX = "javax.servlet";

  private static final String JAKARTA = "jakarta.servlet";

  private static final String JAVAX_ONLY_REQUEST_METHOD = "boolean isRequestedSessionIdFromUrl();";

  private static final String JAKARTA_ONLY_REQUEST_METHOD = "String getRequestId();";

  /** The Grails version the test application reports, or {@code null} to leave the application to the base class. */
  private volatile @Nullable Version myGrailsVersion;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    GrailsApplicationProvider.APPLICATION_PROVIDER.getPoint()
      .registerExtension(new VersionedApplicationProvider(), LoadingOrder.FIRST, myFixture.getTestRootDisposable());
  }

  /**
   * Groovy only. The base class would add the Grails 2 jars, putting both Grails APIs on the classpath, which no
   * real project has.
   */
  @Override
  protected void configureGrails(@NotNull Module module, @NotNull ModifiableRootModel model, ContentEntry contentEntry) {
    PsiTestUtil.addLibrary(model, "GROOVY", GrailsTestUtil.getMockGrailsLibraryHome(), TestUtils.GROOVY_JAR);
  }

  public void testNoNamespaceOrServletContextBeforeGrails6_2_1() {
    useGrailsVersion("6.2.0");
    addGrails3Api();
    addServletApi(JAVAX, JAVAX_ONLY_REQUEST_METHOD);

    GspImplicitVariableAssertions.assertVariableTypes(this, grails3Types(JAVAX), Set.of());
    GspImplicitVariableAssertions.assertNoVariables(this, "namespace", "servletContext");
  }

  public void testNamespaceFromGrails6_2_1() {
    useGrailsVersion("6.2.1");
    addGrails3Api();
    addServletApi(JAVAX, JAVAX_ONLY_REQUEST_METHOD);

    GspImplicitVariableAssertions.assertVariableTypes(this, Map.of("namespace", "java.lang.String"), Set.of());
    GspImplicitVariableAssertions.assertNoVariables(this, "servletContext");
  }

  public void testNoServletContextBeforeGrails8_0_0_M6() {
    useGrailsVersion("8.0.0-M5");
    addGrails3Api();
    addServletApi(JAKARTA, JAKARTA_ONLY_REQUEST_METHOD);

    GspImplicitVariableAssertions.assertVariableTypes(this, Map.of("namespace", "java.lang.String"), Set.of());
    GspImplicitVariableAssertions.assertNoVariables(this, "servletContext");
  }

  public void testServletContextFromGrails8_0_0_M6() {
    useGrailsVersion("8.0.0-M6");
    addGrails3Api();
    addServletApi(JAKARTA, JAKARTA_ONLY_REQUEST_METHOD);

    GspImplicitVariableAssertions.assertVariableTypes(this, Map.of(
      "namespace", "java.lang.String",
      "servletContext", "jakarta.servlet.ServletContext"), Set.of());
  }

  public void testGrails3To6TypesUseTheJavaxServletApi() {
    addGrails3Api();
    addServletApi(JAVAX, JAVAX_ONLY_REQUEST_METHOD);

    GspImplicitVariableAssertions.assertVariableTypes(this, grails3Types(JAVAX), Set.of());
  }

  public void testGrails7TypesUseTheJakartaServletApi() {
    addGrails3Api();
    addServletApi(JAKARTA, JAKARTA_ONLY_REQUEST_METHOD);

    GspImplicitVariableAssertions.assertVariableTypes(this, grails3Types(JAKARTA), Set.of());
  }

  public void testJakartaServletApiWinsWhenTheJavaxApiIsAlsoOnTheClasspath() {
    addGrails3Api();
    addServletApi(JAVAX, JAVAX_ONLY_REQUEST_METHOD);
    addServletApi(JAKARTA, JAKARTA_ONLY_REQUEST_METHOD);

    GspImplicitVariableAssertions.assertVariableTypes(this, grails3Types(JAKARTA), Set.of());
  }

  public void testRequestCompletionOffersTheJakartaRequestMembers() {
    addGrails3Api();
    addServletApi(JAVAX, JAVAX_ONLY_REQUEST_METHOD);
    addServletApi(JAKARTA, JAKARTA_ONLY_REQUEST_METHOD);

    configureByView("ccc/index.gsp", "${request.<caret>}");

    checkCompletion("requestId");
    checkNonExistingCompletionVariants("requestedSessionIdFromUrl", "isRequestedSessionIdFromUrl");
  }

  public void testPageExtendsTheGrails3GroovyPage() {
    addGrails3Api();

    GrGspClass page = GspImplicitVariableAssertions.pageClass(addView("ccc/index.gsp", "${this}"));

    PsiClass superClass = page.getSuperClass();
    assertNotNull(superClass);
    assertEquals("org.grails.gsp.GroovyPage", superClass.getQualifiedName());
    assertEquals("org.grails.gsp.GroovyPage", page.getSuperTypes()[0].getCanonicalText());
  }

  public void testThisCompletionOffersTheGrails3GroovyPageMembers() {
    addGrails3Api();

    PsiFile gsp = configureByView("ccc/index.gsp", "${this.<caret>}");

    checkCompletion(gsp, "outputStack");
  }

  private static Map<String, String> grails3Types(String servletPackage) {
    return Map.ofEntries(
      Map.entry("actionName", "java.lang.String"),
      Map.entry("controllerName", "java.lang.String"),
      Map.entry("application", servletPackage + ".ServletContext"),
      Map.entry("request", servletPackage + ".http.HttpServletRequest"),
      Map.entry("response", servletPackage + ".http.HttpServletResponse"),
      Map.entry("session", servletPackage + ".http.HttpSession"),
      Map.entry("pageScope", "groovy.lang.Binding"),
      Map.entry("applicationContext", "org.springframework.context.ApplicationContext"),
      Map.entry("flash", "grails.web.mvc.FlashScope"),
      Map.entry("grailsApplication", "grails.core.GrailsApplication"),
      Map.entry("out", "java.io.Writer"),
      Map.entry("params", "grails.web.servlet.mvc.GrailsParameterMap"),
      Map.entry("webRequest", "org.grails.web.servlet.mvc.GrailsWebRequest"),
      Map.entry("exception", "org.grails.web.errors.GrailsWrappedRuntimeException"));
  }

  /** Stubs of the Grails 3+ classes behind the implicit variables, with the package names Grails 3.3 to 8 use. */
  private void addGrails3Api() {
    // Both are real GroovyPage methods: getOut() declares `out`, and getOutputStack() shows completion reaches the
    // superclass.
    myFixture.addClass("""
                         package org.grails.gsp;
                         public abstract class GroovyPage extends groovy.lang.Script {
                           public final java.io.Writer getOut() { return null; }
                           public final Object getOutputStack() { return null; }
                         }
                         """);
    myFixture.addClass("package org.grails.web.servlet.mvc; public class GrailsWebRequest {}");
    myFixture.addClass("package grails.web.servlet.mvc; public class GrailsParameterMap {}");
    myFixture.addClass("package grails.web.mvc; public interface FlashScope {}");
    myFixture.addClass("package grails.core; public interface GrailsApplication {}");
    myFixture.addClass("package org.grails.web.errors; public class GrailsWrappedRuntimeException extends RuntimeException {}");
    myFixture.addClass("package org.springframework.context; public interface ApplicationContext {}");
  }

  private void addServletApi(String servletPackage, String requestMethod) {
    myFixture.addClass("package " + servletPackage + "; public interface ServletContext {}");
    myFixture.addClass("package " + servletPackage + ".http; public interface HttpServletRequest { " + requestMethod + " }");
    myFixture.addClass("package " + servletPackage + ".http; public interface HttpServletResponse {}");
    myFixture.addClass("package " + servletPackage + ".http; public interface HttpSession {}");
  }

  /** Makes the test application report this Grails version, as a Gradle import of that release does. */
  private void useGrailsVersion(String version) {
    myGrailsVersion = new VersionImpl(version);
    PlatformTestUtil.waitForFuture(GrailsApplicationManager.getInstance(getProject()).queueUpdate(), 5 * 60 * 1000);

    Collection<GrailsApplication> applications = GrailsApplicationManager.getInstance(getProject()).getApplications();
    assertSize(1, applications);
    assertEquals(version, applications.iterator().next().getGrailsVersion().toString());
  }

  /** Creates the test application, like {@code TestGrailsApplicationProvider}, with the version the test chose. */
  private final class VersionedApplicationProvider extends GrailsApplicationProvider {

    @Override
    public @Nullable GrailsApplication createApplication(@NotNull Project project, @NotNull VirtualFile root) {
      Version version = myGrailsVersion;
      if (version == null || root.findChild("application.properties") == null) return null;
      Module module = ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(root);
      return module == null ? null : new VersionedApplication(module, root, version);
    }
  }

  private static final class VersionedApplication extends OldGrailsModuleBasedApplication {

    private final @NotNull Version myVersion;

    VersionedApplication(@NotNull Module module, @NotNull VirtualFile root, @NotNull Version version) {
      super(module, root);
      myVersion = version;
    }

    @Override
    public @NotNull String getName() {
      return getModule().getName();
    }

    @Override
    public @NotNull Version getGrailsVersion() {
      return myVersion;
    }

    // The version takes part, so that a new version replaces the application on the next update.
    @Override
    public boolean equals(Object other) {
      return super.equals(other) && myVersion.equals(((VersionedApplication)other).myVersion);
    }

    @Override
    public int hashCode() {
      return 31 * super.hashCode() + myVersion.hashCode();
    }
  }
}
