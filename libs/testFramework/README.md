# testFramework

Test fixtures and base classes for **this plugin's own tests**. Not shipped — it is consumed as
`testImplementation project(':testFramework')` and does not appear in the distribution ZIP.

It also vendors copies of IntelliJ Platform test infrastructure (Gradle importing test cases, SDK
test helpers, EEL test utilities) that the platform does not publish in a consumable form.

## Grails version support

Not applicable: this code targets the IntelliJ Platform test framework, not Grails.

## Gotcha: test data is resolved from the working directory

`GrailsTestUtil.getTestRootPath()` resolves paths with `new File("")`, i.e. the **test JVM's working
directory**, which Gradle defaults to the project directory of the project that owns the `test` task:

```java
public static String getTestRootPath(String directory) {
  return FileUtil.toSystemIndependentName(new File("").getAbsoluteFile() + directory);
}
```

Roughly 25 test classes call `getTestRootPath("/testdata/...")`. `testdata/` must therefore live in
the project that owns the tests. Do not "fix" this by setting `test { workingDir = ... }` — that
hides the coupling rather than removing it.

## Build

Kotlin is still required here: 16 of the vendored platform files are Kotlin and are deliberately
kept as-is so future platform upgrades can be reconciled against upstream.
