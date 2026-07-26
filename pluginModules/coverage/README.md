# coverage

IntelliJ **content module** integrating Grails test runs with the IDE's code-coverage tooling.

## Grails version support

- **Minimum / maximum:** no version gating.

## Packaging

Content module `org.apache.grails.intellij.module.coverage`, shipped as
`lib/modules/org.apache.grails.intellij.module.coverage.jar`.

Its descriptor declares `<plugin id="Coverage"/>`. This matters: depending on the
`com.intellij.modules.coverage` alias instead caused a `NoClassDefFoundError` at IDE startup, because
the alias does not carry the classes. A content module's `<dependencies>` must name the
class-bearing plugin id.
