import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author Edam
 */
public class Manager {

    private final Path gameRoot;
    private final Path backupRoot;
    private final Path modRoot;
    private final Path recordPath;

    private int nRemoved = 0;
    private int nSkipped = 0;
    private int nCopied = 0;

    public Manager(final Path gameRoot, final Path modRoot, final Path backupRoot,
                   Path recordPath) {
        this.gameRoot = gameRoot;
        this.backupRoot = backupRoot;
        this.modRoot = modRoot;
        this.recordPath = recordPath;
    }

    /**
     * Operação de adicionar um Mod
     *
     * @param mod O Mod a adicionar
     */
    public Output installMod(Path mod) throws IOException {
        copyFilesRecursively(mod, Source.SOURCE_MOD);
        registerMod(mod);
        return generateOutput("Add");
    }

    private void registerMod(Path mod) throws IOException {
        // apenas regista se ainda não estiver registado
        if (!isModInstalled(mod)) {
            File record = new File(recordPath.toString());
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(record, true))) {
                writer.append(String.valueOf(mod.getFileName()));
                writer.newLine();
            }
        }
    }

    private boolean isModInstalled(Path mod) throws IOException {
        List<String> installedMods = Files.readAllLines(recordPath);
        return installedMods.stream().anyMatch(m -> m.equals(String.valueOf(mod.getFileName())));
    }

    /**
     * Operação de remover um Mod.
     * <p>
     * A operação de remover um Mod consiste em apagar os ficheiros de um Mod que já estão no jogo e
     * substituí-los pelos ficheiros originais.
     *
     * @param mod O Mod a remover
     */
    public Output uninstallMod(Path mod) throws IOException {
        deleteFilesRecursively(mod);
        copyFilesRecursively(mod, Source.SOURCE_BACKUP);
        unregisterMod(mod);
        return generateOutput("Remove");
    }

    private void unregisterMod(Path mod) throws IOException {
        List<String> lines = Files.readAllLines(recordPath);
        lines.removeIf(line -> line.equals(String.valueOf(mod.getFileName())));
        Files.write(recordPath, lines);
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
