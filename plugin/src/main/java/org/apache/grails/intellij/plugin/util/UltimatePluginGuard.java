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
package org.apache.grails.intellij.plugin.util;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Guards the handful of code paths that touch Ultimate-only plugins from the single plugin ZIP
 * that must also load on Community Edition. The plugin ZIP is built once and plugin.xml declares
 * the Ultimate integrations as optional {@code <depends config-file=...>} entries, so the classes
 * below stay in the jar; what they must not do is *execute* on an IDE that does not install the
 * Ultimate plugin the class belongs to. Because the JVM resolves class constant-pool entries
 * lazily, a direct reference placed inside {@link #runIfPluginAvailable} or
 * {@link #callIfPluginAvailable} is never resolved on an IDE missing the plugin -- which is what
 * makes the guarded call sites safe.
 *
 * <p>Static-field access that would otherwise unconditionally load the owning class (icons,
 * file-type instances, editor keys) goes through {@link #staticFieldValue} instead, which
 * resolves the field reflectively and falls back to a core substitute.
 */
public final class UltimatePluginGuard {

  private static final Logger LOG = Logger.getInstance(UltimatePluginGuard.class);

  public static final String SPRING_PLUGIN = "com.intellij.spring";
  public static final String JAVAEE_PLUGIN = "com.intellij.javaee";
  public static final String JSP_PLUGIN = "com.intellij.jsp";
  public static final String EL_PLUGIN = "com.intellij.javaee.el";

  private static final Map<String, Object> STATIC_FIELD_CACHE = new ConcurrentHashMap<>();

  private UltimatePluginGuard() {
  }

  /** @return {@code true} when the plugin with {@code pluginId} is installed and enabled. */
  public static boolean isPluginAvailable(@NotNull String pluginId) {
    return PluginManagerCore.getPlugin(PluginId.getId(pluginId)) != null;
  }

  /** Runs {@code action} only when the plugin with {@code pluginId} is present. */
  public static void runIfPluginAvailable(@NotNull String pluginId, @NotNull Runnable action) {
    if (isPluginAvailable(pluginId)) {
      action.run();
    }
  }

  /**
   * Runs {@code action} only when the plugin with {@code pluginId} is present, returning
   * {@code fallback} otherwise. Used for expression-producing call sites whose result is only
   * meaningful on the owning plugin.
   */
  public static <T> T callIfPluginAvailable(@NotNull String pluginId, @NotNull Supplier<T> action, T fallback) {
    return isPluginAvailable(pluginId) ? action.get() : fallback;
  }

  /**
   * Reads the value of the public static field {@code fieldName} on {@code className} by
   * reflection, caching the result, returning {@code fallback} when the class or field is not
   * available. Prevents loading Ultimate-only classes on an IDE that does not install them.
   */
  public static @Nullable <T> T staticFieldValue(@NotNull String className, @NotNull String fieldName, @Nullable T fallback) {
    String key = className + '#' + fieldName;
    Object cached = STATIC_FIELD_CACHE.get(key);
    if (cached != null) {
      @SuppressWarnings("unchecked")
      T value = (T)cached;
      return value;
    }
    try {
      Object value = Class.forName(className).getField(fieldName).get(null);
      STATIC_FIELD_CACHE.put(key, value);
      @SuppressWarnings("unchecked")
      T typed = (T)value;
      return typed;
    }
    catch (Throwable e) {
      LOG.warn("Cannot read static field " + key + "; using fallback", e);
      return fallback;
    }
  }

  /**
   * Invokes the public static no-arg method {@code methodName} on {@code className} by reflection,
   * doing nothing when the class or method is not present. Same contract as {@link #staticFieldValue}
   * for methods: a guard whose predicate is the class being loaded rather than a plugin id, which
   * is what makes it safe for classes whose owning plugin is registered even when the class jar
   * itself is absent (e.g. the full-platform module set leaking into a Community test sandbox).
   */
  public static void invokeStaticIfAvailable(@NotNull String className, @NotNull String methodName) {
    try {
      Class.forName(className).getMethod(methodName).invoke(null);
    }
    catch (Throwable e) {
      LOG.debug("Cannot invoke static method " + className + '#' + methodName, e);
    }
  }
}