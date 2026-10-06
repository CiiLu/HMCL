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
import com.google.gson.annotations.SerializedName;
import org.jackhuang.hmcl.util.gson.Validation;
import org.jackhuang.hmcl.util.versioning.MavenVersionRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

/// The information file of a patch pack, which is stored as `patchpackinfo.json` in the root
/// directory of a patch pack archive.
///
/// A patch pack is a ZIP archive whose content is extracted over the run directory of an existing
/// instance, optionally after applying the file operations described by [#diff()].
///
/// @param formatVersion       the version of the patch pack format, must be [#CURRENT_FORMAT_VERSION]
/// @param name               the display name of the patch pack
/// @param description        the description of the patch pack, or `null` if it is absent
/// @param modpackVersionRange the Maven version range of the modpack versions this patch pack is
///                            designed for, or `null` if the range is absent
/// @param authors            the authors of the patch pack, or `null` if it is absent
/// @param url                the page where the patch pack is published, or `null` if it is absent
/// @param diff               the file operations performed before the archive content is extracted,
///                           or `null` if there are no such operations
@NotNullByDefault
public record PatchPackInfo(
        @SerializedName("formatVersion") int formatVersion,
        @SerializedName("name") @NotNull String name,
        @SerializedName("description") @Nullable String description,
        @SerializedName("modpackVersionRange") @Nullable String modpackVersionRange,
        @SerializedName("authors") @Nullable @Unmodifiable List<String> authors,
        @SerializedName("url") @Nullable String url,
        @SerializedName("diff") @Nullable Diff diff
) implements Validation {

    /// The version of the patch pack format supported by this launcher.
    public static final int CURRENT_FORMAT_VERSION = 1;

    /// The name of the information file stored in the root directory of a patch pack archive.
    public static final String FILE_NAME = "patchpackinfo.json";

    /// The parsed [#modpackVersionRange()], or `null` if the range is absent or malformed.
    ///
    /// @return the parsed version range, or `null` if it cannot be parsed
    public @Nullable MavenVersionRange parsedModpackVersionRange() {
        if (modpackVersionRange == null || modpackVersionRange.isBlank())
            return null;

        try {
            return MavenVersionRange.parse(modpackVersionRange);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /// Tests whether the given modpack version lies outside [#modpackVersionRange()].
    ///
    /// An absent, blank, malformed range or an unknown modpack version is never treated as out of
    /// range, because in these cases the launcher cannot decide whether the patch pack is
    /// compatible. Callers should only warn the user instead of refusing the installation.
    ///
    /// @param modpackVersion the version of the target modpack, or `null` if it is unknown
    /// @return `true` if the range is known and `modpackVersion` does not satisfy it
    public boolean isOutOfRange(@Nullable String modpackVersion) {
        if (modpackVersion == null)
            return false;

        MavenVersionRange range = parsedModpackVersionRange();
        return range != null && !range.contains(modpackVersion);
    }

    @Override
    public void validate() throws JsonParseException {
        if (formatVersion <= 0)
            throw new JsonParseException("patchpackinfo.json is missing a valid `formatVersion`");
        if (formatVersion > CURRENT_FORMAT_VERSION)
            throw new JsonParseException("Unsupported patch pack format version: " + formatVersion);

        if (name == null || name.isBlank())
            throw new JsonParseException("patchpackinfo.json is missing `name`");

        if (modpackVersionRange == null || modpackVersionRange.isBlank())
            throw new JsonParseException("patchpackinfo.json is missing `modpackVersionRange`");

        try {
            MavenVersionRange.parse(modpackVersionRange);
        } catch (IllegalArgumentException e) {
            throw new JsonParseException("patchpackinfo.json contains a malformed `modpackVersionRange`: " + modpackVersionRange, e);
        }

        if (diff != null)
            diff.validate();
    }

    /// The file operations applied to the target instance before the patch pack content is
    /// extracted.
    ///
    /// @param delete the paths to delete, or `null` if no path should be deleted
    /// @param rename the files to rename or move, or `null` if no file should be renamed
    @NotNullByDefault
    public record Diff(
            @SerializedName("delete") @Nullable @Unmodifiable List<String> delete,
            @SerializedName("rename") @Nullable @Unmodifiable List<Rename> rename
    ) implements Validation {

        @Override
        public void validate() throws JsonParseException {
            if (delete != null) {
                for (String path : delete) {
                    if (path == null || path.isBlank())
                        throw new JsonParseException("`diff.delete` contains an empty path");
                }
            }

            if (rename != null) {
                for (Rename entry : rename) {
                    if (entry == null)
                        throw new JsonParseException("`diff.rename` contains a null entry");
                    entry.validate();
                }
            }
        }

        /// A single rename or move operation.
        ///
        /// @param from the path of the file or directory to move
        /// @param to the destination path
        @NotNullByDefault
        public record Rename(
                @SerializedName("from") @NotNull String from,
                @SerializedName("to") @NotNull String to
        ) implements Validation {

            @Override
            public void validate() throws JsonParseException {
                if (from == null || from.isBlank())
                    throw new JsonParseException("`diff.rename` contains an empty `from` path");
                if (to == null || to.isBlank())
                    throw new JsonParseException("`diff.rename` contains an empty `to` path");
            }
        }
    }
}
