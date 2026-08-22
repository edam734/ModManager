import java.nio.file.Path;

public class ModItem implements Item {
    private int option;
    private Path name;

    public ModItem() {
        this.option = -1;
        this.name = Path.of("");
    }

    public ModItem(int option, String nameMode) {
        this.option = option;
        this.name = Path.of(nameMode);
    }

    public int getOption() {
        return option;
    }

    public Path getName() {
        return name;
    }
}
