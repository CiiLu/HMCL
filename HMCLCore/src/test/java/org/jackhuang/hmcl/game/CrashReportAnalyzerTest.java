/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2020  huangyuhui <huanghongxun2008@126.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.jackhuang.hmcl.game;

import org.jackhuang.hmcl.util.Log4jLevel;
import org.jackhuang.hmcl.util.Pair;
import org.jackhuang.hmcl.util.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class CrashReportAnalyzerTest {
    private String loadLog(String path) throws IOException {
        List<Pair<String, Log4jLevel>> logs = new ArrayList<>();
        InputStream is = CrashReportAnalyzerTest.class.getResourceAsStream(path);
        if (is == null) {
            throw new IllegalStateException("Resource not found: " + path);
        }
        return IOUtils.readFullyAsString(is);
    }

    private CrashReportAnalyzer.Result findResultByRule(Set<CrashReportAnalyzer.Result> results, CrashReportAnalyzer.Rule rule) {
        CrashReportAnalyzer.Result r = results.stream().filter(result -> result.getRule() == rule).findFirst().orElse(null);
        assertNotNull(r);
        return r;
    }

    @Test
    public void jdk9() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/java9.txt")),
                CrashReportAnalyzer.Rule.JDK_9);
    }

    @Test
    public void jadeForestOptifine() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/jade_forest_optifine.txt")),
                CrashReportAnalyzer.Rule.JADE_FOREST_OPTIFINE);
    }

    @Test
    public void rtssForestSodium() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/crash-report/rtss_forest_sodium.txt")),
                CrashReportAnalyzer.Rule.RTSS_FOREST_SODIUM);
    }

    @Test
    public void jvm32() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/jvm_32bit.txt")),
                CrashReportAnalyzer.Rule.JVM_32BIT);
    }

    @Test
    public void jvm321() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/jvm_32bit2.txt")),
                CrashReportAnalyzer.Rule.JVM_32BIT);
    }

    @Test
    public void modResolution() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/mod_resolution.txt")),
                CrashReportAnalyzer.Rule.MOD_RESOLUTION);
        assertEquals(("Errors were found!\n" +
                        " - Mod test depends on mod {fabricloader @ [>=0.11.3]}, which is missing!\n" +
                        " - Mod test depends on mod {fabric @ [*]}, which is missing!\n" +
                        " - Mod test depends on mod {java @ [>=16]}, which is missing!\n").replaceAll("\\s+", ""),
                result.getMatcher().group("reason").replaceAll("\\s+", ""));
    }

    @Test
    public void forgemodResolution() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/forgemod_resolution.txt")),
                CrashReportAnalyzer.Rule.FORGEMOD_RESOLUTION);
        assertEquals(("\tMod ID: 'vampirism', Requested by: 'werewolves', Expected range: '[1.9.0-beta.1,)', Actual version: '[MISSING]'\n").replaceAll("\\s+", ""),
                result.getMatcher().group("reason").replaceAll("\\s+", ""));
    }

    @Test
    public void forgeFoundDuplicateMods() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/forge_found_duplicate_mods.txt")),
                CrashReportAnalyzer.Rule.FORGE_FOUND_DUPLICATE_MODS);
        assertEquals(("\tMod ID: 'jei' from mod files: REIPluginCompatibilities-forge-12.0.93.jar, jei-1.20.1-forge-15.2.0.27.jar\n").replaceAll("\\s+", ""),
                result.getMatcher().group("reason").replaceAll("\\s+", ""));
    }

    @Test
    public void modResolutionCollection() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/mod_resolution_collection.txt")),
                CrashReportAnalyzer.Rule.MOD_RESOLUTION_COLLECTION);
        assertEquals("tabtps-fabric", result.getMatcher().group("sourcemod"));
        assertEquals("{fabricloader @ [>=0.11.1]}", result.getMatcher().group("destmod"));
    }

    @Test
    public void forgeEroor() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/crash-report/forge_error.txt")),
                CrashReportAnalyzer.Rule.FORGE_ERROR);
        assertEquals(("\nnet.minecraftforge.fml.common.MissingModsException: Mod pixelmon (Pixelmon) requires [forge@[14.23.5.2860,)]\n" +
                        "\tat net.minecraftforge.fml.common.Loader.sortModList(Loader.java:264) ~[Loader.class:?]\n" +
                        "\tat net.minecraftforge.fml.common.Loader.loadMods(Loader.java:570) ~[Loader.class:?]\n" +
                        "\tat net.minecraftforge.fml.client.FMLClientHandler.beginMinecraftLoading(FMLClientHandler.java:232) [FMLClientHandler.class:?]\n" +
                        "\tat net.minecraft.client.Minecraft.func_71384_a(Minecraft.java:467) [bib.class:?]\n" +
                        "\tat net.minecraft.client.Minecraft.func_99999_d(Minecraft.java:378) [bib.class:?]\n" +
                        "\tat net.minecraft.client.main.Main.main(SourceFile:123) [Main.class:?]\n" +
                        "\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[?:1.8.0_131]\n" +
                        "\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62) ~[?:1.8.0_131]\n" +
                        "\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[?:1.8.0_131]\n" +
                        "\tat java.lang.reflect.Method.invoke(Method.java:498) ~[?:1.8.0_131]\n" +
                        "\tat net.minecraft.launchwrapper.Launch.launch(Launch.java:135) [launchwrapper-1.12.jar:?]\n" +
                        "\tat net.minecraft.launchwrapper.Launch.main(Launch.java:28) [launchwrapper-1.12.jar:?]\n" +
                        "\tat sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[?:1.8.0_131]\n" +
                        "\tat sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62) ~[?:1.8.0_131]\n" +
                        "\tat sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[?:1.8.0_131]\n" +
                        "\tat java.lang.reflect.Method.invoke(Method.java:498) ~[?:1.8.0_131]\n" +
                        "\tat oolloo.jlw.Wrapper.invokeMain(Wrapper.java:58) [JavaWrapper.jar:?]\n" +
                        "\tat oolloo.jlw.Wrapper.main(Wrapper.java:51) [JavaWrapper.jar:?]").replaceAll("\\s+", ""),
                result.getMatcher().group("reason").replaceAll("\\s+", ""));
    }

    @Test
    public void modResolution0() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/crash-report/mod_resolution0.txt")),
                CrashReportAnalyzer.Rule.MOD_RESOLUTION0);
    }

    @Test
    public void tooOldJava() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/too_old_java.txt")),
                CrashReportAnalyzer.Rule.TOO_OLD_JAVA);
        assertEquals("60", result.getMatcher().group("expected"));
    }

    @Test
    public void tooOldJava1() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/crash-report/too_old_java.txt")),
                CrashReportAnalyzer.Rule.TOO_OLD_JAVA);
        assertEquals("52", result.getMatcher().group("expected"));
    }

    @Test
    public void tooOldJava2() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/crash-report/too_old_java2.txt")),
                CrashReportAnalyzer.Rule.TOO_OLD_JAVA);
        assertEquals("52", result.getMatcher().group("expected"));
    }

    @Test
    public void javaVersionIsTooHigh() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/java_version_is_too_high.txt")),
                CrashReportAnalyzer.Rule.JAVA_VERSION_IS_TOO_HIGH);
    }

    @Test
    public void securityException() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/crash-report/security.txt")),
                CrashReportAnalyzer.Rule.FILE_CHANGED);
        assertEquals("assets/minecraft/texts/splashes.txt", result.getMatcher().group("file"));
    }

    @Test
    public void noClassDefFoundError1() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/crash-report/no_class_def_found_error.txt")),
                CrashReportAnalyzer.Rule.NO_CLASS_DEF_FOUND_ERROR);
        assertEquals("blk", result.getMatcher().group("class"));
    }

    @Test
    public void noClassDefFoundError2() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/crash-report/no_class_def_found_error2.txt")),
                CrashReportAnalyzer.Rule.NO_CLASS_DEF_FOUND_ERROR);
        assertEquals("cer", result.getMatcher().group("class"));
    }

    @Test
    public void fileAlreadyExists() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/crash-report/file_already_exists.txt")),
                CrashReportAnalyzer.Rule.FILE_ALREADY_EXISTS);
        assertEquals(
                "D:\\Games\\Minecraft\\Minecraft Longtimeusing\\.minecraft\\versions\\1.12.2-forge1.12.2-14.23.5.2775\\config\\pvpsettings.txt",
                result.getMatcher().group("file"));
    }

    @Test
    public void loaderExceptionModCrash() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/crash-report/loader_exception_mod_crash.txt")),
                CrashReportAnalyzer.Rule.LOADING_CRASHED_FORGE);
        assertEquals("Better PvP", result.getMatcher().group("name"));
        assertEquals("xaerobetterpvp", result.getMatcher().group("id"));
    }

    @Test
    public void loaderExceptionModCrash2() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/crash-report/loader_exception_mod_crash2.txt")),
                CrashReportAnalyzer.Rule.LOADING_CRASHED_FORGE);
        assertEquals("Inventory Sort", result.getMatcher().group("name"));
        assertEquals("invsort", result.getMatcher().group("id"));
    }

    @Test
    public void loaderExceptionModCrash3() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/crash-report/loader_exception_mod_crash3.txt")),
                CrashReportAnalyzer.Rule.LOADING_CRASHED_FORGE);
        assertEquals("SuperOres", result.getMatcher().group("name"));
        assertEquals("superores", result.getMatcher().group("id"));
    }

    @Test
    public void loaderExceptionModCrash4() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/crash-report/loader_exception_mod_crash4.txt")),
                CrashReportAnalyzer.Rule.LOADING_CRASHED_FORGE);
        assertEquals("Kathairis", result.getMatcher().group("name"));
        assertEquals("kathairis", result.getMatcher().group("id"));
    }

    @Test
    public void loadingErrorFabric() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/crash-report/loading_error_fabric.txt")),
                CrashReportAnalyzer.Rule.LOADING_CRASHED_FABRIC);
        assertEquals("test", result.getMatcher().group("id"));
    }

    @Test
    public void graphicsDriver() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/crash-report/graphics_driver.txt")),
                CrashReportAnalyzer.Rule.GRAPHICS_DRIVER);
    }

    @Test
    public void graphicsDriverJVM() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/graphics_driver.txt")),
                CrashReportAnalyzer.Rule.GRAPHICS_DRIVER);
    }

    @Test
    public void splashScreen() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/crash-report/splashscreen.txt")),
                CrashReportAnalyzer.Rule.GRAPHICS_DRIVER);
    }

    @Test
    public void macosFailedToFindServicePortForDisplay() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/macos_failed_to_find_service_port_for_display.txt")),
                CrashReportAnalyzer.Rule.MACOS_FAILED_TO_FIND_SERVICE_PORT_FOR_DISPLAY);
    }

    @Test
    public void modName() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/mod_name.txt")),
                CrashReportAnalyzer.Rule.MOD_NAME);
    }

    @Test
    public void openj9() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/openj9.txt")),
                CrashReportAnalyzer.Rule.OPENJ9);
    }

    @Test
    public void resolutionTooHigh() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/crash-report/resourcepack_resolution.txt")),
                CrashReportAnalyzer.Rule.RESOLUTION_TOO_HIGH);
    }

    @Test
    public void bootstrapFailed() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/bootstrap.txt")),
                CrashReportAnalyzer.Rule.BOOTSTRAP_FAILED);
        assertEquals("prefab", result.getMatcher().group("id"));
    }

    @Test
    public void mixinApplyModFailed() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/mixin_apply_mod_failed.txt")),
                CrashReportAnalyzer.Rule.MIXIN_APPLY_MOD_FAILED);
        assertEquals("enhancedblockentities", result.getMatcher().group("id"));
    }

    @Test
    public void unsatisfiedLinkError() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/unsatisfied_link_error.txt")),
                CrashReportAnalyzer.Rule.UNSATISFIED_LINK_ERROR);
        assertEquals("lwjgl.dll", result.getMatcher().group("name"));
    }

    @Test
    public void outOfMemoryMC() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/crash-report/out_of_memory.txt")),
                CrashReportAnalyzer.Rule.OUT_OF_MEMORY);
    }

    @Test
    public void outOfMemoryJVM() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/out_of_memory.txt")),
                CrashReportAnalyzer.Rule.OUT_OF_MEMORY);
    }

    @Test
    public void outOfMemoryJVM1() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/out_of_memory2.txt")),
                CrashReportAnalyzer.Rule.OUT_OF_MEMORY);
    }

    @Test
    public void memoryExceeded() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/memory_exceeded.txt")),
                CrashReportAnalyzer.Rule.MEMORY_EXCEEDED);
    }

    @Test
    public void config() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/crash-report/config.txt")),
                CrashReportAnalyzer.Rule.CONFIG);
        assertEquals("jumbofurnace", result.getMatcher().group("id"));
        assertEquals("jumbofurnace-server.toml", result.getMatcher().group("file"));
    }

    @Test
    public void fabricWarnings() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/fabric_warnings.txt")),
                CrashReportAnalyzer.Rule.FABRIC_WARNINGS);
        assertEquals((" - Conflicting versions found for fabric-api-base: used 0.3.0+a02b446313, also found 0.3.0+a02b44633d, 0.3.0+a02b446318\n" +
                        " - Conflicting versions found for fabric-rendering-data-attachment-v1: used 0.1.5+a02b446313, also found 0.1.5+a02b446318\n" +
                        " - Conflicting versions found for fabric-rendering-fluids-v1: used 0.1.13+a02b446318, also found 0.1.13+a02b446313\n" +
                        " - Conflicting versions found for fabric-lifecycle-events-v1: used 1.4.4+a02b44633d, also found 1.4.4+a02b446318\n" +
                        " - Mod 'Sodium Extra' (sodium-extra) recommends any version of mod reeses-sodium-options, which is missing!\n" +
                        "\t - You must install any version of reeses-sodium-options.\n" +
                        " - Conflicting versions found for fabric-screen-api-v1: used 1.0.4+155f865c18, also found 1.0.4+198a96213d\n" +
                        " - Conflicting versions found for fabric-key-binding-api-v1: used 1.0.4+a02b446318, also found 1.0.4+a02b44633d\n").replaceAll("\\s+", ""),
                result.getMatcher().group("reason").replaceAll("\\s+", ""));
    }

    @Test
    public void fabricWarnings1() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/fabric_warnings2.txt")),
                CrashReportAnalyzer.Rule.FABRIC_WARNINGS);
        assertEquals(("net.fabricmc.loader.impl.FormattedException: Mod resolution encountered an incompatible mod set!\n" +
                        "A potential solution has been determined:\n" +
                        "\t - Install roughlyenoughitems, version 6.0.2 or later.\n" +
                        "Unmet dependency listing:\n" +
                        "\t - Mod 'Roughly Searchable' (roughlysearchable) 2.2.1+1.17.1 requires version 6.0.2 or later of roughlyenoughitems, which is missing!\n" +
                        "\tat net.fabricmc.loader.impl.FabricLoaderImpl.load(FabricLoaderImpl.java:190) ~").replaceAll("\\s+", ""),
                result.getMatcher().group("reason").replaceAll("\\s+", ""));
    }

    @Test
    public void fabricWarnings2() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/fabric_warnings3.txt")),
                CrashReportAnalyzer.Rule.FABRIC_WARNINGS);
        assertEquals(("net.fabricmc.loader.impl.FormattedException: Some of your mods are incompatible with the game or each other!\n" +
                        "确定了一种可能的解决方法，这样做可能会解决你的问题：\n" +
                        "\t - 安装 fabric-api，任意版本。\n" +
                        "\t - 安装 sodium，0.5.6 及以上版本。\n" +
                        "更多信息：\n" +
                        "\t - 模组 'Sodium Extra' (sodium-extra) 0.5.4+mc1.20.4-build.116 需要 fabric-api 的 任意版本，但没有安装它！\n" +
                        "\t - 模组 'Sodium Extra' (sodium-extra) 0.5.4+mc1.20.4-build.116 需要 sodium 的 0.5.6 及以上版本，但没有安装它！\n" +
                        "\tat net.fabricmc.loader.impl.FormattedException.ofLocalized(FormattedException.java:51) ~").replaceAll("\\s+", ""),
                result.getMatcher().group("reason").replaceAll("\\s+", ""));
    }

    @Test
    public void fabricConflicts() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/fabric-mod-conflict.txt")),
                CrashReportAnalyzer.Rule.MOD_RESOLUTION_CONFLICT);
        assertEquals("phosphor", result.getMatcher().group("sourcemod"));
        assertEquals("{starlight @ [*]}", result.getMatcher().group("destmod"));
    }

    @Test
    public void fabricMissing() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/fabric-mod-missing.txt")),
                CrashReportAnalyzer.Rule.MOD_RESOLUTION_MISSING);
        assertEquals("pca", result.getMatcher().group("sourcemod"));
        assertEquals("{fabric @ [>=0.39.2]}", result.getMatcher().group("destmod"));
    }

    @Test
    public void fabric0_12() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/fabric-version-0.12.txt")),
                CrashReportAnalyzer.Rule.FABRIC_VERSION_0_12);
    }

    @Test
    public void twilightForestOptiFineIncompatible() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/crash-report/mod/twilightforest_optifine_incompatibility.txt")),
                CrashReportAnalyzer.Rule.TWILIGHT_FOREST_OPTIFINE);
    }

    @Test
    public void performantOptiFineIncompatible() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/crash-report/mod/performant_optifine_incompatibility.txt")),
                CrashReportAnalyzer.Rule.PERFORMANT_FOREST_OPTIFINE);
    }

    @Test
    public void neoforgeForestOptiFineIncompatible() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/crash-report/mod/neoforgeforest_optifine_incompatibility.txt")),
                CrashReportAnalyzer.Rule.NEOFORGE_FOREST_OPTIFINE);
    }

    @Test
    public void fabricMissingMinecraft() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/fabric-minecraft.txt")),
                CrashReportAnalyzer.Rule.MOD_RESOLUTION_MISSING_MINECRAFT);
        assertEquals("fabric", result.getMatcher().group("mod"));
        assertEquals("[~1.16.2-alpha.20.28.a]", result.getMatcher().group("version"));
    }

    @Test
    public void optifineRepeatInstallation() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/optifine_repeat_installation.txt")),
                CrashReportAnalyzer.Rule.OPTIFINE_REPEAT_INSTALLATION);
    }

    @Test
    public void incompleteForgeInstallation() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/incomplete_forge_installation.txt")),
                CrashReportAnalyzer.Rule.INCOMPLETE_FORGE_INSTALLATION);
    }

    @Test
    public void incompleteForgeInstallation2() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/incomplete_forge_installation2.txt")),
                CrashReportAnalyzer.Rule.INCOMPLETE_FORGE_INSTALLATION);
    }

    @Test
    public void incompleteForgeInstallation3() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/incomplete_forge_installation3.txt")),
                CrashReportAnalyzer.Rule.INCOMPLETE_FORGE_INSTALLATION);
    }

    @Test
    public void incompleteForgeInstallation4() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/incomplete_forge_installation4.txt")),
                CrashReportAnalyzer.Rule.INCOMPLETE_FORGE_INSTALLATION);
    }

    @Test
    public void incompleteForgeInstallation5() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/incomplete_forge_installation5.txt")),
                CrashReportAnalyzer.Rule.INCOMPLETE_FORGE_INSTALLATION);
    }

    @Test
    public void incompleteForgeInstallation6() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/incomplete_forge_installation6.txt")),
                CrashReportAnalyzer.Rule.INCOMPLETE_FORGE_INSTALLATION);
    }

    @Test
    public void incompleteForgeInstallation7() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/incomplete_forge_installation7.txt")),
                CrashReportAnalyzer.Rule.INCOMPLETE_FORGE_INSTALLATION);
    }

    @Test
    public void forgeRepeatInstallation() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/forge_repeat_installation.txt")),
                CrashReportAnalyzer.Rule.FORGE_REPEAT_INSTALLATION);
    }

    @Test
    public void forgeRepeatInstallation1() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/forge_repeat_installation2.txt")),
                CrashReportAnalyzer.Rule.FORGE_REPEAT_INSTALLATION);
    }

    @Test
    public void needJDK11() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/crash-report/need_jdk11.txt")),
                CrashReportAnalyzer.Rule.NEED_JDK11);
    }

    @Test
    public void needJDK112() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/crash-report/need_jdk112.txt")),
                CrashReportAnalyzer.Rule.NEED_JDK11);
    }

    @Test
    public void needJDK113() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/crash-report/need_jdk113.txt")),
                CrashReportAnalyzer.Rule.NEED_JDK11);
    }

    @Test
    public void optifineIsNotCompatibleWithForge() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/optifine_is_not_compatible_with_forge.txt")),
                CrashReportAnalyzer.Rule.OPTIFINE_IS_NOT_COMPATIBLE_WITH_FORGE);
    }

    @Test
    public void optifineIsNotCompatibleWithForge1() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/optifine_is_not_compatible_with_forge2.txt")),
                CrashReportAnalyzer.Rule.OPTIFINE_IS_NOT_COMPATIBLE_WITH_FORGE);
    }

    @Test
    public void optifineIsNotCompatibleWithForge2() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/optifine_is_not_compatible_with_forge3.txt")),
                CrashReportAnalyzer.Rule.OPTIFINE_IS_NOT_COMPATIBLE_WITH_FORGE);
    }

    @Test
    public void optifineIsNotCompatibleWithForge3() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/optifine_is_not_compatible_with_forge4.txt")),
                CrashReportAnalyzer.Rule.OPTIFINE_IS_NOT_COMPATIBLE_WITH_FORGE);
    }

    @Test
    public void optifineIsNotCompatibleWithForge4() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/optifine_is_not_compatible_with_forge5.txt")),
                CrashReportAnalyzer.Rule.OPTIFINE_IS_NOT_COMPATIBLE_WITH_FORGE);
    }

    @Test
    public void optifineIsNotCompatibleWithForge5() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/optifine_is_not_compatible_with_forge6.txt")),
                CrashReportAnalyzer.Rule.OPTIFINE_IS_NOT_COMPATIBLE_WITH_FORGE);
    }

    @Test
    public void shadersMod() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/shaders_mod.txt")),
                CrashReportAnalyzer.Rule.SHADERS_MOD);
    }

    @Test
    public void installMixinbootstrap() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/logs/install_mixinbootstrap.txt")),
                CrashReportAnalyzer.Rule.INSTALL_MIXINBOOTSTRAP);
    }

    @Test
    public void nightconfigfixes() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/crash-report/night_config_fixes.txt")),
                CrashReportAnalyzer.Rule.NIGHT_CONFIG_FIXES);
    }

    @Test
    public void customNpc() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/crash-report/mod/customnpc.txt")),
                CrashReportAnalyzer.Rule.ENTITY);
        assertEquals("customnpcs.CustomNpc (noppes.npcs.entity.EntityCustomNpc)",
                result.getMatcher().group("type"));
        assertEquals("99942.59, 4.00, 100000.98",
                result.getMatcher().group("location"));

        assertEquals(
                new HashSet<>(Arrays.asList("npcs", "noppes")),
                CrashReportAnalyzer.findKeywordsFromCrashReport(loadLog("/crash-report/mod/customnpc.txt")));
    }

    @Test
    public void tconstruct() throws IOException {
        CrashReportAnalyzer.Result result = findResultByRule(
                CrashReportAnalyzer.analyze(loadLog("/crash-report/mod/tconstruct.txt")),
                CrashReportAnalyzer.Rule.BLOCK);
        assertEquals("Block{tconstruct:seared_drain}[active=true,facing=north]",
                result.getMatcher().group("type"));
        assertEquals("World: (1370,92,-738), Chunk: (at 10,5,14 in 85,-47; contains blocks 1360,0,-752 to 1375,255,-737), Region: (2,-2; contains chunks 64,-64 to 95,-33, blocks 1024,0,-1024 to 1535,255,-513)",
                result.getMatcher().group("location"));

        assertEquals(
                new HashSet<>(Arrays.asList("tconstruct", "slimeknights", "smeltery")),
                CrashReportAnalyzer.findKeywordsFromCrashReport(loadLog("/crash-report/mod/tconstruct.txt")));
    }

    @Test
    public void bettersprinting() throws IOException {
        assertEquals(
                new HashSet<>(Arrays.asList("chylex", "bettersprinting")),
                CrashReportAnalyzer.findKeywordsFromCrashReport(loadLog("/crash-report/mod/bettersprinting.txt")));
    }

    @Test
    public void ic2() throws IOException {
        assertEquals(
                Collections.singleton("ic2"),
                CrashReportAnalyzer.findKeywordsFromCrashReport(loadLog("/crash-report/mod/ic2.txt")));
    }

    @Test
    public void nei() throws IOException {
        assertEquals(
                new HashSet<>(Arrays.asList("nei", "codechicken", "guihook")),
                CrashReportAnalyzer.findKeywordsFromCrashReport(loadLog("/crash-report/mod/nei.txt")));
    }

    @Test
    public void netease() throws IOException {
        assertEquals(
                new HashSet<>(Arrays.asList("netease", "battergaming")),
                CrashReportAnalyzer.findKeywordsFromCrashReport(loadLog("/crash-report/mod/netease.txt")));
    }

    @Test
    public void flammpfeil() throws IOException {
        assertEquals(
                new HashSet<>(Arrays.asList("slashblade", "flammpfeil")),
                CrashReportAnalyzer.findKeywordsFromCrashReport(loadLog("/crash-report/mod/flammpfeil.txt")));
    }

    @Test
    public void creativemd() throws IOException {
        assertEquals(
                new HashSet<>(Arrays.asList("creativemd", "itemphysic")),
                CrashReportAnalyzer.findKeywordsFromCrashReport(loadLog("/crash-report/mod/creativemd.txt")));
    }

    @Test
    public void mapletree() throws IOException {
        assertEquals(
                new HashSet<>(Arrays.asList("MapleTree", "bamboo", "uraniummc", "ecru")),
                CrashReportAnalyzer.findKeywordsFromCrashReport(loadLog("/crash-report/mod/mapletree.txt")));
    }

    @Test
    public void thaumcraft() throws IOException {
        assertEquals(
                Collections.singleton("thaumcraft"),
                CrashReportAnalyzer.findKeywordsFromCrashReport(loadLog("/crash-report/mod/thaumcraft.txt")));
    }

    @Test
    public void shadersmodcore() throws IOException {
        assertEquals(
                Collections.singleton("shadersmodcore"),
                CrashReportAnalyzer.findKeywordsFromCrashReport(loadLog("/crash-report/mod/shadersmodcore.txt")));
    }

    @Test
    public void twilightforest() throws IOException {
        assertEquals(
                Collections.singleton("twilightforest"),
                CrashReportAnalyzer.findKeywordsFromCrashReport(loadLog("/crash-report/mod/twilightforest.txt")));
    }

    @Test
    public void optifine() throws IOException {
        assertEquals(
                Collections.singleton("OptiFine"),
                CrashReportAnalyzer.findKeywordsFromCrashReport(loadLog("/crash-report/mod/twilightforest_optifine_incompatibility.txt")));
    }

    @Test
    public void wizardry() throws IOException {
        assertEquals(
                new HashSet<>(Arrays.asList("wizardry", "electroblob", "projectile")),
                CrashReportAnalyzer.findKeywordsFromCrashReport(loadLog("/crash-report/mod/wizardry.txt")));
    }

    @Test
    public void icycream() throws IOException {
        assertEquals(
                new HashSet<>(Collections.singletonList("icycream")),
                CrashReportAnalyzer.findKeywordsFromCrashReport(loadLog("/crash-report/mod/icycream.txt")));
    }
}
