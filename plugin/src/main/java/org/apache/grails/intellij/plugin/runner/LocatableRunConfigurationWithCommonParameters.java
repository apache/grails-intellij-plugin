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

package org.apache.grails.intellij.plugin.runner;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.LocatableConfigurationBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.JdomKt;
import org.jdom.Element;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApiStatus.Internal
public abstract class LocatableRunConfigurationWithCommonParameters extends LocatableConfigurationBase {

  private String myProgramParameters;
  private String myVMParameters;
  private final Map<String, String> myEnvs = new HashMap<>();
  private boolean myPassParentEnv = true;

  protected LocatableRunConfigurationWithCommonParameters(@NotNull Project project,
                                                          @NotNull ConfigurationFactory factory,
                                                          String name) {
    super(project, factory, name);
  }

  public @Nullable String getProgramParameters() {
    return myProgramParameters;
  }

  public void setProgramParameters(@Nullable String value) {
    myProgramParameters = value;
  }

  public String getVMParameters() {
    return myVMParameters;
  }

  public void setVMParameters(String vmParams) {
    this.myVMParameters = vmParams;
  }

  public @NotNull Map<String, String> getEnvs() {
    return myEnvs;
  }

  public void setEnvs(@NotNull Map<String, String> envs) {
    this.myEnvs.clear();
    this.myEnvs.putAll(envs);
  }

  public boolean isPassParentEnvs() {
    return myPassParentEnv;
  }

  public void setPassParentEnvs(boolean passParentEnv) {
    this.myPassParentEnv = passParentEnv;
  }

  @Override
  public void readExternal(@NotNull Element element) throws InvalidDataException {
    super.readExternal(element);
    myVMParameters = readSetting(element, "vmparams");
    myProgramParameters = readSetting(element, "cmdLine");

    String sPassParentEnvironment = readSetting(element, "passParentEnv");
    myPassParentEnv = StringUtil.isEmpty(sPassParentEnvironment) || Boolean.parseBoolean(sPassParentEnvironment);

    myEnvs.clear();
    for (Element env : element.getChildren("env")) {
      String name = env.getAttributeValue("name");
      if (name != null) {
        myEnvs.put(name, env.getAttributeValue("value"));
      }
    }
  }

  @Override
  public void writeExternal(@NotNull Element element) throws WriteExternalException {
    super.writeExternal(element);
    writeSetting(element, "vmparams", myVMParameters);
    writeSetting(element, "cmdLine", myProgramParameters);
    myEnvs.keySet().stream().sorted().forEach(name -> {
      Element env = new Element("env");
      env.setAttribute("name", name);
      String value = myEnvs.get(name);
      if (value != null) {
        env.setAttribute("value", value);
      }
      element.addContent(env);
    });
    JdomKt.addOptionTag(element, "passParentEnv", Boolean.toString(myPassParentEnv), "setting");
  }

  /**
   * Writes a {@code <setting name=".." value=".."/>} child element, matching the legacy
   * {@code JDOMExternalizer.write} XML format so persisted run configurations stay compatible.
   */
  protected static void writeSetting(@NotNull Element root, @NotNull String name, @Nullable String value) {
    Element setting = new Element("setting");
    setting.setAttribute("name", name);
    setting.setAttribute("value", value == null ? "" : value);
    root.addContent(setting);
  }

  /**
   * Reads the value of a {@code <setting name=".."/>} child element, matching the legacy
   * {@code JDOMExternalizer.readString} XML format.
   */
  protected static @Nullable String readSetting(@NotNull Element root, @NotNull String name) {
    List<Element> settings = root.getChildren("setting");
    for (Element setting : settings) {
      if (Comparing.strEqual(setting.getAttributeValue("name"), name)) {
        return setting.getAttributeValue("value");
      }
    }
    return null;
  }
}
