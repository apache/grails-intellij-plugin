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
package org.jetbrains.plugins.groovy.grails.gsp;

import com.intellij.openapi.module.Module;
import com.intellij.testFramework.DumbModeTestUtils;
import org.jetbrains.plugins.grails.lang.gsp.resolve.taglib.GspTagLibUtil;
import org.jetbrains.plugins.grails.lang.gsp.resolve.taglib.TagLibNamespaceDescriptor;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

import java.util.Map;
import java.util.Set;

/**
 * Opening a GSP while indexing is still running used to throw IndexNotReadyException: the platform's
 * XML machinery calls XmlTag.getNamespace() from the daemon, which reaches GspXmlRootTagImpl ->
 * GspTagLibUtil -> GrailsArtifact and hit the stub index in dumb mode.
 *
 * <p>The taglib set is now allowed to be partial while indexing, and every cache in that chain
 * depends on the DumbService modification tracker so the partial result cannot outlive indexing.
 */
public class GspTagLibDumbModeTest extends GrailsTestCase {

  /** The reported crash: any of these calls threw while indexing. */
  public void testTaglibLookupDoesNotThrowWhileIndexing() {
    addTaglib("""

class MyTagLib {
  static namespace = "my"
  def hello = { attrs -> }
}
""");
    addView("book/list.gsp", "<g:message code='x'/>");
    Module module = getModule();

    DumbModeTestUtils.runInDumbModeSynchronously(getProject(), () -> {
      Map<String, TagLibNamespaceDescriptor> taglibs = GspTagLibUtil.getTagLibClasses(module);
      assertNotNull(taglibs);
      // The default "g" prefix is unconditional, so GSP editing still resolves g: tags.
      assertTrue("default taglib prefix must survive dumb mode",
                 taglibs.containsKey(GspTagLibUtil.DEFAULT_TAGLIB_PREFIX));
      // The direct throw site from the reported stack.
      assertNotNull(GrailsArtifact.CONTROLLER.getInstances(module));
      assertNotNull(GrailsArtifact.TAGLIB.getInstances(module));
    });
  }

  /**
   * The degraded dumb-mode result must be discarded once indexing finishes, otherwise custom
   * prefixes would stay broken for the rest of the session. A custom-namespace taglib is the
   * signal: unavailable while dumb (its prefix cannot be resolved without the index), present
   * afterwards.
   *
   * <p>Note this asserts recovery end-to-end, not which cache dependency delivers it — the
   * fixture bumps {@code PsiModificationTracker} often enough that this test still passes with
   * the DumbService tracker removed. The tracker is kept because indexing finishing does not
   * inherently modify PSI, so in a real IDE it is the only dependency guaranteed to change.
   */
  public void testCustomPrefixAppearsAfterIndexing() {
    addTaglib("""

class MyTagLib {
  static namespace = "my"
  def hello = { attrs -> }
}
""");
    Module module = getModule();

    Map<String, TagLibNamespaceDescriptor> whileDumb =
      DumbModeTestUtils.computeInDumbModeSynchronously(getProject(), () -> GspTagLibUtil.getTagLibClasses(module));
    assertEquals("only the default prefix is resolvable while indexing",
                 Set.of(GspTagLibUtil.DEFAULT_TAGLIB_PREFIX), whileDumb.keySet());

    Map<String, TagLibNamespaceDescriptor> whenSmart = GspTagLibUtil.getTagLibClasses(module);
    assertTrue("dumb-mode result must not outlive indexing; got " + whenSmart.keySet(),
               whenSmart.containsKey("my"));
  }
}
