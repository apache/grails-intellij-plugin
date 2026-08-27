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
@file:Suppress("MemberVisibilityCanBePrivate", "unused")
package com.intellij.openapi.externalSystem.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.util.ThrowableComputable

fun <R> runReadAction(action: () -> R): R {
    return ApplicationManager.getApplication().runReadAction(ThrowableComputable(action))
}

fun <R> runWriteActionAndGet(action: () -> R): R {
    return invokeAndWaitIfNeeded {
        ApplicationManager.getApplication().runWriteAction(ThrowableComputable(action))
    }
}

fun runWriteActionAndWait(action: () -> Unit) {
    ApplicationManager.getApplication().invokeAndWait {
        ApplicationManager.getApplication().runWriteAction(action)
    }
}