package com.edam.modmanager;

import com.edam.modmanager.registry.ModRegistry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
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
    private final ModRegistry modRegistry;

    private int nRemoved = 0;
    private int nSkipped = 0;
    private int nCopied = 0;

    public Manager(final Path gameRoot, final Path modRoot, final Path backupRoot,
                   Path recordPath) throws IOException {
        this.gameRoot = gameRoot;
        this.backupRoot = backupRoot;
        this.modRoot = modRoot;
        this.modRegistry = new ModRegistry(recordPath, gameRoot);
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
        modRegistry.registerMod(mod);
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
        uninstallFiles(mod);
        // copyFilesRecursively(mod, Source.SOURCE_BACKUP);
        modRegistry.unregisterMod(mod);
        return generateOutput("Remove");
    }

    private void uninstallFiles(Path modToRemove) throws IOException {
        List<String> installedModNames = new ArrayList<>(modRegistry.getInstalledMods());
        Collections.reverse(installedModNames);
        uninstallFilesRecursively(modToRemove, modToRemove, installedModNames);
    }

    /**
     * Recursively processes the files of a mod being uninstalled. Preserves files overridden by
     * newer mods, restores files from older installed mods when available, and falls back to the
     * original game files or deletion when no other mod uses them.
     *
     * @param currentPath       the current file or directory being processed
     * @param modToRemove       the root directory of the mod being uninstalled
     * @param installedModNames the names of installed mods in reverse installation order, from
     *                          newest to oldest
     * @throws IOException if an I/O error occurs during file processing
     */
    private void uninstallFilesRecursively(Path currentPath, Path modToRemove,
                                           List<String> installedModNames) throws IOException {
        if (Files.isDirectory(currentPath)) {
            try (Stream<Path> stream = Files.list(currentPath)) {
                for (Path subfile : stream.toList()) {
                    uninstallFilesRecursively(subfile, modToRemove, installedModNames);
                }
            }
        } else {
            Path gameFile = mapModFileTo(currentPath, Source.SOURCE_GAME);
            boolean passedTargetMod = false;
            boolean fileResolved = false;
            Path relativePath = modToRemove.relativize(currentPath);
            for (String installedModName : installedModNames) {
                if (!installedModName.equals(modToRemove.getFileName().toString())) {
                    Path otherModDir = modToRemove.getParent().resolve(installedModName);
                    Path otherModFile = otherModDir.resolve(relativePath);
                    if (Files.isRegularFile(otherModFile)) {
                        fileResolved = true;
                        // Newer mods take priority; otherwise, restore the latest older mod's file.
                        if (passedTargetMod) {
                            copyFile(otherModFile, gameFile);
                        }
                        break;
                    }
                } else {
                    passedTargetMod = true;
                }
            }

            // No other mod uses this file: restore the original or delete it.
            if (!fileResolved) {
                Path backupFile = mapModFileTo(currentPath, Source.SOURCE_BACKUP);
                if (Files.isRegularFile(backupFile)) {
                    copyFile(backupFile, gameFile);
                } else {
                    deleteFile(gameFile);
                    deleteDirectoryIfEmpty(gameFile.getParent());
                }
            }
        }
    }

    private Path mapModFileTo(Path modFile, Source source) {
        Path relativePath = modRoot.relativize(modFile);

        return switch (source) {
            case SOURCE_MOD -> modFile;
            case SOURCE_GAME -> gameRoot.resolve(relativePath);
            case SOURCE_BACKUP -> backupRoot.resolve(relativePath);
        };
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