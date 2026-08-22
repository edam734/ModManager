import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

/**
 * @author Eduardo
 */
public class Main {

    public static final String GAMES = "C:/Program Files (x86)/Steam/steamapps/common/";
    public static final String BACKUPS = GAMES.concat("__Backups");
    public static final String MODS = GAMES.concat("__Mods");

    private static final String RECORD_FILE = "Record [mod manager]";
    private static final String[] EXCEPTIONS = new String[]{"__Backups", "__Mods", "__ModManager", "manager.bat", RECORD_FILE, "Fluffy Mod Manager", "Steam Controller Configs", "Steamworks Shared", "z- RE5 Lista de ARCS (personagens).url"};

    public static void main(String[] args) throws IOException {
        Path record = createFile(RECORD_FILE);
        try (Scanner scanner = new Scanner(System.in)) {
            List<GameItem> gameItems = searchForGames(Path.of(GAMES));
            System.out.println("0- exit");
            printGameOptions(gameItems);
            System.out.println("Escolha um jogo:");
            int gameNumber = scanner.nextInt();
            exitIfChosen(gameNumber);
            Path gameChosen = getGame(gameItems, gameNumber);
            System.out.println("**Você escolheu o jogo: " + gameChosen.getFileName() + "**");
            Path modDir = Paths.get(MODS, gameChosen.getFileName().toString());
            List<ModItem> modItems = searchForMods(modDir);
            System.out.println("0- exit");
            printModOptions(modItems);
            printInstalledMods(modItems, record);
            System.out.println("Escolha o Mod:");
            int modNumber = scanner.nextInt();
            exitIfChosen(modNumber);
            Path modChosen = getMod(modItems, modNumber);
            System.out.println("**Você escolheu o mod: " + modChosen.getFileName() + "**");

            Path backupRoot = Paths.get(BACKUPS, gameChosen.getFileName().toString());
            Manager manager = new Manager(gameChosen, modChosen, backupRoot, record);
            System.out.println("Escolha a operação:");
            System.out.println("0- exit");
            System.out.println("1- Adicionar o mod");
            System.out.println("2- Remover o mod");
            int operationChosen = scanner.nextInt();
            exitIfChosen(operationChosen);
            System.out.println("Base directory: " + GAMES);
            Output output;
            if (operationChosen == 1) {
                output = manager.addMod(modChosen);
                System.out.println();
                System.out.println(output);
                System.out.printf("**O mod '%s' foi adicionado com sucesso**/n", modChosen.getFileName());
            } else if (operationChosen == 2) {
                output = manager.removeMod(modChosen);
                System.out.println();
                System.out.println(output);
                System.out.printf("**O mod '%s' foi removido com sucesso**/n", modChosen.getFileName());
            }
            printInstalledMods(modItems, record);
        }
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
