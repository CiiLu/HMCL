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
package org.jackhuang.hmcl.util.io;

import org.glavo.chardet.DetectedCharset;
import org.glavo.chardet.UniversalDetector;
import org.jackhuang.hmcl.util.Lang;
import org.jackhuang.hmcl.util.StringUtils;
import org.jackhuang.hmcl.util.function.ExceptionalConsumer;
import org.jackhuang.hmcl.util.platform.OperatingSystem;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Predicate;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.jackhuang.hmcl.util.logging.Logger.LOG;

/**
 * @author huang
 */
public final class FileUtils {

    private FileUtils() {
    }

    public static boolean canCreateDirectory(String path) {
        try {
            return canCreateDirectory(Paths.get(path));
        } catch (InvalidPathException e) {
            return false;
        }
    }

    public static boolean canCreateDirectory(Path path) {
        if (Files.isDirectory(path)) return true;
        else if (Files.exists(path)) return false;
        else {
            Path lastPath = path; // always not exist
            path = path.getParent();
            // find existent ancestor
            while (path != null && !Files.exists(path)) {
                lastPath = path;
                path = path.getParent();
            }
            if (path == null) return false; // all ancestors are nonexistent
            if (!Files.isDirectory(path)) return false; // ancestor is file
            try {
                Files.createDirectory(lastPath); // check permission
                Files.delete(lastPath); // safely delete empty directory
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }

    public static String getNameWithoutExtension(String fileName) {
        return StringUtils.substringBeforeLast(fileName, '.');
    }

    public static String getNameWithoutExtension(File file) {
        return StringUtils.substringBeforeLast(file.getName(), '.');
    }

    public static String getNameWithoutExtension(Path file) {
        return StringUtils.substringBeforeLast(getName(file), '.');
    }

    public static String getExtension(File file) {
        return StringUtils.substringAfterLast(file.getName(), '.');
    }

    public static String getExtension(Path file) {
        return StringUtils.substringAfterLast(getName(file), '.');
    }

    /**
     * This method is for normalizing ZipPath since Path.normalize of ZipFileSystem does not work properly.
     */
    public static String normalizePath(String path) {
        return StringUtils.addPrefix(StringUtils.removeSuffix(path, "/", "\\"), "/");
    }

    public static String getName(Path path) {
        if (path.getFileName() == null) return "";
        return StringUtils.removeSuffix(path.getFileName().toString(), "/", "\\");
    }

    public static String getName(Path path, String candidate) {
        if (path.getFileName() == null) return candidate;
        else return getName(path);
    }

    public static String readTextMaybeNativeEncoding(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);

        if (OperatingSystem.NATIVE_CHARSET == UTF_8)
            return new String(bytes, UTF_8);

        UniversalDetector detector = new UniversalDetector();
        detector.handleData(bytes);
        detector.dataEnd();

        DetectedCharset detectedCharset = detector.getDetectedCharset();
        if (detectedCharset != null && detectedCharset.isSupported()
                && (detectedCharset == DetectedCharset.UTF_8 || detectedCharset == DetectedCharset.US_ASCII))
            return new String(bytes, UTF_8);
        else
            return new String(bytes, OperatingSystem.NATIVE_CHARSET);
    }

    /**
     * Write plain text to file. Characters are encoded into bytes using UTF-8.
     * <p>
     * We don't care about platform difference of line separator. Because readText accept all possibilities of line separator.
     * It will create the file if it does not exist, or truncate the existing file to empty for rewriting.
     * All characters in text will be written into the file in binary format. Existing data will be erased.
     *
     * @param file the path to the file
     * @param text the text being written to file
     * @throws IOException if an I/O error occurs
     */
    public static void writeText(File file, String text) throws IOException {
        writeText(file.toPath(), text);
    }

    /**
     * Write plain text to file. Characters are encoded into bytes using UTF-8.
     * <p>
     * We don't care about platform difference of line separator. Because readText accept all possibilities of line separator.
     * It will create the file if it does not exist, or truncate the existing file to empty for rewriting.
     * All characters in text will be written into the file in binary format. Existing data will be erased.
     *
     * @param file the path to the file
     * @param text the text being written to file
     * @throws IOException if an I/O error occurs
     */
    public static void writeText(Path file, String text) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, text);
    }

    /**
     * Write byte array to file.
     * It will create the file if it does not exist, or truncate the existing file to empty for rewriting.
     * All bytes in byte array will be written into the file in binary format. Existing data will be erased.
     *
     * @param file the path to the file
     * @param data the data being written to file
     * @throws IOException if an I/O error occurs
     */
    public static void writeBytes(File file, byte[] data) throws IOException {
        writeBytes(file.toPath(), data);
    }

    /**
     * Write byte array to file.
     * It will create the file if it does not exist, or truncate the existing file to empty for rewriting.
     * All bytes in byte array will be written into the file in binary format. Existing data will be erased.
     *
     * @param file the path to the file
     * @param data the data being written to file
     * @throws IOException if an I/O error occurs
     */
    public static void writeBytes(Path file, byte[] data) throws IOException {
        Files.createDirectories(file.getParent());
        Files.write(file, data);
    }

    public static void deleteDirectory(File directory)
            throws IOException {
        if (!directory.exists())
            return;

        if (!isSymlink(directory))
            cleanDirectory(directory);

        if (!directory.delete()) {
            String message = "Unable to delete directory " + directory + ".";

            throw new IOException(message);
        }
    }

    public static boolean deleteDirectoryQuietly(File directory) {
        try {
            deleteDirectory(directory);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Copy directory.
     * Paths of all files relative to source directory will be the same as the ones relative to destination directory.
     *
     * @param src  the source directory.
     * @param dest the destination directory, which will be created if not existing.
     * @throws IOException if an I/O error occurs.
     */
    public static void copyDirectory(Path src, Path dest) throws IOException {
        copyDirectory(src, dest, path -> true);
    }

    public static void copyDirectory(Path src, Path dest, Predicate<String> filePredicate) throws IOException {
        Files.walkFileTree(src, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!filePredicate.test(src.relativize(file).toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                Path destFile = dest.resolve(src.relativize(file).toString());
                Files.copy(file, destFile, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (!filePredicate.test(src.relativize(dir).toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                Path destDir = dest.resolve(src.relativize(dir).toString());
                Files.createDirectories(destDir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static boolean hasKnownDesktop() {
        if (!OperatingSystem.CURRENT_OS.isLinuxOrBSD())
            return true;

        String desktops = System.getenv("XDG_CURRENT_DESKTOP");
        if (desktops == null) {
            desktops = System.getenv("XDG_SESSION_DESKTOP");
        }

        if (desktops == null) {
            return false;
        }
        for (String desktop : desktops.split(":")) {
            switch (desktop.toLowerCase(Locale.ROOT)) {
                case "gnome":
                case "xfce":
                case "kde":
                case "mate":
                case "deepin":
                case "x-cinnamon":
                    return true;
            }
        }

        return false;
    }

    /**
     * Move file to trash.
     *
     * @param file the file being moved to trash.
     * @return false if moveToTrash does not exist, or platform does not support Desktop.Action.MOVE_TO_TRASH
     */
    public static boolean moveToTrash(File file) {
        if (OperatingSystem.CURRENT_OS.isLinuxOrBSD() && hasKnownDesktop()) {
            if (!file.exists()) {
                return false;
            }

            String xdgData = System.getenv("XDG_DATA_HOME");

            Path trashDir;
            if (StringUtils.isNotBlank(xdgData)) {
                trashDir = Paths.get(xdgData, "Trash");
            } else {
                trashDir = Paths.get(System.getProperty("user.home"), ".local/share/Trash");
            }

            Path infoDir = trashDir.resolve("info");
            Path filesDir = trashDir.resolve("files");

            try {
                Files.createDirectories(infoDir);
                Files.createDirectories(filesDir);

                String name = file.getName();

                Path infoFile = infoDir.resolve(name + ".trashinfo");
                Path targetFile = filesDir.resolve(name);

                int n = 0;
                while (Files.exists(infoFile) || Files.exists(targetFile)) {
                    n++;
                    infoFile = infoDir.resolve(name + "." + n + ".trashinfo");
                    targetFile = filesDir.resolve(name + "." + n);
                }

                String time = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS));
                if (file.isDirectory()) {
                    FileUtils.copyDirectory(file.toPath(), targetFile);
                } else {
                    FileUtils.copyFile(file.toPath(), targetFile);
                }

                FileUtils.writeText(infoFile, "[Trash Info]\nPath=" + file.getAbsolutePath() + "\nDeletionDate=" + time + "\n");
                FileUtils.forceDelete(file);
            } catch (IOException e) {
                LOG.warning("Failed to move " + file + " to trash", e);
                return false;
            }

            return true;
        }

        try {
            return java.awt.Desktop.getDesktop().moveToTrash(file);
        } catch (Exception e) {
            return false;
        }
    }

    public static void cleanDirectory(File directory)
            throws IOException {
        if (!directory.exists()) {
            if (!makeDirectory(directory))
                throw new IOException("Failed to create directory: " + directory);
            return;
        }

        if (!directory.isDirectory()) {
            String message = directory + " is not a directory";
            throw new IllegalArgumentException(message);
        }

        File[] files = directory.listFiles();
        if (files == null)
            throw new IOException("Failed to list contents of " + directory);

        IOException exception = null;
        for (File file : files)
            try {
                forceDelete(file);
            } catch (IOException ioe) {
                exception = ioe;
            }

        if (null != exception)
            throw exception;
    }

    public static boolean cleanDirectoryQuietly(File directory) {
        try {
            cleanDirectory(directory);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static void forceDelete(File file)
            throws IOException {
        if (file.isDirectory()) {
            deleteDirectory(file);
        } else {
            boolean filePresent = file.exists();
            if (!file.delete()) {
                if (!filePresent)
                    throw new FileNotFoundException("File does not exist: " + file);
                throw new IOException("Unable to delete file: " + file);
            }
        }
    }

    public static boolean isSymlink(File file)
            throws IOException {
        Objects.requireNonNull(file, "File must not be null");
        if (File.separatorChar == '\\')
            return false;
        File fileInCanonicalDir;
        if (file.getParent() == null)
            fileInCanonicalDir = file;
        else {
            File canonicalDir = file.getParentFile().getCanonicalFile();
            fileInCanonicalDir = new File(canonicalDir, file.getName());
        }

        return !fileInCanonicalDir.getCanonicalFile().equals(fileInCanonicalDir.getAbsoluteFile());
    }

    public static void copyFile(File srcFile, File destFile)
            throws IOException {
        Objects.requireNonNull(srcFile, "Source must not be null");
        Objects.requireNonNull(destFile, "Destination must not be null");
        if (!srcFile.exists())
            throw new FileNotFoundException("Source '" + srcFile + "' does not exist");
        if (srcFile.isDirectory())
            throw new IOException("Source '" + srcFile + "' exists but is a directory");
        if (srcFile.getCanonicalPath().equals(destFile.getCanonicalPath()))
            throw new IOException("Source '" + srcFile + "' and destination '" + destFile + "' are the same");
        File parentFile = destFile.getParentFile();
        if (parentFile != null && !FileUtils.makeDirectory(parentFile))
            throw new IOException("Destination '" + parentFile + "' directory cannot be created");
        if (destFile.exists() && !destFile.canWrite())
            throw new IOException("Destination '" + destFile + "' exists but is read-only");

        Files.copy(srcFile.toPath(), destFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void copyFile(Path srcFile, Path destFile)
            throws IOException {
        Objects.requireNonNull(srcFile, "Source must not be null");
        Objects.requireNonNull(destFile, "Destination must not be null");
        if (!Files.exists(srcFile))
            throw new FileNotFoundException("Source '" + srcFile + "' does not exist");
        if (Files.isDirectory(srcFile))
            throw new IOException("Source '" + srcFile + "' exists but is a directory");
        Path parentFile = destFile.getParent();
        Files.createDirectories(parentFile);
        if (Files.exists(destFile) && !Files.isWritable(destFile))
            throw new IOException("Destination '" + destFile + "' exists but is read-only");

        Files.copy(srcFile, destFile, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void moveFile(File srcFile, File destFile) throws IOException {
        copyFile(srcFile, destFile);
        srcFile.delete();
    }

    public static boolean makeDirectory(File directory) {
        directory.mkdirs();
        return directory.isDirectory();
    }

    public static boolean makeFile(File file) {
        return makeDirectory(file.getAbsoluteFile().getParentFile()) && (file.exists() || Lang.test(file::createNewFile));
    }

    public static List<File> listFilesByExtension(File file, String extension) {
        List<File> result = new ArrayList<>();
        File[] files = file.listFiles();
        if (files != null)
            for (File it : files)
                if (extension.equals(getExtension(it)))
                    result.add(it);
        return result;
    }

    /**
     * Tests whether the file is convertible to [java.nio.file.Path] or not.
     *
     * @param file the file to be tested
     * @return true if the file is convertible to Path.
     */
    public static boolean isValidPath(File file) {
        try {
            file.toPath();
            return true;
        } catch (InvalidPathException ignored) {
            return false;
        }
    }

    public static Optional<Path> tryGetPath(String first, String... more) {
        if (first == null) return Optional.empty();
        try {
            return Optional.of(Paths.get(first, more));
        } catch (InvalidPathException e) {
            return Optional.empty();
        }
    }

    public static Path tmpSaveFile(Path file) {
        return file.toAbsolutePath().resolveSibling("." + file.getFileName().toString() + ".tmp");
    }

    public static void saveSafely(Path file, String content) throws IOException {
        Path tmpFile = tmpSaveFile(file);
        try (BufferedWriter writer = Files.newBufferedWriter(tmpFile, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
            writer.write(content);
        }

        try {
            if (Files.exists(file) && Files.getAttribute(file, "dos:hidden") == Boolean.TRUE) {
                Files.setAttribute(tmpFile, "dos:hidden", true);
            }
        } catch (Throwable ignored) {
        }

        Files.move(tmpFile, file, StandardCopyOption.REPLACE_EXISTING);
    }

    public static void saveSafely(Path file, ExceptionalConsumer<? super OutputStream, IOException> action) throws IOException {
        Path tmpFile = tmpSaveFile(file);

        try (OutputStream os = Files.newOutputStream(tmpFile, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
            action.accept(os);
        }

        try {
            if (Files.exists(file) && Files.getAttribute(file, "dos:hidden") == Boolean.TRUE) {
                Files.setAttribute(tmpFile, "dos:hidden", true);
            }
        } catch (Throwable ignored) {
        }

        Files.move(tmpFile, file, StandardCopyOption.REPLACE_EXISTING);
    }

    public static String printFileStructure(Path path, int maxDepth) throws IOException {
        return DirectoryStructurePrinter.list(path, maxDepth);
    }
}
