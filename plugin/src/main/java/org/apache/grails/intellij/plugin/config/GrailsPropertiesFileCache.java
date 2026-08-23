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

import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.impl.PropertiesFileImpl;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nullable;

class GrailsPropertiesFileCache {

  private PropertiesFileImpl myPropertiesFile;

  private long myModificationCount;

  private String myAppName;

  GrailsPropertiesFileCache(GrailsStructure grailsStructure) {
    PropertiesFileImpl prop = null;

    VirtualFile child = grailsStructure.getAppRoot().findChild("application.properties");
    if (child != null) {
      PsiFile file = grailsStructure.getManager().findFile(child);
      if (file instanceof PropertiesFileImpl) {
        prop = (PropertiesFileImpl)file;
      }
    }

    if (prop != null) {
      myPropertiesFile = prop;
      myModificationCount = prop.getModificationStamp();

      IProperty appNameProperty = prop.findPropertyByKey("app.name");
      if (appNameProperty != null) {
        String value = appNameProperty.getValue();
        if (!StringUtil.isEmptyOrSpaces(value)) {
          myAppName = value.trim();
        }
      }
    }
  }

  boolean isOutdated() {
    return myPropertiesFile != null && (!myPropertiesFile.isValid() || myModificationCount != myPropertiesFile.getModificationStamp());
  }

  public @Nullable String getAppName() {
    return myAppName;
  }

}
