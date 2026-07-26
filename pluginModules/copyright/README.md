# copyright

IntelliJ **content module** adding copyright-header support to GSP files, so the IDE's Copyright
feature can insert and update headers in `.gsp` sources.

`UpdateGspFileCopyright` derives from the Copyright plugin's `UpdatePsiFileCopyright` and needs
nothing from javaee/JSP — see that class for why the JSP original's checks were inert here.

## Grails version support

- **Minimum / maximum:** no version gating. The feature is about GSP file syntax, which has not
  changed in a way this code cares about.

## Packaging

Content module `org.apache.grails.intellij.module.copyright`, shipped as
`lib/modules/org.apache.grails.intellij.module.copyright.jar`. The module name, the descriptor file name and the
jar base name are one three-way contract — see the `intellij-module` convention plugin.

Requires the `com.intellij.copyright` plugin at runtime.
