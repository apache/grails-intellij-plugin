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

Roughly half this jar is unreachable. All of it arrived already-orphaned in the initial JetBrains
import (`3d4bc40`) — `git log -S"addAgentJar"` returns that one commit, so none of it was wired up
and later broken. **Do not treat these as bugs**; revive-or-delete is tracked as item 0.1b in
[IMPROVEMENT-PLAN.md](../IMPROVEMENT-PLAN.md).

| Dormant | Why it is unreachable |
| --- | --- |
| `GrailsExecutionUtils.addAgentJar` | no callers, so the `-javaagent:` path never executes |
| `Agent` | reached only via `addAgentJar` |
| `AddAgentJarToClassPathTransformer` | installed only by `Agent.premain` |
| `ForkListenerTransformer` | installed only by `Agent.premain`, and only when `idea.grails.kind.file` is set |
| `GrailsIdeaTestListener` | implements `grails.build.GrailsBuildListener`, a Grails 2 API absent from 3+. Its only reference is `GrailsTaskManagerExtension`, gated on a Gradle task named `grails-test-app` that only the old Grails Gradle plugin defines — so unreachable on Grails 3+ too |

The jar *is* nonetheless a valid Java agent: `Premain-Class` and `Can-Redefine-Classes` are declared
on the `jar` task. They cannot live in a `META-INF/MANIFEST.MF` resource — Gradle's `Jar` task writes
its own manifest and silently shadows one, which is why the packaged jar previously carried only
`Manifest-Version: 1.0` and would have failed a `-javaagent:` launch with *"Failed to find
Premain-Class manifest attribute."* Restoring it changed no behaviour, since nothing injects the
agent; it just means the jar no longer lies about what it is.

## Build

Java 8 bytecode: this runs in whatever JVM the user's Grails build uses.
