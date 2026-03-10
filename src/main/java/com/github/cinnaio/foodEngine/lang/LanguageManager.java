package com.github.cinnaio.foodEngine.lang;

import com.github.cinnaio.foodEngine.FoodEngine;
import com.github.cinnaio.foodEngine.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LanguageManager {

    private final FoodEngine plugin;
    private final Map<String, FileConfiguration> bundles = new ConcurrentHashMap<>();
    private String defaultLangCode = "en_us";

    public LanguageManager(FoodEngine plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        this.defaultLangCode = normalize(plugin.getConfig().getString("language", "en_us"));
        bundles.clear();

        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            // noinspection ResultOfMethodCallIgnored
            langFolder.mkdirs();
        }

        // Always ensure defaults exist on disk
        plugin.saveResource("lang/en_us.yml", false);
        plugin.saveResource("lang/zh_cn.yml", false);

        // Preload default bundle
        bundle(defaultLangCode);
    }

    public String raw(String path, String langCode) {
        FileConfiguration cfg = bundle(langCode);
        if (cfg == null) {
            return path;
        }
        return cfg.getString(path, path);
    }

    public Component component(String path, Player player) {
        String lang = defaultLangCode;
        if (player != null) {
            String locale = player.getLocale();
            if (locale != null && !locale.isBlank()) {
                lang = normalize(locale);
            }
        }

        String msg = raw(path, lang);
        if (msg.equals(path) && !lang.equals(defaultLangCode)) {
            msg = raw(path, defaultLangCode);
        }
        if (msg.equals(path) && !"en_us".equals(defaultLangCode)) {
            msg = raw(path, "en_us");
        }
        return TextUtil.toComponent(msg, player);
    }

    private FileConfiguration bundle(String langCode) {
        String code = normalize(langCode);
        return bundles.computeIfAbsent(code, c -> {
            File langFolder = new File(plugin.getDataFolder(), "lang");
            File langFile = new File(langFolder, c + ".yml");
            if (!langFile.exists()) {
                try {
                    plugin.saveResource("lang/" + c + ".yml", false);
                } catch (IllegalArgumentException ignored) {
                    // Not shipped in jar; may be provided by admin
                }
            }

            FileConfiguration config = YamlConfiguration.loadConfiguration(langFile);

            InputStream defStream = plugin.getResource("lang/" + c + ".yml");
            if (defStream != null) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defStream, StandardCharsets.UTF_8));
                config.setDefaults(defConfig);
            }

            try {
                config.save(langFile);
            } catch (IOException e) {
                plugin.getLogger().warning("Could not save language file " + langFile.getName());
            }
            return config;
        });
    }

    private String normalize(String code) {
        if (code == null) {
            return "en_us";
        }
        return code.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }
}

