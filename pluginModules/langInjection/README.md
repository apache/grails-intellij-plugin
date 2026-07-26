# langInjection

IntelliJ **content module** providing language injection for Grails code — injecting HQL, SQL and
other languages into the string literals and GSP positions where Grails expects them.

Configuration is data rather than code: the module ships `META-INF/languageInjections.xml` and points
at it from its descriptor with `<injectionConfig config="META-INF/languageInjections.xml"/>`.

## Grails version support

- **Minimum / maximum:** no version gating.

## Packaging

Content module `intellij.groovy.grails.langInjection`, shipped as
`lib/modules/intellij.groovy.grails.langInjection.jar`.

This is the only module whose descriptor depends on a bundled **module** rather than a plugin
(`<module name="intellij.platform.langInjection"/>`), and the only one with no Java/Kotlin sources at
all — it is resources only.
