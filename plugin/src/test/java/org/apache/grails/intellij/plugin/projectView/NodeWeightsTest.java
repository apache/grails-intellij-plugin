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
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * GrailsNodeComparator orders directory nodes by subtracting their weights, so two nodes sharing a
 * weight compare equal and their order falls through to the platform comparator.
 */
public class NodeWeightsTest {

  @Test
  public void everyWeightIsDistinct() throws IllegalAccessException {
    Map<Integer, String> byValue = new HashMap<>();
    List<String> collisions = new ArrayList<>();
    for (Field field : NodeWeights.class.getDeclaredFields()) {
      if (!Modifier.isStatic(field.getModifiers()) || field.getType() != int.class) continue;
      String name = field.getName();
      int weight = field.getInt(null);
      String previous = byValue.putIfAbsent(weight, name);
      if (previous != null) collisions.add(previous + " and " + name + " are both " + weight);
    }
    assertEquals("node weights must be distinct, got " + collisions, List.of(), collisions);
  }
}
