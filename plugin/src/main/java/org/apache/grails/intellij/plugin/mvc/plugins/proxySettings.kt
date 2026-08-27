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

package org.apache.grails.intellij.plugin.mvc.plugins

import com.intellij.ui.JBIntSpinner
import com.intellij.ui.UIBundle
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.dialog
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.selected
import com.intellij.util.net.ProxyConfiguration
import com.intellij.util.net.ProxyCredentialStore
import com.intellij.util.net.ProxySettings
import org.apache.grails.intellij.plugin.GrailsBundle
import java.awt.Component
import javax.swing.JPasswordField
import javax.swing.JTextField

@JvmRecord
internal data class MvcProxyConfig(val host: String, val port: Int, val userName: String, val password: String)

internal fun editProxySettings(parent: Component?): MvcProxyConfig? {
  lateinit var noProxyRb: JBRadioButton
  lateinit var httpProxyRb: JBRadioButton
  lateinit var hostField: JTextField
  lateinit var portField: JBIntSpinner
  lateinit var loginField: JTextField
  lateinit var passwordField: JPasswordField

  val panel = panel {
    buttonsGroup {
      row {
        noProxyRb = radioButton(UIBundle.message("proxy.direct.rb")).component
      }
      row {
        httpProxyRb = radioButton(UIBundle.message("proxy.manual.rb")).component
      }
      indent {
        row(UIBundle.message("proxy.manual.host")) {
          hostField = textField().align(AlignX.FILL).component
        }
        row(UIBundle.message("proxy.manual.port")) {
          portField = spinner(0..65535).component
        }
        row(UIBundle.message("auth.login.label")) {
          loginField = textField().align(AlignX.FILL).component
        }
        row(UIBundle.message("auth.password.label")) {
          passwordField = passwordField().align(AlignX.FILL).component
        }
      }.enabledIf(httpProxyRb.selected)
    }
  }

  val cfg = ProxySettings.getInstance().getProxyConfiguration()
  if (cfg is ProxyConfiguration.StaticProxyConfiguration) {
    httpProxyRb.isSelected = true
    hostField.text = cfg.host
    portField.value = cfg.port
    val credentials = ProxyCredentialStore.getInstance().getCredentials(cfg.host, cfg.port)
    if (credentials != null) {
      loginField.text = credentials.userName ?: ""
      passwordField.text = credentials.getPasswordAsString() ?: ""
    }
  }
  else {
    noProxyRb.isSelected = true
  }

  val dialog = dialog(GrailsBundle.message("mvc.plugins.proxy.settings.title"), panel, resizable = true, parent = parent)

  return when {
    !dialog.showAndGet() -> null
    httpProxyRb.isSelected -> MvcProxyConfig(hostField.text, portField.number, loginField.text, String(passwordField.password))
    else -> MvcProxyConfig("", -1, "", "")
  }
}
