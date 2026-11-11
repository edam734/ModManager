package com.edam.modmanager.registry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModRegistryTest {

    @TempDir
    Path tempDir;

    Path recordPath;
    Path gameRoot;

    @BeforeEach
    void setUp() throws IOException {
        recordPath = tempDir.resolve("installed-mods.ini");
        gameRoot = tempDir.resolve("Resident Evil 5");

        Files.writeString(recordPath, """
                [Resident Evil 5]
                Mod A
                Mod B
                """);
    }

    @Test
    void testRegisterMod() throws IOException {
        // Initialize the registry for Resident Evil 5.
        Path gameRoot = tempDir.resolve("Resident Evil 5");
        ModRegistry registry = new ModRegistry(recordPath, gameRoot);

        // Register another mod.
        Path mod = tempDir.resolve("Mod C");
        registry.registerMod(mod);

        // Verify the registry contents.
        List<String> lines = Files.readAllLines(recordPath);

        assertEquals(List.of("[Resident Evil 5]", "Mod A", "Mod B", "Mod C"), lines);
    }

    @Test
    void testRegisterMultipleMods() throws IOException {
        Path gameRoot = tempDir.resolve("Resident Evil 5");
        ModRegistry registry = new ModRegistry(recordPath, gameRoot);

        Path mod1 = tempDir.resolve("Mod C");
        Path mod2 = tempDir.resolve("Mod D");
        Path mod3 = tempDir.resolve("Mod E");

        registry.registerMod(mod1);
        registry.registerMod(mod2);
        registry.registerMod(mod3);

        List<String> lines = Files.readAllLines(recordPath);

        assertEquals(List.of("[Resident Evil 5]", "Mod A", "Mod B", "Mod C", "Mod D", "Mod E"),
                lines);
    }

    @Test
    void testRegisterDuplicateMod() throws IOException {
        Path gameRoot = tempDir.resolve("Resident Evil 5");
        ModRegistry registry = new ModRegistry(recordPath, gameRoot);

        Path mod1 = tempDir.resolve("Mod C");
        Path mod2 = tempDir.resolve("Mod C");

        registry.registerMod(mod1);
        registry.registerMod(mod2);

        List<String> lines = Files.readAllLines(recordPath);

        assertEquals(List.of("[Resident Evil 5]", "Mod A", "Mod B", "Mod C"), lines);
    }

    @Test
    void testRegisterModForNewGame() throws IOException {
        Path gameRoot = tempDir.resolve("F.E.A.R.");
        ModRegistry registry = new ModRegistry(recordPath, gameRoot);

        // Register another mod.
        Path mod = tempDir.resolve("Mod A");
        registry.registerMod(mod);

        // Verify the registry contents.
        List<String> lines = Files.readAllLines(recordPath);

        assertEquals(List.of("[Resident Evil 5]", "Mod A", "Mod B", "", "[F.E.A.R.]", "Mod A"),
                lines);
    }

    @Test
    void testUnregisterMod() throws IOException {
        Path gameRoot = tempDir.resolve("Resident Evil 5");
        ModRegistry registry = new ModRegistry(recordPath, gameRoot);

        Path mod = tempDir.resolve("Mod A");
        registry.unregisterMod(mod);

        // Verify the registry contents.
        List<String> lines = Files.readAllLines(recordPath);

        assertEquals(List.of("[Resident Evil 5]", "Mod B"), lines);
    }

    @Test
    void testUnregisterNonexistentMod() throws IOException {
        Path gameRoot = tempDir.resolve("Resident Evil 5");
        ModRegistry registry = new ModRegistry(recordPath, gameRoot);

        Path mod = tempDir.resolve("Mod C");
        registry.unregisterMod(mod);

        // Verify the registry contents.
        List<String> lines = Files.readAllLines(recordPath);

        assertEquals(List.of("[Resident Evil 5]", "Mod A", "Mod B"), lines);
    }

    @Test
    void testMultipleGames() throws IOException {
        Files.writeString(recordPath, """
                [Resident Evil 5]
                Mod A
                Mod B
                
                [Resident Evil 4]
                Mod X
                """);

        ModRegistry registryRE5 = new ModRegistry(recordPath, gameRoot);
        registryRE5.registerMod(tempDir.resolve("Mod C"));

        ModRegistry registryRE4 = new ModRegistry(recordPath, tempDir.resolve("Resident Evil 4"));
        registryRE4.unregisterMod(tempDir.resolve("Mod X"));

        List<String> expected = List.of("[Resident Evil 5]", "Mod A", "Mod B", "Mod C", "",
                "[Resident Evil 4]");

        assertEquals(expected, Files.readAllLines(recordPath));
    }

    @Test
    void testRegistryFileCreation() throws IOException {
        // Delete the file created by @BeforeEach to simulate the application's first run.
        Files.delete(recordPath);

        new ModRegistry(recordPath, gameRoot);

        assertTrue(Files.isRegularFile(recordPath));
        assertEquals(List.of(), Files.readAllLines(recordPath));
    }

    @Test
    void testRegistryPersistence() throws IOException {
        Path mod = tempDir.resolve("Mod C");

        ModRegistry registry1 = new ModRegistry(recordPath, gameRoot);
        registry1.registerMod(mod);

        // Create a new instance to verify that the registry was persisted.
        ModRegistry registry2 = new ModRegistry(recordPath, gameRoot);
        registry2.unregisterMod(mod);

        List<String> expected = List.of("[Resident Evil 5]", "Mod A", "Mod B");

        assertEquals(expected, Files.readAllLines(recordPath));
    }
}