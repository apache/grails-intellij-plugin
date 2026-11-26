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

package org.apache.grails.intellij.plugin.commands;

import junit.framework.TestCase;
import org.apache.grails.intellij.lib.testFramework.GrailsTestUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Reads documents that {@code grails help --json} of Grails 9.0.0-SNAPSHOT wrote in a web application it had just
 * created ({@code grails create-app com.example.shop}):
 * <ul>
 *   <li>{@code web-app.json} after {@code ./gradlew grailsCommandCatalog} had listed the commands of the application
 *       and its plugins, as recorded;</li>
 *   <li>{@code web-app-commands-not-listed.json} before, with the options, parameters and relations of each command
 *       left out to keep the file small.</li>
 * </ul>
 * The path of the project in both is {@code /home/me/shop}.
 */
public class Grails9HelpDocumentTest extends TestCase {

  static String recorded(String name) throws IOException {
    return Files.readString(Path.of(GrailsTestUtil.getTestRootPath("/testdata/grails9/help-json/" + name)), StandardCharsets.UTF_8);
  }

  public void testTheCommandsOfAWebApplication() throws IOException {
    Grails9HelpDocument document = Grails9HelpDocument.parse(recorded("web-app.json"));

    assertTrue(document.isComplete());
    assertEquals("/home/me/shop", document.getProjectDirectory());
    List<String> names = document.getCommandNames();
    assertTrue(names.toString(), names.containsAll(List.of(
      "create-app", "feature", "project", "gradle", "help",
      "create-controller", "create-service", "create-domain-class", "create-taglib", "create-interceptor",
      "compile", "clean", "assemble", "test-app", "run-app", "stop-app", "console", "shell", "run-script", "run-tool", "create-tool",
      "update-wrapper")));
    assertTrue("aliases are typed as names: " + names, names.containsAll(List.of("war", "package")));
    assertTrue("the commands of the plugins of the application: " + names,
               names.containsAll(List.of("generate-all", "generate-controller", "generate-views", "url-mappings-report", "schema-export")));
  }

  public void testCommandsOfTheGrailsShellThatGrails9DoesNotHaveAreNotOffered() throws IOException {
    List<String> names = Grails9HelpDocument.parse(recorded("web-app.json")).getCommandNames();
    for (String removed : List.of("open", "install", "create-profile", "list-profiles", "profile-info")) {
      assertFalse(removed, names.contains(removed));
    }
  }

  public void testWhatTheDocumentSaysOfACommand() throws IOException {
    Grails9HelpDocument document = Grails9HelpDocument.parse(recorded("web-app.json"));

    Grails9HelpDocument.Command controller = command(document, "create-controller");
    assertEquals("built-in", controller.origin());
    assertTrue(controller.project());
    assertFalse(controller.description().isEmpty());

    Grails9HelpDocument.Command assemble = command(document, "assemble");
    assertEquals(List.of("war", "package"), assemble.aliases());

    Grails9HelpDocument.Command feature = command(document, "feature");
    assertEquals(List.of("add", "list"), feature.commands().stream().map(Grails9HelpDocument.Command::name).toList());

    assertTrue(command(document, "generate-all").origin().startsWith("org.apache.grails:grails-scaffolding-cli:"));
    assertEquals("descriptor", command(document, "generate-all").registration());
    assertFalse(command(document, "generate-all").isBuiltIn());
    assertFalse(command(document, "generate-all").isScript());
    assertTrue(controller.isBuiltIn());
    assertEquals("a command of the command line itself is not registered by the build", "", controller.registration());
    assertEquals("a command of the plugin that has the name of one of the command line itself is not one: " + document.getDiagnostics(),
                 1, document.getCommands().stream().filter(command -> command.name().equals("install-templates")).count());
  }

  public void testBeforeTheBuildListedTheCommandsOfTheApplication() throws IOException {
    Grails9HelpDocument document = Grails9HelpDocument.parse(recorded("web-app-commands-not-listed.json"));

    assertFalse(document.isComplete());
    assertTrue(document.getDiagnostics().toString(), document.getDiagnostics().get(0).contains("grailsCommandCatalog"));
    assertTrue(document.getCommandNames().contains("create-controller"));
    assertFalse(document.getCommandNames().contains("generate-all"));
  }

  public void testAnotherFormatIsRefused() {
    for (String json : List.of("{\"format\": 2, \"commands\": []}", "{\"commands\": []}", "[]", "not json")) {
      try {
        Grails9HelpDocument.parse(json);
        fail(json);
      }
      catch (IOException expected) {
        // a reader of format 1 would misread a later format
      }
    }
  }

