# Mod Manager

<br>

A lightweight Java application for installing and removing game mods based on file replacement.

The application maintains a backup of the game's base files, allowing modified files to be restored when a mod is removed. Files introduced exclusively by a mod are deleted during removal, while files that existed in the base installation are restored from the backup.

<br>

### Current Features

- Detect available games and mods
- Install mods by copying their file structure into the game directory
- Remove all files belonging to a mod
- Restore replaced files from a base backup
- Keep track of installed mods

<br>

## Roadmap

### v1.0.0
- Path handling refactored with `Path.relativize()` and `Path.resolve()`

### v2.0.0
- Mod conflict detection
- Tracking of file overrides between mods
- Mod priority/order management
- Safe restoration when multiple mods modify the same file
- Optional verbose logging to file

<br>
