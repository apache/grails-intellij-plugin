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
package org.jetbrains.groovy.grails.rt;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class ForkListenerTransformer implements ClassFileTransformer {

  private boolean done;

  private final String fileName;

  public ForkListenerTransformer(String fileName) {
    this.fileName = fileName;
  }

  @Override
  public byte[] transform(ClassLoader loader,
                          String className,
                          Class classBeingRedefined,
                          ProtectionDomain protectionDomain,
                          byte[] classfileBuffer) throws IllegalClassFormatException {
    if (done) return null;

    if (AgentUtils.isGrailsClassLoader(loader)) {
      writeToFile(fileName, new byte[]{1});
      done = true;
    }
    else {
      if ("org/grails/maven/plugin/tools/ForkedGrailsRuntime".equals(className)) {
        writeToFile(fileName, new byte[]{1, 2, 3});
        done = true;
      }
    }

    return null;
  }

  private static void writeToFile(String fileName, byte[] content) {
    try {
      try (FileOutputStream outputStream = new FileOutputStream(fileName)) {
        outputStream.write(content);
      }
    }
    catch (IOException ignored) {

    }
  }

}
