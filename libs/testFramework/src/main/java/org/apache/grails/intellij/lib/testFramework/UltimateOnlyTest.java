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
package org.apache.grails.intellij.lib.testFramework;

/**
 * JUnit 4 category marker for tests that can only load and run when at least one Ultimate-only
 * plugin is installed on the test IDE sandbox (Spring, JSP, Java EE, persistence...). The Community
 * Edition test task ({@code :plugin:testIdeCe}) excludes this category wholesale, since the class
 * files themselves import classes the Community classpath simply does not have.
 */
public interface UltimateOnlyTest {
}