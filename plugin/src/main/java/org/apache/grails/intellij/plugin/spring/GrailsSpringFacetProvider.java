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
package org.apache.grails.intellij.plugin.spring;

import com.intellij.facet.ModifiableFacetModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.spring.facet.SpringFacet;
import com.intellij.spring.facet.SpringFileSet;
import com.intellij.util.Consumer;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.apache.grails.intellij.plugin.util.GrailsFacetProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public final class GrailsSpringFacetProvider implements GrailsFacetProvider {
  private static final @NonNls String GRAILS_FILESET = "Grails";

  private static final String[] configurationLocations = {"web-app/WEB-INF/applicationContext.xml", "grails-app/conf/spring/resources.xml"};

  @Override
  public void addFacets(Collection<Consumer<ModifiableFacetModel>> actions,
                        final Module module,
                        Collection<VirtualFile> roots) {
    final List<VirtualFile> configFiles = new ArrayList<>();

    for (VirtualFile root : roots) {
      for (String configurationLocation : configurationLocations) {
        final VirtualFile appContext = root.findFileByRelativePath(configurationLocation);
        if (appContext != null) {
          configFiles.add(appContext);
        }
      }
    }
    actions.add(model -> {
      Collection<SpringFacet> facets = model.getFacetsByType(SpringFacet.FACET_TYPE_ID);

      if (facets.isEmpty()) {
        var facetType = SpringFacet.getSpringFacetType();
        SpringFacet facet = facetType.createFacet(
          module, facetType.getPresentableName(), facetType.createDefaultConfiguration(), null
        );

        if (!configFiles.isEmpty()) {
          SpringFileSet fileSet = facet.addFileSet(GRAILS_FILESET, GRAILS_FILESET);

          for (VirtualFile configFile : configFiles) {
            fileSet.addFile(configFile);
          }
        }

        model.addFacet(facet);
      }
      else if (!configFiles.isEmpty()) {
        SpringFileSet fileSet = null;

        for (SpringFacet springFacet : facets) {
          for (SpringFileSet set : springFacet.getFileSets()) {
            if (GRAILS_FILESET.equals(set.getId())) {
              fileSet = set;
            }

            for (Iterator<VirtualFile> itr = configFiles.iterator(); itr.hasNext(); ) {
              VirtualFile file = itr.next();
              if (set.hasFile(file)) itr.remove();
            }
          }
        }

        if (fileSet == null) {
          SpringFacet facet = ContainerUtil.getFirstItem(facets);
          assert facet != null;
          fileSet = facet.addFileSet(GRAILS_FILESET, GRAILS_FILESET);
        }

        if (fileSet != null) {
          for (VirtualFile configFile : configFiles) {
            fileSet.addFile(configFile);
          }
        }
      }
    });
  }
}
