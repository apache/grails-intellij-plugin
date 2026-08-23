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
package org.apache.grails.intellij.plugin.feature.linkable;

import com.intellij.openapi.util.Key;

/**
 * Shared between the transformation that synthesises {@code link(Map)} and the provider that
 * supplies its named arguments: the marker lets the provider recognise the synthetic method.
 */
final class Linkable {

  private Linkable() {
  }

  static final String LINK_FQN = "grails.rest.Link";

  static final Key<Object> LINK_METHOD_KEY = Key.create("grails.linkable.method.key");

  static final Object LINK_METHOD_MARKER = new Object();
}
