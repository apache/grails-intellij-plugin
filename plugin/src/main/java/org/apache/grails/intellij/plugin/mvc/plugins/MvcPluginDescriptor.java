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

package org.apache.grails.intellij.plugin.mvc.plugins;

import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MvcPluginDescriptor {

  public static final MvcPluginDescriptor[] EMPTY_ARRAY = new MvcPluginDescriptor[0];

  private final String myName;

  private Release myLastRelease;

  private final List<Release> releases = new ArrayList<>();

  public MvcPluginDescriptor(@NotNull String name) {
    this.myName = name;
  }

  public @NlsSafe String getName() {
    return myName;
  }

  public Release getLastRelease() {
    return myLastRelease;
  }

  public void setLastRelease(Release lastRelease) {
    myLastRelease = lastRelease;
  }

  public List<Release> getReleases() {
    return releases;
  }

  public @Nullable @NlsSafe String getLatestVersion() {
    return myLastRelease == null ? null : myLastRelease.getVersion();
  }

  public @Nullable @NlsSafe String getTitle() {
    return myLastRelease == null ? null : myLastRelease.getTitle();
  }

  @Override
  public String toString() {
    return myName;
  }

  public static class Release {
    private final MvcPluginDescriptor myPlugin;
    private final String myVersion;
    private final String myType;
    private final String myTitle;
    private final String myAuthor;
    private final String myDescription;
    private final String myEmail;
    private final String myZipRelease;
    private final String myDocumentation;

    public Release(MvcPluginDescriptor plugin, String version, String type, String title, String author, String description, String email, String zipRelease, String documentation) {
      myPlugin = plugin;
      myTitle = title;
      myType = type;
      myVersion = version;
      myAuthor = author;
      myDescription = description;
      myEmail = email;
      myZipRelease = zipRelease;
      myDocumentation = documentation;
    }

    public @NlsSafe String getTitle() {
      return myTitle;
    }

    public @NlsSafe String getAuthor() {
      return myAuthor;
    }

    public @NlsSafe String getDescription() {
      return myDescription;
    }

    public @NlsSafe String getEmail() {
      return myEmail;
    }

    public @NlsSafe String getZipRelease() {
      return myZipRelease;
    }

    public @NlsSafe String getDocumentation() {
      return myDocumentation;
    }

    public @NlsSafe String getVersion() {
      return myVersion;
    }

    public String getType() {
      return myType;
    }

    public MvcPluginDescriptor getPlugin() {
      return myPlugin;
    }
  }
}