  /**
   * A document of a later format is one of the command line of Grails 9 all the same; anything else that is not of
   * format 1 is no document of it, as the output of an earlier build of Grails 9 is not.
   */
  public void testALaterFormatIsToldFromNoDocument() {
    for (String json : List.of("{\"format\": 2, \"commands\": []}", "{\"format\": 12345678901}")) {
      try {
        Grails9HelpDocument.parse(json);
        fail(json);
      }
      catch (Grails9HelpDocument.OtherFormatException e) {
        assertTrue(e.getFormat(), json.contains(e.getFormat()));
      }
      catch (IOException e) {
        fail(json + ": " + e);
      }
    }
    for (String json : List.of("{\"format\": \"1\"}", "{\"format\": 1.5}", "{\"commands\": []}", "[]", "Usage: grails")) {
      try {
        Grails9HelpDocument.parse(json);
        fail(json);
      }
      catch (Grails9HelpDocument.OtherFormatException e) {
        fail(json + " is no document of the command line: " + e);
      }
      catch (IOException expected) {
        // not of the command line of Grails 9
      }
    }
  }

  public void testTheOptionsOfTheCommandLineItself() throws IOException {
    assertEquals(List.of("-h", "--help", "--non-interactive", "--plain-output", "-x", "--stacktrace", "-v", "--verbose", "-V", "--version"),
                 Grails9HelpDocument.parse(recorded("web-app.json")).getOptionNames());
    assertEquals(List.of("--plain-output"), Grails9HelpDocument.parse("""
      {"format": 1, "options": [{"names": ["--plain-output"], "label": null}, {"names": []}, {"label": "no names"}, {"names": [7, ""]}]}
      """).getOptionNames());
    assertNull("a document without the member says nothing of the options",
               Grails9HelpDocument.parse("{\"format\": 1, \"commands\": []}").getOptionNames());
    assertNull(Grails9HelpDocument.EMPTY.getOptionNames());
  }

  public void testMembersThatAreNotKnownAreIgnored() throws IOException {
    Grails9HelpDocument document = Grails9HelpDocument.parse("""
      {"format": 1, "complete": true, "diagnostics": [], "projectDirectory": null, "future": {"a": 1},
       "commands": [{"name": "compile", "aliases": [], "newMember": 7},
                    {"aliases": ["nameless"]},
                    {"name": "", "aliases": []},
                    {"name": "clean"}]}
      """);

    assertNull(document.getProjectDirectory());
    assertEquals(List.of("compile", "clean"), document.getCommandNames());
    assertEquals("", command(document, "clean").origin());
  }

  public void testTheEmptyDocumentHasNoCommands() {
    assertEmpty(Grails9HelpDocument.EMPTY.getCommandNames());
    assertFalse(Grails9HelpDocument.EMPTY.isComplete());
  }

  public void testACommandIsFoundByItsNameAndByItsOtherNames() throws IOException {
    Grails9HelpDocument document = Grails9HelpDocument.parse(recorded("web-app.json"));

    assertSame(command(document, "assemble"), document.getCommand("assemble"));
    assertSame(command(document, "assemble"), document.getCommand("war"));
    assertSame(command(document, "schema-export"), document.getCommand("schema-export"));
    assertNull("a command of a command is not typed first", document.getCommand("add"));
    assertNull(document.getCommand("dbm-update"));
    assertNull(Grails9HelpDocument.EMPTY.getCommand("war"));
  }

  public void testAScriptOfTheApplicationIsToldFromACommandOfIt() throws IOException {
    Grails9HelpDocument document = Grails9HelpDocument.parse("""
      {"format":1,"complete":true,"diagnostics":[],"projectDirectory":"/home/me/shop","commands":[
        {"name":"audit-report","aliases":[],"description":"Reports","project":true,"origin":"application",
         "target":"application","registration":"class","commands":[]},
        {"name":"seed-data","aliases":[],"description":"","project":true,"origin":"application",
         "target":"application","startsApplication":false,"registration":"script","commands":[]}]}
      """);

    assertFalse(document.getCommand("audit-report").isScript());
    assertFalse(document.getCommand("audit-report").isBuiltIn());
    assertTrue(document.getCommand("seed-data").isScript());
    assertFalse(document.getCommand("seed-data").isBuiltIn());
  }

