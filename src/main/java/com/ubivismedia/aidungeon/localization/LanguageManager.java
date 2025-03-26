package com.ubivismedia.aidungeon.localization;

import com.ubivismedia.aidungeon.AIDungeonGenerator;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {
    private final AIDungeonGenerator plugin;
    private YamlConfiguration languageConfig;
    private Map<String, String> messages = new HashMap<>();

    public LanguageManager(AIDungeonGenerator plugin) {
        this.plugin = plugin;
        loadLanguageFile();
    }

    private void loadLanguageFile() {
        // Get language from config or default to English
        String language = plugin.getConfig().getString("settings.language", "en");
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdirs();
        }

        File langFile = new File(langDir, language + ".yml");

        // Create default language file if not exists
        if (!langFile.exists()) {
            // Copy from resources if available
            plugin.saveResource("lang/" + language + ".yml", false);
        }

        languageConfig = YamlConfiguration.loadConfiguration(langFile);

        // Load all messages
        for (String key : languageConfig.getKeys(true)) {
            if (languageConfig.isString(key)) {
                messages.put(key, languageConfig.getString(key));
            }
        }
    }

    public String getMessage(String key) {
        return messages.getOrDefault(key, key);
    }

    public String getMessage(String key, Object... replacements) {
        String message = getMessage(key);
        for (int i = 0; i < replacements.length; i++) {
            message = message.replace("{" + i + "}", String.valueOf(replacements[i]));
        }
        return message;
    }

    public void reloadLanguage() {
        loadLanguageFile();
    }
}