# Apache Grails IntelliJ Plugin

IntelliJ IDEA plugin for the [Apache Grails](https://grails.apache.org/) framework:
GSP language support (parsing, highlighting, completion, refactoring), Grails project
structure and navigation, run configurations, taglib/domain-class support, and
integrations for i18n, coverage, Hibernate, Maven, and language injection.

The Grails project view pane gives `grails-app/` dedicated **Stylesheets**, **Images**,
**JavaScripts**, **Migrations**, **Translations** and **Utils** nodes, and lifts the test
source roots out of `src` as **Tests:unit**, **Tests:integration** and **Tests:functional**.
Users can configure custom folders — global defaults with a per-project override —
that appear as nodes below the test nodes in the Grails view. Custom folders reference
directories within the Grails application: a configured path is relative and is resolved
against the Grails application root, so there is no built-in way to browse an external
directory in the Grails view. Six folders render at a time by default; the limit is a
setting on *Languages & Frameworks ▸ Grails*, where it can be raised or lowered.
Hidden directories such as `.github` render as folder nodes, because the ignore list
is never consulted for a custom folder node itself. Their contents show too: the
default *Ignored Files and Folders* list matches named entries and glob suffixes
such as `.git` rather than dot-directories as a class. Adding `.github` to that list
yourself leaves the node where it is and leaves its contents visible — the platform
matches each child under its own name, so an entry for `.github` does not match
`workflows` or `ci.yml` — and a file inside the folder is still reachable, because
the *Reveal* check reads the file being revealed. The one thing it does stop is
*Reveal in Project View* selecting the `.github` node itself, whose own `contains()`
then answers false.

The plugin runs on IntelliJ IDEA Ultimate and, with a reduced feature set, on IntelliJ IDEA
Community Edition 2026.2+ (see [CE-SUPPORT.md](CE-SUPPORT.md)).

This codebase was originally developed by JetBrains s.r.o. and donated to the Apache
Software Foundation under a software grant. It was imported from the `grails` directory of
[JetBrains/intellij-obsolete-plugins](https://github.com/JetBrains/intellij-obsolete-plugins)
at commit
[`dfe54ea`](https://github.com/JetBrains/intellij-obsolete-plugins/commit/dfe54eaecdbe50ba9c7c1ed162ff9a547fa3ca4a)
(tree `a8bd06f`), the last commit to touch that directory upstream.

## Requirements

To **run** the plugin:

- IntelliJ IDEA **Ultimate** 2026.2.2+ (`sinceBuild` 262.10315.125) for the full feature set, or
  IntelliJ IDEA **Community Edition** 2026.2+ for the reduced one. The plugin loads on Community:
  every Ultimate-only integration lives in a content module that the platform skips when its
  dependencies are absent, so the main jar compiles against the Community API only. See
  [CE-SUPPORT.md](CE-SUPPORT.md) for exactly which features are missing.

To **build** the plugin:

| | Version | Notes |
| --- | --- | --- |
| JDK | Liberica 25 (`25.0.3-librca`) | Pinned in [`.sdkmanrc`](.sdkmanrc). The exact JDK matters — see [reproducible builds](RELEASE.md#verifying-a-reproducible-build). |
| Gradle | 9.8.0 | Pinned in [`.sdkmanrc`](.sdkmanrc). Only needed to bootstrap the wrapper; otherwise use `./gradlew`. |
| Git | any recent | To clone the repository. |
| Disk | ~10 GB free | The IntelliJ Platform and bundled plugins are downloaded into `.intellijPlatform` and the Gradle cache. |
| Memory | 8 GB+ | The build runs a 4 GB Gradle daemon (set in `gradle.properties`). |

The build needs network access to Maven Central and the JetBrains IntelliJ Platform
repositories. The `grails-rt`, `grails-compiler-patch`, and `jps-plugin` modules target
older bytecode levels (Java 8/11) but still build with the JDK above.

## Building from source

These steps take you from an empty machine to an installable plugin ZIP. They cover a
clone of this repository; if you are starting from a released **source distribution**
instead, see [INSTALL](INSTALL) — the only difference is that step 3 is mandatory there.

### 1. Get the source

```bash
git clone https://github.com/apache/grails-intellij-plugin.git
cd grails-intellij-plugin
```

### 2. Install the JDK and Gradle

The required versions are pinned in [`.sdkmanrc`](.sdkmanrc). The simplest way to get
exactly those versions is [SDKMAN!](https://sdkman.io):

```bash
curl -s "https://get.sdkman.io" | bash     # if SDKMAN! is not installed yet
source "$HOME/.sdkman/bin/sdkman-init.sh"

sdk env install    # reads .sdkmanrc, installs the pinned JDK + Gradle
sdk env            # selects them for the current shell
```

If you would rather not use SDKMAN!, install the same versions by any means you prefer —
for example download [Liberica JDK 25](https://bell-sw.com/pages/downloads/) and
[Gradle 9.8.0](https://gradle.org/releases/) manually — then make sure `java -version`
and `gradle --version` report them:

```bash
export JAVA_HOME=/path/to/liberica-jdk-25
export PATH="$JAVA_HOME/bin:/path/to/gradle-9.8.0/bin:$PATH"
```

Building with a different JDK will usually work, but the artifacts will not be
bit-for-bit reproducible against an official release.

### 3. Bootstrap the Gradle Wrapper

**From a git clone this step is not needed** — `gradlew` and the wrapper jar are checked
in, so skip straight to step 4.

**From a source distribution it is required.** ASF source releases may not contain
compiled binaries, so the wrapper jar is stripped out and must be regenerated once using
the local Gradle from step 2:

```bash
cd gradle-bootstrap
gradle bootstrap
cd ..
```

[`gradle-bootstrap`](gradle-bootstrap) is a tiny standalone Gradle build whose only job is
to run the `wrapper` task at the version named in `.sdkmanrc` and copy the results
(`gradlew`, `gradlew.bat`, `gradle/wrapper/`) up to the source root. After it runs, use
`./gradlew` for everything below.

### 4. Build the plugin

```bash
./gradlew buildPlugin
```

The plugin ZIP is written to `plugin/build/distributions/` and can be installed in IntelliJ IDEA
via **Settings > Plugins > ⚙ > Install Plugin from Disk**.

> The first build downloads the IntelliJ Platform (IntelliJ IDEA Ultimate) and the bundled
> plugins it compiles against. Expect it to take a while and pull down several GB;
> subsequent builds reuse the `.intellijPlatform` cache.

To build a specific version rather than the default in `gradle.properties`:

```bash
./gradlew buildPlugin -Pversion=262.0.1
```

### 5. Verify the build (optional)

```bash
./gradlew check                  # compile and run the test suite
./gradlew verifyPlugin           # IntelliJ Plugin Verifier
./gradlew rat                    # Apache RAT license audit
./gradlew runIde                 # launch a sandbox IDE with the plugin installed
./gradlew testCodeCoverageReport # aggregate JaCoCo coverage report
./gradlew ossIndexAudit          # Sonatype OSS Index vulnerability scan (needs SONATYPE_GUIDE_TOKEN)
```

To confirm the build is reproducible, see
[RELEASE.md](RELEASE.md#verifying-a-reproducible-build).

## Releasing

The release process, artifact verification, and reproducibility requirements are
documented in [RELEASE.md](RELEASE.md).

## Links

- [Apache Grails](https://grails.apache.org/)
- [Issue tracker](https://github.com/apache/grails-intellij-plugin/issues)
- [Mailing lists](https://grails.apache.org/community/#mailing-lists)

## License

Apache License, Version 2.0 — see [LICENSE](LICENSE) and [NOTICE](NOTICE).
