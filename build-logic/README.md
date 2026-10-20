# build-logic

The build's convention plugins, as an **included build** (`pluginManagement { includeBuild }` in
`settings.gradle`). Not shipped, and not published.

Written as **precompiled Groovy script plugins** in `src/main/groovy`, named after their plugin id,
so a subproject build file is one `plugins { }` line plus its own dependencies. Deliberately not
imperative `Plugin<Project>` classes.

One of them targets `Settings` rather than `Project`: the `.settings.gradle` suffix on
`org.apache.grails.intellij.build.repositories.settings.gradle` makes it a settings plugin, applied
from `settings.gradle`. That is what keeps repository declarations out of the projects entirely.

## Why plugin versions live here

`build-logic/build.gradle` declares the IntelliJ Platform, Kotlin, RAT and CycloneDX plugins as
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
| `validate-actions` | root only — `validateActions`, the ASF approved-actions check on `.github/workflows` |
| `vulnerability-scan-report` | root only — `vulnerabilityScanReport`, the Markdown summary of every project's scan at `build/reports/vulnerability-scan/summary.md` |
| `sbom` | the plugin project (via `intellij-plugin`) — `sbom`, the reproducible CycloneDX SBOM packaged into the ZIP and the composed jar |
| `reproducible`, `jacoco`, `vulnerability-scan` | applied transitively |
| `repositories` | **settings** plugin — applied from `settings.gradle`, not by a project |

## Vulnerability scan

`vulnerability-scan` (applied by `java`, so by every project that compiles) registers
`vulnerabilityScan`, which looks the resolved `runtimeClasspath` and `compileClasspath` up in the
[OSV](https://osv.dev) database and, when `SONATYPE_GUIDE_USERNAME` and `SONATYPE_GUIDE_TOKEN` are
set, in Sonatype Guide. The scanner is this build's own code in `org.apache.grails.intellij.build.scan`,
ported from grails-core's `build-logic/vulnerability-scan`; it replaced the Sonatype scan-gradle-plugin,
whose task is incompatible with the configuration cache and discarded the entry of every build that
ran it. Points that differ from a plain dependency scan:

- **The IntelliJ Platform is left out.** IPGP puts the IDE's modules and bundled plugins on every
  compile classpath as Maven-like components (`bundledModule:intellij.platform.core:IU-…`) that no
  database knows. `intellij-platform` sets `vulnerabilityScan.ignoredGroups` to those groups; a
  real Maven artifact the platform pulls in stays scanned.
- **`java-legacy` projects scan `runtimeClasspath` only.** Their `compileOnly` dependencies are
  era-pinned stubs of the Grails/Groovy they run inside, never shipped.
- **Credentials are read when a scan runs**, never at configuration time, so they are not in the
  configuration cache entry and a rotated token reuses it. Answers are cached in the Gradle user
  home for an hour (`-PvulnerabilityScan.cacheTtl=PT0S` asks again), in the directory grails-core
  uses, so a machine that builds both asks once.
- **Exclusions need a reason** and are limited to findings with no fix available; the built-in list
  is `BuiltInExclusions`, a project adds its own with `vulnerabilityScan { exclude(id, module, reason) }`,
  and the summary calls out exclusions that no longer match anything.

## Tests

Task classes with real logic are tested with Gradle TestKit under `src/test/groovy`, on the same
JUnit 4 + AssertJ stack as the plugin. `./gradlew :build-logic:test` runs them; the root `check`
depends on build-logic's `check`, so CI runs them (and `validatePlugins`) on every build. The scan
tests run the convention plugins against in-process stubs of the OSV and Sonatype Guide APIs and a
local Maven repository, with the configuration cache on and its problems fatal.

## Groovy DSL gotchas

- Groovy resolves closures **owner-first**, so `extensions.configure(SomeType) { foo = ... }` assigns
  to the `Project` and fails with `MissingPropertyException`. Bind an explicit parameter.
- A convention plugin that configures another plugin's extensions must **apply that plugin in its own
  `plugins { }` block**; the consuming plugin applies it too late, after this body has run.
- Kotlin `object`s need `.INSTANCE` from Groovy, and Kotlin named/default arguments are unreachable —
  see how `testFramework(...)` is called in `testFramework/build.gradle`.
- There are no type-safe accessors. Run a real build after each change; a typo is a
  configuration-time `MissingMethodException`, not a compile error.

## Repositories

All dependency repositories are declared once, by the `repositories` settings plugin, and
`dependencyResolutionManagement` is set to `FAIL_ON_PROJECT_REPOS`. A project that declares its own
`repositories { }` block now fails the build:

> Build was configured to prefer settings repositories over project repositories but repository
> 'MavenRepo' was added by build file 'pluginModules/hibernate/build.gradle'

The IntelliJ Platform repositories come from `org.jetbrains.intellij.platform.settings`, which
registers the `intellijPlatform` repositories extension onto `dependencyResolutionManagement`.

This replaced `repositories { }` blocks in two separate convention plugins that had to be kept in
sync — and whose absence on the root project broke the licence audit the moment the root stopped
applying the java conventions, because RAT resolves its own tool jar at execution time.

`build-logic/settings.gradle` keeps its own repository declarations: it is a separate build and
cannot consume a plugin it has not yet compiled.
