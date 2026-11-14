package com.edam.modmanager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ManagerTest {

    @TempDir
    Path tempDir;

    private Path gameRoot;
    private Path backupRoot;
    private Path modsDir;
    private Path recordPath;

    private final Path relativePath = Path.of("nativePC_MT", "player.arc");

    @BeforeEach
    void setUp() throws IOException {
        gameRoot = Files.createDirectories(tempDir.resolve("Resident Evil 5"));
        backupRoot = Files.createDirectories(tempDir.resolve("Backups"));
        modsDir = Files.createDirectories(tempDir.resolve("Mods"));
        recordPath = tempDir.resolve("installed-mods.ini");

        // Create the original game file and its backup.
        Files.createDirectories(gameRoot.resolve(relativePath).getParent());
        Files.createDirectories(backupRoot.resolve(relativePath).getParent());

        Files.writeString(gameRoot.resolve(relativePath), "ORIGINAL");
        Files.writeString(backupRoot.resolve(relativePath), "ORIGINAL");
    }

    private Path createMod(String name, String content) throws IOException {
        Path modDir = Files.createDirectories(modsDir.resolve(name));
        Path modFile = modDir.resolve(relativePath);

        Files.createDirectories(modFile.getParent());
        Files.writeString(modFile, content);

        return modDir;
    }

    private void install(Path mod) throws IOException {
        new Manager(gameRoot, mod, backupRoot, recordPath).installMod(mod);
    }

    private void uninstall(Path mod) throws IOException {
        new Manager(gameRoot, mod, backupRoot, recordPath).uninstallMod(mod);
    }

    @Test
    void testUninstallOlderModPreservesNewerMod() throws IOException {
        Path modA = createMod("Mod A", "A");
        Path modB = createMod("Mod B", "B");
        Path modC = createMod("Mod C", "C");

        install(modA);
        install(modB);
        install(modC);

        uninstall(modB);

        assertEquals("C", Files.readString(gameRoot.resolve(relativePath)));
    }

    @Test
    void testUninstallNewestModRestoresPreviousMod() throws IOException {
        Path modA = createMod("Mod A", "A");
        Path modB = createMod("Mod B", "B");
        Path modC = createMod("Mod C", "C");

        install(modA);
        install(modB);
        install(modC);

        uninstall(modC);

        assertEquals("B", Files.readString(gameRoot.resolve(relativePath)));
    }

    @Test
    void testUninstallModsInDifferentOrder() throws IOException {
        Path modA = createMod("Mod A", "A");
        Path modB = createMod("Mod B", "B");
        Path modC = createMod("Mod C", "C");

        install(modA);
        install(modB);
        install(modC);

        uninstall(modB);
        uninstall(modC);

        assertEquals("A", Files.readString(gameRoot.resolve(relativePath)));
    }

    @Test
    void testUninstallLastModRestoresOriginal() throws IOException {
        Path modA = createMod("Mod A", "A");

        install(modA);
        uninstall(modA);

        assertEquals("ORIGINAL", Files.readString(gameRoot.resolve(relativePath)));
    }

    @Test
    void testUninstallModDeletesNewFiles() throws IOException {
        Path modA = Files.createDirectories(modsDir.resolve("Mod A"));
        Path newFile = Path.of("nativePC_MT", "new.arc");

        Files.createDirectories(modA.resolve(newFile).getParent());
        Files.writeString(modA.resolve(newFile), "NEW FILE");

        install(modA);

        assertTrue(Files.exists(gameRoot.resolve(newFile)));

        uninstall(modA);

        assertFalse(Files.exists(gameRoot.resolve(newFile)));
    }

    @Test
    void testUninstallNonConflictingModPreservesOtherFiles() throws IOException {
        Path modA = createMod("Mod A", "A");
        Path modB = Files.createDirectories(modsDir.resolve("Mod B"));

        Path weaponFile = Path.of("nativePC_MT", "weapon.arc");
        Path unrelatedFile = Path.of("nativePC_MT", "unrelated.arc");

        // Create original game files.
        Files.writeString(gameRoot.resolve(weaponFile), "ORIGINAL WEAPON");
        Files.writeString(backupRoot.resolve(weaponFile), "ORIGINAL WEAPON");
        Files.writeString(gameRoot.resolve(unrelatedFile), "UNCHANGED");

        // Mod B modifies a different file from Mod A.
        Files.createDirectories(modB.resolve(weaponFile).getParent());
        Files.writeString(modB.resolve(weaponFile), "B");

        install(modA);
        install(modB);

        uninstall(modB);

        assertEquals("A", Files.readString(gameRoot.resolve(relativePath)));
        assertEquals("ORIGINAL WEAPON", Files.readString(gameRoot.resolve(weaponFile)));
        assertEquals("UNCHANGED", Files.readString(gameRoot.resolve(unrelatedFile)));
    }

    @Test
    void testUninstallSharedFileWithoutOriginalBackup() throws IOException {
        Path newFile = Path.of("nativePC_MT", "new.arc");

        Path modA = Files.createDirectories(modsDir.resolve("Mod A"));
        Path modFileA = modA.resolve(newFile);
        Files.createDirectories(modFileA.getParent());
        Files.writeString(modFileA, "New Content A");

        Path modB = Files.createDirectories(modsDir.resolve("Mod B"));
        Path modFileB = modB.resolve(newFile);
        Files.createDirectories(modFileB.getParent());
        Files.writeString(modFileB, "New Content B");

        install(modA);
        install(modB);

        uninstall(modB);
        assertEquals("New Content A", Files.readString(gameRoot.resolve(newFile)));
        uninstall(modA);

        assertFalse(Files.exists(gameRoot.resolve(newFile)));
    }
}