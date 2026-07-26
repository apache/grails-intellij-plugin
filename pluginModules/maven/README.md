# maven

IntelliJ **content module** supporting Grails projects built with **Maven** rather than Grails' own
build system or Gradle: project import, a Maven-backed application model, run/debug via Maven goals,
and a Maven node in the Grails project view.

## Grails version support

- **Minimum:** 1.x
- **Maximum:** effectively 2.x

There is no hard upper gate in the code, but Maven-built Grails is a 1.x/2.x arrangement
(`grails-maven-plugin`); Grails 3 and later are Gradle-based and are handled by `gradle-tooling`
instead.

One explicit version gate exists, in `MavenCommandExecutor`: for Grails **< 2.1.0** the `test-app`
goal mapping is skipped, because those versions must run tests as
`mvn -Dcommand=test-app -Dargs=<TestClassName> grails:exec` rather than
`mvn grails:test-app -Dgrails.cli.args=<TestClassName>` (IDEA-105206).

## Packaging

Content module `intellij.groovy.grails.maven`, shipped as
`lib/modules/intellij.groovy.grails.maven.jar`.

Requires `org.jetbrains.idea.maven` and `com.intellij.java` at runtime — the Java debugger classes
moved into the java plugin modules in 2026.2.
