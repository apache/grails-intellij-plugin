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
package org.apache.grails.intellij.plugin.feature.scaffold;

import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture;
import org.jetbrains.annotations.NotNull;

/**
 * The slice of grails-scaffolding and grails-rest the tests need: the annotation and the two scaffold
 * bases, with the extras a compiled Groovy class carries - a property accessor and a {@code @Generated}
 * trait method - so the member listing can be seen to skip them.
 */
final class ScaffoldingStubs {

  private ScaffoldingStubs() {
  }

  static void addTo(@NotNull JavaCodeInsightTestFixture fixture) {
    fixture.addClass("""
                       package groovy.transform;
                       public @interface Generated {}
                       """);
    fixture.addClass("""
                       package grails.plugin.scaffolding.annotation;
                       public @interface Scaffold {
                         Class<?> value() default Void.class;
                         Class<?> domain() default Void.class;
                         boolean readOnly() default false;
                       }
                       """);
    fixture.addClass("""
                       package grails.rest;
                       public class RestfulController<T> {
                         public RestfulController(Class<T> resource) {}
                         public RestfulController(Class<T> resource, boolean readOnly) {}
                         public Class<T> getResource() { return null; }
                         @groovy.transform.Generated public void render(Object o) {}
                         public Object index(Integer max) { return null; }
                         public Object show() { return null; }
                         public Object create() { return null; }
                         public Object save() { return null; }
                         public Object edit() { return null; }
                         public Object patch() { return null; }
                         public Object update() { return null; }
                         public Object delete() { return null; }
                         protected T queryForResource(java.io.Serializable id) { return null; }
                         public static Object getStaticApplicationContext() { return null; }
                       }
                       """);
    fixture.addClass("""
                       package grails.plugin.scaffolding;
                       public class RestfulServiceController<T> extends grails.rest.RestfulController<T> {
                         public RestfulServiceController(Class<T> resource, boolean readOnly) { super(resource, readOnly); }
                       }
                       """);
    fixture.addClass("""
                       package grails.plugin.scaffolding;
                       public class GormService<T> {
                         public GormService(Class<T> resource, boolean readOnly) {}
                         public boolean isReadOnly() { return false; }
                         public T get(java.io.Serializable id) { return null; }
                         public java.util.List<T> list(java.util.Map args) { return null; }
                         public Long count(java.util.Map args) { return null; }
                         public void delete(java.io.Serializable id) {}
                         public T save(T instance) { return null; }
                       }
                       """);
  }
}
