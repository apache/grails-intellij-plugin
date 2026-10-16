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

package org.apache.grails.intellij.plugin.projectView;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * GrailsNodeComparator orders directory nodes by subtracting their weights and returning the
 * difference directly, so two nodes sharing a weight compare as 0 and their relative order is
 * unspecified — the platform comparator is never reached.
 */
public class NodeWeightsTest {

  @Test
  public void everyWeightIsDistinct() {
    Map<Integer, String> byValue = new HashMap<>();
    List<String> collisions = new ArrayList<>();
    for (Map.Entry<String, Integer> weight : declaredWeights().entrySet()) {
      String previous = byValue.putIfAbsent(weight.getValue(), weight.getKey());
      if (previous != null) collisions.add(previous + " and " + weight.getKey() + " are both " + weight.getValue());
    }
    assertEquals("node weights must be distinct, got " + collisions, List.of(), collisions);
  }

  /**
   * The values are the rendered order, so an edit that silently shifts a node has to fail here:
   * distinctness alone would not notice a value that drifted while staying unique.
   *
   * <p>This guards value drift, not deletion. Every constant is referenced from {@code src/main}, so
   * removing one fails at compile time and never reaches the test JVM.
   */
  @Test
  public void everyDeclaredWeightHasItsDocumentedValue() {
    Map<String, Integer> expected = new LinkedHashMap<>();
    expected.put("DOMAIN_CLASSES_FOLDER", 20);
    expected.put("SERVICES_FOLDER", 25);
    expected.put("CONTROLLERS_FOLDER", 30);
    expected.put("INTERCEPTORS_FOLDER", 31);
    expected.put("IMAGES_FOLDER", 32);
    expected.put("JAVASCRIPTS_FOLDER", 33);
    expected.put("STYLESHEETS_FOLDER", 34);
    expected.put("VIEWS_FOLDER", 40);
    expected.put("MIGRATIONS_FOLDER", 41);
    expected.put("TRANSLATIONS_FOLDER", 45);
    expected.put("UTILS_FOLDER", 46);
    expected.put("INIT_FOLDER", 59);
    expected.put("CONFIG_FOLDER", 60);
    expected.put("OTHER_GRAILS_APP_FOLDER", 64);
    expected.put("WEB_APP_FOLDER", 65);
    expected.put("SRC_FOLDERS", 70);
    expected.put("TESTS_FOLDER", 80);
    expected.put("CUSTOM_FOLDER", 85);
    expected.put("TAGLIB_FOLDER", 90);
    expected.put("FOLDER", 100);

    assertEquals("NodeWeights must declare exactly these weights and values", expected, declaredWeights());
  }

  private static Map<String, Integer> declaredWeights() {
    Map<String, Integer> result = new LinkedHashMap<>();
    for (Field field : NodeWeights.class.getDeclaredFields()) {
      if (!Modifier.isStatic(field.getModifiers()) || field.getType() != int.class) continue;
      try {
        result.put(field.getName(), field.getInt(null));
      }
      catch (IllegalAccessException e) {
        throw new AssertionError("cannot read NodeWeights." + field.getName(), e);
      }
    }
    return result;
  }
}
