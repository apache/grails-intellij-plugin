# gradle-tooling

A Gradle **tooling API model builder**. It runs inside the Gradle process during project import and
reports Grails-specific information back to the IDE as a `GrailsModule` model.

Registered through `META-INF/services/org.jetbrains.plugins.gradle.tooling.ModelBuilderService`.

## What it does

`GrailsModuleModelBuilderImpl` determines, per Gradle project:

- the Grails version — preferring the explicit `grailsVersion` project property, because it must be
  known before the Grails shell CLI dependency can be added (the artifact coordinates changed across
  versions, so adding it unversioned fails to resolve and breaks the whole import)
- the Grails plugin id
- the shell CLI dependency URLs

## Grails version support

- **Minimum:** 3.0 — this only applies to Gradle-built Grails projects, which is Grails 3 and newer
- **Maximum:** none

## Build

Java 8 bytecode: it is loaded by the Gradle daemon, which may run an older JVM than the IDE.
