/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2026 huangyuhui <huanghongxun2008@126.com> and contributors
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
package org.jackhuang.hmcl.patchpack;

import com.google.gson.JsonParseException;
import org.jackhuang.hmcl.util.gson.JsonUtils;
import org.jackhuang.hmcl.util.io.CompressingUtils;
import org.jackhuang.hmcl.util.io.FileUtils;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/// Utilities for reading and installing [patch packs][PatchPackInfo].
@NotNullByDefault
public final class PatchPackHelper {

    private PatchPackHelper() {
    }

    /// Tests whether the given file may be a patch pack by its extension.
    ///
    /// Patch packs are ZIP archives, so this only filters the files offered in file dialogs; the
    /// authoritative test is [PatchPackInfo#FILE_NAME] being present in the archive.
    ///
    /// @param file the file to test
    /// @return `true` if the file name ends with `.zip`
    public static boolean isPatchPackByExtension(Path file) {
        return "zip".equalsIgnoreCase(FileUtils.getExtension(file));
    }

    /// Detects the charset used to decode the names of the archive entries.
    ///
    /// @param file the patch pack archive
    /// @return the detected charset
    /// @throws IOException if the archive cannot be read
    public static Charset findSuitableEncoding(Path file) throws IOException {
        return CompressingUtils.findSuitableEncoding(file);
    }

    /// Reads the patch pack information from `patchpackinfo.json` in the root directory of the
    /// archive.
    ///
    /// @param file    the patch pack archive
    /// @param charset the charset used to decode the names of the archive entries, or `null` to
    ///                detect it automatically
    /// @return the parsed and validated patch pack information, never `null`
    /// @throws IOException if the archive cannot be read or does not contain a valid information file
    public static PatchPackInfo readPatchPackInfo(Path file, @Nullable Charset charset) throws IOException {
        Charset encoding = charset != null ? charset : findSuitableEncoding(file);

        String json;
        try (var zip = CompressingUtils.openZipFile(file, encoding)) {
            var entry = zip.getEntry(PatchPackInfo.FILE_NAME);
            if (entry == null)
                throw new IOException("Missing " + PatchPackInfo.FILE_NAME + " in the patch pack");

            try (InputStream input = zip.getInputStream(entry)) {
                json = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        PatchPackInfo info;
        try {
            info = JsonUtils.fromJson(json, PatchPackInfo.class);
        } catch (JsonParseException e) {
            throw new IOException("Malformed " + PatchPackInfo.FILE_NAME, e);
        }

        if (info == null)
            throw new IOException("Empty " + PatchPackInfo.FILE_NAME);

        try {
            info.validate();
        } catch (JsonParseException e) {
            throw new IOException("Invalid " + PatchPackInfo.FILE_NAME, e);
        }

        return info;
    }

    /// Creates a task installing the given patch pack over the run directory of an instance.
    ///
    /// @param zipFile       the patch pack archive
    /// @param charset       the charset used to decode the names of the archive entries
    /// @param info          the patch pack information read from `zipFile`
    /// @param runDirectory  the run directory of the target instance
    /// @return the install task
    public static PatchPackInstallTask getInstallTask(Path zipFile, Charset charset, PatchPackInfo info, Path runDirectory) {
        return new PatchPackInstallTask(zipFile, charset, info, runDirectory);
    }
}
