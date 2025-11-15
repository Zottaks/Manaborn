package com.manaborn.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Режим работы меню заклинаний
 */
public class SpellMenuMode {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("manaborn-spell-mode.json");
    
    private static SpellMenuMode INSTANCE;
    
    // true = нажатие (press), false = удержание (hold)
    public boolean isPressMode = true;
    
    public static SpellMenuMode getInstance() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }
    
    private static SpellMenuMode load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                return GSON.fromJson(json, SpellMenuMode.class);
            } catch (IOException e) {
                System.err.println("Failed to load spell mode config: " + e.getMessage());
            }
        }
        
        SpellMenuMode config = new SpellMenuMode();
        config.save();
        return config;
    }
    
    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            String json = GSON.toJson(this);
            Files.writeString(CONFIG_PATH, json);
        } catch (IOException e) {
            System.err.println("Failed to save spell mode config: " + e.getMessage());
        }
    }
    
    public void toggle() {
        isPressMode = !isPressMode;
        save();
    }
    
    public String getModeName() {
        return isPressMode ? "Press" : "Hold";
    }
}
