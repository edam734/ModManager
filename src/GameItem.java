import java.nio.file.Path;

public class GameItem implements Item {
    private int option;
    private Path name;

    public GameItem(int option, Path gameName) {
        this.option = option;
        this.name = gameName;
    }

    public int getOption() {
        return option;
    }

    public Path getName() {
        return name;
    }
}
