package com.github.cinnaio.foodEngine.config;

import com.github.cinnaio.foodEngine.FoodEngine;
import com.github.cinnaio.foodEngine.manager.FoodRegistry;
import com.github.cinnaio.foodEngine.model.FoodConditions;
import com.github.cinnaio.foodEngine.model.ParsedAction;
import com.github.cinnaio.foodEngine.parser.ActionParser;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;
import java.util.Map;

public class FoodConfigLoader {

    private final FoodEngine plugin;
    private final ActionParser parser;
    private final FoodRegistry registry;

    public FoodConfigLoader(FoodEngine plugin, ActionParser parser, FoodRegistry registry) {
        this.plugin = plugin;
        this.parser = parser;
        this.registry = registry;
    }

    public void loadAll() {
        File folder = new File(plugin.getDataFolder(), "foods");
        if (!folder.exists()) {
            // noinspection ResultOfMethodCallIgnored
            folder.mkdirs();
            if (plugin.getConfig().getBoolean("auto_load_example_foods", true)) {
                plugin.saveResource("foods/example_foods.yml", false);
            }
        }

        registry.clear();

        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".yml"));
        if (files == null) {
            return;
        }

        int totalFoods = 0;
        for (File file : files) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection foodsSection = config.getConfigurationSection("foods");
            if (foodsSection == null) {
                continue;
            }
            for (String id : foodsSection.getKeys(false)) {
                ConfigurationSection foodSection = foodsSection.getConfigurationSection(id);
                if (foodSection == null) {
                    continue;
                }
                List<String> actionsRaw = foodSection.getStringList("actions");
                List<ParsedAction> actions = parser.parseList(actionsRaw);

                // parse overuse_actions if present
                List<String> overuseRaw = foodSection.getStringList("overuse_actions");
                List<ParsedAction> overuseActions = parser.parseList(overuseRaw);

                // parse single conditions object (optional)
                FoodConditions conditions = parseConditions(foodSection.getConfigurationSection("conditions"));

                if (!actions.isEmpty() || conditions != null || !overuseActions.isEmpty()) {
                    registry.register(id, actions, conditions, overuseActions);
                    totalFoods++;
                }
            }
        }

        if (plugin.isDebug()) {
            plugin.getLogger().info("Loaded " + totalFoods + " foods from " + files.length + " file(s).");
        }
    }

    private FoodConditions parseConditions(ConfigurationSection condSection) {
        if (condSection == null) {
            return null;
        }
        Map<String, Object> condMap = condSection.getValues(false);
        if (condMap == null || condMap.isEmpty()) {
            return null;
        }

        String foodEngineId = getString(condMap, "food_engine_id");
        String materialName = getString(condMap, "material");
        Material material = null;
        if (materialName != null && !materialName.isBlank()) {
            try {
                material = Material.valueOf(materialName.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                material = null;
            }
        }

        long maxInterval = getLong(condMap, "max_interval", 0L);
        int trigger = (int) getLong(condMap, "trigger", 1L);
        Integer overuse = null;
        long overuseLong = getLong(condMap, "overuse_trigger", -1L);
        if (overuseLong >= 0L) {
            overuse = (int) overuseLong;
        }

        if (overuse != null && trigger >= overuse) {
            plugin.getLogger().warning("Invalid conditions: trigger >= overuse_trigger for food. Conditions disabled.");
            return null;
        }

        if ((foodEngineId == null || foodEngineId.isBlank()) && material == null) {
            return null;
        }

        return new FoodConditions(foodEngineId, material, maxInterval, trigger, overuse);
    }

    private String getString(Map<?, ?> map, String key) {
        Object v = map.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private long getLong(Map<?, ?> map, String key, long def) {
        Object v = map.get(key);
        if (v == null) {
            return def;
        }
        if (v instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(v).trim());
        } catch (NumberFormatException ex) {
            return def;
        }
    }
}
