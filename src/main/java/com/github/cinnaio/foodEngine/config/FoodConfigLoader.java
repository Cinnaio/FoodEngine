package com.github.cinnaio.foodEngine.config;

import com.github.cinnaio.foodEngine.FoodEngine;
import com.github.cinnaio.foodEngine.manager.FoodRegistry;
import com.github.cinnaio.foodEngine.model.FoodCombo;
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
                List<ParsedAction> parsed = parser.parseList(actionsRaw);
                List<FoodCombo> combos = parseCombos(foodSection);
                if (!parsed.isEmpty() || (combos != null && !combos.isEmpty())) {
                    registry.register(id, parsed, combos);
                    totalFoods++;
                }
            }
        }

        if (plugin.isDebug()) {
            plugin.getLogger().info("Loaded " + totalFoods + " foods from " + files.length + " file(s).");
        }
    }

    @SuppressWarnings("unchecked")
    private List<FoodCombo> parseCombos(ConfigurationSection foodSection) {
        if (!foodSection.isList("combo")) {
            return List.of();
        }
        List<?> list = foodSection.getList("combo");
        if (list == null || list.isEmpty()) {
            return List.of();
        }

        java.util.ArrayList<FoodCombo> combos = new java.util.ArrayList<>();
        for (Object entry : list) {
            if (!(entry instanceof Map<?, ?> comboMap)) {
                continue;
            }

            Object conditionsObj = comboMap.get("conditions");
            if (!(conditionsObj instanceof Map<?, ?> condMap)) {
                continue;
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
            FoodCombo.ComboConditions conditions = new FoodCombo.ComboConditions(foodEngineId, material, maxInterval, trigger);

            long cooldown = getLong(comboMap, "cooldown", 0L);

            Object actionsObj = comboMap.get("actions");
            if (!(actionsObj instanceof List<?> actionsListRaw)) {
                continue;
            }

            java.util.ArrayList<String> strings = new java.util.ArrayList<>();
            for (Object a : actionsListRaw) {
                if (a instanceof String s) {
                    strings.add(s);
                }
            }
            List<ParsedAction> parsedActions = parser.parseList(strings);
            if (parsedActions.isEmpty()) {
                continue;
            }

            combos.add(new FoodCombo(conditions, cooldown, parsedActions));
        }
        return combos;
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

