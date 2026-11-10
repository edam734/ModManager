package com.edam.modmanager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;


/**
 * Manages the installation and uninstallation of game mods. Handles file copying, deletion, backup
 * restoration, and the registry of installed mods.
 *
 * @author Edam
 */
public class Manager {

    private final Path gameRoot;
    private final Path backupRoot;
    private final Path modRoot;
    private final Path recordPath;
    private final String gameSectionHeader;
    private final List<String> installedMods;

    private int nRemoved = 0;
    private int nSkipped = 0;
    private int nCopied = 0;

    public Manager(final Path gameRoot, final Path modRoot, final Path backupRoot,
                   Path recordPath) throws IOException {
        this.gameRoot = gameRoot;
        this.backupRoot = backupRoot;
        this.modRoot = modRoot;
        this.recordPath = recordPath;
        this.gameSectionHeader = "[" + gameRoot.getFileName() + "]";
        this.installedMods = getInstalledMods();
    }

    /**
     * Installs a mod by copying its files into the game directory and registering it as installed.
     *
     * @param mod the path to the mod directory
     * @return an {@link Output} containing the installation results
     * @throws IOException if an I/O error occurs during installation
     */
    public Output installMod(Path mod) throws IOException {
        copyFilesRecursively(mod, Source.SOURCE_MOD);
        registerMod(mod);
        return generateOutput("Add");
    }

    /**
     * Uninstalls a mod by removing its files from the game directory, restoring the original files
     * from the backup when available, and removing the mod from the installed mods registry.
     *
     * @param mod the path to the mod directory
     * @return an {@link Output} containing the uninstallation results
     * @throws IOException if an I/O error occurs during uninstallation
     */
    public Output uninstallMod(Path mod) throws IOException {
        deleteFilesRecursively(mod);
        copyFilesRecursively(mod, Source.SOURCE_BACKUP);
        unregisterMod(mod);
        return generateOutput("Remove");
    }

    private List<String> getInstalledMods() throws IOException {
        List<String> lines = Files.readAllLines(recordPath);
        List<String> installedMods = new ArrayList<>();

        boolean iniSection = false;

        for (String elem : lines) {
            if (iniSection) {
                if (elem.isBlank() || (elem.startsWith("[") && elem.endsWith("]"))) {
                    break;
                } else {
                    installedMods.add(elem);
                }
            }

            if (elem.equalsIgnoreCase(gameSectionHeader)) {
                iniSection = true;
            }
        }

        return installedMods;
    }

    private void registerMod(Path mod) throws IOException {
        // Register the mod only if it is not already registered.
        if (!isModInstalled(mod)) {
            List<String> lines = Files.readAllLines(recordPath);
            List<String> newContent = new ArrayList<>();
            boolean iniSection = false;
            boolean added = false;
            for (String line : lines) {
                if (iniSection) {
                    if (line.isBlank() || (line.startsWith("[") && line.endsWith("]"))) {
                        newContent.add(mod.getFileName().toString());
                        iniSection = false;
                        added = true;
                    }
                }
                if (line.equalsIgnoreCase(gameSectionHeader)) {
                    iniSection = true;
                }
                newContent.add(line);
            }
            // Handle cases where the game's section is at the end of the file or does not exist.
            if (!added) {
                if (iniSection) {
                    newContent.add(mod.getFileName().toString());
                } else {
                    if (!newContent.isEmpty() && !newContent.get(newContent.size() - 1).isBlank()) {
                        newContent.add("");
                    }
                    newContent.add(gameSectionHeader);
                    newContent.add(mod.getFileName().toString());
                }
            }
            Files.write(recordPath, newContent);
        }
    }

    private boolean isModInstalled(Path mod) {
        return installedMods.stream().anyMatch(m -> m.equals(String.valueOf(mod.getFileName())));
    }

    private void unregisterMod(Path mod) throws IOException {
        // Unregister the mod only if it is registered.
        if (isModInstalled(mod)) {
            List<String> lines = Files.readAllLines(recordPath);
            List<String> newContent = new ArrayList<>();
            boolean iniSection = false;

            for (String line : lines) {
                if (iniSection) {
                    if (line.isBlank() || (line.startsWith("[") && line.endsWith("]"))) {
                        iniSection = false;
                    }
                }

                if (line.equalsIgnoreCase(gameSectionHeader)) {
                    iniSection = true;
                }

                // Skip the mod being unregistered.
                if (iniSection && line.equals(mod.getFileName().toString())) {
                    continue;
                }

                newContent.add(line);
            }

            Files.write(recordPath, newContent);
            installedMods.remove(mod.getFileName().toString());
        }
    }

    private void deleteFilesRecursively(Path modFile) throws IOException {
        if (Files.isDirectory(modFile)) {
            try (Stream<Path> stream = Files.list(modFile)) {
                for (Path subfile : stream.toList()) {
                    deleteFilesRecursively(subfile);
                }
            }
        } else {
            Path gameFile = mapModFileTo(modFile, Source.SOURCE_GAME);
            if (null != gameFile) {
                deleteFile(gameFile);
                deleteDirectoryIfEmpty(gameFile.getParent());
            }
        }
    }

    private Path mapModFileTo(Path modFile, Source other) {
        if (Source.SOURCE_MOD.equals(other)) {
            return modFile;
        }

        Path relativePath = modRoot.relativize(modFile);
        if (Source.SOURCE_BACKUP.equals(other)) {
            return backupRoot.resolve(relativePath);
        } else if (Source.SOURCE_GAME.equals(other)) {
            return gameRoot.resolve(relativePath);
        }
        return null;
    }

    private void deleteDirectoryIfEmpty(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (Stream<Path> stream = Files.list(dir)) {
                if (stream.findAny().isEmpty()) {
                    deleteFile(dir);
                }
            }
        }
    }

    private void deleteFile(Path gameFile) throws IOException {
        boolean deleted = Files.deleteIfExists(gameFile);
        if (deleted) {
            nRemoved++;
        } else {
            nSkipped++;
        }
        System.out.printf("%s - %s%n", gameRoot.relativize(gameFile),
                deleted ? "removed" : "NOT removed or doesn't exist");
    }

    private void copyFilesRecursively(Path modFile, Source source) throws IOException {
        if (Files.isDirectory(modFile)) {
            try (Stream<Path> stream = Files.list(modFile)) {
                for (Path subfile : stream.toList()) {
                    copyFilesRecursively(subfile, source);
                }
            }
        } else {
            Path sourceFile = mapModFileTo(modFile, source); // source
            Path gameFile = mapModFileTo(modFile, Source.SOURCE_GAME); // destination
            if (gameFile != null && sourceFile != null && Files.exists(sourceFile)) {
                copyFile(sourceFile, gameFile);
            }
        }
    }

    private void copyFile(Path sourceFile, Path gameFile) throws IOException {
        Files.createDirectories(gameFile.getParent());
        Files.copy(sourceFile, gameFile, StandardCopyOption.REPLACE_EXISTING);

        System.out.printf("%s >> %s%n", sourceFile, gameRoot.relativize(gameFile));

        nCopied++;
    }

    private Output generateOutput(String operation) {
        return new Output(operation, nRemoved, nSkipped, nCopied);
    }
}
