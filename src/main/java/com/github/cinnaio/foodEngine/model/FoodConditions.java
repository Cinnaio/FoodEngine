package com.github.cinnaio.foodEngine.model;

import org.bukkit.Material;

public record FoodConditions(
        String foodEngineId,
        Material material,
        long maxIntervalSeconds,
        int trigger,
        Integer overuseTrigger
) {
}
