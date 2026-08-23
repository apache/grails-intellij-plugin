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

public final class PrintGrailsSettingsConstants {

  // See
  public static final String COMPILE = "Compile";
  public static final String RUNTIME = "Runtime";
  public static final String TESTS = "Test";
  public static final String PROVIDED = "Provided";
  public static final String BUILD = "Build";

  public static final String CUSTOM_PLUGIN_PREFIX = "grails.plugin.location.";

  public static final String SETTINGS_START_MARKER = "---=== IDEA Grails build settings ===---";
  public static final String SETTINGS_END_MARKER = "---=== End IDEA Grails build settings ===---";

  public static final String DEBUG_RUN_FORK = "grails.project.fork.run";
  public static final String DEBUG_TEST_FORK = "grails.project.fork.test";

  // --- === Copied from BuildConfig.groovy === ---
  public static final String WORK_DIR = "grails.work.dir";
  public static final String PROJECT_WORK_DIR = "grails.project.work.dir";
  public static final String PLUGINS_DIR = "grails.project.plugins.dir";
  public static final String GLOBAL_PLUGINS_DIR = "grails.global.plugins.dir";

  //public static final String PROJECT_RESOURCES_DIR = "grails.project.resource.dir";
  //public static final String PROJECT_CLASSES_DIR = "grails.project.class.dir";
  //public static final String PROJECT_TEST_CLASSES_DIR = "grails.project.test.class.dir";
  //public static final String PROJECT_WAR_FILE = "grails.project.war.file";
  //public static final String PROJECT_WAR_EXPLODED_DIR = "grails.project.war.exploded.dir";
  //public static final String PROJECT_SOURCE_DIR = "grails.project.source.dir";
  //public static final String PROJECT_WEB_XML_FILE = "grails.project.web.xml";
  //public static final String PROJECT_TEST_REPORTS_DIR = "grails.project.test.reports.dir";
  //public static final String PROJECT_TEST_SOURCE_DIR = "grails.project.test.source.dir";
  //public static final String PROJECT_TARGET_DIR = "grails.project.target.dir";

  private PrintGrailsSettingsConstants() {
  }

}
