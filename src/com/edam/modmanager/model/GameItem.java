package com.edam.modmanager.model;

import java.nio.file.Path;

public class GameItem implements Item {
    private final int option;
    private final Path path;

    public GameItem(int option, Path gamePath) {
        this.option = option;
        this.path = gamePath;
    }

    public int getOption() {
        return option;
    }

    public Path getPath() {
        return path;
    }
}
