import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * @author Eduardo
 */
public class Main {

    private static final Scanner SCANNER = new Scanner(System.in);
    private static final String[] EXCEPTIONS = new String[]{"__Backups", "__Mods", "__ModManager", "manager.bat", "Fluffy Mod Manager", "Steam Controller Configs", "Steamworks Shared", "z- RE5 Lista de ARCS (personagens).url"};

    public static String gamesDir;
    public static String backupsDir;
    public static String modsDir;

    public static void main(String[] args) throws IOException {
        Path appDir = Path.of("").toAbsolutePath();
        Path configFile = appDir.resolve("config.properties");
        Properties properties = getProperties(appDir, configFile);
        gamesDir = properties.getProperty("games.dir");
        backupsDir = properties.getProperty("backups.dir");
        modsDir = properties.getProperty("mods.dir");

        if (gamesDir == null || gamesDir.isBlank()) {
            gamesDir = askDirectory("Games directory: ");
            properties.setProperty("games.dir", gamesDir);
        }

        if (backupsDir == null || backupsDir.isBlank()) {
            backupsDir = askDirectory("Backups directory: ");
            properties.setProperty("backups.dir", backupsDir);
        }

        if (modsDir == null || modsDir.isBlank()) {
            modsDir = askDirectory("Mods directory: ");
            properties.setProperty("mods.dir", modsDir);
        }

        try (OutputStream output = Files.newOutputStream(configFile)) {
            properties.store(output, null);
        }

        Path record = appDir.resolve("installed-mods.txt");
        if (Files.notExists(record)) {
            Files.createFile(record);
        }
//        Path record = createFile(RECORD_FILE);
        try (Scanner scanner = new Scanner(System.in)) {
            List<GameItem> gameItems = searchForGames(Path.of(gamesDir));
            System.out.println("0- exit");
            printGameOptions(gameItems);
            System.out.println("Escolha um jogo:");
            int gameNumber = scanner.nextInt();
            exitIfChosen(gameNumber);
            Path gameChosen = getGame(gameItems, gameNumber);
            System.out.println("**Você escolheu o jogo: " + gameChosen.getFileName() + "**");
            Path modDir = Paths.get(modsDir, gameChosen.getFileName().toString());
            List<ModItem> modItems = searchForMods(modDir);
            System.out.println("0- exit");
            printModOptions(modItems);
            printInstalledMods(modItems, record);
            System.out.println("Escolha o Mod:");
            int modNumber = scanner.nextInt();
            exitIfChosen(modNumber);
            Path modChosen = getMod(modItems, modNumber);
            System.out.println("**Você escolheu o mod: " + modChosen.getFileName() + "**");

            Path backupRoot = Paths.get(backupsDir, gameChosen.getFileName().toString());
            Manager manager = new Manager(gameChosen, modChosen, backupRoot, record);
            System.out.println("Escolha a operação:");
            System.out.println("0- exit");
            System.out.println("1- Adicionar o mod");
            System.out.println("2- Remover o mod");
            int operationChosen = scanner.nextInt();
            exitIfChosen(operationChosen);
            System.out.println("Base directory: " + gamesDir);
            Output output;
            if (operationChosen == 1) {
                output = manager.addMod(modChosen);
                System.out.println();
                System.out.println(output);
                System.out.printf("**O mod '%s' foi adicionado com sucesso**%n", modChosen.getFileName());
            } else if (operationChosen == 2) {
                output = manager.removeMod(modChosen);
                System.out.println();
                System.out.println(output);
                System.out.printf("**O mod '%s' foi removido com sucesso**%n", modChosen.getFileName());
            }
            printInstalledMods(modItems, record);
        }
        SCANNER.close();
    }

    private static String askDirectory(String message) {
        while (true) {
            System.out.println(message);
            String input = SCANNER.nextLine().trim();

            Path path = Path.of(input);
            if (Files.isDirectory(path)) {
                return input;
            }
            System.out.println("Directory does not exist. Please try again.");
        }
    }

    private static Properties getProperties(Path appDir, Path configFile) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(configFile)) {
            properties.load(input);
        } catch (NoSuchFileException e) {
            throw new IOException("Configuration file not found: " + configFile, e);
        }
        return properties;
    }

    private static void printInstalledMods(List<ModItem> modItems, Path record) throws IOException {
        List<String> installedMods = Files.readAllLines(record);
        System.out.println("******* Mods Instalados *******");
        System.out.println("*                             *");
        installedMods.forEach(e -> {
            int pos = getModPosition(modItems, e);
            System.out.printf("%s [%d] \n", e, pos);
        });
        System.out.println("*                             *");
        System.out.println("*******************************");
    }

    private static int getModPosition(List<ModItem> list, String im) {
        return list.stream()
                .filter(modItem -> im.equals(modItem.getName().getFileName().toString()))
                .findFirst().orElseGet(ModItem::new).getOption();
    }

    private static Path createFile(String filename) throws IOException {
        try {
            return Files.createFile(Path.of(filename));
        } catch (FileAlreadyExistsException e) {
            return Path.of(filename);
        }
    }

    private static void exitIfChosen(int chosen) {
        if (0 == chosen) {
            System.exit(0);
        }
    }

    private static Path getGame(List<GameItem> gameItems, int gameNumber) {
        Optional<GameItem> gameChosenOpt = gameItems.stream()
                .filter(mod -> mod.getOption() == gameNumber).findFirst();
        GameItem gameItem = gameChosenOpt.orElseThrow(() -> new NoSuchElementException(
                "A opção " + gameNumber + " é inválida."));
        String nameGame = gameItem.getName().toString();
        return Path.of(nameGame);
    }

    private static void printGameOptions(List<GameItem> gameItems) {
        gameItems.forEach(item -> {
            System.out.println(item.getOption() + "- " + item.getName().getFileName());
        });
    }

    private static List<GameItem> searchForGames(Path gamesDir) throws IOException {
        List<Path> games;

        try (Stream<Path> stream = Files.list(gamesDir)) {
            games = stream.toList();
        }
        List<GameItem> gameItems = new ArrayList<>();
        int op = 1;
        for (Path game : games) {
            if (!isException(game.getFileName().toString())) {
                gameItems.add(new GameItem(op++, game));
            }
        }
        return gameItems;
    }

    private static boolean isException(String elem) {
        return Arrays.stream(EXCEPTIONS).toList().contains(elem);
    }

    private static Path getMod(List<ModItem> modItems, final int modNumber) {
        Optional<ModItem> modChosenOpt = modItems.stream()
                .filter(mod -> mod.getOption() == modNumber).findFirst();
        ModItem modItem = modChosenOpt.orElseThrow(() -> new NoSuchElementException(
                "A opção " + modNumber + " é inválida."));
        String nameMode = modItem.getName().toString();
        return Path.of(nameMode);
    }

    private static void printModOptions(List<ModItem> modItems) {
        modItems.forEach(item -> System.out.println(item.getOption() + "- " + item.getName().getFileName()));
    }

    private static List<ModItem> searchForMods(Path modDir) throws IOException {
        List<Path> mods;

        try (Stream<Path> stream = Files.list(modDir)) {
            mods = stream.toList();
        }

        List<ModItem> modItems = new ArrayList<>();
        int op = 1;
        for (Path mod : mods) {
            modItems.add(new ModItem(op++, mod.toString()));
        }
        return modItems;
    }
}
