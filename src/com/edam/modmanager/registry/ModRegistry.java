package com.edam.modmanager.registry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the registry of installed mods for a specific game. Handles loading, registering, and
 * unregistering mods while preserving their installation order.
 *
 * @author Edam
 */
public class ModRegistry {

    private final Path recordPath;
    private final String gameSectionHeader;
    private final List<String> installedMods;


    public ModRegistry(Path recordPath, Path gameRoot) throws IOException {
        this.recordPath = recordPath;
        this.gameSectionHeader = "[" + gameRoot.getFileName() + "]";
        if (Files.notExists(recordPath)) {
            Files.createFile(recordPath);
        }
        this.installedMods = getInstalledMods();
    }

    /**
     * Registers a mod in the game's section of the installed mods registry, preserving the
     * installation order. Does nothing if the mod is already registered.
     *
     * @param mod the path to the mod directory
     * @throws IOException if an I/O error occurs while updating the registry
     */
    public void registerMod(Path mod) throws IOException {
        // Register the mod only if it is not already registered.
        if (!isModInstalled(mod)) {
            List<String> lines = Files.readAllLines(recordPath);
            List<String> updatedLines = new ArrayList<>();
            boolean iniSection = false;
            boolean added = false;
            for (String line : lines) {
                if (iniSection) {
                    if (line.isBlank() || (line.startsWith("[") && line.endsWith("]"))) {
                        updatedLines.add(mod.getFileName().toString());
                        iniSection = false;
                        added = true;
                    }
                }
                if (line.equalsIgnoreCase(gameSectionHeader)) {
                    iniSection = true;
                }
                updatedLines.add(line);
            }
            // Handle cases where the game's section is at the end of the file or does not exist.
            if (!added) {
                if (iniSection) {
                    updatedLines.add(mod.getFileName().toString());
                } else {
                    if (!updatedLines.isEmpty() && !updatedLines.get(updatedLines.size() - 1).isBlank()) {
                        updatedLines.add("");
                    }
                    updatedLines.add(gameSectionHeader);
                    updatedLines.add(mod.getFileName().toString());
                }
            }
            Files.write(recordPath, updatedLines);
            installedMods.add(mod.getFileName().toString());
        }
    }

    /**
     * Removes a mod from the game's section of the installed mods registry, preserving the
     * remaining mods and their installation order. Does nothing if the mod is not registered.
     *
     * @param mod the path to the mod directory
     * @throws IOException if an I/O error occurs while updating the registry
     */
    public void unregisterMod(Path mod) throws IOException {
        // Unregister the mod only if it is registered.
        if (isModInstalled(mod)) {
            List<String> lines = Files.readAllLines(recordPath);
            List<String> updatedLines = new ArrayList<>();
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

                updatedLines.add(line);
            }

            Files.write(recordPath, updatedLines);
            installedMods.remove(mod.getFileName().toString());
        }
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

    private boolean isModInstalled(Path mod) {
        return installedMods.stream().anyMatch(m -> m.equals(String.valueOf(mod.getFileName())));
    }
}