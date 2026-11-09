import java.nio.file.Path;

public class ModItem implements Item {
    private final int option;
    private final Path path;

    public ModItem(int option, Path modPath) {
        this.option = option;
        this.path = modPath;
    }

    public int getOption() {
        return option;
    }

    public Path getPath() {
        return path;
    }
}
