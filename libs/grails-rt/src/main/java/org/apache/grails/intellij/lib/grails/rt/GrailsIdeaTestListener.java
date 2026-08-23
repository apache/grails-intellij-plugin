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

package org.apache.grails.intellij.lib.grails.rt;

import grails.build.GrailsBuildListener;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Maxim.Medvedev
 * @noinspection UseOfSystemOutOrSystemErr,UnusedDeclaration
 */
public class GrailsIdeaTestListener implements GrailsBuildListener {
  private final Map<String, Long> myProperties = new HashMap<>();
  private String myClassName;

  private PrintStream out;

  @Override
  public void receiveGrailsBuildEvent(String s, Object[] objects) {
    if (out == null) {
      out = System.out; // Save System.out. They change it during test running.
    }

    if ("TestCaseEnd".equals(s)) {
      out.println(objects[1]);
      testSuiteFinished((String)objects[0]);
    }

    else if ("TestCaseStart".equals(s)) {
      myClassName = (String)objects[0];
      testSuiteStarted(myClassName);
    }

    else if ("TestStart".equals(s)) {
      testStarted((String)objects[0]);
    }

    else if ("TestEnd".equals(s)) {
      testFinished((String)objects[0]);
    }

    else if ("TestFailure".equals(s)) {
      Object failure = objects[1];
      String message = failure instanceof Throwable ?
                       replaceEscapedSymbols(((Throwable)failure).getMessage()) :
                       String.valueOf(failure);
      String details = failure instanceof Throwable ? getStackTrace((Throwable)failure) : "none";
      String error = failure instanceof AssertionError || failure == null ? "error='true'" : "";
      out.println("\n##teamcity[testFailed name='" + replaceEscapedSymbols(((String)objects[0])) +
                  "' message='" + message +
                  "' details='" + details +
                  "' " + error + "]");
    }
  }

  private static String replaceEscapedSymbols(String s) {
    if (s == null) return null;
    return s.replaceAll("[\\|'\\[\\]]", "\\|$0").
      replaceAll("\n", "|n").
      replaceAll("\r", "|r");
  }

  static String getStackTrace(Throwable e) {
    StringWriter writer = new StringWriter();
    e.printStackTrace(new PrintWriter(writer));
    return replaceEscapedSymbols(writer.getBuffer().toString());
  }

  private void testFinished(String testName) {
    long duration = System.currentTimeMillis() - myProperties.get(testName).longValue();
    out.println("\n##teamcity[testFinished name='" + replaceEscapedSymbols(testName) + "' duration='" + duration + "']");
  }

  private void testStarted(String testName) {
    String testLocation = replaceEscapedSymbols(myClassName + '.' + testName);
    out.println("\n##teamcity[testStarted name='" +
                replaceEscapedSymbols(testName) +
                "' captureStandardOutput='false' locationHint='grails://methodName::" +
                testLocation +
                "']");
    myProperties.put(testName, Long.valueOf(System.currentTimeMillis()));
  }

  private void testSuiteStarted(String name) {
    out.println("\n##teamcity[testSuiteStarted name='" + name + "' locationHint='grails://className::" + name + "']");
    myProperties.put(name, Long.valueOf(System.currentTimeMillis()));
  }

  private void testSuiteFinished(String name) {
    long duration = System.currentTimeMillis() - myProperties.get(name).longValue();
    out.println("\n##teamcity[testSuiteFinished name='" + name + "' duration='" + duration + "']");
  }
}
