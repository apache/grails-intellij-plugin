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

package org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.gtag;

import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.util.TagSetRuleProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.gtag.GspGrailsTag;

public final class GspTagRuleProvider extends TagSetRuleProvider {

  @Override
  protected String getNamespace(@NotNull XmlTag tag) {
    return tag instanceof GspGrailsTag && tag.getName().startsWith("g:") ? "g" : null;
  }

  @Override
  protected void initMap(TagsRuleMap map, @NotNull String version) {
    map.add("if", requireAttr("test", "env"));
    map.add("elseif", requireAttr("test", "env"));
    map.add("include", shouldHaveParams());
    map.add("applyLayout", shouldHaveParams(), unusedIfPresent("url", "view", "template"));
    map.add("render", shouldHaveParams(), unusedIfPresent("plugin", "contextPath"));

    map.add("resource", shouldHaveParams(), unusedIfPresent("plugin", "contextPath"), unusedIfPresent("base", "absolute"));
    map.put("createLinkTo", map.get("resource"));

    map.add("link", unusedIfPresent("base", "absolute"));
    map.add("createLink", unusedAllIfPresent("uri", "base", "absolute"), unusedIfPresent("base", "absolute"));

    map.add("sortableColumn", requireAttr("title", "titleKey"));
  }

}

