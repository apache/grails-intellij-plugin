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

import com.intellij.util.xmlb.XmlSerializer;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.jdom.Element;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * The choice of the command line of the projects of Grails 9 and later is kept with the project, and starts out
 * automatic.
 */
public class GrailsCommandLineSettingsTest extends GrailsTestCase {

  private GrailsCommandLineSettings settings() {
    return GrailsCommandLineSettings.getInstance(getProject());
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      settings().setChoice(GrailsCommandLineSettings.Choice.AUTOMATIC);
    }
    catch (Throwable e) {
      addSuppressedException(e);
    }
    finally {
      super.tearDown();
    }
  }

  public void testTheChoiceIsAutomaticUntilAnotherIsMade() {
    assertEquals(GrailsCommandLineSettings.Choice.AUTOMATIC, settings().getChoice());
  }

  public void testTheChoiceIsKept() {
    settings().setChoice(GrailsCommandLineSettings.Choice.SHELL);
    Element stored = XmlSerializer.serialize(settings().getState());

    settings().setChoice(GrailsCommandLineSettings.Choice.AUTOMATIC);
    settings().loadState(XmlSerializer.deserialize(stored, GrailsCommandLineSettings.StateHolder.class));

    assertEquals(GrailsCommandLineSettings.Choice.SHELL, settings().getChoice());
  }

  public void testAChoiceThisVersionDoesNotKnowIsAutomatic() {
    GrailsCommandLineSettings.StateHolder state = new GrailsCommandLineSettings.StateHolder();
    for (String stored : new String[]{"GRAILS_10_COMMAND_LINE", "", null}) {
      settings().setChoice(GrailsCommandLineSettings.Choice.COMMAND_LINE);
      state.choice = stored;
      settings().loadState(state);
      assertEquals(String.valueOf(stored), GrailsCommandLineSettings.Choice.AUTOMATIC, settings().getChoice());
    }
  }

  public void testAChangeIsToldOnce() {
    AtomicInteger told = new AtomicInteger();
    getProject().getMessageBus().connect(getTestRootDisposable())
      .subscribe(GrailsCommandLineSettings.Listener.TOPIC, (GrailsCommandLineSettings.Listener)told::incrementAndGet);

    settings().setChoice(GrailsCommandLineSettings.Choice.COMMAND_LINE);
    settings().setChoice(GrailsCommandLineSettings.Choice.COMMAND_LINE);
    assertEquals("setting the same choice again changes nothing", 1, told.get());

    settings().setChoice(GrailsCommandLineSettings.Choice.AUTOMATIC);
    assertEquals(2, told.get());
  }
}
