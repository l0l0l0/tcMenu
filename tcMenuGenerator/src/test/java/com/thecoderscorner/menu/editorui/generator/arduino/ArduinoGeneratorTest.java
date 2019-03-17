/*
 * Copyright (c)  2016-2019 https://www.thecoderscorner.com (Nutricherry LTD).
 * This product is licensed under an Apache license, see the LICENSE file in the top-level directory.
 *
 */

package com.thecoderscorner.menu.editorui.generator.arduino;

import com.thecoderscorner.menu.domain.state.MenuTree;
import com.thecoderscorner.menu.editorui.generator.util.LibraryStatus;
import com.thecoderscorner.menu.pluginapi.*;
import com.thecoderscorner.menu.pluginapi.model.CodeVariableBuilder;
import com.thecoderscorner.menu.pluginapi.model.FunctionCallBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.thecoderscorner.menu.editorui.util.TestUtils.assertEqualsIgnoringCRLF;
import static com.thecoderscorner.menu.editorui.util.TestUtils.buildSimpleTree;
import static com.thecoderscorner.menu.pluginapi.EmbeddedPlatform.ARDUINO32;
import static com.thecoderscorner.menu.pluginapi.EmbeddedPlatform.ARDUINO_AVR;
import static com.thecoderscorner.menu.pluginapi.PluginFileDependency.PackagingType;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

public class ArduinoGeneratorTest {

    private Path dir;

    @BeforeEach
    public void setUp() throws Exception {
        dir = Files.createTempDirectory("tcmenu");
    }

    @AfterEach
    public void tearDown() throws Exception {
        Files.walk(dir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    void testConversionForAvr() throws IOException {
        runConversionWith(ARDUINO_AVR, "/generator/template");
    }

    @Test
    void testConversionForSamd() throws IOException {
        runConversionWith(ARDUINO32, "/generator/template32");
    }

    private void runConversionWith(EmbeddedPlatform platform, String templateToUse) throws IOException {
        ArduinoSketchFileAdjuster adjuster = Mockito.mock(ArduinoSketchFileAdjuster.class);

        MenuTree tree = buildSimpleTree();
        ArduinoLibraryInstaller installer = Mockito.mock(ArduinoLibraryInstaller.class);
        Mockito.when(installer.statusOfAllLibraries()).thenReturn(new LibraryStatus(true, true, true));

        List<EmbeddedCodeCreator> generators = unitTestGenerator();
        ArduinoGenerator generator = new ArduinoGenerator(adjuster, installer, platform);

        assertTrue(generator.startConversion(dir, generators, tree));

        var cppGenerated = new String(Files.readAllBytes(dir.resolve(dir.getFileName() + "_menu.cpp")));
        var hGenerated = new String(Files.readAllBytes(dir.resolve(dir.getFileName() + "_menu.h")));
        var pluginGenerated = new String(Files.readAllBytes(dir.resolve("replacementSource.h")));

        var cppTemplate = new String(getClass().getResourceAsStream(templateToUse + ".cpp").readAllBytes());
        var hTemplate = new String(getClass().getResourceAsStream(templateToUse + ".h").readAllBytes());
        var expectedPlugin = new String(getClass().getResourceAsStream("/generator/replacementExpected.h").readAllBytes());

        cppGenerated = cppGenerated.replaceAll("#include \"tcmenu[^\"]*\"", "replacedInclude");
        cppTemplate = cppTemplate.replaceAll("#include \"tcmenu[^\"]*\"", "replacedInclude");

        // these files should line up. IF they do not because of the change in the ArduinoGenerator,
        // then make sure the change is good before adjusting the templates.
        assertEqualsIgnoringCRLF(cppTemplate, cppGenerated);
        assertEqualsIgnoringCRLF(hTemplate, hGenerated);
        assertEqualsIgnoringCRLF(expectedPlugin, pluginGenerated);

        Mockito.verify(adjuster).makeAdjustments(any(Consumer.class),
                eq(dir.resolve(dir.resolve(dir.getFileName() + ".ino")).toString()),
                eq(dir.getFileName().toString()), eq(List.of("callback1")));
    }

    private List<EmbeddedCodeCreator> unitTestGenerator() {
        EmbeddedCodeCreator gen = new AbstractCodeCreator() {
            @Override
            public List<CreatorProperty> properties() {
                return List.of(new CreatorProperty("A_DEFINE", "blah", "2", SubSystem.INPUT));
            }

            @Override
            protected void initCreator(String root) {
                addVariable(new CodeVariableBuilder()
                        .requiresHeader("header1.h", false)
                        .variableName("varName").variableType("VarType")
                        .param("1234.34")
                        .exportNeeded()
                );

                addFunctionCall(new FunctionCallBuilder().functionName("begin").objectName("lcd").param(16).param(2));

                addLibraryFiles(new PluginFileDependency(
                        "testSrc/replacementSource.h",
                        PackagingType.WITH_PLUGIN,
                        Map.of(
                                "Replacement1.h", "ChangedHeader.h",
                                "ReplacementServer", "ChangedServer")
                ));
            }
        };
        return Collections.singletonList(gen);
    }
}