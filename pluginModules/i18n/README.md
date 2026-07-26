# i18n

IntelliJ **content module** for Grails message bundles: `i18n/messages*.properties` support,
extract-to-bundle intentions and inspections for hard-coded strings in GSP and Groovy.

## Grails version support

- **Minimum / maximum:** no version gating. The `grails-app/i18n` message-bundle convention is stable
  across supported versions.

## Packaging

Content module `org.apache.grails.intellij.module.i18n`, shipped as
`lib/modules/org.apache.grails.intellij.module.i18n.jar`. Its descriptor also declares
`<resource-bundle>messages.GrailsBundle</resource-bundle>`.

Requires `com.intellij.java-i18n` and `org.intellij.groovy` at runtime.

This module contains **no Kotlin** — it previously applied the Kotlin plugin with zero Kotlin
sources, which has been removed.
