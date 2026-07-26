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

package org.apache.grails.intellij.plugin.addins.js;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.RequiredElement;
import com.intellij.util.xmlb.annotations.Attribute;

/**
 * Extension bean contributing the qualified name of a GSP tag (e.g. {@code asset:script}) whose
 * body should be treated as inline JavaScript — for language injection and cross-tag symbol
 * visibility. Core recognizes {@code g:javascript} and {@code r:script} directly; optional plugin
 * modules contribute additional tags through this extension point so third-party taglib knowledge
 * stays out of core.
 *
 * @see JavaScriptIntegrationUtil#isJsInjectionTag(String)
 */
public final class GspJsInjectionTagBean {

  public static final ExtensionPointName<GspJsInjectionTagBean> EP_NAME =
    ExtensionPointName.create("org.intellij.grails.gspJsInjectionTag");

  /** Qualified GSP tag name, including the namespace prefix, e.g. {@code asset:script}. */
  @Attribute("name")
  @RequiredElement
  public String name;
}
