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

package org.apache.grails.intellij.plugin;


import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import com.intellij.psi.PsiFile;

public class GrailsGantTest extends GrailsTestCase {
  public void testResolve() {
    PsiFile script = myFixture.addFileToProject("scripts/SomeScript.groovy", """
      target(main: '''Script used to interact with remote Tomcat. The following subcommands are available:
      grails tomcat deploy - Deploy to a tomcat server
      grails tomcat undeploy - Undeploy from a tomcat server
      ''') {
          depends("aaa", "bbb", "ccc")
          // Do somthing
        }
      }
      setDefaultTarget("main")
      """);

    GrailsTestCase.checkResolve(script);
  }
}
