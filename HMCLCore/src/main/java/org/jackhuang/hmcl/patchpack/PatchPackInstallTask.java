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

import org.jackhuang.hmcl.task.Task;
import org.jackhuang.hmcl.util.io.FileUtils;
import org.jackhuang.hmcl.util.io.Unzipper;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/// Installs a patch pack by applying its file operations and then extracting its content over the
/// run directory of an existing instance.
///
/// The file operations declared by [PatchPackInfo#diff()] are executed before the archive content is
/// extracted, so that a patch pack can remove or move the files it replaces. Files that are already
/// present in the target directory and are not part of the patch pack are left untouched.
@NotNullByDefault
public final class PatchPackInstallTask extends Task<Void> {

    /// The prefix of the archive entries that are never extracted.
    private static final String META_INF = "META-INF/";

    private final Path zipFile;
    private final Charset charset;
    private final PatchPackInfo info;
    private final Path destination;

    /// Creates an install task.
    ///
    /// @param zipFile     the patch pack archive
    /// @param charset     the charset used to decode the names of the archive entries
    /// @param info        the patch pack information read from `zipFile`
    /// @param destination the run directory of the target instance
    public PatchPackInstallTask(Path zipFile, Charset charset, PatchPackInfo info, Path destination) {
        this.zipFile = zipFile;
        this.charset = charset;
        this.info = info;
        this.destination = destination;
    }

    @Override
    public void execute() throws Exception {
        applyDiff();
        extract();
    }

    /// Applies the delete and rename operations declared by the patch pack.
    ///
    /// Deletions are executed before renames, and paths are deleted from the deepest to the
    /// shallowest one, so that deleting a directory cannot drop the destination of a rename.
    ///
    /// @throws IOException if a file operation fails
    private void applyDiff() throws IOException {
        PatchPackInfo.Diff diff = info.diff();
        if (diff == null)
            return;

        List<Path> deleteTargets = new ArrayList<>();
        if (diff.delete() != null) {
            for (String path : diff.delete()) {
                deleteTargets.add(resolveInside(path));
            }
        }

        // Delete directories after their children, otherwise deleting a directory would remove
        // files that are only listed later in `diff.delete`.
        deleteTargets.sort(Comparator.comparingInt(Path::getNameCount).reversed());
        for (Path path : deleteTargets) {
            if (Files.isDirectory(path)) {
                FileUtils.deleteDirectory(path);
            } else {
                Files.deleteIfExists(path);
            }
        }

        if (diff.rename() != null) {
            for (PatchPackInfo.Diff.Rename rename : diff.rename()) {
                Path from = resolveInside(rename.from());
                if (!Files.exists(from))
                    continue;

                Path to = resolveInside(rename.to());
                @Nullable Path parent = to.getParent();
                if (parent != null)
                    Files.createDirectories(parent);

                // Move the target away first, because moving a directory into an existing directory
                // would nest it instead of replacing it on most platforms.
                if (Files.isDirectory(to)) {
                    FileUtils.deleteDirectory(to);
                } else {
                    Files.deleteIfExists(to);
                }

                Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    /// Extracts the patch pack archive over the destination directory.
    ///
    /// @throws IOException if the archive is malformed or a file cannot be written
    private void extract() throws IOException {
        Files.createDirectories(destination);

        new Unzipper(zipFile, destination)
                .setReplaceExistentFile(true)
                .setEncoding(charset)
                .setFilter((entry, destFile, relativePath) -> {
                    // The information file is metadata of the patch pack, it must not pollute the
                    // instance directory. META-INF belongs to the archive itself, not to the
                    // instance, so its content is never extracted either.
                    return !PatchPackInfo.FILE_NAME.equals(relativePath)
                            && !relativePath.startsWith(META_INF);
                })
                .unzip();
    }

    /// Resolves a path declared by the patch pack against the destination directory.
    ///
    /// @param path the relative path declared in `patchpackinfo.json`
    /// @return the absolute normalized path
    /// @throws IOException if `path` is empty or points outside of the destination directory
    private Path resolveInside(String path) throws IOException {
        Path base = destination.toAbsolutePath().normalize();
        // The path is normalized after being resolved, because FileUtils.normalizePath would drop
        // the drive letter of an absolute Windows path such as "C:/instance".
        Path resolved = base.resolve(path).toAbsolutePath().normalize();
        // An empty path denotes the instance directory itself, which a patch pack must never
        // delete or move.
        if (resolved.equals(base) || !resolved.startsWith(base))
            throw new IOException("Patch pack is trying to access a path outside of the instance directory: " + path);
        return resolved;
    }
}
