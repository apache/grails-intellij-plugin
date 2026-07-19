# Apache Grails IntelliJ Plugin

IntelliJ IDEA plugin for the [Apache Grails](https://grails.apache.org/) framework:
GSP language support (parsing, highlighting, completion, refactoring), Grails project
structure and navigation, run configurations, taglib/domain-class support, and
integrations for i18n, coverage, Hibernate, Maven, and language injection.

This codebase was originally developed by JetBrains s.r.o. and imported from
[JetBrains/intellij-obsolete-plugins](https://github.com/JetBrains/intellij-obsolete-plugins).

## Requirements

- IntelliJ IDEA Ultimate 2026.2+ (`sinceBuild` 262)
- Java 25 (the `grails-rt`, `grails-compiler-patch`, and `jps-plugin` modules target Java 8/11)

## Building from source

For building, using, and running from a **source distribution**, see the
[INSTALL](INSTALL) document. The steps below cover a clone of this repository.

### 1. Set up the tooling (JDK + Gradle)

The required JDK and Gradle versions are pinned in [`.sdkmanrc`](.sdkmanrc). The
recommended way to install them is with [SDKMAN!](https://sdkman.io); from the source
root run:

```
sdk env install
```

This installs the pinned Java (Liberica 25) and Gradle versions and selects them for the
current shell. You may also install those versions by any other means you prefer.

### 2. Bootstrap the Gradle wrapper (only if `gradlew` is missing)

A clone of this repository already contains the Gradle Wrapper, so you can skip this step
and use `./gradlew` directly.

A **source distribution** ships *without* the Gradle Wrapper jar (per ASF source-release
policy), so it must be regenerated once. With a local Gradle installed (from step 1), run:

```
cd gradle-bootstrap
gradle bootstrap
cd ..
```

This pins the wrapper to the version in `.sdkmanrc` and installs `gradlew` /
`gradlew.bat` at the source root. Afterwards, use `./gradlew` for every command below.

### 3. Build the plugin

```
./gradlew buildPlugin
```

The plugin ZIP is written to `build/distributions/` and can be installed in IntelliJ IDEA
via **Settings > Plugins > Install Plugin from Disk**.

> The first build downloads the IntelliJ Platform (IntelliJ IDEA Ultimate) and the bundled
> plugins it compiles against — expect network access and several GB of disk usage.

### Other useful tasks

```
./gradlew check                  # compile and run tests
./gradlew verifyPlugin           # IntelliJ Plugin Verifier
./gradlew rat                    # Apache RAT license audit
./gradlew runIde                 # launch a sandbox IDE with the plugin
./gradlew testCodeCoverageReport # aggregate JaCoCo coverage report
./gradlew ossIndexAudit          # Sonatype OSS Index vulnerability scan (needs SONATYPE_GUIDE_TOKEN)
```

To verify the build is reproducible, see [RELEASE.md](RELEASE.md#verifying-a-reproducible-build).

## Links

- [Apache Grails](https://grails.apache.org/)
- [Issue tracker](https://github.com/apache/grails-intellij-plugin/issues)
- [Mailing lists](https://grails.apache.org/community/#mailing-lists)

## License

Apache License, Version 2.0 — see [LICENSE](LICENSE) and [NOTICE](NOTICE).
