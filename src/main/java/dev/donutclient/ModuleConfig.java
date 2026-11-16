package dev.donutclient;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ModuleConfig {

    private static final Properties VALUES = new Properties();
    private static final Path FILE = FabricLoader.getInstance()
            .getConfigDir().resolve("donutclient.properties");

    static {
        load();
    }

    private ModuleConfig() {
    }

    private static synchronized void load() {
        if (!Files.exists(FILE)) return;

        try (Reader reader = Files.newBufferedReader(FILE)) {
            VALUES.load(reader);
        } catch (IOException exception) {
            System.err.println("[DONUT CLIENT] Could not load config: " + exception.getMessage());
        }
    }

    private static synchronized void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                VALUES.store(writer, "DONUT CLIENT settings");
            }
        } catch (IOException exception) {
            System.err.println("[DONUT CLIENT] Could not save config: " + exception.getMessage());
        }
    }

    private static String key(String name, String setting) {
        return name.toLowerCase().replaceAll("[^a-z0-9]+", "_") + "." + setting;
    }

    public static synchronized boolean isEnabled(String name) {
        return Boolean.parseBoolean(VALUES.getProperty(key(name, "enabled"), "false"));
    }

    public static synchronized boolean isFavorite(String name) {
        return Boolean.parseBoolean(VALUES.getProperty(key(name, "favorite"), "false"));
    }

    public static synchronized void setEnabled(String name, boolean enabled) {
        VALUES.setProperty(key(name, "enabled"), Boolean.toString(enabled));
        save();
    }

    public static synchronized void setFavorite(String name, boolean favorite) {
        VALUES.setProperty(key(name, "favorite"), Boolean.toString(favorite));
        save();
    }
}
