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
package com.intellij.platform.testFramework.eelJava

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.projectRoots.impl.jdkDownloader.JdkInstallRequestInfo
import com.intellij.openapi.projectRoots.impl.jdkDownloader.JdkInstaller
import com.intellij.openapi.projectRoots.impl.jdkDownloader.JdkItem
import com.intellij.openapi.projectRoots.impl.jdkDownloader.ReadJdkItemsForWSL
import com.intellij.platform.testFramework.eelJava.EelTestUtil.getEelFixtureEngineJavaHome
import com.intellij.platform.testFramework.eelJava.EelTestUtil.getFixtureEngine
import com.intellij.platform.testFramework.eelJava.EelTestUtil.getTeamcityWslJdkDefinition
import com.intellij.platform.eel.EelDescriptor
import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path
import kotlin.io.path.exists

@ApiStatus.Internal
object EelTestJdkProvider {

    private val LOG = logger<EelTestJdkProvider>()

    // 2026.2 platform test-framework code calls this overload; for local (non-eel) runs
    // the platform implementation returns null, matching the no-arg variant below
    @JvmStatic
    fun getJdkPath(eelDescriptor: EelDescriptor): Path? = getJdkPath()

    @JvmStatic
    fun getJdkPath(): Path? {
        val engine = getFixtureEngine()
        if (engine == EelTestUtil.EelFixtureEngine.NONE) {
            return null
        }
        val jdkPath = getEelFixtureEngineJavaHome()
        if (engine == EelTestUtil.EelFixtureEngine.WSL) {
            val definition = getTeamcityWslJdkDefinition()
            if (definition != null) {
                val jdkToInstall = readJdkItem(definition)
                checkOrInstallJDK(jdkPath, jdkToInstall)
            }
        }
        return jdkPath
    }

    private fun readJdkItem(path: Path): JdkItem {
        return ReadJdkItemsForWSL.readJdkItems(path)[0]
    }

    private fun checkOrInstallJDK(path: Path, jdkItem: JdkItem) {
        if (path.resolve("bin/java").exists()) {
            LOG.info("JDK is installed in $path. Nothing to do.")
        }
        else {
            ProgressManager.getInstance().runUnderEmptyProgress { progress ->
                val installer = JdkInstaller.getInstance()
                installer.installJdk(JdkInstallRequestInfo(jdkItem, path), progress, null)
            }
        }
    }

    private fun ProgressManager.runUnderEmptyProgress(fn: (indicator: ProgressIndicator) -> Unit) {
        val indicator = EmptyProgressIndicator()
        runProcess({ fn(indicator) }, indicator)
    }
}