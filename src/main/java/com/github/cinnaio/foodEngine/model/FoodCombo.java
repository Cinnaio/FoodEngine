package com.github.cinnaio.foodEngine.model;

import org.bukkit.Material;

import java.util.List;

public record FoodCombo(
        ComboConditions conditions,
        long cooldownSeconds,
        List<ParsedAction> actions
) {

    public record ComboConditions(
            String foodEngineId,
            Material material,
            long maxIntervalSeconds,
            int trigger
    ) {
    }
}

