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

## Building

```
./gradlew buildPlugin
```

The plugin ZIP is written to `build/distributions/`.

Other useful tasks:

```
./gradlew check          # compile and run tests
./gradlew verifyPlugin   # IntelliJ Plugin Verifier
./gradlew rat            # Apache RAT license audit
./gradlew runIde         # launch a sandbox IDE with the plugin
```

## Links

- [Apache Grails](https://grails.apache.org/)
- [Issue tracker](https://github.com/apache/grails-intellij-plugin/issues)
- [Mailing lists](https://grails.apache.org/community/#mailing-lists)

## License

Apache License, Version 2.0 — see [LICENSE](LICENSE) and [NOTICE](NOTICE).
