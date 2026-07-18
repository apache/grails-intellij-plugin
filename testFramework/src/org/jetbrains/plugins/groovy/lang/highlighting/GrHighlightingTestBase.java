/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.plugins.groovy.lang.highlighting;

import com.intellij.codeInspection.InspectionProfileEntry;
import com.intellij.codeInspection.LocalInspectionTool;
import org.jetbrains.plugins.groovy.LightGroovyTestCase;
import org.jetbrains.plugins.groovy.codeInspection.untypedUnresolvedAccess.GrUnresolvedAccessInspection;
import org.jetbrains.plugins.groovy.util.TestUtils;

/**
 * @author Max Medvedev
 */
public abstract class GrHighlightingTestBase extends LightGroovyTestCase {

    public static final InspectionProfileEntry[] EMPTY_INSPECTION_PROFILE_ENTRY_ARRAY = new InspectionProfileEntry[0];

    @Override
    public String getBasePath() {
        return TestUtils.getTestDataPath() + "highlighting/";
    }

    public InspectionProfileEntry[] getCustomInspections() {
        return EMPTY_INSPECTION_PROFILE_ENTRY_ARRAY;
    }

    public void doTest(boolean checkWarnings, boolean checkInfos, boolean checkWeakWarnings, InspectionProfileEntry... tools) {
        myFixture.enableInspections(tools);
        myFixture.enableInspections(getCustomInspections());
        myFixture.testHighlighting(checkWarnings, checkInfos, checkWeakWarnings, getTestName(false) + ".groovy");
    }

    public void doTest(boolean checkWarnings, boolean checkInfos, InspectionProfileEntry... tools) {
        doTest(checkWarnings, checkInfos, true, tools);
    }

    public void doTest(boolean checkWarnings, InspectionProfileEntry... tools) {
        doTest(checkWarnings, false, true, tools);
    }

    public void doTest(InspectionProfileEntry... tools) {
        doTest(true, false, true, tools);
    }

    public void doRefTest(boolean checkWarnings, boolean checkInfos, boolean checkWeakWarnings, InspectionProfileEntry... tools) {
        myFixture.enableInspections(new GrUnresolvedAccessInspection());
        myFixture.enableInspections(tools);
        myFixture.enableInspections(getCustomInspections());
        myFixture.testHighlighting(checkWarnings, checkInfos, checkWeakWarnings, getTestName(false) + ".groovy");
    }

    public void doRefTest(InspectionProfileEntry... tools) {
        doRefTest(true, false, true, tools);
    }

    public void doTestHighlighting(String text,
                                   boolean checkWarnings,
                                   boolean checkInfos,
                                   boolean checkWeakWarnings,
                                   Class<? extends LocalInspectionTool>... inspections) {
        myFixture.configureByText("_.groovy", text);
        myFixture.enableInspections(inspections);
        myFixture.enableInspections(getCustomInspections());
        myFixture.testHighlighting(checkWarnings, checkInfos, checkWeakWarnings);
    }

    public void doTestHighlighting(String text,
                                   boolean checkWarnings,
                                   boolean checkInfos,
                                   Class<? extends LocalInspectionTool>... inspections) {
        doTestHighlighting(text, checkWarnings, checkInfos, true, inspections);
    }

    public void doTestHighlighting(String text, boolean checkWarnings, Class<? extends LocalInspectionTool>... inspections) {
        doTestHighlighting(text, checkWarnings, false, true, inspections);
    }

    public void doTestHighlighting(String text, Class<? extends LocalInspectionTool>... inspections) {
        doTestHighlighting(text, true, false, true, inspections);
    }
}
