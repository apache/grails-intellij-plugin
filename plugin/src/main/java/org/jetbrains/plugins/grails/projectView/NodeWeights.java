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

package org.jetbrains.plugins.grails.projectView;

public final class NodeWeights {

  private NodeWeights() {}

  public static final int DOMAIN_CLASSES_FOLDER = 20;
  public static final int CONTROLLERS_FOLDER = 30;
  public static final int VIEWS_FOLDER = 40;
  public static final int SERVICES_FOLDER = 50;
  public static final int CONFIG_FOLDER = 60;
  public static final int OTHER_GRAILS_APP_FOLDER = 64;
  public static final int WEB_APP_FOLDER = 65;
  public static final int SRC_FOLDERS = 70;
  public static final int TESTS_FOLDER = 80;
  public static final int TAGLIB_FOLDER = 90;
  public static final int FOLDER = 100;
}
