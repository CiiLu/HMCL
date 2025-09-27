package org.jackhuang.hmcl.resourcepack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public interface ResourcepackFile {
    String getDescription();

    String getName();

    Path getPath();

    Path getIcon();

    static ResourcepackFile parse(Path path) throws IOException {
        String fileName = path.getFileName().toString();
        if (Files.isRegularFile(path) && fileName.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            return new ResourcepackZipFile(path);
        } else if (Files.isDirectory(path) && Files.exists(path.resolve("pack.mcmeta"))) {
            return new ResourcepackFolder(path);
        }
        return null;
    }
}
