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

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;

public final class AddAgentJarToClassPathTransformer implements ClassFileTransformer {
  private final Set<ClassLoader> oldClassLoaders = new HashSet<>();

  @Override
  public byte[] transform(ClassLoader loader,
                          String className,
                          Class classBeingRedefined,
                          ProtectionDomain protectionDomain,
                          byte[] classfileBuffer) throws IllegalClassFormatException {
    if (!AgentUtils.isGrailsClassLoader(loader)) {
      return null;
    }

    synchronized (oldClassLoaders) {
      if (!oldClassLoaders.add(loader)) {
        return null;
      }

      String agentClassUrl = Agent.class.getClassLoader().getResource(Agent.class.getName().replace('.', '/') + ".class").toString();
      int jarEnd = agentClassUrl.indexOf('!');
      if (!agentClassUrl.startsWith("jar:") || jarEnd == -1) {
        return null;
      }

      String jarPath = agentClassUrl.substring("jar:".length(), jarEnd);
      try {
        Method addUrlMethod = loader.getClass().getMethod("addURL", URL.class);
        addUrlMethod.invoke(loader, new URL(jarPath));
      }
      catch (Exception e) {
        System.out.println("Failed to add IDE test listener, some IDE features will be disabled");
        e.printStackTrace();
      }
    }
    return null;
  }
}
