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

import com.intellij.util.PairConsumer;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.util.GrailsUtils;

import java.util.List;

public abstract class GrailsAbstractConfigSupport extends AbstractConfigSupport {

  @Override
  public void collectVariants(@NotNull List<String> prefix, @NotNull PairConsumer<String, Boolean> consumer) {
    if (prefix.isEmpty()) {
      consumer.consume(GrailsUtils.ENVIRONMENTS, false);
    }
    else if (prefix.size() == 1) {
      if (GrailsUtils.ENVIRONMENTS.equals(prefix.get(0))) {
        for (String s : GrailsUtils.ENVIRONMENT_LIST) {
          consumer.consume(s, false);
        }

        return;
      }
    }
    else {
      if (GrailsUtils.ENVIRONMENTS.equals(prefix.get(0)) && prefix.get(1) != null && GrailsUtils.ENVIRONMENT_LIST.contains(prefix.get(1))) {
        super.collectVariants(prefix.subList(2, prefix.size()), consumer);
        return;
      }
    }

    super.collectVariants(prefix, consumer);
  }
}
