# grails-rt

Runtime payload injected into the **user's** Grails/Gradle JVM. `rt` is the JVM convention for a
runtime sidecar (as in the old JDK `rt.jar`); IntelliJ plugins ship `*-rt.jar` for this pattern.

Nothing here runs in the IDE. The plugin locates this jar on disk with
`PathUtil.getJarPathForClass(GrailsRtMarker.class)` and adds it to a launched process's classpath —
so the **jar's file name is not a contract**, unlike the compiler plugins.

## Contents, and which Grails era uses each

| Entry | Grails versions | Status |
| --- | --- | --- |
| `idea-injected-traits.groovy` | 3.0+ | **live** — GORM trait discovery (`TraitInjectorService`) |
| `META-INF/commands/intellij-command-proxy.groovy` | 3.0+ | **live** — `Grails3TestCommandLineState` |
| `GrailsRtMarker` | any | **live** — empty class, exists only so the jar can be located |
| `GrailsIdeaTestListener` | 1.x–2.x | **dormant** |
| `Agent`, `AddAgentJarToClassPathTransformer`, `ForkListenerTransformer` | 1.x–2.x | **dormant** |

## Grails version support

- **Minimum:** 1.1 (the legacy classes compile against `grails-bootstrap:1.2.0`)
- **Maximum:** none — the two Groovy resources are used by the Grails 3.0+ code paths
  (`Grails3GradleCommandExecutor`, `Grails3InstallationCommandExecutor`,
  `GrailsProjectResolverExtension`, `GrailsTaskManagerExtension`)

Grails 3+ only ever loads the Groovy resources and the marker, none of which touch
`grails-bootstrap`, so the legacy classes being unloadable there is harmless.

## Dormant code

Both of these arrived already-orphaned in the initial JetBrains import (`3d4bc40`); neither was
broken by later work.

- **`GrailsExecutionUtils.addAgentJar` has no callers**, so the `-javaagent:` path never executes.
  The jar *is* a valid agent (`Premain-Class` is declared on the `jar` task), but the capability is
  unused. The attributes must be set on the task, not via a `META-INF/MANIFEST.MF` resource —
  Gradle's `Jar` task writes its own manifest and silently shadows such a resource, which is why the
  packaged jar previously carried only `Manifest-Version: 1.0`.
- **`GrailsIdeaTestListener`** implements `grails.build.GrailsBuildListener`, the Grails 2 build
  system API. Its only reference is `GrailsTaskManagerExtension`, gated on a Gradle task named
  `grails-test-app` that only the old Grails Gradle plugin defines — so it is unreachable on
  Grails 3+ as well.

## Build

Java 8 bytecode: this runs in whatever JVM the user's Grails build uses.
