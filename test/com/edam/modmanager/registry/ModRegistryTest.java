package com.edam.modmanager.registry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModRegistryTest {

    @TempDir
    Path tempDir;

    @Test
    void testRegisterMod() throws IOException {

        // Create a temporary registry file.
        Path recordPath = tempDir.resolve("installed-mods.ini");
        Files.writeString(recordPath, "[Resident Evil 5]\nMod A\n");

        // Initialize the registry for Resident Evil 5.
        Path gameRoot = tempDir.resolve("Resident Evil 5");
        ModRegistry registry = new ModRegistry(recordPath, gameRoot);

        // Register another mod.
        Path mod = tempDir.resolve("Mod B");
        registry.registerMod(mod);

        // Verify the registry contents.
        List<String> lines = Files.readAllLines(recordPath);

        assertEquals(
                List.of("[Resident Evil 5]", "Mod A", "Mod B"),
                lines
        );
    }
}