  public void testTheMembersOfAToolOfTheContractOfTools() throws IOException {
    Grails9HelpDocument document = Grails9HelpDocument.parse(recorded("web-app.json"));

    Grails9HelpDocument.Command schemaExport = command(document, "schema-export");
    assertEquals("application", schemaExport.target());
    assertTrue(schemaExport.startsApplication());
    assertEquals(List.of("type"), schemaExport.deprecated());
    assertTrue(schemaExport.isDeprecated());
    assertFalse(schemaExport.isStandalone());
    Grails9HelpDocument.Command queryTail = command(document, "query-tail");
    assertTrue(queryTail.isStandalone());
    assertFalse(queryTail.startsApplication());
    assertFalse(queryTail.isDeprecated());
    assertFalse(command(document, "generate-all").isDeprecated());

    Grails9HelpDocument.Command runTool = command(document, "run-tool");
    assertTrue(runTool.isBuiltIn());
    assertEquals(List.of("run-command"), runTool.aliases());
    assertEquals(List.of("run-command"), runTool.deprecatedNames());
    assertTrue(runTool.isDeprecatedName("run-command"));
    assertEquals(List.of("run-script"), command(document, "run-script").deprecatedNames());
    assertEquals(List.of(), command(document, "assemble").deprecatedNames());
    assertFalse(command(document, "assemble").startsApplication());
    assertSame("a deprecated name still finds the command", runTool, document.getCommand("run-command"));
  }

  public void testCreateToolIsOfferedAndCreateCommandIsNot() throws IOException {
    List<String> names = Grails9HelpDocument.parse(recorded("web-app.json")).getCommandNames();

    assertTrue(names.toString(), names.contains("create-tool"));
    assertFalse(names.toString(), names.contains("create-command"));
    assertTrue(names.toString(), names.contains("run-tool"));
    assertFalse("a deprecated name is not offered: " + names, names.contains("run-command"));
    assertTrue("a command is offered by its name, though it is also a deprecated one: " + names, names.contains("run-script"));
  }

  public void testADocumentOfAnEarlierBuildOfGrails9IsStillRead() throws IOException {
    Grails9HelpDocument document = Grails9HelpDocument.parse(recorded("web-app-earlier-build.json"));

    assertTrue(document.isComplete());
    assertTrue(document.getCommandNames().contains("create-command"));
    assertTrue(document.getCommandNames().contains("run-command"));
    assertEquals(List.of(), command(document, "run-command").deprecatedNames());
    Grails9HelpDocument.Command schemaExport = command(document, "schema-export");
    assertTrue("a tool without startsApplication starts the application", schemaExport.startsApplication());
    assertEquals(List.of(), schemaExport.deprecated());
    assertTrue(command(document, "query-tail").isStandalone());
    assertFalse("a standalone tool does not start it", command(document, "query-tail").startsApplication());
    assertFalse("a command of the command line itself does not", command(document, "assemble").startsApplication());
    assertFalse(command(document, "install-templates").isStandalone());
  }

  public void testATargetThatIsNotKnownIsToleratedAndKeptAsItIs() throws IOException {
    Grails9HelpDocument document = Grails9HelpDocument.parse("""
      {"format":1,"complete":true,"commands":[
        {"name":"old-tool","aliases":[],"origin":"application","target":"gradle","registration":"legacy","commands":[]},
        {"name":"odd","aliases":[],"origin":"application","target":7,"startsApplication":"yes","deprecated":["folder",1,""],
         "deprecatedNames":"x","commands":[]}]}
      """);

    Grails9HelpDocument.Command old = command(document, "old-tool");
    assertEquals("gradle", old.target());
    assertEquals("legacy", old.registration());
    assertFalse(old.isStandalone());
    assertTrue(old.startsApplication());
    Grails9HelpDocument.Command odd = command(document, "odd");
    assertEquals("7", odd.target());
    assertEquals(List.of("folder"), odd.deprecated());
    assertEquals(List.of(), odd.deprecatedNames());
    assertTrue(odd.startsApplication());
  }

  private static Grails9HelpDocument.Command command(Grails9HelpDocument document, String name) {
    return document.getCommands().stream().filter(command -> command.name().equals(name)).findFirst()
      .orElseThrow(() -> new AssertionError(name + " is not in " + document.getCommandNames()));
  }

  private static void assertEmpty(List<?> list) {
    assertTrue(list.toString(), list.isEmpty());
  }
}
