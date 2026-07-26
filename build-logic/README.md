# build-logic

The build's convention plugins, as an **included build** (`pluginManagement { includeBuild }` in
`settings.gradle`). Not shipped, and not published.

Written as **precompiled Groovy script plugins** in `src/main/groovy`, named after their plugin id,
so a subproject build file is one `plugins { }` line plus its own dependencies. Deliberately not
imperative `Plugin<Project>` classes.

## Why plugin versions live here

`build-logic/build.gradle` declares the IntelliJ Platform, Kotlin, RAT and Sonatype scan plugins as
`implementation` dependencies. That is what lets the convention plugins apply them **without a
version**, and it removes the previous fragility where `org.jetbrains.intellij.platform.module` was
applied version-free in every module and resolved only by inheriting the root build script's plugin
classpath.

These versions are literals here rather than in `gradle.properties`, because build-logic is a
separate build and cannot read the root build's properties. Dependency versions used by the main
build still come from the root `gradle.properties` via `providers.gradleProperty(...)`.

## The plugins

| Plugin (`org.apache.grails.intellij.build.` + …) | Applied by |
| --- | --- |
| `base` | every project — repositories, reproducible archives |
| `java` | every project that compiles — layout, release level, jacoco, vulnerability scan, test |
| `java-legacy` | projects needing pre-25 bytecode; `legacyBytecode { release = N }` |
| `kotlin` | projects with Kotlin sources |
| `test` | test conventions |
| `intellij-platform` | shared IPGP repositories + platform dependency |
| `intellij-plugin` | the plugin project |
| `intellij-module` | the six content modules |
| `lib` | plain libraries (no IntelliJ Platform) |
| `intellij-lib` | libraries that compile against the platform |
| `compiler` | compiler plugins named in `compileServer.plugin` |
| `rat` | root only — the licence audit |
| `coverage-aggregation` | root only — cross-project JaCoCo |
| `reproducible`, `jacoco`, `vulnerability-scan` | applied transitively |
| `legacy-layout` | **transitional** — delete once all tiers use standard source layout |

## Groovy DSL gotchas

- Groovy resolves closures **owner-first**, so `extensions.configure(SomeType) { foo = ... }` assigns
  to the `Project` and fails with `MissingPropertyException`. Bind an explicit parameter.
- A convention plugin that configures another plugin's extensions must **apply that plugin in its own
  `plugins { }` block**; the consuming plugin applies it too late, after this body has run.
- Kotlin `object`s need `.INSTANCE` from Groovy, and Kotlin named/default arguments are unreachable —
  see how `testFramework(...)` is called in `testFramework/build.gradle`.
- There are no type-safe accessors. Run a real build after each change; a typo is a
  configuration-time `MissingMethodException`, not a compile error.
