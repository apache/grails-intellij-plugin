# grails-compiler-patch

Groovy AST compilation-unit patchers pushed onto the Groovy compiler classpath **inside the user's
JPS build**, so that Grails 1.x/2.x sources compile the way Grails itself compiles them.

This is a **compiler plugin**, not a library: its jar name appears literally in
`<compileServer.plugin classpath="..."/>` in `plugin.xml`. JPS resolves those entries by file name
and silently skips anything it cannot find, so the name is pinned by the
`org.apache.grails.intellij.build.compiler` convention plugin and must stay in sync with the
descriptor.

## Contents

| Patcher | Applies to |
| --- | --- |
| `GrailsDomainClassPatcher` | Grails < 1.2 |
| `GrailsJUnitPatcher` | all supported legacy versions |
| `Grails2_0_JUnitPatcher` | Grails >= 2.0 |
| `EmptyGrailsAwarePatcher` | Grails >= 2.0 |
| `GrailsCompilerRtMarker` | locator marker |

## Grails version support

- **Minimum:** 1.0
- **Maximum:** 2.x

`jps-plugin`'s `GrailsBuilderExtension.shouldInjectGrails()` only injects this jar when the module's
`grails-core-<version>.jar` is **older than 3.0**, so it is inert for Grails 3+ projects. Grails 3
moved to Gradle and no longer needs the patchers.

## Build

Java 8 bytecode, and it deliberately compiles against era-correct dependencies —
`grails-core:1.2.0` and `groovy-all:2.4.21` — with the IDE's bundled Groovy excluded from
`compileClasspath`. It must not be built against modern Groovy, because it runs inside Grails 2.x
builds.
