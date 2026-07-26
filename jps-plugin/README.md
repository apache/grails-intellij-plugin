# jps-plugin

The plugin's participant in the **user's JPS build process** (the external build process IntelliJ
IDEA runs when compiling a project). Registered through
`META-INF/services/org.jetbrains.jps.incremental.groovy.GroovyBuilderExtension`.

Despite the name this is **shipped product code**, not build tooling for this repository.

This is a **compiler plugin**: its jar name appears literally in
`<compileServer.plugin classpath="..."/>` in `plugin.xml`, resolved by JPS by file name, so the name
is pinned by the `org.apache.grails.intellij.build.compiler` convention plugin.

## What it does

`GrailsBuilderExtension` inspects each JPS module's library dependencies for a
`grails-core-<version>.jar`. When it finds one older than 3.0 it:

1. adds `grails-compiler-patch` to the Groovy compilation classpath, and
2. registers the AST compilation-unit patchers appropriate to that Grails version.

## Grails version support

- **Minimum:** 1.0
- **Maximum:** 2.x — `shouldInjectGrails()` returns false for 3.0 and above

The jar is always shipped, but it does nothing for Grails 3+ projects.

## Build

Java 11 bytecode: it is loaded by the JPS build process, not by the IDE.
