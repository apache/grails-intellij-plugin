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

package org.apache.grails.intellij.plugin.references.jobs;

import com.intellij.psi.PsiClass;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.apache.grails.intellij.plugin.references.MemberProvider;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.util.dynamicMembers.DynamicMemberUtils;

public final class JobsMemberProvider extends MemberProvider {
  private static final String CLASS_SOURCE = "class JobElements {" +
                                             " public static Date schedule(String cronExpression, Map params = null) {}" +
                                             " public static Date schedule(Long interval, Integer repeatCount = org.quartz.SimpleTrigger.REPEAT_INDEFINITELY, Map params = null) {}" +
                                             " public static Date schedule(Date scheduleDate) {}" +
                                             " public static Date schedule(Date scheduleDate, Map params) {}" +
                                             " public static Date schedule(org.quartz.Trigger trigger) {}" +
                                             " public static void triggerNow(Map params = null) {}" +
                                             " public static boolean removeJob() {}" +
                                             " public static Date reschedule(org.quartz.Trigger trigger) {}" +
                                             " public static boolean unschedule(String triggerName, String triggerGroup = org.codehaus.groovy.grails.plugins.quartz.GrailsTaskClassProperty.DEFAULT_TRIGGERS_GROUP) {}" +
                                             "}";

  @Override
  public void processMembers(PsiScopeProcessor processor, PsiClass psiClass, GrReferenceExpression ref) {
    DynamicMemberUtils.process(processor, psiClass, ref, CLASS_SOURCE);
  }
}
