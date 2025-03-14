package com.ubivismedia.dungeonlobby.localization;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import com.ubivismedia.dungeonlobby.DungeonLobby;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {
    private final DungeonLobby plugin;
    private final File langFolder;
    private final Map<String, YamlConfiguration> loadedLanguages = new HashMap<>();
    private final YamlConfiguration defaultLanguage;

    public LanguageManager(DungeonLobby plugin) {
        this.plugin = plugin;
        this.langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        // Load default English messages
        defaultLanguage = loadLanguageFile("en");

        // Load all available languages
        loadAllLanguages();
    }

    private void loadAllLanguages() {
        for (File file : langFolder.listFiles()) {
            if (file.getName().endsWith(".yml")) {
                String langCode = file.getName().replace("messages_", "").replace(".yml", "");
                loadedLanguages.put(langCode, YamlConfiguration.loadConfiguration(file));
            }
        }
    }

    private YamlConfiguration loadLanguageFile(String langCode) {
        File file = new File(langFolder, "messages_" + langCode + ".yml");
        if (file.exists()) {
            return YamlConfiguration.loadConfiguration(file);
        }

        // Fallback to internal resource
        InputStream resource = plugin.getResource("lang/messages_" + langCode + ".yml");
        if (resource != null) {
            return YamlConfiguration.loadConfiguration(new InputStreamReader(resource, StandardCharsets.UTF_8));
        }

        return new YamlConfiguration(); // Empty config if nothing found
    }

    public String getMessage(Player player, String key) {
        String locale = player.getLocale().split("_")[0]; // Extracts "en" from "en_US"
        YamlConfiguration langConfig = loadedLanguages.getOrDefault(locale, defaultLanguage);

        String message = langConfig.getString(key, defaultLanguage.getString(key, "&cMessage not found: " + key));
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public void sendMessage(Player player, String key) {
        player.sendMessage(getMessage(player, key));
    }
}
